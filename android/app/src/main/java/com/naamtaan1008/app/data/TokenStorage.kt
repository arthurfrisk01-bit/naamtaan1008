package com.naamtaan1008.app.data

import android.content.Context
import android.content.SharedPreferences

/**
 * 社区登录态持久化。JWT 存 SharedPreferences，登录/登出统一走这里。
 * 单例，进程内共享。
 */
object TokenStorage {
    private const val PREFS = "naamtaan1008_community"
    private const val KEY_TOKEN = "jwt"
    private const val KEY_EMAIL = "email"

    @Volatile
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        }
    }

    private fun sp(): SharedPreferences {
        val p = prefs
        requireNotNull(p) { "TokenStorage 未初始化，请先调用 init(context)" }
        return p
    }

    fun token(): String = sp().getString(KEY_TOKEN, "").orEmpty()

    fun email(): String = sp().getString(KEY_EMAIL, "").orEmpty()

    fun isLoggedIn(): Boolean = token().isNotEmpty()

    fun save(token: String, email: String) {
        sp().edit().putString(KEY_TOKEN, token).putString(KEY_EMAIL, email).apply()
    }

    fun clear() {
        sp().edit().remove(KEY_TOKEN).remove(KEY_EMAIL).apply()
    }
}
