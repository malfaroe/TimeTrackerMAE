package com.timetrackermae.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * colorIndex cycles through the fixed 8-color palette (colors.xml
 * project_color_1..8) — see Success Criteria in the design doc.
 */
@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorIndex: Int
)
