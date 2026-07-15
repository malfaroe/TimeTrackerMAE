package com.timetrackermae.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.timetrackermae.data.Project
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun TimerScreen(viewModel: TimerViewModel) {
    val state by viewModel.uiState.collectAsState()
    var tickMillis by remember { mutableStateOf(0L) }

    LaunchedEffect(state.runningEntry) {
        while (state.runningEntry != null) {
            tickMillis = viewModel.elapsedMillis()
            delay(1000)
        }
        tickMillis = 0L
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    val mustCreateFirstProject = state.projects.isEmpty()

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (mustCreateFirstProject) {
            // Premisa 10: first launch forces project creation before start is usable.
            CreateProjectDialog(
                onConfirm = { name -> viewModel.createProject(name) },
                onDismissAllowed = false
            )
        } else {
            ProjectDropdown(
                projects = state.projects,
                selectedName = state.runningProjectName,
                onSelect = { viewModel.startTimer(it.id) },
                onRequestCreate = { showCreateDialog = true }
            )

            Box(modifier = Modifier.height(220.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatElapsed(tickMillis),
                        style = MaterialTheme.typography.displayMedium,
                        textAlign = TextAlign.Center
                    )
                    if (state.runningEntry != null) {
                        Text("corriendo", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (state.runningEntry != null) {
                Button(onClick = { viewModel.stopTimer() }, shape = CircleShape, modifier = Modifier.size(140.dp)) {
                    Text("■ STOP")
                }
            } else {
                OutlinedButton(
                    onClick = { /* select a project above to start */ },
                    enabled = false,
                    shape = CircleShape,
                    modifier = Modifier.size(140.dp)
                ) { Text("Elegí un proyecto") }
            }

            Text("Hoy", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 30.dp, bottom = 10.dp))
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(state.todaySummary) { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(item.projectName)
                        Text(formatHours(item.hours))
                    }
                }
            }
        }

        if (showCreateDialog) {
            CreateProjectDialog(
                onConfirm = { name -> viewModel.createProject(name) },
                onDismiss = { showCreateDialog = false },
                onDismissAllowed = true
            )
        }
    }
}

@Composable
private fun ProjectDropdown(
    projects: List<Project>,
    selectedName: String?,
    onSelect: (Project) -> Unit,
    onRequestCreate: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedName ?: "Elegí un proyecto",
            onValueChange = {},
            readOnly = true,
            label = { Text("Proyecto") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth()
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            projects.forEach { project ->
                DropdownMenuItem(text = { Text(project.name) }, onClick = {
                    expanded = false
                    onSelect(project)
                })
            }
            DropdownMenuItem(text = { Text("+ Nuevo proyecto") }, onClick = {
                expanded = false
                onRequestCreate()
            })
        }
    }
}

@Composable
private fun CreateProjectDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit = {},
    onDismissAllowed: Boolean
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (onDismissAllowed) onDismiss() },
        title = { Text(if (onDismissAllowed) "Nuevo proyecto" else "Creá tu primer proyecto") },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre del proyecto…") })
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    onConfirm(name.trim())
                    if (onDismissAllowed) onDismiss()
                }
            }) { Text("Guardar") }
        },
        dismissButton = if (onDismissAllowed) {
            { TextButton(onClick = onDismiss) { Text("Cancelar") } }
        } else null
    )
}

private fun formatElapsed(millis: Long): String {
    val totalSeconds = millis / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
}

private fun formatHours(hours: Double): String {
    val totalMinutes = (hours * 60).toLong()
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
