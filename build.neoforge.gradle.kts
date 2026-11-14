plugins {
    id("net.neoforged.moddev")
}

fun prop(key: String): String = sc.properties.get<String>(key)

val modId = prop("mod.id")
version = "${prop("mod.version")}+${sc.current.version}"
group = prop("mod.group")
base.archivesName = "$modId-neoforge"

sourceSets.main {
    // The metrics core is plain Java: compiled into the mod, so NeoForge loads it with the mod.
    java.srcDir(rootProject.file("core/src/main/java"))
    // The other loaders' adapters are not part of this node.
    java.exclude("ru/wilyfox/lacmus/fabric/**")
}

neoForge {
    version = prop("deps.neoforge")

    mods {
        register(modId) {
            sourceSet(sourceSets.main.get())
        }
    }

    runs {
        register("client") {
            // One run directory for every node.
            gameDirectory = rootProject.file("run")
            client()
        }
    }
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 25
    options.encoding = "UTF-8"
}

tasks.processResources {
    val props =
        mapOf(
            "id" to modId,
            "name" to prop("mod.name"),
            "version" to version.toString(),
            "minecraft" to prop("mod.mc_compat"),
            "neoforge" to prop("deps.neoforge"),
        )
    inputs.properties(props)
    filesMatching("META-INF/neoforge.mods.toml") { expand(props) }
    exclude("fabric.mod.json")
}

tasks.jar {
    from(rootProject.file("LICENSE")) { rename { "${it}_$modId" } }
}

tasks.named<Jar>("sourcesJar") {
    from(rootProject.file("LICENSE")) { rename { "${it}_$modId" } }
}
