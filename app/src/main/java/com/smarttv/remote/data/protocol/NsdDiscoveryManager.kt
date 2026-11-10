package com.smarttv.remote.data.protocol

import android.content.Context
import android.net.ConnectivityManager
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import com.smarttv.remote.data.model.TvDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

/**
 * Finds real Google TVs on the local Wi-Fi: mDNS (_androidtvremote2._tcp) plus a fast
 * port-6466 sweep of the local /24 for routers that block multicast. Nothing is faked:
 * if no TV answers, the list stays empty.
 */
class NsdDiscoveryManager(private val context: Context) {

    companion object {
        private const val TAG = "NsdDiscovery"
        private const val SERVICE_TYPE_ATV = "_androidtvremote2._tcp."
        const val REMOTE_PORT = 6466
        const val PAIRING_PORT = 6467
        private const val SCAN_WINDOW_MS = 12000L
        private val IPV4 = Regex("""^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$""")

        fun isValidIpv4(ip: String): Boolean {
            val m = IPV4.matchEntire(ip.trim()) ?: return false
            return m.groupValues.drop(1).all { it.toInt() in 0..255 }
        }
    }

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _discoveredDevices = MutableStateFlow<List<TvDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<TvDevice>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val devicesByHost = ConcurrentHashMap<String, TvDevice>()
    private var listener: NsdManager.DiscoveryListener? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    private var sessionJob: Job? = null
    @Volatile private var generation = 0

    private val resolveQueue = ArrayDeque<NsdServiceInfo>()
    private var resolving = false

    fun startDiscovery() {
        stopDiscovery()
        val gen = ++generation
        devicesByHost.clear()
        publish()
        _isScanning.value = true

        try {
            multicastLock = wifiManager.createMulticastLock("smarttvremote-mdns").apply {
                setReferenceCounted(false)
                acquire()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Multicast lock failed: ${e.message}")
        }

        val l = createListener()
        listener = l
        try {
            nsdManager.discoverServices(SERVICE_TYPE_ATV, NsdManager.PROTOCOL_DNS_SD, l)
        } catch (e: Exception) {
            Log.e(TAG, "mDNS discovery failed to start: ${e.message}")
        }

        sessionJob = scope.launch {
            try {
                sweepSubnet()
                delay(SCAN_WINDOW_MS)
            } finally {
                // Ignore a cancelled older session so it cannot stop a newer scan.
                if (gen == generation) stopDiscoveryInternal(keepResults = true)
            }
        }
    }

    fun stopDiscovery() {
        generation++
        sessionJob?.cancel()
        sessionJob = null
        stopDiscoveryInternal(keepResults = true)
    }

    private fun stopDiscoveryInternal(keepResults: Boolean) {
        _isScanning.value = false
        listener?.let {
            try { nsdManager.stopServiceDiscovery(it) } catch (_: Exception) {}
        }
        listener = null
        try {
            multicastLock?.takeIf { it.isHeld }?.release()
        } catch (_: Exception) {
        }
        multicastLock = null
        synchronized(resolveQueue) { resolveQueue.clear() }
        if (!keepResults) {
            devicesByHost.clear()
            publish()
        }
    }

    private fun createListener() = object : NsdManager.DiscoveryListener {
        override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
            Log.e(TAG, "Discovery failed to start: $errorCode")
        }

        override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {}
        override fun onDiscoveryStarted(serviceType: String?) {}
        override fun onDiscoveryStopped(serviceType: String?) {}

        override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
            serviceInfo?.let { enqueueResolve(it) }
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo?) {}
    }

    // Android only allows one resolve at a time, so resolves are queued.
    private fun enqueueResolve(info: NsdServiceInfo) {
        synchronized(resolveQueue) {
            resolveQueue.addLast(info)
            if (!resolving) resolveNext()
        }
    }

    private fun resolveNext() {
        val next = synchronized(resolveQueue) {
            val item = resolveQueue.removeFirstOrNull()
            resolving = item != null
            item
        } ?: return

        try {
            @Suppress("DEPRECATION")
            nsdManager.resolveService(next, object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                    Log.w(TAG, "Resolve failed for ${serviceInfo?.serviceName}: $errorCode")
                    resolveNext()
                }

                override fun onServiceResolved(resolved: NsdServiceInfo?) {
                    try {
                        @Suppress("DEPRECATION")
                        val host = resolved?.host as? Inet4Address
                        if (resolved != null && host != null) {
                            val ip = host.hostAddress ?: return
                            val name = resolved.serviceName.replace("\\032", " ")
                            addDevice(
                                TvDevice(
                                    id = name,
                                    name = name,
                                    model = "Google TV",
                                    host = ip,
                                    port = REMOTE_PORT,
                                    pairingPort = PAIRING_PORT
                                ),
                                named = true
                            )
                        }
                    } finally {
                        resolveNext()
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Resolve exception: ${e.message}")
            resolveNext()
        }
    }

    private fun addDevice(device: TvDevice, named: Boolean) {
        val existing = devicesByHost[device.host]
        // A name from mDNS always wins over a bare IP found by the port sweep.
        if (existing == null || named) {
            devicesByHost[device.host] = device
            publish()
        }
    }

    private fun publish() {
        _discoveredDevices.value = devicesByHost.values.sortedBy { it.name.lowercase() }
    }

    private suspend fun sweepSubnet() {
        val local = localIpv4() ?: return
        val parts = local.split(".")
        if (parts.size != 4) return
        val base = parts.take(3).joinToString(".")
        val gate = Semaphore(48)

        coroutineScope {
            (1..254).map { i ->
                async {
                    val ip = "$base.$i"
                    if (ip != local) {
                        gate.withPermit {
                            if (isPortOpen(ip, REMOTE_PORT, 700)) {
                                addDevice(
                                    TvDevice(
                                        id = "ip_$ip",
                                        name = "Google TV",
                                        model = "Google TV",
                                        host = ip,
                                        port = REMOTE_PORT,
                                        pairingPort = PAIRING_PORT
                                    ),
                                    named = false
                                )
                            }
                        }
                    }
                }
            }.awaitAll()
        }
    }

    private fun localIpv4(): String? {
        return try {
            val network = connectivityManager.activeNetwork ?: return null
            val props = connectivityManager.getLinkProperties(network) ?: return null
            props.linkAddresses
                .map { it.address }
                .filterIsInstance<Inet4Address>()
                .firstOrNull { !it.isLoopbackAddress }
                ?.hostAddress
        } catch (e: Exception) {
            null
        }
    }

    /** True when the TV's remote service accepts connections on this IP. */
    suspend fun probe(ip: String): Boolean = withContext(Dispatchers.IO) {
        isPortOpen(ip, REMOTE_PORT, 2500) || isPortOpen(ip, PAIRING_PORT, 2500)
    }

    private fun isPortOpen(ip: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { s ->
                s.connect(InetSocketAddress(ip, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    fun manualDevice(ip: String): TvDevice {
        val device = TvDevice(
            id = "ip_$ip",
            name = "Google TV",
            model = "Google TV",
            host = ip,
            port = REMOTE_PORT,
            pairingPort = PAIRING_PORT
        )
        addDevice(device, named = false)
        return device
    }
}
