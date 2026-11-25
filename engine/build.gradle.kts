// Headless engine: PR list via gh, preparation queue, worker launcher, budget, sync.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(21)
}

// The bundle schemas live at the repo root so any tool can read them; the engine ships them on its classpath.
sourceSets.main {
    resources.srcDir(rootProject.layout.projectDirectory.dir("schema"))
}

dependencies {
    implementation(project(":core-model"))
    implementation(libs.json.schema.validator)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

// scripts/validate runs the validator straight on the JVM, so it gets clean output and a real exit code.
val validatorClasspath by tasks.registering {
    val classpath = sourceSets.main.get().runtimeClasspath
    val output = layout.buildDirectory.file("validator.classpath")
    inputs.files(classpath)
    outputs.file(output)
    doLast { output.get().asFile.writeText(classpath.files.joinToString(File.pathSeparator)) }
}
