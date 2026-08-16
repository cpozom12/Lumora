package com.lumora.data.backup

import android.content.Context
import android.net.Uri

/**
 * CPZ hardened baseline: manual backup/import is deliberately disabled.
 *
 * The inherited implementation exported provider connection fields to a user-selected JSON file
 * without confidentiality or authenticated encryption. Even though SAF requires an explicit user
 * action, that is not an acceptable secret-handling contract for a trusted build.
 *
 * A future replacement must use a user-provided passphrase, a modern KDF and authenticated
 * encryption (for example AES-GCM) before this feature is re-enabled. The public data shapes are
 * retained temporarily so existing UI call sites keep compiling.
 */
class BackupManager(@Suppress("UNUSED_PARAMETER") private val context: Context) {

    companion object {
        const val BACKUP_VERSION = 2
    }

    data class BackupData(
        val version: Int = BACKUP_VERSION,
        val createdAt: String = "",
        val appVersion: String = "",
        val providers: List<ProviderBackup> = emptyList(),
        val epgSources: List<EpgSourceBackup> = emptyList(),
        val customGroups: List<CustomGroupBackup> = emptyList(),
        val favorites: List<String> = emptyList(),
        val watchHistory: List<WatchHistoryBackup> = emptyList(),
        val recordingStorage: RecordingStorageBackup? = null,
        val recordingSchedules: List<RecordingScheduleBackup> = emptyList(),
        val checksum: String = ""
    )

    data class ProviderBackup(
        val id: String, val name: String, val type: String,
        val serverUrl: String?, val username: String?, val password: String?,
        val m3uUrl: String?, val userAgent: String?, val macAddress: String?,
        val serialNumber: String?, val active: Boolean,
        val syncEnabled: Boolean, val epgSyncEnabled: Boolean
    )

    data class EpgSourceBackup(
        val id: String, val name: String, val url: String,
        val enabled: Boolean, val priority: Int
    )

    data class CustomGroupBackup(
        val id: String, val name: String, val mediaType: String,
        val isHidden: Boolean, val members: List<String> = emptyList()
    )

    data class WatchHistoryBackup(
        val channelId: String, val channelName: String,
        val mediaType: String, val positionMs: Long,
        val durationMs: Long, val status: String,
        val lastWatchedAt: Long
    )

    data class RecordingStorageBackup(
        val maxSimultaneous: Int, val retentionDays: Int,
        val fileNamePattern: String
    )

    data class RecordingScheduleBackup(
        val channelId: String, val channelName: String,
        val programTitle: String, val startTimeUtc: Long,
        val stopTimeUtc: Long, val recurringRule: String?
    )

    data class ImportResult(
        val providersImported: Int = 0,
        val epgSourcesImported: Int = 0,
        val customGroupsImported: Int = 0,
        val watchHistoryImported: Int = 0,
        val recordingSchedulesImported: Int = 0,
        val conflicts: Int = 0
    )

    suspend fun exportTo(@Suppress("UNUSED_PARAMETER") uri: Uri): Boolean = false

    suspend fun importFrom(
        @Suppress("UNUSED_PARAMETER") uri: Uri,
        @Suppress("UNUSED_PARAMETER") confirmed: Boolean = false
    ): ImportResult = ImportResult()
}
