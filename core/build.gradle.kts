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

mavenPublishing {
    pom {
        name = "core"
    }
}
