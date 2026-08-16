package com.lumora.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalMediaProviderRegistryTest {

    @Test
    fun providerIdsAndPackagesAreUnique() {
        val providers = ExternalMediaProviderRegistry.providers
        val ids = providers.map { it.id }
        val packages = providers.flatMap { it.packageNames }

        assertEquals(ids.size, ids.toSet().size)
        assertEquals(packages.size, packages.toSet().size)
    }

    @Test
    fun v1ContainsOnlyReviewedOfficialPackageIds() {
        val packages = ExternalMediaProviderRegistry.providers.flatMap { it.packageNames }.toSet()
        assertEquals(
            setOf(
                "pe.movistar.go",
                "com.netflix.mediaclient",
                "com.disney.disneyplus",
                "com.google.android.youtube",
                "com.amazon.avod.thirdpartyclient",
                "com.wbd.stream",
            ),
            packages,
        )
        assertTrue(packages.all { it.isNotBlank() })
        assertFalse(packages.any { it.contains("://") })
    }
}
