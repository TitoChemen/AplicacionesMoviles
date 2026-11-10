
package cl.duoc.huertaescolar.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cl.duoc.huertaescolar.data.EstadoTarea
import cl.duoc.huertaescolar.viewmodel.TareaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleTareaEstudianteScreen(
    navController: NavController,
    viewModel: TareaViewModel,
    tareaId: Int
) {
    // No depende del filtro de la lista principal
    val tareas by viewModel.tareasSinFiltrar.collectAsState()
    val tarea = tareas.find { it.id == tareaId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Tarea") },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() }
                    ) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor =
                        MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor =
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->

        if (tarea == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No se encontró la tarea.")
            }
        } else {

            val estaRechazada =
                tarea.estado == EstadoTarea.RECHAZADA

            val puedeEnviarEvidencia =
                tarea.estado == EstadoTarea.PENDIENTE ||
                        estaRechazada

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement =
                    Arrangement.spacedBy(16.dp)
            ) {

                // INFORMACIÓN DE LA TAREA
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Tipo: ${tarea.tipo}",
                            style =
                                MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = "Descripción: ${tarea.descripcion}",
                            style =
                                MaterialTheme.typography.bodyLarge
                        )

                        Text(
                            text = "Fecha programada: ${tarea.fechaProgramada}",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Estado: ${EstadoTarea.etiqueta(tarea.estado)}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // RETROALIMENTACIÓN DEL ADMINISTRADOR
                if (!tarea.comentarioDocente.isNullOrBlank()) {

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (estaRechazada) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (estaRechazada) {
                                    "Motivo del rechazo"
                                } else {
                                    "Retroalimentación del administrador"
                                },
                                style =
                                    MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = tarea.comentarioDocente.orEmpty(),
                                style =
                                    MaterialTheme.typography.bodyLarge
                            )

                            if (estaRechazada) {
                                Text(
                                    text = "Corrige tu evidencia según las observaciones y vuelve a enviarla.",
                                    style =
                                        MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                } else if (estaRechazada) {

                    Text(
                        text = "La tarea fue rechazada, pero no se registró un motivo.",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // INFORMACIÓN DEL ESTADO
                when (tarea.estado) {

                    EstadoTarea.PENDIENTE_VALIDACION,
                    "REALIZADA" -> {
                        Text(
                            text = "Tu evidencia fue enviada y está esperando revisión.",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    EstadoTarea.VALIDADA -> {
                        Text(
                            text = "¡Tu evidencia fue aprobada!",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // ENVIAR O REENVIAR EVIDENCIA
                if (puedeEnviarEvidencia) {

                    Button(
                        onClick = {
                            navController.navigate(
                                "evidencia/${tarea.id}"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(
                            Icons.Filled.CameraAlt,
                            contentDescription = null
                        )

                        Spacer(Modifier.width(8.dp))

                        Text(
                            if (estaRechazada) {
                                "Corregir y reenviar evidencia"
                            } else {
                                "Subir evidencia"
                            }
                        )
                    }
                }
            }
        }
    }
}
