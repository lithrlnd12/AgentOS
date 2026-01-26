package com.agentos.agents.voice

import android.util.Log
import kotlin.math.*

/**
 * Audio processing utilities for voice enhancement
 * Includes noise reduction, echo cancellation, and quality improvement
 */
object AudioProcessingUtils {
    
    private const val TAG = "AudioProcessingUtils"
    
    /**
     * Apply noise reduction to audio data
     * Uses spectral subtraction algorithm
     */
    fun applyNoiseReduction(
        audioData: ByteArray,
        noiseLevel: Float = 0.3f,
        sampleRate: Int = 16000
    ): ByteArray {
        if (noiseLevel <= 0.0f) return audioData
        
        return try {
            // Convert bytes to samples
            val samples = bytesToSamples(audioData)
            
            // Apply spectral subtraction noise reduction
            val denoisedSamples = spectralSubtraction(samples, noiseLevel, sampleRate)
            
            // Convert back to bytes
            samplesToBytes(denoisedSamples)
            
        } catch (e: Exception) {
            Log.e(TAG, "Noise reduction failed", e)
            audioData // Return original on error
        }
    }
    
    /**
     * Apply echo cancellation
     * Uses adaptive filtering algorithm
     */
    fun applyEchoCancellation(
        audioData: ByteArray,
        referenceSignal: ByteArray? = null,
        sampleRate: Int = 16000
    ): ByteArray {
        return try {
            val samples = bytesToSamples(audioData)
            val reference = referenceSignal?.let { bytesToSamples(it) }
            
            val cleanedSamples = adaptiveEchoCancellation(samples, reference, sampleRate)
            
            samplesToBytes(cleanedSamples)
            
        } catch (e: Exception) {
            Log.e(TAG, "Echo cancellation failed", e)
            audioData
        }
    }
    
    /**
     * Enhance audio quality
     * Applies multiple enhancement techniques
     */
    fun enhanceAudioQuality(
        audioData: ByteArray,
        sampleRate: Int = 16000,
        enhancementConfig: AudioEnhancementConfig = AudioEnhancementConfig.DEFAULT
    ): ByteArray {
        return try {
            var enhancedAudio = audioData
            
            // Apply noise reduction
            if (enhancementConfig.noiseReduction) {
                enhancedAudio = applyNoiseReduction(enhancedAudio, enhancementConfig.noiseLevel, sampleRate)
            }
            
            // Apply echo cancellation
            if (enhancementConfig.echoCancellation) {
                enhancedAudio = applyEchoCancellation(enhancedAudio, null, sampleRate)
            }
            
            // Apply gain normalization
            if (enhancementConfig.gainNormalization) {
                enhancedAudio = normalizeGain(enhancedAudio, enhancementConfig.targetLevel)
            }
            
            // Apply high-pass filter
            if (enhancementConfig.highPassFilter) {
                enhancedAudio = applyHighPassFilter(enhancedAudio, sampleRate, enhancementConfig.cutoffFreq)
            }
            
            // Apply low-pass filter
            if (enhancementConfig.lowPassFilter) {
                enhancedAudio = applyLowPassFilter(enhancedAudio, sampleRate, enhancementConfig.cutoffFreqHigh)
            }
            
            enhancedAudio
            
        } catch (e: Exception) {
            Log.e(TAG, "Audio enhancement failed", e)
            audioData
        }
    }
    
    /**
     * Estimate background noise level
     * Returns noise level between 0.0 and 1.0
     */
    fun estimateNoiseLevel(audioData: ByteArray, sampleRate: Int = 16000): Float {
        return try {
            val samples = bytesToSamples(audioData)
            
            // Calculate RMS energy
            val rmsEnergy = calculateRMSEnergy(samples)
            
            // Calculate spectral centroid
            val spectralCentroid = calculateSpectralCentroid(samples, sampleRate)
            
            // Estimate noise based on energy and spectral characteristics
            val noiseEstimate = estimateNoiseFromCharacteristics(rmsEnergy, spectralCentroid, samples.size, sampleRate)
            
            noiseEstimate.coerceIn(0.0f, 1.0f)
            
        } catch (e: Exception) {
            Log.e(TAG, "Noise level estimation failed", e)
            0.0f
        }
    }
    
