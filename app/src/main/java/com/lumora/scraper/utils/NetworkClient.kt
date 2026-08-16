package com.lumora.scraper.utils

import android.os.Build
import android.util.Log
import android.webkit.CookieManager
import com.lumora.R
import com.lumora.scraper.ScraperApp
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import okhttp3.ConnectionPool
import okhttp3.ConnectionSpec
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.Dispatcher
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.TlsVersion

/** Shared network clients for the scraper compatibility layer.
 *
 * CPZ hardening invariants:
 *  - certificate validation is never disabled;
 *  - hostname verification is never disabled;
 *  - request headers/cookies/tokens are not dumped to logcat;
 *  - HTTPS uses TLS 1.2+ only.
 */
object NetworkClient {

    private const val TAG = "NetworkClient"
    const val USER_AGENT = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"

    private val cookieManager by lazy { CookieManager.getInstance() }

    val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookies.forEach { cookie -> cookieManager.setCookie(url.toString(), cookie.toString()) }
            cookieManager.flush()
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val cookieString = cookieManager.getCookie(url.toString()) ?: return emptyList()
            return cookieString.split(";").mapNotNull { Cookie.parse(url, it.trim()) }
        }
    }

    private val sharedPool: ConnectionPool by lazy { ConnectionPool() }
    private val sharedDispatcher: Dispatcher by lazy { Dispatcher() }

    val default: OkHttpClient by lazy { buildClient(DnsResolver.doh) }
    val systemDns: OkHttpClient by lazy { buildClient(Dns.SYSTEM) }
    val noRedirects: OkHttpClient by lazy {
        buildClient(DnsResolver.doh) { it.followRedirects(false).followSslRedirects(false) }
    }

    /**
     * Compatibility alias for old provider code that called `trustAll`. It is intentionally the
     * same fully validating client as [default]. Keeping the symbol lets us harden first and then
     * remove legacy `buildUnsafe()` APIs without leaving any path that disables TLS validation.
     */
    @Deprecated("Unsafe TLS is disabled in the CPZ hardened build")
    val trustAll: OkHttpClient get() = default

    fun newClient(
        dns: Dns = DnsResolver.doh,
        customizer: ((OkHttpClient.Builder) -> Unit)? = null,
    ): OkHttpClient = buildClient(dns) { builder ->
        builder.connectionPool(sharedPool).dispatcher(sharedDispatcher)
        customizer?.invoke(builder)
    }

    private fun buildClient(dns: Dns, customizer: ((OkHttpClient.Builder) -> Unit)? = null): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                val isCorsRequest = original.header("Sec-Fetch-Mode") == "cors" ||
                    original.header("Sec-Fetch-Dest") == "empty"
                if (original.header("User-Agent") == null) requestBuilder.header("User-Agent", USER_AGENT)
                if (original.header("Accept") == null) {
                    requestBuilder.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                }
                if (original.header("Accept-Language") == null) requestBuilder.header("Accept-Language", "it-IT,it;q=0.9,en-US;q=0.8,en;q=0.7")
                if (!isCorsRequest && original.header("Sec-Fetch-Dest") == null) requestBuilder.header("Sec-Fetch-Dest", "document")
                if (!isCorsRequest && original.header("Sec-Fetch-Mode") == null) requestBuilder.header("Sec-Fetch-Mode", "navigate")
                if (!isCorsRequest && original.header("Sec-Fetch-Site") == null) requestBuilder.header("Sec-Fetch-Site", "none")
                if (!isCorsRequest && original.header("Upgrade-Insecure-Requests") == null) requestBuilder.header("Upgrade-Insecure-Requests", "1")
                chain.proceed(requestBuilder.build())
            }
            .cookieJar(cookieJar)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .dns(dns)

        val secureTls = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2)
            .build()
        builder.connectionSpecs(listOf(secureTls, ConnectionSpec.CLEARTEXT))

        // Preserve compatibility with Let's Encrypt on old Android without bypassing PKI:
        // system roots remain trusted and the bundled ISRG Root X1 is added as one additional CA.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            try {
                val cf = CertificateFactory.getInstance("X.509")
                val isrgCert = ScraperApp.instance.resources.openRawResource(R.raw.isrg_root_x1).use {
                    cf.generateCertificate(it)
                }
                val tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm()
                val customStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
                    load(null, null)
                    setCertificateEntry("isrg_root_x1", isrgCert)
                }
                val customTmf = TrustManagerFactory.getInstance(tmfAlgorithm).apply { init(customStore) }
                val systemTmf = TrustManagerFactory.getInstance(tmfAlgorithm).apply { init(null as KeyStore?) }
                val systemTm = systemTmf.trustManagers.first { it is X509TrustManager } as X509TrustManager
                val customTm = customTmf.trustManagers.first { it is X509TrustManager } as X509TrustManager
                val combined = object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {
                        systemTm.checkClientTrusted(chain, authType)
                    }
                    override fun checkServerTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {
                        try {
                            systemTm.checkServerTrusted(chain, authType)
                        } catch (systemFailure: Exception) {
                            customTm.checkServerTrusted(chain, authType)
                        }
                    }
                    override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> =
                        systemTm.acceptedIssuers + customTm.acceptedIssuers
                }
                val sslContext = SSLContext.getInstance("TLS").apply {
                    init(null, arrayOf(combined), SecureRandom())
                }
                builder.sslSocketFactory(sslContext.socketFactory, combined)
            } catch (e: Exception) {
                Log.w(TAG, "Could not add legacy ISRG root; using platform TLS: ${e.message}")
            }
        }

        customizer?.invoke(builder)
        return builder.build()
    }
}
