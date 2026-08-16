package com.lumora.plugin.js

import android.content.Context
import android.content.SharedPreferences
import com.lumora.R
import java.io.File

/**
 * Finds installed JS plugin scripts and remembers which are enabled.
 *
 * CPZ hardening rule: installing executable plugin code never enables it automatically. A user
 * must separately opt in after the script has been saved and inspected/listed.
 */
class PluginScriptManager(
    private val context: Context,
    private val prefs: SharedPreferences,
    private val engine: JsPluginEngine = JsPluginEngine(),
) {
    private var scripts: List<PluginScript> = emptyList()

    private val pluginPrefs: SharedPreferences by lazy {
        if (prefs.contains(PREF_ENABLED_SCRIPTS)) {
            prefs.edit().remove(PREF_ENABLED_SCRIPTS).apply()
        }
        context.getSharedPreferences(PLUGIN_PREFS_FILE, Context.MODE_PRIVATE)
    }

    suspend fun discoverScripts(): List<PluginScript> {
        val enabledIds = enabledScriptIds()
        val result = mutableListOf<PluginScript>()

        userScriptsDir().listFiles { f -> f.isFile && f.name.endsWith(".js") }?.forEach { file ->
            val text = runCatching { file.readText() }.getOrNull()
            if (text != null) {
                val fallbackId = file.name.removeSuffix(".js")
                toPluginScript(file.name, fallbackId, text, enabled = false)?.let {
                    result.add(it.copy(enabled = it.id in enabledIds))
                }
            }
        }

        scripts = result.sortedBy { it.label.lowercase() }
        pruneEnabledIds(scripts.map { it.id }.toSet())
        return scripts
    }

    private fun pruneEnabledIds(installedIds: Set<String>) {
        val stored = enabledScriptIds()
        val kept = stored.filterTo(mutableSetOf()) { it in installedIds }
        if (kept.size != stored.size) {
            pluginPrefs.edit().putStringSet(PREF_ENABLED_SCRIPTS, kept).apply()
        }
    }

    fun getDiscoveredScripts(): List<PluginScript> = scripts

    fun readSource(script: PluginScript): String =
        runCatching { File(userScriptsDir(), script.fileName).readText() }.getOrElse {
            PluginLog.w(TAG, "readSource failed for ${script.fileName}: ${it.message}")
            ""
        }

    fun isEnabled(scriptId: String): Boolean = scriptId in enabledScriptIds()

    fun setEnabled(scriptId: String, enabled: Boolean) {
        val current = enabledScriptIds().toMutableSet()
        if (enabled) current.add(scriptId) else current.remove(scriptId)
        pluginPrefs.edit().putStringSet(PREF_ENABLED_SCRIPTS, current).apply()
        scripts = scripts.map { if (it.id == scriptId) it.copy(enabled = enabled) else it }
    }

    fun addUserScript(fileName: String, text: String): File {
        val file = File(userScriptsDir(), fileNameFor(fileName))
        file.writeText(text)
        return file
    }

    fun removeUserScript(fileName: String): Boolean = File(userScriptsDir(), fileName).delete()

    sealed class InstallResult {
        data class Installed(val script: PluginScript) : InstallResult()
        data class Rejected(val reason: String) : InstallResult()
    }

    /**
     * Validates and saves [text]. A new script always lands disabled. Re-installing an existing
     * script preserves its previous enable state, so an update cannot silently reactivate code
     * the user disabled.
     */
    suspend fun installScript(text: String): InstallResult {
        val fallbackId = "script-${System.currentTimeMillis()}"
        val manifest = try {
            engine.probeManifest(text)
        } catch (_: Exception) {
            null
        } ?: return InstallResult.Rejected(context.getString(R.string.ui_plugin_invalid_script))

        val capabilities = extractCapabilities(manifest)
        if (capabilities.isEmpty()) return InstallResult.Rejected(context.getString(R.string.ui_plugin_no_capability))

        val id = (manifest["id"] as? String)?.takeIf { it.isNotBlank() } ?: fallbackId
        val isFirstInstall = scripts.none { it.id == id }
        val file = addUserScript(id, text)
        val enabled = if (isFirstInstall) {
            setEnabled(id, false)
            false
        } else {
            isEnabled(id)
        }
        val script = PluginScript(
            fileName = file.name,
            id = id,
            label = (manifest["label"] as? String)?.takeIf { it.isNotBlank() } ?: id,
            description = manifest["description"] as? String,
            capabilities = capabilities,
            enabled = enabled,
            resolvesNatively = manifest["resolvesNatively"] as? Boolean ?: false,
            contentTypes = extractContentTypes(manifest),
        )
        discoverScripts()
        return InstallResult.Installed(script)
    }

    private suspend fun toPluginScript(
        fileName: String,
        fallbackId: String,
        text: String,
        enabled: Boolean,
    ): PluginScript? {
        val manifest = try {
            engine.probeManifest(text)
        } catch (_: Exception) {
            null
        } ?: return null

        val capabilities = extractCapabilities(manifest)
        if (capabilities.isEmpty()) return null

        val id = (manifest["id"] as? String)?.takeIf { it.isNotBlank() } ?: fallbackId
        return PluginScript(
            fileName = fileName,
            id = id,
            label = (manifest["label"] as? String)?.takeIf { it.isNotBlank() } ?: id,
            description = manifest["description"] as? String,
            capabilities = capabilities,
            enabled = enabled,
            resolvesNatively = manifest["resolvesNatively"] as? Boolean ?: false,
            contentTypes = extractContentTypes(manifest),
        )
    }

    private fun extractCapabilities(manifest: Map<String, Any?>): Set<String> =
        (manifest["capabilities"] as? List<*>)
            ?.mapNotNull { it as? String }
            ?.filter { it in KNOWN_CAPABILITIES }
            ?.toSet()
            .orEmpty()

    private fun extractContentTypes(manifest: Map<String, Any?>): Set<String> =
        (manifest["contentTypes"] as? List<*>)
            ?.mapNotNull { it as? String }
            ?.toSet()
            .orEmpty()

    private fun fileNameFor(fileName: String): String =
        fileName.replace(Regex("[^A-Za-z0-9._-]"), "_").let { if (it.endsWith(".js")) it else "$it.js" }

    private fun userScriptsDir(): File = File(context.filesDir, "plugin_scripts").apply { mkdirs() }

    private fun enabledScriptIds(): Set<String> =
        pluginPrefs.getStringSet(PREF_ENABLED_SCRIPTS, emptySet()) ?: emptySet()

    companion object {
        private const val TAG = "PluginScriptManager"
        private const val PREF_ENABLED_SCRIPTS = "plugin_enabled_scripts"
        private const val PLUGIN_PREFS_FILE = "plugin_prefs"
        private val KNOWN_CAPABILITIES = setOf(
            JsPluginContract.CAPABILITY_PROVIDER_DISCOVERY,
            JsPluginContract.CAPABILITY_STREAM_SEARCH,
            JsPluginContract.CAPABILITY_SCRAPER_SITES,
        )
    }
}
