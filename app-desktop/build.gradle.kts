import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvmToolchain(21)
    jvm("desktop")

    sourceSets {
        getByName("desktopMain").dependencies {
            implementation(project(":core-model"))
            implementation(compose.desktop.currentOs)
            implementation(libs.compose.material3)
        }
    }
}

compose.desktop {
    application {
        mainClass = "io.github.anwarshahriar.sift.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg)
            packageName = "Sift"
            packageVersion = "0.1.0"
        }
    }
}
