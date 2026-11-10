package com.smarttv.remote.data.protocol

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemReader
import org.bouncycastle.util.io.pem.PemWriter
import java.io.ByteArrayInputStream
import java.io.StringReader
import java.io.StringWriter
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Date

object CertUtils {

    data class KeyCertPair(
        val certificate: X509Certificate,
        val privateKey: PrivateKey,
        val certPem: String,
        val keyPem: String
    )

    fun generateSelfSignedCertificate(): KeyCertPair {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048)
        val keyPair: KeyPair = keyGen.generateKeyPair()

        val notBefore = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L)
        val notAfter = Date(System.currentTimeMillis() + 10L * 365 * 24 * 60 * 60 * 1000L) // 10 years

        val subject = X500Name("CN=SmartTVRemote")
        val serialNumber = BigInteger.valueOf(System.currentTimeMillis())

        val certBuilder = JcaX509v3CertificateBuilder(
            subject,
            serialNumber,
            notBefore,
            notAfter,
            subject,
            keyPair.public
        )

        val signer = JcaContentSignerBuilder("SHA256WithRSAEncryption").build(keyPair.private)
        val certHolder = certBuilder.build(signer)
        val cert = JcaX509CertificateConverter().getCertificate(certHolder)

        val certPem = toPemString("CERTIFICATE", cert.encoded)
        val keyPem = toPemString("PRIVATE KEY", keyPair.private.encoded)

        return KeyCertPair(cert, keyPair.private, certPem, keyPem)
    }

    private fun toPemString(type: String, bytes: ByteArray): String {
        val sw = StringWriter()
        PemWriter(sw).use { writer ->
            writer.writeObject(PemObject(type, bytes))
        }
        return sw.toString()
    }

    fun parseCertificatePem(pem: String): X509Certificate {
        PemReader(StringReader(pem)).use { reader ->
            val pemObject = reader.readPemObject()
            val cf = CertificateFactory.getInstance("X.509")
            return cf.generateCertificate(ByteArrayInputStream(pemObject.content)) as X509Certificate
        }
    }

    fun parsePrivateKeyPem(pem: String): PrivateKey {
        PemReader(StringReader(pem)).use { reader ->
            val pemObject = reader.readPemObject()
            val spec = PKCS8EncodedKeySpec(pemObject.content)
            val kf = KeyFactory.getInstance("RSA")
            return kf.generatePrivate(spec)
        }
    }
}
