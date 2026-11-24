package com.smarttv.remote.data.protocol

import java.io.ByteArrayOutputStream
import java.io.BufferedOutputStream
import java.net.ServerSocket
import java.net.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Shared state between the mirror service and the UI. */
object MirrorHub {
    private val _url = MutableStateFlow<String?>(null)
    val url: StateFlow<String?> = _url.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun reset() { _url.value = null; _error.value = null }
    fun publish(url: String) { _url.value = url }
    fun fail(message: String) { _error.value = message }
}

/** Packs H.264 access units into MPEG-TS packets (video only). */
class TsWriter {
    private val cc = IntArray(8192)

    fun writePatPmt(out: ByteArrayOutputStream) {
        val pat = byteArrayOf(
            0x00, 0xB0.toByte(), 13, 0x00, 0x01, 0xC1.toByte(), 0x00, 0x00,
            0x00, 0x01, (0xE0 or (PMT_PID shr 8)).toByte(), (PMT_PID and 0xFF).toByte()
        )
        psi(out, 0, withCrc(pat))
        val pmt = byteArrayOf(
            0x02, 0xB0.toByte(), 18, 0x00, 0x01, 0xC1.toByte(), 0x00, 0x00,
            (0xE0 or (VIDEO_PID shr 8)).toByte(), (VIDEO_PID and 0xFF).toByte(), 0xF0.toByte(), 0x00,
            0x1B, (0xE0 or (VIDEO_PID shr 8)).toByte(), (VIDEO_PID and 0xFF).toByte(), 0xF0.toByte(), 0x00
        )
        psi(out, PMT_PID, withCrc(pmt))
    }

    private fun psi(out: ByteArrayOutputStream, pid: Int, section: ByteArray) {
        val pkt = ByteArray(188) { 0xFF.toByte() }
        pkt[0] = 0x47
        pkt[1] = (0x40 or (pid shr 8)).toByte()
        pkt[2] = (pid and 0xFF).toByte()
        pkt[3] = (0x10 or (cc[pid]++ and 0x0F)).toByte()
        pkt[4] = 0x00
        System.arraycopy(section, 0, pkt, 5, section.size)
        out.write(pkt)
    }

    private fun withCrc(section: ByteArray): ByteArray {
        var crc = -1
        for (b in section) {
            crc = crc xor ((b.toInt() and 0xFF) shl 24)
            repeat(8) { crc = if (crc < 0) (crc shl 1) xor 0x04C11DB7 else crc shl 1 }
        }
        return section + byteArrayOf((crc ushr 24).toByte(), (crc ushr 16).toByte(), (crc ushr 8).toByte(), crc.toByte())
    }

    /** One video access unit (Annex B) with a 90 kHz presentation timestamp. */
    fun writeVideo(out: ByteArrayOutputStream, pts: Long, es: ByteArray, keyFrame: Boolean) {
        val pes = ByteArrayOutputStream(es.size + 19)
        pes.write(byteArrayOf(0, 0, 1, 0xE0.toByte(), 0, 0, 0x80.toByte(), 0x80.toByte(), 5))
        pes.write(0x21 or (((pts shr 30) and 7).toInt() shl 1))
        pes.write(((pts shr 22) and 0xFF).toInt())
        pes.write((((pts shr 15) and 0x7F).toInt() shl 1) or 1)
        pes.write(((pts shr 7) and 0xFF).toInt())
        pes.write(((pts and 0x7F).toInt() shl 1) or 1)
        pes.write(es)
        val data = pes.toByteArray()

        var pos = 0
        var first = true
        while (pos < data.size) {
            val pkt = ByteArray(188)
            pkt[0] = 0x47
            pkt[1] = ((if (first) 0x40 else 0) or (VIDEO_PID shr 8)).toByte()
            pkt[2] = (VIDEO_PID and 0xFF).toByte()
            val remaining = data.size - pos
            val wantPcr = first && keyFrame
            val minAdapt = if (wantPcr) 8 else 0   // 1 length + 1 flags + 6 PCR
            var payload = minOf(remaining, 184 - minAdapt)
            // Adaptation field needed for PCR or to pad the last packet.
            val adaptLen = 184 - payload          // total bytes used by adaptation (incl. length byte), 0 = none
            if (adaptLen == 0) {
                pkt[3] = (0x10 or (cc[VIDEO_PID]++ and 0x0F)).toByte()
                System.arraycopy(data, pos, pkt, 4, payload)
            } else {
                pkt[3] = (0x30 or (cc[VIDEO_PID]++ and 0x0F)).toByte()
                pkt[4] = (adaptLen - 1).toByte()
                if (adaptLen >= 2) {
                    var flags = 0
                    if (wantPcr) flags = flags or 0x10
                    if (first && keyFrame) flags = flags or 0x40
                    pkt[5] = flags.toByte()
                    var i = 6
                    if (wantPcr) {
                        val base = maxOf(0L, pts - 9000)
                        pkt[6] = (base shr 25).toByte()
                        pkt[7] = (base shr 17).toByte()
                        pkt[8] = (base shr 9).toByte()
                        pkt[9] = (base shr 1).toByte()
                        pkt[10] = (((base and 1L).toInt() shl 7) or 0x7E).toByte()
                        pkt[11] = 0
                        i = 12
                    }
                    while (i < 4 + adaptLen) pkt[i++] = 0xFF.toByte()
                }
                System.arraycopy(data, pos, pkt, 4 + adaptLen, payload)
            }
            pos += payload
            first = false
            out.write(pkt)
        }
    }

