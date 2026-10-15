package cl.duoc.huertaescolar.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.huertaescolar.data.local.entity.Usuario
import cl.duoc.huertaescolar.data.repository.HuertaRepository
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
}