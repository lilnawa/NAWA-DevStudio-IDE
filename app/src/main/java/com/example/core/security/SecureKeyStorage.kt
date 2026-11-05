package com.example.core.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureKeyStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("nawa_secure_prefs", Context.MODE_PRIVATE)
    private val keyAlias = "NawaDevStudioMasterKey"
    private val providerName = "AndroidKeyStore"
    private val transformation = "AES/GCM/NoPadding"

    init {
        ensureMasterKey()
    }

    private fun ensureMasterKey() {
        try {
            val keyStore = KeyStore.getInstance(providerName).apply { load(null) }
            if (!keyStore.containsAlias(keyAlias)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, providerName)
                val spec = KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
        } catch (_: Exception) {
            // Handled gracefully
        }
    }

    private fun getSecretKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(providerName).apply { load(null) }
            val entry = keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
            entry?.secretKey
        } catch (_: Exception) {
            null
        }
    }

    fun saveApiKey(providerId: String, apiKey: String) {
        if (apiKey.isBlank()) {
            removeApiKey(providerId)
            return
        }

        try {
            val key = getSecretKey()
            if (key != null) {
                val cipher = Cipher.getInstance(transformation)
                cipher.init(Cipher.ENCRYPT_MODE, key)
                val iv = cipher.iv
                val encrypted = cipher.doFinal(apiKey.toByteArray(Charsets.UTF_8))
                val encodedIv = Base64.encodeToString(iv, Base64.NO_WRAP)
                val encodedCipher = Base64.encodeToString(encrypted, Base64.NO_WRAP)
                prefs.edit().putString("key_$providerId", "$encodedIv:$encodedCipher").apply()
            } else {
                // Obfuscated fallback if Keystore is unavailable in test environment
                val obfuscated = Base64.encodeToString(apiKey.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                prefs.edit().putString("obf_$providerId", obfuscated).apply()
            }
        } catch (e: Exception) {
            val obfuscated = Base64.encodeToString(apiKey.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            prefs.edit().putString("obf_$providerId", obfuscated).apply()
        }
    }

    fun getApiKey(providerId: String): String {
        try {
            val stored = prefs.getString("key_$providerId", null)
            if (stored != null && stored.contains(":")) {
                val parts = stored.split(":")
                val iv = Base64.decode(parts[0], Base64.NO_WRAP)
                val cipherText = Base64.decode(parts[1], Base64.NO_WRAP)
                val key = getSecretKey()
                if (key != null) {
                    val cipher = Cipher.getInstance(transformation)
                    val spec = GCMParameterSpec(128, iv)
                    cipher.init(Cipher.DECRYPT_MODE, key, spec)
                    val decrypted = cipher.doFinal(cipherText)
                    return String(decrypted, Charsets.UTF_8)
                }
            }

            val obf = prefs.getString("obf_$providerId", null)
            if (obf != null) {
                return String(Base64.decode(obf, Base64.NO_WRAP), Charsets.UTF_8)
            }
        } catch (_: Exception) {
            // Return empty if decryption fails or key corrupted
        }
        return ""
    }

    fun removeApiKey(providerId: String) {
        prefs.edit().remove("key_$providerId").remove("obf_$providerId").apply()
    }

    fun hasApiKey(providerId: String): Boolean {
        return getApiKey(providerId).isNotBlank()
    }
}
