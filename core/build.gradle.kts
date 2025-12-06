dependencies {
    api("io.netty:netty-transport:4.1.112.Final")
    implementation("io.netty:netty-codec-http:4.1.112.Final")
    implementation("io.netty:netty-transport-native-epoll:4.1.112.Final:linux-x86_64")
    implementation("org.slf4j:slf4j-api:1.8.0-beta4")
    implementation("com.google.crypto.tink:tink:1.14.1")
    compileOnly("org.jetbrains:annotations:13.0")
}
