// Headless engine: PR list via gh, preparation queue, worker launcher, budget, sync.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":core-model"))
}
