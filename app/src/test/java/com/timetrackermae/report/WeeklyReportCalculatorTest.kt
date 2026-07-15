package com.timetrackermae.report

import com.timetrackermae.data.Project
import com.timetrackermae.data.TimeEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class WeeklyReportCalculatorTest {

    private val zone: ZoneId = ZoneOffset.UTC
    private val mondayWeek1: LocalDate = LocalDate.of(2026, 7, 6) // a Monday
    private val projects = mapOf(
        1L to Project(1L, "BloqueMAE", colorIndex = 0),
        2L to Project(2L, "Lectura", colorIndex = 1)
    )

    private fun millisAt(date: LocalDate, hour: Int, minute: Int = 0): Long =
        date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

    // Case 1: entry entirely within a single day -> 1 bucket
    @Test
    fun `entry within a single day produces one bucket`() {
        val entry = TimeEntry(1, projectId = 1L, startTime = millisAt(mondayWeek1, 9), endTime = millisAt(mondayWeek1, 11))
        val split = WeeklyReportCalculator.splitAcrossDays(entry, zone, Instant.now())
        assertEquals(1, split.size)
        assertEquals(mondayWeek1, split[0].first)
        assertEquals(2.0, split[0].second, 0.0001)
    }

    // Case 2: entry crosses exactly one midnight -> 2 buckets
    @Test
    fun `entry crossing one midnight splits into two buckets`() {
        val tue = mondayWeek1.plusDays(1)
        val entry = TimeEntry(1, projectId = 1L, startTime = millisAt(mondayWeek1, 22), endTime = millisAt(tue, 2))
        val split = WeeklyReportCalculator.splitAcrossDays(entry, zone, Instant.now())
        assertEquals(2, split.size)
        assertEquals(mondayWeek1 to 2.0, split[0].first to split[0].second)
        assertEquals(tue to 2.0, split[1].first to split[1].second)
    }

    // Case 3: entry crosses 2+ midnights (timer forgotten running for days)
    @Test
    fun `entry crossing multiple midnights splits into N buckets`() {
        val sat = mondayWeek1.plusDays(5) // Saturday
        val mon2 = mondayWeek1.plusDays(9) // following Monday
        // Sat 22:00 -> Mon 03:00 (next week): Sat(2h), Sun(24h), Mon(3h)
        val entry = TimeEntry(1, projectId = 1L, startTime = millisAt(sat, 22), endTime = millisAt(mon2, 3))
        val split = WeeklyReportCalculator.splitAcrossDays(entry, zone, Instant.now())
        assertEquals(3, split.size)
        assertEquals(2.0, split[0].second, 0.0001)
        assertEquals(24.0, split[1].second, 0.0001)
        assertEquals(3.0, split[2].second, 0.0001)
    }

    // Case 4: entry crosses the week boundary (Sun -> Mon) -> buckets land in different weeks
    @Test
    fun `entry crossing week boundary allocates hours to each week separately`() {
        val sun = mondayWeek1.plusDays(6)
        val mon2 = mondayWeek1.plusDays(7)
        val entry = TimeEntry(1, projectId = 1L, startTime = millisAt(sun, 23), endTime = millisAt(mon2, 1))

        val week1Report = WeeklyReportCalculator.calculate(
            weekEntries = listOf(entry), projects = projects, weekStart = mondayWeek1,
            zone = zone, now = Instant.now(), previousWeekTotalHours = null
        )
        val week2Report = WeeklyReportCalculator.calculate(
            weekEntries = listOf(entry), projects = projects, weekStart = mon2,
            zone = zone, now = Instant.now(), previousWeekTotalHours = null
        )
        assertEquals(1.0, week1Report.totalHours, 0.0001) // Sun 23:00-24:00
        assertEquals(1.0, week2Report.totalHours, 0.0001) // Mon 00:00-01:00
    }

    // Case 5: previous week exists -> delta computed
    @Test
    fun `delta is computed when previous week data exists`() {
        val entry = TimeEntry(1, projectId = 1L, startTime = millisAt(mondayWeek1, 9), endTime = millisAt(mondayWeek1, 13))
        val report = WeeklyReportCalculator.calculate(
            weekEntries = listOf(entry), projects = projects, weekStart = mondayWeek1,
            zone = zone, now = Instant.now(), previousWeekTotalHours = 2.0
        )
        assertEquals(4.0, report.totalHours, 0.0001)
        assertEquals(2.0, report.deltaHours!!, 0.0001)
    }

    // Case 6: first week of use -> deltaHours is null, not zero
    @Test
    fun `first week of use has null delta`() {
        val entry = TimeEntry(1, projectId = 1L, startTime = millisAt(mondayWeek1, 9), endTime = millisAt(mondayWeek1, 10))
        val report = WeeklyReportCalculator.calculate(
            weekEntries = listOf(entry), projects = projects, weekStart = mondayWeek1,
            zone = zone, now = Instant.now(), previousWeekTotalHours = null
        )
        assertNull(report.deltaHours)
    }

    // Case 7: project >= 5% of total gets its own wedge
    @Test
    fun `project at or above 5 percent gets its own bucket`() {
        val big = TimeEntry(1, projectId = 1L, startTime = millisAt(mondayWeek1, 0), endTime = millisAt(mondayWeek1, 19))
        val small = TimeEntry(2, projectId = 2L, startTime = millisAt(mondayWeek1, 19), endTime = millisAt(mondayWeek1, 20))
        val report = WeeklyReportCalculator.calculate(
            weekEntries = listOf(big, small), projects = projects, weekStart = mondayWeek1,
            zone = zone, now = Instant.now(), previousWeekTotalHours = null
        )
        // small = 1h / 20h = 5% exactly -> stays as its own bucket, not "Otros"
        assertEquals(2, report.projectHours.size)
        assertEquals(0.0, report.othersHours, 0.0001)
    }

    // Case 8: project under 5% of total gets grouped into "Otros"
    @Test
    fun `project under 5 percent is grouped into others`() {
        val big = TimeEntry(1, projectId = 1L, startTime = millisAt(mondayWeek1, 0), endTime = millisAt(mondayWeek1, 20))
        val tiny = TimeEntry(2, projectId = 2L, startTime = millisAt(mondayWeek1, 20), endTime = millisAt(mondayWeek1, 20, 30))
        val report = WeeklyReportCalculator.calculate(
            weekEntries = listOf(big, tiny), projects = projects, weekStart = mondayWeek1,
            zone = zone, now = Instant.now(), previousWeekTotalHours = null
        )
        // tiny = 0.5h / 20.5h ≈ 2.4% -> grouped into "Otros"
        assertEquals(1, report.projectHours.size)
        assertTrue(report.othersHours > 0.0)
    }

    // Bonus: a day with zero entries still appears in dailyHours as 0.0, not omitted
    @Test
    fun `day with no entries is present with zero hours`() {
        val entry = TimeEntry(1, projectId = 1L, startTime = millisAt(mondayWeek1, 9), endTime = millisAt(mondayWeek1, 10))
        val report = WeeklyReportCalculator.calculate(
            weekEntries = listOf(entry), projects = projects, weekStart = mondayWeek1,
            zone = zone, now = Instant.now(), previousWeekTotalHours = null
        )
        assertEquals(7, report.dailyHours.size)
        assertEquals(0.0, report.dailyHours.last().hours, 0.0001) // Sunday untouched
    }
}
