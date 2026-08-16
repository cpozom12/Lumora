package com.lumora.hub

import android.content.Context
import android.content.Intent

/** Result of an explicit, local-only handoff to an already-installed official app. */
sealed interface ExternalLaunchResult {
    data object Launched : ExternalLaunchResult
    data object NotInstalled : ExternalLaunchResult
    data class Blocked(val reason: String? = null) : ExternalLaunchResult
}

/**
 * Opens reviewed provider packages on the Android phone.
 *
 * This intentionally does not use CarContext.startCarApp for third-party packages. The Car App
 * Library only permits a constrained set of car intents and explicitly rejects attempts to start
 * another app by component. Android Auto/head-unit presentation therefore remains host-controlled.
 */
class ExternalProviderLauncher(private val context: Context) {

    fun installedPackage(provider: ExternalMediaProvider): String? =
        provider.packageNames.firstOrNull { packageName ->
            context.packageManager.getLaunchIntentForPackage(packageName) != null
        }

    fun isInstalled(provider: ExternalMediaProvider): Boolean = installedPackage(provider) != null

    fun launchOnPhone(provider: ExternalMediaProvider): ExternalLaunchResult {
        val packageName = installedPackage(provider) ?: return ExternalLaunchResult.NotInstalled
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return ExternalLaunchResult.NotInstalled

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return try {
            context.startActivity(launchIntent)
            ExternalLaunchResult.Launched
        } catch (t: Throwable) {
            // Background-activity and car-host policy differs by Android/host version. Fail closed
            // and surface the result to the user instead of trying undocumented bypasses.
            ExternalLaunchResult.Blocked(t.message)
        }
    }
}
