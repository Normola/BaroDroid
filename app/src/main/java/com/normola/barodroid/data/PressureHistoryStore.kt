package com.normola.barodroid.data

import android.content.Context
import com.normola.barodroid.core.PressureMath
import com.normola.barodroid.core.PressureSample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Append-only history of pressure samples, kept as a small CSV file in internal
 * storage and mirrored in memory.
 *
 * A database would be overkill: a couple of days of samples at one every few
 * minutes is a few hundred lines. The in-memory mirror matters more than the
 * format — every process that touches the history (the app, the widgets, the
 * sampling worker) shares this instance, so the file is parsed once per process
 * rather than once per reading.
 */
class PressureHistoryStore(context: Context) {

    private val appContext = context.applicationContext
    private val file: File get() = File(appContext.filesDir, FILE_NAME)
    private val mutex = Mutex()

    @Volatile
    private var loadedFromDisk = false

    private val _samples = MutableStateFlow<List<PressureSample>>(emptyList())

    /** Samples currently in memory, oldest first. Empty until [load] has run. */
    val samples: StateFlow<List<PressureSample>> = _samples.asStateFlow()

    /** The newest sample held in memory, without touching the disk. */
    val latest: PressureSample? get() = _samples.value.lastOrNull()

    /** Reads the history, from memory once the file has been parsed once. */
    suspend fun load(): List<PressureSample> = mutex.withLock { loadLocked() }

    /** Forces a re-read from disk, for the rare case of an external change. */
    suspend fun refresh(): List<PressureSample> = mutex.withLock {
        loadedFromDisk = false
        loadLocked()
    }

    /**
     * Records a sample, unless an equally recent one is already stored.
     *
     * Only the newest in-memory sample is consulted, so a reading costs nothing
     * but a comparison until the interval has elapsed and there is something
     * genuinely new to append.
     *
     * @return true when the sample was written.
     */
    suspend fun record(
        hPa: Double,
        timestamp: Long = System.currentTimeMillis(),
        minIntervalMillis: Long = MIN_INTERVAL_MILLIS,
    ): Boolean {
        if (!PressureMath.isPlausible(hPa)) return false

        // Cheap check before taking the lock: the common case by far is a
        // reading that arrives well inside the interval and is simply dropped.
        val newestInMemory = latest
        if (loadedFromDisk && newestInMemory != null &&
            timestamp - newestInMemory.timestamp < minIntervalMillis
        ) {
            return false
        }

        return mutex.withLock {
            val existing = loadLocked()
            val newest = existing.lastOrNull()
            if (newest != null && timestamp - newest.timestamp < minIntervalMillis) return@withLock false

            val sample = PressureSample(timestamp, hPa)
            val updated = (existing + sample).prune(timestamp)
            if (updated.size < existing.size + 1) {
                writeAll(updated)
            } else {
                appendLine(sample)
            }
            _samples.value = updated
            true
        }
    }

    /** Drops every stored sample; used by the "clear history" action in settings. */
    suspend fun clear() = mutex.withLock {
        withContext(Dispatchers.IO) { file.delete() }
        loadedFromDisk = true
        _samples.value = emptyList()
    }

    private suspend fun loadLocked(): List<PressureSample> {
        if (loadedFromDisk) {
            // Pruning is cheap and keeps the window honest as time passes.
            val pruned = _samples.value.prune(System.currentTimeMillis())
            if (pruned.size != _samples.value.size) _samples.value = pruned
            return pruned
        }

        val parsed = withContext(Dispatchers.IO) {
            if (!file.exists()) return@withContext emptyList<PressureSample>()
            runCatching {
                file.readLines().mapNotNull(::parseLine)
            }.getOrDefault(emptyList())
        }.sortedBy { it.timestamp }.prune(System.currentTimeMillis())

        loadedFromDisk = true
        _samples.value = parsed
        return parsed
    }

    private fun parseLine(line: String): PressureSample? {
        val parts = line.split(',')
        if (parts.size != 2) return null
        val timestamp = parts[0].trim().toLongOrNull() ?: return null
        val hPa = parts[1].trim().toDoubleOrNull() ?: return null
        if (!PressureMath.isPlausible(hPa)) return null
        return PressureSample(timestamp, hPa)
    }

    private suspend fun appendLine(sample: PressureSample) = withContext(Dispatchers.IO) {
        runCatching { file.appendText(sample.toCsv()) }
    }

    private suspend fun writeAll(samples: List<PressureSample>) = withContext(Dispatchers.IO) {
        runCatching {
            val temp = File(file.parentFile, "$FILE_NAME.tmp")
            temp.writeText(samples.joinToString(separator = "") { it.toCsv() })
            if (!temp.renameTo(file)) {
                file.writeText(samples.joinToString(separator = "") { it.toCsv() })
                temp.delete()
            }
        }
    }

    private fun PressureSample.toCsv(): String = "$timestamp,$hPa\n"

    private fun List<PressureSample>.prune(now: Long): List<PressureSample> {
        val cutoff = now - HISTORY_MILLIS
        val recent = filter { it.timestamp in cutoff..(now + CLOCK_SKEW_MILLIS) }
        return if (recent.size > MAX_SAMPLES) recent.takeLast(MAX_SAMPLES) else recent
    }

    companion object {
        private const val FILE_NAME = "pressure_history.csv"
        private const val MIN_INTERVAL_MILLIS = 60_000L

        /** Two days of history is enough for the 24 h graph plus a margin. */
        const val HISTORY_MILLIS = 48 * 60 * 60 * 1000L
        private const val CLOCK_SKEW_MILLIS = 5 * 60 * 1000L
        private const val MAX_SAMPLES = 4000
    }
}
