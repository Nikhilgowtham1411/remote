package com.smarttv.remote.data.protocol

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CastManager(private val context: Context) {

    companion object {
        private const val TAG = "CastManager"
    }

    data class CastRoute(val id: String, val name: String, val description: String?)

    private val mediaRouter: MediaRouter? = try { MediaRouter.getInstance(context) } catch (e: Exception) { null }
    private val selector = MediaRouteSelector.Builder()
        .addControlCategory(CastMediaControlIntent.categoryForCast(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID))
        .build()
    private val routeCallback = object : MediaRouter.Callback() {
        override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) = refreshRoutes()
        override fun onRouteRemoved(router: MediaRouter, route: MediaRouter.RouteInfo) = refreshRoutes()
        override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) = refreshRoutes()
    }
    private var scanning = false
    private val mediaServer = LocalMediaServer(context)

    private val _routes = MutableStateFlow<List<CastRoute>>(emptyList())
    val routes: StateFlow<List<CastRoute>> = _routes.asStateFlow()

    private val _castStatus = MutableStateFlow<String?>(null)
    val castStatus: StateFlow<String?> = _castStatus.asStateFlow()

    /** Starts looking for Cast devices (the TV) on the Wi-Fi. Call from the main thread. */
    fun startScan() {
        val router = mediaRouter ?: return
        if (!scanning) {
            router.addCallback(selector, routeCallback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN)
            scanning = true
        }
        refreshRoutes()
    }

    fun stopScan() {
        val router = mediaRouter ?: return
        if (scanning) {
            router.removeCallback(routeCallback)
            scanning = false
        }
    }

    private fun refreshRoutes() {
        val router = mediaRouter ?: return
        _routes.value = router.routes
            .filter { !it.isDefault && !it.isBluetooth && it.matchesSelector(selector) }
            .map { CastRoute(it.id, it.name, it.description) }
    }

    /** Picking a route makes the Cast framework open a session to that TV. */
    fun selectRoute(id: String) {
        val router = mediaRouter ?: return
        val route = router.routes.firstOrNull { it.id == id } ?: return
        _castStatus.value = "Connecting to ${route.name}..."
        router.selectRoute(route)
    }

    /** Serves a file from this phone and plays it on the TV. Returns an error text or null. */
    fun castLocalFile(uri: Uri, mimeType: String): String? {
        val client = currentCastSession?.remoteMediaClient
            ?: return "Choose your TV first."
        val url = try {
            mediaServer.serve(uri, mimeType)
        } catch (e: Exception) {
            return "Could not share the file: ${e.message}"
        } ?: return "Could not find this phone's Wi-Fi address."
        castMedia(url = url, title = mediaServer.displayName(uri), mimeType = mimeType)
        _castStatus.value = "Playing on ${_castDeviceName.value ?: "TV"}"
        return null
    }

    private var castContext: CastContext? = null
    private var currentCastSession: CastSession? = null

    private val _isCastConnected = MutableStateFlow(false)
    val isCastConnected: StateFlow<Boolean> = _isCastConnected.asStateFlow()

    private val _castDeviceName = MutableStateFlow<String?>(null)
    val castDeviceName: StateFlow<String?> = _castDeviceName.asStateFlow()

    private val sessionManagerListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(session: CastSession) {}
        override fun onSessionStarted(session: CastSession, sessionId: String) {
            currentCastSession = session
            _isCastConnected.value = true
            _castDeviceName.value = session.castDevice?.friendlyName ?: "Google Cast Receiver"
            _castStatus.value = "Connected to ${_castDeviceName.value}"
            Log.d(TAG, "Cast session started with ${session.castDevice?.friendlyName}")
        }
        override fun onSessionStartFailed(session: CastSession, error: Int) {
            _castStatus.value = "Could not connect to the TV for casting."
            currentCastSession = null
            _isCastConnected.value = false
            _castDeviceName.value = null
        }
        override fun onSessionEnding(session: CastSession) {}
        override fun onSessionEnded(session: CastSession, error: Int) {
            currentCastSession = null
            _isCastConnected.value = false
            _castDeviceName.value = null
        }
        override fun onSessionResuming(session: CastSession, sessionId: String) {}
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            currentCastSession = session
            _isCastConnected.value = true
            _castDeviceName.value = session.castDevice?.friendlyName
        }
        override fun onSessionResumeFailed(session: CastSession, error: Int) {
            currentCastSession = null
            _isCastConnected.value = false
        }
        override fun onSessionSuspended(session: CastSession, reason: Int) {}
    }

    fun initialize() {
        try {
            castContext = CastContext.getSharedInstance(context)
            castContext?.sessionManager?.addSessionManagerListener(sessionManagerListener, CastSession::class.java)
            currentCastSession = castContext?.sessionManager?.currentCastSession
            _isCastConnected.value = currentCastSession?.isConnected == true
            _castDeviceName.value = currentCastSession?.castDevice?.friendlyName
        } catch (e: Exception) {
            Log.w(TAG, "CastContext init failed: ${e.message}")
        }
    }

    fun castMedia(
        url: String,
        title: String,
        subtitle: String = "",
        mimeType: String = "video/mp4",
        imageUrl: String? = null
    ) {
        val remoteMediaClient = currentCastSession?.remoteMediaClient ?: return

        val metadata = MediaMetadata(
            if (mimeType.startsWith("image/")) MediaMetadata.MEDIA_TYPE_PHOTO
            else if (mimeType.startsWith("audio/")) MediaMetadata.MEDIA_TYPE_MUSIC_TRACK
            else MediaMetadata.MEDIA_TYPE_MOVIE
        ).apply {
            putString(MediaMetadata.KEY_TITLE, title)
            putString(MediaMetadata.KEY_SUBTITLE, subtitle)
            imageUrl?.let { addImage(WebImage(Uri.parse(it))) }
        }

        val mediaInfo = MediaInfo.Builder(url)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(mimeType)
            .setMetadata(metadata)
            .build()

        val requestData = MediaLoadRequestData.Builder()
            .setMediaInfo(mediaInfo)
            .setAutoplay(true)
            .build()

        remoteMediaClient.load(requestData)
    }

    fun play() {
        currentCastSession?.remoteMediaClient?.play()
    }

    fun pause() {
        currentCastSession?.remoteMediaClient?.pause()
    }

    fun seek(positionMs: Long) {
        currentCastSession?.remoteMediaClient?.seek(positionMs)
    }

    fun disconnect() {
        castContext?.sessionManager?.endCurrentSession(true)
        mediaServer.stop()
        _castStatus.value = null
    }
}
