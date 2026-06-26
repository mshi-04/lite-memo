import io.gitlab.arturbosch.detekt.Detekt
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.detekt)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.google.services)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kover)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties =
    Properties().apply {
        if (keystorePropertiesFile.exists()) {
            keystorePropertiesFile.inputStream().use { load(it) }
            val requiredKeys = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
            val missingKeys = requiredKeys.filter { getProperty(it).isNullOrBlank() }
            if (missingKeys.isNotEmpty()) {
                throw GradleException(
                    "keystore.properties に必須キーがありません: ${missingKeys.joinToString()}"
                )
            }
        }
    }

android {
    namespace = "com.appvoyager.litememo"
    compileSdk {
        version =
            release(36) {
                minorApiLevel = 1
            }
    }

    defaultConfig {
        applicationId = "com.appvoyager.litememo"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            signingConfig = signingConfigs.getByName("debug")
        }
        create("prod") {
            dimension = "environment"
        }
    }

    buildTypes {
        release {
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
    lint {
        warningsAsErrors = true
        abortOnError = true
        disable += setOf(
            "NewerVersionAvailable",
            "GradleDependency",
            "AndroidGradlePluginVersion",
            "OldTargetApi"
        )
    }
}

kotlin {
    jvmToolchain(17)
}

ktlint {
    android.set(true)
    version.set(libs.versions.ktlintCli)
    outputToConsole.set(true)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    baseline = rootProject.file("config/detekt/baseline.xml")
}

val preCommitFilesProperty = providers.gradleProperty("preCommitFiles")

tasks.register<Detekt>("detektPreCommit") {
    description = "Runs detekt only on files passed via -PpreCommitFiles."
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    baseline.set(rootProject.file("config/detekt/baseline.xml"))
    val files =
        preCommitFilesProperty.orNull
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.map { rootProject.file(it) }
            .orEmpty()
    setSource(files)
    onlyIf { files.isNotEmpty() }
}

val ktlintCliConfiguration: Configuration by configurations.creating
dependencies {
    ktlintCliConfiguration(libs.ktlint.cli)
}

tasks.register<JavaExec>("ktlintFormatPreCommit") {
    description = "Formats files passed via -PpreCommitFiles using the ktlint CLI."
    classpath = ktlintCliConfiguration
    mainClass.set("com.pinterest.ktlint.Main")
    val files =
        preCommitFilesProperty.orNull
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.map { rootProject.file(it).absolutePath }
            .orEmpty()
    args = listOf("--format") + files
    onlyIf { files.isNotEmpty() }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // AndroidX
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)

    // Kotlin
    implementation(platform(libs.kotlinx.serialization.bom))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)

    // Ads
    implementation(libs.play.services.ads)

    // DI
    implementation(libs.hilt.android)
    implementation(libs.hilt.lifecycle.viewmodel.compose)

    // Annotation processors
    ksp(libs.androidx.room.compiler)
    ksp(libs.hilt.compiler)

    // Unit test
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)

    // Instrumented test
    androidTestImplementation(platform(libs.kotlinx.serialization.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)

    // Debug tooling
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Static analysis
    detektPlugins(libs.detekt.compose.rules)
}
