package cl.duoc.huertaescolar.ui.theme.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaTareasScreen(
    navController: NavController,
    viewModel: TareaViewModel,
    usuarioActivo: Usuario? // Lo pasamos desde el NavHost para saber su rol
) {
    val tareas by viewModel.tareasFiltradas.collectAsState()
    val filtroActual by viewModel.filtroSeleccionado.collectAsState()
    val esAdmin = usuarioActivo?.perfil == Perfil.ADMINISTRADOR

    LaunchedEffect(usuarioActivo) {
        viewModel.cargarTareasSegunRol(usuarioActivo)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (esAdmin) "Gestión de Tareas" else "Mis Tareas") },
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
        },
        floatingActionButton = {
            // Solo el administrador puede crear tareas
            if (esAdmin) {
                FloatingActionButton(
                    onClick = { navController.navigate("crear_tarea") },
                    containerColor = MaterialTheme.colorScheme.primary
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
        ) {
            // Carrusel horizontal de filtros
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(EstadoTarea.filtros) { opcion ->
                    FilterChip(
                        selected = filtroActual == opcion,
                        onClick = { viewModel.actualizarFiltro(opcion) },
                        label = { Text(EstadoTarea.etiqueta(opcion)) }
                    )
                }
            }

            if (tareas.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No hay tareas bajo el estado '${EstadoTarea.etiqueta(filtroActual)}'.",
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(tareas) { tarea ->
                        TareaCard(tarea = tarea) {
                            // Admin revisa las entregadas; estudiante entrega las pendientes/rechazadas
                            if (esAdmin) {
                                if (tarea.estado == EstadoTarea.PENDIENTE_VALIDACION) {
                                    navController.navigate("validacion_tarea/${tarea.id}")
                                }
                            } else {
                                if (tarea.estado == EstadoTarea.PENDIENTE ||
                                    tarea.estado == EstadoTarea.RECHAZADA
                                ) {
                                    navController.navigate("detalle_tarea/${tarea.id}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TareaCard(tarea: Tarea, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Assignment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tarea.descripcion,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Vence: ${tarea.fechaProgramada}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Etiqueta semántica de estado
            Surface(
                color = obtenerColorPorEstado(tarea.estado).copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = EstadoTarea.etiqueta(tarea.estado),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = obtenerColorPorEstado(tarea.estado),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Colores semánticos para estados
@Composable
fun obtenerColorPorEstado(estado: String): Color {
    return when (estado) {
        EstadoTarea.PENDIENTE -> Color(0xFFFFA000) // Naranja (Advertencia)
        EstadoTarea.PENDIENTE_VALIDACION -> Color(0xFFAB47BC) // Morado (Revisión)
        EstadoTarea.VALIDADA -> Color(0xFF4CAF50) // Verde (Éxito)
        EstadoTarea.RECHAZADA -> MaterialTheme.colorScheme.error // Rojo (Error)
        else -> MaterialTheme.colorScheme.outline
    }
}
