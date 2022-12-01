<div align="center">
  <h1>Trouser-Streak</h1>
    <p>Trouser-Streak is a compilation of modules, updated to the latest version and optimized for maximum grief. I did not make most of these.</p>
  <img src="src/main/resources/assets/icon/icon.png" alt="Trouser-Streak Logo" width="28%"/>
</div>  

## Credits to the people I skidded from:
In no particular order
- [Meteor Client](https://github.com/meteordevelopment/meteor-client)
- [Allah-Hack](https://github.com/TaxEvasiqn/allah-hack)
- [Meteor-Rejects](https://github.com/AntiCope/meteor-rejects)
- [Frostburn Client](https://github.com/evaan/FrostBurn)
- [Banana](https://github.com/Bennooo/banana-for-everyone)
- [1.17 Crafting Dupe](https://github.com/B2H990/NUMA-117-Crafting-Dupe/)
- [InstantKillBow](https://github.com/Saturn5Vfive/InstantKillBow)
- [LecternCrash](https://github.com/Coderx-Gamer/lectern-crash)

 <div align="left">
    <p>This modpack would not have been possible without you
 </p>

## Features:
- **Airstrike+:** Rains whatever entities you desire from a list similar to Boom. It used to only rain fireballs. (Credits to Allah-Hack) 
- **AutoBuild:** Places blocks according to placement in a 5x5 grid. You can draw pictures with it! (Credits to Banana) I modified it so it places lengthways infront of you. This way you can draw stairs and place them infront of you, hop up em and keep going.
- **AutoDrop:** Drops the stack in your selected slot automatically. You can shift click your inventory items to slot one to dump your trash easily.
- **AutoMountain:** AutoMountain builds stairs in the direction you aim. It builds upward if you are looking toward the horizon or higher, and builds downward if you are looking down. (Credits to Frostburn for the base for the code, and Banana for the player centering utils.)
- *AutoMountain Controls:* 
- Left and RightKeys turn Mountain building.
- ForwardKey Turns mountain up, Back Key turns mountain down.
- UseKey (Right Click) disables AutoMountain.
- JumpKey adjusts spacing of stairs according to the OnDemandSpacing value 
- **AutoStaircase:** Builds stairs upward in the direction you are facing. (Credits to Frostburn, and Banana for the player centering utils to make it work correctly) I just had to fix up some stuff for this one but Frostburn had the base code there. I believe this is the first publicly available automatic staircase builder in a Meteor addon, correct me if I'm wrong maybe I didn't have to learn some Java to do this.
- **BetterScaffold:** Give you more options for scaffolding, bigger range and others. (Credits to Meteor-Tweaks)
- **Boom:** Throws entities when you click (Credits to Allah-Hack) I just added some more fun things you might want to throw.
- **ExplosionAura:** Spawns creepers at your position that explode as you move. Like a bigger, more laggy Nuker module for creative mode. The use of the module Velocity is recommended to avoid being thrown around.
- **HandOfGod:** Deletes the world around you as you fly, and as you click. It deletes when you press the directional keys, or when you click it fills with the specified block. Operator status required.
- **Inventory Dupe (1.17):** Duplicates things in your crafting slots when the module is enabled and the Dupe button is pressed in your inventory. (Credit to ItsVen and Da0neDatGotAway for original creation of the dupe, and to B2H990 for making the fabric mod.)
- **InstaKill:** Shoots arrows and tridents with incredible power and velocity. Enabling multiple buttons causes the amount to add up. (Credits to Saturn5Vfive)
It can also retrieve arrows and items from a distance if you shoot in that direction, I have noticed.
- **LecternCrash:** Crash 1.18.X vanilla servers and possibly below. (Credits to Coderx-Gamer)
- **Phase:** Allows you to phase through blocks vertically, and through thin blocks horizontally such as doors and world border (Credits to Meteor-Rejects) Please add this back it's not too terrible.
- **RedstoneNuker:** It's just the regular Nuker module from Meteor client, customized for only breaking things that generate redstone signals. Also with included AutoTool. To keep you safer when placing lots of TNT.
- **ShulkerDupe:** Duplicates the contents of a shulker when opening. Only works on Vanilla, Forge, and Fabric servers 1.19 and below. Use multiconnect or viafabric (Credits to Allah-Hack)
- **TPFly:** It is a purely setPos based flight. Based off the ClickTP code, credits to Meteor for that. ***EXPERIMENTAL, movement is a little weird lol***
- **TrouserFlight:** I just added a Normal mode antikick for Velocity flight cuz missing at the time
- **Voider:** Replaces the world from the top down (Credits to Allah-Hack) I only added options to set max and minimum height for voiding, and instead of just air it can do water and lava now too.

## Known Bugs:
- **Do Not Use These Blocks With AutoMountain**(especially with the SwapStackonRunOut option)**:**
- Walls and Fences
- Falling Blocks (Sand, Gravel, Anvils, etc.)
- Doors
- Flowers
- Torches
- Anything that requires support from a block beneath
- There may be more
- **Other:**
- TPFly can hurt you once in a while on disable. I tried to prevent this. You also rubberband if going toward a block because it attempts to teleport you through it.
- Adjusting  TPFly antikick values while flying can be deadly
