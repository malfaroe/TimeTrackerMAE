package com.timetrackermae.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timetrackermae.data.Project
import com.timetrackermae.data.ProjectDao
import com.timetrackermae.data.TimeEntry
import com.timetrackermae.data.TimeEntryDao
import com.timetrackermae.report.WeeklyReportCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class TodayProjectSummary(val projectName: String, val hours: Double)

data class TimerUiState(
    val projects: List<Project> = emptyList(),
    val runningEntry: TimeEntry? = null,
    val runningProjectName: String? = null,
    val todaySummary: List<TodayProjectSummary> = emptyList()
)

class TimerViewModel(
    private val projectDao: ProjectDao,
    private val timeEntryDao: TimeEntryDao,
    private val timeSource: TimeSource = SystemTimeSource,
    private val zone: ZoneId = ZoneId.systemDefault()
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    init {
        projectDao.getAll().onEach { projects ->
            _uiState.value = _uiState.value.copy(
                projects = projects,
                runningProjectName = projects.find { it.id == _uiState.value.runningEntry?.projectId }?.name
            )
        }.launchIn(viewModelScope)

        timeEntryDao.observeRunning().onEach { running ->
            val projectName = _uiState.value.projects.find { it.id == running?.projectId }?.name
            _uiState.value = _uiState.value.copy(runningEntry = running, runningProjectName = projectName)
            refreshTodaySummary()
        }.launchIn(viewModelScope)
    }

    /** Premisa 6: starting a new timer auto-stops any other timer already running. */
    fun startTimer(projectId: Long) = viewModelScope.launch {
        val running = timeEntryDao.getRunning()
        if (running != null) {
            timeEntryDao.update(running.copy(endTime = timeSource.nowMillis()))
        }
        timeEntryDao.insert(TimeEntry(projectId = projectId, startTime = timeSource.nowMillis()))
    }

    fun stopTimer() = viewModelScope.launch {
        val running = timeEntryDao.getRunning() ?: return@launch
        timeEntryDao.update(running.copy(endTime = timeSource.nowMillis()))
    }

    fun elapsedMillis(): Long {
        val running = _uiState.value.runningEntry ?: return 0L
        return ElapsedTimeCalculator.elapsedMillis(running.startTime, timeSource.nowMillis())
    }

    /** Premisa 10: project creation lives inline in the Timer dropdown, no dedicated screen. */
    fun createProject(name: String) = viewModelScope.launch {
        val nextIndex = projectDao.count() % 8
        projectDao.insert(Project(name = name, colorIndex = nextIndex))
    }

    /**
     * Reuses WeeklyReportCalculator.splitAcrossDays (the same boundary-split
     * logic as the weekly report, Premisa 9) instead of writing a second,
     * separate aggregation for "today" — DRY.
     */
    private suspend fun refreshTodaySummary() {
        val today = LocalDate.now(zone)
        val todayStartMillis = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val todayEndMillis = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val entries = timeEntryDao.getOverlapping(todayStartMillis, todayEndMillis)
        val now = Instant.ofEpochMilli(timeSource.nowMillis())

        val hoursByProject = LinkedHashMap<Long, Double>()
        for (entry in entries) {
            for ((date, hours) in WeeklyReportCalculator.splitAcrossDays(entry, zone, now)) {
                if (date != today) continue
                hoursByProject[entry.projectId] = (hoursByProject[entry.projectId] ?: 0.0) + hours
            }
        }

        val projects = _uiState.value.projects
        val summary = hoursByProject.mapNotNull { (projectId, hours) ->
            projects.find { it.id == projectId }?.let { TodayProjectSummary(it.name, hours) }
        }
        _uiState.value = _uiState.value.copy(todaySummary = summary)
    }
}
