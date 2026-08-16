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

        // CPZ trusted build: no remote executable-plugin runtime and no inherited web-scraper
        // application context is initialized here. Keeping startup free of those compatibility
        // surfaces allows R8 to remove their unreachable implementation from the shipped APK.

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
