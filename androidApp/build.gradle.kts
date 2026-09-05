import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

/**
 * Secret resolution: environment variable first (CI — local.properties does not
 * exist there), then the gitignored local.properties, then a hard failure at
 * CONFIGURATION time. A missing secret must fail the build in seconds with an
 * actionable message, never surface as a runtime 401. See local.properties.example.
 */
fun resolveSecret(envName: String, propertyName: String): String =
    System.getenv(envName)?.takeIf { it.isNotBlank() }
        ?: rootProject.file("local.properties").takeIf { it.exists() }
            ?.let { propsFile ->
                Properties().apply { propsFile.inputStream().use(::load) }
            }?.getProperty(propertyName)?.takeIf { it.isNotBlank() }
        ?: throw GradleException(
            "Missing secret '$propertyName' (env: $envName). " +
                "Add it to local.properties — see local.properties.example — or set the $envName environment variable.",
        )

/**
 * Optional variant: missing/blank resolves to "" instead of failing (used for
 * the RevenueCat key — blank means the SDK stays unconfigured, which is a
 * supported state until real keys are created).
 */
fun resolveSecretOrEmpty(envName: String, propertyName: String): String =
    System.getenv(envName)?.takeIf { it.isNotBlank() }
        ?: rootProject.file("local.properties").takeIf { it.exists() }
            ?.let { propsFile ->
                Properties().apply { propsFile.inputStream().use(::load) }
            }?.getProperty(propertyName)?.takeIf { it.isNotBlank() }
        ?: ""

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":shared"))
    implementation(project(":onboarding"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.core.splashscreen)
    implementation(libs.supabase.kt)
    implementation(libs.supabase.auth.kt)
    implementation(libs.supabase.compose.auth)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.datetime)
    testImplementation(libs.compose.components.resources)
    testImplementation(project(":home"))
}

android {
    namespace = "app.usefoster"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "app.usefoster"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"

        // Build-time secret injection (hygiene, not secrecy — see Secrets.kt).
        buildConfigField("String", "SUPABASE_URL", "\"${resolveSecret("SUPABASE_URL", "supabase.url")}\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"${resolveSecret("SUPABASE_PUBLISHABLE_KEY", "supabase.publishable_key")}\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${resolveSecret("GOOGLE_WEB_CLIENT_ID", "google.web.client.id")}\"")
        buildConfigField("String", "REVENUECAT_ANDROID_KEY", "\"${resolveSecretOrEmpty("REVENUECAT_ANDROID_KEY", "revenuecat.android.key")}\"")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}