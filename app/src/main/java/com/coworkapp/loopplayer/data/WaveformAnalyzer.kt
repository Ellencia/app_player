package com.coworkapp.loopplayer.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import linc.com.amplituda.Amplituda
import java.io.File

/**
 * 오디오 파형 추출. content:// URI는 캐시 디렉토리로 한 번 복사한 뒤 Amplituda 로 분석.
 *
 * - 결과는 target 막대수로 다운샘플 (버킷 최대값) + 0..1로 정규화
 * - 캐시 파일명에 nanoTime을 붙여 트랙 빠른 전환 시 경쟁 회피
 */
class WaveformAnalyzer(private val appContext: Context) {

    suspend fun analyze(uri: Uri, targetBars: Int = 200): List<Float> =
        withContext(Dispatchers.IO) {
            val tempFile = File(appContext.cacheDir, "waveform_${System.nanoTime()}.tmp")
            try {
                appContext.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                } ?: return@withContext emptyList()

                val processed = Amplituda(appContext).processAudio(tempFile).get()
                val raw: List<Int> = processed.amplitudesAsList()
                downsample(raw, targetBars)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            } finally {
                tempFile.delete()
            }
        }

    private fun downsample(raw: List<Int>, targetBars: Int): List<Float> {
        if (raw.isEmpty()) return emptyList()
        val max = (raw.maxOrNull() ?: 1).coerceAtLeast(1).toFloat()
        if (raw.size <= targetBars) {
            return raw.map { (it / max).coerceIn(0f, 1f) }
        }
        val bucketSize = raw.size.toDouble() / targetBars
        val result = ArrayList<Float>(targetBars)
        for (i in 0 until targetBars) {
            val start = (i * bucketSize).toInt()
            val end = ((i + 1) * bucketSize).toInt().coerceAtMost(raw.size)
            if (start >= end) {
                result.add(0f); continue
            }
            var bucketMax = 0
            for (j in start until end) if (raw[j] > bucketMax) bucketMax = raw[j]
            result.add((bucketMax / max).coerceIn(0f, 1f))
        }
        return result
    }
}
