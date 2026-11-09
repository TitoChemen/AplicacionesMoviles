package cl.duoc.huertaescolar.data

/** Valores de perfil EXACTAMENTE como se guardan en la base de datos. */
object Perfil {
    const val ADMINISTRADOR = "ADMINISTRADOR"
    const val ESTUDIANTE = "ESTUDIANTE"
}

/** Estados de una tarea EXACTAMENTE como se guardan en la base de datos. */
object EstadoTarea {
    const val TODAS = "TODAS" // solo para el filtro de la UI, no se guarda
    const val PENDIENTE = "PENDIENTE"
    const val PENDIENTE_VALIDACION = "PENDIENTE_VALIDACION"
    const val VALIDADA = "VALIDADA"
    const val RECHAZADA = "RECHAZADA"

    val filtros = listOf(TODAS, PENDIENTE, PENDIENTE_VALIDACION, VALIDADA, RECHAZADA)

    fun etiqueta(estado: String): String = when (estado) {
        TODAS -> "Todas"
        PENDIENTE -> "Pendiente"
        PENDIENTE_VALIDACION -> "Pendiente de validación"
        VALIDADA -> "Validada"
        RECHAZADA -> "Rechazada"
        else -> estado
    }
}
