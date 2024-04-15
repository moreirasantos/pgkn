import com.bmuschko.gradle.docker.tasks.container.DockerCreateContainer
import com.bmuschko.gradle.docker.tasks.container.DockerRemoveContainer
import com.bmuschko.gradle.docker.tasks.container.DockerStartContainer
import com.bmuschko.gradle.docker.tasks.image.DockerPullImage
import io.gitlab.arturbosch.detekt.Detekt
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    val kotlinVersion = "1.9.23"
    kotlin("multiplatform") version kotlinVersion
    id("com.bmuschko.docker-remote-api") version "9.4.0"
    id("io.gitlab.arturbosch.detekt").version("1.23.6")
    id("convention.publication")
}

group = "io.github.moreirasantos"
version = "1.1.0"

repositories {
    mavenCentral()
}
kotlin {
    // Tiers are in accordance with <https://kotlinlang.org/docs/native-target-support.html>
    // Tier 1
    // TODO find out how to get mac to compile linux64
    when (System.getProperty("os.name")) {
        "Mac OS X" -> macosArm64 {
            val main by compilations.getting
            val libpq by main.cinterops.creating {
                defFile(project.file("src/nativeInterop/cinterop/libpq.def"))
            }
        }

        "Linux" -> linuxX64 {
            val main by compilations.getting
            val libpq by main.cinterops.creating {
                defFile(project.file("src/nativeInterop/cinterop/libpq.def"))
            }
        }
    }
    jvm()

    /*
    // Currently unsupported
    // Tier 2
    linuxArm64("linuxArm64")
    // Tier 3
    mingwX64("mingwX64")

    // Tier 1
    macosX64("macosX64")
    */

    // android, ios, watchos, tvos, js will never(?) be supported
    applyDefaultHierarchyTemplate()
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
                implementation("io.github.oshai:kotlin-logging:6.0.3")
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation("org.springframework.data:spring-data-r2dbc:3.2.5")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.8.0")
                implementation("org.postgresql:r2dbc-postgresql:1.0.4.RELEASE")
                implementation("io.r2dbc:r2dbc-pool:1.0.1.RELEASE")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("org.postgresql:r2dbc-postgresql:1.0.4.RELEASE")
                implementation("org.jetbrains.kotlin:kotlin-test:1.9.23")
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
    this.envVars.set(
        mapOf(
            "POSTGRES_PASSWORD" to "postgres"
        )
    )
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
    // Related to mac linux compile issue
    when (System.getProperty("os.name")) {
        "Mac OS X" -> {
            val macosArm64Test by getting {
                dependsOn(start)
                finalizedBy(remove)
            }
        }

        "Linux" -> {
            val linuxX64Test by getting {
                dependsOn(start)
                finalizedBy(remove)
            }
        }
    }
    val jvmTest by getting {
        dependsOn(start)
        finalizedBy(remove)
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

/*
// TODO: Check if it now works without this workaround
tasks {
    val publishMacosArm64PublicationToSonatypeRepository by getting {
        // Explicit dependency because gradle says it's implicit and fails build
        dependsOn("signKotlinMultiplatformPublication")
    }
}
*/

java {
    targetCompatibility = JavaVersion.VERSION_21
}
