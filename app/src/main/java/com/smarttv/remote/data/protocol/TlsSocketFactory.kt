package com.smarttv.remote.data.protocol

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

    fun createSslSocket(
        host: String,
        port: Int,
        certificate: X509Certificate,
        privateKey: PrivateKey,
        timeoutMs: Int = 5000
    ): SSLSocket {
        // Create an in-memory KeyStore with the client certificate
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

        // Permissive TrustManager for TV self-signed certificate
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
        socket.connect(java.net.InetSocketAddress(host, port), timeoutMs)
        socket.soTimeout = 10000
        socket.startHandshake()
        return socket
    }
}
