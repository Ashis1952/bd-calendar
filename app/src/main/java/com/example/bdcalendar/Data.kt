package com.example.bdcalendar

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Event(
    val id: Long,
    val title: String,
    val category: String,
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val alarmOn: Boolean,
    val yearly: Boolean
)

object Store {
    private const val PREF = "bdcal"
    private const val KEY = "events"

    fun load(ctx: Context): MutableList<Event> {
        val list = mutableListOf<Event>()
        val s = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        val arr = JSONArray(s)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                Event(
                    o.getLong("id"), o.getString("title"), o.getString("cat"),
                    o.getInt("y"), o.getInt("m"), o.getInt("d"),
                    o.getInt("h"), o.getInt("min"),
                    o.getBoolean("alarm"), o.getBoolean("yearly")
                )
            )
        }
        return list
    }

    fun save(ctx: Context, list: List<Event>) {
        val arr = JSONArray()
        for (e in list) {
            val o = JSONObject()
            o.put("id", e.id)
            o.put("title", e.title)
            o.put("cat", e.category)
            o.put("y", e.year)
            o.put("m", e.month)
            o.put("d", e.day)
            o.put("h", e.hour)
            o.put("min", e.minute)
            o.put("alarm", e.alarmOn)
            o.put("yearly", e.yearly)
            arr.put(o)
        }
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }
}

object Holidays {
    val fixed = listOf(
        Triple(2, 21, "শহীদ দিবস ও আন্তর্জাতিক মাতৃভাষা দিবস"),
        Triple(3, 26, "স্বাধীনতা ও জাতীয় দিবস"),
        Triple(4, 14, "পহেলা বৈশাখ"),
        Triple(5, 1, "মে দিবস"),
        Triple(12, 16, "বিজয় দিবস"),
        Triple(12, 25, "বড়দিন")
    )

    fun on(month: Int, day: Int): String? =
        fixed.firstOrNull { it.first == month && it.second == day }?.third
}

object Bn {
    val months = arrayOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )
    val days = arrayOf("রবি", "সোম", "মঙ্গল", "বুধ", "বৃহ", "শুক্র", "শনি")

    fun num(n: Int): String {
        val d = "০১২৩৪৫৬৭৮৯"
        return n.toString().map { d[it - '0'] }.joinToString("")
    }

    fun num2(n: Int): String = num(n / 10) + num(n % 10)
}
