plugins {
    id("net.minecraftforge.gradle")
}

fun prop(key: String): String = sc.properties.get<String>(key)

val modId = prop("mod.id")
// The Minecraft part of the version names the releases the jar covers, such as 26.1.x.
version = "${prop("mod.version")}+${prop("mod.mc_label")}"
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
    inputs.property("mixin_level", "JAVA_21")
    val pack = mapOf("pack_major" to prop("mod.pack_major"), "pack_minor" to prop("mod.pack_minor"))
    inputs.properties(pack)
    filesMatching("META-INF/mods.toml") { expand(props) }
    filesMatching("pack.mcmeta") { expand(pack) }
    // Forge ships Mixin 0.8.7, which knows compatibility levels up to JAVA_21.
    filesMatching("lacmus.mixins.json") { expand("mixin_level" to "JAVA_21") }
    // Forge reads the mod's resources only with a pack.mcmeta, in the data pack format of the game.
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
