# Arson Client

A client-side Fabric utility client for Minecraft 1.21.11.

## Native module checklist

### Movement

- [x] Auto Walk
- [x] Auto Jump
- [x] Air Jump
- [x] Sprint
- [x] Fullbright
- [x] Auto Sprint
- [x] Fast Climb
- [x] Step
- [x] No Fall
- [ ] Speed
- [ ] Long Jump
- [ ] Bunny Hop
- [ ] No Slow
- [ ] Jesus
- [ ] Spider
- [ ] Flight
- [ ] High Jump
- [ ] Safe Walk

### Other implemented modules

- [x] Smooth aim assist
- [x] Trigger bot
- [x] Crystal macro
- [x] Attribute swap
- [x] Player tracers
- [x] ESP
- [x] Xray
- [x] Freecam
- [x] Auto schematic builder
- [x] Persistent saved base coordinates
- [x] Scaffold
- [x] JSON configuration
- [x] Arson Client fire-orange ClickGUI theme

## Requirements

- Minecraft 1.21.11
- Fabric Loader 0.18.1 or newer
- Fabric API
- Java 21

## Controls

| Key | Action |
| --- | --- |
| `R` | Toggle aim assist |
| `F6` | Toggle freecam |
| `F7` | Toggle auto schematic builder |
| `V` | Save current base |

Module keybinds can be assigned in Minecraft's Controls menu. The ClickGUI is opened with `Right Shift`.

## Schematic builder

Put JSON schematics in:

`config/our-client1/schematics/build.json`

The builder processes a limited number of placement attempts per client tick. Change `schematicPlacementsPerTick` in `config/our-client1.json` to tune the throughput from 1 to 20.

Large schematics are loaded into memory, so very large files should be tested on the target machine before use.

## Build

GitHub Actions builds the project on every push and pull request and uploads the generated JAR as an artifact.
