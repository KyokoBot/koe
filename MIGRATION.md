# Migrating from Koe 2.x to 3.0

## VoiceServerInfo

1. The public constructor has been removed. Use `VoiceServerInfo#builder()` to create instances of this class instead.
2. It's now required to pass `channelId` (the ID of the voice channel), as it is used as a MLS group identifier while using DAVE E2E encryption.
