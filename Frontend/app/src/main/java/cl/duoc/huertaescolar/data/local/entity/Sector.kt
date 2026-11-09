package cl.duoc.huertaescolar.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sectores")
data class Sector(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val codigo: String,
    val nombre: String,
    val descripcion: String,
    val ubicacion: String,
    val estado: String, // "DISPONIBLE", "EN USO", etc.
    val fechaCreacion: String,
    val responsableId: String // Referencia al Usuario responsable
)