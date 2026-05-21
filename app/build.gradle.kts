import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
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

    buildTypes {
        release {
            isMinifyEnabled = false
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

fun buildApkFileName(outputDir: File): String {
    val date = SimpleDateFormat("yyMMdd", Locale.US).format(Date())
    val regex = Regex("^PDF_${date}_(\\d{2})\\.apk${'$'}")

    val maxNumber = outputDir
        .listFiles()
        ?.mapNotNull { file ->
            val match = regex.matchEntire(file.name)
            match?.groupValues?.getOrNull(1)?.toIntOrNull()
        }
        ?.maxOrNull()
        ?: 0

    val nextNumber = maxNumber + 1
    return "PDF_${date}_${"%02d".format(nextNumber)}.apk"
}

val renameDebugApk by tasks.registering {
    doLast {
        val outputDir = layout.buildDirectory.dir("outputs/apk/debug").get().asFile
        outputDir.mkdirs()

        val defaultDebugApk = outputDir
            .listFiles()
            ?.firstOrNull { it.isFile && it.extension == "apk" && !it.name.startsWith("PDF_") }
            ?: return@doLast

        val nextName = buildApkFileName(outputDir)
        val targetFile = File(outputDir, nextName)

        if (!targetFile.exists()) {
            defaultDebugApk.copyTo(targetFile)
        }
    }
}

tasks.configureEach {
    if (name == "assembleDebug") {
        finalizedBy(renameDebugApk)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.activity:activity-compose:1.9.1")

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
