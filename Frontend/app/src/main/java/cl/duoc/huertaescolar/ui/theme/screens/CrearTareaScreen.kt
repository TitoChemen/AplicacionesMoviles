package cl.duoc.huertaescolar.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
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
    viewModel: TareaViewModel,
    accessToken: String?
) {
    val estudiantes by viewModel.estudiantesDisponibles.collectAsState()
    val sectores by viewModel.sectoresDisponibles.collectAsState()
    val cursosEstudiantes by viewModel.cursoIdPorEstudiante.collectAsState()
    val cursoDelAlumno = viewModel.responsableIdSeleccionado?.toLongOrNull()?.let {
        cursosEstudiantes[it]
    }
    val sectoresCompatibles = if (cursoDelAlumno == null) sectores else sectores.filter {
        it.cursoId == cursoDelAlumno
    }
    var expandedSector by remember { mutableStateOf(false) }
    val nombreSectorSeleccionado = sectoresCompatibles.firstOrNull {
        it.id == viewModel.sectorIdSeleccionado
    }?.nombre.orEmpty()
    var expandedDropdown by remember { mutableStateOf(false) }
    val nombreEstudianteSeleccionado = estudiantes.firstOrNull {
        it.backendId?.toString() == viewModel.responsableIdSeleccionado
    }?.nombreFicticio.orEmpty()
    var mensajeExito by rememberSaveable { mutableStateOf("") }

    // Cargamos los estudiantes apenas se abre la pantalla
    LaunchedEffect(accessToken) {
        viewModel.cargarEstudiantes(accessToken)
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
                .imePadding()
                .verticalScroll(rememberScrollState())
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
                                        viewModel.responsableIdSeleccionado = estudiante.backendId?.toString()
                                        val cursoElegido = cursosEstudiantes[estudiante.backendId]
                                        if (cursoElegido != null && sectores.none {
                                                it.id == viewModel.sectorIdSeleccionado && it.cursoId == cursoElegido
                                            }) {
                                            viewModel.sectorIdSeleccionado = null
                                        }
                                         expandedDropdown = false
                                        viewModel.errorResponsable = null
                                    }
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = expandedSector,
                        onExpandedChange = { expandedSector = !expandedSector }
                    ) {
                        OutlinedTextField(
                            value = nombreSectorSeleccionado.ifBlank { "Selecciona sector del curso..." },
                            onValueChange = {}, readOnly = true,
                            label = { Text("Sector del curso *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSector) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedSector,
                            onDismissRequest = { expandedSector = false }
                        ) {
                            sectoresCompatibles.forEach { sector ->
                                DropdownMenuItem(
                                    text = { Text(sector.nombre) },
                                    onClick = {
                                        viewModel.sectorIdSeleccionado = sector.id
                                         expandedSector = false
                                    }
                                )
                            }
                        }
                    }

                    viewModel.errorCreacion?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
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
                            viewModel.crearTarea(accessToken) {
                                mensajeExito = "¡Tarea asignada correctamente!"
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