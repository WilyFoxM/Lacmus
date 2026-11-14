plugins {
    id("net.minecraftforge.gradle")
}

fun prop(key: String): String = sc.properties.get<String>(key)

val modId = prop("mod.id")
version = "${prop("mod.version")}+${sc.current.version}"
group = prop("mod.group")
base.archivesName = "$modId-forge"

sourceSets.main {
    // The metrics core is plain Java: compiled into the mod, so Forge loads it with the mod.
    java.srcDir(rootProject.file("core/src/main/java"))
    // The other loaders' adapters are not part of this node.
    java.exclude("ru/wilyfox/lacmus/fabric/**", "ru/wilyfox/lacmus/neoforge/**")
}

repositories {
    minecraft.mavenizer(this)
    maven(fg.forgeMaven)
    maven(fg.minecraftLibsMaven)
    mavenCentral()
}

dependencies {
    implementation(minecraft.dependency("net.minecraftforge:forge:${prop("deps.forge")}"))
}

minecraft {
    runs {
        register("client") {
            // One run directory for every node.
            workingDir = rootProject.layout.projectDirectory.dir("run")
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
            "forge" to prop("deps.forge_major"),
        )
    inputs.properties(props)
    filesMatching("META-INF/mods.toml") { expand(props) }
    exclude("fabric.mod.json", "META-INF/neoforge.mods.toml")
}

tasks.jar {
    // Forge finds a mod's mixin configs through the jar manifest.
    manifest { attributes("MixinConfigs" to "$modId.mixins.json") }
    from(rootProject.file("LICENSE")) { rename { "${it}_$modId" } }
}

tasks.named<Jar>("sourcesJar") {
    from(rootProject.file("LICENSE")) { rename { "${it}_$modId" } }
}
