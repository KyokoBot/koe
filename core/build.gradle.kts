dependencies {
    api(libs.netty.transport)
    implementation(libs.netty.codec.http)
    implementation(libs.netty.transport.native.epoll.linux) {
        artifact {
            classifier = "linux-x86_64"
        }
    }

    implementation(libs.slf4j.api)
    implementation(libs.libdave.api)
    implementation(libs.libdave.impl.jni)
    implementation(libs.jetbrains.annotations)

    testImplementation(libs.bouncycastle)
    testRuntimeOnly(libs.libdave.natives.darwin)
    testRuntimeOnly(libs.libdave.natives.linux.glibc.aarch64)
    testRuntimeOnly(libs.libdave.natives.linux.glibc.amd64)
    testRuntimeOnly(libs.libdave.natives.linux.musl.aarch64)
    testRuntimeOnly(libs.libdave.natives.linux.musl.amd64)
    testRuntimeOnly(libs.libdave.natives.win.aarch64)
    testRuntimeOnly(libs.libdave.natives.win.amd64)
}

val koeDisplayVersion: String by rootProject.extra
val buildConstantsDir = layout.buildDirectory.dir("generated/sources/buildConstants/java/main")

val generateBuildConstants by tasks.registering {
    inputs.property("version", koeDisplayVersion)
    outputs.dir(buildConstantsDir)
    doLast {
        val file = buildConstantsDir.get().file("moe/kyokobot/koe/internal/BuildConstants.java").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            package moe.kyokobot.koe.internal;

            public final class BuildConstants {
                /**
                 * The Koe version, either a release tag such as {@code 3.1.0} or {@code <major>.x+git<hash>} for untagged builds.
                 */
                public static final String VERSION = "$koeDisplayVersion";

                private BuildConstants() {
                }
            }
            """.trimIndent() + "\n"
        )
    }
}

sourceSets.main {
    java.srcDir(generateBuildConstants)
}

mavenPublishing {
    pom {
        name = "core"
    }
}
