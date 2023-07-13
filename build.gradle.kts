import com.bmuschko.gradle.docker.tasks.container.DockerCreateContainer
import com.bmuschko.gradle.docker.tasks.container.DockerRemoveContainer
import com.bmuschko.gradle.docker.tasks.container.DockerStartContainer
import com.bmuschko.gradle.docker.tasks.image.DockerPullImage
import io.gitlab.arturbosch.detekt.Detekt

plugins {
    val kotlinVersion = "1.9.0"
    kotlin("multiplatform") version kotlinVersion
    id("com.bmuschko.docker-remote-api") version "6.7.0"
    id("io.gitlab.arturbosch.detekt").version("1.23.0")
    id("maven-publish")
}

group = "me.miguel"
version = "1.0.0"

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

detekt {
    buildUponDefaultConfig = true // preconfigure defaults
    config.setFrom("$projectDir/config/detekt.yml")
}

tasks.withType<Detekt>().configureEach {
    setSource(files(project.projectDir))
    exclude("**/*.kts")
    exclude("**/build/**")

    reports {
        jvmTarget = "17"
        html.required.set(true) // observe findings in your browser with structure and code snippets
        sarif.required.set(true) // standardized SARIF format (https://sarifweb.azurewebsites.net/)
    }
}

java {
    targetCompatibility = JavaVersion.VERSION_17
}
