# Koe - Native UDP-Queue extension

An extension that provides an implementation of [JDA-NAS](https://github.com/sedmelluq/jda-nas) in Koe, 
which moves packet sending/scheduling logic outside JVM, therefore audio packets can be sent during GC pauses (as long as there's enough audio data in the queue).

Note that custom codec support is limited, adds additional latency and proper usage of Netty already 
limits GC pressure because of much smaller number of allocations.

### Usage

Just add it to KoeOptions :^)

```java
var Koe = Koe.koe(KoeOptions.builder()
    .setFramePollerFactory(new UdpQueueFramePollerFactory())
    .create());
```
