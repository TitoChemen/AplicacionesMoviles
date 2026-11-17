package cl.duoc.huertaescolar.ui.theme.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cl.duoc.huertaescolar.viewmodel.TareaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialActividadesScreen(navController: NavController, viewModel: TareaViewModel) {
    val tareas by viewModel.tareasSinFiltrar.collectAsState()
    val intentos by viewModel.intentosPorTarea.collectAsState()
    val registradas = tareas.filter { tarea ->
        (intentos[tarea.id] ?: 0) > 0 || tarea.estado in listOf(
            "APROBADA", "RECHAZADA", "PENDIENTE_VALIDACION", "REALIZADA")
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Historial de actividades") },
        navigationIcon = { IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
        } }, colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer)) }) { insets ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(insets),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Toca una tarea para revisar todos sus intentos y comentarios.") }
            if (registradas.isEmpty()) item { Text("Aún no tienes entregas registradas en el nuevo historial.") }
            items(registradas, key = { it.id }) { tarea ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()
                    .clickable { navController.navigate("historial_tarea/${tarea.id}") }) {
                    Column(Modifier.padding(16.dp)) {
                        Text(tarea.tipo, style = MaterialTheme.typography.titleMedium)
                        Text(if ((intentos[tarea.id] ?: 0) > 0)
                            "${intentos[tarea.id]} envío(s) · ${tarea.estado}"
                        else "Estado actual: ${tarea.estado} · Sin historial anterior",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
