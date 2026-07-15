package com.timetrackermae.timer

/** Injectable so tests can fake "now" without touching the system clock. */
fun interface TimeSource {
    fun nowMillis(): Long
}

val SystemTimeSource = TimeSource { System.currentTimeMillis() }
