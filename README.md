<p align="center">
  <img src="docs/banner.png" alt="Lacmus" width="100%">
</p>

<p align="center">
  <a href="https://github.com/WilyFoxM/Lacmus/actions/workflows/build.yml"><img src="https://img.shields.io/github/actions/workflow/status/WilyFoxM/Lacmus/build.yml?branch=main&label=build&labelColor=272727" alt="Build"></a>
  <img src="https://img.shields.io/badge/Minecraft-26.2-d1495b?labelColor=272727" alt="Minecraft 26.2">
  <img src="https://img.shields.io/badge/loader-Fabric%20%7C%20Quilt%20%7C%20NeoForge-d1495b?labelColor=272727" alt="Fabric | Quilt | NeoForge">
  <img src="https://img.shields.io/badge/Java-25-d1495b?labelColor=272727" alt="Java 25">
  <img src="https://img.shields.io/badge/license-MIT-d1495b?labelColor=272727" alt="MIT">
</p>

# Lacmus

A small client-side network graph HUD for Minecraft, in the spirit of `net_graph`.

Lacmus is in early development.

## What it shows

Three lines in the top-left corner by default, one for each side of the game, hidden while the
F3 screen is open:

```
fps 144 (max 18 ms)  tick 2.1 ms  queue 4 ms
rtt 42 ms ±3 ms  in 12 kB/s 40/s  out 1.2 kB/s 20/s
tps ≈19.9
```

**The client**

- **fps**: frames per second, as the game counts them. **(max 18 ms)** is the longest frame of the
  last second, as the game times it for its own frame chart: a stutter that the frame rate hides.
- **tick**: how long the client's tick takes on average over the last second. Above 50 ms the
  client cannot keep 20 ticks a second.
- **queue**: the longest a packet waited in the last second for the game to handle it after it
  arrived. The game works through arriving packets once per frame, so up to about one frame is
  normal (17 ms at 60 fps); a long wait means the client, not the network or the server, cannot
  keep up.

**The connection**

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

To time the round trip, Lacmus sends two of the game's own ping requests per second (a
setting) while the HUD is shown on a server. The game itself sends them only while its network chart is open; then
Lacmus times the game's requests and sends none.

**The server**

- **tps**: an estimate of how many ticks a second the server runs, from the time it sends once
  every 20 ticks anyway, over the last 5 seconds. Time that arrives late, as it does when a proxy
  switches servers, does not push the estimate above the real rate; for about two seconds after
  joining a server it shows **—** instead. When the server aims at a rate other than 20
  (`/tick rate`), it is shown too: `≈9.9 of 10`. **frozen** when the server froze its ticks,
  **late** when its next time is overdue, **paused** in a paused single-player game. Unknown
  during a stall: then the connection, not the server, decides what arrives.

A line that reports a problem on its side turns red: the client line when packets wait 250 ms or
more or the client tick takes 50 ms or more, the connection line during a stall or a pause, the
server line when the server falls below 90 % of its rate or its time is late. **—** means the
value is unknown.

## Settings

**F6** opens the settings; **Show or hide Lacmus** in Controls → Lacmus has no key by default.
Changes apply and save at once, in `config/lacmus.json`.

- **General**: show the HUD; the mode, **Full** (every metric) or **Easy to read** (one line:
  fps, latency and jitter); the latency, **rtt** (the newest round trip) or **ping** (the mean
  round trip of the last 5 seconds); the screen corner; the size; the background opacity.
- **Metrics**: in the full mode, each part on or off: the longest frame, the client tick, the
  packet queue, the jitter, bytes, packets and the server line.
- **Alarms**: red lines on or off, and when they turn red: packets waiting 250 ms, a client
  tick of 50 ms, a server below 90 % of its rate.
- **Network**: the HUD's own ping requests on or off, and how many a second (2 by default).
  Off, the latency is known only while the F3 network chart is open.

## Requirements

- Minecraft 26.2
- Fabric Loader 0.19.3 or newer with Fabric API, Quilt Loader with Fabric API, or NeoForge
  26.2.0.93 or newer

Lacmus is a client-side mod: a server does not need it.

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
