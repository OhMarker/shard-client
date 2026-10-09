import java.security.MessageDigest

plugins {
    // Applies fabric-loom-remap (Minecraft < 26.1) or fabric-loom (26.1+, unobfuscated).
    id("dev.kikugie.loom-back-compat")
    `maven-publish`
}

fun prop(name: String): String = project.property(name).toString()

// This build script runs once per Minecraft version in versions/ (Stonecutter nodes).
val mcVersion: String = project.name
val modVersion = prop("modVersion")
val loaderVersion = prop("loaderVersion")
val fabricApiVersion = prop("fabricApiVersion")
val modmenuVersion = prop("modmenuVersion")
val mcCompat = prop("mcCompat")
val javaVersion = if (sc.current.parsed >= "26.1") 25 else 21

version = "$modVersion+$mcVersion"
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
    minecraft("com.mojang:minecraft:$mcVersion")
    loomx.applyMojangMappings()
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")

    // Mod Menu: compile against it for the config-screen entrypoint.
    modImplementation("com.terraformersmc:modmenu:$modmenuVersion")

    // The launcher's bundled set, present in the dev client so mixin conflicts show up early.
    // Versions without these properties run without them.
    for ((slug, key) in listOf("sodium" to "sodiumVersion", "iris" to "irisVersion", "lithium" to "lithiumVersion")) {
        findProperty(key)?.let { modRuntimeOnly("maven.modrinth:$slug:$it") }
    }
    // -PextraMods=<folder>: also run with these mod jars (reproducing a player's mod list in dev).
    if (project.hasProperty("extraMods")) {
        modRuntimeOnly(fileTree(project.property("extraMods").toString()) { include("*.jar") })
    }
    findProperty("clothConfigVersion")?.let {
        modRuntimeOnly("me.shedaniel.cloth:cloth-config-fabric:$it") {
            exclude(group = "net.fabricmc.fabric-api")
        }
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
            // 1.21.11 keeps the original run/ folder; other versions get run-<version>/.
            val runFolder = rootProject.file(if (mcVersion == "1.21.11") "run" else "run-$mcVersion")
            runDir = runFolder.relativeTo(projectDir).invariantSeparatorsPath
            // -PwindowSize=1920x1080 changes the dev window (the smoke test uses 1280x720 by default).
            val size = (project.findProperty("windowSize")?.toString() ?: "1280x720").split("x")
            programArgs("--width", size[0], "--height", size[1])
            // -PcountInjections: injectors that match nothing fail start-up (most Shard injectors are
            // require = 0, so a target that moved between versions would otherwise fail silently).
            // Applies to every mod's mixins, so run it without the dev companions (Sodium trips it).
            if (project.hasProperty("countInjections")) {
                vmArgs("-Dmixin.debug.countInjections=true")
            }
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
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaVersion)
    options.compilerArgs.addAll(listOf("-Xmaxerrs", "5000"))
}

tasks.processResources {
    inputs.property("version", modVersion)
    inputs.property("minecraft_compat", mcCompat)
    inputs.property("loader_version", loaderVersion)
    inputs.property("java_version", javaVersion)
    filesMatching("fabric.mod.json") {
        expand(
            mapOf(
                "version" to modVersion,
                "minecraft_compat" to mcCompat,
                "loader_version" to loaderVersion,
                "java_version" to javaVersion
            )
        )
    }
    // shard.mixins.json may carry "versioned": { "SomeMixin": ">=1.21.9 <26.1" }. Mixins listed
    // there are kept only on matching Minecraft versions; the key itself is removed.
    inputs.property("mc_version", mcVersion)
    filesMatching("shard.mixins.json") {
        filter(VersionedMixins(mcVersion))
    }
}

tasks.jar {
    from(rootProject.file("LICENSE")) { rename { "${it}_shard" } }
}

tasks.test {
    useJUnitPlatform()
    testLogging { events("passed", "failed", "skipped") }
}

// sha512 of the mod jar, for shard-manifest.json (see CONTRACT.md in the launcher repo).
// Jars and their .sha512 files are collected in the root build/libs/<modVersion>/.
val modJar = loomx.modJar
val sha512 = tasks.register("sha512") {
    dependsOn(modJar)
    val jar = modJar.flatMap { it.archiveFile }
    val outDir = rootProject.layout.buildDirectory.dir("libs/$modVersion")
    inputs.file(jar)
    outputs.dir(outDir)
    doLast {
        val src = jar.get().asFile
        val dir = outDir.get().asFile.apply { mkdirs() }
        val copy = dir.resolve(src.name)
        src.copyTo(copy, overwrite = true)
        val digest = MessageDigest.getInstance("SHA-512")
        val hex = digest.digest(copy.readBytes()).joinToString("") { b -> "%02x".format(b) }
        dir.resolve("${src.name}.sha512").writeText("$hex  ${src.name}\n")
        println("sha512 ${src.name} $hex")
    }
}
tasks.build { dependsOn(sha512) }

/** Compares dotted release versions ("1.21.9" < "1.21.10" < "26.1"). */
fun compareMc(a: String, b: String): Int {
    val pa = a.split('.').map { it.toInt() }
    val pb = b.split('.').map { it.toInt() }
    for (i in 0 until maxOf(pa.size, pb.size)) {
        val d = pa.getOrElse(i) { 0 } - pb.getOrElse(i) { 0 }
        if (d != 0) return d
    }
    return 0
}

/** True when the version satisfies every space-separated predicate (">=1.21.9 <26.1", "1.21.11"). */
fun mcMatches(version: String, predicates: String): Boolean = predicates.trim().split(Regex("\\s+")).all { p ->
    val m = Regex("^(>=|<=|>|<|=|!=)?(.+)$").find(p) ?: error("bad predicate $p")
    val c = compareMc(version, m.groupValues[2])
    when (m.groupValues[1]) {
        ">=" -> c >= 0; "<=" -> c <= 0; ">" -> c > 0; "<" -> c < 0; "!=" -> c != 0; else -> c == 0
    }
}

/** Line filter for shard.mixins.json: drops "versioned" mixins that do not apply to [mc]. */
class VersionedMixins(private val mc: String) : Transformer<String?, String> {
    private var buffer = StringBuilder()
    override fun transform(line: String): String? {
        // Collect the whole file (it is small), then emit it once on the closing brace.
        buffer.append(line).append('\n')
        if (line.trimEnd() != "}") return null
        @Suppress("UNCHECKED_CAST")
        val json = groovy.json.JsonSlurper().parseText(buffer.toString()) as MutableMap<String, Any?>
        val versioned = (json.remove("versioned") as Map<String, String>?).orEmpty()
        for (key in listOf("client", "mixins")) {
            val list = (json[key] as List<String>?) ?: continue
            json[key] = list.filter { name -> versioned[name]?.let { mcMatches(mc, it) } ?: true }
        }
        return groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(json))
    }
}
