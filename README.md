<p align="center">
  <img src="docs/banner.png" alt="Lacmus" width="100%">
</p>

<p align="center">
  <a href="https://github.com/WilyFoxM/Lacmus/actions/workflows/build.yml"><img src="https://img.shields.io/github/actions/workflow/status/WilyFoxM/Lacmus/build.yml?branch=main&label=build&labelColor=272727" alt="Build"></a>
  <img src="https://img.shields.io/badge/Minecraft-26.2-d1495b?labelColor=272727" alt="Minecraft 26.2">
  <img src="https://img.shields.io/badge/loader-Fabric-d1495b?labelColor=272727" alt="Fabric">
  <img src="https://img.shields.io/badge/Java-25-d1495b?labelColor=272727" alt="Java 25">
  <img src="https://img.shields.io/badge/license-MIT-d1495b?labelColor=272727" alt="MIT">
</p>

# Lacmus

A small client-side network graph HUD for Minecraft, in the spirit of `net_graph`.

Lacmus is in early development.

## What it shows

Three lines in the top-left corner, hidden while the F3 screen is open:

```
fps 144  rtt 42 ms ±3 ms
in 12 kB/s 40/s  out 1.2 kB/s 20/s
tps ≈19.9  queue 4 ms
```

- **fps**: frames per second, as the game counts them.
- **rtt**: the round trip of a ping request to the server and back, timed where the packets
  enter and leave the network layer, so a client stutter does not show up as network lag.
  **±** is the jitter: how much one round trip differs from the previous one, on average over
  the last 16.
- **in / out**: bytes per second over the connection, after compression and encryption but
  without TCP/IP headers, and packets per second (a bundle of packets counts as one). Both are
  averaged over the last second.
- **rtt stall**: on a server, nothing has arrived for more than 1.5 seconds, so the ping
  requests are not answered either; it shows for how long. After 5 seconds it also counts down
  to the game's own 30-second timeout. The server answers ping requests even while it is busy,
  so a stall points at the connection rather than at a slow server.
- **no data**: the same pause in single player, where there is no round trip.
- **tps**: an estimate of how many ticks a second the server runs, from the time it sends once
  every 20 ticks anyway, over the last 5 seconds. Time that arrives late, as it does when a proxy
  switches servers, does not push the estimate above the real rate; for about two seconds after
  joining a server it shows **—** instead. When the server aims at a rate other than 20
  (`/tick rate`), it is shown too: `≈9.9 of 10`. **frozen** when the server froze its ticks,
  **late** when its next time is overdue, **paused** in a paused single-player game. Unknown
  during a stall: then the connection, not the server, decides what arrives.
- **queue**: the longest a packet waited in the last second for the game to handle it after it
  arrived. The game works through arriving packets once per frame, so up to about one frame is
  normal (17 ms at 60 fps); a long wait means the client, not the network or the server, cannot
  keep up.
- A line that reports a problem turns red: the first line during a stall or a pause, the last
  line when the server falls below 90 % of its rate, its time is late, or packets wait 250 ms or
  more.
- **—**: the value is unknown. In single player there is no round trip and no bytes on a wire.

To time the round trip, Lacmus sends two of the game's own ping requests per second while the
HUD is shown on a server. The game itself sends them only while its network chart is open; then
Lacmus times the game's requests and sends none.

## Controls

**Show or hide Lacmus** in Controls → Lacmus. It has no key by default.

## Requirements

- Minecraft 26.2
- Fabric Loader 0.19.3 or newer
- Fabric API

## License

The code is under the [MIT](LICENSE) license. The mod icon and the banner are
simplified recolors of a Minecraft texture, which belongs to Mojang, and are not covered
by that license.

## Attributions

- [NetGraph](https://modrinth.com/mod/netgraph) by denny4 (MIT), version 1.0.0+1.21.8: the idea
  of counting bytes and packets with handlers in the connection's Netty pipeline. Lacmus has its
  own implementation.
- [Where Is My Packet?](https://github.com/greneboy/wimp) by greneboy (MIT): the idea of showing
  how long no packet has arrived, with a countdown to the timeout. Lacmus has its own
  implementation.
