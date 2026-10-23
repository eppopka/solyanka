pluginManagement {
    val loomVersion = providers.gradleProperty("loom_version").get()
    val kotlinVersion = providers.gradleProperty("kotlin_version").get()
    plugins {
        id("net.fabricmc.fabric-loom") version loomVersion
        kotlin("jvm") version kotlinVersion
    }
    repositories {
        maven("https://maven.fabricmc.net/")
        gradlePluginPortal()
        mavenCentral()
    }
}
rootProject.name = "solyanka"
