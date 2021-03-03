import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.4.20"
    id("org.jetbrains.intellij") version "0.7.2"
    id("com.github.johnrengelman.shadow") version "5.1.0"
    id("org.jetbrains.dokka") version "0.10.1"
    id("org.jlleitschuh.gradle.ktlint") version "9.4.1"
    kotlin("plugin.serialization") version "1.4.20"
}

group = "org.jetbrains.research.ml.ast.transformations"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation("com.github.gumtreediff", "core", "2.1.2")
    implementation("org.apache.commons:commons-lang3:3.12.0")

    // Need for CLI
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:1.0-M1-1.4.0-rc-218")
    implementation("com.charleskorn.kaml:kaml:0.27.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.4.20")
    implementation("org.reflections:reflections:0.9.12")
    implementation("com.xenomachina:kotlin-argparser:2.0.7")
}

intellij {
    type = "PC"
    version = "2020.3.3"
    downloadSources = true
    setPlugins("PythonCore")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

ktlint {
    enableExperimentalRules.set(true)
}

tasks {
    runIde {
        val input: String? by project
        val output: String? by project
        val yaml: String? by project
        args = listOfNotNull(
            "python-transformations",
            input?.let { "--input_path=$it" },
            output?.let { "--output_path=$it" },
            yaml?.let { "--yaml_path=$it" }
        )
        jvmArgs = listOf("-Djava.awt.headless=true", "--add-exports", "java.base/jdk.internal.vm=ALL-UNNAMED")
        standardInput = System.`in`
        standardOutput = System.`out`
    }

    register("cli") {
        dependsOn("runIde")
    }
}

// This task does not work on Ubuntu OS: Not found document '/META-INF/tips-pycharm-community.xml'
tasks.withType<org.jetbrains.intellij.tasks.BuildSearchableOptionsTask>()
    .forEach { it.enabled = false }
