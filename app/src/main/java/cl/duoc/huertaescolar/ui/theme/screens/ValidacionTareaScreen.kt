package cl.duoc.huertaescolar.ui.theme.screens

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cl.duoc.huertaescolar.viewmodel.TareaViewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ValidacionTareaScreen(
    navController: NavController,
    viewModel: TareaViewModel,
    tareaId: Int
) {
    val tareas by viewModel.todasLasTareas.collectAsState()
    val tarea = tareas.find { it.id == tareaId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Validar Evidencia de Tarea") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        if (tarea == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. INFORMACIÓN GENERAL DE LA TAREA
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tipo: ${tarea.tipo}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = tarea.descripcion,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Asignado a: ${tarea.asignadoA}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // 2. SECCIÓN DE EVIDENCIA ENVIADA POR EL ESTUDIANTE
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Evidencia del Estudiante",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Comentario del alumno:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (!tarea.comentarioEstudiante.isNullOrBlank()) tarea.comentarioEstudiante!! else "Sin comentarios del alumno.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Fotografía Adjunta:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (!tarea.rutaFotografia.isNullOrBlank()) {
                        AsyncImage(
                            model = Uri.parse(tarea.rutaFotografia),
                            contentDescription = "Evidencia fotográfica",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = "No se adjuntó ninguna imagen en esta entrega.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // 3. CAMPO DE RETROALIMENTACIÓN DEL DOCENTE
            OutlinedTextField(
                value = viewModel.comentarioValidacion,
                onValueChange = { viewModel.comentarioValidacion = it },
                label = { Text("Comentario de retroalimentación (Opcional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 4. BOTONES DE ACCIÓN (Aprobar / Rechazar) BIEN ORDENADOS EN UNA FILA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.evaluarTarea(tareaId, aprobada = false) {
                            navController.popBackStack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Rechazar")
                }

                Button(
                    onClick = {
                        viewModel.evaluarTarea(tareaId, aprobada = true) {
                            navController.popBackStack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Aprobar")
                }
            }
        }
    }
}