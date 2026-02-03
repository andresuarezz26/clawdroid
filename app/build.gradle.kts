import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
  id("com.google.devtools.ksp")
  id("com.google.dagger.hilt.android")
  id("kotlin-parcelize")
}

val localProperties = Properties().apply {
  val localPropertiesFile = rootProject.file("local.properties")
  if (localPropertiesFile.exists()) {
    load(localPropertiesFile.inputStream())
  }
}

android {
  namespace = "com.aiassistant"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.aiassistant"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    buildConfigField("String", "OPENAI_API_KEY", "\"${localProperties.getProperty("OPENAI_API_KEY", "")}\"")
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  kotlinOptions {
    jvmTarget = "11"
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  packaging {
    resources {
      excludes += listOf(
        "META-INF/INDEX.LIST",
        "META-INF/io.netty.versions.properties",
        "META-INF/DEPENDENCIES",
        "META-INF/LICENSE",
        "META-INF/LICENSE.txt",
        "META-INF/license.txt",
        "META-INF/NOTICE",
        "META-INF/NOTICE.txt",
        "META-INF/notice.txt",
        "META-INF/ASL2.0",
        "META-INF/*.kotlin_module"
      )
    }
  }
}

dependencies {

  //region Presentation

  // Compose
  implementation(libs.androidx.navigation.compose)
  androidTestImplementation(libs.androidx.compose.ui.test.junit)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))

  // Coil
  implementation(libs.coil.compose)
  implementation(libs.coil.network)

  // Serialization
  implementation(libs.kotlinx.serialization.json)

  // Hilt
  implementation(libs.dagger.hilt.android)
  ksp(libs.dagger.hilt.android.compiler)
  implementation(libs.hilt.navigation.compose)

  // Coroutines
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

  // Mockito
  testImplementation(libs.mockito.core)
  testImplementation(libs.mockito.kotlin)
  androidTestImplementation(libs.mockito.android)

  // Android ktx
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)

  // Android UI
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.ui.tooling.preview)
  implementation(libs.androidx.material3)

  //endregion

  //region Data

  // Ktor
  implementation(libs.ktor.client.core)
  implementation(libs.ktor.client.cio)
  implementation(libs.ktor.content.negotiation)
  implementation(libs.ktor.serialization)

  // Room
  implementation(libs.room.runtime)
  implementation(libs.room.ktx)
  ksp(libs.room.compiler)
  testImplementation(libs.room.testing)

  // Koog AI
  implementation(libs.koog.agents)

  // Security
  implementation(libs.androidx.security.crypto)

  //endregion

  //region Test

  // General Test
  androidTestImplementation(platform(libs.androidx.compose.bom))
  testImplementation(libs.androidx.core.testing)
  testImplementation(kotlin("test"))
  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.ui.test.junit4)
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")

  debugImplementation(libs.androidx.ui.tooling)
  debugImplementation(libs.androidx.ui.test.manifest)

  //endregion
}
