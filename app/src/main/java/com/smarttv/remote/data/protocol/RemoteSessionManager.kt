package com.smarttv.remote.data.protocol

import android.util.Log
import com.smarttv.remote.data.model.RemoteCommand
import com.smarttv.remote.data.model.TvDevice
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.net.ssl.SSLSocket

class RemoteSessionManager {

    companion object {
        private const val TAG = "RemoteSession"
    }

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError.asStateFlow()

    private var socket: SSLSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var readJob: Job? = null
    private var pingJob: Job? = null
    private var currentDevice: TvDevice? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend fun connect(
        device: TvDevice,
        cert: X509Certificate,
        key: PrivateKey
    ): Boolean = withContext(Dispatchers.IO) {
        disconnect()
        currentDevice = device
        _connectionError.value = null

        try {
            Log.d(TAG, "Connecting to ${device.name} at ${device.host}:${device.port}...")
            val sock = TlsSocketFactory.createSslSocket(
                device.host,
                device.port,
                cert,
                key,
                timeoutMs = 6000
            )
            socket = sock
            outputStream = sock.getOutputStream()
            inputStream = sock.getInputStream()

            // Send initial configure message
            sendConfigure()

            _isConnected.value = true
            startListening()
            startKeepAlive()
            Log.d(TAG, "Connected successfully to ${device.name}!")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Connection failed: ${e.message}", e)
            _connectionError.value = "Failed to connect to ${device.name}: ${e.localizedMessage}"
            _isConnected.value = false
            disconnect()
            false
        }
    }

    private fun startListening() {
        readJob = scope.launch {
            try {
                val inStream = inputStream ?: return@launch
                while (isActive && _isConnected.value) {
                    val message = readLengthDelimited(inStream)
                    if (message.isEmpty()) {
                        delay(50)
                        continue
                    }
                    handleIncomingMessage(message)
                }
            } catch (e: Exception) {
                if (isActive) {
                    Log.w(TAG, "Socket read exception: ${e.message}")
                    _isConnected.value = false
                    _connectionError.value = "Connection lost: ${e.localizedMessage}"
                }
            }
        }
    }

    private fun startKeepAlive() {
        pingJob = scope.launch {
            while (isActive && _isConnected.value) {
                delay(15000)
                sendPing()
            }
        }
    }

    private fun handleIncomingMessage(data: ByteArray) {
        // Parse protobuf tag to check if it's PingRequest (field 1 or 2)
        // If ping request received, immediately answer with pong
        try {
            sendPong()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to respond with pong: ${e.message}")
        }
    }

    suspend fun sendCommand(command: RemoteCommand): Boolean = withContext(Dispatchers.IO) {
        if (!_isConnected.value || outputStream == null) {
            Log.w(TAG, "Cannot send command: Remote not connected.")
            return@withContext false
        }
        try {
            // Android TV Remote v2: Key press consists of ACTION_DOWN followed by ACTION_UP
            sendKeyEvent(command.keyCode, direction = 1) // 1 = ACTION_DOWN
            delay(40)
            sendKeyEvent(command.keyCode, direction = 2) // 2 = ACTION_UP
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending command ${command.label}: ${e.message}")
            false
        }
    }

