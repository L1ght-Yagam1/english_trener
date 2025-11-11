package com.example.lab5

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.AlarmManagerCompat

class NotificationService {
    companion object {
        fun scheduleNotification(context: Context) {
            val settingsManager = SettingsManager(context)
            val intervalMinutes = settingsManager.getIntervalMinutes()
            val intervalMillis = intervalMinutes * 60 * 1000

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Отменяем предыдущие уведомления
            alarmManager.cancel(pendingIntent)

            // Устанавливаем уведомление
            val triggerAtMillis = System.currentTimeMillis() + intervalMillis
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AlarmManagerCompat.setExactAndAllowWhileIdle(
                    alarmManager,
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }

        fun cancelNotification(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }

        fun showNotificationImmediately(context: Context) {
            // Показываем уведомление сразу
            NotificationReceiver.showNotificationNow(context)
            
            // Планируем следующее уведомление
            scheduleNotification(context)
        }
    }
}

