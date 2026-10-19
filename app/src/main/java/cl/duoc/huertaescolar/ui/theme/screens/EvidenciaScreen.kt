package cl.duoc.huertaescolar.ui.theme.screens

import android.content.Context
import android.net.Uri
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
import coil.compose.AsyncImage
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvidenciaScreen(
    navController: NavController,
    // viewModel: TareaViewModel // Aquí pasarás tu ViewModel para guardar la URI final
) {
    val context = LocalContext.current

    // Estado para guardar la URI de la imagen seleccionada o capturada
    var evidenciaUri by remember { mutableStateOf<Uri?>(null) }

    // Variable temporal para decirle a la cámara DÓNDE guardar la foto en alta calidad
    var uriTemporalParaCamara by remember { mutableStateOf<Uri?>(null) }

    // 1. Launcher de la Cámara Nativa (Checklist)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { exito ->
        if (exito) {
            // Si la foto se tomó bien, pasamos la URI temporal al estado principal
            evidenciaUri = uriTemporalParaCamara
            // Aquí llamarías a: viewModel.guardarUriEvidencia(evidenciaUri.toString())
        }
    }

    // 2. Launcher de la Galería (Checklist: Photo Picker)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uriSeleccionada ->
        uriSeleccionada?.let { uri ->
            // TRUCO VITAL (Checklist): Tomar permisos persistentes para que la foto
            // siga cargando si cerramos y volvemos a abrir la app mañana.
            // El Photo Picker NO permite permisos persistentes: copiamos la imagen a la app
            evidenciaUri = copiarImagenAAlmacenamientoInterno(context, uri)
            // Aquí llamarías a: viewModel.guardarUriEvidencia(uri.toString())
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
            // Checklist: Mostrar una vista previa de las imágenes
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
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
                        // Creamos un archivo temporal y su URI segura usando FileProvider
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
        }
    }
}

// Función auxiliar para generar un archivo temporal seguro para la cámara
fun crearUriDeArchivo(context: Context): Uri {
    // Generamos un nombre único basado en la fecha
    val nombreArchivo = "evidencia_${System.currentTimeMillis()}.jpg"
    val archivo = File(context.filesDir, nombreArchivo)

    // IMPORTANTE: El "authority" debe coincidir con el que pusiste en tu AndroidManifest.xml
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider", // Revisa que este texto coincida con tu Manifest
        archivo
    )
}

// Copia una imagen elegida en la galería al almacenamiento interno de la app,
// para que siga disponible aunque se cierre la app (el Photo Picker solo da acceso temporal).
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
