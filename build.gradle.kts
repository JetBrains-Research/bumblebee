import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    val kotlinVersion = "1.4.30"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    id("org.jetbrains.intellij") version "0.7.2"
    id("com.github.johnrengelman.shadow") version "5.1.0"
    id("org.jetbrains.dokka") version "0.10.1"
    id("org.jlleitschuh.gradle.ktlint") version "9.4.1"
}

allprojects {
    repositories {
        mavenCentral()
        jcenter()
    }
    apply {
        plugin("org.jlleitschuh.gradle.ktlint")
    }

    ktlint {
        enableExperimentalRules.set(true)
    }
}

subprojects {
    group = "org.jetbrains.research.ml.ast.transformations"
    version = "1.0-SNAPSHOT"
    apply {
        plugin("java")
        plugin("kotlin")
        plugin("org.jetbrains.intellij")
        plugin("com.github.johnrengelman.shadow")
        plugin("org.jetbrains.dokka")
        plugin("org.jetbrains.kotlin.plugin.serialization")
    }

    intellij {
        type = "PC"
        version = "2020.3.3"
        downloadSources = false
        setPlugins("PythonCore")
        updateSinceUntilBuild = true
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

    dependencies {
        implementation(kotlin("stdlib-jdk8"))
        implementation("com.github.gumtreediff", "core", "2.1.2")
        implementation("org.apache.commons:commons-lang3:3.12.0")
    }
}