    suspend fun sendText(text: String): Boolean = withContext(Dispatchers.IO) {
        if (!_isConnected.value || outputStream == null) return@withContext false
        try {
            // Send each character or text injection
            for (char in text) {
                val keyCode = charToKeyCode(char)
                if (keyCode != 0) {
                    sendKeyEvent(keyCode, 1)
                    delay(25)
                    sendKeyEvent(keyCode, 2)
                    delay(25)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending text: ${e.message}")
            false
        }
    }

    suspend fun launchApp(deepLinkUri: String): Boolean = withContext(Dispatchers.IO) {
        if (!_isConnected.value || outputStream == null) return@withContext false
        try {
            val packet = buildAppLinkPacket(deepLinkUri)
            val out = outputStream ?: return@withContext false
            writeLengthDelimited(out, packet)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error launching app: ${e.message}")
            false
        }
    }

    private fun sendKeyEvent(keyCode: Int, direction: Int) {
        val out = outputStream ?: return
        val packet = buildKeyInjectPacket(keyCode, direction)
        writeLengthDelimited(out, packet)
    }

    private fun sendConfigure() {
        val out = outputStream ?: return
        val baos = ByteArrayOutputStream()
        // field 1: configure submessage
        val sub = ByteArrayOutputStream()
        // code = 622, model = "SmartTVRemote"
        writeStringField(sub, 1, "SmartTVRemote")
        writeStringField(sub, 2, "GoogleTVCompanion")
        val subBytes = sub.toByteArray()
        writeTag(baos, 1, 2)
        writeVarint(baos, subBytes.size)
        baos.write(subBytes)
        writeLengthDelimited(out, baos.toByteArray())
    }

    private fun sendPing() {
        val out = outputStream ?: return
        val baos = ByteArrayOutputStream()
        // RemotePingRequest (field 2)
        baos.write(byteArrayOf(0x10, 0x01)) // val1 = 1
        writeLengthDelimited(out, baos.toByteArray())
    }

    private fun sendPong() {
        val out = outputStream ?: return
        val baos = ByteArrayOutputStream()
        // RemotePingResponse (field 3)
        baos.write(byteArrayOf(0x18, 0x01)) // val1 = 1
        writeLengthDelimited(out, baos.toByteArray())
    }

    private fun buildKeyInjectPacket(keyCode: Int, direction: Int): ByteArray {
        val baos = ByteArrayOutputStream()
        // RemoteKeyInject (field 7)
        val sub = ByteArrayOutputStream()
        // field 1: keyCode (varint)
        writeTag(sub, 1, 0)
        writeVarint(sub, keyCode)
        // field 2: direction (1 = down, 2 = up)
        writeTag(sub, 2, 0)
        writeVarint(sub, direction)

        val subBytes = sub.toByteArray()
        writeTag(baos, 7, 2)
        writeVarint(baos, subBytes.size)
        baos.write(subBytes)
        return baos.toByteArray()
    }

    private fun buildAppLinkPacket(appLink: String): ByteArray {
        val baos = ByteArrayOutputStream()
        // RemoteAppLink (field 19)
        val sub = ByteArrayOutputStream()
        writeStringField(sub, 1, appLink)
        val subBytes = sub.toByteArray()
        writeTag(baos, 19, 2)
        writeVarint(baos, subBytes.size)
        baos.write(subBytes)
        return baos.toByteArray()
    }

    private fun charToKeyCode(c: Char): Int {
        return when (c.lowercaseChar()) {
            'a' -> 29
            'b' -> 30
            'c' -> 31
            'd' -> 32
            'e' -> 33
            'f' -> 34
            'g' -> 35
            'h' -> 36
            'i' -> 37
            'j' -> 38
            'k' -> 39
            'l' -> 40
            'm' -> 41
            'n' -> 42
            'o' -> 43
            'p' -> 44
            'q' -> 45
            'r' -> 46
            's' -> 47
            't' -> 48
            'u' -> 49
            'v' -> 50
            'w' -> 51
            'x' -> 52
            'y' -> 53
            'z' -> 54
            '0' -> 7
            '1' -> 8
            '2' -> 9
            '3' -> 10
            '4' -> 11
            '5' -> 12
            '6' -> 13
            '7' -> 14
            '8' -> 15
            '9' -> 16
            ' ' -> 62
            '\n' -> 66
            else -> 0
        }
    }

    fun disconnect() {
        readJob?.cancel()
        pingJob?.cancel()
        readJob = null
        pingJob = null
        try {
            outputStream?.close()
            inputStream?.close()
            socket?.close()
        } catch (_: Exception) {}
        socket = null
        outputStream = null
        inputStream = null
        _isConnected.value = false
    }

    private fun writeLengthDelimited(out: OutputStream, data: ByteArray) {
        val baos = ByteArrayOutputStream()
        writeVarint(baos, data.size)
        out.write(baos.toByteArray())
        out.write(data)
        out.flush()
    }

    private fun readLengthDelimited(inStream: InputStream): ByteArray {
        val length = readVarint(inStream)
        if (length <= 0 || length > 65536) return ByteArray(0)
        val buffer = ByteArray(length)
        var totalRead = 0
        while (totalRead < length) {
            val read = inStream.read(buffer, totalRead, length - totalRead)
            if (read < 0) break
            totalRead += read
        }
        return buffer
    }

    private fun writeTag(os: OutputStream, fieldNumber: Int, wireType: Int) {
        writeVarint(os, (fieldNumber shl 3) or wireType)
    }

    private fun writeVarint(os: OutputStream, value: Int) {
        var v = value
        while (true) {
            if ((v and 0x7F.inv()) == 0) {
                os.write(v)
                return
            } else {
                os.write((v and 0x7F) or 0x80)
                v = v ushr 7
            }
        }
    }

    private fun readVarint(isStream: InputStream): Int {
        var result = 0
        var shift = 0
        while (shift < 32) {
            val b = isStream.read()
            if (b == -1) return -1
            result = result or ((b and 0x7F) shl shift)
            if ((b and 0x80) == 0) return result
            shift += 7
        }
        return result
    }

    private fun writeStringField(os: OutputStream, fieldNumber: Int, value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        writeTag(os, fieldNumber, 2)
        writeVarint(os, bytes.size)
        os.write(bytes)
    }
}
