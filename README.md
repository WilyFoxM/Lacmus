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

Two lines in the top-left corner, hidden while the F3 screen is open:

```
fps 144  rtt 42 ms ±3 ms
in 12 kB/s 40/s  out 1.2 kB/s 20/s
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
- During a stall or a pause the whole first line turns red.
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
