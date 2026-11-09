package cl.duoc.huertaescolar.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cl.duoc.huertaescolar.viewmodel.TareaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearTareaScreen(
    navController: NavController,
    viewModel: TareaViewModel
) {
    val estudiantes by viewModel.estudiantesDisponibles.collectAsState()
    var expandedDropdown by remember { mutableStateOf(false) }
    var nombreEstudianteSeleccionado by remember { mutableStateOf("") }
    var mensajeExito by remember { mutableStateOf("") }

    // Cargamos los estudiantes apenas se abre la pantalla
    LaunchedEffect(Unit) {
        viewModel.cargarEstudiantes()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Asignar Nueva Tarea") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Detalles de la Tarea", style = MaterialTheme.typography.titleMedium)

                    // 1. Dropdown para seleccionar Responsable
                    ExposedDropdownMenuBox(
                        expanded = expandedDropdown,
                        onExpandedChange = { expandedDropdown = !expandedDropdown }
                    ) {
                        OutlinedTextField(
                            value = nombreEstudianteSeleccionado.ifBlank { "Seleccione un colaborador..." },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Estudiante Responsable *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            isError = viewModel.errorResponsable != null,
                            supportingText = { viewModel.errorResponsable?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
                        )
                        ExposedDropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            estudiantes.forEach { estudiante ->
                                DropdownMenuItem(
                                    text = { Text(estudiante.nombreFicticio) },
                                    onClick = {
                                        viewModel.responsableIdSeleccionado = estudiante.identificador
                                        nombreEstudianteSeleccionado = estudiante.nombreFicticio
                                        expandedDropdown = false
                                        viewModel.errorResponsable = null
                                    }
                                )
                            }
                        }
                    }

                    // 2. Tipo de Tarea
                    OutlinedTextField(
                        value = viewModel.tipoTarea,
                        onValueChange = { viewModel.tipoTarea = it },
                        label = { Text("Tipo (ej. Riego, Siembra) *") },
                        isError = viewModel.errorTipoTarea != null,
                        supportingText = { viewModel.errorTipoTarea?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // 3. Descripción
                    OutlinedTextField(
                        value = viewModel.descripcionTarea,
                        onValueChange = { viewModel.descripcionTarea = it },
                        label = { Text("Descripción *") },
                        isError = viewModel.errorDescripcionTarea != null,
                        supportingText = { viewModel.errorDescripcionTarea?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    // 4. Fecha Programada
                    OutlinedTextField(
                        value = viewModel.fechaProgramada,
                        onValueChange = { viewModel.fechaProgramada = it },
                        label = { Text("Fecha Límite (YYYY-MM-DD) *") },
                        isError = viewModel.errorFechaTarea != null,
                        supportingText = { viewModel.errorFechaTarea?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (mensajeExito.isNotBlank()) {
                        Text(text = mensajeExito, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                    }

                    Button(
                        onClick = {
                            viewModel.crearTarea {
                                mensajeExito = "¡Tarea asignada correctamente!"
                                nombreEstudianteSeleccionado = "" // Reiniciar la vista
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Asignar Tarea")
                    }
                }
            }
        }
    }
}