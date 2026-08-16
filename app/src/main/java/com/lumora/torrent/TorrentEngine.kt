package com.lumora.torrent

/**
 * Compatibility stub for the CPZ hardened baseline.
 *
 * The upstream application bundled a native libtorrent engine and a NanoHTTPD local streaming
 * server. Neither is required for the trusted IPTV/Jellyfin/Plex/Android Auto product core, and
 * keeping a P2P/native/network server stack would materially enlarge the attack surface.
 *
 * Call sites are intentionally retained temporarily so the rest of the upstream UI can compile;
 * any attempt to resolve a native torrent fails closed.
 */
class TorrentEngine(@Suppress("UNUSED_PARAMETER") context: android.content.Context) {

    fun start(
        @Suppress("UNUSED_PARAMETER") magnet: String,
        @Suppress("UNUSED_PARAMETER") season: Int?,
        @Suppress("UNUSED_PARAMETER") episode: Int?,
        @Suppress("UNUSED_PARAMETER") onProgress: (String) -> Unit,
    ): String {
        throw SecurityException("Torrent/P2P playback is disabled in the CPZ hardened build")
    }

    fun stop() {
        // No native session or local HTTP server exists in the hardened build.
    }
}
