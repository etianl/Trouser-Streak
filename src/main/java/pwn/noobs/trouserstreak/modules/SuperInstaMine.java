package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.item.ToolItem;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import pwn.noobs.trouserstreak.Trouser;

public class SuperInstaMine extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
            .name("Break Modes (Range)")
            .description("The range around the center block to break more blocks")
            .defaultValue(0)
            .sliderRange(-1,7)
            .min(-1)
            .max(7)
            .build()
    );
    private final Setting<Boolean> aorient = sgGeneral.add(new BoolSetting.Builder()
            .name("AutoOrientBreakDirection")
            .description("For Break Mode 3 and 4. Automatically chooses whether to break upright or horizontal.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Modes> mode = sgGeneral.add(new EnumSetting.Builder<Modes>()
            .name("Break Direction Mode")
            .description("For Break Mode 3 and 4. Choose whether to break upright or horizontal.")
            .defaultValue(Modes.Vertical)
            .visible(() -> !aorient.get())
            .build());
    private final Setting<Integer> tickDelay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("The delay between breaks.")
            .defaultValue(0)
            .min(0)
            .sliderMax(20)
            .build()
    );

    private final Setting<Boolean> pick = sgGeneral.add(new BoolSetting.Builder()
            .name("only-pick")
            .description("Only tries to mine the block if you are holding a pickaxe.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
            .name("Swing Hand")
            .description("Do or Do Not swing hand when instamining.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Faces the blocks being mined server side.")
            .defaultValue(true)
            .build()
    );

    // Render

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
            .name("render")
            .description("Renders a block overlay on the block being broken.")
            .defaultValue(true)
            .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The color of the sides of the blocks being rendered.")
            .defaultValue(new SettingColor(204, 0, 0, 10))
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The color of the lines of the blocks being rendered.")
            .defaultValue(new SettingColor(204, 0, 0, 255))
            .build()
    );

    private int ticks;

    private final BlockPos.Mutable blockPos = new BlockPos.Mutable(0, -1, 0);
    private final BlockPos.Mutable blockPos1 = new BlockPos.Mutable(0, -1, 0);
    private final BlockPos.Mutable blockPos2 = new BlockPos.Mutable(0, -1, 0);
    private final BlockPos.Mutable blockPos3 = new BlockPos.Mutable(0, -1, 0);
    private final BlockPos.Mutable blockPos4 = new BlockPos.Mutable(0, -1, 0);
    private final BlockPos.Mutable blockPos5 = new BlockPos.Mutable(0, -1, 0);
    private final BlockPos.Mutable blockPos6 = new BlockPos.Mutable(0, -1, 0);
    private final BlockPos.Mutable blockPos7 = new BlockPos.Mutable(0, -1, 0);
    private final BlockPos.Mutable blockPos8 = new BlockPos.Mutable(0, -1, 0);
    private final BlockPos.Mutable blockPos9 = new BlockPos.Mutable(0, -1, 0);
    private final BlockPos.Mutable blockPos10 = new BlockPos.Mutable(0, -1, 0);
    private final BlockPos.Mutable blockPos11 = new BlockPos.Mutable(0, -1, 0);
    private final BlockPos.Mutable blockPos12 = new BlockPos.Mutable(0, -1, 0);
    private final BlockPos.Mutable blockPos13 = new BlockPos.Mutable(0, -1, 0);
    private final BlockPos.Mutable blockPos14 = new BlockPos.Mutable(0, -1, 0);
    private final BlockPos.Mutable blockPos15 = new BlockPos.Mutable(0, -1, 0);
    private final BlockPos.Mutable blockPos16 = new BlockPos.Mutable(0, -1, 0);
    private final BlockPos.Mutable blockPos17 = new BlockPos.Mutable(0, -1, 0);
    private final BlockPos.Mutable blockPos18 = new BlockPos.Mutable(0, -1, 0);
    private final BlockPos.Mutable blockPos19 = new BlockPos.Mutable(0, -1, 0);
    private final BlockPos.Mutable blockPos20 = new BlockPos.Mutable(0, -1, 0);
    private final BlockPos.Mutable blockPos21 = new BlockPos.Mutable(0, -1, 0);
    private final BlockPos.Mutable blockPos22 = new BlockPos.Mutable(0, -1, 0);
    private final BlockPos.Mutable blockPos23 = new BlockPos.Mutable(0, -1, 0);
    private final BlockPos.Mutable blockPos24 = new BlockPos.Mutable(0, -1, 0);
    private final BlockPos.Mutable blockPos25 = new BlockPos.Mutable(0, -1, 0);
    private final BlockPos.Mutable blockPos26 = new BlockPos.Mutable(0, -1, 0);


    private Direction direction;
    private Direction playermovingdirection;
    private int playerpitch;

    public SuperInstaMine() {
        super(Trouser.Main, "SuperInstaMine", "Attempts to instantly mine blocks. Modified to be able to break many blocks at a time.");
    }

    @Override
    public void onActivate() {
        ticks = 0;
        blockPos.set(0, -128, 0);
        blockPos1.set(0, -128, 0);
        blockPos2.set(0, -128, 0);
        blockPos3.set(0, -128, 0);
        blockPos4.set(0, -128, 0);
        blockPos5.set(0, -128, 0);
        blockPos6.set(0, -128, 0);
        blockPos7.set(0, -128, 0);
        blockPos8.set(0, -128, 0);
        blockPos9.set(0, -128, 0);
        blockPos10.set(0, -128, 0);
        blockPos11.set(0, -128, 0);
        blockPos12.set(0, -128, 0);
        blockPos12.set(0, -128, 0);
        blockPos14.set(0, -128, 0);
        blockPos15.set(0, -128, 0);
        blockPos16.set(0, -128, 0);
        blockPos17.set(0, -128, 0);
        blockPos18.set(0, -128, 0);
        blockPos19.set(0, -128, 0);
        blockPos20.set(0, -128, 0);
        blockPos21.set(0, -128, 0);
        blockPos22.set(0, -128, 0);
        blockPos23.set(0, -128, 0);
        blockPos24.set(0, -128, 0);
        blockPos25.set(0, -128, 0);
        blockPos26.set(0, -128, 0);
    }

    @EventHandler
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        direction = event.direction;
        playermovingdirection = mc.player.getMovementDirection();
        playerpitch= Math.round(mc.player.getPitch());
        //middle layer 3x3x3
        blockPos.set(event.blockPos);
        blockPos1.set(new BlockPos(event.blockPos.getX()+1, event.blockPos.getY(), event.blockPos.getZ()));
        blockPos2.set(new BlockPos(event.blockPos.getX()-1, event.blockPos.getY(), event.blockPos.getZ()));
        blockPos3.set(new BlockPos(event.blockPos.getX(), event.blockPos.getY(), event.blockPos.getZ()+1));
        blockPos4.set(new BlockPos(event.blockPos.getX(), event.blockPos.getY(), event.blockPos.getZ()-1));
        blockPos5.set(new BlockPos(event.blockPos.getX()+1, event.blockPos.getY(), event.blockPos.getZ()+1));
        blockPos6.set(new BlockPos(event.blockPos.getX()-1, event.blockPos.getY(), event.blockPos.getZ()-1));
        blockPos7.set(new BlockPos(event.blockPos.getX()+1, event.blockPos.getY(), event.blockPos.getZ()-1));
        blockPos8.set(new BlockPos(event.blockPos.getX()-1, event.blockPos.getY(), event.blockPos.getZ()+1));
        //top layer 3x3x3
        blockPos9.set(new BlockPos(event.blockPos.getX(), event.blockPos.getY()+1, event.blockPos.getZ()));
        blockPos10.set(new BlockPos(event.blockPos.getX()+1, event.blockPos.getY()+1, event.blockPos.getZ()));
        blockPos11.set(new BlockPos(event.blockPos.getX()-1, event.blockPos.getY()+1, event.blockPos.getZ()));
        blockPos12.set(new BlockPos(event.blockPos.getX(), event.blockPos.getY()+1, event.blockPos.getZ()+1));
        blockPos13.set(new BlockPos(event.blockPos.getX(), event.blockPos.getY()+1, event.blockPos.getZ()-1));
        blockPos14.set(new BlockPos(event.blockPos.getX()+1, event.blockPos.getY()+1, event.blockPos.getZ()+1));
        blockPos15.set(new BlockPos(event.blockPos.getX()-1, event.blockPos.getY()+1, event.blockPos.getZ()-1));
        blockPos16.set(new BlockPos(event.blockPos.getX()+1, event.blockPos.getY()+1, event.blockPos.getZ()-1));
        blockPos17.set(new BlockPos(event.blockPos.getX()-1, event.blockPos.getY()+1, event.blockPos.getZ()+1));
        //bottom layer 3x3
        blockPos18.set(new BlockPos(event.blockPos.getX(), event.blockPos.getY()-1, event.blockPos.getZ()));
        blockPos19.set(new BlockPos(event.blockPos.getX()+1, event.blockPos.getY()-1, event.blockPos.getZ()));
        blockPos20.set(new BlockPos(event.blockPos.getX()-1, event.blockPos.getY()-1, event.blockPos.getZ()));
        blockPos21.set(new BlockPos(event.blockPos.getX(), event.blockPos.getY()-1, event.blockPos.getZ()+1));
        blockPos22.set(new BlockPos(event.blockPos.getX(), event.blockPos.getY()-1, event.blockPos.getZ()-1));
        blockPos23.set(new BlockPos(event.blockPos.getX()+1, event.blockPos.getY()-1, event.blockPos.getZ()+1));
        blockPos24.set(new BlockPos(event.blockPos.getX()-1, event.blockPos.getY()-1, event.blockPos.getZ()-1));
        blockPos25.set(new BlockPos(event.blockPos.getX()+1, event.blockPos.getY()-1, event.blockPos.getZ()-1));
        blockPos26.set(new BlockPos(event.blockPos.getX()-1, event.blockPos.getY()-1, event.blockPos.getZ()+1));
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (ticks >= tickDelay.get()) {
            ticks = 0;

            if (shouldMine() && range.get()==-1) {
                if (rotate.get() && BlockUtils.canBreak(blockPos))Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction)));
                else if (!rotate.get() && BlockUtils.canBreak(blockPos))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction));
                switch (playermovingdirection){
                    case NORTH -> {
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos2)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos2)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos2, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos2)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos2)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos2, direction));
                    }
                    case SOUTH -> {
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos1)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos1)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos1, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos1)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos1)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos1, direction));
                    }
                    case EAST -> {
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos4)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos4)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos4, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos4)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos4)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos4, direction));
                    }
                    case WEST -> {
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos3)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos3)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos3, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos3)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos3)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos3, direction));
                    }
                }
                if (rotate.get() && BlockUtils.canBreak(blockPos))Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction)));
                else if (!rotate.get() && BlockUtils.canBreak(blockPos))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction));
                if (swing.get()) mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }

            if (shouldMine() && range.get()==0) {
                if (rotate.get() && BlockUtils.canBreak(blockPos))Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction)));
                else if (!rotate.get() && BlockUtils.canBreak(blockPos))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction));
                if (swing.get()) mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }

            if (shouldMine() && range.get()==1) {
                if (rotate.get() && BlockUtils.canBreak(blockPos))Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction)));
                else if (!rotate.get() && BlockUtils.canBreak(blockPos))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction));
                switch (playermovingdirection){
                    case NORTH -> {
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos1)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos1)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos1, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos1)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos1)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos1, direction));
                    }
                    case SOUTH -> {
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos2)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos2)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos2, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos2)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos2)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos2, direction));
                    }
                    case EAST -> {
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos3)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos3)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos3, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos3)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos3)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos3, direction));
                    }
                    case WEST -> {
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos4)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos4)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos4, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos4)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos4)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos4, direction));
                    }
                }
                if (rotate.get() && BlockUtils.canBreak(blockPos))Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction)));
                else if (!rotate.get() && BlockUtils.canBreak(blockPos))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction));
                if (swing.get()) mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }
            if (shouldMine() && range.get()==2) {
                if (rotate.get() && BlockUtils.canBreak(blockPos))Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction)));
                else if (!rotate.get() && BlockUtils.canBreak(blockPos))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction));
                if (playermovingdirection == Direction.NORTH || playermovingdirection == Direction.SOUTH) {
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos2)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos2)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos2, direction));
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos2)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos2)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos2, direction));
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos1)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos1)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos1, direction));
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos1)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos1)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos1, direction));
                }
                if (playermovingdirection == Direction.EAST || playermovingdirection == Direction.WEST) {
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos4)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos4)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos4, direction));
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos4)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos4)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos4, direction));
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos3)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos3)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos3, direction));
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos3)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos3)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos3, direction));
                }
                if (rotate.get() && BlockUtils.canBreak(blockPos))Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction)));
                else if (!rotate.get() && BlockUtils.canBreak(blockPos))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction));
                if (swing.get()) mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }
            if (shouldMine() && range.get()==3) {
                if (rotate.get() && BlockUtils.canBreak(blockPos))Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction)));
                else if (!rotate.get() && BlockUtils.canBreak(blockPos))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction));
                if ((aorient.get() && playerpitch<=30 && playerpitch>=-30) || (mode.get() == Modes.Vertical && !aorient.get())) {
                    if (playermovingdirection == Direction.NORTH || playermovingdirection == Direction.SOUTH && (playerpitch <= 30 && playerpitch >= -30)) {
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos2)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos2)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos2, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos2)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos2)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos2, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos1)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos1)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos1, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos1)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos1)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos1, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos9)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos9)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos9, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos9)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos9)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos9, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos18)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos18)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos18, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos18)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos18)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos18, direction));
                    }
                    if (playermovingdirection == Direction.EAST || playermovingdirection == Direction.WEST && (playerpitch <= 30 && playerpitch >= -30)) {
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos4)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos4)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos4, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos4)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos4)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos4, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos3)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos3)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos3, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos3)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos3)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos3, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos9)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos9)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos9, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos9)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos9)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos9, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos18)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos18)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos18, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos18)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos18)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos18, direction));
                    }
                }
                if ((aorient.get() && playerpitch>30 | playerpitch<-30) || (mode.get() == Modes.Horizontal && !aorient.get())){
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos1)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos1)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos1, direction));
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos1)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos1)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos1, direction));
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos2)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos2)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos2, direction));
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos2)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos2)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos2, direction));
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos3)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos3)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos3, direction));
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos3)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos3)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos3, direction));
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos4)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos4)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos4, direction));
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos4)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos4)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos4, direction));
                }
                if (rotate.get() && BlockUtils.canBreak(blockPos))Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction)));
                else if (!rotate.get() && BlockUtils.canBreak(blockPos))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction));
                if (swing.get()) mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }
            if (shouldMine() && range.get()==4) {
                if (rotate.get() && BlockUtils.canBreak(blockPos))Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction)));
                else if (!rotate.get() && BlockUtils.canBreak(blockPos))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction));
                if ((aorient.get() && playerpitch<=30 && playerpitch>=-30) || (mode.get() == Modes.Vertical && !aorient.get())) {
                    if (playermovingdirection == Direction.NORTH || playermovingdirection == Direction.SOUTH) {
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos2)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos2)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos2, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos2)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos2)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos2, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos1)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos1)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos1, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos1)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos1)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos1, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos9)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos9)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos9, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos9)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos9)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos9, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos18)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos18)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos18, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos18)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos18)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos18, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos10)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos10)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos10, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos10)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos10)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos10, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos11)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos11)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos11, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos11)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos11)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos11, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos19)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos19)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos19, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos19)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos19)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos19, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos20)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos20)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos20, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos20)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos20)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos20, direction));
                    }
                    if (playermovingdirection == Direction.EAST || playermovingdirection == Direction.WEST) {
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos4)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos4)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos4, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos4)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos4)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos4, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos3)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos3)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos3, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos3)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos3)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos3, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos9)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos9)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos9, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos9)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos9)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos9, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos18)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos18)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos18, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos18)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos18)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos18, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos12)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos12)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos12, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos12)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos12)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos12, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos13)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos13)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos13, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos13)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos13)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos13, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos21)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos21)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos21, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos21)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos21)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos21, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos22)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos22)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos22, direction));
                        if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos22)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos22)))
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos22, direction));
                    }
                }
                if ((aorient.get() && playerpitch>30 | playerpitch<-30) || (mode.get() == Modes.Horizontal && !aorient.get())){
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos1)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos1)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos1, direction));
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos1)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos1)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos1, direction));
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos2)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos2)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos2, direction));
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos2)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos2)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos2, direction));
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos3)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos3)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos3, direction));
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos3)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos3)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos3, direction));
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos4)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos4)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos4, direction));
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos4)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos4)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos4, direction));
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos5)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos5)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos5, direction));
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos5)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos5)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos5, direction));
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos6)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos6)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos6, direction));
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos6)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos6)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos6, direction));
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos7)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos7)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos7, direction));
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos7)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos7)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos7, direction));
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos8)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos8)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos8, direction));
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos8)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos8)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos8, direction));
                }
                if (rotate.get() && BlockUtils.canBreak(blockPos))Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction)));
                else if (!rotate.get() && BlockUtils.canBreak(blockPos))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction));
                if (swing.get()) mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }
            if (shouldMine() && range.get()==5) {
                if (rotate.get() && BlockUtils.canBreak(blockPos))Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction)));
                else if (!rotate.get() && BlockUtils.canBreak(blockPos))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos1)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos1)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos1, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos1)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos1)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos1, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos2)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos2)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos2, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos2)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos2)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos2, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos3)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos3)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos3, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos3)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos3)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos3, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos4)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos4)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos4, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos4)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos4)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos4, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos5)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos5)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos5, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos5)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos5)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos5, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos6)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos6)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos6, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos6)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos6)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos6, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos7)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos7)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos7, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos7)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos7)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos7, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos8)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos8)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos8, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos8)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos8)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos8, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos9)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos9)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos9, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos9)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos9)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos9, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos18)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos18)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos18, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos18)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos18)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos18, direction));
                if (rotate.get() && BlockUtils.canBreak(blockPos))Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction)));
                else if (!rotate.get() && BlockUtils.canBreak(blockPos))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction));
                if (swing.get()) mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }
            if (shouldMine() && range.get()==6) {
                if (rotate.get() && BlockUtils.canBreak(blockPos))Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction)));
                else if (!rotate.get() && BlockUtils.canBreak(blockPos))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos1)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos1)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos1, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos1)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos1)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos1, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos2)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos2)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos2, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos2)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos2)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos2, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos3)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos3)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos3, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos3)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos3)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos3, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos4)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos4)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos4, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos4)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos4)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos4, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos5)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos5)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos5, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos5)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos5)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos5, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos6)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos6)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos6, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos6)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos6)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos6, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos7)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos7)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos7, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos7)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos7)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos7, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos8)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos8)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos8, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos8)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos8)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos8, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos9)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos9)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos9, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos9)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos9)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos9, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos10)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos10)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos10, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos10)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos10)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos10, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos11)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos11)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos11, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos11)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos11)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos11, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos12)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos12)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos12, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos12)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos12)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos12, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos13)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos13)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos13, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos13)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos13)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos13, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos18)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos18)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos18, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos18)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos18)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos18, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos19)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos19)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos19, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos19)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos19)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos19, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos20)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos20)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos20, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos20)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos20)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos20, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos21)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos21)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos21, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos21)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos21)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos21, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos22)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos22)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos22, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos22)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos22)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos22, direction));
                if (rotate.get() && BlockUtils.canBreak(blockPos))Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction)));
                else if (!rotate.get() && BlockUtils.canBreak(blockPos))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction));
                if (swing.get()) mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }
            if (shouldMine() && range.get()==7) {
                if (rotate.get() && BlockUtils.canBreak(blockPos))Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction)));
                else if (!rotate.get() && BlockUtils.canBreak(blockPos))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos1)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos1)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos1, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos1)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos1)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos1, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos2)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos2)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos2, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos2)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos2)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos2, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos3)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos3)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos3, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos3)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos3)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos3, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos4)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos4)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos4, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos4)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos4)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos4, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos5)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos5)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos5, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos5)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos5)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos5, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos6)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos6)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos6, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos6)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos6)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos6, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos7)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos7)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos7, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos7)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos7)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos7, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos8)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos8)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos8, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos8)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos8)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos8, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos9)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos9)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos9, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos9)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos9)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos9, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos10)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos10)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos10, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos10)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos10)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos10, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos11)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos11)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos11, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos11)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos11)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos11, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos12)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos12)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos12, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos12)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos12)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos12, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos13)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos13)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos13, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos13)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos13)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos13, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos14)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos14)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos14, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos14)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos14)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos14, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos15)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos15)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos15, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos15)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos15)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos15, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos16)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos16)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos16, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos16)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos16)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos16, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos17)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos17)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos17, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos17)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos17)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos17, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos18)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos18)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos18, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos18)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos18)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos18, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos19)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos19)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos19, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos19)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos19)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos19, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos20)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos20)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos20, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos20)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos20)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos20, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos21)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos21)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos21, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos21)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos21)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos21, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos22)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos22)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos22, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos22)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos22)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos22, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos23)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos23)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos23, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos23)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos23)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos23, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos24)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos24)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos24, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos24)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos24)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos24, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos25)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos25)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos25, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos25)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos25)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos25, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos26)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos26)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos26, direction));
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos26)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos26)))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos26, direction));
                if (rotate.get() && BlockUtils.canBreak(blockPos))Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction)));
                else if (!rotate.get() && BlockUtils.canBreak(blockPos))mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction));
                if (swing.get()) mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }
        } else {
            ticks++;
        }
    }

    private boolean shouldMine() {
        if (blockPos.getY() == -128) return false;
        return !pick.get() || (mc.player.getMainHandStack().getItem() == Items.DIAMOND_PICKAXE || mc.player.getMainHandStack().getItem() == Items.NETHERITE_PICKAXE);
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get() || !shouldMine()) return;
        if (BlockUtils.canBreak(blockPos))event.renderer.box(blockPos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        if (((range.get()==-1 && playermovingdirection==Direction.SOUTH) || (range.get()==1 && playermovingdirection==Direction.NORTH) || (range.get()==2 && (playermovingdirection==Direction.NORTH | playermovingdirection==Direction.SOUTH))) && (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos1)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos1))))event.renderer.box(blockPos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        if (((range.get()==-1 && playermovingdirection==Direction.NORTH) || (range.get()==1 && playermovingdirection==Direction.SOUTH) || (range.get()==2 && (playermovingdirection==Direction.NORTH | playermovingdirection==Direction.SOUTH))) && (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos2)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos2))))event.renderer.box(blockPos2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        if (((range.get()==-1 && playermovingdirection==Direction.WEST) || (range.get()==1 && playermovingdirection==Direction.EAST) || (range.get()==2 && (playermovingdirection==Direction.EAST | playermovingdirection==Direction.WEST))) && (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos3)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos3))))event.renderer.box(blockPos3, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        if (((range.get()==-1 && playermovingdirection==Direction.EAST) || (range.get()==1 && playermovingdirection==Direction.WEST) || (range.get()==2 && (playermovingdirection==Direction.EAST | playermovingdirection==Direction.WEST))) && (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos4)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos4))))event.renderer.box(blockPos4, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        if (range.get()==3){
            if ((aorient.get() && playerpitch<=30 && playerpitch>=-30) || (mode.get() == Modes.Vertical && !aorient.get())) {
                if ((playermovingdirection == Direction.NORTH || playermovingdirection == Direction.SOUTH)) {
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos1)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos1)))
                        event.renderer.box(blockPos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos2)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos2)))
                        event.renderer.box(blockPos2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos9)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos9)))
                        event.renderer.box(blockPos9, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos10)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos18)))
                        event.renderer.box(blockPos18, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                }
                if ((playermovingdirection == Direction.EAST || playermovingdirection == Direction.WEST)) {
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos9)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos9)))
                        event.renderer.box(blockPos9, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos18)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos18)))
                        event.renderer.box(blockPos18, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos3)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos3)))
                        event.renderer.box(blockPos3, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos4)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos4)))
                        event.renderer.box(blockPos4, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                }
            }
            if ((aorient.get() && playerpitch>30 | playerpitch<-30) || (mode.get() == Modes.Horizontal && !aorient.get())){
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos1)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos1)))event.renderer.box(blockPos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos2)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos2)))event.renderer.box(blockPos2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos3)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos3)))event.renderer.box(blockPos3, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos4)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos4)))event.renderer.box(blockPos4, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            }
        }
        if (range.get()==4){
            if ((aorient.get() && playerpitch<=30 && playerpitch>=-30) || (mode.get() == Modes.Vertical && !aorient.get())) {
                if (playermovingdirection == Direction.NORTH || playermovingdirection == Direction.SOUTH) {
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos1)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos1)))
                        event.renderer.box(blockPos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos2)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos2)))
                        event.renderer.box(blockPos2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos9)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos9)))
                        event.renderer.box(blockPos9, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos18)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos18)))
                        event.renderer.box(blockPos18, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos10)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos10)))
                        event.renderer.box(blockPos10, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos11)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos11)))
                        event.renderer.box(blockPos11, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos19)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos19)))
                        event.renderer.box(blockPos19, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos20)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos20)))
                        event.renderer.box(blockPos20, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                }
                if (playermovingdirection == Direction.EAST || playermovingdirection == Direction.WEST) {
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos9)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos9)))
                        event.renderer.box(blockPos9, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos18)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos18)))
                        event.renderer.box(blockPos18, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos3)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos3)))
                        event.renderer.box(blockPos3, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos4)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos4)))
                        event.renderer.box(blockPos4, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos12)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos12)))
                        event.renderer.box(blockPos12, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos13)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos13)))
                        event.renderer.box(blockPos13, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos21)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos21)))
                        event.renderer.box(blockPos21, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos22)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos22)))
                        event.renderer.box(blockPos22, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                }
            }
            if ((aorient.get() && playerpitch>30 | playerpitch<-30) || (mode.get() == Modes.Horizontal && !aorient.get())){
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos1)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos1)))event.renderer.box(blockPos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos2)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos2)))event.renderer.box(blockPos2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos3)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos3)))event.renderer.box(blockPos3, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos4)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos4)))event.renderer.box(blockPos4, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos5)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos5)))event.renderer.box(blockPos5, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos6)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos6)))event.renderer.box(blockPos6, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos7)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos7)))event.renderer.box(blockPos7, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos8)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos8)))event.renderer.box(blockPos8, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            }
        }
        if (range.get()==5){
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos1)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos1)))event.renderer.box(blockPos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos2)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos2)))event.renderer.box(blockPos2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos3)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos3)))event.renderer.box(blockPos3, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos4)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos4)))event.renderer.box(blockPos4, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos5)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos5)))event.renderer.box(blockPos5, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos6)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos6)))event.renderer.box(blockPos6, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos7)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos7)))event.renderer.box(blockPos7, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos8)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos8)))event.renderer.box(blockPos8, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos9)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos9)))event.renderer.box(blockPos9, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos18)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos18)))event.renderer.box(blockPos18, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
        if (range.get()==6){
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos1)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos1)))event.renderer.box(blockPos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos2)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos2)))event.renderer.box(blockPos2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos3)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos3)))event.renderer.box(blockPos3, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos4)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos4)))event.renderer.box(blockPos4, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos5)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos5)))event.renderer.box(blockPos5, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos6)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos6)))event.renderer.box(blockPos6, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos7)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos7)))event.renderer.box(blockPos7, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos8)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos8)))event.renderer.box(blockPos8, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos9)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos9)))event.renderer.box(blockPos9, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos10)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos10)))event.renderer.box(blockPos10, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos11)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos11)))event.renderer.box(blockPos11, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos12)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos12)))event.renderer.box(blockPos12, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos13)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos13)))event.renderer.box(blockPos13, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos18)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos18)))event.renderer.box(blockPos18, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos19)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos19)))event.renderer.box(blockPos19, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos20)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos20)))event.renderer.box(blockPos20, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos21)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos21)))event.renderer.box(blockPos21, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos22)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos22)))event.renderer.box(blockPos22, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
        if (range.get()==7){
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos1)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos1)))event.renderer.box(blockPos1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos2)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos2)))event.renderer.box(blockPos2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos3)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos3)))event.renderer.box(blockPos3, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos4)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos4)))event.renderer.box(blockPos4, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos5)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos5)))event.renderer.box(blockPos5, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos6)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos6)))event.renderer.box(blockPos6, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos7)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos7)))event.renderer.box(blockPos7, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos8)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos8)))event.renderer.box(blockPos8, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos9)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos9)))event.renderer.box(blockPos9, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos10)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos10)))event.renderer.box(blockPos10, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos11)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos11)))event.renderer.box(blockPos11, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos12)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos12)))event.renderer.box(blockPos12, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos13)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos13)))event.renderer.box(blockPos13, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos14)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos14)))event.renderer.box(blockPos14, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos15)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos15)))event.renderer.box(blockPos15, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos16)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos16)))event.renderer.box(blockPos16, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos17)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos17)))event.renderer.box(blockPos17, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos18)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos18)))event.renderer.box(blockPos18, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos19)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos19)))event.renderer.box(blockPos19, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos20)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos20)))event.renderer.box(blockPos20, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos21)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos21)))event.renderer.box(blockPos21, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos22)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos22)))event.renderer.box(blockPos22, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos23)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos23)))event.renderer.box(blockPos23, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos24)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos24)))event.renderer.box(blockPos24, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos25)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos25)))event.renderer.box(blockPos25, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (((mc.player.getAbilities().creativeMode | !(mc.player.getMainHandStack().getItem() instanceof ToolItem)) && BlockUtils.canBreak(blockPos26)) || mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(blockPos26)))event.renderer.box(blockPos26, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    }
    public enum Modes {
        Horizontal, Vertical
    }
}