package cl.duoc.huertaescolar.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cl.duoc.huertaescolar.data.local.entity.Usuario
import cl.duoc.huertaescolar.data.repository.HuertaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: HuertaRepository) : ViewModel() {
    var usuarioActual by mutableStateOf<Usuario?>(null)
        private set

    var mensajeError by mutableStateOf<String?>(null)
        private set

    fun iniciarSesion(identificador: String, onLoginSuccess: (String) -> Unit) {
        viewModelScope.launch {
            val usuario = repository.login(identificador)
            if (usuario != null) {
                usuarioActual = usuario
                mensajeError = null
                onLoginSuccess(usuario.perfil) // Retorna si es ADMINISTRADOR o ESTUDIANTE
            } else {
                mensajeError = "Usuario no encontrado o cuenta inactiva."
            }
        }
    }
    /*lo que la IA me dijo*/
    // Almacena el usuario autenticado. Si es null, no hay sesión activa.
    private val _usuarioActivo = MutableStateFlow<Usuario?>(null)
    val usuarioActivo: StateFlow<Usuario?> = _usuarioActivo.asStateFlow()

    // Maneja los mensajes de error para mostrarlos en la UI
    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    fun login(correo: String, contrasena: String) {
        // 1. Validación inicial (Checklist: Validar campos obligatorios)
        if (correo.isBlank() || contrasena.isBlank()) {
            _loginError.value = "Por favor, completa todos los campos."
            return
        }

        // 2. Consulta a la base de datos de forma asíncrona
        viewModelScope.launch {
            try {
                // El repositorio llama al UsuarioDao.login()
                val usuario = repository.loginUsuario(correo, contrasena)

                if (usuario != null) {
                    _usuarioActivo.value = usuario
                    _loginError.value = null // Limpiamos el error si el login es exitoso
                } else {
                    _loginError.value = "Correo o contraseña incorrectos."
                }
            } catch (e: Exception) {
                _loginError.value = "Ocurrió un error al intentar iniciar sesión."
            }
        }
    }

    fun logout() {
        _usuarioActivo.value = null
        _loginError.value = null
    }

    fun clearError() {
        _loginError.value = null
    }
}

@Composable
fun HuertaAppNavigation(authViewModel: AuthViewModel){
    val navController = rememberNavController()

    //val usuario by authViewModel.usuarioActual.collectAsState() NO PUDE ARREGLAR EL ERROR

    NavHost(navController = navController, startDestination = "Login"){
        composable("login"){
            //LoginScreen(navController = navController, viewModel = authViewModel)NO PUDE ARREGLAR EL ERROR
        }
        composable("dashboard") {
            // El dashboard lee quién es el usuario para mostrar botones de Admin o Alumno
            //DashboardScreen(usuario = usuario, navController = navController)NO PUDE ARREGLAR EL ERROR
        }
    }

}