import java.util.Properties

plugins {
    id("dev.kikugie.loom-back-compat")
}

val versionName = stonecutter.current.version
val versionProps = Properties()
rootProject.file("versions/$versionName/gradle.properties").inputStream().use { versionProps.load(it) }
val unobfuscated = stonecutter.eval(versionName, ">=26.1")
val javaVersion = versionProps.getProperty("java").toInt()

base {
    archivesName = rootProject.property("archives_base_name") as String
    version = "${rootProject.property("mod_version")}+$versionName"
    group = rootProject.property("maven_group") as String
}

repositories {
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:${versionProps.getProperty("minecraft")}")

    if (!unobfuscated) {
        mappings("net.fabricmc:yarn:${versionProps.getProperty("yarn")}:v2")
    }

    modImplementation("net.fabricmc:fabric-loader:${versionProps.getProperty("loader")}")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release = javaVersion
}

tasks.processResources {
    val props = mapOf(
        "version" to project.version,
        "mc_version" to versionProps.getProperty("minecraft"),
        "java_version" to javaVersion
    )

    inputs.properties(props)
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(props)
    }
}
