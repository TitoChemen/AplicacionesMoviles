package cl.duoc.huertaescolar.ui.theme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cl.duoc.huertaescolar.data.EstadoTarea
import cl.duoc.huertaescolar.data.Perfil
import cl.duoc.huertaescolar.data.local.entity.Tarea
import cl.duoc.huertaescolar.data.local.entity.Usuario
import cl.duoc.huertaescolar.viewmodel.TareaViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ListaTareasScreen(
    navController: NavController,
    viewModel: TareaViewModel,
    usuarioActivo: Usuario?,
    accessToken: String?
) {
    val tareas by viewModel.tareasFiltradas.collectAsState()
    val filtroActual by viewModel.filtroSeleccionado.collectAsState()
    val esAdmin = Perfil.esPersonal(usuarioActivo?.perfil)
    val cursoPorTarea by viewModel.cursoPorTarea.collectAsState()
    val intentosPorTarea by viewModel.intentosPorTarea.collectAsState()


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (esAdmin) "Gestión de Tareas - Huerto" else "Mis Tareas del Huerto") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2E7D32), // Verde Huerto Principal
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (esAdmin) {
                FloatingActionButton(
                    onClick = {
                        viewModel.limpiarFormularioTarea()
                        navController.navigate("crear_tarea")
                    },
                    containerColor = Color(0xFF388E3C), // Verde Hoja
                    contentColor = Color.White
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Nueva Tarea")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF1F8E9)) // Fondo verde ultra claro (tipo campo / huerto)
        ) {
            // Filtros responsivos: se acomodan en nuevas filas sin cortar etiquetas.
            FlowRow(
                modifier = Modifier.fillMaxWidth()
                    .padding(vertical = 6.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                EstadoTarea.filtros.forEach { opcion ->
                    FilterChip(
                        selected = filtroActual == opcion,
                        onClick = { viewModel.actualizarFiltro(opcion) },
                        label = { Text(EstadoTarea.etiqueta(opcion)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF558B2F),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            if (tareas.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No hay tareas bajo el estado '${EstadoTarea.etiqueta(filtroActual)}'.",
                        color = Color(0xFF558B2F)
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(tareas, key = { it.id }) { tarea ->
                        val numeroReintento = intentosPorTarea[tarea.id] ?: 0

                        TareaCard(
                            tarea = tarea,
                            numeroReintento = numeroReintento,
                            esAdmin = esAdmin,
                            curso = cursoPorTarea[tarea.id],
                            onEdit = {
                                viewModel.prepararEdicion(tarea)
                                navController.navigate("crear_tarea")
                            },
                            onDelete = { viewModel.eliminarTarea(tarea, accessToken) }
                        ) {
                            if (esAdmin) {
                                navController.navigate("validacion_tarea/${tarea.id}")
                            } else {
                                navController.navigate("detalle_tarea/${tarea.id}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TareaCard(
    tarea: Tarea,
    numeroReintento: Int,
    esAdmin: Boolean = false,
    curso: String? = null,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onClick: () -> Unit
) {
    val colorEstado = obtenerColorPorEstado(tarea.estado)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Franja lateral de color según el estado natural
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(colorEstado)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val iconoAsignado = when (tarea.estado) {
                    EstadoTarea.VALIDADA -> Icons.Filled.CheckCircle
                    EstadoTarea.RECHAZADA -> Icons.Filled.Cancel
                    else -> Icons.Filled.Assignment
                }

                val tintIcono = when (tarea.estado) {
                    EstadoTarea.VALIDADA -> Color(0xFF2E7D32) // Verde Huerto
                    EstadoTarea.RECHAZADA -> Color(0xFFC62828) // Rojo Tierra/Alerta
                    else -> Color(0xFFEF6C00) // Naranja Semilla
                }

                Icon(
                    imageVector = iconoAsignado,
                    contentDescription = null,
                    tint = tintIcono,
                    modifier = Modifier.size(36.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // 🌿 Etiqueta de Reintento con color tierra / verde musgo natural
                    if (numeroReintento > 1) {
                        Surface(
                            color = Color(0xFFDCEDC8), // Verde hoja suave
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = "🌱 Intento #$numeroReintento",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF33691E), // Verde musgo oscuro
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }


                    Text(
                        text = tarea.tipo,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF1B5E20),
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = tarea.descripcion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF558B2F)
                    )

                    if (esAdmin && !curso.isNullOrBlank()) {
                        Text(
                            text = "Curso: $curso",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF1B5E20),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = "Vence: ${tarea.fechaProgramada}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF558B2F)
                    )


                    // Etiqueta semántica de estado
                    Surface(
                        color = colorEstado.copy(alpha = 0.15f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = EstadoTarea.etiqueta(tarea.estado),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = colorEstado,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (esAdmin && (numeroReintento > 0 || !tarea.rutaFotografia.isNullOrBlank())) {
                        Text("Historial protegido · Toca para revisar",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary)
                    }
                    if (esAdmin && numeroReintento == 0 && tarea.rutaFotografia.isNullOrBlank()) {
                        Row {
                            IconButton(onClick = onEdit) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Editar tarea",
                                    tint = Color(0xFF2E7D32)
                                )
                            }

                            IconButton(onClick = onDelete) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Eliminar tarea",
                                    tint = Color(0xFFC62828)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Colores semánticos inspirados en la naturaleza y el huerto
@Composable
fun obtenerColorPorEstado(estado: String): Color {
    return when (estado) {
        EstadoTarea.PENDIENTE -> Color(0xFFEF6C00)
        EstadoTarea.PENDIENTE_VALIDACION -> Color(0xFF6A1B9A)
        EstadoTarea.VALIDADA -> Color(0xFF2E7D32)
        EstadoTarea.RECHAZADA -> Color(0xFFC62828)
        else -> Color(0xFF78909C)
    }
}