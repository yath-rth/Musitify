import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.net.HttpURLConnection
import java.net.URI
import java.util.zip.ZipFile

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
    // Remote images (YouTube search thumbnails); local art keeps loading from files.
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.6.3")

    // Networking (same as Android)
    implementation("com.squareup.okhttp3:okhttp:5.4.0")

    // Playback (replaces ExoPlayer)
    listOf("base", "graphics", "media", "swing").forEach {
        implementation("org.openjfx:javafx-$it:$javafxVersion:$javafxPlatform")
    }

    // Global media keys
    implementation("com.github.kwhat:jnativehook:2.2.2")

    testImplementation(kotlin("test"))
    testImplementation("com.squareup.okhttp3:mockwebserver3:5.4.0")
}

// ---------------------------------------------------------------------------
// Bundled native tools (yt-dlp.exe, ffmpeg.exe, ffprobe.exe)
//
// downloadTools  -> fetches the exes into ./bin (gitignored), skipped if present
// stageTools     -> copies them into build/appResources/windows/ which is the
//                   Compose `appResourcesRootDir`; at runtime the folder is exposed
//                   through System.getProperty("compose.application.resources.dir")
// ---------------------------------------------------------------------------
val toolsBinDir = layout.projectDirectory.dir("bin")
val appResourcesRoot = layout.buildDirectory.dir("appResources")

val downloadTools by tasks.registering {
    group = "musitify"
    description = "Downloads yt-dlp.exe, ffmpeg.exe and ffprobe.exe into ./bin"
    val binDir = toolsBinDir.asFile
    outputs.dir(binDir)
    outputs.upToDateWhen {
        listOf("yt-dlp.exe", "ffmpeg.exe", "ffprobe.exe").all { File(binDir, it).isFile }
    }
    doLast {
        binDir.mkdirs()
        fun fetch(url: String, dest: File) {
            val tmp = File(dest.parentFile, dest.name + ".part")
            var lastError: Exception? = null
            for (attempt in 1..3) {
                try {
                    logger.lifecycle("Downloading $url (attempt $attempt)")
                    val conn = URI(url).toURL().openConnection() as HttpURLConnection
                    conn.instanceFollowRedirects = true
                    conn.connectTimeout = 30_000
                    conn.readTimeout = 120_000
                    conn.setRequestProperty("User-Agent", "Musitify-build")
                    conn.inputStream.use { input -> tmp.outputStream().use { input.copyTo(it) } }
                    if (dest.exists()) dest.delete()
                    check(tmp.renameTo(dest)) { "Could not move $tmp to $dest" }
                    return
                } catch (e: Exception) {
                    lastError = e
                    logger.warn("Download failed: ${e.message}")
                    tmp.delete()
                }
            }
            throw GradleException("Failed to download $url", lastError)
        }

        val ytDlp = File(binDir, "yt-dlp.exe")
        if (!ytDlp.isFile) {
            fetch("https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe", ytDlp)
        }

        val wanted = listOf("ffmpeg.exe", "ffprobe.exe")
        if (wanted.any { !File(binDir, it).isFile }) {
            val zip = File(temporaryDir, "ffmpeg.zip")
            fetch(
                "https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win64-gpl.zip",
                zip
            )
            ZipFile(zip).use { zf ->
                for (name in wanted) {
                    val entry = zf.entries().asSequence()
                        .firstOrNull { !it.isDirectory && it.name.substringAfterLast('/') == name && it.name.contains("/bin/") }
                        ?: error("$name not found in ffmpeg archive")
                    zf.getInputStream(entry).use { input ->
                        File(binDir, name).outputStream().use { input.copyTo(it) }
                    }
                }
            }
            zip.delete()
        }
    }
}

val stageTools by tasks.registering(Sync::class) {
    group = "musitify"
    description = "Stages bundled tools under the Compose appResources/windows folder"
    dependsOn(downloadTools)
    from(toolsBinDir) { include("yt-dlp.exe", "ffmpeg.exe", "ffprobe.exe") }
    into(appResourcesRoot.map { it.dir("windows") })
}

tasks.matching {
    it.name == "prepareAppResources" || it.name == "run" || it.name.startsWith("package") ||
        it.name.startsWith("createDistributable") || it.name.startsWith("createRuntimeImage") ||
        it.name.startsWith("runDistributable") || it.name.startsWith("runRelease")
}.configureEach { dependsOn(stageTools) }

compose.desktop {
    application {
        mainClass = "com.yatharth.musitify.MainKt"
        jvmArgs += listOf("-Dfile.encoding=UTF-8")

        nativeDistributions {
            appResourcesRootDir.set(appResourcesRoot)
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
                iconFile.set(project.file("src/main/resources/icons/musitify.ico"))
            }
        }
    }
}
