# Tilth Atlas

Unofficial NeoForge 1.21.1 ports of three Fabric mods, so that they run natively instead of through Sinytra Connector and Forgified Fabric API:

| Mod | What it does | Upstream | Ported from |
|---|---|---|---|
| Surveyor Map Framework | Records explored terrain, structures and landmarks, and shares them between players | [sleepingdragoninn/surveyor](https://github.com/sleepingdragoninn/surveyor) | 1.2.4+1.21 (branch `1.21`, commit `22e24e0`) |
| Antique Atlas 4 | The hand-drawn atlas itself, client side | [sleepingdragoninn/antique-atlas](https://github.com/sleepingdragoninn/antique-atlas) | 3.1.2+1.21 (branch `1.21`, commit `1821a06`) |
| Antique Transport | Create tracks, trains and stations, and Sable ships, on the atlas | [MarizPOG/antique_transport](https://github.com/MarizPOG/antique_transport) | 1.3.0 (commit `00f0251`) |

**The ports are not made, endorsed or supported by the original authors.** Please report problems [here](https://github.com/salvagedisc/tilth-atlas/issues), never upstream.

## Requirements

- Minecraft 1.21.1, NeoForge 21.1 or later, Java 21.
- Surveyor goes on the client and, for map sharing and structure discovery, on the server.
- Antique Atlas 4 is a client mod and needs Surveyor. Its jar does nothing on a dedicated server.
- Antique Transport needs Surveyor, and Antique Atlas 4 on the client. Create, Sable and Create Aeronautics are optional; each one enables its own layer of the map.

Do not install these jars next to the Fabric builds of the same mods. Connector and Forgified Fabric API are not needed by them.

## Compatibility with upstream

Mod ids, packet ids, config files (`surveyor.toml`, `antique-atlas.toml`, `antique_transport.toml`) and save formats are unchanged. A server world explored with the Fabric builds through Connector loads and saves again without errors. Client data is read from the same files, so a client should keep its map and waypoints too; that side has not been tried with a real save. Resource packs written for Antique Atlas 4 work as they are.

Client and server are expected to run the same ports. Connecting a Fabric build on one side to a port on the other has not been tried.

## What changed from upstream

Game logic is untouched, with one exception: the ship teleport of Antique Transport is removed (last item). The other changes are what the move to another loader and to Mojang names requires:

- Sources remapped from Yarn to Mojang names with Loom's `migrateMappings`, then imports sorted again and the qualified names the tool leaves behind cleaned up. Surveyor's access widener became `META-INF/accesstransformer.cfg`.
- Entry points, payload registration, events, commands, key mapping, reload listeners, creative tab entry and mod metadata use NeoForge's API instead of Fabric API. Each mod has a common entry point and, where it has client code, a client one.
- **Surveyor**: the point-of-interest landmark mixin targeted two lambdas of `ServerLevel#onBlockStateChange` by their intermediary names. It now hooks `DebugPackets#sendPoiAddedPacket` and `#sendPoiRemovedPacket`, which only those lambdas call (`MixinDebugPackets`).
- **Antique Atlas 4**:
  - NeoForge patches the map check of `ItemInHandRenderer#renderArmWithItem` into an `instanceof MapItem`; the mixin that shows the atlas held in the hand wraps that check instead of the vanilla call.
  - The atlas model is registered with `ModelEvent.RegisterAdditional` instead of a `ModelBakery` mixin.
  - Reload listeners are registered in dependency order, since NeoForge has no equivalent of Fabric's declared listener dependencies.
  - The pre-1.21 `c:` biome tags that Fabric API still declares are declared in `LegacyConventionalBiomeTags`; the current ones come from NeoForge's `Tags.Biomes`.
  - The built-in `shader_patch` resource pack is no longer registered: its files are not in the upstream sources.
- **Antique Transport**: the Teleport button of the ship window is removed, with its packet and its server receivers. Upstream registers two receivers for that packet, and the one that takes effect lets any player teleport, although the button is only shown to operators.

## Status

Tried on a dedicated server and a client that both run a large modpack, Create and Sable included: the client joins, terrain syncs from the server, the atlas opens with its key, a book renamed "Antique Atlas" takes the atlas model and shows the map when held, and the client map is saved on disconnect.

Not tried yet: trains and ships on the map, structure discovery and map sharing between several players.

## Building

```
./gradlew build
```

The three jars are written to `surveyor/build/libs/`, `antique-atlas/build/libs/` and `antique-transport/build/libs/`. As upstream, the Surveyor jar bundles [Kaleido Config](https://github.com/sleepingdragoninn/kaleido-config) and the Antique Transport jar bundles [Sable Companion](https://github.com/ryanhcode/sable-companion). Version numbers are the upstream version followed by `-neoforge.N`, set by `port_suffix` in `gradle.properties`.

Antique Transport compiles against Create, Sable and Create Aeronautics, fetched from the Modrinth Maven. Their version ids are at the top of `antique-transport/build.gradle`.

## Following upstream

There is no automatic sync. To take a new upstream version:

1. In a checkout of the upstream mod, run `./gradlew migrateMappings --mappings "net.minecraft:mappings:1.21.1"`; the result is in `remappedSrc/`.
2. Compare it with the same output for the commit named in the table above, and apply the difference to the module here.
3. Update the commit and version in this file and in the module's `build.gradle`, and reset `port_suffix` to `neoforge.1`.

Antique Transport is already written with Mojang names and needs no remapping.

## License and credits

- Code: LGPL-3.0-or-later for Surveyor and Antique Atlas 4, LGPL-3.0 for Antique Transport, like upstream. The changes of this port, made in November 2026, are under the same licenses. Each module carries its upstream license file, and `LICENSE` at the root is the LGPL-3.0 text.
- Antique Atlas 4 textures: CC BY-NC-SA 4.0, with per-texture attribution in `antique-atlas/CREDITS`.

All credit for the mods goes to their authors:

- Surveyor and Antique Atlas 4: Sisby folk and contributors. Antique Atlas 4 is a rewrite of Antique Atlas by Hunternif, continued by Kenkron, asie and tyra314; textures by Hunternif and lumiscosity.
- Antique Transport: Mariz.
