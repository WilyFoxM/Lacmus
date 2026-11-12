plugins {
    id("net.fabricmc.fabric-loom")
}

// The metrics core is a plain Java project; its classes ship inside the mod jar.
evaluationDependsOn(":core")
val coreMain = project(":core").the<SourceSetContainer>()["main"]

fun prop(key: String): String = sc.properties.get<String>(key)

val modId = prop("mod.id")
version = "${prop("mod.version")}+${sc.current.version}"
group = prop("mod.group")
base.archivesName = "$modId-fabric"

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    implementation("net.fabricmc:fabric-loader:${prop("deps.fabric_loader")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${prop("deps.fabric_api")}")
    implementation(project(":core"))
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

loom {
    runConfigs.all {
        // One run directory for every node.
        runDirectory = rootProject.file("run")
    }
}

tasks.processResources {
    val props =
        mapOf(
            "id" to modId,
            "name" to prop("mod.name"),
            "version" to version.toString(),
            "minecraft" to prop("mod.mc_compat"),
            "loader" to prop("deps.fabric_loader"),
        )
    inputs.properties(props)
    filesMatching("fabric.mod.json") { expand(props) }
}

tasks.jar {
    from(coreMain.output)
    from(rootProject.file("LICENSE")) { rename { "${it}_$modId" } }
}

tasks.named<Jar>("sourcesJar") {
    from(coreMain.allSource)
    from(rootProject.file("LICENSE")) { rename { "${it}_$modId" } }
}
