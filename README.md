# Fabric Per-Player-Spawns

>This mod is intended to distribute mobs evenly between all players on a fabric modloaded server. To install, download
the appropriate jar from the release section and place into your (server's) mod folder. The fabric-api mod is not required.
>
>Copied and implemented as mixin from this
>[Paper](https://github.com/PaperMC/Paper/blob/master/Spigot-Server-Patches/0396-implement-optional-per-player-mob-spawns.patch)
patch. With thanks to HalfOf2 from Fabric discord for the initial mixin work.
>
Forked from Lambdacraft because he went afk.
>If you appreciate his maintenance of this mod please consider joining the [LambdaCraft](https://lambdacraft.dev/craft) server.

I rewired the mixins to work with 1.16.5, and then just rewrote that part of the mod as I see fit.

How it works: 

- modifies spawn logic to count chunks (and mobs in them) in a (square) radius around the player and account for chunks shared between the players
- radius to account is based on (server) render distance (like mob caps in vanilla)
- in the end each player has their own mob cap which applies to chunks around that player. (the most full one is applied in case of overlap)
- hold glistering melon in creative/spectator to see debug info

\[This means if two players are relatively close to each other\*\*, they can either have 70\* mobs in the area between them, or 70\* mobs each if no mobs are between them. Though, usually, it will be $70^*⋅1.5^{**}=105$ mobs, as intended by vanilla.\]\
    \* default monster mobcap, can be changed\
    \*\* ~1 server render distance; $\frac{\text{N chunks loaded by these playres}}{(SRD⋅2+1)^2}$


Works with [Proper Mobcap Modifier](https://github.com/vlad2305m/Proper-Mobcap-Modifier-Fabric) (by me)

[This on Modrinth](https://modrinth.com/mod/fabric-per-player-spawns)

<a href='https://ko-fi.com/M4M4I866V' target='_blank'><img height='36' style='border:0px;height:36px;' src='https://storage.ko-fi.com/cdn/kofi1.png?v=3' border='0' alt='Buy Me a Coffee at ko-fi.com' /></a>
