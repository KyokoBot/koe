# Migrating from Koe 2.x to 3.0

## VoiceServerInfo

1. The public constructor has been removed. Use `VoiceServerInfo#builder()` to create instances of this class instead.
2. It's now required to pass `channelId` (the ID of the voice channel), as it is used as a MLS group identifier while using DAVE E2E encryption.

## Codec API Changes

### Overview

The old `Codec` class has been replaced with two new classes:
- **`CodecInfo`**: Immutable codec capabilities (what a codec CAN do)
- **`CodecInstance`**: Session-specific codec instance with negotiated payload types (what a codec IS doing)

### Codec Class Names

All codec classes have been renamed with an `Info` suffix:

| Old (2.x)            | New (3.0)                |
|----------------------|--------------------------|
| `OpusCodec.INSTANCE` | `OpusCodecInfo.INSTANCE` |
| `H264Codec.INSTANCE` | `H264CodecInfo.INSTANCE` |
| `VP8Codec.INSTANCE`  | `VP8CodecInfo.INSTANCE`  |
| `VP9Codec.INSTANCE`  | `VP9CodecInfo.INSTANCE`  |

## MediaFrameProvider Changes

The `MediaFrameProvider` interface now uses `CodecInstance` instead of `Codec`:

**Old (2.x):**
```java
public interface MediaFrameProvider {
    boolean canSendFrame(Codec codec);
    boolean retrieve(Codec codec, ByteBuf buf, IntReference timestamp);
}
```

**New (3.0):**
```java
public interface MediaFrameProvider {
    boolean canSendFrame(CodecInstance codec);
    boolean retrieve(CodecInstance codec, ByteBuf buf, IntReference timestamp);
}
```
