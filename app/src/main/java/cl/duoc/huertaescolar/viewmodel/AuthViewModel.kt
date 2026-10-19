package cl.duoc.huertaescolar.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.huertaescolar.data.database.HuertaDatabase
import cl.duoc.huertaescolar.data.local.entity.Usuario
import cl.duoc.huertaescolar.data.repository.HuertaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HuertaRepository

    init {
        val database = HuertaDatabase.obtenerBaseDatos(application)
        repository = HuertaRepository(
            sectorDao = database.sectorDao(),
            cultivoDao = database.cultivoDao(),
            tareaDao = database.tareaDao(),
            usuarioDao = database.usuarioDao(),
            alertaDao = database.alertaDao()
        )
    }

    // 🔄 Nombres alineados con tu HuertaAppNavigation.kt
    private val _usuarioActivo = MutableStateFlow<Usuario?>(null)
    val usuarioActivo: StateFlow<Usuario?> = _usuarioActivo.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    fun login(identificador: String, contrasena: String, onSuccess: () -> Unit) {
        if (identificador.isBlank() || contrasena.isBlank()) {
            _loginError.value = "Por favor, completa todos los campos."
            return
        }

        viewModelScope.launch {
            try {
                val usuario = repository.login(identificador.trim(), contrasena.trim())
                if (usuario != null) {
                    if (usuario.estadoCuenta == "ACTIVA") {
                        _usuarioActivo.value = usuario
                        _loginError.value = null
                        onSuccess()
                    } else {
                        _loginError.value = "Esta cuenta se encuentra inactiva."
                    }
                } else {
                    _loginError.value = "Identificador o contraseña incorrectos."
                }
            } catch (e: Exception) {
                _loginError.value = "Ocurrió un error al intentar iniciar sesión."
            }
        }
    }

    // 🔄 Nombre alineado a logout() como lo pide la navegación
    fun logout() {
        _usuarioActivo.value = null
        _loginError.value = null
    }

    fun clearError() {
        _loginError.value = null
    }
}