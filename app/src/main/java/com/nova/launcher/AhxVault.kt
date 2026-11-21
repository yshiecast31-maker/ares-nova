package com.nova.launcher

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object AhxVault {
    private const val INFO = "ahx1-android-v1"
    private const val PREF = "ahx_vault_v1"

    fun rootFromHex(hex: String): ByteArray {
        val clean = hex.replace("\\s".toRegex(), "").lowercase()
        require(clean.matches(Regex("[0-9a-f]{64}"))) { "Root must be 64 hex chars" }
        return ByteArray(32) { i -> clean.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
    }

    fun generateRootHex(): String {
        val b = ByteArray(32)
        SecureRandom().nextBytes(b)
        return b.joinToString("") { "%02x".format(it) }
    }

    private fun hkdf(root: ByteArray, salt: ByteArray, info: String, len: Int = 32): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(root, "HmacSHA256"))
        mac.update(salt)
        val prk = mac.doFinal()
        mac.init(SecretKeySpec(prk, "HmacSHA256"))
        val result = ByteArray(len)
        var t = ByteArray(0)
        var offset = 0
        var counter = 1
        while (offset < len) {
            mac.reset()
            mac.update(t)
            mac.update(info.toByteArray(Charsets.UTF_8))
            mac.update(counter.toByte())
            t = mac.doFinal()
            val copy = minOf(t.size, len - offset)
            System.arraycopy(t, 0, result, offset, copy)
            offset += copy
            counter++
        }
        return result
    }

    fun seal(root: ByteArray, plaintext: String): String {
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        val keyBytes = hkdf(root, iv, INFO, 32)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(128, iv))
        val ct = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return "ahx1-android:v1." + b64(iv) + "." + b64(ct)
    }

    fun open(root: ByteArray, token: String): String {
        require(token.startsWith("ahx1-android:v1.")) { "Bad token" }
        val parts = token.removePrefix("ahx1-android:v1.").split(".")
        require(parts.size == 2) { "Bad token parts" }
        val iv = unb64(parts[0])
        val ct = unb64(parts[1])
        val keyBytes = hkdf(root, iv, INFO, 32)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(ct), Charsets.UTF_8)
    }

    fun saveNote(ctx: Context, id: String, sealed: String) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString("note_$id", sealed).apply()
    }

    fun loadNote(ctx: Context, id: String): String? =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("note_$id", null)

    fun listNoteIds(ctx: Context): List<String> =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).all.keys
            .filter { it.startsWith("note_") }.map { it.removePrefix("note_") }.sorted()

    private fun b64(b: ByteArray) =
        Base64.encodeToString(b, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    private fun unb64(s: String) =
        Base64.decode(s, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
}
