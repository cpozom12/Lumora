package com.lumora.pairing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.util.EnumMap

/**
 * CPZ hardened baseline.
 *
 * The upstream quick-pair feature accepted provider credentials through a temporary cleartext
 * HTTP server on the local network. A random token and short expiry limited unauthorized use but
 * did not provide transport confidentiality. Credential-bearing LAN pairing therefore remains
 * disabled until it is replaced with an authenticated encrypted protocol.
 *
 * The generic QR encoder is retained because non-secret external login URLs (for example a Plex
 * account authorization page) can still be displayed safely without opening a local server.
 */
class QrPairingManager(@Suppress("UNUSED_PARAMETER") private val context: Context) {

    var onProviderReceived: ((type: String, formData: Map<String, String>) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onServerReady: ((url: String, port: Int) -> Unit)? = null
    var onJellyfinQuickConnect: (suspend (serverUrl: String) -> QuickConnectStart)? = null
    var onPlexPinLogin: (suspend () -> PlexPinStart)? = null

    data class QuickConnectStart(val code: String?, val secret: String?, val error: String?)
    data class PlexPinStart(val code: String?, val authUrl: String?, val error: String?)

    data class PairingResult(
        val url: String,
        val qrBitmap: Bitmap,
        val expiresAtMs: Long,
        val ipAddress: String,
        val port: Int
    )

    val result: PairingResult? get() = null

    suspend fun start(@Suppress("UNUSED_PARAMETER") providerType: String? = null): PairingResult? {
        onError?.invoke("LAN credential pairing is disabled in the hardened build")
        return null
    }

    fun stop() {
        // No socket/server exists in the hardened build.
    }

    companion object {
        private const val QR_SIZE = 512

        /** Encode a non-secret URL/string as a QR bitmap without opening any network listener. */
        fun createQrBitmap(value: String): Bitmap {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.MARGIN, 2)
                put(EncodeHintType.ERROR_CORRECTION, com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M)
            }
            val matrix = QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints)
            val pixels = IntArray(QR_SIZE * QR_SIZE)
            for (y in 0 until QR_SIZE) {
                for (x in 0 until QR_SIZE) {
                    pixels[y * QR_SIZE + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
                }
            }
            return Bitmap.createBitmap(QR_SIZE, QR_SIZE, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, QR_SIZE, 0, 0, QR_SIZE, QR_SIZE)
            }
        }
    }
}
