package com.timetrackermae.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timetrackermae.data.Project
import com.timetrackermae.data.ProjectDao
import com.timetrackermae.data.TimeEntryDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class ReportUiState(
    val loading: Boolean = true,
    val report: WeeklyReport? = null,
    val barSpecs: List<BarSpec> = emptyList(),
    val donutSlices: List<DonutSlice> = emptyList()
)

class ReportViewModel(
    private val projectDao: ProjectDao,
    private val timeEntryDao: TimeEntryDao,
    private val zone: ZoneId = ZoneId.systemDefault()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() = viewModelScope.launch {
        val today = LocalDate.now(zone)
        // Premisa 8: week is Monday-Sunday.
        val weekStart = today.minusDays((today.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
        val weekEnd = weekStart.plusDays(7)
        val prevWeekStart = weekStart.minusDays(7)

        val rangeStartMillis = prevWeekStart.atStartOfDay(zone).toInstant().toEpochMilli()
        val rangeEndMillis = weekEnd.atStartOfDay(zone).toInstant().toEpochMilli()
        val allEntries = timeEntryDao.getOverlapping(rangeStartMillis, rangeEndMillis)

        val projects: Map<Long, Project> = projectDao.getAll().first().associateBy { it.id }
        val now = Instant.now()

        val hasPriorHistory = timeEntryDao.hasAnyEntryBefore(
            weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
        )

        val previousWeekTotalHours: Double? = if (!hasPriorHistory) {
            null
        } else {
            val prevReport = WeeklyReportCalculator.calculate(
                weekEntries = allEntries, projects = projects, weekStart = prevWeekStart,
                zone = zone, now = now, previousWeekTotalHours = null
            )
            prevReport.totalHours
        }

        val report = WeeklyReportCalculator.calculate(
            weekEntries = allEntries, projects = projects, weekStart = weekStart,
            zone = zone, now = now, previousWeekTotalHours = previousWeekTotalHours
        )

        _uiState.value = ReportUiState(
            loading = false,
            report = report,
            barSpecs = ChartMath.barSpecs(report.dailyHours),
            donutSlices = ChartMath.donutSlices(report.projectHours, report.othersHours)
        )
    }
}
