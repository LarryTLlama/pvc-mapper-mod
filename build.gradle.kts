plugins {
    id("dev.kikugie.loom-back-compat")
    id("maven-publish")
}

val targetVersion = sc.current.version
val javaVer = if (sc.current.parsed >= "26.1") 25 else 21

version = "${property("mod_version")}+mc${targetVersion}"
group = property("maven_group") as String

base {
    archivesName.set(property("archives_base_name") as String)
}

repositories {
    maven("https://maven.shedaniel.me/") { name = "Shedaniel" }
    maven("https://maven.terraformersmc.com/") { name = "TerraformersMC" }
}

val loom = project.extensions.getByName<net.fabricmc.loom.api.LoomGradleExtensionAPI>("loom")

loom.splitEnvironmentSourceSets()
loom.mods.register("pvc-mapper-mod") {
    sourceSet(sourceSets.main.get())
    sourceSet(sourceSets.getByName("client"))
}

dependencies {
    "minecraft"("com.mojang:minecraft:${property("minecraft_version")}")
    loomx.applyMojangMappings()

    "modImplementation"("net.fabricmc:fabric-loader:${property("loader_version")}")
    "modImplementation"("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")

    "modImplementation"("me.shedaniel.cloth:cloth-config-fabric:${property("cloth_config_version")}") {
        exclude(group = "net.fabricmc.fabric-api")
    }
    "modImplementation"("com.terraformersmc:modmenu:${property("modmenu_version")}")
}

tasks.processResources {
    inputs.property("version", project.version)

    val mcDepVersion = project.findProperty("minecraft_version_dependency") as? String ?: "~${project.property("minecraft_version")}"

    filesMatching("fabric.mod.json") {
        expand(mutableMapOf(
            "version" to project.version,
            "minecraft_dependency" to mcDepVersion
        ))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(javaVer)
}

java {
    withSourcesJar()
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.jar {
    inputs.property("archivesName", project.base.archivesName)
    from(rootProject.file("LICENSE")) {
        rename { "${it}_${inputs.properties["archivesName"]}" }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = property("archives_base_name") as String
            from(components["java"])
        }
    }
}