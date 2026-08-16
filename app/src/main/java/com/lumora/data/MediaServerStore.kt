package com.lumora.data

import android.content.SharedPreferences
import com.lumora.model.MediaServerConfig
import com.lumora.security.SecureValueStore
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Persists Jellyfin/Plex account metadata while encrypting connection details and tokens with an
 * AndroidKeyStore-backed AES-GCM key. Legacy plaintext values are migrated on first successful
 * read and are never written again.
 */
object MediaServerStore {
    private const val KEY = "media_servers_json"
    private val SECRET_KEYS = setOf("url", "username", "password", "token", "userId", "accountToken")

    fun load(prefs: SharedPreferences): List<MediaServerConfig> {
        val raw = prefs.getString(KEY, null) ?: return migrateLegacy(prefs)
        return try {
            val arr = JSONArray(raw)
            val configs = (0 until arr.length()).map { i -> fromJson(arr.getJSONObject(i)) }
            if (containsLegacyPlaintextSecrets(arr)) save(prefs, configs)
            configs
        } catch (_: Exception) {
            // Encrypted data that cannot be authenticated is never treated as plaintext and is
            // never overwritten by an empty/default configuration.
            emptyList()
        }
    }

    fun save(prefs: SharedPreferences, list: List<MediaServerConfig>) {
        val arr = JSONArray()
        list.forEach { arr.put(toJson(it)) }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    fun get(prefs: SharedPreferences, id: String?): MediaServerConfig? =
        if (id.isNullOrBlank()) null else load(prefs).firstOrNull { it.id == id }

    fun upsert(prefs: SharedPreferences, config: MediaServerConfig): List<MediaServerConfig> {
        val current = load(prefs).toMutableList()
        val idx = current.indexOfFirst { it.id == config.id }
        if (idx >= 0) current[idx] = config else current.add(config)
        save(prefs, current)
        return current
    }

    fun remove(prefs: SharedPreferences, id: String): List<MediaServerConfig> {
        val current = load(prefs).filterNot { it.id == id }
        save(prefs, current)
        return current
    }

    fun setEnabled(prefs: SharedPreferences, id: String, enabled: Boolean): List<MediaServerConfig> {
        val current = load(prefs).map { if (it.id == id) it.copy(enabled = enabled) else it }
        save(prefs, current)
        return current
    }

    fun setContentFlags(
        prefs: SharedPreferences,
        id: String,
        live: Boolean? = null,
        movies: Boolean? = null,
        series: Boolean? = null
    ): List<MediaServerConfig> {
        val current = load(prefs).map {
            if (it.id != id) it
            else it.copy(
                liveEnabled = live ?: it.liveEnabled,
                moviesEnabled = movies ?: it.moviesEnabled,
                seriesEnabled = series ?: it.seriesEnabled
            )
        }
        save(prefs, current)
        return current
    }

    fun newId(): String = UUID.randomUUID().toString()

    private fun migrateLegacy(prefs: SharedPreferences): List<MediaServerConfig> {
        val migrated = mutableListOf<MediaServerConfig>()
        val jellyfinUrl = prefs.getString("jellyfin_url", null)
        if (!jellyfinUrl.isNullOrBlank()) {
            migrated += MediaServerConfig(
                id = newId(),
                type = "jellyfin",
                name = "Jellyfin",
                enabled = prefs.getBoolean("jellyfin_provider_enabled", true),
                url = jellyfinUrl,
                username = prefs.getString("jellyfin_user", null)?.takeIf { it.isNotBlank() },
                password = prefs.getString("jellyfin_pass", null)?.takeIf { it.isNotBlank() },
                token = prefs.getString("jellyfin_token", null)?.takeIf { it.isNotBlank() },
                userId = prefs.getString("jellyfin_userid", null)?.takeIf { it.isNotBlank() },
                liveEnabled = prefs.getBoolean("jellyfin_live_enabled", true),
                moviesEnabled = legacyFlag(prefs, "jellyfin_movies_enabled", "jellyfin_disable_vod"),
                seriesEnabled = legacyFlag(prefs, "jellyfin_series_enabled", "jellyfin_disable_vod")
            )
        }
        val plexUrl = prefs.getString("plex_url", null)
        val plexToken = prefs.getString("plex_token", null)
        if (!plexUrl.isNullOrBlank() && !plexToken.isNullOrBlank()) {
            migrated += MediaServerConfig(
                id = newId(),
                type = "plex",
                name = prefs.getString("plex_server_name", null)?.takeIf { it.isNotBlank() } ?: "Plex",
                enabled = prefs.getBoolean("plex_provider_enabled", true),
                url = plexUrl,
                token = plexToken,
                accountToken = prefs.getString("plex_account_token", null)?.takeIf { it.isNotBlank() },
                moviesEnabled = prefs.getBoolean("plex_movies_enabled", true),
                seriesEnabled = prefs.getBoolean("plex_series_enabled", true)
            )
        }
        save(prefs, migrated)
        prefs.edit()
            .remove("jellyfin_url").remove("jellyfin_user").remove("jellyfin_pass")
            .remove("jellyfin_token").remove("jellyfin_userid")
            .remove("jellyfin_provider_enabled").remove("jellyfin_disable_vod")
            .remove("jellyfin_live_enabled").remove("jellyfin_movies_enabled").remove("jellyfin_series_enabled")
            .remove("plex_url").remove("plex_token").remove("plex_account_token")
            .remove("plex_server_name").remove("plex_provider_enabled")
            .remove("plex_movies_enabled").remove("plex_series_enabled")
            .apply()
        return migrated
    }

    private fun legacyFlag(prefs: SharedPreferences, key: String, legacyOffKey: String): Boolean =
        if (prefs.contains(key)) prefs.getBoolean(key, true) else !prefs.getBoolean(legacyOffKey, false)

    private fun toJson(c: MediaServerConfig): JSONObject = JSONObject().apply {
        put("id", c.id)
        put("type", c.type)
        put("name", c.name)
        put("enabled", c.enabled)
        put("liveEnabled", c.liveEnabled)
        put("moviesEnabled", c.moviesEnabled)
        put("seriesEnabled", c.seriesEnabled)
        putSecret("url", c.url)
        putSecret("username", c.username)
        putSecret("password", c.password)
        putSecret("token", c.token)
        putSecret("userId", c.userId)
        putSecret("accountToken", c.accountToken)
    }

    private fun JSONObject.putSecret(key: String, value: String?) {
        value?.takeIf { it.isNotEmpty() }?.let { put(key, SecureValueStore.encrypt(it)) }
    }

    private fun fromJson(o: JSONObject): MediaServerConfig = MediaServerConfig(
        id = o.optString("id").ifBlank { newId() },
        type = o.optString("type", "jellyfin"),
        name = o.optString("name", "Media server"),
        enabled = o.optBoolean("enabled", true),
        url = readSecret(o, "url"),
        username = readSecret(o, "username"),
        password = readSecret(o, "password"),
        token = readSecret(o, "token"),
        userId = readSecret(o, "userId"),
        accountToken = readSecret(o, "accountToken"),
        liveEnabled = o.optBoolean("liveEnabled", true),
        moviesEnabled = o.optBoolean("moviesEnabled", true),
        seriesEnabled = o.optBoolean("seriesEnabled", true)
    )

    private fun readSecret(o: JSONObject, key: String): String? {
        val raw = o.optString(key).takeIf { it.isNotBlank() } ?: return null
        val decoded = SecureValueStore.decrypt(raw)
        if (SecureValueStore.isEncrypted(raw) && decoded == null) {
            throw SecurityException("Could not authenticate encrypted media-server secret: $key")
        }
        return decoded
    }

    private fun containsLegacyPlaintextSecrets(arr: JSONArray): Boolean =
        (0 until arr.length()).any { i ->
            val o = arr.optJSONObject(i) ?: return@any false
            SECRET_KEYS.any { key ->
                o.optString(key).takeIf { it.isNotBlank() }?.let { !SecureValueStore.isEncrypted(it) } ?: false
            }
        }
}
