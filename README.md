# koe

Tiny, minimal dependency and embeddable Discord voice library for JVM based on [Netty](https://netty.io), aiming for high performance and reduced GC usage.

[Get it on JitPack](https://jitpack.io/#moe.kyokobot/koe)

#### Dependencies
- Netty
- slf4j
- Java 11+ (could be backported to Java 8 with minor code changes)

#### Non-goals / won't do

- Support for sending PCM data - Koe only accepts Opus frames to keep things simple, set up an encoder yourself or 
use [lavaplayer](https://github.com/sedmelluq/lavaplayer).
- Voice receiving support - [it's not supported by Discord anyway](https://github.com/discordapp/discord-api-docs/issues/808#issuecomment-458863743).
- JDA-NAS equivalent - Koe itself is made to avoid GCs as much as possible - if your bot is big enough and you are 
keeping everything in single process - consider using an external audio node such as [hibiki](https://github.com/KyokoBot/hibiki), 
[andesite](https://github.com/natanbc/andesite-node) or [Lavalink](https://github.com/Frederikam/Lavalink).

#### Credits

[@TheAkio](https://github.com/TheAkio) for name idea.

Koe includes modified/stripped-down parts based on following open-source projects:

- [tweetnacl-java](https://github.com/InstantWebP2P/tweetnacl-java) (Poly1305, SecretBox)
- [nanojson](https://github.com/mmastrac/nanojson) (enhanced with Netty ByteBuf support)

#### Projects using Koe

- [Kyoko](https://github.com/KyokoBot/kyoko)
- ...maybe your own?