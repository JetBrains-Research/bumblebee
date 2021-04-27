import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "org.jetbrains.research.ml.ast.transformations"
version = "1.0-SNAPSHOT"

plugins {
    java
    kotlin("jvm") version "1.4.30" apply true
    id("org.jetbrains.intellij") version "0.7.2" apply true
    id("org.jetbrains.dokka") version "0.10.1" apply true
    id("org.jlleitschuh.gradle.ktlint") version "9.4.1" apply true
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
        type = "PC"
        version = "2020.3.3"
        downloadSources = false
        setPlugins("PythonCore")
        updateSinceUntilBuild = true
    }

    repositories {
        mavenCentral()
        jcenter()
    }

    dependencies {
        implementation(kotlin("stdlib-jdk8"))
    }

    ktlint {
        enableExperimentalRules.set(true)
    }

    tasks {
        withType<JavaCompile> {
            sourceCompatibility = "11"
            targetCompatibility = "11"
        }
        withType<KotlinCompile> {
            kotlinOptions.jvmTarget = "11"
        }
        // According to this topic:
        // https://intellij-support.jetbrains.com/hc/en-us/community/posts/360010164960-Build-Intellij-plugin-in-IDEA-2019-1-2020-3?page=1#community_comment_360002517940
        withType<org.jetbrains.intellij.tasks.BuildSearchableOptionsTask>()
            .forEach { it.enabled = false }
    }
}
