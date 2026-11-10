package com.smarttv.remote.data.protocol

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey
import java.math.BigInteger
import javax.net.ssl.SSLSocket

sealed class PairingResult {
    object CodeDisplayedOnTv : PairingResult()
    object Success : PairingResult()
    data class Error(val message: String) : PairingResult()
}

/**
 * Android TV Remote Protocol v2 pairing (TV port 6467).
 *
 * Flow: PairingRequest -> Options -> Configuration -> (TV shows a 6 character hex code)
 * -> Secret = SHA-256(clientModulus, clientExponent, serverModulus, serverExponent, code[1..]).
 */
class PairingManager {

    companion object {
        private const val STATUS_OK = 200
        private const val STATUS_BAD_SECRET = 402

        private const val ENCODING_HEXADECIMAL = 3
        private const val CODE_LENGTH = 6
        private const val ROLE_INPUT = 1
    }

    private var socket: SSLSocket? = null
    private var serverCert: X509Certificate? = null
    private var clientCert: X509Certificate? = null

    suspend fun startPairing(
        host: String,
        port: Int = 6467,
        cert: X509Certificate,
        key: PrivateKey,
        deviceName: String = "Smart TV Remote"
    ): PairingResult = withContext(Dispatchers.IO) {
        cancelPairing()
        try {
            clientCert = cert
            val sock = TlsSocketFactory.createSslSocket(
                host, port, cert, key,
                connectTimeoutMs = 5000,
                readTimeoutMs = 15000
            )
            socket = sock

            val peer = sock.session.peerCertificates.firstOrNull() as? X509Certificate
                ?: return@withContext fail("The TV did not present a certificate.")
            serverCert = peer

            val out = sock.outputStream
            val input = sock.inputStream

            // 1. PairingRequest
            send(out) {
                message(10) {
                    string(1, "androidtvremote2")
                    string(2, deviceName)
                }
            }
            readOk(input, "pairing request")

            // 2. Options (we can enter a 6 digit hexadecimal code)
            send(out) {
                message(20) {
                    message(1) {
                        int(1, ENCODING_HEXADECIMAL)
                        int(2, CODE_LENGTH)
                    }
                    int(3, ROLE_INPUT)
                }
            }
            readOk(input, "options")

            // 3. Configuration -> TV now shows the code on screen
            send(out) {
                message(30) {
                    message(1) {
                        int(1, ENCODING_HEXADECIMAL)
                        int(2, CODE_LENGTH)
                    }
                    int(2, ROLE_INPUT)
                }
            }
            readOk(input, "configuration")

            PairingResult.CodeDisplayedOnTv
        } catch (e: Exception) {
            fail(describe(e))
        }
    }

    suspend fun submitPairingCode(code: String): PairingResult = withContext(Dispatchers.IO) {
        val sock = socket ?: return@withContext PairingResult.Error("Pairing session expired. Please go back and try again.")
        val sCert = serverCert ?: return@withContext PairingResult.Error("TV certificate missing. Please try again.")
        val cCert = clientCert ?: return@withContext PairingResult.Error("Client certificate missing. Please try again.")

        val normalized = code.trim().uppercase()
        if (normalized.length != CODE_LENGTH || !normalized.all { it in '0'..'9' || it in 'A'..'F' }) {
            return@withContext PairingResult.Error("Enter the 6 character code shown on your TV (0-9, A-F).")
        }

        try {
            val codeBytes = hexToBytes(normalized)
            val secret = calculateSecret(cCert, sCert, codeBytes)

            // The first byte of the code is a checksum of the secret; a mismatch means a typo.
            if (secret[0] != codeBytes[0]) {
                return@withContext PairingResult.Error("That code is not correct. Check the code on your TV and try again.")
            }

            send(sock.outputStream) {
                message(40) { bytes(1, secret) }
            }

            val reply = ProtoReader.parse(Framing.read(sock.inputStream))
            val status = ProtoReader.find(reply, 2)?.varint?.toInt() ?: STATUS_OK
            cancelPairing()
            when (status) {
                STATUS_OK -> PairingResult.Success
                STATUS_BAD_SECRET -> PairingResult.Error("The TV rejected the code. Please try again.")
                else -> PairingResult.Error("Pairing failed (TV status $status).")
            }
        } catch (e: Exception) {
            cancelPairing()
            fail(describe(e))
        }
    }

    fun cancelPairing() {
        try {
            socket?.close()
        } catch (_: Exception) {
        }
        socket = null
    }

    private fun fail(message: String): PairingResult.Error {
        cancelPairing()
        return PairingResult.Error(message)
    }

    private fun describe(e: Exception): String = when (e) {
        is SocketTimeoutException, is ConnectException, is NoRouteToHostException, is UnknownHostException ->
            "Can't reach the TV. Turn the TV on and make sure it is on the same Wi-Fi."
        is IOException -> "Pairing connection failed: ${e.message ?: "unknown error"}"
        else -> e.message ?: "Pairing failed."
    }

    private fun send(out: java.io.OutputStream, block: ProtoWriter.() -> Unit) {
        val writer = ProtoWriter().int(1, 2).int(2, STATUS_OK)
        writer.block()
        Framing.write(out, writer.toByteArray())
    }

    private fun readOk(input: java.io.InputStream, step: String) {
        val fields = ProtoReader.parse(Framing.read(input))
        val status = ProtoReader.find(fields, 2)?.varint?.toInt() ?: STATUS_OK
        if (status != STATUS_OK) {
            throw IOException("TV refused $step (status $status)")
        }
    }

    private fun calculateSecret(client: X509Certificate, server: X509Certificate, codeBytes: ByteArray): ByteArray {
        val clientKey = client.publicKey as RSAPublicKey
        val serverKey = server.publicKey as RSAPublicKey
        val md = MessageDigest.getInstance("SHA-256")
        md.update(numberBytes(clientKey.modulus))
        md.update(numberBytes(clientKey.publicExponent))
        md.update(numberBytes(serverKey.modulus))
        md.update(numberBytes(serverKey.publicExponent))
        md.update(codeBytes, 1, codeBytes.size - 1)
        return md.digest()
    }

    private fun numberBytes(n: BigInteger): ByteArray {
        var hex = n.toString(16)
        if (hex.length % 2 != 0) hex = "0$hex"
        return hexToBytes(hex)
    }

    private fun hexToBytes(hex: String): ByteArray {
        val out = ByteArray(hex.length / 2)
        for (i in out.indices) {
            out[i] = hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return out
    }
}
