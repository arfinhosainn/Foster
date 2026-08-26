import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Onboarding"
            isStatic = true
            // Single iOS entry-point framework: re-export shared+home so all
            // Kotlin APIs surface through ONE module. Linking multiple static
            // Kotlin frameworks into the app embeds the runtime twice and
            // crashes at launch (KT-42254 "runtime injected twice").
            export(project(":shared"))
            export(project(":home"))
        }
    }

    android {
        namespace = "app.usenekko.onboarding"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(libs.datastore.preferences)
            implementation(libs.ktor.client.okhttp)
        }
        commonMain.dependencies {
            // api() so they can be exported into the iOS framework.
            api(project(":shared"))
            api(project(":home"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.liquid.glass)
            implementation(libs.jetbrains.compose.material.icons.extended)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.supabase.kt)
            implementation(libs.supabase.auth.kt)
            implementation(libs.supabase.postgrest.kt)
            implementation(libs.supabase.storage.kt)
            implementation(libs.supabase.compose.auth)
            implementation(libs.supabase.functions.kt)
            implementation(libs.ktor.client.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}
