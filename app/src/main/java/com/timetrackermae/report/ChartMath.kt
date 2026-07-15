package com.timetrackermae.report

data class BarSpec(val dayLabel: String, val heightFraction: Float, val hours: Double)

data class DonutSlice(
    val label: String,
    val colorIndex: Int?, // null for the "Otros" wedge (uses others_bucket_color)
    val startAngleDeg: Float,
    val sweepAngleDeg: Float,
    val isOthers: Boolean
)

/**
 * Pure geometry — no Android/Compose dependency, so it's testable with plain
 * JUnit and cheap to iterate on without a device build+install cycle (see
 * design doc Approach B feasibility mitigation).
 */
object ChartMath {

    private val DAY_LABELS = listOf("L", "M", "M", "J", "V", "S", "D")

    fun barSpecs(dailyHours: List<DayHours>): List<BarSpec> {
        val maxHours = dailyHours.maxOfOrNull { it.hours } ?: 0.0
        return dailyHours.mapIndexed { index, day ->
            val fraction = if (maxHours <= 0.0) 0f else (day.hours / maxHours).toFloat()
            BarSpec(DAY_LABELS.getOrElse(index) { "" }, fraction, day.hours)
        }
    }

    fun donutSlices(projectHours: List<ProjectHours>, othersHours: Double): List<DonutSlice> {
        val total = projectHours.sumOf { it.hours } + othersHours
        if (total <= 0.0) return emptyList()

        val slices = mutableListOf<DonutSlice>()
        var angleCursor = 0f
        for (p in projectHours) {
            val sweep = (p.hours / total * 360.0).toFloat()
            slices.add(DonutSlice(p.projectName, p.colorIndex, angleCursor, sweep, isOthers = false))
            angleCursor += sweep
        }
        if (othersHours > 0.0) {
            val sweep = (othersHours / total * 360.0).toFloat()
            slices.add(DonutSlice("Otros", null, angleCursor, sweep, isOthers = true))
            angleCursor += sweep
        }
        return slices
    }
}
