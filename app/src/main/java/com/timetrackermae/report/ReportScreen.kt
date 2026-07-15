package com.timetrackermae.report

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.timetrackermae.R

@Composable
fun ReportScreen(viewModel: ReportViewModel) {
    val state by viewModel.uiState.collectAsState()
    val report = state.report

    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
        Text("Esta semana", style = MaterialTheme.typography.labelMedium)

        val totalLabel = report?.let { formatHoursLabel(it.totalHours) } ?: "0h 0m"
        val deltaLabel = report?.deltaHours?.let { d ->
            val sign = if (d >= 0) "+" else "−"
            "($sign${formatHoursLabel(kotlin.math.abs(d))} vs sem. pasada)"
        } ?: "Primera semana — sin comparación"

        Row(verticalAlignment = Alignment.Bottom) {
            Text(totalLabel, style = MaterialTheme.typography.displaySmall)
            Text(" $deltaLabel", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 6.dp))
        }

        Text("Horas por día", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
        BarChart(state.barSpecs, modifier = Modifier.fillMaxWidth().height(140.dp))

        Text("Por proyecto", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
        val palette = projectColorPalette()
        val othersColor = colorResource(R.color.others_bucket_color)
        Row(verticalAlignment = Alignment.CenterVertically) {
            DonutChart(state.donutSlices, palette, othersColor, modifier = Modifier.size(110.dp))
            Column(modifier = Modifier.padding(start = 16.dp)) {
                state.donutSlices.forEach { slice ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(colorForSlice(slice, palette, othersColor), CircleShape)
                        )
                        Text(
                            " ${slice.label}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun projectColorPalette(): List<Color> = listOf(
    colorResource(R.color.project_color_1), colorResource(R.color.project_color_2),
    colorResource(R.color.project_color_3), colorResource(R.color.project_color_4),
    colorResource(R.color.project_color_5), colorResource(R.color.project_color_6),
    colorResource(R.color.project_color_7), colorResource(R.color.project_color_8)
)

private fun colorForSlice(slice: DonutSlice, palette: List<Color>, othersColor: Color): Color =
    if (slice.isOthers) othersColor else palette[(slice.colorIndex ?: 0) % palette.size]

@Composable
private fun BarChart(specs: List<BarSpec>, modifier: Modifier = Modifier) {
    val barColor = MaterialTheme.colorScheme.primary
    Row(modifier = modifier, horizontalArrangement = Arrangement.SpaceEvenly) {
        specs.forEach { spec ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.height(140.dp)) {
                Canvas(
                    modifier = Modifier
                        .height(110.dp)
                        .width(20.dp)
                ) {
                    val barHeight = size.height * spec.heightFraction
                    drawRect(
                        color = barColor,
                        topLeft = Offset(0f, size.height - barHeight),
                        size = Size(size.width, barHeight)
                    )
                }
                Text(spec.dayLabel, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun DonutChart(slices: List<DonutSlice>, palette: List<Color>, othersColor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.3f
        slices.forEach { slice ->
            drawArc(
                color = colorForSlice(slice, palette, othersColor),
                startAngle = slice.startAngleDeg - 90f,
                sweepAngle = slice.sweepAngleDeg,
                useCenter = false,
                style = Stroke(width = strokeWidth)
            )
        }
    }
}

private fun formatHoursLabel(hours: Double): String {
    val totalMinutes = (hours * 60).toLong()
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return "${h}h ${m}m"
}
