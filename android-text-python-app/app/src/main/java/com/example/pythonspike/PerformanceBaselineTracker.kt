package com.example.pythonspike

data class PerformanceBaselineSnapshot(
    val totalCount: Int,
    val coldCount: Int,
    val warmCount: Int,
    val avgDurationMs: Long,
    val coldAvgDurationMs: Long,
    val warmAvgDurationMs: Long
)

object PerformanceBaselineTracker {
    private val lock = Any()

    private var totalCount = 0
    private var coldCount = 0
    private var warmCount = 0
    private var totalDurationMs = 0L
    private var coldDurationMs = 0L
    private var warmDurationMs = 0L

    fun record(isColdStart: Boolean, durationMs: Long) {
        synchronized(lock) {
            totalCount += 1
            totalDurationMs += durationMs

            if (isColdStart) {
                coldCount += 1
                coldDurationMs += durationMs
            } else {
                warmCount += 1
                warmDurationMs += durationMs
            }
        }
    }

    fun snapshot(): PerformanceBaselineSnapshot {
        synchronized(lock) {
            val avg = if (totalCount == 0) 0L else totalDurationMs / totalCount
            val coldAvg = if (coldCount == 0) 0L else coldDurationMs / coldCount
            val warmAvg = if (warmCount == 0) 0L else warmDurationMs / warmCount

            return PerformanceBaselineSnapshot(
                totalCount = totalCount,
                coldCount = coldCount,
                warmCount = warmCount,
                avgDurationMs = avg,
                coldAvgDurationMs = coldAvg,
                warmAvgDurationMs = warmAvg
            )
        }
    }

    fun resetForTests() {
        synchronized(lock) {
            totalCount = 0
            coldCount = 0
            warmCount = 0
            totalDurationMs = 0L
            coldDurationMs = 0L
            warmDurationMs = 0L
        }
    }
}
