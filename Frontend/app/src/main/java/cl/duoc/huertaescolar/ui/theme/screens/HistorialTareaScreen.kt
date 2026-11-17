package cl.duoc.huertaescolar.ui.theme.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cl.duoc.huertaescolar.data.remote.RetrofitClient
import cl.duoc.huertaescolar.viewmodel.TareaViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Historial servido por MariaDB, NO generado a partir de tareas duplicadas en Android. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialTareaScreen(
    navController: NavController,
    viewModel: TareaViewModel,
    tareaId: Int,
    accessToken: String?
) {
    val eventos by viewModel.historialTarea.collectAsState()
    val tareas by viewModel.tareasSinFiltrar.collectAsState()
    val tarea = tareas.firstOrNull { it.id == tareaId }
    var fotoNombre by remember { mutableStateOf<String?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var errorFoto by remember { mutableStateOf<String?>(null) }
    var cargandoFoto by remember { mutableStateOf(false) }

    LaunchedEffect(tareaId, accessToken) {
        viewModel.cargarHistorial(tareaId, accessToken)
    }
    LaunchedEffect(fotoNombre, accessToken) {
        bitmap = null
        errorFoto = null
        val nombre = fotoNombre ?: return@LaunchedEffect
        if (accessToken.isNullOrBlank()) {
            errorFoto = "Inicia sesión para ver la fotografía"
            return@LaunchedEffect
        }
        cargandoFoto = true
        try {
            val respuesta = withContext(Dispatchers.IO) {
                RetrofitClient.tareaApi.descargarEvidencia(
                    token = "Bearer $accessToken", tareaId = tareaId.toLong(), nombre = nombre)
            }
            if (respuesta.isSuccessful) {
                bitmap = withContext(Dispatchers.IO) {
                    respuesta.body()?.use { BitmapFactory.decodeStream(it.byteStream()) }
                }
                if (bitmap == null) errorFoto = "La imagen no se pudo abrir."
            } else {
                errorFoto = "HTTP ${respuesta.code()} al cargar la foto."
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorFoto = "Error al cargar la foto: ${e.message}"
        } finally {
            cargandoFoto = false
        }
    }

    if (fotoNombre != null) {
        AlertDialog(onDismissRequest = { fotoNombre = null },
            title = { Text("Fotografía del intento") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (cargandoFoto) CircularProgressIndicator()
                    bitmap?.let { foto ->
                        Image(bitmap = foto.asImageBitmap(), contentDescription = "Fotografía histórica de evidencia",
                            modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                            contentScale = ContentScale.Fit)
                    }
                    errorFoto?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }, confirmButton = { TextButton(onClick = { fotoNombre = null }) { Text("Cerrar") } })
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Historial de evidencias") },
            navigationIcon = { IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
            } },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer))
    }) { insets ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(insets),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text(tarea?.tipo ?: "Tarea #$tareaId", style = MaterialTheme.typography.titleLarge)
                Text("Cada envío y revisión queda registrado por separado.",
                    style = MaterialTheme.typography.bodySmall)
            }
            if (viewModel.cargandoHistorial) item {
                CircularProgressIndicator(modifier = Modifier.padding(12.dp))
            }
            viewModel.errorHistorial?.let { error -> item {
                Text(error, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { viewModel.cargarHistorial(tareaId, accessToken) }) { Text("Reintentar") }
            } }
            if (!viewModel.cargandoHistorial && viewModel.errorHistorial == null && eventos.isEmpty()) item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Información anterior a la actualización", style = MaterialTheme.typography.titleMedium)
                        Text("Los intentos antiguos sobrescritos no pueden reconstruirse. " +
                            "Se muestra únicamente el último estado que aún conserva la tarea.")
                        tarea?.comentarioEstudiante?.takeIf { it.isNotBlank() }?.let {
                            Text("Última justificación: $it")
                        }
                        tarea?.comentarioDocente?.takeIf { it.isNotBlank() }?.let {
                            Text("Última observación docente: $it")
                        }
                        val fotoAnterior = tarea?.rutaFotografia?.let { Uri.parse(it).getQueryParameter("nombre") }
                        if (fotoAnterior != null) {
                            OutlinedButton(onClick = { fotoNombre = fotoAnterior }) {
                                Text("Ver última fotografía disponible")
                            }
                        }
                    }
                }
            }
            items(eventos.asReversed(), key = { it.id }) { evento ->
                val nombreAccion = when (evento.accion) {
                    "ENTREGA" -> "Evidencia enviada"
                    "RECHAZO" -> "Evidencia rechazada"
                    "APROBACION" -> "Evidencia aprobada"
                    else -> evento.accion
                }
                val identificacion = if (evento.accion == "ENTREGA") "Estudiante" else "Docente / Administración"
                val nombreArchivo = evento.evidenciaUrl?.let { Uri.parse(it).getQueryParameter("nombre") }
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(nombreAccion, style = MaterialTheme.typography.titleMedium,
                            color = if (evento.accion == "RECHAZO") MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary)
                        Text("Intento ${evento.intento} · $identificacion #${evento.actorId}",
                            style = MaterialTheme.typography.labelMedium)
                        Text(evento.fecha.replace("T", " ").take(19) + " UTC",
                            style = MaterialTheme.typography.labelSmall)
                        evento.comentario?.takeIf { it.isNotBlank() }?.let { comentario ->
                            Text(if (evento.accion == "ENTREGA") "Justificación:" else "Observación:",
                                style = MaterialTheme.typography.labelLarge)
                            Text(comentario, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (nombreArchivo != null) {
                            OutlinedButton(onClick = { fotoNombre = nombreArchivo }) {
                                Icon(Icons.Default.Image, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Ver fotografía de este intento")
                            }
                        } else if (!evento.evidenciaUrl.isNullOrBlank() && evento.accion == "ENTREGA") {
                            Text("Evidencia por URL: ${evento.evidenciaUrl}",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
