package com.bastion.app.data.crypto

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecretsStore(context: Context) {

    private val masterKey = MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "bastion_secrets",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun savePassword(hostId: Long, password: String) {
        prefs.edit().putString("host_${hostId}_password", password).apply()
    }

    fun getPassword(hostId: Long): String? {
        return prefs.getString("host_${hostId}_password", null)
    }

    fun savePrivateKey(hostId: Long, keyPem: String, passphrase: String?) {
        prefs.edit()
            .putString("host_${hostId}_private_key", keyPem)
            .putString("host_${hostId}_passphrase", passphrase)
            .apply()
    }

    fun getPrivateKey(hostId: Long): Triple<String, String?, String?>? {
        val key = prefs.getString("host_${hostId}_private_key", null) ?: return null
        val passphrase = prefs.getString("host_${hostId}_passphrase", null)
        return Triple(key, passphrase, null)
    }

    fun deleteSecret(hostId: Long) {
        prefs.edit()
            .remove("host_${hostId}_password")
            .remove("host_${hostId}_private_key")
            .remove("host_${hostId}_passphrase")
            .apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
