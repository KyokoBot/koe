dependencies {
    implementation(projects.core)
    implementation(projects.extUdpqueue)
    implementation(libs.jda)
    implementation(libs.lavaplayer)
    implementation(libs.lavaplayer.youtube)
    implementation(libs.logback.classic)

    implementation(libs.libdave.natives.linux)
    implementation(libs.libdave.natives.windows)

    implementation(libs.udpqueue.native.linux)
    implementation(libs.udpqueue.native.win)
}
