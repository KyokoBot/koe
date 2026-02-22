import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost
import java.io.ByteArrayOutputStream

plugins {
    id("com.vanniktech.maven.publish") version "0.32.0" apply false
}

val gitVersionInfo = getGitVersion()
logger.lifecycle("Version: ${gitVersionInfo.version} (isCommitHash: ${gitVersionInfo.isCommitHash})")

subprojects {
    apply(plugin = "java-library")

    group = "moe.kyokobot.koe"

    version = gitVersionInfo.version

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    repositories {
        mavenLocal()

        mavenCentral()
        maven {
            url = uri("https://maven.lavalink.dev/releases")
        }
        maven {
            url = uri("https://maven.lavalink.dev/snapshots")
        }
        maven {
            url = uri("https://jitpack.io/")
        }
    }
    if (name != "testbot") {
        apply(plugin = "com.vanniktech.maven.publish")

        afterEvaluate {
            plugins.withId("com.vanniktech.maven.publish.base") {
                configure<PublishingExtension> {
                    val mavenUsername = findProperty("MAVEN_USERNAME") as String?
                    val mavenPassword = findProperty("MAVEN_PASSWORD") as String?
                    if (!mavenUsername.isNullOrEmpty() && !mavenPassword.isNullOrEmpty()) {
                        repositories {
                            val snapshots = "https://maven.lavalink.dev/snapshots"
                            val releases = "https://maven.lavalink.dev/releases"

                            maven(if (gitVersionInfo.isCommitHash) snapshots else releases) {
                                credentials {
                                    username = mavenUsername
                                    password = mavenPassword
                                }
                            }
                        }
                    } else {
                        logger.lifecycle("Not publishing to maven.lavalink.dev because credentials are not set")
                    }
                }

                configure<MavenPublishBaseExtension> {
                    coordinates(group.toString(), project.the<BasePluginExtension>().archivesName.get(), version.toString())
                    val mavenCentralUsername = findProperty("MAVEN_CENTRAL_USERNAME") as String?
                    val mavenCentralPassword = findProperty("MAVEN_CENTRAL_PASSWORD") as String?
                    if (!mavenCentralUsername.isNullOrEmpty() && !mavenCentralPassword.isNullOrEmpty()) {
                        publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, false)
                        if (!gitVersionInfo.isCommitHash) {
                            signAllPublications()
                        }
                    } else {
                        logger.lifecycle("Not publishing to OSSRH due to missing credentials")
                    }

                    pom {
                        url.set("https://github.com/KyokoBot/koe")
                        licenses {
                            license {
                                name.set("The MIT License")
                                url.set("http://www.opensource.org/licenses/mit-license.php")
                            }
                        }
                        developers {
                            developer {
                                id.set("alula")
                                name.set("Alula")
                                email.set("git@alula.me")
                            }
                        }
                        scm {
                            connection.set("scm:git:https://github.com/KyokoBot/koe.git")
                            developerConnection.set("scm:git:ssh://github.com:KyokoBot/koe.git")
                            url.set("https://github.com/KyokoBot/koe")
                        }
                    }
                }
            }
        }
    }
}

data class VersionInfo(val version: String, val isCommitHash: Boolean)

fun getGitVersion(): VersionInfo {
    var versionStr = ByteArrayOutputStream()
    val result = exec {
        standardOutput = versionStr
        errorOutput = versionStr
        isIgnoreExitValue = true
        commandLine("git", "describe", "--exact-match", "--tags")
    }
    if (result.exitValue == 0) {
        return VersionInfo(versionStr.toString().trim(), false)
    }


    versionStr = ByteArrayOutputStream()
    exec {
        standardOutput = versionStr
        errorOutput = versionStr
        commandLine("git", "rev-parse", "--short", "HEAD")
    }

    return VersionInfo(versionStr.toString().trim(), true)
}
