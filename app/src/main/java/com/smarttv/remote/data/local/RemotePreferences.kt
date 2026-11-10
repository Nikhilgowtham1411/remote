package com.smarttv.remote.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.smarttv.remote.data.model.SkipDurationConfig
import com.smarttv.remote.data.model.TvDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "smart_tv_remote_prefs")

class RemotePreferences(private val context: Context) {

    companion object {
        val KEY_PAIRED_DEVICE_ID = stringPreferencesKey("paired_device_id")
        val KEY_PAIRED_DEVICE_NAME = stringPreferencesKey("paired_device_name")
        val KEY_PAIRED_DEVICE_HOST = stringPreferencesKey("paired_device_host")
        val KEY_PAIRED_DEVICE_PORT = intPreferencesKey("paired_device_port")
        val KEY_PAIRED_DEVICE_MAC = stringPreferencesKey("paired_device_mac")
        val KEY_CERT_PEM = stringPreferencesKey("client_cert_pem")
        val KEY_KEY_PEM = stringPreferencesKey("client_key_pem")

        val KEY_SKIP_BACKWARD = intPreferencesKey("skip_backward_seconds")
        val KEY_SKIP_FORWARD = intPreferencesKey("skip_forward_seconds")
        val KEY_SKIP_CUSTOM = intPreferencesKey("skip_custom_seconds")

        val KEY_HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback_enabled")
        val KEY_SOUND_FEEDBACK = booleanPreferencesKey("sound_feedback_enabled")
        val KEY_EASY_MODE = booleanPreferencesKey("easy_mode_enabled")
        val KEY_FIRST_LAUNCH_DONE = booleanPreferencesKey("first_launch_done")
        val KEY_TMDB = stringPreferencesKey("tmdb_api_key")
    }

    val pairedDeviceFlow: Flow<TvDevice?> = context.dataStore.data.map { prefs ->
        val id = prefs[KEY_PAIRED_DEVICE_ID]
        val host = prefs[KEY_PAIRED_DEVICE_HOST]
        if (id != null && host != null) {
            TvDevice(
                id = id,
                name = prefs[KEY_PAIRED_DEVICE_NAME] ?: "Google TV",
                model = "Google TV",
                host = host,
                port = prefs[KEY_PAIRED_DEVICE_PORT] ?: 6466,
                macAddress = prefs[KEY_PAIRED_DEVICE_MAC],
                isPaired = true
            )
        } else {
            null
        }
    }

    val skipDurationConfigFlow: Flow<SkipDurationConfig> = context.dataStore.data.map { prefs ->
        SkipDurationConfig(
            backwardSeconds = prefs[KEY_SKIP_BACKWARD] ?: 10,
            forwardSeconds = prefs[KEY_SKIP_FORWARD] ?: 10,
            customDuration = prefs[KEY_SKIP_CUSTOM] ?: 45
        )
    }

    val hapticFeedbackFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_HAPTIC_FEEDBACK] ?: true
    }

    val easyModeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_EASY_MODE] ?: false
    }

    val isFirstLaunchDoneFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_FIRST_LAUNCH_DONE] ?: false
    }

    suspend fun savePairedDevice(device: TvDevice) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PAIRED_DEVICE_ID] = device.id
            prefs[KEY_PAIRED_DEVICE_NAME] = device.name
            prefs[KEY_PAIRED_DEVICE_HOST] = device.host
            prefs[KEY_PAIRED_DEVICE_PORT] = device.port
            device.macAddress?.let { prefs[KEY_PAIRED_DEVICE_MAC] = it }
        }
    }

    suspend fun clearPairedDevice() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_PAIRED_DEVICE_ID)
            prefs.remove(KEY_PAIRED_DEVICE_NAME)
            prefs.remove(KEY_PAIRED_DEVICE_HOST)
            prefs.remove(KEY_PAIRED_DEVICE_PORT)
            prefs.remove(KEY_PAIRED_DEVICE_MAC)
        }
    }

    suspend fun saveSkipDurations(backward: Int, forward: Int, custom: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SKIP_BACKWARD] = backward
            prefs[KEY_SKIP_FORWARD] = forward
            prefs[KEY_SKIP_CUSTOM] = custom
        }
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_HAPTIC_FEEDBACK] = enabled
        }
    }

    suspend fun setEasyMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_EASY_MODE] = enabled
        }
    }

    suspend fun setFirstLaunchDone() {
        context.dataStore.edit { prefs ->
            prefs[KEY_FIRST_LAUNCH_DONE] = true
        }
    }

    val tmdbKeyFlow: Flow<String> = context.dataStore.data.map { prefs -> prefs[KEY_TMDB] ?: "" }

    suspend fun saveTmdbKey(key: String) {
        context.dataStore.edit { prefs -> prefs[KEY_TMDB] = key.trim() }
    }

    suspend fun saveTlsCredentials(certPem: String, keyPem: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CERT_PEM] = certPem
            prefs[KEY_KEY_PEM] = keyPem
        }
    }

    val tlsCertFlow: Flow<Pair<String?, String?>> = context.dataStore.data.map { prefs ->
        Pair(prefs[KEY_CERT_PEM], prefs[KEY_KEY_PEM])
    }
}
