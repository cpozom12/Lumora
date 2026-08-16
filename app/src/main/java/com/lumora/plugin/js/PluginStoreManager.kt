package com.lumora.plugin.js

import android.content.SharedPreferences
import okhttp3.OkHttpClient

/**
 * CPZ hardened compatibility facade.
 *
 * Remote JavaScript plugin stores are disabled in the trusted baseline. The upstream UI can keep
 * referencing this class, but no store is persisted, fetched or exposed and no script body is
 * downloaded. Reintroducing plugins later requires a separate signed/hash-pinned trust design.
 */
class PluginStoreManager(
    @Suppress("UNUSED_PARAMETER") private val prefs: SharedPreferences,
    @Suppress("UNUSED_PARAMETER") private val httpClient: OkHttpClient = OkHttpClient(),
) {
    fun storeUrls(): List<PluginStore> = emptyList()

    fun addStore(@Suppress("UNUSED_PARAMETER") url: String) {
        // Disabled.
    }

    fun removeStore(@Suppress("UNUSED_PARAMETER") url: String) {
        // Disabled.
    }

    suspend fun fetchCatalog(@Suppress("UNUSED_PARAMETER") storeUrl: String): Result<List<StoreScript>> =
        Result.failure(SecurityException("Remote plugin stores are disabled in the CPZ hardened build"))

    suspend fun fetchStoreName(@Suppress("UNUSED_PARAMETER") storeUrl: String): String? = null

    suspend fun fetchScriptText(@Suppress("UNUSED_PARAMETER") fileUrl: String): String? = null

    companion object {
        /** Kept only for source/API compatibility; it is never contacted by this class. */
        const val DEFAULT_STORE_URL = ""
    }
}
