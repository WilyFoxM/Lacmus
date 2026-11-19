plugins {
    java
}

// The optional server plugin: reports the server's own tick rate and tick times to Lacmus
// clients. Built against the oldest Paper API it supports; the same jar runs on newer 26.x.
evaluationDependsOn(":core")
val coreMain = project(":core").the<SourceSetContainer>()["main"]

val modVersion: String = file("../stonecutter.properties.toml").readLines()
    .first { it.startsWith("mod.version") }.substringAfter('"').substringBefore('"')

version = modVersion
group = "ru.wilyfox.lacmus"
base.archivesName = "lacmus-server-paper"

repositories {
    maven("https://repo.papermc.io/repository/maven-public/") { name = "PaperMC" }
    mavenCentral()
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.74-stable")
    implementation(project(":core"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 25
    options.encoding = "UTF-8"
}

tasks.processResources {
    inputs.property("version", modVersion)
    filesMatching("plugin.yml") { expand("version" to modVersion) }
}

tasks.jar {
    // The core's classes travel inside the plugin; the server has no other copy.
    from(coreMain.output)
    from(rootProject.file("LICENSE")) { rename { "${it}_lacmus" } }
}
