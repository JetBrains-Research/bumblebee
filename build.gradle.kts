import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.4.10"
    id("org.jetbrains.intellij") version "0.5.0"
    id("org.jetbrains.changelog") version "0.6.1"
    id("io.gitlab.arturbosch.detekt") version "1.14.1"
    id("org.jlleitschuh.gradle.ktlint") version "9.4.1"
    id("org.jetbrains.dokka") version "0.11.0-dev-59"
}

group = "io.github.nbirillo.ast.transformations"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.0.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.2")
}

intellij {
    version = "2020.2"
}
configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.getByName<org.jetbrains.intellij.tasks.PatchPluginXmlTask>("patchPluginXml") {
    changeNotes("""
      Add change notes here.<br>
      <em>most HTML tags may be used</em>""")
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}