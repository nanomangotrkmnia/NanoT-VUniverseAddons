plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.3"

tasks.register<Copy>("collectJars") {
    group = "build"
    description = "Builds all Minecraft versions and copies the jars to build/libs."

    dependsOn(":1.21.11:build", ":26.1.2:build", ":26.3:build")

    from("versions/1.21.11/build/libs")
    from("versions/26.1.2/build/libs")
    from("versions/26.3/build/libs")
    include("*.jar")
    exclude("*-sources.jar")

    into("build/libs")

    doFirst {
        delete(layout.buildDirectory.dir("libs"))
    }
}
