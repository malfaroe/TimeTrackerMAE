package com.timetrackermae.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * endTime = null means the timer is currently running (Premisa 7 — no
 * foreground service, elapsed is always recomputed as now - startTime).
 * A row that crosses a day/week boundary is never split here — splitting
 * happens only in memory during report aggregation (Premisa 9).
 */
@Entity(tableName = "time_entries")
data class TimeEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val startTime: Long,
    val endTime: Long? = null
)
