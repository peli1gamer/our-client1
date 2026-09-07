# Our Client

A client-side Fabric utility client for Minecraft 1.21.11.

## Included

- Smooth aim assist
- Player tracers
- Freecam
- Auto schematic builder
- Persistent saved base coordinates
- JSON configuration

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

Tracer is unbound by default and can be assigned in Minecraft's Controls menu.

## Schematic builder

Put JSON schematics in:

`config/our-client1/schematics/build.json`

The builder processes a limited number of placement attempts per client tick. Change `schematicPlacementsPerTick` in `config/our-client1.json` to tune the throughput from 1 to 20.

Large schematics are loaded into memory, so very large files should be tested on the target machine before use.

## Build

GitHub Actions builds the project on every push and pull request and uploads the generated JAR as an artifact.
