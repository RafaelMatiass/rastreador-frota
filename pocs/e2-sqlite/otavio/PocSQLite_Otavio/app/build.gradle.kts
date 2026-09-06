plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // alias(libs.plugins.google.services) // Removed due to missing google-services.json
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.pocsqlite_otavio"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.pocsqlite_otavio"
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
    implementation(libs.androidx.core.ktx)
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
    implementation("androidx.credentials:credentials:1.6.0")
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.navigation:navigation-compose:2.8.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")

    // Firebase (BoM controla as versões de todos os módulos abaixo)
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.android.libraries.identity.googleid:googleid:1.2.0")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore-ktx")

    // Coroutines para tarefas assíncronas (login, sincronização)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Testes
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)


    // room-ktx foi incorporado ao room-runtime nas versões atuais,
    // então Flow/suspend já funcionam sem dependência extra
    val roomVersion = "2.8.4"
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.room:room-runtime:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    debugImplementation("androidx.compose.ui:ui-tooling")
}