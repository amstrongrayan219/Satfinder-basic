package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "satellites")
data class Satellite(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val longitude: Double, // positive for East, negative for West
    val isCustom: Boolean = false
) {
    val formattedLongitude: String
        get() = if (longitude >= 0) {
            String.format("%.1f°E", longitude)
        } else {
            String.format("%.1f°W", -longitude)
        }
}
