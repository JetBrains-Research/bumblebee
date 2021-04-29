group = rootProject.group
version = rootProject.version

dependencies {
    implementation("com.github.gumtreediff", "core", "2.1.2")
    implementation("org.apache.commons:commons-lang3:3.12.0")
}

tasks {
    jar {
        from(sourceSets["main"].allSource)
        archiveClassifier.set("sources")
    }
}
