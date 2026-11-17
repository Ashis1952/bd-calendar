package com.example.bdcalendar

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar

object Scheduler {
    const val CHANNEL = "alarm_channel_v1"

    fun triggerTime(e: Event): Long? {
        val now = System.currentTimeMillis()
        val startYear = maxOf(Calendar.getInstance().get(Calendar.YEAR), e.year)
        val count = if (e.yearly) 9 else 1
        val first = if (e.yearly) startYear else e.year
        for (i in 0 until count) {
            val c = Calendar.getInstance()
            c.clear()
            c.set(first + i, e.month - 1, e.day, e.hour, e.minute, 0)
            if (c.timeInMillis > now) return c.timeInMillis
        }
        return null
    }

    private fun pi(ctx: Context, e: Event): PendingIntent {
        val i = Intent(ctx, AlarmReceiver::class.java)
        i.putExtra("id", e.id)
        i.putExtra("title", e.title)
        i.putExtra("cat", e.category)
        return PendingIntent.getBroadcast(
            ctx, e.id.toInt(), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun schedule(ctx: Context, e: Event) {
        if (!e.alarmOn) return
        val t = triggerTime(e) ?: return
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val show = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        am.setAlarmClock(AlarmManager.AlarmClockInfo(t, show), pi(ctx, e))
    }

    fun cancel(ctx: Context, e: Event) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pi(ctx, e))
    }

    fun scheduleAll(ctx: Context) {
        for (e in Store.load(ctx)) schedule(ctx, e)
    }

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL) == null) {
                val ch = NotificationChannel(CHANNEL, "অ্যালার্ম", NotificationManager.IMPORTANCE_HIGH)
                val attr = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                ch.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), attr)
                ch.enableVibration(true)
                ch.vibrationPattern = longArrayOf(0, 800, 400, 800, 400, 800)
                ch.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                nm.createNotificationChannel(ch)
            }
        }
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val id = intent.getLongExtra("id", 0L)
        val title = intent.getStringExtra("title") ?: "রিমাইন্ডার"
        val cat = intent.getStringExtra("cat") ?: ""
        Scheduler.ensureChannel(ctx)
        val open = PendingIntent.getActivity(
            ctx, id.toInt(), Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(ctx, Scheduler.CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(cat)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(open)
            .setFullScreenIntent(open, true)
            .setVibrate(longArrayOf(0, 800, 400, 800, 400, 800))
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), AudioManager.STREAM_ALARM)
            .build()
        n.flags = n.flags or Notification.FLAG_INSISTENT
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(id.toInt(), n)

        val ev = Store.load(ctx).firstOrNull { it.id == id }
        if (ev != null && ev.yearly) Scheduler.schedule(ctx, ev)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Scheduler.scheduleAll(ctx)
    }
}
