group = rootProject.group
version = rootProject.version

dependencies {
    implementation(project(":ast-transformations-core"))
    // Need for CLI
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:1.0-M1-1.4.0-rc-218")
    implementation("com.charleskorn.kaml:kaml:0.27.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.4.20")
    implementation("org.reflections:reflections:0.9.12")
    implementation("com.xenomachina:kotlin-argparser:2.0.7")
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
