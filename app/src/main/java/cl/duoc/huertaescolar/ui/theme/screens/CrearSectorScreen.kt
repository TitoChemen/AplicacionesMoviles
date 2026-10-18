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
import cl.duoc.huertaescolar.viewmodel.SectorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearSectorScreen(
    navController: NavController,
    viewModel: SectorViewModel,
    idAdministradorActivo: Int // Pasado desde el AuthViewModel en el NavHost
) {
    var mensajeExito by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar Nuevo Sector") },
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Datos del Sector",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Campo: Nombre del Sector (Obligatorio)
                    OutlinedTextField(
                        value = viewModel.nombreSector,
                        onValueChange = { viewModel.onNombreSectorChange(it) },
                        label = { Text("Nombre del Sector *") },
                        isError = viewModel.errorNombreSector != null,
                        supportingText = {
                            viewModel.errorNombreSector?.let { error ->
                                Text(text = error, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Campo: Ubicación (Obligatorio según tu nueva lógica)
                    OutlinedTextField(
                        value = viewModel.ubicacionSector,
                        onValueChange = { viewModel.onUbicacionSectorChange(it) },
                        label = { Text("Ubicación en el huerto *") },
                        isError = viewModel.errorUbicacionSector != null,
                        supportingText = {
                            viewModel.errorUbicacionSector?.let { error ->
                                Text(text = error, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Campo: Descripción (Opcional)
                    OutlinedTextField(
                        value = viewModel.descripcionSector,
                        onValueChange = { viewModel.descripcionSector = it },
                        label = { Text("Descripción (Opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    // Mensaje de éxito
                    if (mensajeExito.isNotBlank()) {
                        Text(
                            text = mensajeExito,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    // Botón Guardar
                    Button(
                        onClick = {
                            viewModel.registrarSector(responsableId = idAdministradorActivo) {
                                mensajeExito = "¡Sector registrado con éxito!"
                                // Opcional: navController.popBackStack() para volver automáticamente a la lista
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Guardar Sector")
                    }
                }
            }
        }
    }
}