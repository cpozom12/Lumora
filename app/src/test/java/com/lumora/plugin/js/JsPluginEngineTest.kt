package com.lumora.plugin.js

import com.lumora.plugin.DiscoveryResult
import com.lumora.plugin.ResolveResult
import com.lumora.plugin.SearchResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regression tests for the CPZ trusted-baseline rule: executable plugins never run. */
class JsPluginEngineTest {

    private val engine = JsPluginEngine()

    @Test
    fun `discovery fails closed`() = runBlocking {
        assertTrue(engine.runDiscovery("throw new Error('should never execute')") is DiscoveryResult.Failed)
    }

    @Test
    fun `search fails closed`() = runBlocking {
        assertTrue(
            engine.runSearch(
                source = "throw new Error('should never execute')",
                query = "test",
                year = null,
                season = null,
                episode = null,
            ) is SearchResult.Failed
        )
    }

    @Test
    fun `resolve fails closed`() = runBlocking {
        assertTrue(
            engine.resolve(
                source = "throw new Error('should never execute')",
                token = "secret",
                season = null,
                episode = null,
            ) is ResolveResult.Failed
        )
    }

    @Test
    fun `manifest probing and scraper manifests are disabled`() = runBlocking {
        assertNull(engine.probeManifest("PLUGIN = {}"))
        assertNull(engine.scraperSites("function sites() { return '[]'; }"))
    }
}
