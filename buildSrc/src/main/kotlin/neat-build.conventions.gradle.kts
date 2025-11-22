import java.util.*

repositories {
    maven(url = "https://cloudrep.veritaris.me/repos/") {
        metadataSources {
            mavenPom()
            artifact()
        }
    }
    mavenCentral()
    maven(url = "https://chickenbones.net/maven") {
    }
    flatDir {
        dirs("libs")
    }
}

plugins {
    kotlin("jvm")
    id("java")
    id("forge")
    id("maven-publish")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val forgeVersion: String by project
val buildVersion: String by project
val gameVersion: String by project
val modVersion: String by project
val modGroup: String by project
val modId: String by project
val modName: String by project
val modArchivesName: String by project
val modAuthor: String by project
val modIcon: String by project
val modDescription: String by project
val modCredits: String by project
val isClientBuild: String by project

minecraft {
    version = "$gameVersion-$forgeVersion-$gameVersion"
    runDir = "run"
    replace("{{modVersion}}", modVersion)
    replace("BuildController.internalBuildState()", isClientBuild)
}

tasks {
    runClient {
        if (project.hasProperty("clientRunArgs")) {
            println("clientRunArgs: ${project.property("clientRunArgs")}")
            args(project.property("clientRunArgs"))
        }
    }

    runServer {
        if (project.hasProperty("serverRunArgs")) {
            println("serverRunArgs: ${project.property("serverRunArgs")}")
            args(project.property("serverRunArgs"))
        }
    }

    jar.configure {
        manifest {
            attributes(
                "FMLAT" to "${project.name}_at.cfg",
                "Specification-Version" to "1",
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
                "Implementation-Timestamp" to Date().toInstant().toString(),
                "FMLCorePluginContainsFMLMod" to "true"
            )
        }
        archiveClassifier = if (isClientBuild.toBoolean()) {
            "client"
        } else {
            "universal"
        }
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

    register<Jar>("devJar") {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest.attributes(jar.get().manifest.attributes)

        archiveClassifier.set("dev")
        from(sourceSets.main.get().output)
        exclude("net/minecraft/**")
    }

    register<Jar>("sourcesJar") {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest.attributes(jar.get().manifest.attributes)

        archiveClassifier.set("dev-sources")
        from(sourceSets.main.get().allSource)
        exclude("net/minecraft/**")
    }

    processResources {
        from(sourceSets.main.get().resources) {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
            include("mcmod.info")
            expand(
                "gameVersion" to gameVersion,
                "modVersion" to modVersion,
                "modGroup" to modGroup,
                "modId" to modId,
                "modName" to modName,
                "modArchivesName" to modArchivesName,
                "modAuthor" to modAuthor,
                "modIcon" to modIcon,
                "modDescription" to modDescription,
                "modCredits" to modCredits,
            )
        }
    }

    register<GradleBuild>("buildClient") {
        startParameter.projectProperties["isClientBuild"] = "true"
        tasks.add("build")
    }

    register<GradleBuild>("runProtectedClient") {
        startParameter.projectProperties["isClientBuild"] = "true"
        tasks.add("runClient")
    }

    // Semantic versioning start
    val semanticVersioning = mapOf(
        "Major" to "Builds .jar increasing major number: major.y.z",
        "Minor" to "Builds .jar increasing minor number: x.minor.y",
        "Patch" to "Builds .jar increasing patch number: x.y.patch",
        "JustBuild" to "Builds .jar adding \"-build-N\" suffix and increasing build number: x.y.z-build-N",
    )
    fun makeVersion(bumpType: String): String {
//        val prevVersion = project.version as String
        val prevVersion = modVersion
        val versionSpec = prevVersion.split(".")
        if (versionSpec.size < 3) {
            throw IllegalArgumentException("Bad project version: expected x.y.z or x.z.y-N, got $prevVersion")
        }
        val major = versionSpec.component1()
        val minor = versionSpec.component2()
        val patchAndBuild = versionSpec.component3()
        println("Old version: ${prevVersion}, old build number: $buildVersion")
        val patch = patchAndBuild.split("-")[0]

        val newVersion = when (bumpType.lowercase()) {
            "major" -> "${Integer.parseInt(major) + 1}.0.0"
            "minor" -> "${major}.${Integer.parseInt(minor) + 1}.0"
            "patch" -> "${major}.${minor}.${Integer.parseInt(patch) + 1}"
            else -> "${major}.${minor}.${patch}-build-${Integer.parseInt(buildVersion) + 1}"
        }

        if (bumpType in arrayOf("major", "minor", "patch")) {
            println("Migrating from $prevVersion to $newVersion")
        } else {
            println("Building version $newVersion")
        }
        project.version = newVersion
        return newVersion
    }

    semanticVersioning.keys.forEach { semVerType ->
        register<WriteProperties>(semVerType) {
//            TODO(Veritaris): figure out how to properly store build version and project version for auto semver
//            destinationFile = file(projectBuildPropertiesFile)

            val buildVersion = if (semVerType.lowercase() == "justbuild") {
                "${buildVersion.toInt() + 1}"
            } else {
                "0"
            }

//            projectBuildProperties.replace(
//                "buildVersion",
//                buildVersion
//            )
            version = makeVersion(semVerType)

            this.group = "Semantic versioned"
            this.finalizedBy("build")
        }
    }

    register<GradleBuild>("TestSemVerBuilds") {
        tasks.addAll(arrayOf("JustBuild", "Patch", "Minor", "Major"))
    }
    // Semantic versioning end
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("modPublish") {
                groupId = "org.dreamfinity"
                artifactId = project.name
                version = project.version.toString()

                artifact(tasks.named("devJar")) {
                    classifier = "dev"
                }

                artifact(tasks.named("sourcesJar")) {
                    classifier = "sources"
                }

                pom {
                    developers {
                        developer {
                            id.set("Veritaris")
                            name.set("Veritaris")
                            email.set("georgiiimeshkenov@gmail.com")
                        }
                    }

                    withXml {
                        val dependencies = asNode().appendNode("dependencies")
                        fun appendDependency(dependency: Dependency) {
                            dependencies.appendNode("dependency").apply {
                                appendNode("groupId", dependency.group)
                                appendNode("artifactId", dependency.name)
                                appendNode("version", dependency.version)
                            }
                        }
                    }
                }
            }
        }

        repositories {
            mavenLocal()
        }
    }
}