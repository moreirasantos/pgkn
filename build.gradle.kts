import com.bmuschko.gradle.docker.tasks.container.DockerCreateContainer
import com.bmuschko.gradle.docker.tasks.container.DockerRemoveContainer
import com.bmuschko.gradle.docker.tasks.container.DockerStartContainer
import com.bmuschko.gradle.docker.tasks.image.DockerPullImage
import io.gitlab.arturbosch.detekt.Detekt
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    val kotlinVersion = "1.9.23"
    kotlin("multiplatform") version kotlinVersion
    id("com.bmuschko.docker-remote-api") version "9.4.0"
    id("io.gitlab.arturbosch.detekt").version("1.23.6")
    id("convention.publication")
}

group = "io.github.moreirasantos"
version = "1.2.1"

repositories {
    mavenCentral()
}

val chosenTargets = (properties["targets"] as? String)?.split(",")
    ?: listOf("macosArm64", "macosX64", "linuxArm64", "linuxX64", "jvm")

kotlin {
    // Tiers are in accordance with <https://kotlinlang.org/docs/native-target-support.html>
    fun KotlinNativeTarget.libpq(filename: String) {
        val main by compilations.getting {
            cinterops {
                val libpq by registering {
                    defFile(project.file("src/nativeInterop/cinterop/$filename"))
                }
            }
        }
    }

    val availableTargets = mapOf(
        Pair("macosArm64") { macosArm64 { libpq("libpqArm.def") } },
        Pair("macosX64") { macosX64 { libpq("libpqX.def") } },
        Pair("linuxArm64") { linuxArm64 { libpq("libpqArm.def") } },
        Pair("linuxX64") { linuxX64 { libpq("libpqlinuxX.def") } },
        Pair("jvm") { jvm() },
    )
    chosenTargets.forEach {
        println("Enabling target $it")
        availableTargets[it]?.invoke()
    }

    /*
    // Currently unsupported
    // Tier 3
    mingwX64("mingwX64")
    */

    // Android, ios, watchOS, tvos, js will never(?) be supported.
    applyDefaultHierarchyTemplate()
    sourceSets {
        configureEach {
            languageSettings.progressiveMode = true
        }
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
                implementation("io.github.oshai:kotlin-logging:6.0.3")
            }
        }

        if (chosenTargets.contains("jvm")) {
            val jvmMain by getting {
                dependencies {
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.8.0")
                    implementation("org.postgresql:r2dbc-postgresql:1.0.4.RELEASE")
                    implementation("io.r2dbc:r2dbc-pool:1.0.1.RELEASE")
                }
            }
            val jvmTest by getting {
                dependencies {
                    // https://mvnrepository.com/artifact/org.slf4j/slf4j-api
                    implementation("org.slf4j:slf4j-api:2.0.13")
                    implementation("org.slf4j:slf4j-reload4j:2.0.13")
                    implementation("org.postgresql:r2dbc-postgresql:1.0.4.RELEASE")
                    implementation("org.jetbrains.kotlin:kotlin-test:1.9.23")
                }
            }
        }
    }
}

val pull by tasks.registering(DockerPullImage::class) {
    image.set("postgres:15-alpine")
}

val create by tasks.registering(DockerCreateContainer::class) {
    dependsOn(pull)
    containerName.set("test")
    imageId.set("postgres:15-alpine")
    envVars.set(mapOf("POSTGRES_PASSWORD" to "postgres"))
    hostConfig.portBindings.set(listOf("5678:5432"))

    /*
    Doesn't work
    healthCheck.cmd.set(listOf("pg_isready -U postgres"))
    healthCheck.interval.set(1000000000)
    healthCheck.timeout.set(1000000000)
    healthCheck.retries.set(3)
    healthCheck.startPeriod.set(100000000000000)
    */
}
val start by tasks.registering(DockerStartContainer::class) {
    dependsOn(create)
    containerId.set(create.get().containerId)
    doLast {
        // Sleeping while healthcheck doesn't work
        Thread.sleep(2000)
    }
}

val remove by tasks.registering(DockerRemoveContainer::class) {
    dependsOn(create)
    dependsOn(start)
    containerId.set(create.get().containerId)
    force.set(true)
}

tasks {
    val dependencies: Task.() -> Unit = {
        dependsOn(start)
        finalizedBy(remove)
    }
    chosenTargets.forEach {
        findByName("${it}Test")?.dependencies()
            ?: register("${it}Test")(dependencies)
    }
}

tasks.withType<Test> {
    testLogging {
        events("PASSED", "FAILED", "SKIPPED")
        exceptionFormat = TestExceptionFormat.FULL
        showStandardStreams = true
        showStackTraces = true
    }
}

detekt {
    buildUponDefaultConfig = true // preconfigure defaults
    config.setFrom("$projectDir/config/detekt.yml")
}

tasks.withType<Detekt>().configureEach {
    setSource(files(project.projectDir))
    exclude("**/*.kts")
    exclude("**/build/**")

    reports {
        jvmTarget = "21"
        html.required.set(true) // observe findings in your browser with structure and code snippets
        sarif.required.set(true) // standardized SARIF format (https://sarifweb.azurewebsites.net/)
    }
}


tasks {
    // Explicit dependency because gradle says it's implicit and fails build
    val dependencies: Task.() -> Unit = {
        listOf(
            "signJvmPublication",
            "signLinuxX64Publication",
            "signLinuxArm64Publication",
            "signMacosArm64Publication",
            "signMacosX64Publication",
            "signKotlinMultiplatformPublication"
        ).forEach { dependsOn(it) }
    }
    listOf(
        "publishMacosArm64PublicationToSonatypeRepository",
        "publishMacosX64PublicationToSonatypeRepository",
        "publishJvmPublicationToSonatypeRepository",
        "publishKotlinMultiplatformPublicationToSonatypeRepository",
        "publishLinuxX64PublicationToSonatypeRepository"
    ).forEach { findByName(it)?.dependencies() }

}

java {
    targetCompatibility = JavaVersion.VERSION_21
}
