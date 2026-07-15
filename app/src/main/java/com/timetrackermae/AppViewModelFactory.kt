package com.timetrackermae

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.timetrackermae.data.AppDatabase
import com.timetrackermae.report.ReportViewModel
import com.timetrackermae.timer.TimerViewModel

class AppViewModelFactory(private val db: AppDatabase) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        TimerViewModel::class.java -> TimerViewModel(db.projectDao(), db.timeEntryDao()) as T
        ReportViewModel::class.java -> ReportViewModel(db.projectDao(), db.timeEntryDao()) as T
        else -> throw IllegalArgumentException("Unknown ViewModel: $modelClass")
    }
}
