package com.sahra4ever.app

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class WatchHistory(
    val id: Int,
    val title: String,
    val type: String, // movie or tv
    val posterPath: String?,
    val season: Int = 1,
    val episode: Int = 1
)

object HistoryManager {
    private const val PREF_NAME = "sahra_history"
    private const val KEY_HISTORY = "history_list"

    // حفظ عمل جديد في القائمة
    fun addToHistory(context: Context, item: WatchHistory) {
        val list = getHistory(context).toMutableList()
        // إزالة العمل القديم إذا كان موجوداً (لكي نضعه في الأعلى)
        list.removeAll { it.id == item.id && it.type == item.type }
        // إضافته في بداية القائمة
        list.add(0, item)
        // الاحتفاظ بآخر 10 أعمال فقط
        val finalList = list.take(10)
        
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_HISTORY, Gson().toJson(finalList)).apply()
    }

    // استرجاع القائمة
    fun getHistory(context: Context): List<WatchHistory> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        val type = object : TypeToken<List<WatchHistory>>() {}.type
        return Gson().fromJson(json, type)
    }
}