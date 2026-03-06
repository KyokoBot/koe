dependencies {
    compileOnly(projects.core)
    implementation(libs.lava.common)
    implementation(libs.udpqueue.api)
    implementation(libs.jetbrains.annotations)
}

mavenPublishing {
    pom {
        name = "ext-udpqueue"
        description.set("An extension that provides an implementation of JDA-NAS in Koe, moving packet sending/scheduling logic outside the JVM. This allows audio packets to be sent during GC pauses, provided there's sufficient audio data in the queue. Note that custom codec support is limited, it may add additional latency, and proper Netty usage already helps reduce GC pressure by minimizing allocations.")
    }
}
