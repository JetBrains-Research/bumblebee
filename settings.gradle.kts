rootProject.name = "ast-transformations"

include(
    "ast-transformations-core",
    "ast-transformations-plugin"
)

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
