# Discord voice quirks

Things Discord voice servers do that the docs don't tell you about, collected while working on Koe. Some are bugs,
some are just undocumented. Twice now, the only way to get to the bottom of one was to ask someone at Discord.

## Late joiners can't hear a bot that's already playing (2020)

In April 2020, music bots started going silent after a few hours of uptime. It hit around 5% of guilds: the bot had
the green ring, but nobody heard anything, and the voice debug panel in the client showed its packets as unknown.
It wasn't Koe specific, Lavalink without Koe had it too. Resuming the voice connection didn't fix it, only stopping
and starting the player did.

It turned out the voice server's speaking state handling was broken. Anyone who joined the channel while the bot was
already playing never got its speaking update (op 5), so their client ignored its audio. We reported it to Discord
and they passed it on to their native client team. In the meantime Koe got a hack: whenever someone joins (op 12
in gateway v4), send op 5 again.

The hack is still there, six years later, as `Op12HackListener` in `AbstractOpusFramePoller`. Since October 2026 it
also resends op 5 after every session description (op 4), because the speaking state doesn't carry over when the bot
moves to another channel or voice server while it keeps playing.

## Bots get a garbage SSRC on stage channels (2026)

A bot joining a stage channel gets an SSRC in READY (op 2) that doesn't work. The connection looks fine, packets go
out and even show up in the client's debug panel, but nobody hears anything, and it stays that way until the bot
reconnects. A bot only gets a working SSRC if it has requested to speak or was moved to the stage by someone else.

A user joining the same way, without requesting to speak, gets a working SSRC, even though the official client's
WebSocket and HTTP traffic looked the same as the bot's. So it's not about what the bot sends, but about the server
treating bots differently.

The workaround is to have the bot request to speak (`PATCH /guilds/{guild.id}/voice-states/@me` with
`request_to_speak_timestamp`). After that, the voice gateway hands it a working SSRC. See `requestToSpeakIfSuppressed`
in Koe's test bot. We reported it to Discord in October 2026.

We don't know how this works on Discord's side, and we likely never will. Our guess: bots get special treatment as
users that are always connected, stage channels get special treatment on the backend too, and when the voice gateway
doesn't know about the bot yet, it sends back an uninitialized variable as the SSRC (plausible since apparently 
part of the backend is implemented in C++... oops).

As a bonus, these SSRCs are above 2^31, which showed us that Koe had been sending SSRCs back as signed 32-bit
integers, so op 5 and op 12 went out with negative numbers. Both Discord's docs and the IETF specs say SSRCs are
unsigned (a 32-bit field in RTP per RFC 3550, 0 to 2^32-1 in SDP per RFC 5576), we just never expected to see ones
that large.

## Speaking stop cuts off the end of the audio

Clients stop playing your audio as soon as they get a speaking update with an empty mask, even if some of it is
still buffered on their side. A bot that sends it right after its last frame loses the tail. Clients notice you
stopped talking on their own once packets stop coming, so since October 2026 Koe doesn't send it unless you turn
it on with `KoeOptionsBuilder#setSendSpeakingStop` (#21).

## Endpoints with port 80

`VOICE_SERVER_UPDATE` used to give endpoints with port 80, but the voice gateway only took secure WebSocket
connections on 443. The docs back then didn't mention it, they only said the endpoint comes without the `wss://`
prefix.
Libraries learned about it from each other instead: Discord4J, Disgord, DiscordGo, DiscordPHP and others all strip
the `:80`.

Koe does the same and connects to the default port (`KoeOptionsBuilder#setEnableWSSPortOverride`, on by default,
#38).

## The video flag was forced off for bots (2019/2020)

Media sink wants (op 15) only gets sent if you identify with `video: true`. Around 2019 and 2020, Discord forced this
flag to false for bots because of a voice server bug that broke clients. This may not be the case anymore.
