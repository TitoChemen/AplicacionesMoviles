package cl.duoc.huertaescolar.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    error: String?,
    onLogin: (String) -> Unit,
    onEditar: () -> Unit
) {
    var identificador by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Huerta Escolar",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = identificador,
            onValueChange = {
                identificador = it
                onEditar()
            },
            label = { Text("Identificador de usuario") },
            isError = error != null,
            supportingText = {
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { onLogin(identificador) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ingresar")
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = "Usuarios de prueba: admin1 (administrador) · estudiante1 (estudiante)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
    }
}
