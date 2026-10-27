package cl.duoc.huertaescolar.ui.theme.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import cl.duoc.huertaescolar.viewmodel.TareaViewModel
import coil.compose.AsyncImage
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvidenciaScreen(
    tareaId: Int,
    navController: NavController,
    viewModel: TareaViewModel
) {
    val context = LocalContext.current

    // Estado para guardar la URI de la imagen seleccionada o capturada
    var evidenciaUri by remember { mutableStateOf<Uri?>(null) }

    // Variable temporal para decirle a la cámara DÓNDE guardar la foto en alta calidad
    var uriTemporalParaCamara by remember { mutableStateOf<Uri?>(null) }

    // Estado para el comentario de la tarea
    var comentario by remember { mutableStateOf("") }

    // 1. Launcher de la Cámara Nativa
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { exito ->
        if (exito) {
            evidenciaUri = uriTemporalParaCamara
        } else {
            Toast.makeText(context, "Captura de foto cancelada", Toast.LENGTH_SHORT).show()
        }
    }

    // 2. Launcher de la Galería (Photo Picker)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uriSeleccionada ->
        uriSeleccionada?.let { uri ->
            // Copiamos la imagen al almacenamiento interno para permisos persistentes
            evidenciaUri = copiarImagenAAlmacenamientoInterno(context, uri)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Subir Evidencia") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Vista previa de la imagen cargada
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (evidenciaUri != null) {
                    AsyncImage(
                        model = evidenciaUri,
                        contentDescription = "Vista previa de la evidencia",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Ninguna imagen seleccionada", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Botones de captura / selección
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Botón Galería
                FilledTonalButton(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = "Galería")
                    Spacer(Modifier.width(8.dp))
                    Text("Galería")
                }

                // Botón Cámara
                Button(
                    onClick = {
                        uriTemporalParaCamara = crearUriDeArchivo(context)
                        uriTemporalParaCamara?.let { uri ->
                            cameraLauncher.launch(uri)
                        }
                    }
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = "Cámara")
                    Spacer(Modifier.width(8.dp))
                    Text("Cámara")
                }
            }

            // Input para el comentario del estudiante
            OutlinedTextField(
                value = comentario,
                onValueChange = { comentario = it },
                label = { Text("Comentario de la tarea (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 3
            )

            Spacer(Modifier.weight(1f))

            // Botón para guardar en la base de datos Room a través del Repositorio
            Button(
                enabled = evidenciaUri != null,
                onClick = {
                    evidenciaUri?.let { uri ->
                        viewModel.guardarEvidenciaPorId(
                            tareaId = tareaId,
                            comentario = comentario,
                            rutaFoto = uri.toString()
                        )
                        Toast.makeText(context, "Evidencia guardada con éxito", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar Evidencia y Finalizar Tarea")
            }
        }
    }
}

// Función auxiliar para generar un archivo temporal seguro para la cámara
fun crearUriDeArchivo(context: Context): Uri {
    val nombreArchivo = "evidencia_${System.currentTimeMillis()}.jpg"
    val archivo = File(context.filesDir, nombreArchivo)

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        archivo
    )
}

// Copia una imagen elegida en la galería al almacenamiento interno de la app
fun copiarImagenAAlmacenamientoInterno(context: Context, uri: Uri): Uri? {
    return try {
        val destino = File(context.filesDir, "evidencia_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { entrada ->
            destino.outputStream().use { salida -> entrada.copyTo(salida) }
        } ?: return null
        Uri.fromFile(destino)
    } catch (e: Exception) {
        null
    }
}