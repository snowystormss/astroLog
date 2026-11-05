package com.example.data

/**
 * Metadata and runtime state for a user-hosted on-device quantized model (4B parameters or under).
 */
data class HostedModelInfo(
    val fileName: String,
    val fileUri: String,
    val fileSizeBytes: Long,
    val formattedFileSize: String,
    val parameterTier: String,
    val quantizationType: String,
    val contextCapacity: Int = 16384,
    val isMounted: Boolean = true,
    val mountedTimestamp: Long = System.currentTimeMillis()
)

/**
 * Real-time RAM status on the host device (e.g. 8GB physical memory).
 */
data class DeviceRamInfo(
    val totalRamBytes: Long,
    val availRamBytes: Long,
    val thresholdRamBytes: Long,
    val isLowMemory: Boolean,
    val formattedTotalRam: String,
    val formattedAvailRam: String,
    val memoryUsagePercent: Float
)

/**
 * Real-time tracking of the 16k rolling context buffer.
 */
data class RollingContextState(
    val maxTokens: Int = 16384,
    val estimatedActiveTokens: Int = 0,
    val systemAnchorTokens: Int = 0,
    val retainedTurnsCount: Int = 0,
    val capacityPercent: Float = 0f
)
