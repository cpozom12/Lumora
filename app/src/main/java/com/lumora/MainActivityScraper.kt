package com.lumora

import android.widget.Toast
import com.lumora.model.Channel

/**
 * CPZ trusted-build compatibility facade for the inherited web-scraper feature.
 *
 * The personal Media Hub does not ship web-site scraping as an executable playback source. These
 * functions remain only because older catalogue/settings code calls them. They intentionally have
 * no dependency on `com.lumora.scraper`, allowing R8 to remove the inherited scraper graph from
 * the APK while the broader upstream UI is refactored incrementally.
 */
internal fun MainActivity.scraperCanSource(@Suppress("UNUSED_PARAMETER") item: Channel): Boolean = false

internal fun MainActivity.scrapersUsable(): Boolean = false

internal fun MainActivity.hasProviderlessSource(): Boolean = false

internal fun MainActivity.showScraperSourceDialog(
    @Suppress("UNUSED_PARAMETER") item: Channel,
    @Suppress("UNUSED_PARAMETER") season: Int? = null,
    @Suppress("UNUSED_PARAMETER") episode: Int? = null,
) {
    scraperToast(getString(R.string.cpz_scraper_playback_disabled))
}

/** Remote scraper-site manifests are never loaded in the trusted build. */
internal fun MainActivity.loadScraperSiteManifest() = Unit

/** Pure media helper retained for ordinary authorized HLS URLs. */
internal fun hlsMimeIfLooksLikeHls(url: String): String? =
    if (url.contains("m3u8", ignoreCase = true)) androidx.media3.common.MimeTypes.APPLICATION_M3U8
    else null

internal fun MainActivity.scraperToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
