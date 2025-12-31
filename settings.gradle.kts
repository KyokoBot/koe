rootProject.name = "koe"

include(":core")
include(":ext-udpqueue")
include(":testbot")

dependencyResolutionManagement {
    versionCatalogs {
        create("netty") {
            version("netty", "4.2.9.Final")
        }
    }
}
