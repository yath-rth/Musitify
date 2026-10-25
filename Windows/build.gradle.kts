import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.4.10"
    kotlin("plugin.serialization") version "2.4.10"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10"
    id("org.jetbrains.compose") version "1.12.2"
    id("com.google.devtools.ksp") version "2.3.12"
    id("androidx.room") version "2.8.4"
}

group = "com.yatharth.musitify"
version = "1.0.0"

kotlin {
    jvmToolchain(17)
}

room {
    schemaDirectory("$projectDir/schemas")
}

val javafxVersion = "21.0.13"
val javafxPlatform = "win"
val roomVersion = "2.8.4"

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
    implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.9.6")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    // Database (same Room schema as Android)
    implementation("androidx.room:room-runtime:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.sqlite:sqlite-bundled:2.6.2")

    // DI (replaces Hilt)
    implementation("io.insert-koin:koin-core:4.2.2")
    implementation("io.insert-koin:koin-compose:4.2.2")
    implementation("io.insert-koin:koin-compose-viewmodel:4.2.2")

    // Images (replaces Coil 2)
    implementation("io.coil-kt.coil3:coil-compose:3.6.3")

    // Networking (same as Android)
    implementation("com.squareup.okhttp3:okhttp:5.4.0")

    // Playback (replaces ExoPlayer)
    listOf("base", "graphics", "media", "swing").forEach {
        implementation("org.openjfx:javafx-$it:$javafxVersion:$javafxPlatform")
    }

    // Global media keys
    implementation("com.github.kwhat:jnativehook:2.2.2")

    testImplementation(kotlin("test"))
}

compose.desktop {
    application {
        mainClass = "com.yatharth.musitify.MainKt"
        jvmArgs += listOf("-Dfile.encoding=UTF-8")

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "Musitify"
            packageVersion = "1.0.0"
            description = "Offline-first music player and downloader"
            vendor = "Yatharth"
            modules("java.sql", "java.naming", "jdk.unsupported")
            windows {
                menuGroup = "Musitify"
                upgradeUuid = "6f1d1c3e-2b8a-4c55-9a3e-6b0f8a1d7c42"
                shortcut = true
                dirChooser = true
            }
        }
    }
}
