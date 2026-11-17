package com.example.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

/**
 * 계정 정보(이름·이메일)를 기기에 저장합니다.
 * 지금 로그인/회원가입 화면은 Firebase 계정과 아직 연결되지 않아서,
 * 로그인할 때 입력한 이메일과 계정 정보 화면에서 정한 이름을 이 기기에만 저장합니다.
 */
object AccountStore {
    private const val PREFS = "tripmate_account"
    private const val KEY_EMAIL = "email"
    private const val KEY_NAME = "name"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun email(context: Context): String =
        prefs(context).getString(KEY_EMAIL, null)
            ?: runCatching { FirebaseAuth.getInstance().currentUser?.email }.getOrNull()
            ?: ""

    /** 이름이 없으면 이메일 앞부분을 이름처럼 보여 줍니다. */
    fun name(context: Context): String =
        prefs(context).getString(KEY_NAME, null)?.takeIf { it.isNotBlank() }
            ?: email(context).substringBefore("@")

    fun saveLogin(context: Context, email: String) {
        val p = prefs(context)
        val before = p.getString(KEY_EMAIL, null)
        p.edit().apply {
            putString(KEY_EMAIL, email)
            if (before != null && before != email) remove(KEY_NAME) // 다른 계정이면 이름 초기화
        }.apply()
    }

    fun saveName(context: Context, name: String) {
        prefs(context).edit().putString(KEY_NAME, name.trim()).apply()
    }

    /**
     * 로그아웃.
     * 익명 로그인(테스트용)은 로그아웃하면 그 계정으로 만든 여행에 다시 들어갈 수 없어서 유지하고,
     * 실제 Firebase 계정으로 로그인한 경우에만 Firebase 에서도 로그아웃합니다.
     */
    fun logout(context: Context) {
        prefs(context).edit().remove(KEY_EMAIL).apply()
        runCatching {
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser?.isAnonymous == false) auth.signOut()
        }
    }
}
