package com.sahra4ever.app

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken

data class WatchHistory(
    val id: Int,
    val title: String,
    val type: String,
    val posterPath: String?,
    val season: Int = 1,
    val episode: Int = 1
)

object HistoryManager {
    private const val TAG = "HistoryManager"
    private const val PREF_NAME = "sahra_history"
    private const val KEY_HISTORY = "history_list"

    fun addToHistory(context: Context, item: WatchHistory) {
        val list = getHistory(context).toMutableList()
        list.removeAll { it.id == item.id && it.type == item.type }
        list.add(0, item)
        val finalList = list.take(10)
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_HISTORY, Gson().toJson(finalList)).apply()
    }

    fun getHistory(context: Context): List<WatchHistory> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()

        // ✅ استخدام List<WatchHistory?> للتعامل مع العناصر الفارغة
        val rawList: List<WatchHistory?> = try {
            val type = object : TypeToken<List<WatchHistory?>>() {}.type
            Gson().fromJson(json, type) ?: emptyList()
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Corrupted JSON", e)
            prefs.edit().remove(KEY_HISTORY).apply()
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        // ✅ filterNotNull() يحذف العناصر الفارغة، ويمنع الانهيار
        return rawList.filterNotNull().filter { item ->
            @Suppress("SENSELESS_COMPARISON")
            (item.title as String?) != null &&
                (item.type as String?) != null &&
                item.id > 0
        }
    }
}