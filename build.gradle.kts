import com.gtnewhorizons.retrofuturagradle.mcp.DeobfuscateTask
import net.darkhax.curseforgegradle.Constants as CFG_Constants
import net.darkhax.curseforgegradle.TaskPublishCurseForge
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import se.bjurr.gitchangelog.plugin.gradle.GitChangelogTask

plugins {
	id("com.gtnewhorizons.retrofuturagradle") version("1.3.+")
	id("eclipse")
	id("idea")
	id("java")
	id("maven-publish")
	id("net.darkhax.curseforgegradle") version("1.0.8")
	id("se.bjurr.gitchangelog.git-changelog-gradle-plugin") version("1.77.2")
}

// gradle.properties
val buildcraftVersion: String by extra
val curseHomepageUrl: String by extra
val curseProjectId: String by extra
val forgeVersion: String by extra
val ic2Version: String by extra
val jeiMinecraftVersion: String by extra
val jeiVersion: String by extra
val mappingsVersion: String by extra
val minecraftVersion: String by extra
val modId: String by extra
val modJavaVersion: String by extra
val specificationVersion: String by extra
val techRebornMinecraftVersion: String by extra
val techRebornVersion: String by extra
val teslaMinecraftVersion: String by extra
val teslaVersion: String by extra

// adds the build number to the end of the version string if on a build server
version = "${specificationVersion}.${getBuildNumber()}"

group = "net.sengir.forestry" // http://maven.apache.org/guides/mini/guide-naming-conventions.html
val baseArchivesName = "${modId}_${minecraftVersion}"
base {
	archivesName.set(baseArchivesName)
}

// java version
java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(modJavaVersion))
	}
}

minecraft {
	mcVersion.set(minecraftVersion)
	mcpMappingChannel.set("stable")
	mcpMappingVersion.set(mappingsVersion)

	injectedTags.set(
		mapOf(
			"@VERSION@" to project.version,
			"@BUILD_NUMBER@" to getBuildNumber()
		)
	)
}

repositories {
	maven("https://maven.blamejared.com")
	maven("https://maven.modmuss50.me")
	maven("https://mod-buildcraft.com/maven")
	maven("https://modmaven.dev")
	maven("https://repo1.maven.org/maven2")
}

dependencies {
	compileOnly(rfg.deobf("net.darkhax.tesla:Tesla-${teslaMinecraftVersion}:${teslaVersion}"))
	compileOnly(rfg.deobf("mezz.jei:jei_${jeiMinecraftVersion}:${jeiVersion}:api"))
	runtimeOnly(rfg.deobf("mezz.jei:jei_${jeiMinecraftVersion}:${jeiVersion}"))
	compileOnly(rfg.deobf("net.industrial-craft:industrialcraft-2:${ic2Version}:api"))
	compileOnly(rfg.deobf("TechReborn:TechReborn-${techRebornMinecraftVersion}:${techRebornVersion}:api"))
	compileOnly(rfg.deobf("com.mod-buildcraft:buildcraft-api:${buildcraftVersion}"))
}

tasks.withType<ProcessResources> {
	// this will ensure that this task is redone when the versions change.
	inputs.property("version", project.version)

	filesMatching(listOf("mcmod.info")) {
		expand(mapOf(
			"version" to version,
			"mcversion" to minecraftVersion
		))
	}

	// Move access transformers to META-INF
	rename("(.+_at\\.cfg)", "META-INF/$1")
}

tasks.jar {
	manifest {
		attributes(mapOf("FMLAT" to "forestry_at.cfg"))
	}
	from(sourceSets.main.get().output)
	from(sourceSets.api.get().output)
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<DeobfuscateTask> {
	accessTransformerFiles.from("${projectDir}/src/main/resources/forestry_at.cfg")
}

val sourcesJarTask = tasks.register<Jar>("sourcesJar") {
	from(sourceSets.main.get().allJava)
	from(sourceSets.api.get().allJava)

	archiveClassifier.set("sources")
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val apiJarTask = tasks.register<Jar>("apiJar") {
	from(sourceSets.api.get().output)

	// Because of this FG bug, I have to include allJava in the api jar.
	// Otherwise, users of the API will not see the documentation for it.
	// https://github.com/MinecraftForge/ForgeGradle/issues/369
	// Gradle is supposed to be able to pull this info from the separate -sources jar.
	from(sourceSets.api.get().allJava)

	archiveClassifier.set("api")
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val javadocJarTask = tasks.register<Jar>("javadocJar") {
	dependsOn(tasks.javadoc)
	from(tasks.javadoc.get().destinationDir)
	archiveClassifier.set("javadoc")
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

artifacts {
	archives(tasks.reobfJar.get())
	archives(javadocJarTask.get())
	archives(sourcesJarTask.get())
	archives(apiJarTask.get())
}

publishing {
	publications {
		register<MavenPublication>("jar") {
			artifactId = baseArchivesName
			artifact(tasks.reobfJar.get())
		}
		register<MavenPublication>("sourcesJar") {
			artifactId = "${baseArchivesName}-sources"
			artifact(sourcesJarTask.get())
		}
		register<MavenPublication>("apiJar") {
			artifactId = "${baseArchivesName}-api"
			artifact(apiJarTask.get())
		}
		register<MavenPublication>("javadocJar") {
			artifactId = "${baseArchivesName}-javadoc"
			artifact(javadocJarTask.get())
		}
	}
	repositories {
		val deployDir = project.findProperty("DEPLOY_DIR")
		if (deployDir != null) {
			maven(deployDir)
		}
	}
}

fun getBuildNumber(): Any {
	// adds the build number to the end of the version string if on a build server
	return project.findProperty("BUILD_NUMBER") ?: "9999"
}

tasks.register<GitChangelogTask>("makeChangelog") {
	fromRepo = projectDir.absolutePath.toString()
	file = file("changelog.html")
	untaggedName = "Current release ${project.version}"
	fromCommit = "5a3850c2642e535656d090d1473054d5fa8d3331"
	toRef = "HEAD"
	templateContent = file("changelog.mustache").readText()
}

tasks.register<TaskPublishCurseForge>("publishCurseForge") {
	dependsOn(tasks.reobfJar)
	dependsOn(":makeChangelog")

	apiToken = project.findProperty("curseforge_apikey") ?: "0"

	val mainFile = upload(curseProjectId, tasks.reobfJar.get().archiveFile)
	mainFile.changelogType = CFG_Constants.CHANGELOG_HTML
	mainFile.changelog = file("changelog.html")
	mainFile.releaseType = CFG_Constants.RELEASE_TYPE_BETA
	mainFile.addJavaVersion("Java $modJavaVersion")
	mainFile.addGameVersion(minecraftVersion)
	mainFile.addModLoader("Forge")

	doLast {
		project.ext.set("curse_file_url", "${curseHomepageUrl}/files/${mainFile.curseFileId}")
	}
}

// IDE Settings
eclipse {
	classpath {
		isDownloadSources = true
		isDownloadJavadoc = true
	}
}

idea {
	module {
		isDownloadJavadoc = true
		isDownloadSources = true
		inheritOutputDirs = true // Fix resources in IJ-Native runs
	}
}

tasks.withType<Javadoc> {
	// workaround cast for https://github.com/gradle/gradle/issues/7038
	val standardJavadocDocletOptions = options as StandardJavadocDocletOptions
	// prevent java 8"s strict doclint for javadocs from failing builds
	standardJavadocDocletOptions.addStringOption("Xdoclint:none", "-quiet")
}
