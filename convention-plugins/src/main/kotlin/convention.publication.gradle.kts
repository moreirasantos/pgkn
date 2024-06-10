import java.util.*

plugins {
    `maven-publish`
    signing
}

// Stub secrets to let the project sync and build without the publication values set up
ext["signing.keyId"] = null
ext["signing.password"] = null
ext["signing.secretKeyRingFile"] = null
ext["ossrhUsername"] = null
ext["ossrhPassword"] = null

// Grabbing secrets from local.properties file or from environment variables, which could be used on CI
val secretPropsFile = project.rootProject.file("local.properties")
if (secretPropsFile.exists()) {
    secretPropsFile.reader().use {
        Properties().apply {
            load(it)
        }
    }.onEach { (name, value) ->
        ext[name.toString()] = value
    }
    ext["___type"] = "file"
} else {
    println("Skipping local.properties")
    ext["signing.keyId"] = System.getenv("SIGNING_KEY_ID")
    ext["signing.password"] = System.getenv("SIGNING_PASSWORD")
    ext["signing.secretKeyRingFile"] = System.getenv("SIGNING_SECRET_KEY_RING_FILE")
    ext["ossrhUsername"] = System.getenv("OSSRH_USERNAME")
    ext["ossrhPassword"] = System.getenv("OSSRH_PASSWORD")
    ext["___type"] = "env"
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

fun getExtraString(name: String) = ext[name]?.toString()

publishing {
    // Configure maven central repository
    repositories {
        maven {
            name = "sonatype"
            setUrl("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = getExtraString("ossrhUsername")
                password = getExtraString("ossrhPassword")
            }
        }
    }

    // Configure all publications
    publications.withType<MavenPublication> {
        // Stub javadoc.jar artifact
        artifact(javadocJar.get())

        // Provide artifacts information requited by Maven Central
        pom {
            name.set("PostgreSQL Kotlin Native Driver")
            description.set("PostgreSQL Driver implementation for Kotlin Native")
            url.set("https://github.com/moreirasantos/pgkn")

            licenses {
                license {
                    name.set("MIT")
                    url.set("https://opensource.org/licenses/MIT")
                }
            }
            developers {
                developer {
                    id.set("moreirasantos")
                    name.set("Miguel")
                    email.set("Santos")
                }
            }
            scm {
                url.set("https://github.com/moreirasantos/pgkn")
            }
        }
    }
}

// Signing artifacts. Signing.* extra properties values will be used
signing {
    println("signing....")
    println(ext["___type"])
    println(ext["signing.secretKeyRingFile"])
    sign(publishing.publications)
}

tasks{
    val publishKotlinMultiplatformPublicationToSonatypeRepository by getting {
        // Explicit dependency because gradle says it's implicit and fails build
        dependsOn("signLinuxX64Publication")
    }
}
