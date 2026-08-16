package com.lumora.plugin.js

import android.content.Context
import android.content.SharedPreferences
import com.lumora.R
import java.io.File

/**
 * CPZ hardened compatibility facade.
 *
 * Executable JavaScript plugins are disabled in the trusted baseline. The class remains because
 * upstream UI code references its API in several places; every execution/install/enable path is
 * fail-closed. This also means normal startup never needs to initialize the native QuickJS runtime.
 */
class PluginScriptManager(
    private val context: Context,
    @Suppress("UNUSED_PARAMETER") private val prefs: SharedPreferences,
    @Suppress("UNUSED_PARAMETER") private val engine: JsPluginEngine = JsPluginEngine(),
) {
    suspend fun discoverScripts(): List<PluginScript> = emptyList()

    fun getDiscoveredScripts(): List<PluginScript> = emptyList()

    fun readSource(@Suppress("UNUSED_PARAMETER") script: PluginScript): String = ""

    fun isEnabled(@Suppress("UNUSED_PARAMETER") scriptId: String): Boolean = false

    fun setEnabled(@Suppress("UNUSED_PARAMETER") scriptId: String, @Suppress("UNUSED_PARAMETER") enabled: Boolean) {
        // Execution cannot be enabled in the hardened baseline.
    }

    fun addUserScript(@Suppress("UNUSED_PARAMETER") fileName: String, @Suppress("UNUSED_PARAMETER") text: String): File {
        throw SecurityException("Executable plugins are disabled in the CPZ hardened build")
    }

    fun removeUserScript(fileName: String): Boolean =
        File(context.filesDir, "plugin_scripts/$fileName").delete()

    sealed class InstallResult {
        data class Installed(val script: PluginScript) : InstallResult()
        data class Rejected(val reason: String) : InstallResult()
    }

    suspend fun installScript(@Suppress("UNUSED_PARAMETER") text: String): InstallResult =
        InstallResult.Rejected(context.getString(R.string.ui_plugin_invalid_script))
}
