plugins {
    id("net.fabricmc.fabric-loom") version "1.17.13" apply false
    id("net.fabricmc.fabric-loom-remap") version "1.17.13" apply false
    id("maven-publish")
}

val targetVersion = sc.current.version
val isModern = targetVersion.startsWith("26")
val javaVer = if (isModern) 25 else 21

if (isModern) {
    apply(plugin = "net.fabricmc.fabric-loom")
} else {
    apply(plugin = "net.fabricmc.fabric-loom-remap")
}

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
    
    if (isModern) {
        implementation("net.fabricmc:fabric-loader:${property("loader_version")}")
        implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")
        
        "implementation"("me.shedaniel.cloth:cloth-config-fabric:${property("cloth_config_version")}") {
            exclude(group = "net.fabricmc.fabric-api")
        }
        "implementation"("com.terraformersmc:modmenu:${property("modmenu_version")}")
    } else {
        @Suppress("UnstableApiUsage")
        "mappings"(loom.officialMojangMappings())
        "modImplementation"("net.fabricmc:fabric-loader:${property("loader_version")}")
        "modImplementation"("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")
        
        "modImplementation"("me.shedaniel.cloth:cloth-config-fabric:${property("cloth_config_version")}") {
            exclude(group = "net.fabricmc.fabric-api")
        }
        "modImplementation"("com.terraformersmc:modmenu:${property("modmenu_version")}")
    }
}

tasks.processResources {
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand(mutableMapOf(
            "version" to project.version,
            "minecraft_version" to project.property("minecraft_version")
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