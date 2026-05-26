import java.io.File
import java.util.Properties
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
val hasKeystoreProperties = keystorePropertiesFile.exists()

if (hasKeystoreProperties) {
    keystorePropertiesFile.inputStream().use(keystoreProperties::load)
}

val releaseTaskRequested = gradle.startParameter.taskNames.any { taskName ->
    taskName.contains("Release", ignoreCase = true)
}

fun requireKeystoreProperty(name: String): String {
    return keystoreProperties.getProperty(name)?.takeIf { it.isNotBlank() }
        ?: throw GradleException("keystore.properties에 ${name} 값이 필요합니다.")
}

fun buildArtifactFileName(outputDir: File, extension: String): String {
    val date = SimpleDateFormat("yyMMdd", Locale.US).format(Date())
    val regex = Regex("^PDF_${date}_(\\d{2})\\.${extension}${'$'}")

    val maxNumber = outputDir
        .listFiles()
        ?.mapNotNull { file ->
            val match = regex.matchEntire(file.name)
            match?.groupValues?.getOrNull(1)?.toIntOrNull()
        }
        ?.maxOrNull()
        ?: 0

    val nextNumber = maxNumber + 1
    return "PDF_${date}_${"%02d".format(nextNumber)}.${extension}"
}

if (releaseTaskRequested && !hasKeystoreProperties) {
    throw GradleException("Release 빌드를 위해 루트에 keystore.properties 파일이 필요합니다.")
}

android {
    namespace = "com.pdftruth"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.pdftruth"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (hasKeystoreProperties) {
                val storeFilePath = requireKeystoreProperty("storeFile")
                val releaseStoreFile = rootProject.file(storeFilePath)
                if (!releaseStoreFile.exists()) {
                    throw GradleException("Release keystore 파일을 찾을 수 없습니다: ${releaseStoreFile.path}")
                }

                storeFile = releaseStoreFile
                storePassword = requireKeystoreProperty("storePassword")
                keyAlias = requireKeystoreProperty("keyAlias")
                keyPassword = requireKeystoreProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

val renameDebugApk by tasks.registering {
    doLast {
        val outputDir = layout.buildDirectory.dir("outputs/apk/debug").get().asFile
        outputDir.mkdirs()

        val defaultDebugApk = outputDir
            .listFiles()
            ?.firstOrNull { it.isFile && it.extension == "apk" && !it.name.startsWith("PDF_") }
            ?: return@doLast

        val nextName = buildArtifactFileName(outputDir, "apk")
        val targetFile = File(outputDir, nextName)

        if (!targetFile.exists()) {
            defaultDebugApk.copyTo(targetFile)
        }
    }
}

val renameReleaseApk by tasks.registering {
    doLast {
        val outputDir = layout.buildDirectory.dir("outputs/apk/release").get().asFile
        outputDir.mkdirs()

        val defaultReleaseApk = outputDir
            .listFiles()
            ?.firstOrNull { it.isFile && it.extension == "apk" && !it.name.startsWith("PDF_") }
            ?: return@doLast

        val nextName = buildArtifactFileName(outputDir, "apk")
        val targetFile = File(outputDir, nextName)

        if (!targetFile.exists()) {
            defaultReleaseApk.copyTo(targetFile)
        }
    }
}

val renameReleaseBundle by tasks.registering {
    doLast {
        val outputDir = layout.buildDirectory.dir("outputs/bundle/release").get().asFile
        outputDir.mkdirs()

        val defaultReleaseBundle = outputDir
            .listFiles()
            ?.firstOrNull { it.isFile && it.extension == "aab" && !it.name.startsWith("PDF_") }
            ?: return@doLast

        val nextName = buildArtifactFileName(outputDir, "aab")
        val targetFile = File(outputDir, nextName)

        if (!targetFile.exists()) {
            defaultReleaseBundle.copyTo(targetFile)
        }
    }
}

tasks.configureEach {
    if (name == "assembleDebug") {
        finalizedBy(renameDebugApk)
    }
    if (name == "assembleRelease") {
        finalizedBy(renameReleaseApk)
    }
    if (name == "bundleRelease") {
        finalizedBy(renameReleaseBundle)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.3")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
