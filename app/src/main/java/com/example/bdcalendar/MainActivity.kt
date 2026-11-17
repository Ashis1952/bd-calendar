package com.example.bdcalendar

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this)
        tv.text = "বাংলাদেশ ক্যালেন্ডার\nপ্রথম ভার্সন কাজ করছে!"
        tv.textSize = 24f
        tv.setPadding(48, 96, 48, 48)
        setContentView(tv)
    }
}
