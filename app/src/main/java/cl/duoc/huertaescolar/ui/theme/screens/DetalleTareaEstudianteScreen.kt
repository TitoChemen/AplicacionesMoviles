package cl.duoc.huertaescolar.ui.theme.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cl.duoc.huertaescolar.viewmodel.TareaViewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleTareaEstudianteScreen(
    navController: NavController,
    viewModel: TareaViewModel,
    tareaId: Int
) {
    val context = LocalContext.current
    val tareas by viewModel.tareasFiltradas.collectAsState()
    val tarea = tareas.find { it.id == tareaId }

    // Uri temporal para cuando se usa la cámara
    var uriTemporalParaCamara by remember { mutableStateOf<Uri?>(null) }

    // Lanzador para la Cámara
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { exito ->
        if (exito) {
            viewModel.rutaFotografiaEntrega = uriTemporalParaCamara.toString()
        }
    }

    // Lanzador para la Galería
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uriSeleccionada ->
        uriSeleccionada?.let { uri ->
            val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flag)
            viewModel.rutaFotografiaEntrega = uri.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Entregar Tarea") },
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
            // 1. Tarjeta con detalles de la tarea
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Tipo: ${tarea.tipo}", style = MaterialTheme.typography.titleMedium)
                    Text(text = "Descripción: ${tarea.descripcion}", style = MaterialTheme.typography.bodyLarge)
                    Text(text = "Fecha Límite: ${tarea.fechaProgramada}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }

            // 2. Campo para comentarios del estudiante
            OutlinedTextField(
                value = viewModel.comentarioEntrega,
                onValueChange = { viewModel.comentarioEntrega = it },
                label = { Text("Comentarios sobre la ejecución") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4
            )

            // 3. Área de Evidencia Fotográfica
            Text(text = "Evidencia Fotográfica", style = MaterialTheme.typography.titleSmall)

            Card(
                modifier = Modifier.fillMaxWidth().height(250.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                if (viewModel.rutaFotografiaEntrega != null) {
                    AsyncImage(
                        model = Uri.parse(viewModel.rutaFotografiaEntrega),
                        contentDescription = "Evidencia seleccionada",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Ninguna foto adjuntada", color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FilledTonalButton(
                    onClick = {
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = "Galería")
                    Spacer(Modifier.width(8.dp))
                    Text("Galería")
                }

                Button(
                    onClick = {
                        uriTemporalParaCamara = crearUriDeArchivo(context)
                        uriTemporalParaCamara?.let { uri -> cameraLauncher.launch(uri) }
                    }
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = "Cámara")
                    Spacer(Modifier.width(8.dp))
                    Text("Cámara")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Botón de Entrega Final
            Button(
                onClick = {
                    viewModel.entregarTarea(tareaId = tarea.id) {
                        navController.popBackStack() // Volvemos a la lista de tareas tras el éxito
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                // Solo habilitamos el botón si subió foto y escribió algo
                enabled = viewModel.comentarioEntrega.isNotBlank() && viewModel.rutaFotografiaEntrega != null
            ) {
                Text("Entregar Tarea")
            }
        }
    }
}