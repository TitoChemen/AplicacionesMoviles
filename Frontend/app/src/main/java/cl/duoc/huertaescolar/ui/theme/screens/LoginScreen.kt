package cl.duoc.huertaescolar.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    error: String?,
    onLogin: (String, String) -> Unit, // 🔑 Ahora recibe (identificador, contraseña)
    onEditar: () -> Unit
) {
    var identificador by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") } // 🔒 NUEVO: Estado para la contraseña

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

        // Campo de Identificador
        OutlinedTextField(
            value = identificador,
            onValueChange = {
                identificador = it
                onEditar()
            },
            label = { Text("Identificador de usuario") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // 🔒 NUEVO: Campo de Contraseña
        OutlinedTextField(
            value = contrasena,
            onValueChange = {
                contrasena = it
                onEditar()
            },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(), // Oculta los caracteres con puntitos
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Mensaje de Error (si las credenciales fallan)
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(24.dp))

        // Botón de Ingresar
        Button(
            onClick = { onLogin(identificador, contrasena) }, // 🔑 Enviamos ambos datos
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ingresar")
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = "Prueba con:\nadmin1 / admin123 (Administrador)\nestudiante1 / alumno123 (Estudiante)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
    }
}