package com.smarttv.remote.data.protocol

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import java.io.ByteArrayOutputStream

/**
 * Captures the phone screen, encodes it as H.264, packs it into a live HLS stream and
 * serves it on the Wi-Fi so the TV (via Cast) can play it. Video only, a few seconds delay.
 */
class ScreenMirrorService : Service() {

    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var codec: MediaCodec? = null
    private var hls: HlsServer? = null
    @Volatile private var running = false
    private var worker: Thread? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (running) return START_NOT_STICKY
        MirrorHub.reset()
        startAsForeground()
        val code = intent?.getIntExtra("code", 0) ?: 0
        @Suppress("DEPRECATION")
        val data = intent?.getParcelableExtra<Intent>("data")
        if (data == null) {
            MirrorHub.fail("Screen capture was not allowed.")
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            begin(code, data)
        } catch (e: Exception) {
            MirrorHub.fail("Could not start screen mirroring: ${e.message}")
            shutdown()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startAsForeground() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel("mirror", "Screen mirroring", NotificationManager.IMPORTANCE_LOW))
        val n: Notification = NotificationCompat.Builder(this, "mirror")
            .setContentTitle("Mirroring your screen to the TV")
            .setContentText("Open Smart TV Remote and tap Stop to end.")
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setOngoing(true)
            .build()
        ServiceCompat.startForeground(this, 42, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
    }

    private fun begin(resultCode: Int, data: Intent) {
        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val proj = mpm.getMediaProjection(resultCode, data) ?: throw IllegalStateException("no projection")
        projection = proj
        proj.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                shutdown()
                stopSelf()
            }
        }, Handler(Looper.getMainLooper()))

        val metrics = resources.displayMetrics
        val scale = 1280f / maxOf(metrics.widthPixels, metrics.heightPixels)
        val w = (metrics.widthPixels * scale).toInt() / 2 * 2
        val h = (metrics.heightPixels * scale).toInt() / 2 * 2

        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, w, h).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, 4_000_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 150_000L)
        }
        val enc = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        enc.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val surface = enc.createInputSurface()
        enc.start()
        codec = enc
        display = proj.createVirtualDisplay(
            "tv-mirror", w, h, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, surface, null, null
        )

        val server = HlsServer()
        server.start()
        hls = server
        val host = LocalMediaServer(this).hostAddress()
        if (host == null) throw IllegalStateException("no Wi-Fi address")

        running = true
        worker = Thread({ encodeLoop(enc, server, host) }, "mirror-encode").also { it.start() }
    }

    private fun encodeLoop(enc: MediaCodec, server: HlsServer, host: String) {
        val ts = TsWriter()
        val info = MediaCodec.BufferInfo()
        val seg = ByteArrayOutputStream()
        var csd: ByteArray? = null
        var firstUs = -1L
        var segStart = 0L
        var published = false
        val aud = byteArrayOf(0, 0, 0, 1, 0x09, 0xF0.toByte())
        try {
            while (running) {
                val idx = enc.dequeueOutputBuffer(info, 100_000)
                if (idx < 0) continue
                val buf = enc.getOutputBuffer(idx)
                if (buf != null && info.size > 0) {
                    val bytes = ByteArray(info.size)
                    buf.position(info.offset)
                    buf.get(bytes, 0, info.size)
                    if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        csd = bytes
                    } else {
                        val key = info.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0
                        if (firstUs < 0) firstUs = info.presentationTimeUs
                        val pts = (info.presentationTimeUs - firstUs) * 9 / 100 + 90000
                        if (key && seg.size() > 0 && pts - segStart >= 72000) {
                            server.addSegment((pts - segStart) / 90000.0, seg.toByteArray())
                            seg.reset()
                            if (!published && server.segmentCount >= 2) {
                                published = true
                                MirrorHub.publish("http://$host:8766/s${System.currentTimeMillis()}/live.m3u8")
                            }
                        }
                        if (seg.size() == 0) {
                            if (!key) { enc.releaseOutputBuffer(idx, false); continue }
                            segStart = pts
                            ts.writePatPmt(seg)
                        }
                        val au = ByteArrayOutputStream(bytes.size + 64)
                        au.write(aud)
                        if (key && csd != null) au.write(csd)
                        au.write(bytes)
                        ts.writeVideo(seg, pts, au.toByteArray(), key)
                    }
                }
                enc.releaseOutputBuffer(idx, false)
            }
        } catch (e: Exception) {
            if (running) MirrorHub.fail("Screen mirroring stopped: ${e.message}")
        }
    }

    private fun shutdown() {
        running = false
        try { display?.release() } catch (_: Exception) {}
        try { codec?.stop() } catch (_: Exception) {}
        try { codec?.release() } catch (_: Exception) {}
        try { projection?.stop() } catch (_: Exception) {}
        hls?.stop()
        display = null; codec = null; projection = null; hls = null
    }

    override fun onDestroy() {
        shutdown()
        super.onDestroy()
    }
}
