plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.2-fabric-quilt"

stonecutter parameters {
    val (_, loader) = current.project.split('-', limit = 2)

    // Version- and loader-specific values from `stonecutter.properties.toml`.
    properties {
        tags(current.version, loader)
    }

    // `//? if fabric` and the like are reserved for loader seams; most code stays loader-neutral.
    constants["fabric"] = loader == "fabric-quilt"
    constants["neoforge"] = loader == "neoforge"
    constants["forge"] = loader == "forge"
}

// Formatting: google-java-format over every Java source. Spotless is not used, because it
// makes the Stonecutter root buildable and only formats files inside one project's directory.
repositories {
    mavenCentral()
}

val googleJavaFormat: Configuration by configurations.creating {
    // The root has no Java plugin, so ask for the standard JVM runtime variants explicitly.
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named(TargetJvmEnvironment.STANDARD_JVM))
    }
}

dependencies {
    googleJavaFormat("com.google.googlejavaformat:google-java-format:1.31.0")
}

val javaSources = fileTree(rootDir) {
    include("src/**/*.java", "core/src/**/*.java")
}

fun registerFormat(name: String, description: String, vararg mode: String) =
    tasks.register<JavaExec>(name) {
        group = "formatting"
        this.description = description
        classpath = googleJavaFormat
        mainClass = "com.google.googlejavaformat.java.Main"
        jvmArgs(
            listOf("api", "code", "file", "parser", "tree", "util").map {
                "--add-exports=jdk.compiler/com.sun.tools.javac.$it=ALL-UNNAMED"
            },
        )
        inputs.files(javaSources)
        argumentProviders.add(CommandLineArgumentProvider { mode.toList() + javaSources.files.map { it.path } })
    }

registerFormat("formatCheck", "Fails if any Java source is not google-java-format clean.", "--dry-run", "--set-exit-if-changed")
registerFormat("format", "Rewrites Java sources with google-java-format.", "--replace")
