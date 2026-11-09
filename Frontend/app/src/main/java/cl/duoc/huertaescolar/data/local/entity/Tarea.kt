package cl.duoc.huertaescolar.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tareas")
data class Tarea(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tipo: String, // "RIEGO", "SIEMBRA", etc.
    val descripcion: String,
    val sectorId: Int,
    val cultivoId: Int?, // Puede ser null si la tarea es de todo el sector
    val fechaProgramada: String,
    val asignadoA: String, // Identificador del estudiante
    val estado: String, // "PENDIENTE", "REALIZADA", "PENDIENTE_VALIDACION"
    val comentarioEstudiante: String? = null,
    val comentarioDocente: String? = null,
    val rutaFotografia: String? = null // Ruta local de la foto
)