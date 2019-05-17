# koe

Tiny and embeddable Discord voice library based on Netty, aiming for high performance and reduced garbage collection.

##### Non-goals

- Support for sending PCM data - Koe only accepts Opus frames to keep things simple, set up an encoder yourself or use [lavaplayer](https://github.com/sedmelluq/lavaplayer).
- Voice receiving support - [it's not supported by Discord anyway](https://github.com/discordapp/discord-api-docs/issues/808#issuecomment-458863743).