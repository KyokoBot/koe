# Migrating from Koe 2.x to 3.0

## VoiceServerInfo

1. The public constructor has been removed. Use `VoiceServerInfo#builder()` to create instances of this class instead.
2. As DAVE E2E encryption is now mandatory, it's required to pass `channelId` (the ID of the voice channel), because it's needed as the MLS group identifier.

## Minor changes and removals

- Old `VIDEO_SINK_WANTS` alias for `MEDIA_SINK_WANTS` has been removed.
- Compatibility constructors from `KoeOptions` have been removed. Use `KoeOptions#builder()` instead.

## Experimental package

The package `moe.kyokobot.koe.experimental` is reserved for APIs that are not yet stable. Types and members in this package may change or be removed 
in any **minor** release (e.g. 3.1, 3.2). Do not depend on them if you need binary compatibility across minor versions. The rest of the public API 
follows the [binary compatibility policy](README.md#binary-compatibility) described in the README.

The unfinished video support has been moved to the experimental package in intent to be finished in future releases.

## KoeEventListener

1. The old `userConnected` method has been renamed to `userStreamsChanged` to reflect the fact that it's not fired when a user joins the voice channel, but when their stream configuration changes (e.g., starts/stops video).
2. A new `usersConnected` method has been added if you actually need to know when an user has joined the voice channel. 

## Refactoring of the poller / provider API

Frame poller classes moved from `moe.kyokobot.koe.codec` to `moe.kyokobot.koe.poller`.
Transport implementations live under subpackages such as `moe.kyokobot.koe.poller.netty`
and `moe.kyokobot.koe.poller.udpqueue`.

### Class Changes

- `OpusFramePoller` has been replaced by `AbstractOpusFramePoller`.
- Netty and udpqueue opus pollers are now transport adapters on top of `AbstractOpusFramePoller`.
- `FramePollerFactory` moved to `moe.kyokobot.koe.poller.FramePollerFactory`.

### Provider API and Hot-Swap Behavior

- Opus pollers resolve providers from `MediaConnection` on each poll iteration.
- Replacing providers via `setAudioSender(...)` / `setVideoSender(...)` does not require poller recreation.
- `OpusAudioFrameProvider` is no longer required to manage silence/speaking transitions for opus transport behavior.

### MediaFrameProvider → AudioFrameProvider

`MediaFrameProvider` has been replaced by `AudioFrameProvider`. The interface is now focused on audio; codec selection and timestamp handling are handled by the poller.

**Old (2.x):**
```java
public interface MediaFrameProvider {
    boolean canSendFrame(Codec codec);
    boolean retrieve(Codec codec, ByteBuf buf, IntReference timestamp);
}
```

**New (3.0):**
```java
public interface AudioFrameProvider {
    void onCodecChanged(CodecInstance codec);
    void dispose();
    boolean canProvide();
    boolean provideFrame(ByteBuf buf);
}
```

- `setAudioSender` / `setVideoSender` and the corresponding getters now use `AudioFrameProvider` (same type for both in 3.0).
- Implementations must provide frames when `canProvide()` is true by writing to the buffer in `provideFrame(ByteBuf)` and returning whether a frame was written. Silence and speaking state are handled by the opus poller.

### Codec-aware providers: `onCodecChanged(CodecInstance)`

Providers are notified of the current codec so they can reject unsupported formats without racy startup behavior.

**Guarantees:**

- `onCodecChanged(CodecInstance codec)` is called **before** the first `canProvide()` for a provider attached via `setAudioSender` / `setVideoSender`.
- It is called again whenever the codec is changed via `setAudioCodec` / `setVideoCodec` while that provider is attached.

**Expected behavior:**

- In `onCodecChanged`, update any internal codec-dependent state (e.g. store the current `CodecInstance`).
- If the provider cannot supply data for the current codec, return `false` from `canProvide()` until the codec changes to a supported one. The poller will not call `provideFrame` when `canProvide()` is false.

In practice the audio codec is always Opus, so implementing codec checks is not strictly required for audio-only bots. This API is for completeness and for future or experimental video/other codec support (see [Experimental package](#experimental-package)).

**Example (Opus-only provider):**
```java
private boolean isOpus;

@Override
public void onCodecChanged(CodecInstance codec) {
    this.isOpus = OpusCodecInfo.isInstanceOf(codec);
}

@Override
public boolean canProvide() {
    if (!isOpus) return false;
    return /* ... has opus frame ... */;
}
```

### ext-udpqueue Updates

- `UdpQueueFramePollerFactory` moved to `moe.kyokobot.koe.poller.udpqueue`.
- `UdpQueueOpusFramePoller` now follows the shared opus poller hierarchy and uses `CodecInstance`.
- `UdpQueueFramePollerFactory` no longer creates its own `QueueManagerPool`.
  You must create and manage `QueueManagerPool` yourself and pass it to the factory.
  This is a breaking change.

**Old (2.x):**
```java
.setFramePollerFactory(new UdpQueueFramePollerFactory())
```

**New (3.x):**
```java
var queuePool = new QueueManagerPool(
    Runtime.getRuntime().availableProcessors(),
    UdpQueueFramePollerFactory.DEFAULT_BUFFER_DURATION
);

.setFramePollerFactory(new UdpQueueFramePollerFactory(queuePool));

// On shutdown:
queuePool.close();
```

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

## Gateway

The gateway implementation has been moved to the `internal` package until refactoring for an usable public API is complete.
