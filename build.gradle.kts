plugins {
    java
    kotlin("jvm") version "1.3.72"
    kotlin("plugin.serialization") version "1.3.72"
    `maven-publish`
}

group = "com.expansemc"
version = "0.1.0"

repositories {
    mavenCentral()
    // Spigot
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots")
    // Bungeecord text
    maven("https://oss.sonatype.org/content/groups/public/")
    // Configurate and math
    maven("https://repo.spongepowered.org/maven")
    // Configurate-serialization
    maven("https://jitpack.io")
}

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly(kotlin("reflect"))
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.20.0")

    compileOnly("org.spigotmc:spigot-api:1.13.2-R0.1-SNAPSHOT")

    compileOnly("org.spongepowered:configurate-core:3.6.1")
    compileOnly("org.spongepowered:math:2.0.0-SNAPSHOT")

    compileOnly("com.expansemc:bending-api")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}