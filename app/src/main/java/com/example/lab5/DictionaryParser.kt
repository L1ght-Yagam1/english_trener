package com.example.lab5

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.File

data class WordPair(
    val english: String,
    val russian: String
)

class DictionaryParser(private val context: Context) {
    private var words: List<WordPair> = emptyList()

    fun loadDictionary(): List<WordPair> {
        if (words.isNotEmpty()) {
            return words
        }

        // Сначала проверяем файловую систему, если файла нет - копируем из assets
        val file = context.getFileStreamPath("mueller.dict")
        if (!file.exists()) {
            try {
                // Копируем из assets в файловую систему
                val inputStream = context.assets.open("mueller.dict")
                val outputStream = context.openFileOutput("mueller.dict", Context.MODE_PRIVATE)
                inputStream.copyTo(outputStream)
                inputStream.close()
                outputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val wordList = mutableListOf<WordPair>()
        
        try {
            // Читаем файл из файловой системы
            val reader = BufferedReader(file.reader())
            
            var currentWord: String? = null
            var line: String?
            
            while (reader.readLine().also { line = it } != null) {
                val trimmedLine = line!!.trim()
                
                // Если строка пустая, пропускаем
                if (trimmedLine.isEmpty()) {
                    continue
                }
                
                // Если строка не начинается с пробела, это новое английское слово
                if (!trimmedLine.startsWith(" ") && !trimmedLine.startsWith("[")) {
                    currentWord = trimmedLine
                } else if (currentWord != null && trimmedLine.contains("[")) {
                    // Это строка с переводом (содержит транскрипцию в квадратных скобках)
                    // Извлекаем русский перевод
                    val russian = extractRussianTranslation(trimmedLine)
                    if (russian.isNotEmpty()) {
                        wordList.add(WordPair(currentWord, russian))
                    }
                }
            }
            
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
            // Если не удалось загрузить из файловой системы, пробуем из assets напрямую
            try {
                val inputStream = context.assets.open("mueller.dict")
                val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
                var currentWord: String? = null
                var line: String?
                
                while (reader.readLine().also { line = it } != null) {
                    val trimmedLine = line!!.trim()
                    if (trimmedLine.isEmpty()) continue
                    
                    if (!trimmedLine.startsWith(" ") && !trimmedLine.startsWith("[")) {
                        currentWord = trimmedLine
                    } else if (currentWord != null && trimmedLine.contains("[")) {
                        val russian = extractRussianTranslation(trimmedLine)
                        if (russian.isNotEmpty()) {
                            wordList.add(WordPair(currentWord, russian))
                        }
                    }
                }
                reader.close()
                inputStream.close()
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
        
        words = wordList
        return words
    }

    private fun extractRussianTranslation(line: String): String {
        // Убираем транскрипцию в квадратных скобках
        var text = line.replace(Regex("\\[.*?\\]"), "")
        
        // Убираем служебные пометки (_n., _v., _разг. и т.д.)
        text = text.replace(Regex("_\\w+\\.?"), "")
        
        // Убираем знак равенства и другие служебные символы в начале
        text = text.replace(Regex("^\\s*=\\s*"), "")
        
        // Извлекаем русский текст (кириллица) - берем первое вхождение
        // Ищем последовательность кириллических символов
        val russianPattern = Regex("[А-Яа-яЁё][А-Яа-яЁё\\s,;:\\.\\-]*[А-Яа-яЁё]|[А-Яа-яЁё]+")
        val russianMatches = russianPattern.findAll(text)
        val firstMatch = russianMatches.firstOrNull()
        var result = firstMatch?.value?.trim() ?: ""
        
        // Если результат слишком длинный (возможно, захватили несколько слов), берем первое слово
        if (result.length > 50) {
            result = result.split(Regex("[\\s,;:]")).firstOrNull()?.trim() ?: result
        }
        
        return result
    }

    fun getRandomWord(): WordPair? {
        if (words.isEmpty()) {
            loadDictionary()
        }
        return words.randomOrNull()
    }

    fun getRandomWords(count: Int): List<WordPair> {
        if (words.isEmpty()) {
            loadDictionary()
        }
        return words.shuffled().take(count)
    }
}

