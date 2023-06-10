plugins {
    val kotlinVersion = "1.8.21"
    kotlin("multiplatform") version kotlinVersion
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

java {
    targetCompatibility = JavaVersion.VERSION_17
}