    /**
     * Check audio quality
     * Returns quality score between 0.0 and 1.0
     */
    fun checkAudioQuality(audioData: ByteArray, sampleRate: Int = 16000): AudioQualityResult {
        return try {
            val samples = bytesToSamples(audioData)
            
            val rmsEnergy = calculateRMSEnergy(samples)
            val peakLevel = calculatePeakLevel(samples)
            val dynamicRange = calculateDynamicRange(samples)
            val noiseLevel = estimateNoiseLevel(audioData, sampleRate)
            val clipping = detectClipping(samples)
            
            AudioQualityResult(
                overallScore = calculateQualityScore(rmsEnergy, peakLevel, dynamicRange, noiseLevel, clipping),
                rmsEnergy = rmsEnergy,
                peakLevel = peakLevel,
                dynamicRange = dynamicRange,
                noiseLevel = noiseLevel,
                isClipping = clipping,
                recommendations = generateQualityRecommendations(rmsEnergy, peakLevel, noiseLevel, clipping)
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Audio quality check failed", e)
            AudioQualityResult(
                overallScore = 0.0f,
                error = e.message
            )
        }
    }
    
    /**
     * Apply dynamic range compression
     */
    fun applyDynamicRangeCompression(
        audioData: ByteArray,
        ratio: Float = 4.0f,
        threshold: Float = -20.0f,
        attackTime: Float = 0.01f,
        releaseTime: Float = 0.1f,
        sampleRate: Int = 16000
    ): ByteArray {
        return try {
            val samples = bytesToSamples(audioData)
            val compressedSamples = multibandCompression(samples, ratio, threshold, attackTime, releaseTime, sampleRate)
            samplesToBytes(compressedSamples)
            
        } catch (e: Exception) {
            Log.e(TAG, "Dynamic range compression failed", e)
            audioData
        }
    }
    
    // ==================== Private Implementation Methods ====================
    
    private fun bytesToSamples(audioData: ByteArray): FloatArray {
        return audioData.map { byte ->
            (byte.toInt() and 0xFF) / 128.0f - 1.0f
        }.toFloatArray()
    }
    
    private fun samplesToBytes(samples: FloatArray): ByteArray {
        return samples.map { sample ->
            ((sample + 1.0f) * 128.0f).toInt().coerceIn(0, 255).toByte()
        }.toByteArray()
    }
    
    private fun calculateRMSEnergy(samples: FloatArray): Float {
        val sumSquares = samples.sumOf { sample -> (sample * sample).toDouble() }
        return sqrt(sumSquares / samples.size).toFloat()
    }
    
    private fun calculatePeakLevel(samples: FloatArray): Float {
        return samples.maxOf { abs(it) }
    }
    
    private fun calculateDynamicRange(samples: FloatArray): Float {
        val maxLevel = calculatePeakLevel(samples)
        val minLevel = samples.minOf { abs(it) }
        return 20 * log10(maxLevel / (minLevel + 1e-10f))
    }
    
    private fun detectClipping(samples: FloatArray): Boolean {
        val threshold = 0.95f
        return samples.any { abs(it) >= threshold }
    }
    
    private fun spectralSubtraction(
        samples: FloatArray,
        noiseLevel: Float,
        sampleRate: Int
    ): FloatArray {
        // Simplified spectral subtraction
        // In production, implement proper FFT-based spectral subtraction
        val reductionFactor = 1.0f - (noiseLevel * 0.8f)
        return samples.map { sample -> sample * reductionFactor }.toFloatArray()
    }
    
    private fun adaptiveEchoCancellation(
        samples: FloatArray,
        reference: FloatArray?,
        sampleRate: Int
    ): FloatArray {
        // Simplified adaptive filtering
        // In production, implement proper LMS or NLMS algorithm
        return if (reference != null) {
            // Basic subtraction
            samples.mapIndexed { index, sample ->
                val ref = reference.getOrNull(index) ?: 0.0f
                sample - ref * 0.1f
            }.toFloatArray()
        } else {
            samples
        }
    }
    
    private fun normalizeGain(audioData: ByteArray, targetLevel: Float): ByteArray {
        val samples = bytesToSamples(audioData)
        val currentRMS = calculateRMSEnergy(samples)
        val gainFactor = targetLevel / (currentRMS + 1e-10f)
        
        return samplesToBytes(samples.map { it * gainFactor }.toFloatArray())
    }
    
    private fun applyHighPassFilter(audioData: ByteArray, sampleRate: Int, cutoffFreq: Float): ByteArray {
        // Simple high-pass filter implementation
        val samples = bytesToSamples(audioData)
        val filteredSamples = simpleHighPassFilter(samples, cutoffFreq, sampleRate)
        return samplesToBytes(filteredSamples)
    }
    
    private fun applyLowPassFilter(audioData: ByteArray, sampleRate: Int, cutoffFreq: Float): ByteArray {
        // Simple low-pass filter implementation
        val samples = bytesToSamples(audioData)
        val filteredSamples = simpleLowPassFilter(samples, cutoffFreq, sampleRate)
        return samplesToBytes(filteredSamples)
    }
    
    private fun simpleHighPassFilter(samples: FloatArray, cutoffFreq: Float, sampleRate: Int): FloatArray {
        val alpha = ((2 * PI * cutoffFreq / sampleRate) / (2 * PI * cutoffFreq / sampleRate + 1)).toFloat()
        val filtered = FloatArray(samples.size)

        filtered[0] = samples[0]
        for (i in 1 until samples.size) {
            filtered[i] = alpha * (filtered[i-1] + samples[i] - samples[i-1])
        }

        return filtered
    }

    private fun simpleLowPassFilter(samples: FloatArray, cutoffFreq: Float, sampleRate: Int): FloatArray {
        val alpha = ((2 * PI * cutoffFreq / sampleRate) / (2 * PI * cutoffFreq / sampleRate + 1)).toFloat()
        val filtered = FloatArray(samples.size)

        filtered[0] = samples[0]
        for (i in 1 until samples.size) {
            filtered[i] = alpha * samples[i] + (1 - alpha) * filtered[i-1]
        }

        return filtered
    }
    
    private fun estimateNoiseFromCharacteristics(
        rmsEnergy: Float,
        spectralCentroid: Float,
        sampleCount: Int,
        sampleRate: Int = 16000
    ): Float {
        // Simplified noise estimation based on audio characteristics
        val energyFactor = (1.0f - rmsEnergy).coerceIn(0.0f, 1.0f)
        val spectralFactor = (spectralCentroid / (sampleRate / 2.0f)).coerceIn(0.0f, 1.0f)

        return (energyFactor * 0.6f + spectralFactor * 0.4f).coerceIn(0.0f, 1.0f)
    }
    
    private fun calculateSpectralCentroid(samples: FloatArray, sampleRate: Int): Float {
        // Simplified spectral centroid calculation
        // In production, implement proper FFT-based calculation
        val magnitudes = samples.map { abs(it) }
        val totalMagnitude = magnitudes.sum()
        
        return if (totalMagnitude > 0) {
            magnitudes.mapIndexed { index, magnitude ->
                index * magnitude
            }.sum() / totalMagnitude
        } else {
            0.0f
        }
    }
    
    private fun calculateQualityScore(
        rmsEnergy: Float,
        peakLevel: Float,
        dynamicRange: Float,
        noiseLevel: Float,
        clipping: Boolean
    ): Float {
        var score = 1.0f
        
        // Penalize low energy
        if (rmsEnergy < 0.1f) score -= 0.3f
        
        // Penalize clipping
        if (clipping) score -= 0.4f
        
        // Penalize high noise
        score -= (noiseLevel * 0.5f)
        
        // Penalize low dynamic range
        if (dynamicRange < 10.0f) score -= 0.2f
        
        // Penalize extreme peak levels
        if (peakLevel > 0.95f || peakLevel < 0.05f) score -= 0.2f
        
        return score.coerceIn(0.0f, 1.0f)
    }
    
    private fun generateQualityRecommendations(
        rmsEnergy: Float,
        peakLevel: Float,
        noiseLevel: Float,
        clipping: Boolean
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (rmsEnergy < 0.1f) {
            recommendations.add("Increase microphone gain or speak louder")
        }
        
        if (clipping) {
            recommendations.add("Reduce microphone gain to avoid distortion")
        }
        
        if (noiseLevel > 0.3f) {
            recommendations.add("Move to a quieter environment or use noise reduction")
        }
        
        if (peakLevel < 0.1f) {
            recommendations.add("Audio level is very low - check microphone")
        }
        
        return recommendations
    }
    
    private fun multibandCompression(
        samples: FloatArray,
        ratio: Float,
        threshold: Float,
        attackTime: Float,
        releaseTime: Float,
        sampleRate: Int
    ): FloatArray {
        // Simplified multiband compression
        // In production, implement proper multiband compression
        return samples.map { sample ->
            val level = 20 * log10(abs(sample) + 1e-10f)
            if (level > threshold) {
                val excess = level - threshold
                val compressedExcess = excess / ratio
                val outputLevel = threshold + compressedExcess
                val gain = 10.0.pow((outputLevel - level) / 20.0)
                sample * gain.toFloat()
            } else {
                sample
            }
        }.toFloatArray()
    }
}

/**
 * Audio enhancement configuration
 */
data class AudioEnhancementConfig(
    val noiseReduction: Boolean = true,
    val noiseLevel: Float = 0.3f,
    val echoCancellation: Boolean = true,
    val gainNormalization: Boolean = true,
    val targetLevel: Float = -20.0f, // dB
    val highPassFilter: Boolean = true,
    val cutoffFreq: Float = 80.0f, // Hz
    val lowPassFilter: Boolean = false,
    val cutoffFreqHigh: Float = 8000.0f // Hz
) {
    companion object {
        val DEFAULT = AudioEnhancementConfig()
        val AGGRESSIVE = AudioEnhancementConfig(
            noiseReduction = true,
            noiseLevel = 0.5f,
            echoCancellation = true,
            gainNormalization = true,
            highPassFilter = true,
            lowPassFilter = true
        )
        val MINIMAL = AudioEnhancementConfig(
            noiseReduction = false,
            echoCancellation = false,
            gainNormalization = true,
            highPassFilter = true
        )
    }
}

/**
 * Audio quality check result
 */
data class AudioQualityResult(
    val overallScore: Float,
    val rmsEnergy: Float = 0.0f,
    val peakLevel: Float = 0.0f,
    val dynamicRange: Float = 0.0f,
    val noiseLevel: Float = 0.0f,
    val isClipping: Boolean = false,
    val recommendations: List<String> = emptyList(),
    val error: String? = null
) {
    val isGoodQuality: Boolean get() = overallScore >= 0.7f && error == null
    val needsImprovement: Boolean get() = overallScore < 0.5f && error == null
}