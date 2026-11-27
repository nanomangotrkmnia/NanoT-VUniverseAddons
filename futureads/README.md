# FutureAds

A small client-side Fabric mod for Minecraft 1.21.11, 26.1.2 and 26.3. It adds two clickable ads:

- **Join The Future!** bottom-right on the title screen and top-right on the multiplayer screen. Joins `future.playsmp.xyz`.
- **Try the Verity / Try at T&V** bottom-left on the title screen and left of the server list. Joins Truth & Verity.

Hover an ad to see the address; click it to connect. An ad is skipped when it would cover a button or the screen is too narrow.

No Fabric API or Meteor needed. NanoFutureAddon ships the same ads and turns its own off when this mod is installed.

## config

`config/future-ads.properties` is created on first launch and re-read whenever the screen opens:

    enabled=true
    title-screen=true
    multiplayer-screen=true
    future.name=The Future
    future.address=future.playsmp.xyz
    verity.name=Truth & Verity
    verity.address=

An empty address uses the saved server-list entry with that name.

## build

From the repository root:

    ./gradlew -p futureads collectJars

Jars land in `futureads/build/libs/`, one per Minecraft version.

## license

All rights reserved. See `../LICENSE`.
