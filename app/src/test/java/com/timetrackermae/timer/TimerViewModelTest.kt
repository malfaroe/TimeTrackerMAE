package com.timetrackermae.timer

import com.timetrackermae.data.Project
import com.timetrackermae.data.ProjectDao
import com.timetrackermae.data.TimeEntry
import com.timetrackermae.data.TimeEntryDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

private class FakeProjectDao : ProjectDao {
    val projects = mutableListOf<Project>()
    val flow = MutableStateFlow<List<Project>>(emptyList())
    override suspend fun insert(project: Project): Long {
        val id = (projects.maxOfOrNull { it.id } ?: 0L) + 1
        projects.add(project.copy(id = id))
        flow.value = projects.toList()
        return id
    }
    override fun getAll(): Flow<List<Project>> = flow
    override suspend fun count(): Int = projects.size
}

private class FakeTimeEntryDao : TimeEntryDao {
    val entries = mutableListOf<TimeEntry>()
    val runningFlow = MutableStateFlow<TimeEntry?>(null)
    private fun refresh() { runningFlow.value = entries.find { it.endTime == null } }

    override suspend fun insert(entry: TimeEntry): Long {
        val id = (entries.maxOfOrNull { it.id } ?: 0L) + 1
        entries.add(entry.copy(id = id))
        refresh()
        return id
    }
    override suspend fun update(entry: TimeEntry) {
        val idx = entries.indexOfFirst { it.id == entry.id }
        if (idx >= 0) entries[idx] = entry
        refresh()
    }
    override suspend fun getRunning(): TimeEntry? = entries.find { it.endTime == null }
    override fun observeRunning(): Flow<TimeEntry?> = runningFlow
    override suspend fun getOverlapping(rangeStart: Long, rangeEnd: Long): List<TimeEntry> =
        entries.filter { it.startTime < rangeEnd && (it.endTime == null || it.endTime!! > rangeStart) }
    override suspend fun getById(id: Long): TimeEntry? = entries.find { it.id == id }
}

private class FakeTimeSource(var now: Long) : TimeSource {
    override fun nowMillis(): Long = now
}

@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModelTest {

    private lateinit var projectDao: FakeProjectDao
    private lateinit var timeEntryDao: FakeTimeEntryDao
    private lateinit var timeSource: FakeTimeSource
    private lateinit var viewModel: TimerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        projectDao = FakeProjectDao()
        timeEntryDao = FakeTimeEntryDao()
        timeSource = FakeTimeSource(now = 10_000L)
        viewModel = TimerViewModel(projectDao, timeEntryDao, timeSource)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `startTimer with no running timer creates a new entry`() {
        viewModel.startTimer(projectId = 1L)
        val running = timeEntryDao.entries.find { it.endTime == null }
        assertNotNull(running)
        assertEquals(1L, running!!.projectId)
    }

    @Test
    fun `startTimer while another timer runs auto-stops the previous one`() {
        viewModel.startTimer(projectId = 1L)
        timeSource.now = 20_000L
        viewModel.startTimer(projectId = 2L)

        val stillRunning = timeEntryDao.entries.filter { it.endTime == null }
        assertEquals(1, stillRunning.size)
        assertEquals(2L, stillRunning[0].projectId)

        val previousEntry = timeEntryDao.entries.first { it.projectId == 1L }
        assertEquals(20_000L, previousEntry.endTime)
    }

    @Test
    fun `stopTimer with an active timer sets endTime`() {
        viewModel.startTimer(projectId = 1L)
        timeSource.now = 15_000L
        viewModel.stopTimer()

        val entry = timeEntryDao.entries.first()
        assertEquals(15_000L, entry.endTime)
    }

    @Test
    fun `stopTimer with no active timer is a no-op and does not throw`() {
        viewModel.stopTimer()
        assertEquals(0, timeEntryDao.entries.size)
    }

    @Test
    fun `elapsedMillis returns 0 when there is no running timer`() {
        assertEquals(0L, viewModel.elapsedMillis())
    }

    @Test
    fun `elapsedMillis is clamped to 0 when the system clock rolls backward`() {
        viewModel.startTimer(projectId = 1L) // startTime = 10_000
        timeSource.now = 5_000L // clock rolled backward
        assertEquals(0L, viewModel.elapsedMillis())
    }

    @Test
    fun `elapsedMillis returns the normal positive delta`() {
        viewModel.startTimer(projectId = 1L) // startTime = 10_000
        timeSource.now = 13_000L
        assertEquals(3_000L, viewModel.elapsedMillis())
    }
}
