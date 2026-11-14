pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.neoforged.net/releases") { name = "NeoForged" }
        maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        // Fabric Loom for Minecraft 26.x: official names, no remapping.
        id("net.fabricmc.fabric-loom") version "1.17.21"
        // NeoForge's ModDevGradle.
        id("net.neoforged.moddev") version "2.0.148"
        // Forge's ForgeGradle 7.
        id("net.minecraftforge.gradle") version "7.0.40"
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
}

stonecutter {
    create(rootProject) {
        // Nodes are `{minecraft}-{loader}`; each loader has its own build script. The Fabric build
        // also runs on Quilt, hence `fabric-quilt`.
        fun match(minecraft: String, vararg loaders: String) {
            for (loader in loaders) {
                version("$minecraft-$loader", minecraft).buildscript("build.$loader.gradle.kts")
            }
        }

        match("26.2", "fabric-quilt", "neoforge", "forge")
        vcsVersion = "26.2-fabric-quilt"
    }
}

// Pure-Java metrics core, shared by every node; no Minecraft or loader on its classpath.
include("core")

rootProject.name = "Lacmus"
