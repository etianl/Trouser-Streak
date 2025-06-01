package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Flight;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import pwn.noobs.trouserstreak.Trouser;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class HandOfGod extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgClick = settings.createGroup("Click Options");
    private final SettingGroup sgPcentered = settings.createGroup("Player-Centered Options");
    private final SettingGroup sgTroll = settings.createGroup("Troll Other Players!");
    private final SettingGroup sgTerminate = settings.createGroup("Troll Non-Player Entities!");

    private final Setting<Boolean> disconnectdisable = sgGeneral.add(new BoolSetting.Builder()
            .name("Disable on Disconnect")
            .description("Disables module on disconnecting")
            .defaultValue(false)
            .build());
    public final Setting<Boolean> notOP = sgGeneral.add(new BoolSetting.Builder()
            .name("Permission Level Hold")
            .description("Prevents the code running until you get the correct permission level.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> autosave = sgGeneral.add(new BoolSetting.Builder()
            .name("AutoSave and CTRL+S shortcut")
            .description("For saving your progress incase of server shutdown.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Integer> autosavedelay = sgGeneral.add(new IntSetting.Builder()
            .name("AutoSave Delay (Seconds)")
            .description("How many seconds between saving the world.")
            .defaultValue(20)
            .min(1)
            .sliderMax(60)
            .build());

    private final Setting<Block> block = sgClick.add(new BlockSetting.Builder()
            .name("ClickBlock")
            .description("What is created when clicking (Default: Lava)")
            .defaultValue(Blocks.LAVA)
            .build());
    public final Setting<Boolean> replace = sgClick.add(new BoolSetting.Builder()
            .name("Replace Blocks")
            .description("Replace certain blocks in the selection instead of all blocks")
            .defaultValue(false)
            .build()
    );
    private final Setting<Block> blocktoreplace = sgClick.add(new BlockSetting.Builder()
            .name("Block to Replace.")
            .description("What is replaced when clicking or using the Sweep Away option")
            .defaultValue(Blocks.GRASS_BLOCK)
            .visible(replace::get)
            .build());

    private final Setting<Integer> cwidth = sgClick.add(new IntSetting.Builder()
            .name("ClickWidth")
            .description("The width of the click fill")
            .defaultValue(17)
            .min(1)
            .sliderMax(30)
            .build());

    private final Setting<Integer> cheight = sgClick.add(new IntSetting.Builder()
            .name("ClickHeight")
            .description("The height of the click fill")
            .defaultValue(11)
            .min(1)
            .sliderMax(30)
            .build());
    private final Setting<Integer> cdepth = sgClick.add(new IntSetting.Builder()
            .name("ClickDepth")
            .description("The depth of the click fill")
            .defaultValue(17)
            .min(1)
            .sliderMax(30)
            .build());
    public final Setting<Boolean> lightning = sgClick.add(new BoolSetting.Builder()
            .name("Lightning")
            .description("Lightning on/off")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> auto = sgClick.add(new BoolSetting.Builder()
            .name("FULLAUTO")
            .description("FULL AUTO BABY!")
            .defaultValue(false)
            .build()
    );
    public final Setting<Integer> atickdelay = sgClick.add(new IntSetting.Builder()
            .name("FULLAUTOTickDelay")
            .description("Tick Delay for FULLAUTO option.")
            .defaultValue(2)
            .min(0)
            .sliderMax(20)
            .visible(auto::get)
            .build()
    );
    public final Setting<Boolean> fluids = sgClick.add(new BoolSetting.Builder()
            .name("IncludeFluids")
            .description("Includes fluids when targeting, or not.")
            .defaultValue(false)
            .build()
    );
    public final Setting<Boolean> SwpAway = sgClick.add(new BoolSetting.Builder()
            .name("Sweep Away")
            .description("Right Click to sweep the whole world away")
            .defaultValue(false)
            .build()
    );
    private final Setting<Block> sweepblock = sgClick.add(new BlockSetting.Builder()
            .name("SweepBlock")
            .description("What is created when sweeping (Default: Air)")
            .defaultValue(Blocks.AIR)
            .visible(SwpAway::get)
            .build());
    private final Setting<Integer> sweepradius = sgClick.add(new IntSetting.Builder()
            .name("SweepAwayRadius")
            .description("radius")
            .defaultValue(90)
            .sliderRange(1, 90)
            .visible(SwpAway::get)
            .build());
    private final Setting<Integer> sweepstart = sgClick.add(new IntSetting.Builder()
            .name("SweepAwayStartingDistance")
            .description("Starting distance from character for the sweeper.")
            .defaultValue(3)
            .sliderRange(1, 30)
            .visible(SwpAway::get)
            .build());
    public final Setting<Boolean> pReplace = sgPcentered.add(new BoolSetting.Builder()
            .name("Replace Blocks")
            .description("Replace certain blocks in the selection instead of all blocks")
            .defaultValue(false)
            .build()
    );
    private final Setting<Block> pblocktoreplace = sgPcentered.add(new BlockSetting.Builder()
            .name("Block to Replace.")
            .description("What is replaced around the character")
            .defaultValue(Blocks.GRASS_BLOCK)
            .visible(pReplace::get)
            .build());
    private final Setting<Block> pblock = sgPcentered.add(new BlockSetting.Builder()
            .name("Replacement Block")
            .description("What is put in place of the replaced blocks (Default: Lava)")
            .defaultValue(Blocks.LAVA)
            .visible(pReplace::get)
            .build());
    public final Setting<Boolean> rndplyr = sgPcentered.add(new BoolSetting.Builder()
            .name("NukeAroundPlayer")
            .description("Runs /fill air around you every tick.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Integer> pwidth = sgPcentered.add(new IntSetting.Builder()
            .name("PlayerWidth")
            .description("Width /fill'd around player")
            .defaultValue(17)
            .min(1)
            .sliderMax(30)
            .visible(pReplace::get)
            .build()
    );
    public final Setting<Integer> pheight = sgPcentered.add(new IntSetting.Builder()
            .name("PlayerHeight")
            .description("Height /fill'd around player")
            .defaultValue(11)
            .min(1)
            .sliderMax(30)
            .visible(pReplace::get)
            .build()
    );
    public final Setting<Integer> pdepth = sgPcentered.add(new IntSetting.Builder()
            .name("PlayerDepth")
            .description("Depth /fill'd around player")
            .defaultValue(17)
            .min(1)
            .sliderMax(30)
            .visible(pReplace::get)
            .build()
    );
    public final Setting<Integer> tickdelay = sgPcentered.add(new IntSetting.Builder()
            .name("TickDelayAroundPlayer")
            .description("Tick Delay for running /fill around the player.")
            .defaultValue(2)
            .min(0)
            .sliderMax(100)
            .visible(pReplace::get)
            .build()
    );
    public final Setting<Boolean> mgcersr = sgPcentered.add(new BoolSetting.Builder()
            .name("MagicEraser")
            .description("FLY SLOW FOR IT TO WORK CORRECTLY. Runs /fill air in the shape of a wall infront of you every tick.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Integer> mgcradius = sgPcentered.add(new IntSetting.Builder()
            .name("MagicEraserRadius")
            .description("radius")
            .defaultValue(30)
            .sliderRange(1, 90)
            .visible(mgcersr::get)
            .build());
    private final Setting<Integer> mgcdist = sgPcentered.add(new IntSetting.Builder()
            .name("MagicEraserDistance")
            .description("Distance from player which the layer is /fill'ed")
            .defaultValue(5)
            .sliderRange(1, 30)
            .visible(mgcersr::get)
            .build());

    public final Setting<Boolean> voider = sgPcentered.add(new BoolSetting.Builder()
            .name("VoiderAura")
            .description("Runs /fill on a single layer to your specified radius in a range from above your head to beneath your feet.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Integer> radius = sgPcentered.add(new IntSetting.Builder()
            .name("radius")
            .description("radius")
            .defaultValue(90)
            .sliderRange(1, 90)
            .visible(voider::get)
            .build());
    private final Setting<Integer> vrange = sgPcentered.add(new IntSetting.Builder()
            .name("VerticalRange")
            .description("How Far vertically from player to void.")
            .defaultValue(5)
            .sliderRange(1, 20)
            .visible(voider::get)
            .build());
    public final Setting<Boolean> roofer = sgPcentered.add(new BoolSetting.Builder()
            .name("Roofer")
            .description("Runs /fill on the world at a set height")
            .defaultValue(false)
            .build()
    );
    private final Setting<Block> roofblock = sgPcentered.add(new BlockSetting.Builder()
            .name("RooferBlock")
            .description("What is created.")
            .defaultValue(Blocks.OBSIDIAN)
            .visible(roofer::get)
            .build());
    private final Setting<Integer> roofradius = sgPcentered.add(new IntSetting.Builder()
            .name("radius")
            .description("radius")
            .defaultValue(90)
            .sliderRange(1, 90)
            .visible(roofer::get)
            .build());
    private final Setting<Integer> roofheight = sgPcentered.add(new IntSetting.Builder()
            .name("height")
            .description("height")
            .defaultValue(255)
            .sliderRange(64, 319)
            .visible(roofer::get)
            .build());
    public final Setting<Integer> rooftickdelay = sgPcentered.add(new IntSetting.Builder()
            .name("TickDelay")
            .description("Tick Delay for running /fill.")
            .defaultValue(20)
            .min(0)
            .sliderMax(100)
            .visible(roofer::get)
            .build()
    );
    public final Setting<Boolean> troll = sgTroll.add(new BoolSetting.Builder()
            .name("/fill Around All Other Players")
            .description("Runs /fill on all player locations around you")
            .defaultValue(false)
            .build()
    );
    public final Setting<Boolean> trollrenderdist = sgTroll.add(new BoolSetting.Builder()
            .name("Only Render Distance")
            .description("Run /fill on players only within render distance.")
            .defaultValue(false)
            .visible(troll::get)
            .build()
    );
    public final Setting<Boolean> trollfriends = sgTroll.add(new BoolSetting.Builder()
            .name("/fill Friends")
            .description("Runs /fill around friends too")
            .defaultValue(false)
            .visible(troll::get)
            .build()
    );
    public final Setting<Integer> trolltickdelay = sgTroll.add(new IntSetting.Builder()
            .name("TickDelayAroundOtherPlayers")
            .description("Tick Delay for running /fill around other players.")
            .defaultValue(0)
            .min(0)
            .sliderMax(100)
            .visible(troll::get)
            .build()
    );
    private final Setting<Block> trollblock = sgTroll.add(new BlockSetting.Builder()
            .name("OtherPlayersBlock")
            .description("What is created around the players (Default: Lava)")
            .defaultValue(Blocks.LAVA)
            .visible(troll::get)
            .build());
    public final Setting<Boolean> trollreplace = sgTroll.add(new BoolSetting.Builder()
            .name("Replace Blocks")
            .description("Replace certain blocks in the selection instead of all blocks")
            .defaultValue(false)
            .visible(troll::get)
            .build()
    );
    private final Setting<Block> trollblocktoreplace = sgTroll.add(new BlockSetting.Builder()
            .name("Block to Replace.")
            .description("What is replaced around the players.")
            .defaultValue(Blocks.GRASS_BLOCK)
            .visible(() -> troll.get() && trollreplace.get())
            .build());
    public final Setting<Integer> trollwidth = sgTroll.add(new IntSetting.Builder()
            .name("OtherPlayerWidth")
            .description("Width /fill'd around player")
            .defaultValue(17)
            .min(1)
            .sliderMax(30)
            .visible(troll::get)
            .build()
    );
    public final Setting<Integer> trollheight = sgTroll.add(new IntSetting.Builder()
            .name("OtherPlayerHeight")
            .description("Height /fill'd around player")
            .defaultValue(11)
            .min(1)
            .sliderMax(30)
            .visible(troll::get)
            .build()
    );
    public final Setting<Integer> trolldepth = sgTroll.add(new IntSetting.Builder()
            .name("OtherPlayerDepth")
            .description("Depth /fill'd around player")
            .defaultValue(17)
            .min(1)
            .sliderMax(30)
            .visible(troll::get)
            .build()
    );
    public final Setting<Boolean> terminate = sgTerminate.add(new BoolSetting.Builder()
            .name("/fill Around All Non-Player Entities")
            .description("Runs /fill on all entity locations in the world")
            .defaultValue(false)
            .build()
    );
    public final Setting<Boolean> terminaterenderdist = sgTerminate.add(new BoolSetting.Builder()
            .name("Only Render Distance")
            .description("Run /fill on entities only within render distance.")
            .defaultValue(false)
            .visible(terminate::get)
            .build()
    );
    public final Setting<Integer> terminatetickdelay = sgTerminate.add(new IntSetting.Builder()
            .name("TickDelayAroundEntities")
            .description("Tick Delay for running /fill around the entities.")
            .defaultValue(0)
            .min(0)
            .sliderMax(100)
            .visible(terminate::get)
            .build()
    );
    private final Setting<Block> terminateblock = sgTerminate.add(new BlockSetting.Builder()
            .name("EntitiesBlock")
            .description("What is created around the Entities (Default: Lava)")
            .defaultValue(Blocks.LAVA)
            .visible(terminate::get)
            .build());
    public final Setting<Boolean> terminatereplace = sgTerminate.add(new BoolSetting.Builder()
            .name("Replace Blocks")
            .description("Replace certain blocks in the selection instead of all blocks")
            .defaultValue(false)
            .visible(terminate::get)
            .build()
    );
    private final Setting<Block> terminateblocktoreplace = sgTerminate.add(new BlockSetting.Builder()
            .name("Block to Replace.")
            .description("What is replaced around the entities.")
            .defaultValue(Blocks.GRASS_BLOCK)
            .visible(() -> terminate.get() && terminatereplace.get())
            .build());
    public final Setting<Integer> terminatewidth = sgTerminate.add(new IntSetting.Builder()
            .name("EntityFillWidth")
            .description("Width /fill'd around entities")
            .defaultValue(2)
            .min(1)
            .sliderMax(30)
            .visible(terminate::get)
            .build()
    );
    public final Setting<Integer> terminateheight = sgTerminate.add(new IntSetting.Builder()
            .name("EntityFillHeight")
            .description("Height /fill'd around entities")
            .defaultValue(2)
            .min(1)
            .sliderMax(30)
            .visible(terminate::get)
            .build()
    );
    public final Setting<Integer> terminatedepth = sgTerminate.add(new IntSetting.Builder()
            .name("EntityFillDepth")
            .description("Depth /fill'd around Entities")
            .defaultValue(2)
            .min(1)
            .sliderMax(30)
            .visible(terminate::get)
            .build()
    );
    public HandOfGod() {
        super(Trouser.operator, "HandOfGod", "Modify the world and troll players with automated /fill commands. (Must have OP status)");
    }
    private CopyOnWriteArrayList<PlayerListEntry> players;
    private int ticks=0;
    private int swpr=0;
    private boolean sweep=false;
    private int asaveticks=0;
    private int aticks=0;
    private int errticks=0;
    private int roofticks=0;
    private int trollticks=0;
    private int terminateticks=0;

    private int pX;
    private int pY;
    private int pZ;
    private int sX;
    private int sY;
    private int sZ;
    int i;
    public int adjustYValue(int yValue) {
        int bottomY = mc.world.getBottomY();
        int topY = mc.world.getTopYInclusive();

        if (yValue < bottomY) {
            return bottomY;
        } else if (yValue > topY) {
            return topY;
        } else {
            return yValue;
        }
    }
    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (disconnectdisable.get() && event.screen instanceof DisconnectedScreen) {
            toggle();
        }
    }
    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (disconnectdisable.get())toggle();
    }
    @Override
    public void onActivate() {
        roofticks=0;
        aticks=0;
        ticks=0;
        trollticks=0;
        terminateticks=0;
        if (voider.get()){
            i=mc.player.getBlockPos().getY();
        }
    }

    @EventHandler
    private void onMouseButton(MouseButtonEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (notOP.get() && !(mc.player.hasPermissionLevel(2)) && mc.world.isChunkLoaded(mc.player.getChunkPos().x, mc.player.getChunkPos().z)) {
            return;
        }
        if (mc.options.attackKey.isPressed() && mc.currentScreen == null && mc.interactionManager != null) {
            HitResult hr = mc.cameraEntity.raycast(900, 0, fluids.get());
            Vec3d god = hr.getPos();
            BlockPos pos = BlockPos.ofFloored(god);
            if (lightning.get()) {
                ItemStack rst = mc.player.getMainHandStack();
                BlockHitResult bhr = new BlockHitResult(mc.player.getEyePos(), Direction.DOWN, BlockPos.ofFloored(mc.player.getEyePos()), false);
                ItemStack Lightning = new ItemStack(Items.SALMON_SPAWN_EGG);
                var changes = ComponentChanges.builder()
                        .add(DataComponentTypes.ENTITY_DATA, createEntityData(pos))
                        .build();
                Lightning.applyChanges(changes);
                mc.interactionManager.clickCreativeStack(Lightning, 36 + mc.player.getInventory().selectedSlot);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
            }
            String fullString = block.get().toString();
            String[] parts = fullString.split(":");
            String block = parts[1];
            String blockName = block.replace("}", "");
            String repfullString = blocktoreplace.get().toString();
            String[] repparts = repfullString.split(":");
            String repblock = repparts[1];
            String repblockName = repblock.replace("}", "");
            switch (mc.player.getHorizontalFacing()){
                case NORTH, SOUTH -> {
                    int x1 = pos.getX() + cwidth.get();
                    int y1 = pos.getY() + cheight.get();
                    int z1 = pos.getZ() + cdepth.get();
                    int x2 = pos.getX() - cwidth.get();
                    int y2 = pos.getY() - cheight.get();
                    int z2 = pos.getZ() - cdepth.get();
                    if (!replace.get()) ChatUtils.sendPlayerMsg("/fill " + x1 + " " + adjustYValue(y1) + " " + z1 + " " + x2 + " " + adjustYValue(y2) + " " + z2 + " " + blockName);
                    else if (replace.get()) ChatUtils.sendPlayerMsg("/fill " + x1 + " " + adjustYValue(y1) + " " + z1 + " " + x2 + " " + adjustYValue(y2) + " " + z2 + " " + blockName + " replace " + repblockName);
                }
                case EAST, WEST -> {
                    int x1 = pos.getX() + cdepth.get();
                    int y1 = pos.getY() + cheight.get();
                    int z1 = pos.getZ() + cwidth.get();
                    int x2 = pos.getX() - cdepth.get();
                    int y2 = pos.getY() - cheight.get();
                    int z2 = pos.getZ() - cwidth.get();
                    if (!replace.get()) ChatUtils.sendPlayerMsg("/fill " + x1 + " " + adjustYValue(y1) + " " + z1 + " " + x2 + " " + adjustYValue(y2) + " " + z2 + " " + blockName);
                    else if (replace.get()) ChatUtils.sendPlayerMsg("/fill " + x1 + " " + adjustYValue(y1) + " " + z1 + " " + x2 + " " + adjustYValue(y2) + " " + z2 + " " + blockName + " replace " + repblockName);
                }
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        if (notOP.get() && !(mc.player.hasPermissionLevel(2)) && mc.world.isChunkLoaded(mc.player.getChunkPos().x, mc.player.getChunkPos().z)) {
            return;
        }
        pX=mc.player.getBlockPos().getX();
        pY=mc.player.getBlockPos().getY();
        pZ=mc.player.getBlockPos().getZ();
        if (autosave.get()){
            asaveticks++;
            if (asaveticks>=autosavedelay.get()*20){
                ChatUtils.sendPlayerMsg("/save-all");
                asaveticks=0;
            }
            if (mc.options.sneakKey.isPressed() && mc.options.backKey.isPressed()){
                ChatUtils.sendPlayerMsg("/save-all");
                asaveticks=0;
            }
        }
        if (auto.get() && mc.options.attackKey.isPressed() && mc.currentScreen == null) {
            if (aticks<=atickdelay.get()){
                aticks++;
            } else if (aticks>atickdelay.get()){
                HitResult hr = mc.cameraEntity.raycast(900, 0, fluids.get());
                Vec3d god = hr.getPos();
                BlockPos pos = BlockPos.ofFloored(god);
                if (lightning.get()) {
                    ItemStack rst = mc.player.getMainHandStack();
                    BlockHitResult bhr = new BlockHitResult(mc.player.getEyePos(), Direction.DOWN, BlockPos.ofFloored(mc.player.getEyePos()), false);
                    ItemStack Lightning = new ItemStack(Items.SALMON_SPAWN_EGG);
                    var changes = ComponentChanges.builder()
                            .add(DataComponentTypes.ENTITY_DATA, createEntityData(pos))
                            .build();
                    Lightning.applyChanges(changes);

                    mc.interactionManager.clickCreativeStack(Lightning, 36 + mc.player.getInventory().selectedSlot);
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                    mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                }
                String fullString = block.get().toString();
                String[] parts = fullString.split(":");
                String block = parts[1];
                String blockName = block.replace("}", "");
                String repfullString = blocktoreplace.get().toString();
                String[] repparts = repfullString.split(":");
                String repblock = repparts[1];
                String repblockName = repblock.replace("}", "");
                switch (mc.player.getHorizontalFacing()){
                    case NORTH, SOUTH -> {
                        int x1 = pos.getX() + cwidth.get();
                        int y1 = pos.getY() + cheight.get();
                        int z1 = pos.getZ() + cdepth.get();
                        int x2 = pos.getX() - cwidth.get();
                        int y2 = pos.getY() - cheight.get();
                        int z2 = pos.getZ() - cdepth.get();
                        if (!replace.get()) ChatUtils.sendPlayerMsg("/fill " + x1 + " " + adjustYValue(y1) + " " + z1 + " " + x2 + " " + adjustYValue(y2) + " " + z2 + " " + blockName);
                        else if (replace.get()) ChatUtils.sendPlayerMsg("/fill " + x1 + " " + adjustYValue(y1) + " " + z1 + " " + x2 + " " + adjustYValue(y2) + " " + z2 + " " + blockName + " replace " + repblockName);
                    }
                    case EAST, WEST -> {
                        int x1 = pos.getX() + cdepth.get();
                        int y1 = pos.getY() + cheight.get();
                        int z1 = pos.getZ() + cwidth.get();
                        int x2 = pos.getX() - cdepth.get();
                        int y2 = pos.getY() - cheight.get();
                        int z2 = pos.getZ() - cwidth.get();
                        if (!replace.get()) ChatUtils.sendPlayerMsg("/fill " + x1 + " " + adjustYValue(y1) + " " + z1 + " " + x2 + " " + adjustYValue(y2) + " " + z2 + " " + blockName);
                        else if (replace.get()) ChatUtils.sendPlayerMsg("/fill " + x1 + " " + adjustYValue(y1) + " " + z1 + " " + x2 + " " + adjustYValue(y2) + " " + z2 + " " + blockName + " replace " + repblockName);
                    }
                }
                aticks=0;
            }
        }
        if (SwpAway.get()){
            if (mc.options.useKey.isPressed()){
                sweep=true;
            }else if (!mc.options.useKey.isPressed()) sweep=false;
            if (!sweep){
                sX=mc.player.getBlockX();
                sY=mc.player.getBlockY();
                sZ=mc.player.getBlockZ();
            }
            if (sweep){
                String sfullString = sweepblock.get().toString();
                String[] sparts = sfullString.split(":");
                String sblock = sparts[1];
                String sblockName = sblock.replace("}", "");
                String repfullString = blocktoreplace.get().toString();
                String[] repparts = repfullString.split(":");
                String repblock = repparts[1];
                String repblockName = repblock.replace("}", "");
                switch (mc.player.getHorizontalFacing()){
                    case NORTH -> {
                        if (!replace.get())ChatUtils.sendPlayerMsg("/fill " + (sX - sweepradius.get()) + " " + adjustYValue((sY-sweepradius.get())) + " "+ (sZ - (sweepstart.get()+swpr)) +" "+ (sX + sweepradius.get()) + " " + adjustYValue((sY +sweepradius.get())) +" "+ (sZ - (sweepstart.get()+swpr)) + " " + sblockName);
                        else if (!replace.get())ChatUtils.sendPlayerMsg("/fill " + (sX - sweepradius.get()) + " " + adjustYValue((sY-sweepradius.get())) + " "+ (sZ - (sweepstart.get()+swpr)) +" "+ (sX + sweepradius.get()) + " " + adjustYValue((sY +sweepradius.get())) +" "+ (sZ - (sweepstart.get()+swpr)) + " " + sblockName + " replace " + repblockName);
                    }
                    case WEST -> {
                        if (!replace.get())ChatUtils.sendPlayerMsg("/fill " + (sX - (sweepstart.get()+swpr)) + " " + adjustYValue((sY-sweepradius.get())) + " "+ (sZ - sweepradius.get()) +" "+ (sX - (sweepstart.get()+swpr)) + " " + adjustYValue((sY +sweepradius.get())) +" "+ (sZ + sweepradius.get()) + " " + sblockName);
                        else if (!replace.get())ChatUtils.sendPlayerMsg("/fill " + (sX - (sweepstart.get()+swpr)) + " " + adjustYValue((sY-sweepradius.get())) + " "+ (sZ - sweepradius.get()) +" "+ (sX - (sweepstart.get()+swpr)) + " " + adjustYValue((sY +sweepradius.get())) +" "+ (sZ + sweepradius.get()) + " " + sblockName + " replace " + repblockName);
                    }
                    case SOUTH -> {
                        if (!replace.get())ChatUtils.sendPlayerMsg("/fill " + (sX - sweepradius.get()) + " " + adjustYValue((sY-sweepradius.get())) + " "+ (sZ + (sweepstart.get()+swpr)) +" "+ (sX + sweepradius.get()) + " " + adjustYValue((sY +sweepradius.get())) +" "+ (sZ + (sweepstart.get()+swpr)) + " " + sblockName);
                        else if (!replace.get())ChatUtils.sendPlayerMsg("/fill " + (sX - sweepradius.get()) + " " + adjustYValue((sY-sweepradius.get())) + " "+ (sZ + (sweepstart.get()+swpr)) +" "+ (sX + sweepradius.get()) + " " + adjustYValue((sY +sweepradius.get())) +" "+ (sZ + (sweepstart.get()+swpr)) + " " + sblockName + " replace " + repblockName);
                    }
                    case EAST -> {
                        if (!replace.get())ChatUtils.sendPlayerMsg("/fill " + (sX  + (sweepstart.get()+swpr)) + " " + adjustYValue((sY-sweepradius.get())) + " "+ (sZ - sweepradius.get()) +" "+ (sX + (sweepstart.get()+swpr)) + " " + adjustYValue((sY +sweepradius.get())) +" "+ (sZ + sweepradius.get()) + " " + sblockName);
                        else if (!replace.get())ChatUtils.sendPlayerMsg("/fill " + (sX  + (sweepstart.get()+swpr)) + " " + adjustYValue((sY-sweepradius.get())) + " "+ (sZ - sweepradius.get()) +" "+ (sX + (sweepstart.get()+swpr)) + " " + adjustYValue((sY +sweepradius.get())) +" "+ (sZ + sweepradius.get()) + " " + sblockName + " replace " + repblockName);
                    }
                }
                swpr++;
            } else swpr=0;
        }
        if (rndplyr.get()){
            if (ticks<=tickdelay.get()){
                ticks++;
            } else if (ticks>tickdelay.get()){
                String pfullString = pblock.get().toString();
                String[] pparts = pfullString.split(":");
                String pblock = pparts[1];
                String pBlockName = pblock.replace("}", "");
                String prepfullString = pblocktoreplace.get().toString();
                String[] prepparts = prepfullString.split(":");
                String prepblock = prepparts[1];
                String pRepblockName = prepblock.replace("}", "");
                switch (mc.player.getHorizontalFacing()){
                    case NORTH, SOUTH -> {
                        if (!pReplace.get()) ChatUtils.sendPlayerMsg("/fill "+(pX+pwidth.get())+" "+adjustYValue((pY+pheight.get()))+" "+(pZ+pdepth.get())+" "+(pX-pwidth.get())+" "+adjustYValue((pY-pheight.get()))+" "+(pZ-pdepth.get())+" air");
                        else if (pReplace.get()) ChatUtils.sendPlayerMsg("/fill "+(pX+pwidth.get())+" "+adjustYValue((pY+pheight.get()))+" "+(pZ+pdepth.get())+" "+(pX-pwidth.get())+" "+adjustYValue((pY-pheight.get()))+" "+(pZ-pdepth.get())+" "+pBlockName+" replace "+pRepblockName);
                    }
                    case EAST, WEST -> {
                        if (!pReplace.get()) ChatUtils.sendPlayerMsg("/fill "+(pX+pdepth.get())+" "+adjustYValue((pY+pheight.get()))+" "+(pZ+pwidth.get())+" "+(pX-pdepth.get())+" "+adjustYValue((pY-pheight.get()))+" "+(pZ-pwidth.get())+" air");
                        else if (pReplace.get()) ChatUtils.sendPlayerMsg("/fill "+(pX+pdepth.get())+" "+adjustYValue((pY+pheight.get()))+" "+(pZ+pwidth.get())+" "+(pX-pdepth.get())+" "+adjustYValue((pY-pheight.get()))+" "+(pZ-pwidth.get())+" "+pBlockName+" replace "+pRepblockName);
                    }
                }
                ticks=0;
            }
        }
        if (voider.get()){
            String pfullString = pblock.get().toString();
            String[] pparts = pfullString.split(":");
            String pblock = pparts[1];
            String pBlockName = pblock.replace("}", "");
            String prepfullString = pblocktoreplace.get().toString();
            String[] prepparts = prepfullString.split(":");
            String prepblock = prepparts[1];
            String pRepblockName = prepblock.replace("}", "");
            if (i>= mc.player.getBlockPos().getY()-vrange.get()){
                if (!pReplace.get()) ChatUtils.sendPlayerMsg("/fill " + (pX - radius.get()) + " " + adjustYValue(i) +" "+ (pZ - radius.get()) +" "+ (pX + radius.get()) + " " + adjustYValue(i) +" "+ (pZ + radius.get()) +" air");
                else if (pReplace.get()) ChatUtils.sendPlayerMsg("/fill " + (pX - radius.get()) + " " + adjustYValue(i) +" "+ (pZ - radius.get()) +" "+ (pX + radius.get()) + " " + adjustYValue(i) +" "+ (pZ + radius.get()) +" "+pBlockName+" replace "+pRepblockName);
                i--;
            }else if (i<= mc.player.getBlockPos().getY()-vrange.get()){
                i=pY+vrange.get();
            }


        }
        if (mgcersr.get()){
            if (Modules.get().isActive(Flight.class)){
                if (errticks<3){
                    errticks++;}
                if (errticks==2){
                    error("Fly Slow. Set Flight speed to 0.1 or less. :D");
                }
            }
            String pfullString = pblock.get().toString();
            String[] pparts = pfullString.split(":");
            String pblock = pparts[1];
            String pBlockName = pblock.replace("}", "");
            String prepfullString = pblocktoreplace.get().toString();
            String[] prepparts = prepfullString.split(":");
            String prepblock = prepparts[1];
            String pRepblockName = prepblock.replace("}", "");
            if (mc.options.jumpKey.isPressed()){
                if (!pReplace.get()) ChatUtils.sendPlayerMsg("/fill " + (pX - mgcradius.get()) + " " + adjustYValue((pY+mgcdist.get())) + " "+ (pZ - mgcradius.get()) +" "+ (pX + mgcradius.get()) + " " + adjustYValue((pY+mgcdist.get())) +" "+ (pZ + mgcradius.get()) +" air");
                else if (pReplace.get()) ChatUtils.sendPlayerMsg("/fill " + (pX - mgcradius.get()) + " " + adjustYValue((pY+mgcdist.get())) + " "+ (pZ - mgcradius.get()) +" "+ (pX + mgcradius.get()) + " " + adjustYValue((pY+mgcdist.get())) +" "+ (pZ + mgcradius.get()) +" "+pBlockName+" replace "+pRepblockName);
            }
            if (mc.options.sneakKey.isPressed()){
                if (!pReplace.get()) ChatUtils.sendPlayerMsg("/fill " + (pX - mgcradius.get()) + " " + adjustYValue((pY-mgcdist.get())) + " "+ (pZ - mgcradius.get()) +" "+ (pX + mgcradius.get()) + " " + adjustYValue((pY-mgcdist.get())) +" "+ (pZ + mgcradius.get()) +" air");
                else if (pReplace.get()) ChatUtils.sendPlayerMsg("/fill " + (pX - mgcradius.get()) + " " + adjustYValue((pY-mgcdist.get())) + " "+ (pZ - mgcradius.get()) +" "+ (pX + mgcradius.get()) + " " + adjustYValue((pY-mgcdist.get())) +" "+ (pZ + mgcradius.get()) +" "+pBlockName+" replace "+pRepblockName);
            }
            if (mc.options.forwardKey.isPressed()){
                switch (mc.player.getHorizontalFacing()){
                    case NORTH -> {
                        if (!pReplace.get()) ChatUtils.sendPlayerMsg("/fill " + (pX - mgcradius.get()) + " " + adjustYValue((pY-mgcradius.get())) + " "+ (pZ - mgcdist.get()) +" "+ (pX + mgcradius.get()) + " " + adjustYValue((pY+mgcradius.get())) +" "+ (pZ - mgcdist.get()) +" air");
                        else if (pReplace.get()) ChatUtils.sendPlayerMsg("/fill " + (pX - mgcradius.get()) + " " + adjustYValue((pY-mgcradius.get())) + " "+ (pZ - mgcdist.get()) +" "+ (pX + mgcradius.get()) + " " + adjustYValue((pY+mgcradius.get())) +" "+ (pZ - mgcdist.get()) +" "+pBlockName+" replace "+pRepblockName);
                    }
                    case WEST -> {
                        if (!pReplace.get()) ChatUtils.sendPlayerMsg("/fill " + (pX - mgcdist.get()) + " " + adjustYValue((pY-mgcradius.get())) + " "+ (pZ - mgcradius.get()) +" "+ (pX - mgcdist.get()) + " " + adjustYValue((pY+mgcradius.get())) +" "+ (pZ + mgcradius.get()) +" air");
                        else if (pReplace.get()) ChatUtils.sendPlayerMsg("/fill " + (pX - mgcdist.get()) + " " + adjustYValue((pY-mgcradius.get())) + " "+ (pZ - mgcradius.get()) +" "+ (pX - mgcdist.get()) + " " + adjustYValue((pY+mgcradius.get())) +" "+ (pZ + mgcradius.get()) +" "+pBlockName+" replace "+pRepblockName);
                    }
                    case SOUTH -> {
                        if (!pReplace.get()) ChatUtils.sendPlayerMsg("/fill " + (pX - mgcradius.get()) + " " + adjustYValue((pY-mgcradius.get())) + " "+ (pZ + mgcdist.get()) +" "+ (pX + mgcradius.get()) + " " + adjustYValue((pY+mgcradius.get())) +" "+ (pZ + mgcdist.get()) +" air");
                        else if (pReplace.get()) ChatUtils.sendPlayerMsg("/fill " + (pX - mgcradius.get()) + " " + adjustYValue((pY-mgcradius.get())) + " "+ (pZ + mgcdist.get()) +" "+ (pX + mgcradius.get()) + " " + adjustYValue((pY+mgcradius.get())) +" "+ (pZ + mgcdist.get()) +" "+pBlockName+" replace "+pRepblockName);
                    }
                    case EAST -> {
                        if (!pReplace.get()) ChatUtils.sendPlayerMsg("/fill " + (pX + mgcdist.get()) + " " + adjustYValue((pY-mgcradius.get())) + " "+ (pZ - mgcradius.get()) +" "+ (pX + mgcdist.get()) + " " + adjustYValue((pY+mgcradius.get())) +" "+ (pZ + mgcradius.get()) +" air");
                        else if (pReplace.get()) ChatUtils.sendPlayerMsg("/fill " + (pX + mgcdist.get()) + " " + adjustYValue((pY-mgcradius.get())) + " "+ (pZ - mgcradius.get()) +" "+ (pX + mgcdist.get()) + " " + adjustYValue((pY+mgcradius.get())) +" "+ (pZ + mgcradius.get()) +" "+pBlockName+" replace "+pRepblockName);
                    }
                }
            }
            if (mc.options.backKey.isPressed()){
                switch (mc.player.getHorizontalFacing()){
                    case NORTH -> {
                        if (!pReplace.get()) ChatUtils.sendPlayerMsg("/fill " + (pX - mgcradius.get()) + " " + adjustYValue((pY-mgcradius.get())) + " "+ (pZ + mgcdist.get()) +" "+ (pX + mgcradius.get()) + " " + adjustYValue((pY+mgcradius.get())) +" "+ (pZ + mgcdist.get()) +" air");
                        else if (pReplace.get()) ChatUtils.sendPlayerMsg("/fill " + (pX - mgcradius.get()) + " " + adjustYValue((pY-mgcradius.get())) + " "+ (pZ + mgcdist.get()) +" "+ (pX + mgcradius.get()) + " " + adjustYValue((pY+mgcradius.get())) +" "+ (pZ + mgcdist.get()) +" "+pBlockName+" replace "+pRepblockName);
                    }
                    case WEST -> {
                        if (!pReplace.get()) ChatUtils.sendPlayerMsg("/fill " + (pX + mgcdist.get()) + " " + adjustYValue((pY-mgcradius.get())) + " "+ (pZ - mgcradius.get()) +" "+ (pX + mgcdist.get()) + " " + adjustYValue((pY+mgcradius.get())) +" "+ (pZ + mgcradius.get()) +" air");
                        else if (pReplace.get()) ChatUtils.sendPlayerMsg("/fill " + (pX + mgcdist.get()) + " " + adjustYValue((pY-mgcradius.get())) + " "+ (pZ - mgcradius.get()) +" "+ (pX + mgcdist.get()) + " " + adjustYValue((pY+mgcradius.get())) +" "+ (pZ + mgcradius.get()) +" "+pBlockName+" replace "+pRepblockName);
                    }
                    case SOUTH -> {
                        if (!pReplace.get()) ChatUtils.sendPlayerMsg("/fill " + (pX - mgcradius.get()) + " " + adjustYValue((pY-mgcradius.get())) + " "+ (pZ - mgcdist.get()) +" "+ (pX + mgcradius.get()) + " " + adjustYValue((pY+mgcradius.get())) +" "+ (pZ - mgcdist.get()) +" air");
                        else if (pReplace.get()) ChatUtils.sendPlayerMsg("/fill " + (pX - mgcradius.get()) + " " + adjustYValue((pY-mgcradius.get())) + " "+ (pZ - mgcdist.get()) +" "+ (pX + mgcradius.get()) + " " + adjustYValue((pY+mgcradius.get())) +" "+ (pZ - mgcdist.get()) +" "+pBlockName+" replace "+pRepblockName);
                    }
                    case EAST -> {
                        if (!pReplace.get()) ChatUtils.sendPlayerMsg("/fill " + (pX - mgcdist.get()) + " " + adjustYValue((pY-mgcradius.get())) + " "+ (pZ - mgcradius.get()) +" "+ (pX - mgcdist.get()) + " " + adjustYValue((pY+mgcradius.get())) +" "+ (pZ + mgcradius.get()) +" air");
                        else if (pReplace.get()) ChatUtils.sendPlayerMsg("/fill " + (pX - mgcdist.get()) + " " + adjustYValue((pY-mgcradius.get())) + " "+ (pZ - mgcradius.get()) +" "+ (pX - mgcdist.get()) + " " + adjustYValue((pY+mgcradius.get())) +" "+ (pZ + mgcradius.get()) +" "+pBlockName+" replace "+pRepblockName);
                    }
                }
            }
            if (mc.options.rightKey.isPressed()){
                switch (mc.player.getHorizontalFacing()){
                    case NORTH -> {
                        if (!pReplace.get()) ChatUtils.sendPlayerMsg("/fill " + (pX + mgcdist.get()) + " " + adjustYValue((pY-mgcradius.get())) + " "+ (pZ - mgcradius.get()) +" "+ (pX + mgcdist.get()) + " " + adjustYValue((pY+mgcradius.get())) +" "+ (pZ + mgcradius.get()) +" air");
                        else if (pReplace.get()) ChatUtils.sendPlayerMsg("/fill " + (pX + mgcdist.get()) + " " + adjustYValue((pY-mgcradius.get())) + " "+ (pZ - mgcradius.get()) +" "+ (pX + mgcdist.get()) + " " + adjustYValue((pY+mgcradius.get())) +" "+ (pZ + mgcradius.get()) +" "+pBlockName+" replace "+pRepblockName);
                    }
                    case WEST -> {
                        if (!pReplace.get()) ChatUtils.sendPlayerMsg("/fill " + (pX - mgcradius.get()) + " " + adjustYValue((pY-mgcradius.get())) + " "+ (pZ - mgcdist.get()) +" "+ (pX + mgcradius.get()) + " " + adjustYValue((pY+mgcradius.get())) +" "+ (pZ - mgcdist.get()) +" air");
                        else if (pReplace.get()) ChatUtils.sendPlayerMsg("/fill " + (pX - mgcradius.get()) + " " + adjustYValue((pY-mgcradius.get())) + " "+ (pZ - mgcdist.get()) +" "+ (pX + mgcradius.get()) + " " + adjustYValue((pY+mgcradius.get())) +" "+ (pZ - mgcdist.get()) +" "+pBlockName+" replace "+pRepblockName);
                    }
                    case SOUTH -> {
                        if (!pReplace.get()) ChatUtils.sendPlayerMsg("/fill " + (pX - mgcdist.get()) + " " + adjustYValue((pY-mgcradius.get())) + " "+ (pZ - mgcradius.get()) +" "+ (pX - mgcdist.get()) + " " + adjustYValue((pY+mgcradius.get())) +" "+ (pZ + mgcradius.get()) +" air");
                        else if (pReplace.get()) ChatUtils.sendPlayerMsg("/fill " + (pX - mgcdist.get()) + " " + adjustYValue((pY-mgcradius.get())) + " "+ (pZ - mgcradius.get()) +" "+ (pX - mgcdist.get()) + " " + adjustYValue((pY+mgcradius.get())) +" "+ (pZ + mgcradius.get()) +" "+pBlockName+" replace "+pRepblockName);
                    }
                    case EAST -> {
                        if (!pReplace.get()) ChatUtils.sendPlayerMsg("/fill " + (pX - mgcradius.get()) + " " + adjustYValue((pY-mgcradius.get())) + " "+ (pZ + mgcdist.get()) +" "+ (pX + mgcradius.get()) + " " + adjustYValue((pY+mgcradius.get())) +" "+ (pZ + mgcdist.get()) +" air");
                        else if (pReplace.get()) ChatUtils.sendPlayerMsg("/fill " + (pX - mgcradius.get()) + " " + adjustYValue((pY-mgcradius.get())) + " "+ (pZ + mgcdist.get()) +" "+ (pX + mgcradius.get()) + " " + adjustYValue((pY+mgcradius.get())) +" "+ (pZ + mgcdist.get()) +" "+pBlockName+" replace "+pRepblockName);
                    }
                }
            }
            if (mc.options.leftKey.isPressed()){
                switch (mc.player.getHorizontalFacing()){
                    case NORTH -> {
                        if (!pReplace.get()) ChatUtils.sendPlayerMsg("/fill " + (pX - mgcdist.get()) + " " + adjustYValue((pY-mgcradius.get())) + " "+ (pZ - mgcradius.get()) +" "+ (pX - mgcdist.get()) + " " + adjustYValue((pY+mgcradius.get())) +" "+ (pZ + mgcradius.get()) +" air");
                        else if (pReplace.get()) ChatUtils.sendPlayerMsg("/fill " + (pX - mgcdist.get()) + " " + adjustYValue((pY-mgcradius.get())) + " "+ (pZ - mgcradius.get()) +" "+ (pX - mgcdist.get()) + " " + adjustYValue((pY+mgcradius.get())) +" "+ (pZ + mgcradius.get()) +" "+pBlockName+" replace "+pRepblockName);
                    }
                    case WEST -> {
                        if (!pReplace.get()) ChatUtils.sendPlayerMsg("/fill " + (pX - mgcradius.get()) + " " + adjustYValue((pY-mgcradius.get())) + " "+ (pZ + mgcdist.get()) +" "+ (pX + mgcradius.get()) + " " + adjustYValue((pY+mgcradius.get())) +" "+ (pZ + mgcdist.get()) +" air");
                        else if (pReplace.get()) ChatUtils.sendPlayerMsg("/fill " + (pX - mgcradius.get()) + " " + adjustYValue((pY-mgcradius.get())) + " "+ (pZ + mgcdist.get()) +" "+ (pX + mgcradius.get()) + " " + adjustYValue((pY+mgcradius.get())) +" "+ (pZ + mgcdist.get()) +" "+pBlockName+" replace "+pRepblockName);
                    }
                    case SOUTH -> {
                        if (!pReplace.get()) ChatUtils.sendPlayerMsg("/fill " + (pX + mgcdist.get()) + " " + adjustYValue((pY-mgcradius.get())) + " "+ (pZ - mgcradius.get()) +" "+ (pX + mgcdist.get()) + " " + adjustYValue((pY+mgcradius.get())) +" "+ (pZ + mgcradius.get()) +" air");
                        else if (pReplace.get()) ChatUtils.sendPlayerMsg("/fill " + (pX + mgcdist.get()) + " " + adjustYValue((pY-mgcradius.get())) + " "+ (pZ - mgcradius.get()) +" "+ (pX + mgcdist.get()) + " " + adjustYValue((pY+mgcradius.get())) +" "+ (pZ + mgcradius.get()) +" "+pBlockName+" replace "+pRepblockName);
                    }
                    case EAST -> {
                        if (!pReplace.get()) ChatUtils.sendPlayerMsg("/fill " + (pX - mgcradius.get()) + " " + adjustYValue((pY-mgcradius.get())) + " "+ (pZ - mgcdist.get()) +" "+ (pX + mgcradius.get()) + " " + adjustYValue((pY+mgcradius.get())) +" "+ (pZ - mgcdist.get()) +" air");
                        else if (pReplace.get()) ChatUtils.sendPlayerMsg("/fill " + (pX - mgcradius.get()) + " " + adjustYValue((pY-mgcradius.get())) + " "+ (pZ - mgcdist.get()) +" "+ (pX + mgcradius.get()) + " " + adjustYValue((pY+mgcradius.get())) +" "+ (pZ - mgcdist.get()) +" "+pBlockName+" replace "+pRepblockName);
                    }
                }
            }

        }
        if (roofer.get()){
            if (roofticks>0){
                roofticks--;
            } else if (roofticks<=0) {
                String rfullString = roofblock.get().toString();
                String[] rparts = rfullString.split(":");
                String rblock = rparts[1];
                String rblockName = rblock.replace("}", "");
                ChatUtils.sendPlayerMsg("/fill " + (pX - roofradius.get()) + " " + adjustYValue(roofheight.get()) +" "+ (pZ - roofradius.get()) +" "+ (pX + roofradius.get()) + " " + adjustYValue(roofheight.get()) +" "+ (pZ + roofradius.get()) + " "+rblockName);
                roofticks=rooftickdelay.get();
            }
        }
        if (troll.get()) {
            if (trollticks<=trolltickdelay.get()){
                trollticks++;
            } else if (trollticks>trolltickdelay.get()){
                String tfullString = trollblock.get().toString();
                String[] tparts = tfullString.split(":");
                String tblock = tparts[1];
                String tBlockName = tblock.replace("}", "");
                String trepfullString = trollblocktoreplace.get().toString();
                String[] trepparts = trepfullString.split(":");
                String trepblock = trepparts[1];
                String tRepblockName = trepblock.replace("}", "");
                if (!trollrenderdist.get()) {
                    //every player in server, default
                    if (trollfriends.get()) {
                        if (!trollreplace.get())
                            ChatUtils.sendPlayerMsg("/execute at @a[name=!" + mc.player.getName().getLiteralString() + "] run fill " + "~" + trollwidth.get() + " " + "~" + trollheight.get() + " " + "~" + trolldepth.get() + " " + "~-" + trollwidth.get() + " " + "~-" + trollheight.get() + " " + "~-" + trolldepth.get() + " " + tBlockName);
                        else if (trollreplace.get())
                            ChatUtils.sendPlayerMsg("/execute at @a[name=!" + mc.player.getName().getLiteralString() + "] run fill " + "~" + trollwidth.get() + " " + "~" + trollheight.get() + " " + "~" + trolldepth.get() + " " + "~-" + trollwidth.get() + " " + "~-" + trollheight.get() + " " + "~-" + trolldepth.get() + " " + tBlockName + " replace " + tRepblockName);
                    } else if (!trollfriends.get()) {
                        players = new CopyOnWriteArrayList<>(mc.getNetworkHandler().getPlayerList());
                        List<String> friendNames = new ArrayList<>();
                        friendNames.add("name=!" + mc.player.getName().getLiteralString());
                        for (PlayerListEntry player : players) {
                            if (Friends.get().isFriend(player) && !trollfriends.get())
                                friendNames.add("name=!" + player.getProfile().getName());
                        }
                        String friendsString = String.join(",", friendNames);
                        if (!trollreplace.get()){
                            String thecommand = "/execute at @a[" + friendsString + "] run fill " + "~" + trollwidth.get() + " " + "~" + trollheight.get() + " " + "~" + trolldepth.get() + " " + "~-" + trollwidth.get() + " " + "~-" + trollheight.get() + " " + "~-" + trolldepth.get() + " " + tBlockName;
                            if (thecommand.length()<=256){
                                ChatUtils.sendPlayerMsg(thecommand);
                            }
                            else {
                                error("Fill Around Other Players Command is too long, you have too many friends online.");
                            }
                        }
                        else if (trollreplace.get()){
                            String thecommand2 = "/execute at @a[" + friendsString + "] run fill " + "~" + trollwidth.get() + " " + "~" + trollheight.get() + " " + "~" + trolldepth.get() + " " + "~-" + trollwidth.get() + " " + "~-" + trollheight.get() + " " + "~-" + trolldepth.get() + " " + tBlockName + " replace " + tRepblockName;
                            if (thecommand2.length()<=256){
                                ChatUtils.sendPlayerMsg(thecommand2);
                            }
                            else {
                                error("Fill Around Other Players Command is too long, you have too many friends online.");
                            }
                        }
                    }
                } else if (trollrenderdist.get()){
                    //every player in render distance
                    for (Entity entity : mc.world.getEntities()) {
                        if (entity instanceof PlayerEntity && entity != mc.player){
                            if (!trollfriends.get() && entity instanceof PlayerEntity && Friends.get().isFriend((PlayerEntity) entity)) {
                                return;
                            }
                            switch (entity.getHorizontalFacing()){
                                case NORTH, SOUTH -> {
                                    if (!trollreplace.get()) ChatUtils.sendPlayerMsg("/fill "+(entity.getBlockPos().getX()+trollwidth.get())+" "+adjustYValue((entity.getBlockPos().getY()+trollheight.get()))+" "+(entity.getBlockPos().getZ()+trolldepth.get())+" "+(entity.getBlockPos().getX()-trollwidth.get())+" "+adjustYValue((entity.getBlockPos().getY()-trollheight.get()))+" "+(entity.getBlockPos().getZ()-trolldepth.get())+" "+tBlockName);
                                    else if (trollreplace.get()) ChatUtils.sendPlayerMsg("/fill "+(entity.getBlockPos().getX()+trollwidth.get())+" "+adjustYValue((entity.getBlockPos().getY()+trollheight.get()))+" "+(entity.getBlockPos().getZ()+trolldepth.get())+" "+(entity.getBlockPos().getX()-trollwidth.get())+" "+adjustYValue((entity.getBlockPos().getY()-trollheight.get()))+" "+(entity.getBlockPos().getZ()-trolldepth.get())+" "+tBlockName+" replace "+tRepblockName);
                                }
                                case EAST, WEST -> {
                                    if (!trollreplace.get()) ChatUtils.sendPlayerMsg("/fill "+(entity.getBlockPos().getX()+trolldepth.get())+" "+adjustYValue((entity.getBlockPos().getY()+trollheight.get()))+" "+(entity.getBlockPos().getZ()+trollwidth.get())+" "+(entity.getBlockPos().getX()-trolldepth.get())+" "+adjustYValue((entity.getBlockPos().getY()-trollheight.get()))+" "+(entity.getBlockPos().getZ()-trollwidth.get())+" "+tBlockName);
                                    else if (trollreplace.get()) ChatUtils.sendPlayerMsg("/fill "+(entity.getBlockPos().getX()+trolldepth.get())+" "+adjustYValue((entity.getBlockPos().getY()+trollheight.get()))+" "+(entity.getBlockPos().getZ()+trollwidth.get())+" "+(entity.getBlockPos().getX()-trolldepth.get())+" "+adjustYValue((entity.getBlockPos().getY()-trollheight.get()))+" "+(entity.getBlockPos().getZ()-trollwidth.get())+" "+tBlockName+" replace "+tRepblockName);
                                }
                            }
                        }
                    }
                }
                trollticks=0;
            }
        }
        if (terminate.get()) {
            if (terminateticks<=terminatetickdelay.get()){
                terminateticks++;
            } else if (terminateticks>terminatetickdelay.get()){
                String tfullString = terminateblock.get().toString();
                String[] tparts = tfullString.split(":");
                String tblock = tparts[1];
                String tBlockName = tblock.replace("}", "");
                String trepfullString = terminateblocktoreplace.get().toString();
                String[] trepparts = trepfullString.split(":");
                String trepblock = trepparts[1];
                String tRepblockName = trepblock.replace("}", "");
                if (!terminaterenderdist.get()) {
                    //every entity that is within render distance in server, default
                    if (!terminatereplace.get())
                        ChatUtils.sendPlayerMsg("/execute at @e[type=!player] run fill " + "~" + terminatewidth.get() + " " + "~" + terminateheight.get() + " " + "~" + terminatedepth.get() + " " + "~-" + terminatewidth.get() + " " + "~-" + terminateheight.get() + " " + "~-" + terminatedepth.get() + " " + tBlockName);
                    else if (terminatereplace.get())
                        ChatUtils.sendPlayerMsg("/execute at @e[type=!player] run fill " + "~" + terminatewidth.get() + " " + "~" + terminateheight.get() + " " + "~" + terminatedepth.get() + " " + "~-" + terminatewidth.get() + " " + "~-" + terminateheight.get() + " " + "~-" + terminatedepth.get() + " " + tBlockName + " replace " + tRepblockName);
                } else if (terminaterenderdist.get()){
                    //every entity in just your render distance
                    for (Entity entity : mc.world.getEntities()) {
                        if (!(entity instanceof PlayerEntity)){
                            switch (entity.getHorizontalFacing()){
                                case NORTH, SOUTH -> {
                                    if (!terminatereplace.get()) ChatUtils.sendPlayerMsg("/fill "+(entity.getBlockPos().getX()+terminatewidth.get())+" "+adjustYValue((entity.getBlockPos().getY()+terminateheight.get()))+" "+(entity.getBlockPos().getZ()+terminatedepth.get())+" "+(entity.getBlockPos().getX()-terminatewidth.get())+" "+adjustYValue((entity.getBlockPos().getY()-terminateheight.get()))+" "+(entity.getBlockPos().getZ()-terminatedepth.get())+" "+tBlockName);
                                    else if (terminatereplace.get()) ChatUtils.sendPlayerMsg("/fill "+(entity.getBlockPos().getX()+terminatewidth.get())+" "+adjustYValue((entity.getBlockPos().getY()+terminateheight.get()))+" "+(entity.getBlockPos().getZ()+terminatedepth.get())+" "+(entity.getBlockPos().getX()-terminatewidth.get())+" "+adjustYValue((entity.getBlockPos().getY()-terminateheight.get()))+" "+(entity.getBlockPos().getZ()-terminatedepth.get())+" "+tBlockName+" replace "+tRepblockName);
                                }
                                case EAST, WEST -> {
                                    if (!terminatereplace.get()) ChatUtils.sendPlayerMsg("/fill "+(entity.getBlockPos().getX()+terminatedepth.get())+" "+adjustYValue((entity.getBlockPos().getY()+terminateheight.get()))+" "+(entity.getBlockPos().getZ()+terminatewidth.get())+" "+(entity.getBlockPos().getX()-terminatedepth.get())+" "+adjustYValue((entity.getBlockPos().getY()-terminateheight.get()))+" "+(entity.getBlockPos().getZ()-terminatewidth.get())+" "+tBlockName);
                                    else if (terminatereplace.get()) ChatUtils.sendPlayerMsg("/fill "+(entity.getBlockPos().getX()+terminatedepth.get())+" "+adjustYValue((entity.getBlockPos().getY()+terminateheight.get()))+" "+(entity.getBlockPos().getZ()+terminatewidth.get())+" "+(entity.getBlockPos().getX()-terminatedepth.get())+" "+adjustYValue((entity.getBlockPos().getY()-terminateheight.get()))+" "+(entity.getBlockPos().getZ()-terminatewidth.get())+" "+tBlockName+" replace "+tRepblockName);
                                }
                            }
                        }
                    }
                }
                terminateticks=0;
            }
        }
    }
    private NbtComponent createEntityData(BlockPos pos) {
        NbtCompound entityTag = new NbtCompound();
        NbtList Pos = new NbtList();
        Pos.add(NbtDouble.of(pos.getX()));
        Pos.add(NbtDouble.of(pos.getY()));
        Pos.add(NbtDouble.of(pos.getZ()));
        entityTag.put("Pos", Pos);
        entityTag.putString("id", "minecraft:lightning_bolt");
        return NbtComponent.of(entityTag);
    }
}