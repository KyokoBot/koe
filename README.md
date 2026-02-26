# koe

Tiny, minimal dependency and embeddable library implementing Discord media server protocols, built on [Netty](https://netty.io), aiming for high performance and reduced GC usage.

### Versioning and stability policy

Koe follows [semantic versioning](https://semver.org/). API/ABI stability is defined as follows:

- 🟢 **Public API** (all packages except `.experimental` and `.internal`) is guaranteed to be stable and is only extended when needed. Minor and patch releases preserve binary compatibility for the public API.
- 🚧 **Experimental API** (`moe.kyokobot.koe.experimental`) - APIs in this package are subject to change or removal in any **minor** release (e.g. 3.1, 3.2). Use only if you can tolerate breaking changes between minor versions.
- 🔒 **Internal API** (`moe.kyokobot.koe.internal`) - For internal use only. Not part of the public API; may change or break in any release, including **patch** releases.

Example:

```groovy
repositories {
    maven { url 'https://maven.lavalink.dev/releases' }
//    maven { url 'https://maven.lavalink.dev/snapshots' }
}

// ...

dependencies {
    implementation 'moe.kyokobot.koe:core:VERSION'
}
```

`VERSION` can be either a tag or a git commit hash (first 9 characters).

### Dependencies
- Netty
- slf4j
- Java 11+

### Features

- Supports voice gateway v4, v5 and v8.
- Easily extendable for stuff such as support for codecs other than Opus or video sending, if Discord ever decides to support it on bots.
- Experimental video support.
- Basic RTCP support for measuring packet loss and other stuff.

### Non-goals / won't do

- Encoding - Koe only implements voice server communication, not voice handling itself, so it only accepts Opus frames and you have set up an encoder yourself, use [lavaplayer](https://github.com/sedmelluq/lavaplayer), libav/ffmpeg or anything else.
- Voice receiving support - [it's not supported by Discord anyway](https://github.com/discordapp/discord-api-docs/issues/808#issuecomment-458863743), although someone could implement it by registering hooks.

### Extensions

- [UDP-Queue](https://github.com/KyokoBot/koe/tree/master/ext-udpqueue)

### Credits

[Lavalink team](https://github.com/lavalink-devs) for being the main consumer of Koe and providing most feedback and improvements.
[@TheAkio](https://github.com/TheAkio) for name idea.

Koe includes modified/stripped-down parts based on following open-source projects:

- [tweetnacl-java](https://github.com/InstantWebP2P/tweetnacl-java) (Poly1305, SecretBox)
- [nanojson](https://github.com/mmastrac/nanojson) (modified for bytebuf support, changed the API a bit and etc.)
