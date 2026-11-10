package com.smarttv.remote.data.protocol

import android.util.Log
import com.smarttv.remote.data.model.RemoteCommand
import com.smarttv.remote.data.model.TvDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.net.ssl.SSLException
import javax.net.ssl.SSLSocket

sealed class ConnectResult {
    object Connected : ConnectResult()
    /** TV is off, asleep without network standby, or on another network. */
    data class Unreachable(val message: String) : ConnectResult()
    /** TV answered but rejected our certificate: this phone is not paired (any more). */
    data class PairingRequired(val message: String) : ConnectResult()
    data class Failed(val message: String) : ConnectResult()
}

/**
 * Android TV Remote Protocol v2 control session (TV port 6466).
 * "Connected" is only reported after the TV completed the real handshake.
 */
class RemoteSessionManager {

    companion object {
        private const val TAG = "RemoteSession"

        private const val FEATURES = 622 // ping | key | ime | power | volume | app link

        private const val DIRECTION_START_LONG = 1
        private const val DIRECTION_END_LONG = 2
        private const val DIRECTION_SHORT = 3

        private const val KEY_DEL = 67
        private const val KEY_ENTER = 66
        private const val KEY_SHIFT_LEFT = 59
    }

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError.asStateFlow()

    private var socket: SSLSocket? = null
    private var output: OutputStream? = null
    private var readJob: Job? = null
    private val writeMutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend fun connect(
        device: TvDevice,
        cert: X509Certificate,
        key: PrivateKey
    ): ConnectResult = withContext(Dispatchers.IO) {
        disconnect()
        _connectionError.value = null

        var receivedAnything = false
        try {
            Log.d(TAG, "Connecting to ${device.name} at ${device.host}:${device.port}")
            val sock = TlsSocketFactory.createSslSocket(
                device.host, device.port, cert, key,
                connectTimeoutMs = 4000,
                readTimeoutMs = 12000
            )
            socket = sock
            output = sock.outputStream
            val input = sock.inputStream

            // Handshake: TV sends configure -> we answer, TV sends set_active -> we answer,
            // TV sends start. Only then the session is really usable.
            var started = false
            val deadline = System.currentTimeMillis() + 15000
            while (!started) {
                if (System.currentTimeMillis() > deadline) throw IOException("TV did not finish the handshake")
                val fields = ProtoReader.parse(Framing.read(input))
                receivedAnything = true
                for (f in fields) {
                    when (f.number) {
                        1 -> sendConfigure()
                        2 -> sendSetActive()
                        3 -> throw IOException("TV reported an error during handshake")
                        8 -> answerPing(f)
                        40 -> started = true
                    }
                }
            }

            sock.soTimeout = 30000
            _isConnected.value = true
            readJob = scope.launch { readLoop(input) }
            Log.d(TAG, "Connected to ${device.name}")
            ConnectResult.Connected
        } catch (e: Exception) {
            Log.w(TAG, "Connection failed: ${e.message}")
            disconnect()
            classify(e, receivedAnything).also { r ->
                _connectionError.value = when (r) {
                    is ConnectResult.Unreachable -> r.message
                    is ConnectResult.PairingRequired -> r.message
                    is ConnectResult.Failed -> r.message
                    ConnectResult.Connected -> null
                }
            }
        }
    }

    private fun classify(e: Exception, receivedAnything: Boolean): ConnectResult = when {
        e is ConnectException || e is NoRouteToHostException || e is UnknownHostException ||
            (e is SocketTimeoutException && !receivedAnything && e.message?.contains("connect", true) == true) ->
            ConnectResult.Unreachable("Can't reach the TV. Turn it on and make sure it is on the same Wi-Fi.")
        e is SocketTimeoutException ->
            ConnectResult.Unreachable("The TV is not responding. Turn it on and check your Wi-Fi.")
        !receivedAnything && (e is SSLException || e is EOFException || e is SocketException) ->
            ConnectResult.PairingRequired("This phone is not paired with the TV yet.")
        else -> ConnectResult.Failed("Connection failed: ${e.message ?: "unknown error"}")
    }

