package cl.duoc.huertaescolar.ui.theme.screens

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
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
                title = { Text("Revisar Evidencia") },
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
            // 1. Resumen de la tarea
            Text("Detalles de la Tarea", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text("Tipo: ${tarea.tipo}\nDescripción: ${tarea.descripcion}", style = MaterialTheme.typography.bodyMedium)

            Divider()

            // 2. Lo que entregó el estudiante
            Text("Entrega del Estudiante", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(
                text = tarea.comentarioEstudiante ?: "El estudiante no dejó comentarios.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Light
            )

            // 3. La Evidencia Fotográfica
            Card(
                modifier = Modifier.fillMaxWidth().height(250.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (!tarea.rutaFotografia.isNullOrBlank()) {
                    AsyncImage(
                        model = Uri.parse(tarea.rutaFotografia),
                        contentDescription = "Foto de evidencia",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No se adjuntó fotografía", color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            Divider()

            // 4. Feedback del Docente
            Text("Evaluación del Docente", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(
                value = viewModel.comentarioValidacion,
                onValueChange = { viewModel.comentarioValidacion = it },
                label = { Text("Comentario o Feedback (Opcional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            // 5. Botones de Acción
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Botón Rechazar
                OutlinedButton(
                    onClick = {
                        viewModel.evaluarTarea(tareaId = tarea.id, aprobada = false) {
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Filled.Cancel, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rechazar")
                }

                // Botón Aprobar
                Button(
                    onClick = {
                        viewModel.evaluarTarea(tareaId = tarea.id, aprobada = true) {
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Aprobar")
                }
            }
        }
    }
}