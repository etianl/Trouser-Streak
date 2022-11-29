package pwn.noobs.trouserstreak.modules;

import pwn.noobs.trouserstreak.Trouser;
import pwn.noobs.trouserstreak.utils.BEntityUtils;
import pwn.noobs.trouserstreak.utils.BPlayerUtils;
import pwn.noobs.trouserstreak.utils.BWorldUtils;
import pwn.noobs.trouserstreak.utils.PositionUtils;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AutoBuild extends Module {
    public enum CenterMode {
        Center,
        Snap,
        None
    }


    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlacing = settings.createGroup("Placing");
    private final SettingGroup sgToggle = settings.createGroup("Toggle Modes");


    // General
    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
            .name("primary-blocks")
            .description("What blocks to use for the build.")
            .defaultValue(Blocks.AIR)
            .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("Tick delay between block placements.")
            .defaultValue(2)
            .range(0,20)
            .sliderRange(0,20)
            .build()
    );

    private final Setting<Integer> blocksPerTick = sgGeneral.add(new IntSetting.Builder()
            .name("blocks-per-tick")
            .description("Blocks placed per tick.")
            .defaultValue(1)
            .range(1,5)
            .sliderRange(1,5)
            .build()
    );

    private final Setting<CenterMode> centerMode = sgGeneral.add(new EnumSetting.Builder<CenterMode>()
            .name("center")
            .description("How Surround+ should center you.")
            .defaultValue(CenterMode.Center)
            .build()
    );

    private final Setting<Boolean> onlyGround = sgGeneral.add(new BoolSetting.Builder()
            .name("only-on-ground")
            .description("Will only try to place if you are on the ground.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> info = sgGeneral.add(new BoolSetting.Builder()
            .name("info")
            .description("idk info")
            .defaultValue(true)
            .build()
    );


    // Placing
    private final Setting<BWorldUtils.SwitchMode> switchMode = sgPlacing.add(new EnumSetting.Builder<BWorldUtils.SwitchMode>()
            .name("switch-mode")
            .description("How to switch to your target block.")
            .defaultValue(BWorldUtils.SwitchMode.Both)
            .build()
    );

    private final Setting<Boolean> switchBack = sgPlacing.add(new BoolSetting.Builder()
            .name("switch-back")
            .description("Switches back to your original slot after placing.")
            .defaultValue(true)
            .build()
    );

    private final Setting<BWorldUtils.PlaceMode> placeMode = sgPlacing.add(new EnumSetting.Builder<BWorldUtils.PlaceMode>()
            .name("place-mode")
            .description("How to switch to your target block.")
            .defaultValue(BWorldUtils.PlaceMode.Both)
            .build()
    );

    private final Setting<Boolean> ignoreEntity = sgPlacing.add(new BoolSetting.Builder()
            .name("ignore-entities")
            .description("Will try to place even if there is an entity in the way.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> airPlace = sgPlacing.add(new BoolSetting.Builder()
            .name("air-place")
            .description("Whether to place blocks mid air or not.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> onlyAirPlace = sgPlacing.add(new BoolSetting.Builder()
            .name("only-air-place")
            .description("Forces you to only airplace to help with stricter rotations.")
            .defaultValue(false)
            .visible(airPlace::get)
            .build()
    );

    private final Setting<BWorldUtils.AirPlaceDirection> airPlaceDirection = sgPlacing.add(new EnumSetting.Builder<BWorldUtils.AirPlaceDirection>()
            .name("place-direction")
            .description("Side to try to place at when you are trying to air place.")
            .defaultValue(BWorldUtils.AirPlaceDirection.Up)
            .visible(airPlace::get)
            .build()
    );

    private final Setting<Boolean> rotate = sgPlacing.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Whether to face towards the block you are placing or not.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> rotationPrio = sgPlacing.add(new IntSetting.Builder()
            .name("rotation-priority")
            .description("Rotation priority for Surround+.")
            .defaultValue(100)
            .sliderRange(0, 200)
            .visible(rotate::get)
            .build()
    );


    // Toggles
    private final Setting<Boolean> toggleOnYChange = sgToggle.add(new BoolSetting.Builder()
            .name("toggle-on-y-change")
            .description("Automatically disables when your Y level changes.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> toggleOnDeath = sgToggle.add(new BoolSetting.Builder()
            .name("toggle-on-death")
            .description("Automatically disables after you die.")
            .defaultValue(false)
            .build()
    );


    public AutoBuild() {
        super(Trouser.Main, "auto-build", "Build whatever you draw.");
    }

    //TODO: Clean this upp maybe and make the grid nicer with a header and stuff.

    boolean ett = false, tva = true, tree = true, fyra = true, fem = false;
    boolean ett1 = false,tva1 = true, tree1 = true, fyra1 = true, fem1 = false;
    boolean ett2 = false, tva2 = true, tree2 = true, fyra2 = true, fem2 = false;
    boolean ett3 = true, tva3 = true, tree3 = true, fyra3 = true, fem3 = true;
    boolean ett4 = true, tva4 = true, tree4 = false, fyra4 = true, fem4 = true;


    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();
        WTable table = theme.table();
        list.add(table);

        WCheckbox one = table.add(theme.checkbox(ett)).expandX().widget();
        one.action = () -> ett = one.checked;
        WCheckbox two = table.add(theme.checkbox(tva)).expandX().widget();
        two.action = () -> tva = two.checked;
        WCheckbox three = table.add(theme.checkbox(tree)).expandX().widget();
        three.action = () -> tree = three.checked;
        WCheckbox four = table.add(theme.checkbox(fyra)).expandX().widget();
        four.action = () -> fyra = four.checked;
        WCheckbox five = table.add(theme.checkbox(fem)).expandX().widget();
        five.action = () -> fem = five.checked;
        table.row();

        WCheckbox one1 = table.add(theme.checkbox(ett1)).expandX().widget();
        one1.action = () -> ett1 = one1.checked;
        WCheckbox two1 = table.add(theme.checkbox(tva1)).expandX().widget();
        two1.action = () -> tva1 = two1.checked;
        WCheckbox three1 = table.add(theme.checkbox(tree1)).expandX().widget();
        three1.action = () -> tree1 = three1.checked;
        WCheckbox four1 = table.add(theme.checkbox(fyra1)).expandX().widget();
        four1.action = () -> fyra1 = four1.checked;
        WCheckbox five1 = table.add(theme.checkbox(fem1)).expandX().widget();
        five1.action = () -> fem1 = five1.checked;
        table.row();

        WCheckbox one2 = table.add(theme.checkbox(ett2)).expandX().widget();
        one2.action = () -> ett2 = one2.checked;
        WCheckbox two2 = table.add(theme.checkbox(tva2)).expandX().widget();
        two2.action = () -> tva2 = two2.checked;
        WCheckbox three2 = table.add(theme.checkbox(tree2)).expandX().widget();
        three2.action = () -> tree2 = three2.checked;
        WCheckbox four2 = table.add(theme.checkbox(fyra2)).expandX().widget();
        four2.action = () -> fyra2 = four2.checked;
        WCheckbox five2 = table.add(theme.checkbox(fem2)).expandX().widget();
        five2.action = () -> fem2 = five2.checked;
        table.row();

        WCheckbox one3 = table.add(theme.checkbox(ett3)).expandX().widget();
        one3.action = () -> ett3 = one3.checked;
        WCheckbox two3 = table.add(theme.checkbox(tva3)).expandX().widget();
        two3.action = () -> tva3 = two3.checked;
        WCheckbox three3 = table.add(theme.checkbox(tree3)).expandX().widget();
        three3.action = () -> tree3 = three3.checked;
        WCheckbox four3 = table.add(theme.checkbox(fyra3)).expandX().widget();
        four3.action = () -> fyra3 = four3.checked;
        WCheckbox five3 = table.add(theme.checkbox(fem3)).expandX().widget();
        five3.action = () -> fem3 = five3.checked;
        table.row();

        WCheckbox one4 = table.add(theme.checkbox(ett4)).expandX().widget();
        one4.action = () -> ett4 = one4.checked;
        WCheckbox two4 = table.add(theme.checkbox(tva4)).expandX().widget();
        two4.action = () -> tva4 = two4.checked;
        WCheckbox three4 = table.add(theme.checkbox(tree4)).expandX().widget();
        three4.action = () -> tree4 = three4.checked;
        WCheckbox four4 = table.add(theme.checkbox(fyra4)).expandX().widget();
        four4.action = () -> fyra4 = four4.checked;
        WCheckbox five4 = table.add(theme.checkbox(fem4)).expandX().widget();
        five4.action = () -> fem4 = five4.checked;
        table.row();

        return list;
    }

    // Fields
    private BlockPos playerPos;
    private int ticksPassed;
    private int blocksPlaced;
    private boolean centered;

    Direction dir;

    @Override
    public void onActivate() {
        ticksPassed = 0;
        blocksPlaced = 0;

        centered = false;
        playerPos = BEntityUtils.playerPos(mc.player);

        if (centerMode.get() != CenterMode.None) {
            if (centerMode.get() == CenterMode.Snap) BWorldUtils.snapPlayer(playerPos);
            else PlayerUtils.centerPlayer();
        }

        dir = BPlayerUtils.direction(mc.gameRenderer.getCamera().getYaw());

        if (info.get()){
            info("Stand still for best result.");
        }
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {

        // Decrement placing timer
        if (ticksPassed >= 0) ticksPassed--;
        else {
            ticksPassed = delay.get();
            blocksPlaced = 0;
        }

        // Update player position
        playerPos = BEntityUtils.playerPos(mc.player);

        if (centerMode.get() != CenterMode.None && !centered && mc.player.isOnGround()) {
            if (centerMode.get() == CenterMode.Snap) BWorldUtils.snapPlayer(playerPos);
            else PlayerUtils.centerPlayer();

            centered = true;
        }

        // Need to recenter again if the player is in the air
        if (!mc.player.isOnGround()) centered = false;

        if (toggleOnYChange.get()) {
            if (mc.player.prevY < mc.player.getY()) {
                toggle();
                return;
            }
        }

        if (PositionUtils.allPlaced(placePos())) {
            toggle();
            return;
        }

        if (onlyGround.get() && !mc.player.isOnGround()) return;

        if (!getTargetBlock().found()) return;

        if (ticksPassed <= 0) {
            for (BlockPos pos : placePos()) {
                if (blocksPlaced >= blocksPerTick.get()) return;
                if (BWorldUtils.place(pos, getTargetBlock(), rotate.get(), rotationPrio.get(), switchMode.get(), placeMode.get(), onlyAirPlace.get(), airPlaceDirection.get(), false, !ignoreEntity.get(), switchBack.get())) {
                    blocksPlaced++;
                }
            }
        }

    }

    private List<BlockPos> placePos() {
        List<BlockPos> pos = new ArrayList<>();

        int x = 2, y = 1, xy = 0, yx = -1, z = -2;
        int  a = 2, b = 2, ab = 2, ba = 2, c = 2;

        if (dir == Direction.SOUTH){
            x = 0; y = 0; xy = 0; yx = 0; z = 0;
            a = 1; b = 2; ab = 3; ba = 4; c = 5;
        }
        else if (dir == Direction.WEST) {
            x = -1; y = -2; xy = -3; yx = -4; z = -5;
            a = 0; b = 0; ab = 0; ba = 0; c = 0;
        }
        else if (dir == Direction.NORTH) {
            x = 0; y = 0; xy = 0; yx = 0; z = 0;
            a = -1; b = -2; ab = -3; ba = -4; c = -5;
        }
        else if (dir == Direction.EAST) {
            x = 1; y = 2; xy = 3; yx = 4; z = 5;
            a = 0; b = 0; ab = 0; ba = 0; c = 0;
        }

        if (ett) add(pos, playerPos.add(x , 4, a));
        if (tva) add(pos, playerPos.add(y, 4, b));
        if (tree) add(pos, playerPos.add(xy, 4, ab));
        if (fyra) add(pos, playerPos.add(yx , 4,  ba));
        if (fem) add(pos, playerPos.add(z, 4, c));

        if (ett1) add(pos, playerPos.add(x , 3, a));
        if (tva1) add(pos, playerPos.add(y, 3, b));
        if (tree1) add(pos, playerPos.add(xy, 3, ab));
        if (fyra1) add(pos, playerPos.add(yx , 3,  ba));
        if (fem1) add(pos, playerPos.add(z, 3, c));

        if (ett2) add(pos, playerPos.add(x , 2, a));
        if (tva2) add(pos, playerPos.add(y, 2, b));
        if (tree2) add(pos, playerPos.add(xy, 2, ab));
        if (fyra2) add(pos, playerPos.add(yx , 2,  ba));
        if (fem2) add(pos, playerPos.add(z, 2, c));

        if (ett3) add(pos, playerPos.add(x , 1, a));
        if (tva3) add(pos, playerPos.add(y, 1, b));
        if (tree3) add(pos, playerPos.add(xy, 1, ab));
        if (fyra3) add(pos, playerPos.add(yx , 1,  ba));
        if (fem3) add(pos, playerPos.add(z, 1, c));

        if (ett4) add(pos, playerPos.add(x , 0, a));
        if (tva4) add(pos, playerPos.add(y, 0, b));
        if (tree4) add(pos, playerPos.add(xy, 0, ab));
        if (fyra4) add(pos, playerPos.add(yx , 0,  ba));
        if (fem4) add(pos, playerPos.add(z, 0, c));

        return pos;
    }

    // adds block to list and structure block if needed to place
    private void add(List<BlockPos> list, BlockPos pos) {
        if (mc.world.getBlockState(pos).isAir()
                && allAir(pos.north(), pos.east(), pos.south(), pos.west(), pos.up(), pos.down())
                && !airPlace.get()
        ) list.add(pos.down());
        list.add(pos);
    }

    private boolean allAir(BlockPos... pos) {
        return Arrays.stream(pos).allMatch(blockPos -> mc.world.getBlockState(blockPos).getMaterial().isReplaceable());
    }

    private boolean anyAir(BlockPos... pos) {
        return Arrays.stream(pos).anyMatch(blockPos -> mc.world.getBlockState(blockPos).getMaterial().isReplaceable());
    }

    private FindItemResult getTargetBlock() {
        return InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem())));
    }

    //Toggle
    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event)  {
        if (event.packet instanceof DeathMessageS2CPacket packet) {
            Entity entity = mc.world.getEntityById(packet.getEntityId());
            if (entity == mc.player && toggleOnDeath.get()) {
                toggle();
            }
        }
    }
}
