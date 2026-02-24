plugins {
    id("application")
}

application {
    mainClass = "moe.kyokobot.koe.testbot.KoeTestBotLauncher"
}

dependencies {
    implementation(projects.core)
    implementation(projects.extUdpqueue)
    implementation(libs.jda)
    implementation(libs.lavaplayer)
    implementation(libs.lavaplayer.youtube)
    implementation(libs.logback.classic)

    implementation(libs.libdave.natives.linux)
    implementation(libs.libdave.natives.win)
    implementation(libs.libdave.natives.win.arm64)
    implementation(libs.libdave.natives.darwin)

    implementation(libs.udpqueue.native.linux)
    implementation(libs.udpqueue.native.win)
    implementation(libs.udpqueue.native.win.arm64)
    implementation(libs.udpqueue.native.darwin)
}
