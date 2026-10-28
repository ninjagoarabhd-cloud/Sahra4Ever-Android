package com.sahra4ever.app

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
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

    /**
     * ✅ (تحصين إضافي) محمية الآن ضد:
     * 1. JSON معطوب الصياغة بالكامل (JsonSyntaxException) → تُرجع قائمة فارغة
     *    بدل إسقاط التطبيق عند فتح الصفحة الرئيسية.
     * 2. عناصر فردية تالفة داخل القائمة (title/type فعلياً null رغم تعريف
     *    النوع في Kotlin، بسبب تجاوز Gson لنظام null-safety) → تُستبعد
     *    تلقائياً بدل أن تصل لشاشة الـ Navigation وتسبب كراشاً لاحقاً.
     */
    fun getHistory(context: Context): List<WatchHistory> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()

        val rawList: List<WatchHistory> = try {
            val type = object : TypeToken<List<WatchHistory>>() {}.type
            Gson().fromJson(json, type) ?: emptyList()
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Corrupted history JSON, clearing it", e)
            // تنظيف البيانات التالفة حتى لا تتكرر المشكلة في كل فتح
            prefs.edit().remove(KEY_HISTORY).apply()
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Unknown error reading history", e)
            emptyList()
        }

        // استبعاد أي عنصر وصلت فيه قيم غير صالحة فعلياً رغم تعريف النوع
        return rawList.filter { item ->
            @Suppress("SENSELESS_COMPARISON")
            (item.title as String?) != null &&
                (item.type as String?) != null &&
                item.id > 0
        }
    }
}