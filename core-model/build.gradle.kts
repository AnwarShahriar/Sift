// Models shared by the engine and every UI, so what the worker writes and what a screen shows can't drift apart.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvmToolchain(21)
    jvm()
}
