package com.smarttv.remote.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.smarttv.remote.data.content.ContentItem
import com.smarttv.remote.data.content.ContentState
import com.smarttv.remote.data.content.Platform
import com.smarttv.remote.data.content.TmdbClient
import com.smarttv.remote.data.local.RemotePreferences
import com.smarttv.remote.data.model.*
import com.smarttv.remote.data.protocol.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.max
import java.security.PrivateKey
import java.security.cert.X509Certificate

data class RemoteUiState(
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val wifiConnected: Boolean = true,
    val wifiSsid: String = "Wi-Fi network",
    val discoveredDevices: List<TvDevice> = emptyList(),
    val isScanning: Boolean = false,
    val selectedDevice: TvDevice? = null,
    val pairingCode: String = "",
    val pairingError: String? = null,
    val isPairingLoading: Boolean = false,
    val skipDurationConfig: SkipDurationConfig = SkipDurationConfig(),
    val mediaState: MediaState = MediaState(),
    val appsList: List<TvApp> = TvApp.DEFAULT_APPS,
    val appFilter: AppCategory = AppCategory.ALL,
    val searchQuery: String = "",
    val keyboardText: String = "",
    val hapticEnabled: Boolean = true,
    val easyModeEnabled: Boolean = false,
    val isListeningVoice: Boolean = false,
    val lastRecognizedVoice: String = "",
    val isBusy: Boolean = false,
    val errorMessage: String? = null,
    val userMessage: String? = null,
    val savedDeviceLoaded: Boolean = false,
    val contentState: ContentState = ContentState.Idle,
    val tmdbKey: String = "",
    val tvAwake: Boolean? = null,
    val protocolLog: List<String> = emptyList(),
    val castConnected: Boolean = false,
    val castDeviceName: String? = null,
    val castStatus: String? = null,
    val castPlaying: Boolean = false,
    val tvTextFieldActive: Boolean = false,
    val mirroring: Boolean = false,
    val castAvailable: Boolean = true,
    val castRoutes: List<CastManager.CastRoute> = emptyList()
)

class RemoteViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()
    private val preferences = RemotePreferences(context)
    private val nsdDiscovery = NsdDiscoveryManager(context)
    private val pairingManager = PairingManager()
    private val remoteSession = RemoteSessionManager()
    private val speechManager = SpeechManager(context)
    val castManager = CastManager(context)

    private val _uiState = MutableStateFlow(RemoteUiState())
    val uiState: StateFlow<RemoteUiState> = _uiState.asStateFlow()

    private val credentials = CompletableDeferred<Pair<X509Certificate, PrivateKey>>()
    private val connectMutex = Mutex()
    private var rediscoveryTried = false

    init {
        castManager.initialize()
        _uiState.update { it.copy(castAvailable = castManager.castAvailable) }
        checkWifiConnection()
        loadOrGenerateCertificates()
        observePreferences()
        observeDiscovery()
        observeSession()
        setupVoiceListener()
        loadSavedDeviceAndReconnect()
        observeTvState()
        observeCast()
        observeContentKey()
    }

    private fun observeTvState() {
        viewModelScope.launch {
            remoteSession.tvAwake.collect { awake -> _uiState.update { it.copy(tvAwake = awake) } }
        }
        viewModelScope.launch {
            remoteSession.log.collect { log -> _uiState.update { it.copy(protocolLog = log) } }
        }
    }

    private fun observeCast() {
        viewModelScope.launch {
            remoteSession.textFieldActive.collect { a -> _uiState.update { it.copy(tvTextFieldActive = a) } }
        }
        viewModelScope.launch {
            com.smarttv.remote.data.protocol.MirrorHub.url.collect { u ->
                if (u != null) {
                    castManager.castMedia(u, "Phone screen", mimeType = "application/x-mpegURL", live = true)
                    _uiState.update { it.copy(mirroring = true) }
                }
            }
        }
        viewModelScope.launch {
            com.smarttv.remote.data.protocol.MirrorHub.error.collect { e ->
                if (e != null) {
                    _uiState.update { it.copy(mirroring = false) }
                    toast(e)
                }
            }
        }
        viewModelScope.launch {
            castManager.routes.collect { r -> _uiState.update { it.copy(castRoutes = r) } }
        }
        viewModelScope.launch {
            castManager.isCastConnected.collect { c -> _uiState.update { it.copy(castConnected = c) } }
        }
        viewModelScope.launch {
            castManager.castDeviceName.collect { n -> _uiState.update { it.copy(castDeviceName = n) } }
        }
        viewModelScope.launch {
            castManager.isPlaying.collect { p -> _uiState.update { it.copy(castPlaying = p) } }
        }
        viewModelScope.launch {
            castManager.castStatus.collect { st -> _uiState.update { it.copy(castStatus = st) } }
        }
    }

    private fun observeContentKey() {
        viewModelScope.launch {
            preferences.tmdbKeyFlow.distinctUntilChanged().collect { key ->
                _uiState.update { it.copy(tmdbKey = key) }
                loadContent()
            }
        }
    }

    fun loadContent() {
        val key = _uiState.value.tmdbKey
        if (key.isBlank()) {
            _uiState.update { it.copy(contentState = ContentState.NeedsKey) }
            return
        }
        _uiState.update { it.copy(contentState = ContentState.Loading) }
        viewModelScope.launch {
            val state = try {
                val rows = TmdbClient(key).loadHome()
                if (rows.isEmpty()) ContentState.Error("Nothing found right now. Please try again later.")
                else ContentState.Loaded(rows)
            } catch (e: TmdbClient.AuthException) {
                ContentState.Error("That TMDB key was not accepted. Check it in Settings.")
            } catch (e: java.io.IOException) {
                ContentState.Error("Can't load shows. Check the phone's internet connection.")
            } catch (e: Exception) {
                ContentState.Error("Could not load shows: ${e.message}")
            }
            _uiState.update { it.copy(contentState = state) }
        }
    }

    fun saveTmdbKey(key: String) {
        viewModelScope.launch { preferences.saveTmdbKey(key) }
    }

    fun openPlatform(platform: Platform) {
        triggerHaptic()
        viewModelScope.launch {
            if (!ensureConnected()) return@launch
            if (!remoteSession.launchApp("market://launch?id=${platform.tvPackage}")) {
                toast("Could not open ${platform.label} on the TV.")
            }
        }
    }

    fun playOnTv(item: ContentItem) {
        openPlatform(item.platform)
    }

    /** Types the title into whatever search box is open on the TV. */
    fun typeTitleOnTv(title: String) {
        triggerHaptic()
        viewModelScope.launch {
            if (!ensureConnected()) return@launch
            if (!remoteSession.sendText(title)) toast("Could not type on the TV.")
        }
    }

    // ---- Cast ----
    fun startCastScan() = castManager.startScan()
    fun stopCastScan() = castManager.stopScan()
    fun selectCastRoute(id: String) = castManager.selectRoute(id)
    fun stopCast() {
        stopMirror()
        castManager.disconnect()
    }

    fun startMirror(resultCode: Int, data: Intent) {
        if (!_uiState.value.castConnected) {
            toast("Choose your TV first.")
            return
        }
        val i = Intent(context, com.smarttv.remote.data.protocol.ScreenMirrorService::class.java)
            .putExtra("code", resultCode)
            .putExtra("data", data)
        androidx.core.content.ContextCompat.startForegroundService(context, i)
    }

    fun stopMirror() {
        context.stopService(Intent(context, com.smarttv.remote.data.protocol.ScreenMirrorService::class.java))
        _uiState.update { it.copy(mirroring = false) }
    }
    fun toggleCastPlayback() = castManager.togglePlayPause()
    fun stopCastPlayback() {
        stopMirror()
        castManager.stopPlayback()
    }

    fun castFile(uri: Uri, fallbackMime: String) {
        val mime = context.contentResolver.getType(uri) ?: fallbackMime
        val error = castManager.castLocalFile(uri, mime)
        if (error != null) toast(error)
    }

    fun castScreen() {
        val intents = listOf(
            Intent(Settings.ACTION_CAST_SETTINGS),
            Intent("android.settings.WIFI_DISPLAY_SETTINGS"),
            Intent(Settings.ACTION_WIRELESS_SETTINGS)
        )
        for (i in intents) {
            try {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(i)
                return
            } catch (_: Exception) {
            }
        }
        toast("Open your phone's Quick Settings and tap Cast / Smart View.")
    }

    private fun checkWifiConnection() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(network)
        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

        var ssid = "Wi-Fi network"
        if (isWifi) {
            try {
                val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                @Suppress("DEPRECATION")
                val rawSsid = wm.connectionInfo?.ssid?.replace("\"", "")
                if (!rawSsid.isNullOrBlank() && rawSsid != "<unknown ssid>") {
                    ssid = rawSsid
                }
            } catch (_: Exception) {
            }
        }

        _uiState.update { it.copy(wifiConnected = isWifi, wifiSsid = ssid) }
    }

    private fun loadOrGenerateCertificates() {
        viewModelScope.launch(Dispatchers.Default) {
            val (certPem, keyPem) = preferences.tlsCertFlow.first()
            var pair: Pair<X509Certificate, PrivateKey>? = null
            if (certPem != null && keyPem != null) {
                pair = try {
                    CertUtils.parseCertificatePem(certPem) to CertUtils.parsePrivateKeyPem(keyPem)
                } catch (_: Exception) {
                    null
                }
            }
            if (pair == null) {
                val generated = CertUtils.generateSelfSignedCertificate()
                preferences.saveTlsCredentials(generated.certPem, generated.keyPem)
                pair = generated.certificate to generated.privateKey
            }
            credentials.complete(pair)
        }
    }

    /** On launch: if a TV was paired before, reconnect straight away - no code needed. */
    private fun loadSavedDeviceAndReconnect() {
        viewModelScope.launch {
            val saved = preferences.pairedDeviceFlow.first()
            if (saved != null) {
                _uiState.update { it.copy(selectedDevice = saved, savedDeviceLoaded = true) }
                connectSaved(saved)
            } else {
                _uiState.update { it.copy(savedDeviceLoaded = true) }
            }
        }
    }

    private suspend fun connectSaved(device: TvDevice) {
        val (cert, key) = credentials.await()
        _uiState.update { it.copy(connectionStatus = ConnectionStatus.CONNECTING, errorMessage = null) }
        val result = connectMutex.withLock { remoteSession.connect(device, cert, key) }
        when (result) {
            is ConnectResult.Connected -> _uiState.update { it.copy(connectionStatus = ConnectionStatus.CONNECTED) }
            is ConnectResult.Unreachable -> {
                _uiState.update { it.copy(connectionStatus = ConnectionStatus.DISCONNECTED, errorMessage = result.message) }
                // The TV may have a new IP address (DHCP): look for it by name once.
                if (!rediscoveryTried) {
                    rediscoveryTried = true
                    nsdDiscovery.startDiscovery()
                }
            }
            is ConnectResult.PairingRequired ->
                _uiState.update {
                    it.copy(
                        connectionStatus = ConnectionStatus.DISCONNECTED,
                        selectedDevice = it.selectedDevice?.copy(isPaired = false),
                        errorMessage = result.message
                    )
                }
            is ConnectResult.Failed ->
                _uiState.update { it.copy(connectionStatus = ConnectionStatus.ERROR, errorMessage = result.message) }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferences.skipDurationConfigFlow.collect { cfg ->
                _uiState.update { it.copy(skipDurationConfig = cfg) }
            }
        }

        viewModelScope.launch {
            preferences.hapticFeedbackFlow.collect { enabled ->
                _uiState.update { it.copy(hapticEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            preferences.easyModeFlow.collect { enabled ->
                _uiState.update { it.copy(easyModeEnabled = enabled) }
            }
        }
    }

    private fun observeDiscovery() {
        viewModelScope.launch {
            nsdDiscovery.discoveredDevices.collect { list ->
                _uiState.update { it.copy(discoveredDevices = list) }

                // Saved TV got a new IP address: follow it and reconnect.
                val state = _uiState.value
                val saved = state.selectedDevice
                if (saved != null && saved.isPaired && state.connectionStatus != ConnectionStatus.CONNECTED) {
                    val moved = list.firstOrNull { it.id == saved.id && it.host != saved.host }
                    if (moved != null) {
                        val updated = saved.copy(host = moved.host, port = moved.port, pairingPort = moved.pairingPort)
                        preferences.savePairedDevice(updated)
                        _uiState.update { it.copy(selectedDevice = updated) }
                        connectSaved(updated)
                    }
                }
            }
        }

        viewModelScope.launch {
            nsdDiscovery.isScanning.collect { scanning ->
                _uiState.update { it.copy(isScanning = scanning) }
            }
        }
    }

    private fun observeSession() {
        viewModelScope.launch {
            remoteSession.isConnected.collect { connected ->
                _uiState.update {
                    val status = when {
                        connected -> ConnectionStatus.CONNECTED
                        it.connectionStatus == ConnectionStatus.CONNECTED -> ConnectionStatus.DISCONNECTED
                        else -> it.connectionStatus
                    }
                    it.copy(connectionStatus = status)
                }
            }
        }
    }

    private fun setupVoiceListener() {
        speechManager.onActionDetected = { action ->
            when (action) {
                is VoiceAction.Command -> sendCommand(action.command)
                is VoiceAction.LaunchApp -> launchAppByName(action.appName)
                is VoiceAction.SearchQuery -> {
                    onKeyboardTextChange(action.query)
                }
            }
        }
    }

    fun startDiscovery() {
        checkWifiConnection()
        _uiState.update { it.copy(errorMessage = null) }
        nsdDiscovery.startDiscovery()
    }

    fun stopDiscovery() {
        nsdDiscovery.stopDiscovery()
    }

    fun consumeUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    private fun toast(message: String) {
        _uiState.update { it.copy(userMessage = message) }
    }

    /** User typed an IP: it is only accepted if a TV really answers on it. */
    fun addManualIp(ip: String, onCodePrompt: () -> Unit, onConnected: () -> Unit) {
        val clean = ip.trim()
        if (!NsdDiscoveryManager.isValidIpv4(clean)) {
            _uiState.update { it.copy(errorMessage = "That is not a valid IP address.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessage = null) }
            val reachable = nsdDiscovery.probe(clean)
            if (!reachable) {
                _uiState.update {
                    it.copy(isBusy = false, errorMessage = "No Google TV answered at $clean. Check the IP, turn the TV on and use the same Wi-Fi.")
                }
                return@launch
            }
            val device = nsdDiscovery.manualDevice(clean)
            connectToDevice(device, onCodePrompt, onConnected)
        }
    }

    /**
     * Tries a real connection first (works without any code if this phone was paired before).
     * Only if the TV rejects us does it start pairing, which makes the TV show the code.
     */
    fun connectToDevice(device: TvDevice, onCodePrompt: () -> Unit, onConnected: () -> Unit) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedDevice = device,
                    isBusy = true,
                    errorMessage = null,
                    pairingError = null,
                    pairingCode = "",
                    connectionStatus = ConnectionStatus.CONNECTING
                )
            }
            val (cert, key) = credentials.await()
            val result = connectMutex.withLock { remoteSession.connect(device, cert, key) }
            when (result) {
                is ConnectResult.Connected -> {
                    markPaired(device)
                    _uiState.update { it.copy(isBusy = false, connectionStatus = ConnectionStatus.CONNECTED) }
                    onConnected()
                }
                is ConnectResult.PairingRequired -> beginPairing(device, cert, key, onCodePrompt)
                is ConnectResult.Unreachable ->
                    _uiState.update {
                        it.copy(isBusy = false, connectionStatus = ConnectionStatus.DISCONNECTED, errorMessage = result.message)
                    }
                is ConnectResult.Failed ->
                    _uiState.update {
                        it.copy(isBusy = false, connectionStatus = ConnectionStatus.ERROR, errorMessage = result.message)
                    }
            }
        }
    }

    private suspend fun beginPairing(device: TvDevice, cert: X509Certificate, key: PrivateKey, onCodePrompt: () -> Unit) {
        when (val r = pairingManager.startPairing(device.host, device.pairingPort, cert, key, "Smart TV Remote")) {
            is PairingResult.CodeDisplayedOnTv -> {
                _uiState.update { it.copy(isBusy = false, connectionStatus = ConnectionStatus.PAIRING_REQUIRED) }
                onCodePrompt()
            }
            is PairingResult.Error ->
                _uiState.update { it.copy(isBusy = false, connectionStatus = ConnectionStatus.DISCONNECTED, errorMessage = r.message) }
            is PairingResult.Success -> _uiState.update { it.copy(isBusy = false) }
        }
    }

    fun updatePairingCode(code: String) {
        val cleaned = code.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }
        if (cleaned.length <= 6) {
            _uiState.update { it.copy(pairingCode = cleaned, pairingError = null) }
        }
    }

    fun cancelPairing() {
        pairingManager.cancelPairing()
        _uiState.update { it.copy(isPairingLoading = false, pairingError = null) }
    }

    fun submitPairingCode(onSuccess: () -> Unit) {
        val code = _uiState.value.pairingCode
        if (code.length < 6) {
            _uiState.update { it.copy(pairingError = "Enter the 6 character code shown on your TV.") }
            return
        }
        val device = _uiState.value.selectedDevice ?: return

        _uiState.update { it.copy(isPairingLoading = true, pairingError = null) }

        viewModelScope.launch {
            when (val result = pairingManager.submitPairingCode(code)) {
                is PairingResult.Success -> {
                    val (cert, key) = credentials.await()
                    // The TV needs a moment before it accepts the freshly paired certificate.
                    var connected = false
                    var lastError: String? = null
                    for (attempt in 1..4) {
                        delay(if (attempt == 1) 600L else 900L)
                        val r = connectMutex.withLock { remoteSession.connect(device, cert, key) }
                        if (r is ConnectResult.Connected) {
                            connected = true
                            break
                        }
                        lastError = when (r) {
                            is ConnectResult.Unreachable -> r.message
                            is ConnectResult.PairingRequired -> r.message
                            is ConnectResult.Failed -> r.message
                            else -> null
                        }
                    }
                    if (connected) {
                        markPaired(device)
                        _uiState.update { it.copy(isPairingLoading = false, connectionStatus = ConnectionStatus.CONNECTED) }
                        onSuccess()
                    } else {
                        _uiState.update {
                            it.copy(
                                isPairingLoading = false,
                                pairingError = lastError ?: "Paired, but could not connect. Please try again."
                            )
                        }
                    }
                }
                is PairingResult.Error ->
                    _uiState.update { it.copy(isPairingLoading = false, pairingError = result.message) }
                else -> _uiState.update { it.copy(isPairingLoading = false) }
            }
        }
    }

    private suspend fun markPaired(device: TvDevice) {
        val paired = device.copy(isPaired = true, macAddress = device.macAddress ?: ArpTable.macFor(device.host))
        preferences.savePairedDevice(paired)
        _uiState.update { it.copy(selectedDevice = paired) }
    }

    fun connectToRemoteSession(device: TvDevice) {
        viewModelScope.launch {
            connectSaved(device)
            val state = _uiState.value
            if (state.connectionStatus != ConnectionStatus.CONNECTED) {
                state.errorMessage?.let { toast(it) }
            }
        }
    }

    fun disconnect() {
        remoteSession.disconnect()
        _uiState.update { it.copy(connectionStatus = ConnectionStatus.DISCONNECTED) }
    }

    /** Makes sure there is a live session; reconnects with the saved pairing if the link dropped. */
    private suspend fun ensureConnected(): Boolean {
        if (remoteSession.isConnected.value) return true
        val device = _uiState.value.selectedDevice ?: return false
        if (!device.isPaired) {
            toast("Pair with your TV first.")
            return false
        }
        val (cert, key) = credentials.await()
        _uiState.update { it.copy(connectionStatus = ConnectionStatus.CONNECTING) }
        val result = connectMutex.withLock {
            if (remoteSession.isConnected.value) ConnectResult.Connected
            else remoteSession.connect(device, cert, key)
        }
        return when (result) {
            is ConnectResult.Connected -> {
                _uiState.update { it.copy(connectionStatus = ConnectionStatus.CONNECTED) }
                true
            }
            is ConnectResult.Unreachable -> {
                _uiState.update { it.copy(connectionStatus = ConnectionStatus.DISCONNECTED) }
                toast(result.message)
                false
            }
            is ConnectResult.PairingRequired -> {
                _uiState.update { it.copy(connectionStatus = ConnectionStatus.DISCONNECTED) }
                toast("The TV no longer trusts this phone. Choose your TV again to pair.")
                false
            }
            is ConnectResult.Failed -> {
                _uiState.update { it.copy(connectionStatus = ConnectionStatus.ERROR) }
                toast(result.message)
                false
            }
        }
    }

    fun sendCommand(command: RemoteCommand) {
        triggerHaptic()
        viewModelScope.launch {
            if (!ensureConnected()) return@launch
            if (_uiState.value.tvAwake == false && command != RemoteCommand.POWER) {
                toast("The TV screen is off. Press the red Power button first.")
            }
            if (!remoteSession.sendCommand(command)) {
                toast("Command not sent. TV connection lost.")
            }
        }
    }

    fun sendPower() {
        triggerHaptic()
        viewModelScope.launch {
            val device = _uiState.value.selectedDevice
            if (device == null) {
                toast("Choose a TV first.")
                return@launch
            }
            // 1. TVs that keep their network on in standby accept the power key directly.
            if (device.isPaired && withTimeoutQuiet { ensureConnectedSilently() }) {
                remoteSession.sendCommand(RemoteCommand.POWER)
                return@launch
            }
            // 2. Otherwise wake it with a Wake-on-LAN packet and reconnect.
            val mac = device.macAddress
            if (mac != null) {
                toast("Waking the TV...")
                WakeOnLan.sendMagicPacket(mac)
                delay(4000)
                if (ensureConnectedSilently()) {
                    toast("TV is waking up.")
                    return@launch
                }
            }
            toast("Can't reach the TV. It may be fully powered off.")
        }
    }

    private suspend fun withTimeoutQuiet(block: suspend () -> Boolean): Boolean =
        try { block() } catch (_: Exception) { false }

    private suspend fun ensureConnectedSilently(): Boolean {
        if (remoteSession.isConnected.value) return true
        val device = _uiState.value.selectedDevice ?: return false
        val (cert, key) = credentials.await()
        val r = connectMutex.withLock { remoteSession.connect(device, cert, key) }
        if (r is ConnectResult.Connected) {
            _uiState.update { it.copy(connectionStatus = ConnectionStatus.CONNECTED) }
            return true
        }
        return false
    }

    // The TV does not report its playback position, so seeking sends rewind/forward keys.
    // Most apps jump about 10 seconds per press, so longer skips repeat the key.
    fun sendSeekBackward() {
        triggerHaptic()
        val presses = max(1, _uiState.value.skipDurationConfig.backwardSeconds / 10)
        repeatKey(RemoteCommand.MEDIA_REWIND, presses)
    }

    fun sendSeekForward() {
        triggerHaptic()
        val presses = max(1, _uiState.value.skipDurationConfig.forwardSeconds / 10)
        repeatKey(RemoteCommand.MEDIA_FAST_FORWARD, presses)
    }

    private fun repeatKey(command: RemoteCommand, times: Int) {
        viewModelScope.launch {
            if (!ensureConnected()) return@launch
            repeat(times) {
                remoteSession.sendCommand(command)
                delay(120)
            }
        }
    }

    fun togglePlayPause() {
        triggerHaptic()
        _uiState.update { it.copy(mediaState = it.mediaState.copy(isPlaying = !it.mediaState.isPlaying)) }
        viewModelScope.launch {
            if (!ensureConnected()) return@launch
            remoteSession.sendCommand(RemoteCommand.MEDIA_PLAY_PAUSE)
        }
    }

    fun updateSkipDurations(backward: Int, forward: Int, custom: Int) {
        viewModelScope.launch {
            preferences.saveSkipDurations(backward, forward, custom)
        }
    }

    fun launchApp(app: TvApp) {
        triggerHaptic()
        viewModelScope.launch {
            if (!ensureConnected()) return@launch
            if (launchAndVerify("market://launch?id=${app.packageName}", app.packageName)) return@launch
            if (launchAndVerify(app.deepLinkUri, app.packageName)) return@launch
            toast("${app.name} did not open. Is it installed on the TV?")
        }
    }

    /** Sends an app link, then waits briefly for the TV to report the app as the foreground app. */
    private suspend fun launchAndVerify(link: String, pkg: String): Boolean {
        if (remoteSession.currentApp.value == pkg) return true
        if (!remoteSession.launchApp(link)) return false
        return kotlinx.coroutines.withTimeoutOrNull(3500) {
            remoteSession.currentApp.first { it == pkg }
            true
        } ?: false
    }

    fun launchAppByName(name: String) {
        val app = _uiState.value.appsList.find { it.name.contains(name, ignoreCase = true) }
        app?.let { launchApp(it) }
    }

    fun setAppFilter(filter: AppCategory) {
        _uiState.update { it.copy(appFilter = filter) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    val filteredApps: List<TvApp>
        get() {
            val state = _uiState.value
            return state.appsList.filter { app ->
                val matchesCategory = when (state.appFilter) {
                    AppCategory.ALL -> true
                    AppCategory.STREAMING -> app.category == AppCategory.STREAMING
                    AppCategory.OTHER -> app.category == AppCategory.OTHER
                }
                val matchesSearch = state.searchQuery.isBlank() || app.name.contains(state.searchQuery, ignoreCase = true)
                matchesCategory && matchesSearch
            }
        }

    private val keyboardQueue = kotlinx.coroutines.channels.Channel<suspend () -> Unit>(kotlinx.coroutines.channels.Channel.UNLIMITED)

    init {
        viewModelScope.launch { for (job in keyboardQueue) job() }
    }

    /** Phone text box changed: send only the difference to the TV (typed chars or backspaces). */
    fun onKeyboardTextChange(new: String) {
        val old = _uiState.value.keyboardText
        var p = 0
        while (p < old.length && p < new.length && old[p] == new[p]) p++
        val deletions = old.length - p
        val additions = new.substring(p)
        _uiState.update { it.copy(keyboardText = new) }
        keyboardQueue.trySend {
            if (ensureConnected()) {
                // Best: replace the TV text box content directly. Fallback: type key by key.
                val done = remoteSession.textFieldActive.value && remoteSession.sendImeText(new)
                if (!done) {
                    repeat(deletions) { remoteSession.sendBackspace() }
                    if (additions.isNotEmpty()) remoteSession.sendText(additions)
                }
            }
        }
    }

    fun onKeyboardBackspace() {
        triggerHaptic()
        val cur = _uiState.value.keyboardText
        if (cur.isNotEmpty()) onKeyboardTextChange(cur.dropLast(1))
        else keyboardQueue.trySend { if (ensureConnected()) remoteSession.sendBackspace() }
    }

    fun onKeyboardClear() {
        triggerHaptic()
        onKeyboardTextChange("")
    }

    fun onKeyboardEnter() {
        triggerHaptic()
        keyboardQueue.trySend { if (ensureConnected()) remoteSession.sendEnter() }
    }

    fun toggleVoiceListening() {
        triggerHaptic()
        val listening = !_uiState.value.isListeningVoice
        _uiState.update { it.copy(isListeningVoice = listening) }
        if (listening) {
            speechManager.startListening()
        } else {
            speechManager.stopListening()
        }
    }

    fun toggleHapticFeedback(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setHapticFeedback(enabled)
        }
    }

    fun toggleEasyMode(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setEasyMode(enabled)
        }
    }

    private fun triggerHaptic() {
        if (!_uiState.value.hapticEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (_: Exception) {}
    }

    override fun onCleared() {
        super.onCleared()
        nsdDiscovery.stopDiscovery()
        remoteSession.disconnect()
        pairingManager.cancelPairing()
        speechManager.stopListening()
    }
}
