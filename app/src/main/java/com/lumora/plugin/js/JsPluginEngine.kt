package com.lumora.plugin.js

import com.lumora.plugin.DiscoveredProvider
import com.lumora.plugin.DiscoveryResult
import com.lumora.plugin.ResolveResult
import com.lumora.plugin.SearchResult
import com.lumora.plugin.TorrentResult
import okhttp3.OkHttpClient

/**
 * CPZ hardened compatibility facade.
 *
 * Executable JavaScript plugins are not part of the trusted media-client baseline. The upstream
 * UI still references this API, so the type remains while every operation fails closed. There is
 * no JavaScript VM, native bridge, host HTTP API or network-supplied code evaluation behind it.
 */
class JsPluginEngine(
    @Suppress("UNUSED_PARAMETER") private val httpClient: OkHttpClient = OkHttpClient()
) {
    suspend fun runDiscovery(
        @Suppress("UNUSED_PARAMETER") source: String,
        @Suppress("UNUSED_PARAMETER") onProgress: (String) -> Unit = {},
        @Suppress("UNUSED_PARAMETER") onCandidate: (DiscoveredProvider) -> Unit = {},
    ): DiscoveryResult = DiscoveryResult.Failed(DISABLED_MESSAGE)

    suspend fun runSearch(
        @Suppress("UNUSED_PARAMETER") source: String,
        @Suppress("UNUSED_PARAMETER") query: String,
        @Suppress("UNUSED_PARAMETER") year: Int?,
        @Suppress("UNUSED_PARAMETER") season: Int?,
        @Suppress("UNUSED_PARAMETER") episode: Int?,
        @Suppress("UNUSED_PARAMETER") onProgress: (String) -> Unit = {},
        @Suppress("UNUSED_PARAMETER") onResult: (TorrentResult) -> Unit = {},
    ): SearchResult = SearchResult.Failed(DISABLED_MESSAGE)

    suspend fun resolve(
        @Suppress("UNUSED_PARAMETER") source: String,
        @Suppress("UNUSED_PARAMETER") token: String,
        @Suppress("UNUSED_PARAMETER") season: Int?,
        @Suppress("UNUSED_PARAMETER") episode: Int?,
    ): ResolveResult = ResolveResult.Failed(DISABLED_MESSAGE)

    suspend fun scraperSites(@Suppress("UNUSED_PARAMETER") source: String): String? = null

    suspend fun probeManifest(@Suppress("UNUSED_PARAMETER") source: String): Map<String, Any?>? = null

    companion object {
        private const val DISABLED_MESSAGE = "Executable plugins are disabled in the CPZ hardened build"
    }
}
