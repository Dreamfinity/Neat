repositories {
    gradlePluginPortal()
    mavenCentral()

    exclusiveContent {
        forRepository {
            maven(url = "https://maven.minecraftforge.net/") {
                name = "forge"
            }
        }
        filter {
            includeGroupByRegex("net.minecraftforge.*")
            includeGroupByRegex("de.oceanlabs.*")
        }
    }
}

plugins {
    `kotlin-dsl`
    `maven-publish`
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    jvmToolchain(8)
}

dependencies {
    implementation("com.anatawa12.forge:ForgeGradle:1.2-1.1.+")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.20")
}