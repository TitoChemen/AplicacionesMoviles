
package cl.duoc.huertaescolar.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.huertaescolar.data.local.entity.Usuario
import cl.duoc.huertaescolar.data.remote.LoginRequestDto
import cl.duoc.huertaescolar.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val _usuarioActivo = MutableStateFlow<Usuario?>(null)
    val usuarioActivo: StateFlow<Usuario?> = _usuarioActivo.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _accessToken = MutableStateFlow<String?>(null)
    val accessToken: StateFlow<String?> = _accessToken.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    fun login(
        identificador: String,
        contrasena: String,
        onSuccess: () -> Unit
    ) {
        if (identificador.isBlank() || contrasena.isBlank()) {
            _loginError.value = "Completa todos los campos."
            return
        }

        if (_cargando.value) return

        viewModelScope.launch {
            _cargando.value = true
            _loginError.value = null

            try {
                val respuesta = RetrofitClient.authApi.login(
                    LoginRequestDto(
                        correo = identificador.trim(),
                        contrasena = contrasena
                    )
                )

                val datos = respuesta.usuario

                fun campo(nombre: String): String? {
                    val valor = datos.get(nombre)
                    return if (valor != null && !valor.isJsonNull) {
                        valor.asString
                    } else null
                }



                val activo = datos.get("activo")
                    ?.takeUnless { it.isJsonNull }
                    ?.asBoolean ?: false

                if (!activo) {
                    _loginError.value = "Esta cuenta se encuentra inactiva."
                    return@launch
                }

                val estado = "ACTIVA"

                val usuario = Usuario(
                    backendId = datos.get("id")
                        ?.takeUnless { it.isJsonNull }
                        ?.asLong,
                    identificador = campo("correo")
                        ?: identificador.trim(),
                    nombreFicticio = campo("nombreFicticio")
                        ?: campo("nombre")
                        ?: identificador.trim(),
                    perfil = campo("perfil")
                        ?: campo("rol")
                        ?: "ESTUDIANTE",
                    tallerAsignado = campo("tallerAsignado") ?: "",
                    estadoCuenta = estado,
                    contrasena = ""
                )

                _accessToken.value = respuesta.accessToken
                _usuarioActivo.value = usuario

                onSuccess()


            } catch (e: HttpException) {
                _loginError.value = when (e.code()) {
                    400 -> "Revisa los datos ingresados."
                    401 -> "Correo o contraseña incorrectos."
                    403 -> "No tienes permiso para ingresar."
                    else -> "Error del servidor: ${e.code()}"
                }
            } catch (e: IOException) {
                _loginError.value =
                    "No se pudo conectar con el servidor."
            } catch (e: Exception) {
                _loginError.value =
                    "No se pudo completar el inicio de sesión."
            } finally {
                _cargando.value = false
            }
        }
    }

    fun logout() {
        _usuarioActivo.value = null
        _accessToken.value = null
        _loginError.value = null
    }

    fun clearError() {
        _loginError.value = null
    }
}
