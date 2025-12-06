import java.io.ByteArrayOutputStream

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

subprojects {
    apply(plugin = "maven-publish")
    apply(plugin = "java-library")

    group = "moe.kyokobot.koe"

    if (project.hasProperty("version")) {
        // Used by JitPack
        version = project.version
    } else {
        // Get version from git
        val gitVersion = getGitVersion()
        version = gitVersion.version
    }

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        
        withSourcesJar()
        withJavadocJar()
    }

    repositories {
        mavenLocal()

        mavenCentral()
        maven {
            url = uri("https://maven.lavalink.dev/releases")
        }
        maven {
            url = uri("https://jitpack.io/")
        }
    }

    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
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
