import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost
import japicmp.model.JApiCompatibilityChangeType
import me.champeau.gradle.japicmp.JapicmpTask
import moe.kyokobot.koe.gradle.AbstractMethodAddedRule

plugins {
    id("com.vanniktech.maven.publish") version "0.32.0" apply false
    // Version is set in buildSrc, which also contains custom rules for it.
    id("me.champeau.gradle.japicmp") apply false
}

val apiBaselineVersion = libs.versions.koe.api.baseline.get()
val gitVersionInfo = getGitVersion("3.0")
logger.lifecycle("Version: ${gitVersionInfo.version} (isCommitHash: ${gitVersionInfo.isCommitHash})")
extra["koeDisplayVersion"] = gitVersionInfo.displayVersion

subprojects {
    apply(plugin = "java-library")

    group = "moe.kyokobot.koe"

    version = gitVersionInfo.version

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(11)
    }

    dependencies {
        "testImplementation"(platform(rootProject.libs.junit.bom))
        "testImplementation"(rootProject.libs.junit.jupiter)
        "testRuntimeOnly"(rootProject.libs.junit.platform.launcher)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
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
        apply(plugin = "me.champeau.gradle.japicmp")

        // Public API must stay compatible with the last release, see "Versioning and stability policy" in README.md.
        val apiBaseline = configurations.create("apiBaseline") { isTransitive = false }
        val apiBaselineClasspath = configurations.create("apiBaselineClasspath")
        dependencies {
            apiBaseline("moe.kyokobot.koe:${project.name}:$apiBaselineVersion")
            apiBaselineClasspath("moe.kyokobot.koe:${project.name}:$apiBaselineVersion")
            if (project.name != "core") {
                apiBaselineClasspath("moe.kyokobot.koe:core:$apiBaselineVersion")
            }
        }

        val apiCompatibilityCheck = tasks.register<JapicmpTask>("apiCompatibilityCheck") {
            group = "verification"
            description = "Checks the public API for breaking changes against Koe $apiBaselineVersion."

            oldArchives.from(apiBaseline)
            oldClasspath.from(apiBaselineClasspath)
            newArchives.from(tasks.named("jar"))
            newClasspath.from(tasks.named("jar"), configurations.named("compileClasspath"))
            packageExcludes.addAll(
                "moe.kyokobot.koe.internal", "moe.kyokobot.koe.internal.*",
                "moe.kyokobot.koe.experimental", "moe.kyokobot.koe.experimental.*",
            )
            // japicmp flags this as breaking because a default can conflict with another interface's default
            // (JLS 13.5.6), but existing implementations had to override the abstract method, so their own wins.
            compatibilityChangeExcludes.add("METHOD_ABSTRACT_NOW_DEFAULT")

            richReport {
                title.set("${project.name}: API changes since $apiBaselineVersion")
                reportName.set("api-compatibility.html")
                addDefaultRules.set(true)
                listOf(
                    JApiCompatibilityChangeType.METHOD_ADDED_TO_INTERFACE,
                    JApiCompatibilityChangeType.METHOD_ABSTRACT_ADDED_TO_CLASS,
                    JApiCompatibilityChangeType.METHOD_ABSTRACT_ADDED_IN_SUPERCLASS,
                    JApiCompatibilityChangeType.METHOD_ABSTRACT_ADDED_IN_IMPLEMENTED_INTERFACE,
                ).forEach { addRule(it, AbstractMethodAddedRule::class.java) }
            }
        }

        tasks.named("check") {
            dependsOn(apiCompatibilityCheck)
        }

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
                    val mavenCentralUsername = findProperty("mavenCentralUsername") as String?
                    val mavenCentralPassword = findProperty("mavenCentralPassword") as String?
                    if (gitVersionInfo.isCommitHash) {
                        logger.lifecycle("Not publishing snapshots to Maven Central")
                    } else if (!mavenCentralUsername.isNullOrEmpty() && !mavenCentralPassword.isNullOrEmpty()) {
                        publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, false)
                        signAllPublications()
                    } else {
                        logger.lifecycle("Not publishing to OSSRH due to missing credentials")
                    }

                    pom {
                        description.set("Koe is a tiny, minimal dependency and embeddable library implementing Discord media server protocols, built on Netty, aiming for high performance and reduced GC usage.")
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

/**
 * @property version The Maven version: the tag for tagged builds, the commit hash otherwise.
 * @property displayVersion The version reported at runtime: the tag for tagged builds, `<major>.x+git<hash>` otherwise.
 */
data class VersionInfo(val version: String, val displayVersion: String, val isCommitHash: Boolean)

fun getGitVersion(fallbackVersion: String): VersionInfo {
    val tagged = providers.exec {
        isIgnoreExitValue = true
        commandLine("git", "describe", "--exact-match", "--tags", "--dirty")
    }
    if (tagged.result.get().exitValue == 0) {
        val tag = tagged.standardOutput.asText.get().trim()
        return VersionInfo(tag, tag, false)
    }

    val commit = providers.exec {
        commandLine("git", "describe", "--match=NeVeRmAtCh", "--always", "--abbrev=9", "--dirty")
    }
    val hash = commit.standardOutput.asText.get().trim()

    return VersionInfo(hash, "${fallbackVersion}+git$hash", true)
}
