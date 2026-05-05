package com.example.booksopdsapp

import android.content.Context
import android.provider.Settings
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class SavedOpdsProfile(
    val name: String,
    val url: String,
    val username: String,
    val password: String
)

private data class EncryptedPassword(
    val encryptedBase64: String,
    val saltBase64: String,
    val ivBase64: String
)

fun loadProfiles(context: Context): List<SavedOpdsProfile> {
    val prefs = context.getSharedPreferences("opds_profiles", Context.MODE_PRIVATE)
    val raw = prefs.getString("profiles_json", "[]") ?: "[]"
    return runCatching {
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                add(
                    SavedOpdsProfile(
                        name = obj.optString("name", ""),
                        url = obj.optString("url", ""),
                        username = obj.optString("username", ""),
                        password = when {
                            obj.has("password_enc") && obj.has("password_salt") && obj.has("password_iv") -> {
                                decryptPassword(
                                    context = context,
                                    encryptedBase64 = obj.optString("password_enc", ""),
                                    saltBase64 = obj.optString("password_salt", ""),
                                    ivBase64 = obj.optString("password_iv", "")
                                )
                            }
                            else -> obj.optString("password", "")
                        }
                    )
                )
            }
        }.filter { it.name.isNotBlank() && it.url.isNotBlank() }
    }.getOrDefault(emptyList())
}

fun saveProfiles(context: Context, profiles: List<SavedOpdsProfile>) {
    val arr = JSONArray()
    profiles.forEach { profile ->
        val encrypted = encryptPassword(context, profile.password)
        arr.put(
            JSONObject()
                .put("name", profile.name)
                .put("url", profile.url)
                .put("username", profile.username)
                .put("password_enc", encrypted.encryptedBase64)
                .put("password_salt", encrypted.saltBase64)
                .put("password_iv", encrypted.ivBase64)
        )
    }
    context.getSharedPreferences("opds_profiles", Context.MODE_PRIVATE)
        .edit()
        .putString("profiles_json", arr.toString())
        .apply()
}

fun upsertProfile(
    profiles: List<SavedOpdsProfile>,
    profile: SavedOpdsProfile
): List<SavedOpdsProfile> {
    val index = profiles.indexOfFirst { it.name.equals(profile.name, ignoreCase = true) }
    if (index < 0) return profiles + profile
    return profiles.toMutableList().apply { set(index, profile) }
}

fun exportProfilesJson(profiles: List<SavedOpdsProfile>): String {
    val arr = JSONArray()
    profiles.forEach { profile ->
        arr.put(
            JSONObject()
                .put("name", profile.name)
                .put("url", profile.url)
                .put("username", profile.username)
                .put("password", profile.password)
        )
    }
    return arr.toString(2)
}

fun importProfilesJson(raw: String): List<SavedOpdsProfile> {
    return runCatching {
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val profile = SavedOpdsProfile(
                    name = obj.optString("name", "").trim(),
                    url = obj.optString("url", "").trim(),
                    username = obj.optString("username", "").trim(),
                    password = obj.optString("password", "")
                )
                if (profile.name.isNotBlank() && profile.url.isNotBlank()) {
                    add(profile)
                }
            }
        }
    }.getOrDefault(emptyList())
}

private fun encryptPassword(context: Context, plainPassword: String): EncryptedPassword {
    if (plainPassword.isEmpty()) {
        return EncryptedPassword("", "", "")
    }
    val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
    val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
    val key = deriveAesKey(context, salt)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
    val encryptedBytes = cipher.doFinal(plainPassword.toByteArray(Charsets.UTF_8))
    return EncryptedPassword(
        encryptedBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP),
        saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP),
        ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
    )
}

private fun decryptPassword(
    context: Context,
    encryptedBase64: String,
    saltBase64: String,
    ivBase64: String
): String {
    if (encryptedBase64.isBlank() || saltBase64.isBlank() || ivBase64.isBlank()) return ""
    return runCatching {
        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
        val encrypted = Base64.decode(encryptedBase64, Base64.NO_WRAP)
        val key = deriveAesKey(context, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        val plain = cipher.doFinal(encrypted)
        plain.toString(Charsets.UTF_8)
    }.getOrDefault("")
}

private fun deriveAesKey(context: Context, salt: ByteArray): SecretKeySpec {
    val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    val seed = "${context.packageName}:$androidId"
    val spec = PBEKeySpec(seed.toCharArray(), salt, 120_000, 256)
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val keyBytes = factory.generateSecret(spec).encoded
    return SecretKeySpec(keyBytes, "AES")
}
