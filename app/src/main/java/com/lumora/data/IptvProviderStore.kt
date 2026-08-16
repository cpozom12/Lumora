package com.lumora.data

import android.content.SharedPreferences
import com.lumora.model.IptvProviderConfig
import com.lumora.security.SecureValueStore
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Persists IPTV provider metadata while keeping connection details encrypted with an
 * AndroidKeyStore-backed AES-GCM key.
 *
 * Upstream stored URLs, usernames and passwords directly in the JSON preference. load() accepts
 * that legacy representation once, then immediately rewrites it using encrypted envelopes.
 */
object IptvProviderStore {
    private const val KEY = "iptv_providers_json"
    private const val LEGACY_ENABLED_KEY = "iptv_provider_enabled"
    private val SECRET_KEYS = setOf("url", "username", "password", "userAgent")

    fun load(prefs: SharedPreferences): List<IptvProviderConfig> {
        val raw = prefs.getString(KEY, null)
        if (raw == null) return migrateLegacy(prefs)
        return try {
            val arr = JSONArray(raw)
            val configs = (0 until arr.length()).map { i -> fromJson(arr.getJSONObject(i)) }
            if (containsLegacyPlaintextSecrets(arr)) save(prefs, configs)
            configs
        } catch (_: Exception) {
            // Fail closed. In particular, never overwrite encrypted data if the Android Keystore
            // key is unavailable or an AES-GCM authentication tag does not verify.
            emptyList()
        }
    }

    fun save(prefs: SharedPreferences, list: List<IptvProviderConfig>) {
        val arr = JSONArray()
        list.forEach { arr.put(toJson(it)) }
        // Encryption happens before the preference editor is created. If Keystore encryption
        // fails, the old persisted value remains untouched and plaintext is never written.
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    fun upsert(prefs: SharedPreferences, config: IptvProviderConfig): List<IptvProviderConfig> {
        val current = load(prefs).toMutableList()
        val idx = current.indexOfFirst { it.id == config.id }
        if (idx >= 0) current[idx] = config else current.add(config)
        save(prefs, current)
        return current
    }

    fun remove(prefs: SharedPreferences, id: String): List<IptvProviderConfig> {
        val current = load(prefs).filterNot { it.id == id }
        save(prefs, current)
        return current
    }

    fun setEnabled(prefs: SharedPreferences, id: String, enabled: Boolean): List<IptvProviderConfig> {
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
    ): List<IptvProviderConfig> {
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

    private fun migrateLegacy(prefs: SharedPreferences): List<IptvProviderConfig> {
        val type = prefs.getString("provider_type", null) ?: return emptyList()
        val enabled = prefs.getBoolean(LEGACY_ENABLED_KEY, true)
        val config = when (type) {
            "xtream" -> {
                val url = prefs.getString("xtream_url", null)
                if (url.isNullOrBlank()) null else IptvProviderConfig(
                    id = newId(), type = "xtream",
                    name = prefs.getString("provider_name", "Xtream") ?: "Xtream",
                    enabled = enabled, url = url,
                    username = prefs.getString("xtream_user", null),
                    password = prefs.getString("xtream_pass", null)
                )
            }
            "stalker" -> {
                val url = prefs.getString("stalker_url", null)
                if (url.isNullOrBlank()) null else IptvProviderConfig(
                    id = newId(), type = "stalker",
                    name = prefs.getString("provider_name", "Stalker") ?: "Stalker",
                    enabled = enabled, url = url,
                    userAgent = prefs.getString("stalker_mac", null)
                )
            }
            "m3u" -> {
                val url = prefs.getString("m3u_url", null)
                if (url.isNullOrBlank()) null else IptvProviderConfig(
                    id = newId(), type = "m3u",
                    name = prefs.getString("provider_name", "M3U") ?: "M3U",
                    enabled = enabled, url = url,
                    userAgent = prefs.getString("user_agent", null)
                )
            }
            else -> null
        }
        val migrated = listOfNotNull(config)
        if (migrated.isNotEmpty()) save(prefs, migrated)
        prefs.edit().remove("provider_type").remove(LEGACY_ENABLED_KEY)
            .remove("xtream_url").remove("xtream_user").remove("xtream_pass")
            .remove("stalker_url").remove("stalker_mac")
            .remove("m3u_url").remove("provider_name").remove("user_agent")
            .apply()
        return migrated
    }

    private fun toJson(c: IptvProviderConfig): JSONObject = JSONObject().apply {
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
        putSecret("userAgent", c.userAgent)
    }

    private fun JSONObject.putSecret(key: String, value: String?) {
        value?.takeIf { it.isNotEmpty() }?.let { put(key, SecureValueStore.encrypt(it)) }
    }

    private fun fromJson(o: JSONObject): IptvProviderConfig {
        val legacyVodOff = o.optBoolean("disableVod", false)
        fun flag(key: String) = if (o.has(key)) o.optBoolean(key, true) else !legacyVodOff
        return IptvProviderConfig(
            id = o.optString("id").ifBlank { newId() },
            type = o.optString("type", "m3u"),
            name = o.optString("name", "Provider"),
            enabled = o.optBoolean("enabled", true),
            liveEnabled = o.optBoolean("liveEnabled", true),
            moviesEnabled = flag("moviesEnabled"),
            seriesEnabled = flag("seriesEnabled"),
            url = readSecret(o, "url"),
            username = readSecret(o, "username"),
            password = readSecret(o, "password"),
            userAgent = readSecret(o, "userAgent")
        )
    }

    private fun readSecret(o: JSONObject, key: String): String? {
        val raw = o.optString(key).takeIf { it.isNotBlank() } ?: return null
        val decoded = SecureValueStore.decrypt(raw)
        if (SecureValueStore.isEncrypted(raw) && decoded == null) {
            throw SecurityException("Could not authenticate encrypted IPTV secret: $key")
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
