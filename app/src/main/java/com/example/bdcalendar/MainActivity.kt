package com.example.bdcalendar

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private var year = 0
    private var month = 0
    private var events = mutableListOf<Event>()
    private lateinit var root: LinearLayout
    private val red = Color.parseColor("#C62828")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val now = Calendar.getInstance()
        year = now.get(Calendar.YEAR)
        month = now.get(Calendar.MONTH) + 1
        events = Store.load(this)
        Scheduler.ensureChannel(this)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        val sv = ScrollView(this)
        root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(12), dp(12), dp(12), dp(24))
        sv.addView(root)
        setContentView(sv)
        render()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun icon(cat: String): String = when (cat) {
        "জন্মদিন" -> "🎂"
        "বার্ষিকী" -> "💐"
        "স্কুল/কলেজ ছুটি" -> "🏫"
        else -> "📌"
    }

    private fun inMonth(e: Event): Boolean =
        e.month == month && (e.year == year || (e.yearly && year >= e.year))

    private fun eventsOn(d: Int): List<Event> = events.filter { inMonth(it) && it.day == d }

    private fun changeMonth(delta: Int) {
        month += delta
        if (month > 12) { month = 1; year++ }
        if (month < 1) { month = 12; year-- }
        render()
    }

    private fun cellParams(h: Int): LinearLayout.LayoutParams {
        val p = LinearLayout.LayoutParams(0, dp(h), 1f)
        p.setMargins(dp(2), dp(2), dp(2), dp(2))
        return p
    }

    private fun render() {
        root.removeAllViews()

        val head = LinearLayout(this)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL
        val prev = Button(this)
        prev.text = "◀"
        prev.setOnClickListener { changeMonth(-1) }
        val next = Button(this)
        next.text = "▶"
        next.setOnClickListener { changeMonth(1) }
        val title = TextView(this)
        title.text = Bn.months[month - 1] + " " + Bn.num(year)
        title.textSize = 20f
        title.typeface = Typeface.DEFAULT_BOLD
        title.gravity = Gravity.CENTER
        head.addView(prev)
        head.addView(title, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        head.addView(next)
        root.addView(head)

        val hdr = LinearLayout(this)
        for (i in 0..6) {
            val tv = TextView(this)
            tv.text = Bn.days[i]
            tv.gravity = Gravity.CENTER
            tv.textSize = 13f
            tv.typeface = Typeface.DEFAULT_BOLD
            tv.setTextColor(if (i >= 5) red else Color.DKGRAY)
            hdr.addView(tv, cellParams(32))
        }
        root.addView(hdr)

        val first = Calendar.getInstance()
        first.clear()
        first.set(year, month - 1, 1)
        val offset = first.get(Calendar.DAY_OF_WEEK) - 1
        val dim = first.getActualMaximum(Calendar.DAY_OF_MONTH)
        var d = 1 - offset
        while (d <= dim) {
            val row = LinearLayout(this)
            for (i in 0..6) {
                if (d < 1 || d > dim) {
                    row.addView(TextView(this), cellParams(52))
                } else {
                    row.addView(dayCell(d, i), cellParams(52))
                }
                d++
            }
            root.addView(row)
        }

        val hint = TextView(this)
        hint.text = "যেকোনো তারিখে ট্যাপ করলে নতুন রিমাইন্ডার যোগ করা যাবে"
        hint.textSize = 12f
        hint.setPadding(0, dp(8), 0, 0)
        root.addView(hint)

        val listTitle = TextView(this)
        listTitle.text = Bn.months[month - 1] + " মাসের তালিকা"
        listTitle.textSize = 18f
        listTitle.typeface = Typeface.DEFAULT_BOLD
        listTitle.setPadding(0, dp(16), 0, dp(4))
        root.addView(listTitle)

        val items = mutableListOf<Triple<Int, String, Event?>>()
        for (h in Holidays.fixed) {
            if (h.first == month) items.add(Triple(h.second, "🔴 " + h.third + " (সরকারি ছুটি)", null))
        }
        for (e in events) {
            if (inMonth(e)) {
                var s = icon(e.category) + " " + e.title + " (" + e.category + ")"
                if (e.alarmOn) s += " ⏰ " + Bn.num2(e.hour) + ":" + Bn.num2(e.minute)
                items.add(Triple(e.day, s, e))
            }
        }
        items.sortBy { it.first }
        if (items.isEmpty()) {
            val none = TextView(this)
            none.text = "এই মাসে কিছু নেই"
            root.addView(none)
        }
        for (item in items) {
            val row = LinearLayout(this)
            row.gravity = Gravity.CENTER_VERTICAL
            row.setPadding(0, dp(4), 0, dp(4))
            val t = TextView(this)
            t.text = Bn.num(item.first) + " " + Bn.months[month - 1] + " — " + item.second
            t.textSize = 15f
            row.addView(t, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            val ev = item.third
            if (ev != null) {
                val del = Button(this)
                del.text = "মুছুন"
                del.setOnClickListener { confirmDelete(ev) }
                row.addView(del)
            }
            root.addView(row)
        }

        val test = Button(this)
        test.text = "🔔 টেস্ট অ্যালার্ম (প্রায় ১ মিনিট পরে)"
        test.setOnClickListener {
            val c = Calendar.getInstance()
            c.add(Calendar.MINUTE, 1)
            val te = Event(
                999L, "টেস্ট অ্যালার্ম", "পরীক্ষা",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH),
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true, false
            )
            Scheduler.schedule(this, te)
            Toast.makeText(this, "প্রায় ১ মিনিট পরে অ্যালার্ম বাজবে", Toast.LENGTH_LONG).show()
        }
        root.addView(test)
    }

    private fun dayCell(d: Int, col: Int): TextView {
        val tv = TextView(this)
        tv.gravity = Gravity.CENTER
        val hol = Holidays.on(month, d)
        var label = Bn.num(d)
        if (eventsOn(d).isNotEmpty()) label += "\n•"
        tv.text = label
        tv.textSize = 15f
        tv.setTextColor(if (hol != null || col >= 5) red else Color.parseColor("#212121"))
        val bg = GradientDrawable()
        bg.cornerRadius = dp(8).toFloat()
        bg.setColor(if (hol != null) Color.parseColor("#FFEBEE") else Color.TRANSPARENT)
        val today = Calendar.getInstance()
        if (today.get(Calendar.YEAR) == year && today.get(Calendar.MONTH) + 1 == month &&
            today.get(Calendar.DAY_OF_MONTH) == d
        ) {
            bg.setStroke(dp(2), Color.parseColor("#1565C0"))
        }
        tv.background = bg
        tv.setOnClickListener { showAdd(d) }
        return tv
    }

    private fun confirmDelete(e: Event) {
        AlertDialog.Builder(this)
            .setMessage("\"" + e.title + "\" মুছে ফেলবেন?")
            .setPositiveButton("হ্যাঁ") { _, _ ->
                Scheduler.cancel(this, e)
                events.removeAll { it.id == e.id }
                Store.save(this, events)
                render()
            }
            .setNegativeButton("না", null)
            .show()
    }

    private fun showAdd(day: Int) {
        var y = year
        var m = month
        var dd = day
        var h = 8
        var mi = 0
        val cats = arrayOf("জন্মদিন", "বার্ষিকী", "স্কুল/কলেজ ছুটি", "অন্যান্য")

        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(20), dp(8), dp(20), dp(8))
        val et = EditText(this)
        et.hint = "শিরোনাম (যেমন: রাফির জন্মদিন)"
        val sp = Spinner(this)
        sp.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, cats)
        val dateBtn = Button(this)
        val timeBtn = Button(this)
        val alarmCb = CheckBox(this)
        alarmCb.text = "অ্যালার্ম বাজবে"
        alarmCb.isChecked = true
        val yearlyCb = CheckBox(this)
        yearlyCb.text = "প্রতি বছর পুনরাবৃত্তি"
        yearlyCb.isChecked = true

        fun refresh() {
            dateBtn.text = "📅 " + Bn.num(dd) + " " + Bn.months[m - 1] + " " + Bn.num(y)
            timeBtn.text = "⏰ " + Bn.num2(h) + ":" + Bn.num2(mi)
        }
        refresh()
        dateBtn.setOnClickListener {
            DatePickerDialog(this, { _, yy, mm, d2 ->
                y = yy
                m = mm + 1
                dd = d2
                refresh()
            }, y, m - 1, dd).show()
        }
        timeBtn.setOnClickListener {
            TimePickerDialog(this, { _, hh, mn ->
                h = hh
                mi = mn
                refresh()
            }, h, mi, false).show()
        }
        box.addView(et)
        box.addView(sp)
        box.addView(dateBtn)
        box.addView(timeBtn)
        box.addView(alarmCb)
        box.addView(yearlyCb)

        AlertDialog.Builder(this)
            .setTitle("নতুন যোগ করুন")
            .setView(box)
            .setPositiveButton("সেভ") { _, _ ->
                val name = et.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "শিরোনাম লিখুন", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val ev = Event(
                    System.currentTimeMillis(), name, cats[sp.selectedItemPosition],
                    y, m, dd, h, mi, alarmCb.isChecked, yearlyCb.isChecked
                )
                events.add(ev)
                Store.save(this, events)
                Scheduler.schedule(this, ev)
                year = y
                month = m
                render()
                if (ev.alarmOn && Scheduler.triggerTime(ev) == null) {
                    Toast.makeText(this, "এই সময় পেরিয়ে গেছে, অ্যালার্ম বাজবে না", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("বাতিল", null)
            .show()
    }
}
