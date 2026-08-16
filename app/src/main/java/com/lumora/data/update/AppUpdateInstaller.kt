package com.lumora.data.update

import android.content.Context

/**
 * CPZ hardened compatibility facade.
 *
 * The trusted build never downloads APKs, requests "install unknown apps", opens that settings
 * page, or launches the package installer. Updates must come from a reviewed CPZ build artifact
 * whose hash/signature is verified outside the running application.
 */
class AppUpdateInstaller(@Suppress("UNUSED_PARAMETER") private val context: Context) {

    fun downloadApk(
        @Suppress("UNUSED_PARAMETER") downloadUrl: String,
        @Suppress("UNUSED_PARAMETER") versionName: String
    ): Long = -1L

    fun installApk(@Suppress("UNUSED_PARAMETER") filePath: String): Boolean = false

    fun isDownloadComplete(@Suppress("UNUSED_PARAMETER") downloadId: Long): Boolean = false

    fun isDownloadFailed(@Suppress("UNUSED_PARAMETER") downloadId: Long): Boolean = true

    fun getDownloadedFilePath(@Suppress("UNUSED_PARAMETER") downloadId: Long): String? = null
}
