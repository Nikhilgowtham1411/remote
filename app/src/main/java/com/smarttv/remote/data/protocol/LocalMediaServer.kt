package com.smarttv.remote.data.protocol

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

/**
 * Tiny HTTP server that lets the TV (Cast receiver) fetch a photo/video/song straight from this
 * phone over Wi-Fi. Supports Range requests so videos can be seeked.
 */
class LocalMediaServer(private val context: Context) {

    companion object {
        private const val TAG = "LocalMediaServer"
        const val PORT = 8765
    }

    @Volatile private var server: ServerSocket? = null
    @Volatile private var current: Uri? = null
    @Volatile private var mime: String = "application/octet-stream"
    private val pool = Executors.newCachedThreadPool()

    fun serve(uri: Uri, mimeType: String): String? {
        current = uri
        mime = mimeType
        ensureStarted()
        val host = hostAddress() ?: return null
        // The counter makes the TV refetch when the file changes.
        return "http://$host:$PORT/media/${System.currentTimeMillis()}"
    }

    fun stop() {
        try { server?.close() } catch (_: Exception) {}
        server = null
    }

    fun displayName(uri: Uri): String {
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) return c.getString(0) ?: "Media"
            }
        } catch (_: Exception) {
        }
        return uri.lastPathSegment ?: "Media"
    }

    @Synchronized
    private fun ensureStarted() {
        if (server?.isClosed == false) return
        val s = ServerSocket(PORT)
        s.reuseAddress = true
        server = s
        pool.execute {
            while (!s.isClosed) {
                try {
                    val client = s.accept()
                    pool.execute { handle(client) }
                } catch (_: Exception) {
                    break
                }
            }
        }
    }

    private fun hostAddress(): String? {
        return try {
            java.net.NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList() }
                .filterIsInstance<java.net.Inet4Address>()
                .firstOrNull { it.isSiteLocalAddress }
                ?.hostAddress
        } catch (e: Exception) {
            null
        }
    }

    private fun handle(socket: Socket) {
        socket.use { sock ->
            try {
                sock.soTimeout = 15000
                val input = BufferedInputStream(sock.getInputStream())
                val headers = readHeaders(input)
                val first = headers.firstOrNull() ?: return
                val isHead = first.startsWith("HEAD")
                val range = headers.firstOrNull { it.startsWith("range:", true) }
                    ?.substringAfter(":")?.trim()

                val uri = current
                val out = sock.getOutputStream()
                if (uri == null) {
                    out.write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n".toByteArray())
                    return
                }

                val total = try {
                    context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
                } catch (_: Exception) {
                    -1L
                }

                var start = 0L
                var end = if (total > 0) total - 1 else -1L
                var partial = false
                if (range != null && range.startsWith("bytes=") && total > 0) {
                    val spec = range.removePrefix("bytes=").split("-")
                    start = spec.getOrNull(0)?.toLongOrNull() ?: 0L
                    end = spec.getOrNull(1)?.toLongOrNull() ?: (total - 1)
                    if (end >= total) end = total - 1
                    partial = true
                }

                val sb = StringBuilder()
                sb.append(if (partial) "HTTP/1.1 206 Partial Content\r\n" else "HTTP/1.1 200 OK\r\n")
                sb.append("Content-Type: $mime\r\n")
                sb.append("Accept-Ranges: bytes\r\n")
                sb.append("Access-Control-Allow-Origin: *\r\n")
                sb.append("Access-Control-Allow-Headers: Range, Content-Type\r\n")
                sb.append("Access-Control-Expose-Headers: Content-Range, Content-Length, Accept-Ranges\r\n")
                if (end >= start && total > 0) {
                    sb.append("Content-Length: ${end - start + 1}\r\n")
                    if (partial) sb.append("Content-Range: bytes $start-$end/$total\r\n")
                }
                sb.append("Connection: close\r\n\r\n")
                out.write(sb.toString().toByteArray())
                out.flush()
                if (isHead) return

                context.contentResolver.openInputStream(uri)?.use { stream ->
                    skipFully(stream, start)
                    copy(stream, out, if (end >= start && total > 0) end - start + 1 else Long.MAX_VALUE)
                }
            } catch (e: Exception) {
                Log.d(TAG, "Request ended: ${e.message}")
            }
        }
    }

    private fun readHeaders(input: InputStream): List<String> {
        val lines = mutableListOf<String>()
        val line = StringBuilder()
        var prev = -1
        while (true) {
            val b = input.read()
            if (b < 0) break
            if (b == '\n'.code) {
                val text = line.toString().trimEnd('\r')
                if (text.isEmpty()) break
                lines += text
                line.setLength(0)
            } else {
                line.append(b.toChar())
            }
            prev = b
            if (lines.size > 60 || line.length > 8192) break
        }
        return lines
    }

    private fun skipFully(stream: InputStream, count: Long) {
        var left = count
        while (left > 0) {
            val n = stream.skip(left)
            if (n <= 0) {
                if (stream.read() < 0) return
                left--
            } else {
                left -= n
            }
        }
    }

    private fun copy(input: InputStream, out: OutputStream, limit: Long) {
        val buf = ByteArray(64 * 1024)
        var left = limit
        while (left > 0) {
            val n = input.read(buf, 0, minOf(buf.size.toLong(), left).toInt())
            if (n < 0) break
            out.write(buf, 0, n)
            left -= n
        }
        out.flush()
    }
}
