dependencies {
    compileOnly(projects.core)
    implementation(libs.lava.common)
    implementation(libs.udpqueue.api)
    implementation(libs.jetbrains.annotations)
}

mavenPublishing {
    pom {
        name = "ext-udpqueue"
    }
}
