# Fabric Per-Player-Spawns

This mod is intended to distribute mobs evenly between all players on a fabric modloaded server. To install, download
the appropriate jar from the release section and place into your (server's) mod folder. The fabric-api mod is not required.

Copied and implemented as mixin from this
[Paper](https://github.com/PaperMC/Paper/blob/master/Spigot-Server-Patches/0396-implement-optional-per-player-mob-spawns.patch)
patch. With thanks to HalfOf2 from Fabric discord for the initial mixin work.

Forked from Lambdacraft because he went afk.
If you appreciate his maintenance of this mod please consider joining the [LambdaCraft](https://lambdacraft.dev/craft) server.

Perhaps I didn't look at how the counting works, just rewired the mixins to work with 1.16.4 (and commented out some useless from my point of view old wiring)
