package com.lumora.data.update

import android.content.Context

/**
 * CPZ hardened build: in-app self-updates are deliberately disabled.
 *
 * The upstream implementation trusted the latest APK asset published by another GitHub account.
 * That creates a supply-chain path around this fork's review, CI and signing controls. Releases
 * for this fork must instead be built, reviewed and signed by our own pipeline and installed by
 * the user through Android's normal package installer.
 *
 * The class and [UpdateInfo] shape remain for now so existing UI call sites keep compiling while
 * the update UI is removed in a later cleanup.
 */
class AppUpdateChecker(@Suppress("UNUSED_PARAMETER") context: Context) {

    data class UpdateInfo(
        val latestVersion: String,
        val currentVersion: String,
        val downloadUrl: String,
        val releaseNotes: String,
        val isUpdateAvailable: Boolean
    )

    /** Never performs network I/O and never advertises an APK to install. */
    suspend fun checkForUpdate(): UpdateInfo? = null
}
