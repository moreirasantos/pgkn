import com.bmuschko.gradle.docker.tasks.container.DockerCreateContainer
import com.bmuschko.gradle.docker.tasks.container.DockerRemoveContainer
import com.bmuschko.gradle.docker.tasks.container.DockerStartContainer
import com.bmuschko.gradle.docker.tasks.image.DockerPullImage

plugins {
    val kotlinVersion = "1.8.21"
    kotlin("multiplatform") version kotlinVersion
    id("com.bmuschko.docker-remote-api") version "6.7.0"
}

group = "me.miguel"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}
kotlin {
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.apply {
        binaries {
            executable {
                entryPoint = "main"
            }
        }
        val main by compilations.getting
        val libpq by main.cinterops.creating {
            defFile(project.file("src/nativeInterop/cinterop/libpq.def"))
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
            }
        }
        val commonTest by getting
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
    val nativeTest by getting {
        dependsOn(start)
        finalizedBy(remove)
    }
}

java {
    targetCompatibility = JavaVersion.VERSION_17
}