    private suspend fun readLoop(input: InputStream) {
        while (scope.isActive && _isConnected.value) {
            try {
                val fields = ProtoReader.parse(Framing.read(input))
                for (f in fields) {
                    when (f.number) {
                        2 -> sendSetActive()
                        8 -> answerPing(f)
                        3 -> Log.w(TAG, "TV reported a remote error")
                    }
                }
            } catch (e: SocketTimeoutException) {
                // Idle for 30 s: probe the link. A failed write means the TV is gone.
                try {
                    send { message(8) { int(1, 1) } }
                } catch (_: Exception) {
                    markLost("Connection to the TV was lost.")
                    return
                }
            } catch (e: Exception) {
                markLost("Connection to the TV was lost.")
                return
            }
        }
    }

    private fun markLost(message: String) {
        if (_isConnected.value) {
            _connectionError.value = message
        }
        closeSocket()
        _isConnected.value = false
    }

    suspend fun sendCommand(command: RemoteCommand): Boolean = sendKey(command.keyCode)

    suspend fun sendKey(keyCode: Int): Boolean = withContext(Dispatchers.IO) {
        if (!_isConnected.value) return@withContext false
        try {
            send { message(10) { int(1, keyCode); int(2, DIRECTION_SHORT) } }
            true
        } catch (e: Exception) {
            Log.w(TAG, "Key $keyCode failed: ${e.message}")
            markLost("Connection to the TV was lost.")
            false
        }
    }

    suspend fun sendText(text: String): Boolean = withContext(Dispatchers.IO) {
        if (!_isConnected.value) return@withContext false
        try {
            for (c in text) {
                val (keyCode, shift) = charToKey(c)
                if (keyCode == 0) continue
                if (shift) send { message(10) { int(1, KEY_SHIFT_LEFT); int(2, DIRECTION_START_LONG) } }
                send { message(10) { int(1, keyCode); int(2, DIRECTION_SHORT) } }
                if (shift) send { message(10) { int(1, KEY_SHIFT_LEFT); int(2, DIRECTION_END_LONG) } }
                delay(25)
            }
            true
        } catch (e: Exception) {
            markLost("Connection to the TV was lost.")
            false
        }
    }

    suspend fun sendBackspace(): Boolean = sendKey(KEY_DEL)

    suspend fun sendEnter(): Boolean = sendKey(KEY_ENTER)

    suspend fun launchApp(appLink: String): Boolean = withContext(Dispatchers.IO) {
        if (!_isConnected.value) return@withContext false
        try {
            send { message(90) { string(1, appLink) } }
            true
        } catch (e: Exception) {
            markLost("Connection to the TV was lost.")
            false
        }
    }

    private suspend fun send(block: ProtoWriter.() -> Unit) {
        val data = ProtoWriter().apply(block).toByteArray()
        writeMutex.withLock {
            val out = output ?: throw IOException("Not connected")
            Framing.write(out, data)
        }
    }

    private suspend fun sendConfigure() = send {
        message(1) {
            int(1, FEATURES)
            message(2) {
                string(1, "Android")
                string(2, "Smart TV Remote")
                int(3, 1)
                string(4, "1")
                string(5, "com.smarttv.remote")
                string(6, "1.0.0")
            }
        }
    }

    private suspend fun sendSetActive() = send { message(2) { int(1, FEATURES) } }

    private suspend fun answerPing(request: ProtoField) {
        val value = request.bytes?.let { ProtoReader.find(ProtoReader.parse(it), 1)?.varint?.toInt() } ?: 0
        send { message(9) { int(1, value) } }
    }

    private fun charToKey(c: Char): Pair<Int, Boolean> {
        val lower = c.lowercaseChar()
        val shift = c.isUpperCase()
        val code = when (lower) {
            in 'a'..'z' -> 29 + (lower - 'a')
            in '0'..'9' -> 7 + (lower - '0')
            ' ' -> 62
            '\n' -> KEY_ENTER
            '.' -> 56
            ',' -> 55
            '-' -> 69
            '/' -> 76
            '@' -> 77
            '\'' -> 75
            ';' -> 74
            else -> 0
        }
        return code to shift
    }

    private fun closeSocket() {
        readJob?.cancel()
        readJob = null
        try {
            socket?.close()
        } catch (_: Exception) {
        }
        socket = null
        output = null
    }

    fun disconnect() {
        closeSocket()
        _isConnected.value = false
    }
}
