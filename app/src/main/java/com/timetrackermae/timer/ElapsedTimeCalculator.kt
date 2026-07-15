package com.timetrackermae.timer

/**
 * Pure function, no Android dependency — Premisa 7: if the system clock rolls
 * backward (timezone change, manual clock adjustment) the elapsed time shown
 * is clamped to 0 instead of going negative. A clock jumping forward is
 * accepted as-is (see design doc Architecture Issue 2 — not distinguishable
 * from real elapsed time without fragile heuristics).
 */
object ElapsedTimeCalculator {
    fun elapsedMillis(startTime: Long, now: Long): Long =
        (now - startTime).coerceAtLeast(0L)
}
