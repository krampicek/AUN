# TweakKit (Fabric, Minecraft 26.3, Java 25, Mojang mappings)

Easiest setup: generate a blank 26.3 project at https://fabricmc.net/develop/ and copy in
`src/` plus the dependency lines from build.gradle (plugin id / Loom version may differ).

Build: `./gradlew build`  ->  build/libs/tweakkit-1.0.0.jar
Requires: Fabric API, Cloth Config (26.3.x). Optional: Mod Menu (21.x).
Config file: config/tweakkit.json
Install on BOTH client and server for multiplayer.
