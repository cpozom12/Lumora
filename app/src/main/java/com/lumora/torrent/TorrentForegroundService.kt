package com.lumora.torrent

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

/** Compatibility stub. Torrent/P2P networking is absent from the CPZ hardened build. */
class TorrentForegroundService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stopSelf(startId)
        return START_NOT_STICKY
    }

    companion object {
        fun start(@Suppress("UNUSED_PARAMETER") context: Context) {
            // No-op: there is no torrent engine/network operation to keep alive.
        }

        fun stop(@Suppress("UNUSED_PARAMETER") context: Context) {
            // No-op.
        }
    }
}
