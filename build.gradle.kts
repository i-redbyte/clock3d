import io.gitlab.arturbosch.detekt.Detekt

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.detekt) apply false
}

val detektFormatting =
    "io.gitlab.arturbosch.detekt:detekt-formatting:${libs.versions.detekt.get()}"

subprojects {
    pluginManager.withPlugin("io.gitlab.arturbosch.detekt") {
        dependencies {
            add("detektPlugins", detektFormatting)
        }
        tasks.withType<Detekt>().configureEach {
            jvmTarget = "17"
            autoCorrect = findProperty("detektAutoCorrect") == "true"
            val mainRoots = listOf("src/main/java", "src/main/kotlin")
                .map { project.layout.projectDirectory.file(it).asFile }
                .filter { it.exists() }
            if (mainRoots.isNotEmpty()) {
                setSource(mainRoots)
            }
        }
    }
}
