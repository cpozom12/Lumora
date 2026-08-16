package com.lumora.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Small, dependency-free envelope for secrets persisted by the app.
 *
 * The AES key is generated inside AndroidKeyStore and is not exportable. Persisted values contain
 * only a version marker, a random 96-bit IV and AES-GCM ciphertext+authentication tag. No fallback
 * to plaintext is permitted when encryption fails.
 *
 * Plain values are accepted on read only to support one-time migration from the upstream format;
 * callers are expected to immediately save the decoded model again, which rewrites secrets as
 * encrypted envelopes.
 */
object SecureValueStore {
    private const val KEY_ALIAS = "cpz_lumora_secrets_v1"
    private const val PREFIX = "enc:v1:"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128

    fun isEncrypted(value: String?): Boolean = value?.startsWith(PREFIX) == true

    fun encrypt(value: String?): String? {
        if (value.isNullOrEmpty()) return value
        if (isEncrypted(value)) return value

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val ciphertext = Base64.encodeToString(
            cipher.doFinal(value.toByteArray(Charsets.UTF_8)),
            Base64.NO_WRAP
        )
        return "$PREFIX$iv:$ciphertext"
    }

    /**
     * Returns plaintext for legacy values to support migration. Encrypted values fail closed:
     * tampering, a missing key, or an authentication-tag mismatch returns null rather than the
     * ciphertext or a guessed value.
     */
    fun decrypt(value: String?): String? {
        if (value.isNullOrEmpty()) return value
        if (!isEncrypted(value)) return value

        return runCatching {
            val payload = value.removePrefix(PREFIX)
            val separator = payload.indexOf(':')
            require(separator > 0 && separator < payload.lastIndex) { "Malformed encrypted value" }
            val iv = Base64.decode(payload.substring(0, separator), Base64.NO_WRAP)
            val ciphertext = Base64.decode(payload.substring(separator + 1), Base64.NO_WRAP)
            require(iv.size == 12) { "Unexpected GCM IV length" }

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getExistingKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        }.getOrNull()
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun getExistingKey(): SecretKey {
        val entry = keyStore().getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            ?: throw IllegalStateException("Secret key is unavailable")
        return entry.secretKey
    }

    @Synchronized
    private fun getOrCreateKey(): SecretKey {
        val ks = keyStore()
        (ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }
}
