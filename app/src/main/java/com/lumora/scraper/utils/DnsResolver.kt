package com.lumora.scraper.utils

import android.util.Log
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps

/** DNS resolver with optional DoH. TLS and hostname verification always use platform defaults. */
object DnsResolver : Dns {
    private const val TAG = "DnsResolver"

    private val client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    private var _url: String = UserPreferences.dohProviderUrl
    private var _internalDoh: Dns = buildDoh(_url)

    override fun lookup(hostname: String): List<InetAddress> {
        return try {
            _internalDoh.lookup(hostname)
        } catch (e: Exception) {
            if (_internalDoh === Dns.SYSTEM) throw e
            Log.w(TAG, "DoH lookup failed; falling back to system DNS: ${e.message}")
            Dns.SYSTEM.lookup(hostname)
        }
    }

    val doh: Dns get() = this

    @Synchronized
    fun setDnsUrl(newUrl: String) {
        val normalized = newUrl.trim()
        if (normalized != _url) {
            _url = normalized
            _internalDoh = buildDoh(_url)
        }
    }

    @Synchronized
    private fun buildDoh(url: String): Dns {
        if (url.isBlank()) return Dns.SYSTEM
        if (!url.startsWith("https://", ignoreCase = true)) {
            Log.w(TAG, "Ignoring non-HTTPS DoH endpoint")
            return Dns.SYSTEM
        }
        return try {
            DnsOverHttps.Builder()
                .client(client)
                .url(url.toHttpUrl())
                .build()
        } catch (e: Exception) {
            Log.w(TAG, "Invalid DoH endpoint; using system DNS: ${e.message}")
            Dns.SYSTEM
        }
    }
}
