package com.lumora.scraper.providers

import com.lumora.scraper.adapters.AppAdapter
import com.lumora.scraper.models.Category
import com.lumora.scraper.models.Episode
import com.lumora.scraper.models.Genre
import com.lumora.scraper.models.Movie
import com.lumora.scraper.models.People
import com.lumora.scraper.models.TvShow
import com.lumora.scraper.models.Video
import kotlinx.coroutines.sync.Mutex

interface ProviderPortalUrl {
    val portalUrl: String
    val defaultPortalUrl: String
}

interface ProviderConfigUrl {
    val defaultBaseUrl: String
    suspend fun onChangeUrl(forceRefresh: Boolean = false): String
    val changeUrlMutex: Mutex
}

interface IptvProvider : Provider

interface Provider {
    val baseUrl: String
    val name: String
    val logo: String
    val language: String

    suspend fun getHome(): List<Category>
    suspend fun search(query: String, page: Int = 1): List<AppAdapter.Item>
    suspend fun getMovies(page: Int = 1): List<Movie>
    suspend fun getTvShows(page: Int = 1): List<TvShow>
    suspend fun getMovie(id: String): Movie
    suspend fun getTvShow(id: String): TvShow
    suspend fun getEpisodesBySeason(seasonId: String): List<Episode>
    suspend fun getGenre(id: String, page: Int = 1): Genre
    suspend fun getPeople(id: String, page: Int = 1): People
    suspend fun getServers(id: String, videoType: Video.Type): List<Video.Server>
    suspend fun getVideo(server: Video.Server): Video

    companion object {
        data class ProviderSupport(val movies: Boolean, val tvShows: Boolean)

        /**
         * CPZ hardened build: inherited web-scraper providers are disabled by default.
         *
         * The upstream list mixed roughly sixty independently changing third-party streaming
         * sites into the same process as IPTV/Jellyfin/Plex. Several providers contain legacy
         * TLS-bypass and WebView/extractor logic. None is required for the product's trusted
         * media-client core, so the secure baseline exposes no scraper provider at runtime.
         * Individual providers can only return after a separate review and explicit allowlist.
         */
        val providers: Map<Provider, ProviderSupport> = emptyMap()

        fun supportsMovies(provider: Provider): Boolean =
            providers[provider]?.movies ?: false

        fun supportsTvShows(provider: Provider): Boolean =
            providers[provider]?.tvShows ?: false

        fun findByName(name: String): Provider? =
            providers.keys.find { it.name == name }
    }
}
