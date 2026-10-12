import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.vanniktech.mavenPublish)
    kotlin("plugin.serialization") version libs.versions.kotlin
}

val projectName = "krouter"

group = "io.github.asnaeb"
version = "0.0.1"

kotlin {
    iosArm64()
    iosSimulatorArm64()

    linuxX64()

    jvm()

    js {
        binaries.executable()
        browser()
        nodejs()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        binaries.executable()
        browser()
        nodejs()
    }

    android {
        namespace = "io.github.asnaeb.krouter"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withJava()

        withHostTestBuilder {  }.configure {  }

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.ui.test)
        }

        jvmTest.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    dependencies {
        implementation(libs.compose.runtime)
        implementation(libs.jetbrains.navigation3.ui)
        implementation(libs.kotlinx.serialization.json)
    }
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(group.toString(), projectName, version.toString())

    pom {
        name = projectName
        description = "A tiny layer over Navigation3"
        inceptionYear = "2026"
        url = "https://github.com/asnaeb/$projectName"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "asnaeb"
                name = "Roberto De Lucia"
                url = "https://github.com/asnaeb"
            }
        }
        scm {
            url = "https://github.com/asnaeb/$projectName"
            connection = "scm:git:https://github.com/asnaeb/$projectName.git"
            developerConnection = "scm:git:ssh://git@github.com/asnaeb/$projectName.git"
        }
    }
}