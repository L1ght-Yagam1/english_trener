package com.example.lab5

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationReceiver : BroadcastReceiver() {
    
    companion object {
        // Константы для действий
        const val ACTION_SHOW_LEARNING = "com.example.lab5.SHOW_LEARNING"
        const val ACTION_SHOW_TESTING = "com.example.lab5.SHOW_TESTING"
        const val ACTION_ANSWER = "com.example.lab5.ANSWER"
        
        // Константы для extra данных
        const val EXTRA_CORRECT = "correct"
        const val EXTRA_WORD = "word"
        const val EXTRA_TRANSLATION = "translation"
        
        // ID уведомлений
        const val NOTIFICATION_ID_LEARNING = 1
        const val NOTIFICATION_ID_TESTING = 2
        const val NOTIFICATION_ID_ANSWER = 3
        
        // Request codes для PendingIntent
        const val REQUEST_CODE_CORRECT = 100
        const val REQUEST_CODE_WRONG = 101
        
        // Вспомогательная функция для безопасного показа уведомлений
        private fun safeNotify(context: Context, notificationId: Int, notification: android.app.Notification) {
            val notificationManager = NotificationManagerCompat.from(context)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationManager.notify(notificationId, notification)
                    }
                } else {
                    notificationManager.notify(notificationId, notification)
                }
            } catch (e: SecurityException) {
                // Разрешение не предоставлено, игнорируем
                e.printStackTrace()
            }
        }
        
        fun showNotificationNow(context: Context) {
            val settingsManager = SettingsManager(context)
            val dictionaryParser = DictionaryParser(context)
            
            if (settingsManager.isLearningMode()) {
                showLearningModeNotificationStatic(context, dictionaryParser)
            } else {
                showTestingModeNotificationStatic(context, dictionaryParser)
            }
        }
        
        private fun showLearningModeNotificationStatic(context: Context, dictionaryParser: DictionaryParser) {
            val word = dictionaryParser.getRandomWord() ?: return
            
            val channelId = "learning_channel"
            NotificationReceiver().createNotificationChannel(context, channelId, "Обучение", "Уведомления в режиме обучения")
            
            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Изучение английского")
                .setContentText("${word.english} - ${word.russian}")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("${word.english}\n${word.russian}"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            
            safeNotify(context, NOTIFICATION_ID_LEARNING, notification)
        }
        
        private fun showTestingModeNotificationStatic(context: Context, dictionaryParser: DictionaryParser) {
            val correctWord = dictionaryParser.getRandomWord() ?: return
            val wrongWords = dictionaryParser.getRandomWords(2)
                .filter { it.english != correctWord.english }
                .take(1)
            
            if (wrongWords.isEmpty()) return
            
            val wrongWord = wrongWords[0]
            val options = listOf(correctWord.russian, wrongWord.russian).shuffled()
            
            // Определяем, какой вариант правильный (1 или 2)
            val correctOptionIndex = if (options[0] == correctWord.russian) 1 else 2
            
            val channelId = "testing_channel"
            NotificationReceiver().createNotificationChannel(context, channelId, "Проверка", "Уведомления в режиме проверки")
            
            // Формируем текст уведомления с вариантами ответа
            val notificationText = "Выберите перевод слова:\n${correctWord.english}\n\n1. ${options[0]}\n2. ${options[1]}"
            
            val correctAnswerIntent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_ANSWER
                putExtra(EXTRA_CORRECT, true)
                putExtra(EXTRA_WORD, correctWord.english)
                putExtra(EXTRA_TRANSLATION, correctWord.russian)
            }
            
            val wrongAnswerIntent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_ANSWER
                putExtra(EXTRA_CORRECT, false)
                putExtra(EXTRA_WORD, correctWord.english)
                putExtra(EXTRA_TRANSLATION, correctWord.russian)
            }
            
            val correctPendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_CORRECT,
                correctAnswerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val wrongPendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_WRONG,
                wrongAnswerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Проверка знаний")
                .setContentText("Выберите перевод слова: ${correctWord.english}")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText(notificationText))
                .addAction(
                    android.R.drawable.ic_dialog_info,
                    "1",
                    if (correctOptionIndex == 1) correctPendingIntent else wrongPendingIntent
                )
                .addAction(
                    android.R.drawable.ic_dialog_info,
                    "2",
                    if (correctOptionIndex == 2) correctPendingIntent else wrongPendingIntent
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            
            safeNotify(context, NOTIFICATION_ID_TESTING, notification)
        }
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_SHOW_LEARNING -> {
                showLearningNotification(context, intent)
            }
            ACTION_SHOW_TESTING -> {
                showTestingNotification(context, intent)
            }
            ACTION_ANSWER -> {
                handleAnswer(context, intent)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                // При загрузке системы планируем уведомления
                NotificationService.scheduleNotification(context)
            }
            else -> {
                // Основное уведомление
                val settingsManager = SettingsManager(context)
                val dictionaryParser = DictionaryParser(context)
                
                if (settingsManager.isLearningMode()) {
                    showLearningModeNotification(context, dictionaryParser)
                } else {
                    showTestingModeNotification(context, dictionaryParser)
                }
                
                // Планируем следующее уведомление
                NotificationService.scheduleNotification(context)
            }
        }
    }

    private fun showLearningModeNotification(context: Context, dictionaryParser: DictionaryParser) {
        val word = dictionaryParser.getRandomWord() ?: return
        
        val channelId = "learning_channel"
        createNotificationChannel(context, channelId, "Обучение", "Уведомления в режиме обучения")
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Изучение английского")
            .setContentText("${word.english} - ${word.russian}")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("${word.english}\n${word.russian}"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        Companion.safeNotify(context, NOTIFICATION_ID_LEARNING, notification)
    }

    private fun showTestingModeNotification(context: Context, dictionaryParser: DictionaryParser) {
        val correctWord = dictionaryParser.getRandomWord() ?: return
        val wrongWords = dictionaryParser.getRandomWords(2)
            .filter { it.english != correctWord.english }
            .take(1)
        
        if (wrongWords.isEmpty()) return
        
        val wrongWord = wrongWords[0]
        val options = listOf(correctWord.russian, wrongWord.russian).shuffled()
        
        // Определяем, какой вариант правильный (1 или 2)
        val correctOptionIndex = if (options[0] == correctWord.russian) 1 else 2
        
        val channelId = "testing_channel"
        createNotificationChannel(context, channelId, "Проверка", "Уведомления в режиме проверки")
        
        // Формируем текст уведомления с вариантами ответа
        val notificationText = "Выберите перевод слова:\n${correctWord.english}\n\n1. ${options[0]}\n2. ${options[1]}"
        
        val correctAnswerIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_ANSWER
            putExtra(EXTRA_CORRECT, true)
            putExtra(EXTRA_WORD, correctWord.english)
            putExtra(EXTRA_TRANSLATION, correctWord.russian)
        }
        
        val wrongAnswerIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_ANSWER
            putExtra(EXTRA_CORRECT, false)
            putExtra(EXTRA_WORD, correctWord.english)
            putExtra(EXTRA_TRANSLATION, correctWord.russian)
        }
        
        val correctPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_CORRECT,
            correctAnswerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val wrongPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_WRONG,
            wrongAnswerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Проверка знаний")
            .setContentText("Выберите перевод слова: ${correctWord.english}")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(notificationText))
            .addAction(
                android.R.drawable.ic_dialog_info,
                "1",
                if (correctOptionIndex == 1) correctPendingIntent else wrongPendingIntent
            )
            .addAction(
                android.R.drawable.ic_dialog_info,
                "2",
                if (correctOptionIndex == 2) correctPendingIntent else wrongPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        Companion.safeNotify(context, NOTIFICATION_ID_TESTING, notification)
    }

    private fun showLearningNotification(context: Context, intent: Intent) {
        val word = intent.getStringExtra(EXTRA_WORD) ?: return
        val translation = intent.getStringExtra(EXTRA_TRANSLATION) ?: return
        
        val channelId = "learning_channel"
        createNotificationChannel(context, channelId, "Обучение", "Уведомления в режиме обучения")
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Изучение английского")
            .setContentText("$word - $translation")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$word\n$translation"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        Companion.safeNotify(context, NOTIFICATION_ID_LEARNING, notification)
    }

    private fun showTestingNotification(context: Context, intent: Intent) {
        // Аналогично showTestingModeNotification
    }

    private fun handleAnswer(context: Context, intent: Intent) {
        // Отменяем основное уведомление с вопросом
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(NOTIFICATION_ID_TESTING)
        
        val isCorrect = intent.getBooleanExtra(EXTRA_CORRECT, false)
        val word = intent.getStringExtra(EXTRA_WORD) ?: ""
        val translation = intent.getStringExtra(EXTRA_TRANSLATION) ?: ""
        
        val channelId = "answer_channel"
        createNotificationChannel(context, channelId, "Ответ", "Уведомления с правильным ответом")
        
        val message = if (isCorrect) {
            "Правильно! ✓\n$word - $translation"
        } else {
            "Неправильно ✗\nПравильный ответ: $word - $translation"
        }
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(if (isCorrect) "Правильно!" else "Неправильно")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        Companion.safeNotify(context, NOTIFICATION_ID_ANSWER, notification)
    }

    private fun createNotificationChannel(
        context: Context,
        channelId: String,
        channelName: String,
        channelDescription: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}

