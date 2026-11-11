package com.example.data.preferences

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppPreferences(context: Context) {
    private val sharedPrefs = context.getSharedPreferences("essence_settings", Context.MODE_PRIVATE)
    private val secureStorage = SecureStorage(context)

    companion object {
        const val PROVIDER_ON_DEVICE = "ON_DEVICE"
        const val PROVIDER_CUSTOM = "CUSTOM_ENDPOINT"
    }

    private val _isTranscriptionEnabled = MutableStateFlow(
        sharedPrefs.getBoolean("transcription_enabled", true)
    )
    val isTranscriptionEnabled: StateFlow<Boolean> = _isTranscriptionEnabled.asStateFlow()

    private val _provider = MutableStateFlow(
        sharedPrefs.getString("transcription_provider", PROVIDER_ON_DEVICE) ?: PROVIDER_ON_DEVICE
    )
    val provider: StateFlow<String> = _provider.asStateFlow()

    private val _customBaseUrl = MutableStateFlow(
        sharedPrefs.getString("custom_base_url", "https://api.openai.com/v1") ?: "https://api.openai.com/v1"
    )
    val customBaseUrl: StateFlow<String> = _customBaseUrl.asStateFlow()

    private val _customModelName = MutableStateFlow(
        sharedPrefs.getString("custom_model_name", "whisper-1") ?: "whisper-1"
    )
    val customModelName: StateFlow<String> = _customModelName.asStateFlow()

    private val _aiSummarizeEnabled = MutableStateFlow(
        sharedPrefs.getBoolean("ai_summarize_enabled", true)
    )
    val aiSummarizeEnabled: StateFlow<Boolean> = _aiSummarizeEnabled.asStateFlow()

    private val _aiExtractTasksEnabled = MutableStateFlow(
        sharedPrefs.getBoolean("ai_extract_tasks_enabled", true)
    )
    val aiExtractTasksEnabled: StateFlow<Boolean> = _aiExtractTasksEnabled.asStateFlow()

    private val _hasCompletedOemSetup = MutableStateFlow(
        sharedPrefs.getBoolean("oem_setup_completed", false)
    )
    val hasCompletedOemSetup: StateFlow<Boolean> = _hasCompletedOemSetup.asStateFlow()

    fun setTranscriptionEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("transcription_enabled", enabled).apply()
        _isTranscriptionEnabled.value = enabled
    }

    fun setProvider(providerName: String) {
        sharedPrefs.edit().putString("transcription_provider", providerName).apply()
        _provider.value = providerName
    }

    fun setCustomBaseUrl(url: String) {
        sharedPrefs.edit().putString("custom_base_url", url).apply()
        _customBaseUrl.value = url
    }

    fun setCustomModelName(model: String) {
        sharedPrefs.edit().putString("custom_model_name", model).apply()
        _customModelName.value = model
    }

    fun saveApiKey(key: String) {
        secureStorage.saveEncryptedString("api_key", key)
    }

    fun getApiKey(): String {
        return secureStorage.getDecryptedString("api_key")
    }

    fun setAiSummarizeEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("ai_summarize_enabled", enabled).apply()
        _aiSummarizeEnabled.value = enabled
    }

    fun setAiExtractTasksEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("ai_extract_tasks_enabled", enabled).apply()
        _aiExtractTasksEnabled.value = enabled
    }

    fun setOemSetupCompleted(completed: Boolean) {
        sharedPrefs.edit().putBoolean("oem_setup_completed", completed).apply()
        _hasCompletedOemSetup.value = completed
    }
}
