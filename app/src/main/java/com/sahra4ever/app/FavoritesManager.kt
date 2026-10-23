package com.sahra4ever.app

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** عنصر مفضّل واحد (فيلم أو مسلسل). */
data class FavoriteItem(
    val id: Int,
    val title: String,
    val type: String, // movie or tv
    val posterPath: String?
)

/**
 * ✅ نظام المفضلة الكامل.
 * مبني على StateFlow (وليس قراءة/كتابة مباشرة فقط من SharedPreferences) حتى تتحدّث
 * أيقونة "تمت الإضافة ✓" فوراً وتلقائياً في جميع الشاشات المفتوحة في نفس اللحظة
 * (الرئيسية + التفاصيل + قائمتي) بمجرد الضغط من أي واحدة منها.
 */
object FavoritesManager {
    private const val PREF_NAME = "sahra_favorites"
    private const val KEY_FAVORITES = "favorites_list"

    private val _favorites = MutableStateFlow<List<FavoriteItem>>(emptyList())
    val favorites: StateFlow<List<FavoriteItem>> = _favorites.asStateFlow()

    @Volatile
    private var initialized = false

    /** يجب استدعاؤها مرة واحدة على الأقل (آمنة للاستدعاء المتكرر) قبل استخدام StateFlow. */
    fun init(context: Context) {
        if (initialized) return
        initialized = true
        _favorites.value = loadFromPrefs(context)
    }

    private fun loadFromPrefs(context: Context): List<FavoriteItem> {
        return try {
            val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_FAVORITES, null) ?: return emptyList()
            val type = object : TypeToken<List<FavoriteItem>>() {}.type
            Gson().fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun persist(context: Context, list: List<FavoriteItem>) {
        val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_FAVORITES, Gson().toJson(list)).apply()
    }

    /** هل العنصر موجود حالياً في المفضلة؟ (فحص لحظي بدون Compose) */
    fun isFavorite(id: Int, type: String): Boolean =
        _favorites.value.any { it.id == id && it.type == type }

    /** إضافة/إزالة (Toggle) — هذا ما تستخدمه الأزرار في الواجهة. */
    fun toggle(context: Context, item: FavoriteItem) {
        init(context)
        val current = _favorites.value.toMutableList()
        val exists = current.any { it.id == item.id && it.type == item.type }
        if (exists) {
            current.removeAll { it.id == item.id && it.type == item.type }
        } else {
            current.add(0, item)
        }
        _favorites.value = current
        persist(context, current)
    }

    /** إزالة صريحة (تُستخدم في زر الحذف داخل شاشة قائمتي). */
    fun remove(context: Context, id: Int, type: String) {
        init(context)
        val current = _favorites.value.toMutableList()
        current.removeAll { it.id == id && it.type == type }
        _favorites.value = current
        persist(context, current)
    }

    fun getFavorites(context: Context): List<FavoriteItem> {
        init(context)
        return _favorites.value
    }
}