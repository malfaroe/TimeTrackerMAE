package com.timetrackermae.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeEntryDao {
    @Insert
    suspend fun insert(entry: TimeEntry): Long

    @Update
    suspend fun update(entry: TimeEntry)

    /** The single running entry, if any (Premisa 6 — only one at a time). */
    @Query("SELECT * FROM time_entries WHERE endTime IS NULL LIMIT 1")
    suspend fun getRunning(): TimeEntry?

    @Query("SELECT * FROM time_entries WHERE endTime IS NULL LIMIT 1")
    fun observeRunning(): Flow<TimeEntry?>

    /**
     * Entries overlapping [rangeStart, rangeEnd) — not just ones that start
     * inside the range, since a running or forgotten entry can start before
     * the window and still contribute hours to it (Premisa 9).
     */
    @Query(
        "SELECT * FROM time_entries WHERE startTime < :rangeEnd " +
        "AND (endTime IS NULL OR endTime > :rangeStart) ORDER BY startTime ASC"
    )
    suspend fun getOverlapping(rangeStart: Long, rangeEnd: Long): List<TimeEntry>

    @Query("SELECT * FROM time_entries WHERE id = :id")
    suspend fun getById(id: Long): TimeEntry?

    /**
     * Distinguishes "first week ever" (no prior history at all) from "a prior
     * week existed but had zero hours" — see WeeklyReportCalculator's
     * previousWeekTotalHours contract.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM time_entries WHERE startTime < :beforeMillis)")
    suspend fun hasAnyEntryBefore(beforeMillis: Long): Boolean
}
