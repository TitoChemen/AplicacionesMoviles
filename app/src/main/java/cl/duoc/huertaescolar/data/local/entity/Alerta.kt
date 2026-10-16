package cl.duoc.huertaescolar.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alertas")
data class Alerta(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tipoProblema: String, // "PLAGA", "FALTA_RIEGO", "INFRAESTRUCTURA", "OTRO"
    val descripcion: String,
    val sectorId: Int,
    val fechaReporte: String,
    val reportadoPor: String, // Ej: "estudiante1"
    val estado: String = "PENDIENTE", // "PENDIENTE", "RESUELTA"
    val rutaFotografia: String? = null // Ruta local de la foto de evidencia
)