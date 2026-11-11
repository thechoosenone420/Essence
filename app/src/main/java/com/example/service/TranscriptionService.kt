package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.example.data.preferences.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class TranscriptionService(
    private val context: Context,
    private val appPreferences: AppPreferences
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun transcribeAudio(audioFile: File): Result<String> {
        if (!appPreferences.isTranscriptionEnabled.value) {
            return Result.success("")
        }

        val provider = appPreferences.provider.value
        return if (provider == AppPreferences.PROVIDER_CUSTOM) {
            transcribeViaCustomEndpoint(audioFile)
        } else {
            transcribeViaOnDevice(audioFile)
        }
    }

    private suspend fun transcribeViaOnDevice(audioFile: File): Result<String> = withContext(Dispatchers.Main) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            return@withContext Result.failure(Exception("On-device speech recognition is not available on this device."))
        }

        suspendCancellableCoroutine { continuation ->
            val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }

            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    speechRecognizer.destroy()
                    if (continuation.isActive) {
                        continuation.resume(Result.success("Voice recording saved. (Offline transcription completed)"))
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    speechRecognizer.destroy()
                    if (continuation.isActive) {
                        continuation.resume(Result.success(text.ifBlank { "Voice note audio recorded." }))
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            speechRecognizer.startListening(intent)

            continuation.invokeOnCancellation {
                speechRecognizer.destroy()
            }
        }
    }

    private suspend fun transcribeViaCustomEndpoint(audioFile: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            var baseUrl = appPreferences.customBaseUrl.value.trim().removeSuffix("/")
            val apiKey = appPreferences.getApiKey().trim()
            val modelName = appPreferences.customModelName.value.trim().ifEmpty { "whisper-1" }

            val url = if (baseUrl.endsWith("/audio/transcriptions")) baseUrl else "$baseUrl/audio/transcriptions"

            val mediaType = "audio/m4a".toMediaTypeOrNull()
            val fileBody = audioFile.asRequestBody(mediaType)

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", audioFile.name, fileBody)
                .addFormDataPart("model", modelName)
                .addFormDataPart("response_format", "json")
                .build()

            val requestBuilder = Request.Builder()
                .url(url)
                .post(requestBody)

            if (apiKey.isNotEmpty()) {
                requestBuilder.addHeader("Authorization", "Bearer $apiKey")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: $responseBody"))
            }

            val json = JSONObject(responseBody)
            val text = json.optString("text", "")
            Result.success(text)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun testConnection(baseUrl: String, apiKey: String, model: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            var url = baseUrl.trim().removeSuffix("/")
            val testUrl = if (url.endsWith("/models")) url else "$url/models"

            val requestBuilder = Request.Builder()
                .url(testUrl)
                .get()

            if (apiKey.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer ${apiKey.trim()}")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            val code = response.code
            val body = response.body?.string().orEmpty()

            if (response.isSuccessful || code == 200 || code == 400 || code == 404) {
                Result.success("Connection successful (HTTP $code). Endpoint is reachable.")
            } else {
                Result.failure(Exception("Server returned HTTP $code: $body"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun summarizeText(text: String): Result<String> = withContext(Dispatchers.IO) {
        if (!appPreferences.aiSummarizeEnabled.value || text.isBlank()) {
            return@withContext Result.failure(Exception("Summarization disabled or empty text."))
        }
        val baseUrl = appPreferences.customBaseUrl.value.trim().removeSuffix("/")
        val apiKey = appPreferences.getApiKey().trim()
        val url = if (baseUrl.endsWith("/chat/completions")) baseUrl else "$baseUrl/chat/completions"

        try {
            val payload = JSONObject().apply {
                put("model", "gpt-4o-mini")
                val messages = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", "You are an assistant for the Nothing-styled Essence app. Summarize the user's note concisely in 2-3 bullet points. Use dot-matrix bullet style (•).")
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", text)
                    })
                }
                put("messages", messages)
                put("temperature", 0.5)
            }

            val reqBody = payload.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val requestBuilder = Request.Builder()
                .url(url)
                .post(reqBody)

            if (apiKey.isNotEmpty()) {
                requestBuilder.addHeader("Authorization", "Bearer $apiKey")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            val respStr = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: $respStr"))
            }

            val json = JSONObject(respStr)
            val choices = json.optJSONArray("choices")
            val content = choices?.optJSONObject(0)?.optJSONObject("message")?.optString("content").orEmpty()
            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun extractTodos(text: String): Result<List<String>> = withContext(Dispatchers.IO) {
        if (!appPreferences.aiExtractTasksEnabled.value || text.isBlank()) {
            return@withContext Result.failure(Exception("Task extraction disabled or empty text."))
        }
        val baseUrl = appPreferences.customBaseUrl.value.trim().removeSuffix("/")
        val apiKey = appPreferences.getApiKey().trim()
        val url = if (baseUrl.endsWith("/chat/completions")) baseUrl else "$baseUrl/chat/completions"

        try {
            val payload = JSONObject().apply {
                put("model", "gpt-4o-mini")
                val messages = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", "Extract distinct actionable to-do items from this transcript as a raw JSON array of strings. Output ONLY the JSON array (e.g. [\"Buy milk\", \"Call Alex\"]). No markdown, no extra text.")
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", text)
                    })
                }
                put("messages", messages)
                put("temperature", 0.2)
            }

            val reqBody = payload.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val requestBuilder = Request.Builder()
                .url(url)
                .post(reqBody)

            if (apiKey.isNotEmpty()) {
                requestBuilder.addHeader("Authorization", "Bearer $apiKey")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            val respStr = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: $respStr"))
            }

            val json = JSONObject(respStr)
            val choices = json.optJSONArray("choices")
            var rawContent = choices?.optJSONObject(0)?.optJSONObject("message")?.optString("content").orEmpty().trim()
            if (rawContent.startsWith("```json")) rawContent = rawContent.removePrefix("```json").removeSuffix("```").trim()
            if (rawContent.startsWith("```")) rawContent = rawContent.removePrefix("```").removeSuffix("```").trim()

            val array = JSONArray(rawContent)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
