import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("net.fabricmc.fabric-loom")
    kotlin("jvm")
}

group = property("maven_group")!!
version = property("mod_version")!!
base { archivesName.set("solyanka") }
repositories {
    mavenCentral()
    maven("https://maven.wispforest.io/releases") { content { includeGroup("io.wispforest") } }
    maven("https://api.modrinth.com/maven") {
        content { includeGroup("maven.modrinth") }
    }
}

loom {
    accessWidenerPath.set(file("src/main/resources/solyanka.accesswidener"))
    splitEnvironmentSourceSets()
    mods {
        create("solyanka") {
            sourceSet(sourceSets.main.get())
            sourceSet(sourceSets["client"])
        }
    }
}

dependencies {
    // Minecraft 26.2 is unobfuscated. Do not add Yarn or legacy mappings here.
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
    implementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")
    implementation("maven.modrinth:owo-lib:${property("owo_version")}")
    // owo injects Endec's MapCarrier into CompoundTag. The runtime library is nested in owo.
    "clientCompileOnly"("io.wispforest:endec:0.1.12") { isTransitive = false }
    "clientCompileOnly"("maven.modrinth:modmenu:${property("modmenu_version")}")
    "clientRuntimeOnly"("maven.modrinth:modmenu:${property("modmenu_version")}")
    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    withSourcesJar()
}
kotlin {
    jvmToolchain(25)
    compilerOptions.jvmTarget.set(JvmTarget.JVM_25)
}
tasks.withType<JavaCompile>().configureEach { options.release.set(25) }
tasks.test { useJUnitPlatform() }
tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") { expand("version" to project.version) }
}
tasks.jar {
    from("NOTICE", "LICENSE")
    from("licenses") { into("licenses") }
}

tasks.named<Jar>("sourcesJar") { from("NOTICE", "LICENSE");from("licenses") { into("licenses") } }
