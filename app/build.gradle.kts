plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
    id("com.google.devtools.ksp")
    // ajuste a versão do KSP para casar com a versão do Kotlin do seu projeto
    // (regra: <versão-kotlin>-<versão-ksp>, ex: kotlin 2.0.21 -> ksp 2.0.21-1.0.28
}

android {
    namespace = "br.com.rastreadorfrota"
    compileSdk = 35

    defaultConfig {
        applicationId = "br.com.rastreadorfrota"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-entrega1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Core / Compose
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
    implementation("androidx.credentials:credentials:1.6.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.8.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    // Firebase (BoM controla as versões de todos os módulos abaixo)
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.android.libraries.identity.googleid:googleid:1.2.0")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore-ktx")

    // Coroutines para tarefas assíncronas (login, sincronização)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Testes
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.03"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")


    // room-ktx foi incorporado ao room-runtime nas versões atuais,
    // então Flow/suspend já funcionam sem dependência extra
    val roomVersion = "2.8.4"
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.room:room-runtime:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    debugImplementation("androidx.compose.ui:ui-tooling")
}