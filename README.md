<div align="center">
  <h1>Trouser-Streak</h1>
    <p>Trouser-Streak is a compilation of modules, updated to the latest version and optimized for maximum grief. I did not make most of these.</p>
  <img src="src/main/resources/assets/icon/icon.png" alt="Trouser-Streak Logo" width="28%"/>
</div>  

## Credits to the people I skidded from:
- [Meteor Client](https://github.com/meteordevelopment/meteor-client)
- [Allah-Hack](https://github.com/TaxEvasiqn/allah-hack)
- [Meteor-Rejects](https://github.com/AntiCope/meteor-rejects)
- [GriefWaffen](https://github.com/CuteNoobCodes/GriefWaffen-public)
- [Frostburn Client](https://github.com/evaan/FrostBurn)
- [Banana](https://github.com/Bennooo/banana-for-everyone)
- [1.17 Crafting Dupe](https://github.com/B2H990/NUMA-117-Crafting-Dupe/)
- [InstantKillBow](https://github.com/Saturn5Vfive/InstantKillBow)
- [LecternCrash](https://github.com/Coderx-Gamer/lectern-crash)

 <div align="left">
    <p>This modpack would not have been possible without you
 </p>

## Features:
- **AutoBuild:** Places blocks according to placement in a 5x5 grid. You can draw pictures with it! (Credits to Banana) I modified it so it places lengthways infront of you. This way you can draw stairs and place them infront of you, hop up em and keep going.
- **AutoBuildDown:** Same as above, but it places beneath your feet infront of you for downward stair building. Top right space is not reachable by the player so don't use that space in this case. (Credits to Banana)
- **AutoDrop:** Drops the stack in your selected slot automatically. You can shift click your inventory items to slot one to dump your trash easily.
- **AutoStaircase:** Builds stairs upward in the direction you are facing. (Credits to Frostburn, and Banana for the player centering utils to make it work correctly) I just had to fix up some stuff for this one but Frostburn had the base code there. I believe this is the first publicly available automatic staircase builder in a Meteor addon, correct me if I'm wrong maybe I didn't have to learn some Java to do this.
- **AutoMountain:** AutoMountain builds stairs in the direction you aim. It builds upward if you are looking toward the horizon or higher, and builds downward if you are looking down. (Credits to Frostburn for the base for the code.)
- *AutoMountain Controls:* 
Left and Right Keys turn Mountain building.
Forward Key Turns mountain up, Back Key turns mountain down.
The use key (Right Click) disables AutoMountain. 
- **BetterScaffold:** Give you more options for scaffolding, bigger range and others. (Credits to Meteor-Tweaks)
- **Boom:** Throws entities when you click (Credits to Allah-Hack) I just added some more fun things you might want to throw.
- **ExplosionAura:** Spawns creepers at your position that explode as you move. Like a bigger, more laggy Nuker module for creative mode. The use of the module Velocity is recommended to avoid being thrown around.
- **FireballClicker:** Shoots fireballs wherever you click (Credits to GriefWaffen)
- **FireballRain+:** Rains whatever entities you desire from a list similar to Boom (Credits to Allah-Hack.) I did pull the code from GriefWaffen, the name was from there. It used to only rain fireballs, but I didn't think that was fun enough so I added the things put in Boom.
- **HandOfGod:** Deletes the world around you as you fly, and as you click. It deletes when you press the directional keys, or when you click it fills with the specified block. Operator status required.
- **Inventory Dupe (1.17):** Duplicates things in your crafting slots when the module is enabled and the Dupe button is pressed in your inventory. (Credit to ItsVen and Da0neDatGotAway for original creation of the dupe, and to B2H990 for making the fabric mod.)
- **InstaKillBow** Shoots arrows with incredible power and velocity. Credits to Saturn5Vfive. Disable extra "MovePacket" buttons in the inventory to choose how many to send. I couldn't change the default from them all being enabled and they all add up.
- **LecternCrash** Crash 1.18.X vanilla servers and possibly below. (Credits to Coderx-Gamer)
- **Phase:** Allows you to phase through blocks vertically, and through thin blocks horizontally such as doors and world border (Credits to Meteor-Rejects) Please add this back it's not too terrible.
- **RedstoneNuker:** It's just the regular Nuker module from Meteor client, customized for only breaking things that generate redstone signals. Also with included AutoTool. To keep you safer when placing lots of TNT.
- **ShulkerDupe:** Duplicates the contents of a shulker when opening. Only works on Vanilla, Forge, and Fabric servers 1.19 and below. Use multiconnect or viafabric (Credits to Allah-Hack)
- **TPFly:** It is a purely setPos based flight. Based off the ClickTP code, credits to Meteor for that. ***EXPERIMENTAL, movement is a little weird lol***
- **Voider:** Replaces the world from the top down (Credits to Allah-Hack) I only added options to set max and minimum height for voiding, and instead of just air it can do water and lava now too.

## Known Bugs:
- Turning direction using the back key and mouse with the AutoStaircaseDown module causes double block placement. It's fine though for lava flow, just wastes blocks. Block wastage can be avoided by just disabling the module, turning, then re-enabling.
- The antikick with AutoStaircaseFly does not always work when enabling the module while falling.
- With InstaKillBow if all MovePacket buttons are enabled as is default it attempts to send 800 packets. Disable some of them to reduce it.
- TPFly can hurt you once in a while on disable. I tried to prevent this. You also rubberband if going toward a block because it attempts to teleport you through it.
- Top right space in AutoBuildDown cannot be reached. Do not check it.