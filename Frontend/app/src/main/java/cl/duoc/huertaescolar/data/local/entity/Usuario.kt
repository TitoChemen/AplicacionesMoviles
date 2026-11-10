package cl.duoc.huertaescolar.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usuarios")
data class Usuario(
    @PrimaryKey val identificador: String,
    val nombreFicticio: String,
    val perfil: String,
    val tallerAsignado: String,
    val estadoCuenta: String,
    val contrasena: String,
    val backendId: Long? = null
)