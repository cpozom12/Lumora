package com.lumora.plugin.js

import android.content.SharedPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Minimal in-memory SharedPreferences for the fail-closed plugin-store contract. */
private class FakeSharedPreferences : SharedPreferences {
    private val data = mutableMapOf<String, Any?>()

    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? =
        @Suppress("UNCHECKED_CAST") (data[key] as? MutableSet<String>) ?: defValues

    override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
        private val pending = mutableMapOf<String, Any?>()
        override fun putStringSet(key: String, values: MutableSet<String>?) = this.also { pending[key] = values?.toMutableSet() }
        override fun apply() { data.putAll(pending) }
        override fun commit(): Boolean { data.putAll(pending); return true }
        override fun putString(key: String, value: String?) = this.also { pending[key] = value }
        override fun putInt(key: String, value: Int) = this.also { pending[key] = value }
        override fun putLong(key: String, value: Long) = this.also { pending[key] = value }
        override fun putFloat(key: String, value: Float) = this.also { pending[key] = value }
        override fun putBoolean(key: String, value: Boolean) = this.also { pending[key] = value }
        override fun remove(key: String) = this.also { pending[key] = null }
        override fun clear() = this.also { data.clear() }
    }

    override fun getAll(): MutableMap<String, *> = data
    override fun getString(key: String, defValue: String?): String? = data[key] as? String ?: defValue
    override fun getInt(key: String, defValue: Int): Int = data[key] as? Int ?: defValue
    override fun getLong(key: String, defValue: Long): Long = data[key] as? Long ?: defValue
    override fun getFloat(key: String, defValue: Float): Float = data[key] as? Float ?: defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean = data[key] as? Boolean ?: defValue
    override fun contains(key: String): Boolean = data.containsKey(key)
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
}

class PluginStoreManagerTest {

    @Test
    fun `trusted baseline exposes no remote plugin stores`() {
        val manager = PluginStoreManager(FakeSharedPreferences())
        assertTrue(manager.storeUrls().isEmpty())
        assertEquals("", PluginStoreManager.DEFAULT_STORE_URL)
    }

    @Test
    fun `adding a store is ignored`() {
        val manager = PluginStoreManager(FakeSharedPreferences())
        manager.addStore("https://example.com/plugins/index.json")
        assertTrue(manager.storeUrls().isEmpty())
    }

    @Test
    fun `catalog fetch always fails closed without network trust`() = runBlocking {
        val manager = PluginStoreManager(FakeSharedPreferences())
        val result = manager.fetchCatalog("https://example.com/plugins/index.json")
        assertTrue(result.isFailure)
    }

    @Test
    fun `script download is disabled`() = runBlocking {
        val manager = PluginStoreManager(FakeSharedPreferences())
        assertNull(manager.fetchScriptText("https://example.com/plugin.js"))
    }
}
