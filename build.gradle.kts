import org.jreleaser.model.Active
import java.io.ByteArrayOutputStream

plugins {
    id("java")
    id("java-library")
    id("maven-publish")
    id("org.jreleaser") version "1.14.0"
}

group = "com.us.unix.restfull"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("org.apache.logging.log4j:log4j-core:2.24.0")
    implementation("org.apache.logging.log4j:log4j-api:2.24.0")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.24.0")
    implementation("com.fasterxml.jackson.core:jackson-core:2.17.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.test {
    useJUnitPlatform()
}

tasks.register("pushToGithub") {
    val stdout = ByteArrayOutputStream()
    doLast {
        exec {
            commandLine("git", "commit", "-am", "Version $version")
            standardOutput = stdout
        }
        exec {
            commandLine("git", "push", "-u", "origin")
            standardOutput = stdout
        }
        println(stdout)
    }
}

publishing {
    publications {
        create("maven", MavenPublication::class) {
            groupId = project.group.toString()
            artifactId = project.name

            from(components["java"])

            pom {
                name.set(project.name)
                description.set("REST API Automation")
                url.set("https://github.com/mminichino/restfull-java")
                inceptionYear.set("2024")
                licenses {
                    license {
                        name.set("Apache-2.0")
                        url.set("https://spdx.org/licenses/Apache-2.0.html")
                    }
                }
                developers {
                    developer {
                        id.set("mminichino")
                        name.set("Michael Minichino")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/mminichino/restfull-java.git")
                    developerConnection.set("scm:git:ssh://github.com/mminichino/restfull-java.git")
                    url.set("https://github.com/mminichino/restfull-java")
                }
            }
        }
    }

    repositories {
        maven {
            url = layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()
        }
    }
}

jreleaser {
    signing {
        active.set(Active.ALWAYS)
        armored.set(true)
    }
    deploy {
        maven {
            mavenCentral {
                create("sonatype") {
                    active.set(Active.ALWAYS)
                    url.set("https://central.sonatype.com/api/v1/publisher")
                    stagingRepository("build/staging-deploy")
                }
            }
        }
    }
}
