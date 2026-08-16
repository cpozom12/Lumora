package com.lumora.plugin.js

import android.content.SharedPreferences
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Manages explicitly configured JavaScript plugin stores.
 *
 * CPZ hardening rule: no third-party plugin store is trusted or contacted by default. A clean
 * installation therefore has an empty store list and cannot silently inherit executable code
 * from the upstream project's GitHub account. Stores are opt-in and HTTPS-only.
 */
class PluginStoreManager(
    private val prefs: SharedPreferences,
    private val httpClient: OkHttpClient = OkHttpClient(),
) {
    fun storeUrls(): List<PluginStore> =
        customStoreUrls()
            .filter { isAllowedStoreUrl(it) }
            .sorted()
            .map { PluginStore(url = it, name = null, removable = true) }

    fun addStore(url: String) {
        val normalized = url.trim()
        if (!isAllowedStoreUrl(normalized)) return
        val current = customStoreUrls().toMutableSet()
        current.add(normalized)
        prefs.edit().putStringSet(PREF_STORE_URLS, current).apply()
    }

    fun removeStore(url: String) {
        val current = customStoreUrls().toMutableSet()
        current.remove(url)
        prefs.edit().putStringSet(PREF_STORE_URLS, current).apply()
    }

    private fun customStoreUrls(): Set<String> = prefs.getStringSet(PREF_STORE_URLS, emptySet()) ?: emptySet()

    suspend fun fetchCatalog(storeUrl: String): Result<List<StoreScript>> = withContext(Dispatchers.IO) {
        runCatching {
            require(isAllowedStoreUrl(storeUrl)) { "Plugin stores must use HTTPS" }
            val body = fetchText(storeUrl) ?: error("Couldn't reach that store")
            val json = JsonParser.parseString(body).asJsonObject
            val scriptsArray = json.getAsJsonArray("scripts")
            val baseUrl = storeUrl.substringBeforeLast('/', "") + "/"
            val result = mutableListOf<StoreScript>()
            scriptsArray?.forEach { element ->
                val item = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@forEach
                val id = item.optString("id")?.takeIf { it.isNotBlank() } ?: return@forEach
                val file = item.optString("file")?.takeIf { it.isNotBlank() } ?: return@forEach
                val capabilities = item.get("capabilities")?.takeIf { it.isJsonArray }?.asJsonArray
                    ?.mapNotNull { it.asString?.takeIf(String::isNotBlank) }
                    ?.toSet()
                    .orEmpty()
                val fileUrl = if (file.startsWith("https://")) file else baseUrl + file
                if (!isAllowedStoreUrl(fileUrl)) return@forEach
                result.add(
                    StoreScript(
                        id = id,
                        label = item.optString("label")?.takeIf { it.isNotBlank() } ?: id,
                        description = item.optString("description")?.takeIf { it.isNotBlank() },
                        capabilities = capabilities,
                        fileUrl = fileUrl,
                    )
                )
            }
            result
        }
    }

    suspend fun fetchStoreName(storeUrl: String): String? = withContext(Dispatchers.IO) {
        if (!isAllowedStoreUrl(storeUrl)) return@withContext null
        runCatching {
            val body = fetchText(storeUrl) ?: return@withContext null
            JsonParser.parseString(body).asJsonObject.optString("name")?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun com.google.gson.JsonObject.optString(key: String): String? =
        get(key)?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.asString

    suspend fun fetchScriptText(fileUrl: String): String? = withContext(Dispatchers.IO) {
        if (isAllowedStoreUrl(fileUrl)) fetchText(fileUrl) else null
    }

    private fun fetchText(url: String): String? = try {
        if (!isAllowedStoreUrl(url)) return null
        val request = Request.Builder().url(url).build()
        httpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) response.body?.string() else null
        }
    } catch (_: Exception) {
        null
    }

    private fun isAllowedStoreUrl(url: String): Boolean = url.trim().startsWith("https://")

    companion object {
        private const val PREF_STORE_URLS = "plugin_store_urls"
        /** Upstream store retained only as a label/reference; it is NOT auto-enabled. */
        const val DEFAULT_STORE_URL = "https://raw.githubusercontent.com/disclosurez/Lumora-Plugins/master/scripts/index.json"
    }
}
