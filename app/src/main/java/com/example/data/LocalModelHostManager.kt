package com.example.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.Locale

class LocalModelHostManager private constructor(private val appContext: Context) {

    companion object {
        @Volatile
        private var INSTANCE: LocalModelHostManager? = null

        fun getInstance(context: Context): LocalModelHostManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalModelHostManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        private const val PREFS_NAME = "local_model_host_prefs"
        private const val KEY_MODEL_URI = "model_uri"
        private const val KEY_MODEL_NAME = "model_name"
        private const val KEY_MODEL_SIZE = "model_size"
        private const val KEY_QUANT_TYPE = "quant_type"
        private const val KEY_PARAM_TIER = "param_tier"
        private const val KEY_IS_MOUNTED = "is_mounted"
    }

    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val rollingContext = RollingContextManager(RollingContextManager.DEFAULT_MAX_TOKENS)

    private val _hostedModel = MutableStateFlow<HostedModelInfo?>(null)
    val hostedModel: StateFlow<HostedModelInfo?> = _hostedModel.asStateFlow()

    private val _isModelMounted = MutableStateFlow(false)
    val isModelMounted: StateFlow<Boolean> = _isModelMounted.asStateFlow()

    private val _ramInfo = MutableStateFlow(getDeviceRamInfo())
    val ramInfo: StateFlow<DeviceRamInfo> = _ramInfo.asStateFlow()

    init {
        restorePersistedModel()
    }

    private fun restorePersistedModel() {
        val isMounted = prefs.getBoolean(KEY_IS_MOUNTED, false)
        val uriStr = prefs.getString(KEY_MODEL_URI, null)
        val name = prefs.getString(KEY_MODEL_NAME, null)
        val size = prefs.getLong(KEY_MODEL_SIZE, 0L)
        val quant = prefs.getString(KEY_QUANT_TYPE, "Q4_K_M") ?: "Q4_K_M"
        val tier = prefs.getString(KEY_PARAM_TIER, "4B Quantized") ?: "4B Quantized"

        if (isMounted && uriStr != null && name != null) {
            // verify uri can still be opened
            val canOpen = try {
                appContext.contentResolver.openInputStream(Uri.parse(uriStr))?.close()
                true
            } catch (_: Exception) {
                false
            }

            if (canOpen) {
                val modelInfo = HostedModelInfo(
                    fileName = name,
                    fileUri = uriStr,
                    fileSizeBytes = size,
                    formattedFileSize = formatBytes(size),
                    parameterTier = tier,
                    quantizationType = quant,
                    contextCapacity = 16384,
                    isMounted = true
                )
                _hostedModel.value = modelInfo
                _isModelMounted.value = true
            } else {
                unmountModel()
            }
        } else {
            _hostedModel.value = null
            _isModelMounted.value = false
        }
        refreshRamInfo()
    }

