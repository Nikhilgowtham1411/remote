package com.smarttv.remote.data.protocol

import java.net.InetSocketAddress
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object TlsSocketFactory {

    /**
     * Opens a mutual-TLS connection to the TV. The TV uses a self-signed certificate, so the
     * server certificate is accepted here and checked explicitly by the pairing secret instead.
     */
    fun createSslSocket(
        host: String,
        port: Int,
        certificate: X509Certificate,
        privateKey: PrivateKey,
        connectTimeoutMs: Int = 5000,
        readTimeoutMs: Int = 10000
    ): SSLSocket {
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
            setKeyEntry(
                "client_key",
                privateKey,
                "password".toCharArray(),
                arrayOf(certificate)
            )
        }

        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
            init(keyStore, "password".toCharArray())
        }

        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
        )

        val sslContext = SSLContext.getInstance("TLSv1.2").apply {
            init(kmf.keyManagers, trustAllCerts, SecureRandom())
        }

        val socket = sslContext.socketFactory.createSocket() as SSLSocket
        try {
            socket.tcpNoDelay = true
            socket.connect(InetSocketAddress(host, port), connectTimeoutMs)
            socket.soTimeout = readTimeoutMs
            socket.startHandshake()
        } catch (e: Exception) {
            try { socket.close() } catch (_: Exception) {}
            throw e
        }
        return socket
    }
}
