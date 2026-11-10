
package cl.duoc.huertaescolar.ui.theme.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cl.duoc.huertaescolar.data.EstadoTarea
import cl.duoc.huertaescolar.data.remote.RetrofitClient
import cl.duoc.huertaescolar.viewmodel.TareaViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ValidacionTareaScreen(
    navController: NavController,
    viewModel: TareaViewModel,
    tareaId: Int,
    accessToken: String?
) {
    val context = LocalContext.current

    val tareas by viewModel.tareasSinFiltrar.collectAsState()
    val tarea = tareas.find { it.id == tareaId }

    var procesando by remember {
        mutableStateOf(false)
    }

    var imagenEvidencia by remember {
        mutableStateOf<Bitmap?>(null)
    }

    var errorImagen by remember {
        mutableStateOf<String?>(null)
    }

    var cargandoImagen by remember {
        mutableStateOf(false)
    }

    val puedeEvaluarse =
        tarea?.estado == EstadoTarea.PENDIENTE_VALIDACION ||
                tarea?.estado == "REALIZADA"

    // ==========================================
    // DESCARGAR FOTOGRAFÍA DESDE SPRING BOOT
    // ==========================================

    LaunchedEffect(
        tareaId,
        tarea?.rutaFotografia,
        accessToken
    ) {
        imagenEvidencia = null
        errorImagen = null
        cargandoImagen = false

        val ruta = tarea?.rutaFotografia

        if (ruta.isNullOrBlank()) {
            return@LaunchedEffect
        }

        if (accessToken.isNullOrBlank()) {
            errorImagen = "No hay sesión activa."
            return@LaunchedEffect
        }

        val nombre = Uri.parse(ruta)
            .getQueryParameter("nombre")

        if (nombre.isNullOrBlank()) {
            errorImagen = "La evidencia no tiene un nombre válido."
            return@LaunchedEffect
        }

        cargandoImagen = true

        try {
            val respuesta = withContext(Dispatchers.IO) {
                RetrofitClient.tareaApi.descargarEvidencia(
                    token = "Bearer $accessToken",
                    tareaId = tareaId.toLong(),
                    nombre = nombre
                )
            }

            if (respuesta.isSuccessful) {
                val bitmap = withContext(Dispatchers.IO) {
                    respuesta.body()?.use { cuerpo ->
                        BitmapFactory.decodeStream(
                            cuerpo.byteStream()
                        )
                    }
                }

                if (bitmap != null) {
                    imagenEvidencia = bitmap
                } else {
                    errorImagen = "No se pudo leer la fotografía."
                }
            } else {
                errorImagen =
                    "Error HTTP ${respuesta.code()} al descargar."
            }

        } catch (e: CancellationException) {
            throw e

        } catch (e: Exception) {
            errorImagen =
                e.message ?: "Error al descargar la fotografía."

        } finally {
            cargandoImagen = false
        }
    }

    // ==========================================
    // PANTALLA
    // ==========================================

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Validar Evidencia de Tarea")
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.popBackStack()
                        },
                        enabled = !procesando
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

        if (tarea == null) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No se encontró la tarea solicitada.")
            }

        } else {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ==================================
                // INFORMACIÓN DE LA TAREA
                // ==================================

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Tipo: ${tarea.tipo}",
                            style =
                                MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = tarea.descripcion
                        )

                        Text(
                            text = "Asignado a: ${tarea.asignadoA}",
                            style =
                                MaterialTheme.typography.bodySmall
                        )

                        Text(
                            text = "Estado: ${
                                EstadoTarea.etiqueta(tarea.estado)
                            }",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // ==================================
                // EVIDENCIA DEL ESTUDIANTE
                // ==================================

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {

                        Text(
                            text = "Evidencia del estudiante",
                            style =
                                MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Comentario del estudiante:",
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = tarea.comentarioEstudiante
                                ?.takeIf { it.isNotBlank() }
                                ?: "Sin comentarios."
                        )

                        Divider()

                        Text(
                            text = "Fotografía adjunta:",
                            fontWeight = FontWeight.SemiBold
                        )

                        // MOSTRAR FOTOGRAFÍA
                        when {
                            imagenEvidencia != null -> {
                                Image(
                                    bitmap =
                                        imagenEvidencia!!.asImageBitmap(),
                                    contentDescription =
                                        "Evidencia fotográfica",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(240.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            errorImagen != null -> {
                                Text(
                                    text = errorImagen!!,
                                    color =
                                        MaterialTheme.colorScheme.error
                                )
                            }

                            cargandoImagen -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                    contentAlignment =
                                        Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }

                            tarea.rutaFotografia.isNullOrBlank() -> {
                                Text(
                                    "No se adjuntó fotografía."
                                )
                            }

                            else -> {
                                Text(
                                    "Esperando fotografía..."
                                )
                            }
                        }
                    }
                }

                // ==================================
                // VALIDACIÓN DEL ADMINISTRADOR
                // ==================================

                if (puedeEvaluarse) {

                    OutlinedTextField(
                        value = viewModel.comentarioValidacion,
                        onValueChange = {
                            viewModel.comentarioValidacion = it
                        },
                        label = {
                            Text(
                                "Retroalimentación del administrador"
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !procesando,
                        minLines = 3
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {

                        // RECHAZAR
                        Button(
                            onClick = {
                                procesando = true

                                viewModel.evaluarTarea(
                                    tareaId = tareaId,
                                    aprobada = false,
                                    accessToken = accessToken,
                                    onSuccess = {
                                        procesando = false

                                        Toast.makeText(
                                            context,
                                            "Evidencia rechazada correctamente",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        navController.popBackStack()
                                    },
                                    onError = {
                                        procesando = false

                                        Toast.makeText(
                                            context,
                                            "No se pudo rechazar la evidencia",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                )
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !procesando &&
                                    !accessToken.isNullOrBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor =
                                    MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Rechazar")
                        }

                        // APROBAR
                        Button(
                            onClick = {
                                procesando = true

                                viewModel.evaluarTarea(
                                    tareaId = tareaId,
                                    aprobada = true,
                                    accessToken = accessToken,
                                    onSuccess = {
                                        procesando = false

                                        Toast.makeText(
                                            context,
                                            "Evidencia aprobada correctamente",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        navController.popBackStack()
                                    },
                                    onError = {
                                        procesando = false

                                        Toast.makeText(
                                            context,
                                            "No se pudo aprobar la evidencia",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                )
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !procesando &&
                                    !accessToken.isNullOrBlank()
                        ) {
                            Text("Aprobar")
                        }
                    }

                    if (procesando) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.Center,
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )

                            Spacer(Modifier.width(12.dp))

                            Text("Procesando validación...")
                        }
                    }

                    if (accessToken.isNullOrBlank()) {
                        Text(
                            text =
                                "No hay una sesión activa para validar.",
                            color =
                                MaterialTheme.colorScheme.error
                        )
                    }

                } else {

                    Text(
                        text =
                            "Esta tarea no está pendiente de validación.",
                        color =
                            MaterialTheme.colorScheme.secondary
                    )

                    if (!tarea.comentarioDocente.isNullOrBlank()) {
                        Text(
                            text =
                                "Retroalimentación: ${tarea.comentarioDocente}"
                        )
                    }
                }
            }
        }
    }
}
