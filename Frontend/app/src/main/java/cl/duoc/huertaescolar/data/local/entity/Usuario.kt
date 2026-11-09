package cl.duoc.huertaescolar.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usuarios")
data class Usuario(
    @PrimaryKey val identificador: String, // ej: "admin1", "estudiante1"
    val nombreFicticio: String,
    val perfil: String, // "ADMINISTRADOR", "ESTUDIANTE"
    val tallerAsignado: String,
    val estadoCuenta: String, // "ACTIVA", "INACTIVA"
    val contrasena: String    // 🔑 NUEVO: Contraseña para proteger el acceso
)