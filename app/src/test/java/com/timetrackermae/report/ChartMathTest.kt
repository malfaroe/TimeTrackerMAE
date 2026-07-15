package com.timetrackermae.report

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class ChartMathTest {

    private fun day(hours: Double, dow: DayOfWeek = DayOfWeek.MONDAY) =
        DayHours(LocalDate.of(2026, 7, 6), dow, hours)

    @Test
    fun `bar with zero hours has zero height fraction and does not crash`() {
        val days = listOf(day(4.0), day(0.0), day(2.0))
        val specs = ChartMath.barSpecs(days)
        assertEquals(3, specs.size)
        assertEquals(0f, specs[1].heightFraction, 0.0001f)
        assertEquals(1f, specs[0].heightFraction, 0.0001f) // max day = 100%
    }

    @Test
    fun `all-zero week produces all zero bars without dividing by zero`() {
        val days = listOf(day(0.0), day(0.0))
        val specs = ChartMath.barSpecs(days)
        assertTrue(specs.all { it.heightFraction == 0f })
    }

    @Test
    fun `donut with a single project sweeps the full 360 degrees`() {
        val slices = ChartMath.donutSlices(
            listOf(ProjectHours(1L, "Solo", colorIndex = 0, hours = 10.0)),
            othersHours = 0.0
        )
        assertEquals(1, slices.size)
        assertEquals(0f, slices[0].startAngleDeg, 0.0001f)
        assertEquals(360f, slices[0].sweepAngleDeg, 0.0001f)
    }

    @Test
    fun `donut slices sum to 360 degrees including others wedge`() {
        val slices = ChartMath.donutSlices(
            listOf(
                ProjectHours(1L, "A", colorIndex = 0, hours = 15.0),
                ProjectHours(2L, "B", colorIndex = 1, hours = 5.0)
            ),
            othersHours = 2.0
        )
        val totalSweep = slices.sumOf { it.sweepAngleDeg.toDouble() }
        assertEquals(360.0, totalSweep, 0.01)
        assertEquals(true, slices.last().isOthers)
    }

    @Test
    fun `no data produces empty slice list`() {
        val slices = ChartMath.donutSlices(emptyList(), othersHours = 0.0)
        assertTrue(slices.isEmpty())
    }
}
