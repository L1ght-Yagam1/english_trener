package com.example.lab5

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    
    var mode by remember { mutableStateOf(settingsManager.getMode()) }
    var intervalMinutes by remember { mutableStateOf(settingsManager.getIntervalMinutes().toString()) }
    var isServiceRunning by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Настройки изучения английского",
            style = MaterialTheme.typography.headlineMedium
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Режим работы",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RadioButton(
                        selected = mode == "learning",
                        onClick = { mode = "learning" }
                    )
                    Text(
                        text = "Обучение",
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RadioButton(
                        selected = mode == "testing",
                        onClick = { mode = "testing" }
                    )
                    Text(
                        text = "Проверка",
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Интервал уведомлений (минуты)",
                    style = MaterialTheme.typography.titleMedium
                )
                
                OutlinedTextField(
                    value = intervalMinutes,
                    onValueChange = { 
                        if (it.all { char -> char.isDigit() }) {
                            intervalMinutes = it
                        }
                    },
                    label = { Text("Минуты") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    settingsManager.setMode(mode)
                    val interval = intervalMinutes.toLongOrNull() ?: 30L
                    settingsManager.setIntervalMinutes(interval)
                    
                    if (isServiceRunning) {
                        NotificationService.cancelNotification(context)
                    }
                    // Показываем уведомление сразу и планируем следующие
                    NotificationService.showNotificationImmediately(context)
                    isServiceRunning = true
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Запустить")
            }
            
            Button(
                onClick = {
                    NotificationService.cancelNotification(context)
                    isServiceRunning = false
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Остановить")
            }
        }

        if (isServiceRunning) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "Сервис уведомлений активен",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

