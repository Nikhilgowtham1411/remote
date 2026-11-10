package com.smarttv.remote.data.protocol

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.smarttv.remote.data.model.TvDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetAddress

class NsdDiscoveryManager(private val context: Context) {

    companion object {
        private const val TAG = "NsdDiscovery"
        private const val SERVICE_TYPE_ATV = "_androidtvremote2._tcp."
        private const val SERVICE_TYPE_CAST = "_googlecast._tcp."
    }

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private val _discoveredDevices = MutableStateFlow<List<TvDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<TvDevice>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val discoveredMap = mutableMapOf<String, TvDevice>()
    private var discoveryListenerAtv: NsdManager.DiscoveryListener? = null
    private var discoveryListenerCast: NsdManager.DiscoveryListener? = null

    fun startDiscovery() {
        stopDiscovery()
        discoveredMap.clear()
        _discoveredDevices.value = emptyList()
        _isScanning.value = true

        discoveryListenerAtv = createDiscoveryListener(SERVICE_TYPE_ATV)
        discoveryListenerCast = createDiscoveryListener(SERVICE_TYPE_CAST)

        try {
            nsdManager.discoverServices(SERVICE_TYPE_ATV, NsdManager.PROTOCOL_DNS_SD, discoveryListenerAtv)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start ATV discovery: ${e.message}")
        }

        try {
            nsdManager.discoverServices(SERVICE_TYPE_CAST, NsdManager.PROTOCOL_DNS_SD, discoveryListenerCast)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Cast discovery: ${e.message}")
        }
    }

    fun stopDiscovery() {
        _isScanning.value = false
        discoveryListenerAtv?.let {
            try { nsdManager.stopServiceDiscovery(it) } catch (_: Exception) {}
        }
        discoveryListenerCast?.let {
            try { nsdManager.stopServiceDiscovery(it) } catch (_: Exception) {}
        }
        discoveryListenerAtv = null
        discoveryListenerCast = null
    }

    private fun createDiscoveryListener(serviceType: String) = object : NsdManager.DiscoveryListener {
        override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
            Log.e(TAG, "Discovery failed to start for $serviceType: $errorCode")
            _isScanning.value = false
        }

        override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
            Log.e(TAG, "Discovery failed to stop: $errorCode")
        }

        override fun onDiscoveryStarted(serviceType: String?) {
            Log.d(TAG, "Discovery started for $serviceType")
        }

        override fun onDiscoveryStopped(serviceType: String?) {
            Log.d(TAG, "Discovery stopped for $serviceType")
        }

        override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
            serviceInfo?.let { info ->
                resolveService(info)
            }
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
            serviceInfo?.let { info ->
                discoveredMap.remove(info.serviceName)
                _discoveredDevices.value = discoveredMap.values.toList()
            }
        }
    }

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        try {
            nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                    Log.w(TAG, "Resolve failed for ${serviceInfo?.serviceName}: $errorCode")
                }

                override fun onServiceResolved(resolvedInfo: NsdServiceInfo?) {
                    resolvedInfo?.let { info ->
                        val host = info.host?.hostAddress ?: return
                        val name = info.serviceName.replace("\\.", ".")
                        val tvDevice = TvDevice(
                            id = info.serviceName,
                            name = name,
                            model = if (name.contains("Lumio", ignoreCase = true)) "Lumio Vision 7 (2026)" else "Google TV",
                            host = host,
                            port = 6466,
                            pairingPort = 6467,
                            isGoogleTv = true,
                            isAvailable = true
                        )
                        discoveredMap[info.serviceName] = tvDevice
                        _discoveredDevices.value = discoveredMap.values.toList()
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Exception resolving service: ${e.message}")
        }
    }

    fun addManualDevice(ip: String, name: String = "Lumio Vision 7"): TvDevice {
        val device = TvDevice(
            id = "manual_$ip",
            name = name,
            model = "Lumio Vision 7 (2026)",
            host = ip,
            port = 6466,
            pairingPort = 6467,
            isGoogleTv = true,
            isAvailable = true
        )
        discoveredMap[device.id] = device
        _discoveredDevices.value = discoveredMap.values.toList()
        return device
    }
}
