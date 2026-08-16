package com.lumora

import android.app.Application
import com.lumora.data.local.LumoraDatabase
import com.lumora.data.remote.jellyfin.JellyfinAuthInterceptor
import com.lumora.data.remote.plex.PlexAuthInterceptor
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

class BaseApplication : Application() {

    lateinit var okHttpClient: OkHttpClient
        private set

    var processStartedAt: Long = 0L
        private set

    lateinit var database: LumoraDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        processStartedAt = System.currentTimeMillis()

        try {
            com.google.android.gms.cast.framework.CastContext.getSharedInstance(this)
        } catch (_: Exception) {
            // Google Play Services not available on this device.
        }

        // CPZ hardening: do not initialize/load the native QuickJS runtime during application
        // startup. Remote plugin execution is disabled in the hardened baseline. Keeping the
        // dependency temporarily avoids a broad upstream UI refactor, but its native code is not
        // placed into the running process through the normal startup path.

        // Context retained temporarily for inherited scraper compatibility. The scraper provider
        // registry itself is empty in the hardened baseline.
        com.lumora.scraper.ScraperApp.init(this)

        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .connectionPool(okhttp3.ConnectionPool(4, 30, TimeUnit.SECONDS))
            .dns(okhttp3.Dns.SYSTEM)
            .addInterceptor(JellyfinAuthInterceptor())
            .addInterceptor(PlexAuthInterceptor())
            .cache(Cache(File(cacheDir, "okhttp_cache"), 50L * 1024 * 1024))
            .build()

        database = LumoraDatabase.getInstance(this)
    }

    companion object {
        lateinit var instance: BaseApplication
            private set
    }
}
