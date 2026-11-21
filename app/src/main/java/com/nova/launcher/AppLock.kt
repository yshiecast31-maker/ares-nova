package com.nova.launcher

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** App-level PIN for the launcher only — not the system lock screen. */
object AppLock {
    private const val PREF = "ares_lock_v1"
    private const val ITER = 120_000

    fun isEnabled(ctx: Context) =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).contains("hash")

    fun setPin(ctx: Context, pin: String) {
        require(pin.length in 4..12) { "PIN must be 4–12 characters" }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = derive(pin, salt)
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString("salt", b64(salt))
            .putString("hash", b64(hash))
            .apply()
    }

    fun verify(ctx: Context, pin: String): Boolean {
        val p = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val saltB = p.getString("salt", null) ?: return false
        val hashB = p.getString("hash", null) ?: return false
        return MessageDigest.isEqual(unb64(hashB), derive(pin, unb64(saltB)))
    }

    fun clear(ctx: Context) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITER, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private fun b64(b: ByteArray) = Base64.encodeToString(b, Base64.NO_WRAP)
    private fun unb64(s: String) = Base64.decode(s, Base64.NO_WRAP)
}