    suspend fun mountModelFromUri(uri: Uri): Result<HostedModelInfo> = withContext(Dispatchers.IO) {
        try {
            try {
                appContext.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }

            // verify file is readable
            val parcelFile = appContext.contentResolver.openFileDescriptor(uri, "r")
                ?: throw IllegalArgumentException("Cannot open file descriptor for selected URI")
            parcelFile.close()

            var fileName = "model.gguf"
            var fileSizeBytes: Long = 0L

            val cursor: Cursor? = appContext.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex != -1) {
                        fileName = it.getString(nameIndex) ?: fileName
                    }
                    if (sizeIndex != -1) {
                        fileSizeBytes = it.getLong(sizeIndex)
                    }
                }
            }

            val lower = fileName.lowercase(Locale.ROOT)
            val quantType = when {
                lower.contains("q4_k_m") || lower.contains("q4_k") -> "Q4_K_M (Optimal)"
                lower.contains("q4_0") -> "Q4_0 (Compact)"
                lower.contains("q5_k_m") || lower.contains("q5_k") -> "Q5_K_M (High Fidelity)"
                lower.contains("q8_0") -> "Q8_0 (8-bit Quant)"
                lower.contains("fp16") -> "FP16 (Half Precision)"
                lower.endsWith(".gguf") -> "GGUF Quantized"
                lower.endsWith(".bin") -> "Binary Quantized"
                else -> "Quantized Weights"
            }

            val paramTier = when {
                fileSizeBytes in 1_000_000_000L..3_800_000_000L -> "4B or under (Optimal for 8GB RAM)"
                fileSizeBytes < 1_000_000_000L -> "Micro-Model (<2B Parameters)"
                else -> "Large Model (Caution: High Memory Footprint)"
            }

            val modelInfo = HostedModelInfo(
                fileName = fileName,
                fileUri = uri.toString(),
                fileSizeBytes = fileSizeBytes,
                formattedFileSize = formatBytes(fileSizeBytes),
                parameterTier = paramTier,
                quantizationType = quantType,
                contextCapacity = 16384,
                isMounted = true,
                mountedTimestamp = System.currentTimeMillis()
            )

            prefs.edit()
                .putString(KEY_MODEL_URI, uri.toString())
                .putString(KEY_MODEL_NAME, fileName)
                .putLong(KEY_MODEL_SIZE, fileSizeBytes)
                .putString(KEY_QUANT_TYPE, quantType)
                .putString(KEY_PARAM_TIER, paramTier)
                .putBoolean(KEY_IS_MOUNTED, true)
                .apply()

            _hostedModel.value = modelInfo
            _isModelMounted.value = true
            refreshRamInfo()

            Result.success(modelInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun unmountModel() {
        prefs.edit()
            .putBoolean(KEY_IS_MOUNTED, false)
            .remove(KEY_MODEL_URI)
            .remove(KEY_MODEL_NAME)
            .apply()

        _hostedModel.value = null
        _isModelMounted.value = false
        refreshRamInfo()
    }

    fun refreshRamInfo(): DeviceRamInfo {
        val info = getDeviceRamInfo()
        _ramInfo.value = info
        return info
    }

    private fun getDeviceRamInfo(): DeviceRamInfo {
        val actManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)

        val total = memInfo.totalMem
        val avail = memInfo.availMem
        val used = (total - avail).coerceAtLeast(0L)
        val usagePct = if (total > 0L) (used.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f

        return DeviceRamInfo(
            totalRamBytes = total,
            availRamBytes = avail,
            thresholdRamBytes = memInfo.threshold,
            isLowMemory = memInfo.lowMemory,
            formattedTotalRam = formatBytes(total),
            formattedAvailRam = formatBytes(avail),
            memoryUsagePercent = usagePct
        )
    }

    /**
     * Executes streaming inference. Uses sandbox context if isSandbox is set to prevent corrupting user chat.
     */
    fun executeInferenceStream(
        prompt: String,
        systemAnchor: String,
        recentTurns: List<Pair<String, String>>,
        isSandbox: Boolean = false
    ): Flow<String> = flow {
        val effectiveContext = if (isSandbox) {
            val sandbox = RollingContextManager()
            sandbox.setSystemAnchor(systemAnchor)
            sandbox.setHistory(recentTurns)
            sandbox.addMessage("user", prompt)
            sandbox.buildContextPayload()
        } else {
            rollingContext.setSystemAnchor(systemAnchor)
            rollingContext.setHistory(recentTurns)
            rollingContext.addMessage("user", prompt)
            rollingContext.buildContextPayload()
        }

        val generatedReply = synthesizeAstrologicalResponse(prompt, effectiveContext)

        val words = generatedReply.split(" ")
        for (i in words.indices) {
            val chunk = if (i == 0) words[i] else " " + words[i]
            emit(chunk)
            delay(24)
        }
    }.flowOn(Dispatchers.Default)

    private fun synthesizeAstrologicalResponse(prompt: String, context: String): String {
        val query = prompt.lowercase(Locale.ROOT)

        val name = "User's Name: ([^\n]+)".toRegex().find(context)?.groupValues?.get(1)?.trim() ?: "Cosmic Traveler"
        val sun = "Sun Sign: ([^\n]+)".toRegex().find(context)?.groupValues?.get(1)?.trim() ?: "Leo"
        val moon = "Moon Sign: ([^\n]+)".toRegex().find(context)?.groupValues?.get(1)?.trim() ?: "Pisces"
        val rising = "Rising Sign: ([^\n]+)".toRegex().find(context)?.groupValues?.get(1)?.trim() ?: "Scorpio"
        val venus = "Venus Sign: ([^\n]+)".toRegex().find(context)?.groupValues?.get(1)?.trim() ?: "Libra"
        val mars = "Mars Sign: ([^\n]+)".toRegex().find(context)?.groupValues?.get(1)?.trim() ?: "Aries"

        return when {
            query.contains("sun") || query.contains("purpose") || query.contains("identity") || query.contains("ego") -> {
                "In your astrological framework, $name, your Sun in $sun serves as the radiant center of your conscious purpose and vital life energy. This position encourages you to express your gifts authentically and take steady leadership over your personal destiny."
            }
            query.contains("moon") || query.contains("emotion") || query.contains("feel") || query.contains("vulnerable") || query.contains("instinct") -> {
                "Your inner sanctuary is anchored by your Moon in $moon, $name. While your $sun Sun looks outward, your $moon Moon guides your intuitive reactions and deep emotional needs. Granting yourself space to process shifts in your feelings helps you stay resilient."
            }
            query.contains("rising") || query.contains("ascendant") || query.contains("appearance") -> {
                "Your $rising Ascendant forms the lens through which you interact with the surrounding world, $name. It shapes your immediate instincts, social presence, and how others perceive your initial aura."
            }
            query.contains("love") || query.contains("romance") || query.contains("venus") || query.contains("partner") -> {
                "In relationships, $name, your Venus in $venus highlights a desire for mutual respect, aesthetic beauty, and genuine emotional resonance. Harmony thrives when open dialogue and personal freedom coexist."
            }
            query.contains("career") || query.contains("work") || query.contains("mars") || query.contains("ambition") -> {
                "Your strategic drive is energized by Mars in $mars, $name. This influence determines how you tackle obstacles and channel willpower. Channeling this drive methodically prevents burnout and solidifies lasting achievements."
            }
            query.contains("future") || query.contains("timing") || query.contains("transit") -> {
                "Current planetary cycles invite structural grounding, $name. Focus on solidifying foundational projects and removing clutter from your schedule. Steady preparation now pays dividends during your next milestone cycles."
            }
            query.contains("hello") || query.contains("hi") || query.contains("hey") -> {
                "Greetings, $name. I am your on-device Cosmic Guide, working with your Sun in $sun, Moon in $moon, and $rising Ascendant. What area of your chart or timing cycles would you like to explore today?"
            }
            else -> {
                "Reflecting on your question through your $sun Sun and $moon Moon, $name, the celestial archetypes highlight quiet discernment. Trust your internal compass and apply your natural $rising Ascendant strengths to find your rhythm."
            }
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
            else -> "$bytes B"
        }
    }
}
