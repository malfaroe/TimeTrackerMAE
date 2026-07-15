package com.timetrackermae.report

import com.timetrackermae.data.Project
import com.timetrackermae.data.TimeEntry
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.max

data class DayHours(val date: LocalDate, val dayOfWeek: DayOfWeek, val hours: Double)

data class ProjectHours(val projectId: Long, val projectName: String, val colorIndex: Int, val hours: Double)

data class WeeklyReport(
    val weekStart: LocalDate,
    val dailyHours: List<DayHours>,
    val projectHours: List<ProjectHours>,
    val othersHours: Double,
    val totalHours: Double,
    val deltaHours: Double?
)

/**
 * Pure Kotlin, no Android dependency — see design doc Code Quality Issue 3.
 * Splits each TimeEntry proportionally across every day boundary it crosses
 * (Premisa 9) purely in memory; the stored Room row is never touched.
 */
object WeeklyReportCalculator {

    private const val OTHERS_THRESHOLD = 0.05

    /**
     * @param weekEntries entries overlapping [weekStart, weekStart+7d) — caller
     *   fetches these via TimeEntryDao.getOverlapping using a range wide enough
     *   to catch entries that started before the week but still spill into it.
     * @param previousWeekTotalHours null means no prior week of data exists at
     *   all (first week of use — Success Criteria "Primera semana — sin
     *   comparación"); 0.0 means a prior week existed with zero hours tracked.
     */
    fun calculate(
        weekEntries: List<TimeEntry>,
        projects: Map<Long, Project>,
        weekStart: LocalDate,
        zone: ZoneId,
        now: Instant,
        previousWeekTotalHours: Double?
    ): WeeklyReport {
        require(weekStart.dayOfWeek == DayOfWeek.MONDAY) { "weekStart must be a Monday" }
        val weekEnd = weekStart.plusDays(7)

        // date -> hours, for every day touched by any entry (not just this week —
        // splitAcrossDays returns the full span, we filter to this week below).
        val dayTotals = LinkedHashMap<LocalDate, Double>()
        val projectTotals = LinkedHashMap<Long, Double>()

        for (entry in weekEntries) {
            val perDay = splitAcrossDays(entry, zone, now)
            for ((date, hours) in perDay) {
                if (date < weekStart || date >= weekEnd) continue
                dayTotals[date] = (dayTotals[date] ?: 0.0) + hours
                projectTotals[entry.projectId] = (projectTotals[entry.projectId] ?: 0.0) + hours
            }
        }

        val dailyHours = (0 until 7).map { offset ->
            val date = weekStart.plusDays(offset.toLong())
            DayHours(date, date.dayOfWeek, dayTotals[date] ?: 0.0)
        }

        val totalHours = dailyHours.sumOf { it.hours }

        val (grouped, othersHours) = groupOthers(projectTotals, projects, totalHours)

        val deltaHours = previousWeekTotalHours?.let { totalHours - it }

        return WeeklyReport(weekStart, dailyHours, grouped, othersHours, totalHours, deltaHours)
    }

    /**
     * A single TimeEntry split into (date, hours) pairs at every midnight it
     * crosses. A running entry (endTime == null) uses `now` as the effective
     * end, clamped to never be before startTime (Premisa 7 clock-rollback
     * guard applies at the ViewModel level; here we just clamp non-negative).
     */
    fun splitAcrossDays(entry: TimeEntry, zone: ZoneId, now: Instant): List<Pair<LocalDate, Double>> {
        val startInstant = Instant.ofEpochMilli(entry.startTime)
        val rawEndInstant = entry.endTime?.let { Instant.ofEpochMilli(it) } ?: now
        val endInstant = if (rawEndInstant.isBefore(startInstant)) startInstant else rawEndInstant

        if (startInstant == endInstant) return emptyList()

        val result = mutableListOf<Pair<LocalDate, Double>>()
        var cursor = startInstant
        val startDate = startInstant.atZone(zone).toLocalDate()
        var currentDate = startDate

        while (cursor.isBefore(endInstant)) {
            val nextMidnight = currentDate.plusDays(1).atStartOfDay(zone).toInstant()
            val segmentEnd = if (nextMidnight.isBefore(endInstant)) nextMidnight else endInstant
            val hours = (segmentEnd.toEpochMilli() - cursor.toEpochMilli()) / 3_600_000.0
            if (hours > 0.0) result.add(currentDate to hours)
            cursor = segmentEnd
            currentDate = currentDate.plusDays(1)
        }
        return result
    }

    private fun groupOthers(
        projectTotals: Map<Long, Double>,
        projects: Map<Long, Project>,
        totalHours: Double
    ): Pair<List<ProjectHours>, Double> {
        if (totalHours <= 0.0) {
            return projectTotals.mapNotNull { (id, hours) ->
                projects[id]?.let { ProjectHours(id, it.name, it.colorIndex, hours) }
            } to 0.0
        }
        val main = mutableListOf<ProjectHours>()
        var others = 0.0
        for ((id, hours) in projectTotals) {
            val project = projects[id] ?: continue
            if (hours / totalHours >= OTHERS_THRESHOLD) {
                main.add(ProjectHours(id, project.name, project.colorIndex, hours))
            } else {
                others += hours
            }
        }
        return main to others
    }
}
