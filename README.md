[![Github All Releases](https://img.shields.io/github/downloads/etianl/Trouser-Streak/total.svg)]()

<div align="center">
  <h1><strong>Trouser-Streak</strong></h1>
  <p><strong>Official Griefing Utilities of <a href="https://www.youtube.com/@mountainsoflavainc.6913">Mountains of Lava Inc.</a></strong></p>
  <p><strong>Trouser-Streak</strong> is a compilation of modules for <strong><a href="https://meteorclient.com/">Meteor Client</a></strong>.</p> 
  <p><strong><em>Updated to the latest version and optimized for maximum grief!</em></strong></p>
    <p><strong>Customer Service Discord: <a href="https://www.breakblocks.com/discord">https://www.breakblocks.com/discord</a></strong></p>
  <img src="src/main/resources/assets/icon/icon.png" alt="Trouser-Streak Logo" width="28%"/>
</div>

***A few of these modules were not made by me!***
## Credits to the people I skidded from:
In no particular order
- All of these people https://github.com/etianl/Trouser-Streak/graphs/contributors
- [Meteor Client](https://github.com/meteordevelopment/meteor-client)
- [Allah-Hack](https://github.com/TaxEvasiqn/allah-hack)
- [Meteor-Tweaks](https://github.com/Declipsonator/Meteor-Tweaks)
- [Meteor-Rejects](https://github.com/AntiCope/meteor-rejects)
- [Frostburn Client](https://github.com/evaan/FrostBurn)
- [Banana](https://github.com/Bennooo/banana-for-everyone) Credits for checkbox array from AutoBuild, and the idea for TrouserBuild
- [1.17 Crafting Dupe](https://github.com/B2H990/NUMA-117-Crafting-Dupe/)
- [Book And Quill Dupe](https://github.com/Thorioum)
- [InstantKillBow](https://github.com/Saturn5Vfive/InstantKillBow)
- [LecternCrash](https://github.com/Coderx-Gamer/lectern-crash)
- [etianl](https://github.com/etianl/)

**This modpack would not have been possible without you.**

---
# Modules:
*Categorized like they are in the modules menu!*
## TrouserStreak
### **Aim Bot:** 
Locks your view onto the LivingEntity that you were aiming at when the module is activated. (requested by DXRK-0078 in [Issue: 169](https://github.com/etianl/Trouser-Streak/issues/169))
### **AnHero:**
Become An Hero! (Credits to etianl :D)
### **Attribute Swap:** 
Swaps the current main hand item with another item on the hotbar for a single tick when you attack an entity. The attributes from the target item you are swapping to will be applied to the item you are swapping from. (Credits to [DonKisser](https://github.com/DonKisser) for this module! Their inspiration was this video: https://www.youtube.com/watch?v=q99eqD_fBqo)
### **AutoDrop:** 
Drops the stack in your selected slot, or choose a slot to dump. (Credits to etianl :D)
### **AutoLavaCaster:**
Timer based lavacasting bot. Aim at the top of the block you want to cast on and activate the module. (Credits to etianl :D) 

It places lava, then after an amount of time removes the lava, places the water after a specified delay, removes it after a specified delay, it will build the mountain upward, tower you up and repeat. 
 
**AutoLavaCaster Notes:**\
By default the timings are based on the last set of stairs made with AutoMountain! (UseLastMountain mode)
- UseLastMountain timing mode calculates the flow rate correctly if the top of the mountain is a 45 degree angle and the rest is going straight down to the ground.
- Do not disable AutoMountain before done building the mountain you want to cast on, it can break the timing for the above option. Pause by pressing useKey if you intend to make more stairs on that mountain.
- The FortyFiveDegreeStairs timing mode estimates based on 45degree stairs down to sealevel(Y63), or down to Y-60 if you are below Y64.
- The ChooseBottomY timing mode estimates time based on 45degree stairs going down to the Y level you set.
- If you insist upon **not** starting AutoMountain paused, you can get the correct timing for the UseLastLowestBlockfromAutoMountain in AutoLavaCaster by
  1: ENABLE ResetLowestBlockOnDEACTIVATE in AutoLavaCaster and
  2: DISABLE ResetLowestBlockOnACTIVATE in AutoMountain.
  This will return the lowest block placed with AutoMountain until AutoLavacast is used.
- You can reset lowestblock after doing the above by enabling and disabling AutoLavaCaster or by pressing the button in AutoMountain options.
- **Do not use Timer with this module.**
- Rotating your character will break AutoLavaCaster. Disable rotate options in Freecam, Killaura, and any others that will rotate you when casting.
- The timing will break if the server is under 15TPS.
- If using AutoPosition and only slightly standing on a block (as far off the edge as you can get holdingshift) it will break.
### **AutoMountain:** 
Builds stairs for making lavacasts! (Credits to etianl :D)(Loosely based on the AutoStaircase from [Frostburn Client](https://github.com/evaan/FrostBurn))

Includes a "MountainMakerBot" option! https://www.youtube.com/watch?v=EbZs6FZrNjg

**AutoMountain Controls:**
- MouseTurn option on by default allows stairs building to be controlled by look direction.
- UseKey (Right Click) starts and pauses mountain building.
- Left and RightKeys turn stairs building.
- ForwardKey makes stairs up, Back Key makes stairs down.
- JumpKey adjusts spacing of stairs according to the OnDemandSpacing value.
- Start building, then hold SneakKey and also hold Left or RightKey as well to build stairs diagonally. Release left or right key first to continue building in the direction you were prior.
### **AutoStaircase:** 
Builds stairs upward in the direction you are facing by running forward and jumping. (Credits to [majorsopa](https://github.com/majorsopa) for writing the original in the [Frostburn Client](https://github.com/evaan/FrostBurn). I just made it work good)
### **Auto Wither:**
This allows you to build withers with just one click! (Skidded the module from here [Source](https://github.com/Germanminer/MeteorServerUtils/blob/master/src/main/java/com/example/addon/modules/AutoWither.java). Made by [Germanminer](https://github.com/Germanminer)!
Thank them for their efforts!)
### **BetterAutoSign:** 
Automatically writes signs with the text you specify. Includes a "Sign Aura" option which rewrites all the surrounding signs. (Credits to Meteor-Tweaks for BetterAutoSign, and to [stever9487](https://github.com/stever9487) for the Sign Aura based off of Meteor Rejects' Chest Aura.)
### **BetterScaffold:** 
Give you more options for scaffolding such as bigger range. (Credits to Meteor-Tweaks)
### **BlockListMineCommand:** 
Adds a custom Baritone #mine command to your message history containing all the blocks in a blocklist that are near you. Press T then up arrow, then ENTER key to execute the command. BETTER CHAT module is recommended for infinitely long commands. (Credits to etianl :D)
### **BoatKill:** 
Kills passengers in a boat using funny packets. Is patched in Minecraft 1.21.2. (Credits to [filepile](https://github.com/not-filepile) for writing this and [Nxyi](https://github.com/Nxyi) for making it only kill the passenger!)
### **BoatNoclip:**
Allows you to disable clipping of blocks when you are in a boat. Currently only tested to be working in Paper servers of Minecraft version 1.21.11. It likely works in more servers too! (Credits to [aisiaiiad](https://github.com/aisiaiiad): [PR179](https://github.com/etianl/Trouser-Streak/pull/179)
### **Book And Quill Dupe:** 
Overflows data in a book's title to cause dupes and chunk bans. (Credits to [Thorioum](https://github.com/Thorioum)!)
### **BungeeSpoofer:** 
Allows you to join servers with an exposed bungeecord backend. (Thank you [DAMcraft](https://github.com/DAMcraft/MeteorServerSeeker))
### **FlightAntikick:** 
Moves you down on a tick-based timer. Added in due to the lack of a "Normal" mode antikick for velocity Flight in MeteorClient. Bind it to the same key as Flight. (Credits to etianl :D)
### **InfiniteElytra:** 
Automatically toggles Elytra on/off to conserve durability and auto-uses rockets maintaining flight. (Written by etianl with inspiration from this video https://www.youtube.com/watch?v=WYIMsWBxIhI)
### **InfiniteTools:** 
Swaps to a junk version of the same tool you are using to conserve durability. (This was written based on the AttributeSwap module. Thank you to therandomdude for the idea! https://github.com/etianl/Trouser-Streak/issues/134)
###  **Inventory Dupe:** 
Enables a Dupe button which duplicates things in your crafting slots. **Only works on Minecraft servers on version 1.17.** (Credit to ItsVen and Da0neDatGotAway for creation of the dupe, and to B2H990 for making the fabric mod.)
### **InstaKill:** 
Shoots arrows and tridents with incredible power and velocity. (Credits to Saturn5Vfive)
### **InstaMineNuker:** 
Sends packets to instantly mine the blocks around you until they are gone. There is an option in it to make it only target instamineable blocks. (Credits to etianl and to Meteor Client as well as Meteor Rejects for some borrowed code)
### **InstaSafetyBox:** 
Places a box around you for safety using the hardest blocks available in your hotbar. (Credits to etianl :D)
### **ItemTractorBeam:** 
Sucks up items from a very far distance using hunger points. Only works well for items on the same Y level. (Code based off of the InstaKill by Saturn5Vfive!)
### **LavaAura:** 
Automatically lava buckets an entity's position on a tick delay, or sets the entity on fire. Also has the option of placing on every block face. (Credits to etianl :D)
### **LecternCrash:** 
Crash 1.18.X vanilla servers and possibly below using lecterns. (Credits to Coderx-Gamer)
### **MaceKill:** 
Makes Maces do high damage by spoofing a teleport up then back down. (Credits to etianl :D)
### **MultiUse:** 
Does the item use action more than once per item use. (Credits to [maytrixc](https://github.com/maytrixc))
### **Packet Delay:**
Delays sent and received packets by a certain amount of ticks. Part of this code was skidded from Meteor Client's Packet Canceller module. (requested by DXRK-0078 in [Issue: 169](https://github.com/etianl/Trouser-Streak/issues/169))
### **PlayerJoinedAlarm:** 
Makes noise when a player joins the server or if they enter render distance. (Credits to etianl :D)
### **PortalGodMode:** 
Makes you invincible after you walk through a portal. You will not be able to move while invincible until you disable the module.
### **RedstoneNuker:** 
It's just the regular Nuker module from Meteor client, customized to only break things that generate redstone signals. Also with included AutoTool. Keeps you safer when placing lots of TNT. (Credits to Meteor Client for the borrowed code.)
### **RemoteEnderChest:** 
This module allows you to store an ender chest GUI far away from the original chest, allowing you to store or take things out wherever. (Thank you to Mr. Skills93 for the idea. Credits to etianl for the module. :D)

You can even destroy the ender chest and still retain it's menu!

**Opening your inventory, pressing the Escape key, or transitioning through portals will close the GUI and break your link with the Ender Chest!!!**

To allow normal player movement many controls had to be routed around this GUI.
*This can make for some wacky behaviour sometimes*

**Video demonstration: https://www.youtube.com/watch?v=Bv48zq_w58Y**

**Keys that have been routed past the GUI currently:**
- Meteor Module hotkeys
- Attack/Mining
- Use (kinda janky because we had to implement interactItem/Block/Entity)
- Forward
- Back
- Left
- Right
- Jump
- Sneak
- Sprint
- 1-9 keys for hotbar
- Scroll Wheel up/down hotbar cycling
  
**Keys I would like to implement but HAVE NOT:**
- SwapHand (default F)
- DropItem (default Q)
- Chat (default T)
- Some of the F keys, I noticed F5 doesn't work.
### **ShulkerDupe:** 
Duplicate the contents of a shulker when pressing "Dupe" in the shulker menu. Only works on Vanilla, Forge, and Fabric servers 1.19 and below. (Credits to Allah-Hack, I just made the buttons, and make it dupe slot1 as well.)
### **SpearKill:** 
Increases spear damage! Lunge mode uses increased velocity and Blink mode delays packets allowing normal movement. Thank you to [Kimtaeho](https://github.com/needitem) for Blink mode! Credits to etianl for the Lunge mode. :D
### **StorageLooter:** 
Automatically steals stuff from storage containers, and can put junk items in there too. (Credits to etianl :D)
### **SuperInstaMine:** 
Based on the Meteor Rejects Instamine. I added an option called "Break Modes (Range)" which allows you to break more than one block at a time. (Credits to Meteor Rejects for the original.)
### **Teleport:** 
Long range clickTP. (Credits to etianl :D)
### **TPFly:** 
A purely setPosition based flight. Does not use any added velocity. (Credits to etianl :D)
### **TrailMaker:** 
Leaves blocks behind you in a trail. (Credits to etianl :D)
### **TrouserBuild:** 
Build according to a 5x5 grid centered on the block you are aiming at. Right click to build. (Credits to etianl, and to Banana for the checkboxes and idea. :D)

---
## TrouserHunting
### **ActivatedSpawnerDetector:** 
Detects if a player was ever near a spawner or trial spawner block. (Credits to etianl :D)
### **AdvancedItemESP:** 
Detects any individual item you are searching for. (Made based on the MobGearESP module made by [windoid](https://github.com/windoid))
### **BaseFinder:** 
Automatically detects if un-natural things are in a chunk by checking every block and entity. (Credits to etianl :D, and to Meteor-Rejects for some code from newchunks.)

- The Blocks Lists have been tuned to reduce any false positives while throwing the maximum amount of "good" results for builds. Adjust if you feel you need to.
- The Number of Blocks to Find options is the total amount any of the blocks from one of the lists to find before throwing a base coord.
### **CaveDisturbanceDetector:** 
Scans for single air blocks within the cave air blocks found in caves and underground structures in 1.13+ chunks. (Credits to etianl :D)
### **CollectibleESP:** 
Highlights collectible items in item frames and banners! (Credits to [xqyet](https://github.com/xqyet), modified by etianl)
### **Hole/Tunnel/StairsESP:** 
Detects 1x1 holes going straight down, horizontal tunnels, and staircase tunnels. (Thank you to Meteor Client for some code from TunnelESP)
### **MobGearESP:** 
ESP Module that highlights mobs likely wearing player gear. (Thank you to [windoid](https://github.com/windoid) for this!)
### **NewerNewChunks:**
NewChunks module with new newchunk estimation exploits, and the ability to save chunk data for later! Comes with several new homebrewed newchunks methods made by yours truly. (Credits to Meteor Rejects, and BleachHack from where it was ported, and to etianl for updating it and adding new things! :D.)

***NewerNewChunks Notes:***
- NewerNewChunks stores your NewChunks data as text files seperately per server and per dimension in the TrouserStreak/NewChunks folder in your Minecraft folder. This enables you to chunk trace multiple different servers and dimensions without mixing NewChunks data.
- If the game crashes, chunk data is saved! No loss in tracing progress.

**Palette Exploit Notes:**
- The **PaletteExploit** option enabled by default detects new chunks by scanning the order of chunk section palettes.
- It also highlights chunks that are being updated from an old version of minecraft.
- Chunk are sometimes defined as "Old" by the server in an area a little larger than the render distance sent to the client. 
- Chunks appear to be defined as new until the person who generated them has unrendered them.
- The chunks that stay loaded due to the spawn chunk region always show up as new for some reason.
- In the Overworld dimension there are rare false positives.
- **PaletteExploit** does not work in Minecraft servers where their version is less than 1.18! For those servers, disable **PaletteExploit** and enable Liquid flow and BlockExploit.

**There is also detection options for chunks generated in old versions!**

**Default Color Descriptions:**\
**Red:** New chunk, never loaded before.\
**Green:** Old chunk, only loaded in 1.18 or after.\
**Yellow-Green:** Old Generation chunk, only loaded in 1.17 or before for OVERWORLD, 1.13 or before in END, or 1.15 or before in NETHER. Has been visited since the update.\
**Orange-Yellow:** Old chunk (1.17 or before) being currently updated to 1.18 or after. You are the first to visit since the update.

**More Detection Methods:**\
*for use if **PaletteExploit** does not work*
- The **LiquidExploit** option estimates possible newchunks based on liquid being just starting to flow for the first time.
- The **BlockUpdateExploit** option helps to estimate possible newchunks based on block update packets. SOME OF THESE CHUNKS MAY BE OLD.
- The **BlockUpdateExploit** option can produce false positives if you are hanging around in the same location for a while. It's best to keep moving for it to work best.\

**Chunk Detection Modes:**
- The **"BlockExploitMode"** will render BlockExploit chunks as their own color instead of a newchunk (Normal mode rendering).
- When using BlockExploitMode mode if the BlockUpdateExploit chunks appear infrequently and are combined with Old Chunks, then the chunks you are in are OLD. If there is alot of BlockUpdateExploit chunks appearing and/or they are mixed with NewChunks then the chunks are NEW.
- The **"IgnoreBlockExploit"** will render BlockExploit chunks as an oldchunk instead of a newchunk.
### **NoSpawnerDetector:** 
Detect Dungeons and Mineshafts which have had their spawners removed. (Credits to etianl :D)
### **OnlinePlayerActivityDetector:** 
Detects if an online player is nearby if there are blocks missing from a BlockState palette and your render distances are overlapping. It can detect players that are outside of render distance. (Credits to etianl :D)
### **PortalPatternFinder:** 
Scans for the imprints of Nether Portals within the cave air blocks found in caves and underground structures in 1.13+ chunks. **May be useful for finding portal skips in the Nether**. (Credits to etianl :D)
### **PotESP:** 
Detects Decorated Pots with un-natural contents. (Credits to etianl :D)
### **WaypointCoordExploit:** 
Triangulate player locations with the new waypoint system introduced in Minecraft 1.21.6. Walk perpendicular to the waypoint a bit to get the coordinate. There is also a hud element that you can use to display the coordinates. (Credits to etianl :D)

---
## TrouserOP/Creative
*Everything here requires creative mode or operator status!*
### **Airstrike+:** 
Rains down entities from the sky. (Credits to Allah-Hack for the original, modified by etianl) **CREATIVEMODE**
### **AutoCommand:** 
Run a list of operator commands at the push of a button! (Credits to [aaaasdfghjkllll](https://github.com/aaaasdfghjkllll)) **OPERATOR**
### **AutoDisplays:** 
Spams block displays around all player's heads to blind them or text displays around them for trolling and advertising. (Credits to etianl :D) **OPERATOR**
### **AutoNames** 
Change player name colors, prefix, suffix in tab and chat. Credits to [DedicateDev](https://github.com/DedicateDev) **OPERATOR**
### **AutoScoreboard:** 
Creates a custom scoreboard. Useful for advertising. Credits to [aaaasdfghjkllll](https://github.com/aaaasdfghjkllll) **OPERATOR**
### **AutoTexts:** 
Spawns text around you by creating invisible armor stands. Thank you to [DedicateDev](https://github.com/DedicateDev)! **CREATIVEMODE**
### **AutoTitles:** 
Displays text across the screen of all the individuals who are online on a server. Thank you [ogmur](https://www.youtube.com/@Ogmur) for figuring out the commands. **OPERATOR**
### **Boom+:** 
Throws entities or spawns them on the targeted block when you click (Credits to Allah-Hack for the original, modified by etianl) **CREATIVEMODE**
### **ExplosionAura:** 
Spawns creepers at your position as you move that explode instantly. (Credits to etianl :D) **CREATIVEMODE**
### **ForceOPBook:** 
Create malicious books that can execute commands when clicked. Requires creative mode for you to make them, and requires you to give it to an operator and have them click it. (This is an old method, but credits to etianl for writing this implementation.) **CREATIVEMODE**
### **ForceOPSign:** 
Create malicious signs that can execute commands when clicked. Requires creative mode for you to make them, and requires you to give it to an operator and have them place and click it while they are also in creative mode. The signs placed appear blank and commands executed may not appear in the server chat response for commands.\
(Credits to CrushedPixel for their first implementation of a ForceOP sign module https://www.youtube.com/watch?v=KofDNaPZWfg, and to etianl for writing this implementation.) **CREATIVEMODE**
### **HandOfGod:** 
Runs the "/fill" command in many different ways. Destroy worlds with ease! (Credits to etianl :D) **OPERATOR**
### **MultiverseAnnihilator:** 
Deletes every world listed on a server that has the Multiverse plugin installed. (credits to [ogmur](https://www.youtube.com/@Ogmur) for writing this) **OPERATOR**
### **NbtEditor:** 
Generates items with modified NBT data.  (Credits to etianl :D) **CREATIVEMODE**
### **OPplayerTPmodule:** 
Uses operator commands to teleport you to each player online one by one at the push of a button, or those players to you one by one. (Thank you to [ogmur](https://www.youtube.com/@Ogmur) for the idea.) **OPERATOR**
### **OPServerKillModule:** 
Runs a set of operator commands to disable a server and cover up the tracks of the kill command. (Credits to etianl :D) **OPERATOR**
### **Voider+:** 
Runs /fill commands from the top down. (Credits to Allah-Hack for the original, modified by etianl) **OPERATOR**

---
## Commands
### **.autovclip:** 
Automatically selects the nearest two block gap going either up or down to vclip into. (Credits to the original [AutoVclip](https://github.com/kittenvr/AutoVclip) for minecraft 1.19.2 which inspired me to make this one.)
### **.autovaultclip:**
Same as .autovclip but it can teleport further vertical distances. For Paper servers only.
### **.castertimer:**
Tells you how long each AutoLavaCaster cycle has been running for.
### **.cleanram:**
Cleans the RAM of useless junk and may be handy for clearing lag. (credits to [ogmur](https://www.youtube.com/@Ogmur) for writing this)
### **.crash:** 
Crashes the client of a single or all other players using a nasty particle effect. **Requires OP status.** Credits to [aaaasdfghjkllll](https://github.com/aaaasdfghjkllll)
### **.lavacalc:** 
Gives you an approximation of how long lava will take to flow from top to bottom across a 45 degree staircase at 20TPS (input numbers), or the last Mountain you made from your Y level.
### **.text:**
Very similar to the **AutoTexts** module. The difference with this one is that it can display many lines one after the other. Thank you to [DedicateDev](https://github.com/DedicateDev)!
- The command is accessed by typing .text followed by one of the subcommands. You can choose load or save a preset set of text.
- Some example commands would be ".text load mountains" or ".text save mountains #green [ #dark_red Trolled! #green ]|#gold Mountains of Lava Inc.|#red Youtube: #blue www.youtube.com/@mountainsoflavainc.6913"
- Individual lines are seperated by a | (pipe character) and you can also use #red "wordhere" to format the color of the text.
- You can obfuscate words by using #obfuscated
### **.viewnbt:** 
Returns the nbt data for the item in your hand. There is also a Save option that saves the data to a text file. (Credits to etianl :D)
### **.world** 
Tells you some info about the server like world border coordinates, sometimes the players that have played there (does not work on all servers), and other things. (Credits to etianl :D)

## Requirements:
- Meteor Client https://meteorclient.com/
- Please try [ViaFabricPlus](https://github.com/FlorianMichael/ViaFabricPlus) to connect to old servers from a new version client.

plz give me star on githoob kthx