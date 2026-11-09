package cl.duoc.huertaescolar.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cultivos")
data class Cultivo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sectorId: Int, // Referencia al Sector
    val nombreComun: String,
    val descripcion: String,
    val fechaSiembra: String,
    val fechaCosechaEstimada: String,
    val frecuenciaRiego: String,
    val estadoDesarrollo: String // "SEMBRADO", "EN CRECIMIENTO", etc.
)