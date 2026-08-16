package com.lumora.scraper.extractors

import com.lumora.scraper.models.Video

/**
 * Disabled in the CPZ hardened build.
 *
 * Upstream decoded JavaScript supplied by this remote host and, on fallback, evaluated the
 * downloaded script inside Rhino with a standard Java-enabled scope. That crosses the security
 * boundary from untrusted web content into executable code in the app process. The extractor is
 * intentionally fail-closed until it can be rewritten as a non-executing parser.
 */
class MStreamDayExtractor : Extractor() {
    override val name = "moflix-stream.day"
    override val mainUrl = "https://moflix-stream.day"

    override suspend fun extract(link: String): Video {
        throw SecurityException("MStreamDay extractor disabled in hardened build")
    }
}
