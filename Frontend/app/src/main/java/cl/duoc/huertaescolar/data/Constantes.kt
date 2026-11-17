package cl.duoc.huertaescolar.data

/** Valores de perfil EXACTAMENTE como se guardan en la base de datos. */
object Perfil {
    const val ADMINISTRADOR = "ADMINISTRADOR" // acceso global
    const val DOCENTE = "DOCENTE" // un solo curso
    const val ESTUDIANTE = "ESTUDIANTE"

    fun esPersonal(rol: String?): Boolean = rol == ADMINISTRADOR || rol == DOCENTE
}

/** Estados de una tarea EXACTAMENTE como se guardan en la base de datos. */
object EstadoTarea {
    const val TODAS = "TODAS" // solo para el filtro de la UI, no se guarda
    const val PENDIENTE = "PENDIENTE"
    const val PENDIENTE_VALIDACION = "PENDIENTE_VALIDACION"
    const val VALIDADA = "APROBADA"
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
