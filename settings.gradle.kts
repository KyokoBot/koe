rootProject.name = "koe"

include(":core")
include(":ext-udpqueue")
include(":testbot")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("netty", "4.1.112.Final")
            library("netty-transport", "io.netty", "netty-transport").versionRef("netty")
            library("netty-codec-http", "io.netty", "netty-codec-http").versionRef("netty")
            library("netty-transport-native-epoll-linux", "io.netty", "netty-transport-native-epoll").versionRef("netty")

            library("tink", "com.google.crypto.tink", "tink").version("1.14.1")

            library("jetbrains-annotations", "org.jetbrains", "annotations").version("13.0")

            library("slf4j-api", "org.slf4j", "slf4j-api").version("1.8.0-beta4")
            library("logback-classic", "ch.qos.logback", "logback-classic").version("1.5.18")

            version("lavaplayer", "2.2.6")
            library("lava-common", "dev.arbjerg", "lava-common").versionRef("lavaplayer")
            library("lavaplayer", "dev.arbjerg", "lavaplayer").versionRef("lavaplayer")
            library("lavaplayer-youtube", "com.github.lavalink-devs", "lavaplayer-youtube-source").version("1.18.0")

            library("jda", "net.dv8tion", "JDA").version("5.0.2")

            version("libdave", "6445322dc")
            library("libdave-api", "moe.kyokobot.libdave", "api").versionRef("libdave")
            library("libdave-impl-jni", "moe.kyokobot.libdave", "impl-jni").versionRef("libdave")
            library("libdave-natives-darwin", "moe.kyokobot.libdave", "natives-darwin").versionRef("libdave")
            library("libdave-natives-linux-glibc-aarch64", "moe.kyokobot.libdave", "natives-linux-aarch64").versionRef("libdave")
            library("libdave-natives-linux-glibc-arm", "moe.kyokobot.libdave", "natives-linux-arm").versionRef("libdave")
            library("libdave-natives-linux-glibc-amd64", "moe.kyokobot.libdave", "natives-linux-x86-64").versionRef("libdave")
            library("libdave-natives-linux-glibc-x86", "moe.kyokobot.libdave", "natives-linux-x86").versionRef("libdave")
            library("libdave-natives-linux-musl-aarch64", "moe.kyokobot.libdave", "natives-linux-musl-aarch64").versionRef("libdave")
            library("libdave-natives-linux-musl-arm", "moe.kyokobot.libdave", "natives-linux-musl-arm").versionRef("libdave")
            library("libdave-natives-linux-musl-amd64", "moe.kyokobot.libdave", "natives-linux-musl-x86-64").versionRef("libdave")
            library("libdave-natives-linux-musl-x86", "moe.kyokobot.libdave", "natives-linux-musl-x86").versionRef("libdave")
            library("libdave-natives-win-aarch64", "moe.kyokobot.libdave", "natives-win-aarch64").versionRef("libdave")
            library("libdave-natives-win-amd64", "moe.kyokobot.libdave", "natives-win-x86-64").versionRef("libdave")
            library("libdave-natives-win-x86", "moe.kyokobot.libdave", "natives-win-x86").versionRef("libdave")

            version("udpqueue", "0.2.12")
            library("udpqueue-api", "club.minnced", "udpqueue-api").versionRef("udpqueue")
            library("udpqueue-native-linux-glibc-aarch64", "club.minnced", "udpqueue-native-linux-aarch64").versionRef("udpqueue")
            library("udpqueue-native-linux-glibc-arm", "club.minnced", "udpqueue-native-linux-arm").versionRef("udpqueue")
            library("udpqueue-native-linux-glibc-amd64", "club.minnced", "udpqueue-native-linux-x86-64").versionRef("udpqueue")
            library("udpqueue-native-linux-glibc-x86", "club.minnced", "udpqueue-native-linux-x86").versionRef("udpqueue")
            library("udpqueue-native-linux-musl-aarch64", "club.minnced", "udpqueue-native-linux-musl-aarch64").versionRef("udpqueue")
            library("udpqueue-native-linux-musl-amd64", "club.minnced", "udpqueue-native-linux-musl-x86-64").versionRef("udpqueue")
            library("udpqueue-native-win-aarch64", "club.minnced", "udpqueue-native-win-aarch64").versionRef("udpqueue")
            library("udpqueue-native-win-amd64", "club.minnced", "udpqueue-native-win-x86-64").versionRef("udpqueue")
            library("udpqueue-native-win-x86", "club.minnced", "udpqueue-native-win-x86").versionRef("udpqueue")
            library("udpqueue-native-darwin", "club.minnced", "udpqueue-native-darwin").versionRef("udpqueue")
        }
    }
}
