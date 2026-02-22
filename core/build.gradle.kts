dependencies {
    api(libs.netty.transport)
    implementation(libs.netty.codec.http)
    implementation(libs.netty.transport.native.epoll.linux) {
        artifact {
            classifier = "linux-x86_64"
        }
    }

    implementation(libs.slf4j.api)
    implementation(libs.tink)
    implementation(libs.libdave.api)
    implementation(libs.libdave.impl.jni)
    implementation(libs.jetbrains.annotations)
}

mavenPublishing {
    pom {
        name = "core"
    }
}