    companion object {
        const val PMT_PID = 0x1000
        const val VIDEO_PID = 0x100
    }
}

/** Tiny HTTP server for a live HLS playlist (rolling window of in-memory .ts segments). */
class HlsServer(private val port: Int = 8766) {
    private class Segment(val index: Int, val duration: Double, val data: ByteArray)

    private val segments = ArrayDeque<Segment>()
    private var nextIndex = 0
    @Volatile private var server: ServerSocket? = null
    @Volatile private var running = false

    val segmentCount: Int
        @Synchronized get() = segments.size

    @Synchronized
    fun addSegment(durationSec: Double, data: ByteArray) {
        segments.addLast(Segment(nextIndex++, durationSec, data))
        while (segments.size > 6) segments.removeFirst()
    }

    fun start() {
        running = true
        val ss = ServerSocket(port).also { it.reuseAddress = true }
        server = ss
        Thread({
            while (running) {
                try {
                    val s = ss.accept()
                    Thread({ handle(s) }, "hls-client").start()
                } catch (_: Exception) {
                    break
                }
            }
        }, "hls-accept").start()
    }

    fun stop() {
        running = false
        try { server?.close() } catch (_: Exception) {}
        server = null
        synchronized(this) { segments.clear() }
    }

    private fun handle(sock: Socket) {
        sock.use { s ->
            try {
                s.soTimeout = 15000
                val reader = s.getInputStream().bufferedReader()
                val first = reader.readLine() ?: return
                while (true) { val l = reader.readLine() ?: break; if (l.isEmpty()) break }
                val path = first.split(" ").getOrNull(1) ?: return
                val name = path.substringAfterLast('/').substringBefore('?')
                val out = BufferedOutputStream(s.getOutputStream())
                val cors = "Access-Control-Allow-Origin: *\r\nAccess-Control-Allow-Headers: *\r\nCache-Control: no-cache\r\n"
                if (first.startsWith("OPTIONS")) {
                    out.write("HTTP/1.1 204 No Content\r\n${cors}Content-Length: 0\r\n\r\n".toByteArray())
                } else if (name.endsWith(".m3u8")) {
                    val body = playlist().toByteArray()
                    out.write("HTTP/1.1 200 OK\r\n${cors}Content-Type: application/vnd.apple.mpegurl\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n".toByteArray())
                    out.write(body)
                } else if (name.startsWith("seg") && name.endsWith(".ts")) {
                    val idx = name.removePrefix("seg").removeSuffix(".ts").toIntOrNull()
                    val seg = synchronized(this) { segments.firstOrNull { it.index == idx } }
                    if (seg == null) {
                        out.write("HTTP/1.1 404 Not Found\r\n${cors}Content-Length: 0\r\n\r\n".toByteArray())
                    } else {
                        out.write("HTTP/1.1 200 OK\r\n${cors}Content-Type: video/mp2t\r\nContent-Length: ${seg.data.size}\r\nConnection: close\r\n\r\n".toByteArray())
                        out.write(seg.data)
                    }
                } else {
                    out.write("HTTP/1.1 404 Not Found\r\n${cors}Content-Length: 0\r\n\r\n".toByteArray())
                }
                out.flush()
            } catch (_: Exception) {
            }
        }
    }

    @Synchronized
    private fun playlist(): String {
        val sb = StringBuilder("#EXTM3U\n#EXT-X-VERSION:3\n#EXT-X-TARGETDURATION:2\n")
        sb.append("#EXT-X-MEDIA-SEQUENCE:").append(segments.firstOrNull()?.index ?: 0).append('\n')
        for (seg in segments) {
            sb.append("#EXTINF:").append(String.format(java.util.Locale.US, "%.3f", seg.duration)).append(",\n")
            sb.append("seg").append(seg.index).append(".ts\n")
        }
        return sb.toString()
    }
}
