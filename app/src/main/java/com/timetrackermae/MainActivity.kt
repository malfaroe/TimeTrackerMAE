package com.timetrackermae

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.timetrackermae.data.AppDatabase
import com.timetrackermae.report.ReportScreen
import com.timetrackermae.report.ReportViewModel
import com.timetrackermae.timer.TimerScreen
import com.timetrackermae.timer.TimerViewModel
import com.timetrackermae.ui.theme.TimeTrackerMAETheme

private enum class Tab(val label: String) { TIMER("Timer"), REPORT("Reporte") }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.getInstance(applicationContext)
        val factory = AppViewModelFactory(db)

        setContent {
            TimeTrackerMAETheme {
                AppRoot(factory)
            }
        }
    }
}

@Composable
private fun AppRoot(factory: AppViewModelFactory) {
    var selectedTab by remember { mutableStateOf(Tab.TIMER) }
    val timerViewModel: TimerViewModel = viewModel(factory = factory)
    val reportViewModel: ReportViewModel = viewModel(factory = factory)

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == Tab.TIMER,
                    onClick = { selectedTab = Tab.TIMER },
                    icon = {},
                    label = { Text(Tab.TIMER.label) }
                )
                NavigationBarItem(
                    selected = selectedTab == Tab.REPORT,
                    onClick = {
                        selectedTab = Tab.REPORT
                        reportViewModel.load()
                    },
                    icon = {},
                    label = { Text(Tab.REPORT.label) }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                Tab.TIMER -> TimerScreen(timerViewModel)
                Tab.REPORT -> ReportScreen(reportViewModel)
            }
        }
    }
}
