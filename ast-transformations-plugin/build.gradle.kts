group = rootProject.group
version = rootProject.version

dependencies {
    implementation(project(":ast-transformations-core"))
    // Need for CLI
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.13.3")
    implementation("com.xenomachina:kotlin-argparser:2.0.7")
}

tasks {
    runIde {
        val input: String? by project
        val output: String? by project
        args = listOfNotNull(
            "python-transformations",
            input?.let { "--input_path=$it" },
            output?.let { "--output_path=$it" }
        )
        jvmArgs = listOf("-Djava.awt.headless=true", "--add-exports", "java.base/jdk.internal.vm=ALL-UNNAMED")
        standardInput = System.`in`
        standardOutput = System.`out`
    }

    register("cli") {
        dependsOn("runIde")
    }
}
