package cl.duoc.huertaescolar.ui.theme.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cl.duoc.huertaescolar.data.local.entity.Usuario

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    usuario: Usuario?,
    navController: NavController,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hola, ${usuario?.nombreFicticio?.split(" ")?.first() ?: "Usuario"}") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(imageVector = Icons.Filled.ExitToApp, contentDescription = "Cerrar sesión")
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
            // Indicador visual del rol actual
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Panel de ${usuario?.perfil ?: "Invitado"}",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            // Renderizado dinámico según el rol (Checklist: Mostrar un Dashboard diferente según el rol)
            if (usuario?.perfil == "Administrador") {
                MenuAdministrador(navController)
            } else {
                MenuEstudiante(navController)
            }
        }
    }
}

@Composable
fun MenuAdministrador(navController: NavController) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DashboardCard(
                titulo = "Sectores y Cultivos",
                icono = Icons.Filled.Forest, // Asegúrate de tener la dependencia material-icons-extended
                onClick = { navController.navigate("lista_sectores") }
            )
        }
        item {
            DashboardCard(
                titulo = "Gestión de Tareas",
                icono = Icons.Filled.Assignment,
                onClick = { navController.navigate("lista_tareas") }
            )
        }
        item {
            DashboardCard(
                titulo = "Validar Evidencias",
                icono = Icons.Filled.FactCheck,
                onClick = { navController.navigate("revision_tareas") }
            )
        }
        item {
            DashboardCard(
                titulo = "Reportes Huerto",
                icono = Icons.Filled.Warning,
                onClick = { navController.navigate("lista_reportes") }
            )
        }
    }
}

@Composable
fun MenuEstudiante(navController: NavController) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DashboardCard(
                titulo = "Mis Tareas",
                icono = Icons.Filled.AssignmentInd,
                onClick = { navController.navigate("mis_tareas") }
            )
        }
        item {
            DashboardCard(
                titulo = "Reportar Problema",
                icono = Icons.Filled.ReportProblem,
                onClick = { navController.navigate("crear_reporte") }
            )
        }
        item {
            DashboardCard(
                titulo = "Historial",
                icono = Icons.Filled.History,
                onClick = { navController.navigate("historial_actividades") }
            )
        }
    }
}

// Componente reutilizable para mantener la consistencia en UI (Checklist)
@Composable
fun DashboardCard(titulo: String, icono: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icono,
                contentDescription = titulo,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = titulo,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}