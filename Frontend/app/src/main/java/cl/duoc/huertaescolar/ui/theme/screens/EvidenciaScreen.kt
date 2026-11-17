
package cl.duoc.huertaescolar.ui.theme.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.runtime.saveable.rememberSaveable
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
    viewModel: TareaViewModel,
    accessToken: String?
) {
    val context = LocalContext.current

    // Imagen seleccionada o capturada
    var evidenciaUriTexto by rememberSaveable(tareaId) { mutableStateOf<String?>(null) }
    val evidenciaUri = evidenciaUriTexto?.let(Uri::parse)

    // URI temporal de camara restaurable tras giro
    var uriTemporalCamaraTexto by rememberSaveable(tareaId) { mutableStateOf<String?>(null) }
    val uriTemporalParaCamara = uriTemporalCamaraTexto?.let(Uri::parse)

    // Comentario del estudiante
    var comentario by rememberSaveable(tareaId) {
        mutableStateOf("")
    }

    // Evitar envíos duplicados
    var guardando by remember {
        mutableStateOf(false)
    }

    // ==========================================
    // CÁMARA
    // ==========================================

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { exito ->

        if (exito) {
            evidenciaUriTexto = uriTemporalCamaraTexto
        } else {
            Toast.makeText(
                context,
                "Captura de foto cancelada",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ==========================================
    // GALERÍA
    // ==========================================

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uriSeleccionada ->

        uriSeleccionada?.let { uri ->

            evidenciaUriTexto = copiarImagenAAlmacenamientoInterno(
                context = context,
                uri = uri
            )?.toString()

            if (evidenciaUriTexto == null) {
                Toast.makeText(
                    context,
                    "No se pudo cargar la imagen",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ==========================================
    // INTERFAZ PRINCIPAL
    // ==========================================

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Enviar evidencia")
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.popBackStack()
                        }
                    ) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor =
                        MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor =
                        MaterialTheme.colorScheme.onPrimaryContainer
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ==================================
            // VISTA PREVIA DE LA FOTOGRAFÍA
            // ==================================

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                colors = CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {

                if (evidenciaUri != null) {

                    AsyncImage(
                        model = evidenciaUri,
                        contentDescription = "Vista previa de evidencia",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                } else {

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Ninguna imagen seleccionada",
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ==================================
            // BOTONES CÁMARA Y GALERÍA
            // ==================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {

                FilledTonalButton(
                    enabled = !guardando,
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts
                                    .PickVisualMedia.ImageOnly
                            )
                        )
                    }
                ) {
                    Icon(
                        Icons.Filled.PhotoLibrary,
                        contentDescription = null
                    )

                    Spacer(Modifier.width(8.dp))

                    Text("Galería")
                }

                Button(
                    enabled = !guardando,
                    onClick = {

                        try {
                            val nuevaUri = crearUriDeArchivo(context)
                            uriTemporalCamaraTexto = nuevaUri.toString()
                            cameraLauncher.launch(nuevaUri)

                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "No se pudo abrir la cámara",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                ) {
                    Icon(
                        Icons.Filled.CameraAlt,
                        contentDescription = null
                    )

                    Spacer(Modifier.width(8.dp))

                    Text("Cámara")
                }
            }

            // ==================================
            // COMENTARIO DEL ESTUDIANTE
            // ==================================

            OutlinedTextField(
                value = comentario,
                onValueChange = {
                    comentario = it
                },
                label = {
                    Text("Justificación de tu evidencia (opcional)")
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !guardando,
                minLines = 3,
                maxLines = 5,
                supportingText = { Text("Explica qué hiciste y qué muestra la fotografía.") }
            )

            Text(
                text = "Tu foto y justificación serán guardadas para que las revise tu docente.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ==================================
            // BOTÓN ENVIAR EVIDENCIA
            // ==================================

            Button(
                enabled = evidenciaUri != null &&
                        !guardando &&
                        !accessToken.isNullOrBlank(),
                onClick = {

                    evidenciaUri?.let { uri ->

                        guardando = true

                        viewModel.guardarEvidenciaPorId(
                            tareaId = tareaId,
                            comentario = comentario,
                            rutaFoto = uri.toString(),
                            accessToken = accessToken,

                            onSuccess = {
                                guardando = false

                                Toast.makeText(
                                    context,
                                    "Evidencia enviada para validación",
                                    Toast.LENGTH_SHORT
                                ).show()

                                navController.popBackStack()
                            },

                            onError = {
                                guardando = false

                                Toast.makeText(
                                    context,
                                    "No se pudo enviar la evidencia",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {

                if (guardando) {

                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )

                    Spacer(Modifier.width(10.dp))

                    Text("Enviando...")

                } else {

                    Text("Enviar evidencia para validación")
                }
            }

            if (accessToken.isNullOrBlank()) {
                Text(
                    text = "Debes iniciar sesión para enviar evidencia.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// ==========================================
// CREAR URI PARA LA CÁMARA
// ==========================================

fun crearUriDeArchivo(context: Context): Uri {

    val nombreArchivo =
        "evidencia_${System.currentTimeMillis()}.jpg"

    val archivo = File(
        context.filesDir,
        nombreArchivo
    )

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        archivo
    )
}

// ==========================================
// COPIAR FOTO DESDE LA GALERÍA
// ==========================================

fun copiarImagenAAlmacenamientoInterno(
    context: Context,
    uri: Uri
): Uri? {

    return try {

        val destino = File(
            context.filesDir,
            "evidencia_${System.currentTimeMillis()}.jpg"
        )

        context.contentResolver
            .openInputStream(uri)
            ?.use { entrada ->

                destino.outputStream().use { salida ->
                    entrada.copyTo(salida)
                }

            } ?: return null

        Uri.fromFile(destino)

    } catch (e: Exception) {
        null
    }
}
