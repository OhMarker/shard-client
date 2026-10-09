import java.security.MessageDigest

plugins {
    id("fabric-loom") version "1.18.2"
    `maven-publish`
}

fun prop(name: String): String = project.property(name).toString()

val modVersion = prop("modVersion")
val minecraftVersion = prop("minecraftVersion")
val loaderVersion = prop("loaderVersion")
val fabricApiVersion = prop("fabricApiVersion")
val modmenuVersion = prop("modmenuVersion")
val sodiumVersion = prop("sodiumVersion")
val irisVersion = prop("irisVersion")
val lithiumVersion = prop("lithiumVersion")
val clothConfigVersion = prop("clothConfigVersion")

version = modVersion
group = prop("mavenGroup")

base {
    archivesName.set("shard")
}

repositories {
    maven("https://maven.terraformersmc.com/releases/") { name = "TerraformersMC" }
    maven("https://maven.shedaniel.me/") { name = "Shedaniel" }
    maven("https://api.modrinth.com/maven") {
        name = "Modrinth"
        content { includeGroup("maven.modrinth") }
    }
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")

    // Mod Menu: compile against it for the config-screen entrypoint.
    modImplementation("com.terraformersmc:modmenu:$modmenuVersion")

    // The launcher's bundled set, present in the dev client so mixin conflicts show up early.
    modRuntimeOnly("maven.modrinth:sodium:$sodiumVersion")
    modRuntimeOnly("maven.modrinth:iris:$irisVersion")
    modRuntimeOnly("maven.modrinth:lithium:$lithiumVersion")
    modRuntimeOnly("me.shedaniel.cloth:cloth-config-fabric:$clothConfigVersion") {
        exclude(group = "net.fabricmc.fabric-api")
    }

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

loom {
    runs {
        named("client") {
            client()
            configName = "Shard Client"
            ideConfigGenerated(true)
            runDir = "run"
            // -PwindowSize=1920x1080 changes the dev window (the smoke test uses 1280x720 by default).
            val size = (project.findProperty("windowSize")?.toString() ?: "1280x720").split("x")
            programArgs("--width", size[0], "--height", size[1])
            // Fixed dev username so the offline smoke server can op it (see .smoke-server/ops.json).
            programArgs("--username", "ShardSmoke")
            // Verification hooks: -PquickPlay=host:port joins a server on start;
            // -PsmokeDir=<dir> makes the dev-only SmokeTest screenshot the HUD/GUI and quit.
            if (project.hasProperty("quickPlay")) {
                val target = project.property("quickPlay").toString()
                programArgs("--quickPlayMultiplayer", target)
                // Belt and braces: the dev-only SmokeTest also connects itself from the title screen.
                vmArgs("-Dshard.smoke.server=$target")
            }
            if (project.hasProperty("smokeDir")) {
                vmArgs("-Dshard.smoke.dir=" + project.property("smokeDir").toString())
            }
            // -PsmokeOnly=cosmetics runs only the cape pass of the smoke test.
            if (project.hasProperty("smokeOnly")) {
                vmArgs("-Dshard.smoke.only=" + project.property("smokeOnly").toString())
            }
            // -PequippedPath=<equipped.json> stands in for launcher-info.json's equippedPath in dev runs.
            if (project.hasProperty("equippedPath")) {
                vmArgs("-Dshard.dev.equippedPath=" + project.property("equippedPath").toString())
            }
            // -PapiBase=<url> uses a local Shard API (`npm run dev` in shard-api) with dev sign-in.
            if (project.hasProperty("apiBase")) {
                vmArgs("-Dshard.dev.apiBase=" + project.property("apiBase").toString())
            }
            // -PfakeBridge serves a stand-in for Shard Launcher's account bridge (three dev accounts).
            if (project.hasProperty("fakeBridge")) {
                vmArgs("-Dshard.dev.fakeBridge=1")
            }
            // -PmetaBase=<url> serves the shared cape list from somewhere else (smoke test: a local folder).
            if (project.hasProperty("metaBase")) {
                vmArgs("-Dshard.dev.metaBase=" + project.property("metaBase").toString())
            }
            // -PcatalogueUrl=<url or file:///...> reads the cosmetics catalogue from there (e.g. an unpushed cosmetics-v2.json).
            if (project.hasProperty("catalogueUrl")) {
                vmArgs("-Dshard.dev.catalogueUrl=" + project.property("catalogueUrl").toString())
            }
            // -PsmokeBench runs the BENCHMARKS.md scenario instead of the screenshot pass.
            if (project.hasProperty("smokeBench")) {
                vmArgs("-Dshard.smoke.bench=1")
            }
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minecraft_version", minecraftVersion)
    inputs.property("loader_version", loaderVersion)
    filesMatching("fabric.mod.json") {
        expand(
            mapOf(
                "version" to project.version,
                "minecraft_version" to minecraftVersion,
                "loader_version" to loaderVersion
            )
        )
    }
}

tasks.jar {
    from("LICENSE") { rename { "${it}_shard" } }
}

tasks.test {
    useJUnitPlatform()
    testLogging { events("passed", "failed", "skipped") }
}

// sha512 of the remapped jar, for shard-manifest.json (see CONTRACT.md in the launcher repo).
val sha512 = tasks.register("sha512") {
    dependsOn(tasks.remapJar)
    val jar = tasks.remapJar.flatMap { it.archiveFile }
    val out = layout.buildDirectory.file("libs/shard-$modVersion.jar.sha512")
    inputs.file(jar)
    outputs.file(out)
    doLast {
        val digest = MessageDigest.getInstance("SHA-512")
        val bytes = jar.get().asFile.readBytes()
        val hex = digest.digest(bytes).joinToString("") { b -> "%02x".format(b) }
        out.get().asFile.writeText("$hex  ${jar.get().asFile.name}\n")
        println("sha512 $hex")
    }
}
tasks.build { dependsOn(sha512) }
