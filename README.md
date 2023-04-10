<div align="center">
  <h1>Trouser-Streak</h1>
    <p>Trouser-Streak is a compilation of modules, updated to the latest version and optimized for maximum grief. I did not make all of these.</p>
	    <p>I frequently release updates, please check back here often! :)</p>
  <img src="src/main/resources/assets/icon/icon.png" alt="Trouser-Streak Logo" width="28%"/>
</div>  

## Credits to the people I skidded from:
In no particular order
- [Meteor Client](https://github.com/meteordevelopment/meteor-client)
- [Allah-Hack](https://github.com/TaxEvasiqn/allah-hack)
- [Meteor-Rejects](https://github.com/AntiCope/meteor-rejects)
- [Frostburn Client](https://github.com/evaan/FrostBurn)
- [Banana](https://github.com/Bennooo/banana-for-everyone) credits for the idea behind TrouserBuild
- [1.17 Crafting Dupe](https://github.com/B2H990/NUMA-117-Crafting-Dupe/)
- [InstantKillBow](https://github.com/Saturn5Vfive/InstantKillBow)
- [LecternCrash](https://github.com/Coderx-Gamer/lectern-crash)
- [etianl](https://github.com/etianl/)

 <div align="left">
    <p>This modpack would not have been possible without you
 </p>

## Features:
- **Airstrike+:** Rains whatever entities you desire from a list similar to Boom. It used to only rain fireballs, and I also changed the positioning of the spawning. (Credits to Allah-Hack) 
- **AnHero:** Become An Hero! (A quick way back to spawn.) (Credits to etianl :D)
- **AutoDrop:** Drops the stack in your selected slot automatically, or you can choose a slot to dump. You can shift click your inventory items to dump your trash easily. (Credits to etianl :D)
- **AutoLavaCaster** Simple timer based bot for lavacasting. Aim at the top of the block you want to cast on and activate the module. It places lava, then after an amount of time removes the lava, places the water after a specified delay, removes it after a specified delay, it will build the mountain upward, tower you up and repeat. Position yourself on a block above and diagonally, mostly perpendicular from the targeted block for best results. Use the ".lavacalc" command to determine roughly how long lava will take to finish. (Credits to etianl :D)
- *AutoLavaCaster Notes:*
- The EstimateLavaTimer estimates lava time automatically based on a 45 degree staircase from your Y level down to sea level, or Y-60 if you are below sea level. 
- You can also enable another option to choose the bottom Y level for EstimateLavaTimer
- The .castertimer Command tells you how long each cycle has been running for. 
- The .lavacalc command gives you an approximation of how long lava will take to flow across a 45 degree staircase at 20TPS.
- Do not use Timer with this module.
- Rotating your character will break AutoLavaCaster. Disable rotate options in Freecam, Killaura, and any others that will rotate you when casting.
- Fish buckets, and other water buckets with entities do not work. Put the fishy somewhere safe before mountaining.
- If Build up is enabled, and not holding a block, appropriate blocks are selected from your hotbar automatically from left to right.
- Autoreplenish is recommended if building up, but disable search hotbar option.
- Do not use Flammable blocks if building up, and firespread is on.
- Reducing timing options while it's on can break it.
- **AutoMountain:** AutoMountain builds stairs in the direction you aim. It builds upward if you are looking toward the horizon or higher, and builds downward if you are looking down. (Credits to etianl :D)(Frostburn donated the framework and idea for the code, credits to them for that. <3)
- *AutoMountain Controls:* 
- UseKey (Right Click) starts and pauses mountain building.
- Left and RightKeys turn Mountain building.
- ForwardKey Turns mountain up, Back Key turns mountain down.
- JumpKey adjusts spacing of stairs according to the OnDemandSpacing value. 
- Start building, then hold SneakKey and also hold Left or RightKey as well to build stairs diagonally. Release left or right key first to continue building in the direction you were prior. 
- **AutoStaircase:** Builds stairs upward in the direction you are facing by running forward and jumping. (Credits to etianl for bringing it to life! As well as Credits to Frostburn for writing the original. <3) I just had to fix up some stuff for this one but Frostburn had the base code there. I believe this is the first publicly available automatic staircase builder in a Meteor addon, correct me if I'm wrong maybe I didn't have to learn some Java to do this.
- **BetterAutoSign:** Automatically writes signs with the text you specify, and can also apply glow ink or dye. (Credits to Meteor-Tweaks)
- **BetterScaffold:** Give you more options for scaffolding, bigger range and others. (Credits to Meteor-Tweaks)
- **Boom+:** Throws entities or spawns them on the targeted block when you click (Credits to Allah-Hack) I just added some more fun things you might want to throw.
- **ExplosionAura:** Spawns creepers at your position as you move that explode instantly. Like a bigger, more laggy Nuker module for creative mode. The use of the module Velocity is recommended to avoid being thrown around. (Credits to etianl :D)
- **FlightAntikick:** Moves you down on a tick-based timer. Added in to substitute the lack of an antikick for velocity flight in MeteorClient. Bind it to the same key as flight. (Credits to etianl :D)
- **HandOfGod:** Runs the "/fill" command on the world around you in different ways as you fly, and as you click. Operator status required. (Credits to etianl :D)
- **Inventory Dupe (1.17):** Duplicates things in your crafting slots when the module is enabled and the Dupe button is pressed in your inventory. Only works on Minecraft servers on the version 1.17, not any version before or after.(Credit to ItsVen and Da0neDatGotAway for original creation of the dupe, and to B2H990 for making the fabric mod. Credits to etianl for porting to Meteor.)
- **InstaKill:** Shoots arrows and tridents with incredible power and velocity. Enabling multiple buttons causes the amount of packets to add up. (Credits to Saturn5Vfive)
- **LecternCrash:** Crash 1.18.X vanilla servers and possibly below. (Credits to Coderx-Gamer)
- **NewerNewChunks:** NewChunks module with the ability to save chunk data for later! Also with special options for tracing servers that have been updated from a version before the build limit updates, which throw false positives normally. (Credits to Meteor Rejects, and BleachHack from where it was ported, and etianl for updating :D.)
- *NewerNewChunks Notes:*
- NewerNewChunks stores your NewChunks data as text files seperately per server and per dimension in the NewChunks folder in your Minecraft folder.
- Save and Load ChunkData options are for the stored files.
- This enables you to chunk trace multiple different servers and dimensions without mixing NewChunks data.
- If the game crashes, chunk data is saved! No loss in tracing progress.
- Send chunk data to your friends! Just copy the appropriate folder and send it.
- There is also an option for deleting chunk data in that particular dimension on the server.
- The .newchunkcount command can tell you how many chunks have been saved in data in the dimension you are in.
- *NewerNewChunks Special Options:*
- These are to be used when the server has two distinct diamond layers, and two distinct lava pool layers underground at spawn.
- The "IgnoreFlowBelow0" does not show a new chunk if liquid flow is only below Y zero, but does show a newchunk if flow is above Y zero, or both above AND below Y zero.
- The "AdvancedMode" highlights chunks that have flow only below Y0. If there is nothing but these you are updating old chunks to the new build limits and the FlowIsBelowY0 coloured chunks are OLD. If they are mixed with newchunks the FlowIsBelowY0 coloured chunks are NEW.
- AdvancedMode can be confusing, do not use if you can't interpret the chunk data.
- **RedstoneNuker:** It's just the regular Nuker module from Meteor client, customized for only breaking things that generate redstone signals. Also with included AutoTool. To keep you safer when placing lots of TNT. (Credits to Meteor Client for Nuker code, and AutoTool code inthere.)
- **ShulkerDupe:** Duplicates the contents of a shulker when pressing "Dupe" or "Dupe All" in the shulker menu. Only works on Vanilla, Forge, and Fabric servers 1.19 and below. Use multiconnect or viafabric (Credits to Allah-Hack, I just brought back the buttons, and make it dupe slot1 as well.)
- **Teleport:** Sets your position ontop of targeted block, within a possible reach of 64 blocks. Rarely can cause damage, be careful. ***EXPERIMENTAL*** (Credits to etianl :D)
- **TPFly:** It is a purely setPos based flight. PointAndFly mode is based off the ClickTP and AirPlace code, credits to Meteor for that. ***EXPERIMENTAL, movement is a little weird lol.*** (Credits to etianl :D)
- **TrouserBuild:** It can build either horizontally or vertically according to a 5x5 grid centered on the block you are aiming at. Right click to build at the targeted location. (Credits to etianl, and to Banana for the checkboxes and idea. :D)
- **Voider+:** Replaces the world from the top down. (Credits to Allah-Hack) I added a 3x3 voiding script, a TP foward option for deleting strips, as well as options to set max and minimum height for voiding, and instead of just air it can do water and lava now too.

## Known Bugs:
- **AutoLavaCaster Bugs** 
- The timing will break if the server is under 15TPS.
- If you are too far out of reach it breaks.
- If view of the targeted block is obstructed it breaks.
- **AutoMountain Bugs** 
- Some blocks may cause Automountain to attempt to build while not actually placing anything (Torches, walls, and doors did this until I fixed). If a block does not work please make an issue so I can correct it.
-------------------------------------
- *Blocks That do not work at all with AutoMountain*
- SnowBlocks
- maybe more?
-------------------------------------
-**More Bugs** 
- TPFly can hurt you once in a while on disable. I tried to prevent this. You also rubberband if going toward a block because it attempts to teleport you through it.
- Adjusting  TPFly antikick values while flying can be deadly
- .newchunkcount command shows exactly the chunks that are saved in chunk data, so when you are in normal mode or flowbelowY0 mode the returned values are not exactly in correlation to what is rendered on screen.
- NewerNewChunks has to be turned on atleast once prior to running .newchunkcount for the counter to work.

## Requirements:
- Latest dev build of Meteor-Client if using 1.19.4 or the full build of Meteor-Client if using 1.19.3. Meteor Client 0.5.1 is required for 1.19.2, which is required to connect to servers that have enforce secure chat = true.
- Don't forget to try updating any other mods you are using if your game is still crashing.

## Total Download Count:
**Trouser-Streak :D**
[![Github All Releases](https://img.shields.io/github/downloads/etianl/Trouser-Streak/total.svg)]()

Please check out my little Youtube channel here
https://www.youtube.com/@mountainsoflavainc.6913