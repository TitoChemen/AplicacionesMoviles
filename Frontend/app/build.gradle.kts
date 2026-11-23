import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    alias(libs.plugins.kover)

}

val signingProperties = Properties().apply {
    val config = rootProject.file("signing/demo.properties")
    if (config.exists()) config.inputStream().use { load(it) }
}

android {
    namespace = "cl.duoc.huertaescolar"
    compileSdk = 34

    defaultConfig {
        applicationId = "cl.duoc.huertaescolar"
        minSdk = 24
        targetSdk = 34
        versionCode = 12
        versionName = "2.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Emulador por defecto; usar -PHUERTA_API_BASE_URL=http://IP_DEL_PC:8080/
        // para compilar un APK que funcione en un teléfono conectado a la misma red.
        val backendUrl = providers.gradleProperty("HUERTA_API_BASE_URL")
            .orElse(providers.environmentVariable("HUERTA_API_BASE_URL"))
            .orElse("http://10.0.2.2:8080/").get()
        require(backendUrl.startsWith("http://") || backendUrl.startsWith("https://"))
        require(backendUrl.endsWith("/") && '"' !in backendUrl && '\n' !in backendUrl)
        buildConfigField("String", "BACKEND_BASE_URL", "\"$backendUrl\"")
    }
    signingConfigs {
        create("demoRelease") {
            storeFile = rootProject.file("signing/huerta-demo-release.jks")
            storePassword = signingProperties.getProperty("storePassword")
            keyAlias = signingProperties.getProperty("keyAlias")
            keyPassword = signingProperties.getProperty("keyPassword")
        }
    }
    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
        release {
            signingConfig = signingConfigs.getByName("demoRelease")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")


    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Room (Base de datos local)
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    ksp("androidx.room:room-compiler:$room_version")
    implementation("androidx.room:room-ktx:$room_version")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // ViewModel y Lifecycle Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Miniatura de las fotos
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("androidx.core:core-splashscreen:1.0.1")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
}

kover {
    reports {
        filters {
            includes {
                classes(
                    "cl.duoc.huertaescolar.data.repository.ClimaRepository*",
                    "cl.duoc.huertaescolar.data.Constantes*",
                    "cl.duoc.huertaescolar.data.Perfil*",
                    "cl.duoc.huertaescolar.data.EstadoTarea*",
                    "cl.duoc.huertaescolar.data.local.entity.*"
                )
            }
            excludes {
                classes(
                    "cl.duoc.huertaescolar.ui.*",
                    "cl.duoc.huertaescolar.MainActivity*",
                    "cl.duoc.huertaescolar.HuertaAppNavigation*",
                    "*.*_Impl*"
                )
            }
        }
        verify {
            rule {
                minBound(80)
            }
        }
    }
}
