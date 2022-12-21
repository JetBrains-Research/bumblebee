import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "org.jetbrains.research.ml.ast.transformations"
version = "1.0-SNAPSHOT"

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    java
    kotlin("jvm") version "1.7.21" apply true
    id("org.jetbrains.intellij") version "1.10.0" apply true
    id("org.jetbrains.dokka") version "1.7.0" apply true
    id("org.jlleitschuh.gradle.ktlint") version "10.3.0" apply true
}

allprojects {
    apply {
        apply {
            plugin("java")
            plugin("kotlin")
            plugin("org.jetbrains.intellij")
            plugin("org.jetbrains.dokka")
            plugin("org.jlleitschuh.gradle.ktlint")
        }
    }

    intellij {
        pluginName.set(properties("pluginName"))
        version.set(properties("platformVersion"))
        type.set(properties("platformType"))
        plugins.set(properties("platformPlugins").split(',').map(String::trim).filter(String::isNotEmpty))
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        implementation(kotlin("stdlib-jdk8"))
    }

    ktlint {
        enableExperimentalRules.set(true)
    }

    tasks {
        withType<JavaCompile> {
            sourceCompatibility = "17"
            targetCompatibility = "17"
        }
        withType<KotlinCompile> {
            kotlinOptions.jvmTarget = "17"
        }
        // According to this topic:
        // https://intellij-support.jetbrains.com/hc/en-us/community/posts/360010164960-Build-Intellij-plugin-in-IDEA-2019-1-2020-3?page=1#community_comment_360002517940
        withType<org.jetbrains.intellij.tasks.BuildSearchableOptionsTask>()
            .forEach { it.enabled = false }
    }
}
