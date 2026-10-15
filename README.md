# 🌿 Huerta Escolar - App Móvil

Aplicación móvil desarrollada en **Android Studio (Kotlin)** bajo la arquitectura **MVVM** y almacenamiento local con **Room**, diseñada para la gestión integral de sectores, cultivos, tareas y alertas de un huerto escolar.

---

## 👥 Equipo de Desarrollo y Roles (Ruta Crítica)
El proyecto está estructurado bajo una división estricta de responsabilidades para asegurar la integridad de la arquitectura backend y la rendición de cuentas:

*   **Kevin:** Arquitectura de software, Capa de Datos (Room, Entidades, DAOs), Repositorio y ViewModel de Autenticación (Control de la ruta crítica del backend).
*   **Oscar:** Diseño de Interfaces (UI/UX) con Material 3, formularios de registro, navegación y simulación del módulo de sensores.
*   **Jaime:** Gestión de tareas para estudiantes, alertas, reportes de problemas e integración de recursos nativos (Cámara / Galería).

---

## 🛠️ Tecnologías y Stack
*   **Lenguaje:** Kotlin 2.0.0
*   **Interfaz de Usuario:** Jetpack Compose & Material 3
*   **Arquitectura:** MVVM (Model-View-ViewModel) + Repository Pattern
*   **Base de Datos Local:** Room Database (con KSP y corrutinas Flow)
*   **Navegación:** Navigation Compose
*   **Control de Versiones:** Git / GitHub

---

## 📂 Arquitectura del Proyecto
El proyecto sigue una organización modular limpia dentro del paquete `cl.duoc.huertaescolar`:

```text
cl.duoc.huertaescolar/
│
├── data/
│   ├── database/     # Instancia de HuertaDatabase y precarga de usuarios
│   ├── local/        # DAOs (CultivoDao, SectorDao, TareaDao, UsuarioDao) 
│   │   └── entity/   # Entidades Room (Cultivo, Sector, Tarea, Usuario)
│   └── repository/   # HuertaRepository (Puente de datos central)
│
├── viewmodel/        # AuthViewModel y estados de la UI
├── ui/theme/         # Colores, tipografías y MaterialTheme
└── MainActivity.kt   # Punto de entrada de la aplicación