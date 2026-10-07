plugins {
    id("application")
}

application {
    mainClass = "moe.kyokobot.koe.testbot.KoeTestBotLauncher"
}

// The archives aren't used, and fail on tagged builds when core-<tag>.jar clashes with another dependency's jar,
// e.g. net.sf.trove4j:core:3.1.0.
tasks.named("distTar") { enabled = false }
tasks.named("distZip") { enabled = false }

dependencies {
    implementation(projects.core)
    implementation(projects.extUdpqueue)
    implementation(libs.jda)
    implementation(libs.lavaplayer)
    implementation(libs.lavaplayer.youtube)
    implementation(libs.logback.classic)

    implementation(libs.libdave.api)
    implementation(libs.libdave.impl.jni)
    runtimeOnly(libs.libdave.impl.ffm) {
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 22)
        }
    }
    implementation(libs.libdave.natives.darwin)
    implementation(libs.libdave.natives.linux.glibc.aarch64)
    implementation(libs.libdave.natives.linux.glibc.arm)
    implementation(libs.libdave.natives.linux.glibc.amd64)
    implementation(libs.libdave.natives.linux.glibc.x86)
    implementation(libs.libdave.natives.linux.musl.aarch64)
    implementation(libs.libdave.natives.linux.musl.arm)
    implementation(libs.libdave.natives.linux.musl.amd64)
    implementation(libs.libdave.natives.linux.musl.x86)
    implementation(libs.libdave.natives.win.aarch64)
    implementation(libs.libdave.natives.win.amd64)
    implementation(libs.libdave.natives.win.x86)

    implementation(libs.udpqueue.native.linux.glibc.aarch64)
    implementation(libs.udpqueue.native.linux.glibc.arm)
    implementation(libs.udpqueue.native.linux.glibc.amd64)
    implementation(libs.udpqueue.native.linux.glibc.x86)
    implementation(libs.udpqueue.native.linux.musl.aarch64)
    implementation(libs.udpqueue.native.linux.musl.amd64)
    implementation(libs.udpqueue.native.win.aarch64)
    implementation(libs.udpqueue.native.win.amd64)
    implementation(libs.udpqueue.native.win.x86)
    implementation(libs.udpqueue.native.darwin)
}
