package com.lumora.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lumora.cache.ProgramReminder
import com.lumora.cache.ReminderStore

/**
 * Compatibility receiver retained while the inherited reminder code is being pruned.
 *
 * Personal Media Hub V1 intentionally has no notification permission and does not register
 * reminder/recording receivers in the manifest. If an already-created PendingIntent reaches this
 * class during an upgrade, only clean the stale reminder state; never post a notification.
 */
class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val channelId = intent.getStringExtra(EXTRA_CHANNEL_ID) ?: return
        val channelName = intent.getStringExtra(EXTRA_CHANNEL_NAME) ?: ""
        val programTitle = intent.getStringExtra(EXTRA_PROGRAM_TITLE) ?: ""
        val startTimestamp = intent.getLongExtra(EXTRA_START_TIMESTAMP, 0L)

        ReminderStore.remove(
            context,
            ProgramReminder(channelId, channelName, programTitle, startTimestamp).key
        )
    }
}
