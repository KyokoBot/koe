dependencies {
    compileOnly(projects.core)
    implementation(libs.lava.common)
    implementation(libs.udpqueue.api)
    implementation(libs.jetbrains.annotations)

    testImplementation(projects.core)
    testRuntimeOnly(libs.udpqueue.native.linux.glibc.aarch64)
    testRuntimeOnly(libs.udpqueue.native.linux.glibc.amd64)
    testRuntimeOnly(libs.udpqueue.native.linux.musl.aarch64)
    testRuntimeOnly(libs.udpqueue.native.linux.musl.amd64)
    testRuntimeOnly(libs.udpqueue.native.win.aarch64)
    testRuntimeOnly(libs.udpqueue.native.win.amd64)
    testRuntimeOnly(libs.udpqueue.native.darwin)
}

tasks.named<me.champeau.gradle.japicmp.JapicmpTask>("apiCompatibilityCheck") {
    // Not public API, pollers are created through UdpQueueFramePollerFactory.
    methodExcludes.add(
        "moe.kyokobot.koe.poller.udpqueue.UdpQueueOpusFramePoller#UdpQueueOpusFramePoller(" +
            "moe.kyokobot.koe.poller.udpqueue.QueueManagerPool\$UdpQueueWrapper," +
            "moe.kyokobot.koe.codec.CodecInstance,moe.kyokobot.koe.MediaConnection)"
    )
}

mavenPublishing {
    pom {
        name = "ext-udpqueue"
        description.set("An extension that provides an implementation of JDA-NAS in Koe, moving packet sending/scheduling logic outside the JVM. This allows audio packets to be sent during GC pauses, provided there's sufficient audio data in the queue. Note that custom codec support is limited, it may add additional latency, and proper Netty usage already helps reduce GC pressure by minimizing allocations.")
    }
}
