plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.detekt)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    parallel = true
}

android {
    namespace = "ru.redbyte.clock3d"
    compileSdk = 36

    defaultConfig {
        applicationId = "ru.redbyte.clock3d"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.redbytefx.core)
    implementation(libs.redbytefx.gl)
    implementation(libs.redbytefx.gl.compose)
    implementation(libs.redbytefx.three.d)
    implementation(libs.redbytefx.stdlib)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)

    debugImplementation("androidx.compose.ui:ui-tooling")
}
