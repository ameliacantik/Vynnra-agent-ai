package lol.vynnra.agent.platform

import android.content.Context
import android.util.Base64
import lol.vynnra.agent.core.provider.ProviderConfig
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class ProviderCredentialStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun load(): ProviderConfig = ProviderConfig(
        baseUrl = preferences.getString(KEY_BASE_URL, "") ?: "",
        apiKey = decrypt(preferences.getString(KEY_API_CIPHERTEXT, null), preferences.getString(KEY_IV, null)),
        model = preferences.getString(KEY_MODEL, "") ?: ""
    )

    fun save(config: ProviderConfig) {
        val encrypted = encrypt(config.apiKey.trim())
        preferences.edit()
            .putString(KEY_BASE_URL, config.baseUrl.trim().trimEnd('/'))
            .putString(KEY_MODEL, config.model.trim())
            .putString(KEY_CIPHERTEXT, encrypted.ciphertext)
            .putString(KEY_IV, encrypted.iv)
            .apply()
    }

    fun clearApiKey() {
        preferences.edit().remove(KEY_CIPHERTEXT).remove(KEY_IV).apply()
    }

    private fun encrypt(value: String): EncryptedValue {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        return EncryptedValue(
            ciphertext = Base64.encodeToString(cipher.doFinal(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP),
            iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        )
    }

    private fun decrypt(ciphertext: String?, iv: String?): String {
        if (ciphertext.isNullOrBlank() || iv.isNullOrBlank()) return ""
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(GCM_TAG_BITS, Base64.decode(iv, Base64.NO_WRAP))
            )
            String(cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP)), Charsets.UTF_8)
        }.getOrDefault("")
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyPropertiesCompat.ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(KeyGenSpecFactory.create())
        return generator.generateKey()
    }

    private data class EncryptedValue(val ciphertext: String, val iv: String)

    private companion object {
        const val PREFERENCES = "vynnra_provider"
        const val KEY_BASE_URL = "base_url"
        const val KEY_MODEL = "model"
        const val KEY_CIPHERTEXT = "api_key_ciphertext"
        const val KEY_IV = "api_key_iv"
        const val KEY_ALIAS = "vynnra_provider_api_key"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
    }
}

private object KeyPropertiesCompat {
    const val ALGORITHM_AES = "AES"
    const val PURPOSE_ENCRYPT = 1
    const val PURPOSE_DECRYPT = 2
    const val BLOCK_MODE_GCM = "GCM"
    const val PADDING_NONE = "NoPadding"
}

private object KeyGenSpecFactory {
    fun create(): android.security.keystore.KeyGenParameterSpec =
        android.security.keystore.KeyGenParameterSpec.Builder(
            "vynnra_provider_api_key",
            KeyPropertiesCompat.PURPOSE_ENCRYPT or KeyPropertiesCompat.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyPropertiesCompat.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyPropertiesCompat.PADDING_NONE)
            .build()
}
