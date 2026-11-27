# NanoFutureAddon

A Meteor Client addon for Minecraft 1.21.11, 26.1.2, and 26.3. Adds a few exploit, movement, and utility modules.

## build

    ./gradlew build
    ./gradlew collectJars

`collectJars` copies all version jars to `build/libs/`. The jar name includes the Minecraft version.

## install

Put the jar that matches your Minecraft version in your mods folder next to Meteor Client.

## modules

- `ai-assistant`: chat with a configured provider. Use `.ai <message>`; `.contextclear` resets context. Tool use is the `use-tools` toggle in the module settings. The model can read nearby blocks, entities, the tab list, locator bar, scoreboard, boss bars, chat, open and remembered containers, and the crosshair target. It can save per-server notes and start or stop Baritone paths.
- `auto-chunk-miner`: mines a chunk from above your head down to a target Y. Fills water and lava next to the layer and pauses when your tool is about to break.
- `auto-terraform`: clears, fills, or levels blocks around you to a target layer.
- `auto-torch`: places torches on dark spawnable blocks in a chosen radius. Optional spawn overlay.
- `fast-throw`: sends a small X offset before a bow release or a thrown item.
- `ghost-blocks`: places client-side blocks that break in one hit.
- `infinite-reach`: teleports to distant blocks to break or use them, or to open containers, then teleports back.
- `instant-break`: sends the break packets and stops at the server's 0.7 finish point.
- `keep-break-progress`: keeps break progress when you stop mining.
- `phase`: teleports through walls while your hitbox is inside a block.
- `reminder`: shows a toast with your text at a set interval.
- `spawn-overlay`: renders blocks where hostile mobs can spawn.
- `time-shift`: sends move packet bursts to speed up regen and effects on 1.8 servers.

## notes

1.21.11 builds use Java 21; 26.1.2 and 26.3 builds use Java 25. Gradle selects the toolchain automatically.
AI assistant API keys are masked in the settings window.
`.aitool <name> [json]` runs one tool directly and `.aitool` lists them.

TimeShift only works on 1.8 servers.
Phase needs you to be inside a block first.
InfiniteReach is capped at 22 blocks on vanilla.
`instant-break` has a `fists` toggle on 26.1.2, 26.3, and 1.21.11. It is a test on 1.21.11 and only bypasses servers that accept early STOPs.
AutoTerraform only works within interaction range; walk to cover an area.
AutoChunkMiner uses Baritone when installed, otherwise Reach mode only mines blocks in range.
AutoChunkMiner and AutoTerraform use InfiniteReach for blocks beyond normal reach; vanilla still caps normal reach at about 5.5 blocks.
GhostBlocks swallows right click while active; use SneakRightClick mode to keep normal interactions.

## server ads

The title screen and multiplayer screen show clickable ads for The Future (`future.playsmp.xyz`) and Truth & Verity. Click one to join; hover it to see the address. Settings are in `config/future-ads.properties`. An empty `verity.address` uses the saved server named "Truth & Verity". The same ads ship as the standalone FutureAds mod; when that mod is installed, the addon hides its own copy.

## commands

    .ai <message>
    .contextclear

## license

All rights reserved. See `LICENSE`.
