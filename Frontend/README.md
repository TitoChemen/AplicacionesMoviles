# App Android · Huerta Escolar

Abrir esta carpeta en Android Studio. La arquitectura actual usa Jetpack Compose, ViewModel y Retrofit contra los microservicios; Room permanece en algunos módulos históricos, pero los flujos principales de sectores, cultivos, tareas y alertas usan el backend Spring Boot.

El emulador accede al gateway mediante `http://10.0.2.2:8080/`. Configurar `HUERTA_API_BASE_URL` al compilar para un teléfono físico. La tarjeta de Open-Meteo presenta clima exterior de Santiago, separado de los sensores simulados de la huerta.

Las instrucciones completas de instalación, pruebas, firma y endpoints están en [`../README.md`](../README.md).
