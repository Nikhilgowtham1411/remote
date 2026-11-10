package com.smarttv.remote.data.protocol

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.net.ssl.SSLSocket

sealed class PairingResult {
    object CodeDisplayedOnTv : PairingResult()
    object Success : PairingResult()
    data class Error(val message: String) : PairingResult()
}

class PairingManager {

    private var activeSocket: SSLSocket? = null
    private var serverCert: X509Certificate? = null
    private var clientCert: X509Certificate? = null
    private var clientKey: PrivateKey? = null

    suspend fun startPairing(
        host: String,
        port: Int = 6467,
        cert: X509Certificate,
        key: PrivateKey,
        deviceName: String = "SmartTVRemote"
    ): PairingResult = withContext(Dispatchers.IO) {
        try {
            clientCert = cert
            clientKey = key
            val socket = TlsSocketFactory.createSslSocket(host, port, cert, key, timeoutMs = 8000)
            activeSocket = socket

            // Retrieve server certificate from TLS handshake
            val peerCerts = socket.session.peerCertificates
            if (peerCerts.isNotEmpty() && peerCerts[0] is X509Certificate) {
                serverCert = peerCerts[0] as X509Certificate
            }

            val out = socket.getOutputStream()
            val inStream = socket.getInputStream()

            // 1. Send PairingRequest
            val pairingReqBytes = buildPairingRequestPacket(deviceName)
            writeLengthDelimited(out, pairingReqBytes)

            // 2. Read PairingRequestAck
            val ack1 = readLengthDelimited(inStream)
            if (ack1.isEmpty()) {
                return@withContext PairingResult.Error("No response from TV during pairing request.")
            }

            // 3. Send PairingConfiguration (Request ALPHANUMERIC / 4-6 digit PIN)
            val configBytes = buildPairingConfigurationPacket()
            writeLengthDelimited(out, configBytes)

            // 4. Read PairingConfigurationAck
            val ack2 = readLengthDelimited(inStream)
            if (ack2.isEmpty()) {
                return@withContext PairingResult.Error("Pairing configuration rejected by TV.")
            }

            // TV is now displaying the pairing code on screen!
            return@withContext PairingResult.CodeDisplayedOnTv
        } catch (e: Exception) {
            activeSocket?.close()
            activeSocket = null
            return@withContext PairingResult.Error(e.message ?: "Failed to initiate pairing.")
        }
    }

    suspend fun submitPairingCode(pin: String): PairingResult = withContext(Dispatchers.IO) {
        val socket = activeSocket ?: return@withContext PairingResult.Error("Pairing session expired. Please retry.")
        val sCert = serverCert ?: return@withContext PairingResult.Error("Server certificate missing.")
        val cCert = clientCert ?: return@withContext PairingResult.Error("Client certificate missing.")

        try {
            val out = socket.getOutputStream()
            val inStream = socket.getInputStream()

            // Calculate Secret hash: SHA-256(ClientCert + ServerCert + PIN)
            val secretHash = calculateSecretHash(cCert, sCert, pin)

            // Send PairingSecret packet
            val secretPacket = buildPairingSecretPacket(secretHash)
            writeLengthDelimited(out, secretPacket)

            // Read PairingSecretAck
            val secretAck = readLengthDelimited(inStream)
            if (secretAck.isNotEmpty()) {
                // TV accepted pairing!
                socket.close()
                activeSocket = null
                return@withContext PairingResult.Success
            } else {
                return@withContext PairingResult.Error("Incorrect pairing code. Please check TV screen and retry.")
            }
        } catch (e: Exception) {
            socket.close()
            activeSocket = null
            return@withContext PairingResult.Error(e.message ?: "Pairing verification failed.")
        }
    }

    fun cancelPairing() {
        try {
            activeSocket?.close()
        } catch (_: Exception) {}
        activeSocket = null
    }

    private fun calculateSecretHash(
        clientCert: X509Certificate,
        serverCert: X509Certificate,
        pin: String
    ): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(clientCert.publicKey.encoded)
        md.update(serverCert.publicKey.encoded)
        // Clean and uppercase PIN
        val normalizedPin = pin.trim().uppercase()
        // If 4-digit numeric, add alpha check
        md.update(normalizedPin.toByteArray(Charsets.UTF_8))
        return md.digest()
    }

    private fun buildPairingRequestPacket(deviceName: String): ByteArray {
        val baos = ByteArrayOutputStream()
        // Protocol Version = 2
        baos.write(byteArrayOf(0x08, 0x02)) // field 1: int32 = 2
        // Status = STATUS_OK (200)
        baos.write(byteArrayOf(0x10, 0xC8.toByte(), 0x01)) // field 2: int32 = 200
        // Submessage: PairingRequest (field 10)
        val sub = ByteArrayOutputStream()
        // service_name = "androidtvremote" (field 1)
        writeStringField(sub, 1, "androidtvremote")
        // client_name = deviceName (field 2)
        writeStringField(sub, 2, deviceName)
        val subBytes = sub.toByteArray()
        writeTag(baos, 10, 2)
        writeVarint(baos, subBytes.size)
        baos.write(subBytes)
        return baos.toByteArray()
    }

    private fun buildPairingConfigurationPacket(): ByteArray {
        val baos = ByteArrayOutputStream()
        baos.write(byteArrayOf(0x08, 0x02)) // Protocol Version = 2
        baos.write(byteArrayOf(0x10, 0xC8.toByte(), 0x01)) // Status = 200
        val sub = ByteArrayOutputStream()
        // client_role = ROLE_CONTROLLER (1)
        sub.write(byteArrayOf(0x08, 0x01))
        // encoding = ENCODING_ALPHANUMERIC (2)
        sub.write(byteArrayOf(0x10, 0x02))
        val subBytes = sub.toByteArray()
        writeTag(baos, 20, 2)
        writeVarint(baos, subBytes.size)
        baos.write(subBytes)
        return baos.toByteArray()
    }

    private fun buildPairingSecretPacket(secretHash: ByteArray): ByteArray {
        val baos = ByteArrayOutputStream()
        baos.write(byteArrayOf(0x08, 0x02)) // Protocol Version = 2
        baos.write(byteArrayOf(0x10, 0xC8.toByte(), 0x01)) // Status = 200
        val sub = ByteArrayOutputStream()
        // secret bytes (field 1)
        writeTag(sub, 1, 2)
        writeVarint(sub, secretHash.size)
        sub.write(secretHash)
        val subBytes = sub.toByteArray()
        writeTag(baos, 30, 2)
        writeVarint(baos, subBytes.size)
        baos.write(subBytes)
        return baos.toByteArray()
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
