plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "fr.florianfish.mywhiskybeer"
    compileSdk = 37

    defaultConfig {
        applicationId = "fr.florianfish.mywhiskybeer"
        minSdk = 26
        targetSdk = 35
        // Fournis par la CI (Forge-Apk) : release = tag sans le « v » et n° du run de release ;
        // debug = dernière release et n° du run de build (le suffixe -debug.<run> est ajouté plus bas)
        versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = System.getenv("VERSION_NAME") ?: "0.1.0"
        // Commit compilé, affiché dans Réglages › À propos
        buildConfigField("String", "COMMIT", "\"${System.getenv("GITHUB_SHA")?.take(7) ?: "local"}\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Clé fixe pour que chaque release s'installe par-dessus la précédente (sinon désinstallation = notes perdues)
    signingConfigs {
        create("release") {
            System.getenv("KEYSTORE_FILE")?.let { storeFile = file(it) }
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        // APK de test : suffixe pour ne pas le confondre avec une release, + n° du run CI quand il existe
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug" + (System.getenv("GITHUB_RUN_NUMBER")?.let { ".$it" } ?: "")
        }
        release {
            signingConfig = signingConfigs.getByName("debug") // TEMPORAIRE
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Pas de kotlinOptions avec AGP 9 : jvmTarget suit compileOptions

    // Schémas Room (app/schemas) en assets des builds de debug : MigrationTestHelper les y lit (tests Robolectric)
    sourceSets {
        getByName("debug") {
            assets.directories += "$projectDir/schemas"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true // assets et ressources disponibles pour Robolectric
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true // BuildConfig.VERSION_NAME, affiché dans l'appli
    }
}

ksp {
    // Schémas Room versionnés dans le repo, pour écrire les migrations plus tard
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.androidx.exifinterface)
    implementation(libs.mlkit.text.recognition) // OCR des étiquettes, modèle embarqué (hors-ligne)
    implementation(libs.play.services.code.scanner) // codes-barres

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
}

// Garde-fou (CI uniquement) : les schémas Room générés à la compilation doivent être identiques à ceux
// du repo. Une entité modifiée sans nouvelle version de base, ou une nouvelle version sans son schéma
// commité, fait échouer le build avant les tests.
val verifierSchemasRoom by tasks.registering {
    group = "verification"
    description = "Vérifie que app/schemas est à jour et commité."
    dependsOn("kspDebugKotlin")
    onlyIf { System.getenv("CI") == "true" }
    val schemas = layout.projectDirectory.dir("schemas").asFile
    doLast {
        val changes = providers.exec {
            commandLine("git", "status", "--porcelain", "--", schemas.path)
        }.standardOutput.asText.get()
        if (changes.isNotBlank()) {
            throw GradleException(
                "Schémas Room modifiés ou non commités :\n$changes\n" +
                    "Une entité a changé : augmenter AppDatabase.version, écrire la migration et son test, " +
                    "puis commiter le nouveau fichier de app/schemas.",
            )
        }
    }
}
tasks.matching { it.name == "testDebugUnitTest" }.configureEach { dependsOn(verifierSchemasRoom) }
