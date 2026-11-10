package com.smarttv.remote.data.protocol

import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** Minimal protobuf encoder (enough for the Android TV Remote v2 messages). */
class ProtoWriter {
    private val out = ByteArrayOutputStream()

    fun int(field: Int, value: Int): ProtoWriter {
        tag(field, 0)
        rawVarint(value.toLong() and 0xFFFFFFFFL)
        return this
    }

    fun bytes(field: Int, data: ByteArray): ProtoWriter {
        tag(field, 2)
        rawVarint(data.size.toLong())
        out.write(data)
        return this
    }

    fun string(field: Int, value: String): ProtoWriter = bytes(field, value.toByteArray(Charsets.UTF_8))

    fun message(field: Int, block: ProtoWriter.() -> Unit): ProtoWriter =
        bytes(field, ProtoWriter().apply(block).toByteArray())

    fun toByteArray(): ByteArray = out.toByteArray()

    private fun tag(field: Int, wireType: Int) = rawVarint(((field shl 3) or wireType).toLong())

    private fun rawVarint(value: Long) {
        var v = value
        while (true) {
            if ((v and 0x7FL.inv()) == 0L) {
                out.write(v.toInt())
                return
            }
            out.write(((v and 0x7FL) or 0x80L).toInt())
            v = v ushr 7
        }
    }
}

class ProtoField(val number: Int, val wireType: Int, val varint: Long, val bytes: ByteArray?)

/** Minimal protobuf decoder. */
object ProtoReader {

    fun parse(data: ByteArray): List<ProtoField> {
        val result = mutableListOf<ProtoField>()
        var i = 0

        fun readVarint(): Long {
            var r = 0L
            var shift = 0
            while (true) {
                if (i >= data.size) throw IOException("Truncated protobuf message")
                val b = data[i++].toInt() and 0xFF
                r = r or ((b and 0x7F).toLong() shl shift)
                if ((b and 0x80) == 0) return r
                shift += 7
                if (shift > 63) throw IOException("Bad varint")
            }
        }

        while (i < data.size) {
            val key = readVarint().toInt()
            val number = key ushr 3
            when (val wireType = key and 7) {
                0 -> result += ProtoField(number, wireType, readVarint(), null)
                2 -> {
                    val len = readVarint().toInt()
                    if (len < 0 || i + len > data.size) throw IOException("Bad length in protobuf message")
                    result += ProtoField(number, wireType, 0L, data.copyOfRange(i, i + len))
                    i += len
                }
                1 -> {
                    i += 8
                    result += ProtoField(number, wireType, 0L, null)
                }
                5 -> {
                    i += 4
                    result += ProtoField(number, wireType, 0L, null)
                }
                else -> throw IOException("Unsupported protobuf wire type $wireType")
            }
        }
        return result
    }

    fun find(fields: List<ProtoField>, number: Int): ProtoField? = fields.firstOrNull { it.number == number }
}

/** Varint length-prefixed framing used by both TV ports (6467 pairing, 6466 remote). */
object Framing {

    fun write(out: OutputStream, data: ByteArray) {
        val header = ByteArrayOutputStream()
        var v = data.size
        while (true) {
            if ((v and 0x7F.inv()) == 0) {
                header.write(v)
                break
            }
            header.write((v and 0x7F) or 0x80)
            v = v ushr 7
        }
        out.write(header.toByteArray())
        out.write(data)
        out.flush()
    }

    /** Reads one frame. Throws [EOFException] if the TV closed the connection. */
    fun read(input: InputStream): ByteArray {
        var length = 0
        var shift = 0
        while (true) {
            val b = input.read()
            if (b < 0) throw EOFException("Connection closed by TV")
            length = length or ((b and 0x7F) shl shift)
            if ((b and 0x80) == 0) break
            shift += 7
            if (shift > 28) throw IOException("Bad frame length")
        }
        if (length < 0 || length > 1_000_000) throw IOException("Bad frame length")
        val buffer = ByteArray(length)
        var total = 0
        while (total < length) {
            val n = input.read(buffer, total, length - total)
            if (n < 0) throw EOFException("Connection closed by TV")
            total += n
        }
        return buffer
    }
}
