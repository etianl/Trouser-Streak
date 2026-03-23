package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShearsItem;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import pwn.noobs.trouserstreak.Trouser;

import java.util.List;

public class SuperInstaMine extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    public enum listModes {
        whitelist, blacklist
    }
    private final Setting<listModes> listmode = sgGeneral.add(new EnumSetting.Builder<listModes>()
            .name("List Mode")
            .description("Whether to break or not break the block list.")
            .defaultValue(listModes.blacklist)
            .build());
    private final Setting<List<Block>> skippableBlox = sgGeneral.add(new BlockListSetting.Builder()
            .name("Blocks to Skip")
            .description("Skips instamining this block.")
            .visible(() -> listmode.get()==listModes.blacklist)
            .build()
    );
    private final Setting<List<Block>> nonskippableBlox = sgGeneral.add(new BlockListSetting.Builder()
            .name("Blocks to Break")
            .description("Only instamine this block.")
            .visible(() -> listmode.get()==listModes.whitelist)
            .build()
    );
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
    public enum Modes {
        Horizontal, Vertical
    }
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

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
            .name("render")
            .description("Renders a block overlay on the block being broken.")
            .defaultValue(true)
            .build()
    );
    private final Setting<ShapeMode> shape = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build()
    );
    private final Setting<SettingColor> sColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The color of the sides of the blocks being rendered.")
            .defaultValue(new SettingColor(204, 0, 0, 10))
            .build()
    );
    private final Setting<SettingColor> lColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The color of the lines of the blocks being rendered.")
            .defaultValue(new SettingColor(204, 0, 0, 255))
            .build()
    );

    private int ticks;
    private final BlockPos.Mutable[] bPos = new BlockPos.Mutable[27];
    private Direction direction;
    private Direction playerMoveDir;
    private int playerpitch;
    private boolean startBreak = false;

    public SuperInstaMine() {
        super(Trouser.Main, "SuperInstaMine", "Attempts to instantly mine blocks. Modified to be able to break many blocks at a time.");
    }

    @Override
    public void onActivate() {
        startBreak = false;
        ticks = 0;
        for (int i = 0; i < bPos.length; i++) {
            bPos[i] = new BlockPos.Mutable(0, -128, 0);
        }
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (mc.player == null || startBreak) return;
        if (event.packet instanceof PlayerActionC2SPacket){
            PlayerActionC2SPacket actionPacket = (PlayerActionC2SPacket) event.packet;
            if (actionPacket.getAction() == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK){
                direction = actionPacket.getDirection();
                playerMoveDir = mc.player.getMovementDirection();
                playerpitch= Math.round(mc.player.getPitch());
                BlockPos center = actionPacket.getPos();

                bPos[0].set(center);
                bPos[1].set(center.getX()+1, center.getY(), center.getZ());
                bPos[2].set(center.getX()-1, center.getY(), center.getZ());
                bPos[3].set(center.getX(), center.getY(), center.getZ()+1);
                bPos[4].set(center.getX(), center.getY(), center.getZ()-1);
                bPos[5].set(center.getX()+1, center.getY(), center.getZ()+1);
                bPos[6].set(center.getX()-1, center.getY(), center.getZ()-1);
                bPos[7].set(center.getX()+1, center.getY(), center.getZ()-1);
                bPos[8].set(center.getX()-1, center.getY(), center.getZ()+1);

                bPos[9].set( center.getX(), center.getY()+1, center.getZ());
                bPos[10].set(center.getX()+1, center.getY()+1, center.getZ());
                bPos[11].set(center.getX()-1, center.getY()+1, center.getZ());
                bPos[12].set(center.getX(), center.getY()+1, center.getZ()+1);
                bPos[13].set(center.getX(), center.getY()+1, center.getZ()-1);
                bPos[14].set(center.getX()+1, center.getY()+1, center.getZ()+1);
                bPos[15].set(center.getX()-1, center.getY()+1, center.getZ()-1);
                bPos[16].set(center.getX()+1, center.getY()+1, center.getZ()-1);
                bPos[17].set(center.getX()-1, center.getY()+1, center.getZ()+1);

                bPos[18].set(center.getX(), center.getY()-1, center.getZ());
                bPos[19].set(center.getX()+1, center.getY()-1, center.getZ());
                bPos[20].set(center.getX()-1, center.getY()-1, center.getZ());
                bPos[21].set(center.getX(), center.getY()-1, center.getZ()+1);
                bPos[22].set(center.getX(), center.getY()-1, center.getZ()-1);
                bPos[23].set(center.getX()+1, center.getY()-1, center.getZ()+1);
                bPos[24].set(center.getX()-1, center.getY()-1, center.getZ()-1);
                bPos[25].set(center.getX()+1, center.getY()-1, center.getZ()-1);
                bPos[26].set(center.getX()-1, center.getY()-1, center.getZ()+1);
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player  == null || mc.world == null || bPos[0].getY() == -128) return;
        if (ticks >= tickDelay.get()) {
            ticks = 0;

            if (range.get()==-1) {
                doRotatingBreakPacketWithSwing();
                switch (playerMoveDir){
                    case NORTH -> doStartStopBreakPacket(bPos[2]);
                    case SOUTH -> doStartStopBreakPacket(bPos[1]);
                    case EAST -> doStartStopBreakPacket(bPos[4]);
                    case WEST -> doStartStopBreakPacket(bPos[3]);
                }
            }

            if (range.get()==0) {
                doRotatingBreakPacketWithSwing();
            }

            if (range.get()==1) {
                doRotatingBreakPacketWithSwing();
                switch (playerMoveDir){
                    case NORTH -> doStartStopBreakPacket(bPos[1]);
                    case SOUTH -> doStartStopBreakPacket(bPos[2]);
                    case EAST -> doStartStopBreakPacket(bPos[3]);
                    case WEST -> doStartStopBreakPacket(bPos[4]);
                }
            }
            if (range.get()==2) {
                doRotatingBreakPacketWithSwing();
                if (playerMoveDir == Direction.NORTH || playerMoveDir == Direction.SOUTH) {
                    doStartStopBreakPacket(bPos[2]);
                    doStartStopBreakPacket(bPos[1]);
                }
                if (playerMoveDir == Direction.EAST || playerMoveDir == Direction.WEST) {
                    doStartStopBreakPacket(bPos[4]);
                    doStartStopBreakPacket(bPos[3]);
                }
            }
            if (range.get()==3) {
                doRotatingBreakPacketWithSwing();
                if ((aorient.get() && playerpitch<=30 && playerpitch>=-30) || (mode.get() == Modes.Vertical && !aorient.get())) {
                    if (playerMoveDir == Direction.NORTH || playerMoveDir == Direction.SOUTH && (playerpitch <= 30 && playerpitch >= -30)) {
                        List.of(2,1,9,18).forEach(i ->
                                doStartStopBreakPacket(bPos[i])
                        );
                    }
                    if (playerMoveDir == Direction.EAST || playerMoveDir == Direction.WEST && (playerpitch <= 30 && playerpitch >= -30)) {
                        List.of(4,3,9,18).forEach(i ->
                                doStartStopBreakPacket(bPos[i])
                        );
                    }
                }
                if ((aorient.get() && (playerpitch>30 || playerpitch<-30)) || (mode.get() == Modes.Horizontal && !aorient.get())){
                    List.of(1,2,3,4).forEach(i ->
                            doStartStopBreakPacket(bPos[i])
                    );
                }
            }
            if (range.get()==4) {
                doRotatingBreakPacketWithSwing();
                if ((aorient.get() && playerpitch<=30 && playerpitch>=-30) || (mode.get() == Modes.Vertical && !aorient.get())) {
                    if (playerMoveDir == Direction.NORTH || playerMoveDir == Direction.SOUTH) {
                        List.of(2,1,9,18,10,11,19,20).forEach(i ->
                                doStartStopBreakPacket(bPos[i])
                        );
                    }
                    if (playerMoveDir == Direction.EAST || playerMoveDir == Direction.WEST) {
                        List.of(4,3,9,18,12,13,21,22).forEach(i ->
                                doStartStopBreakPacket(bPos[i])
                        );
                    }
                }
                if ((aorient.get() && (playerpitch>30 || playerpitch<-30)) || (mode.get() == Modes.Horizontal && !aorient.get())){
                    List.of(1,2,3,4,5,6,7,8).forEach(i ->
                            doStartStopBreakPacket(bPos[i])
                    );
                }
            }
            if (range.get()==5) {
                doRotatingBreakPacketWithSwing();
                List.of(1,2,3,4,5,6,7,8,9,18).forEach(i ->
                        doStartStopBreakPacket(bPos[i])
                );
            }
            if (range.get()==6) {
                doRotatingBreakPacketWithSwing();
                List.of(1,2,3,4,5,6,7,8,9,10,11,12,13,18,19,20,21,22).forEach(i ->
                        doStartStopBreakPacket(bPos[i])
                );
            }
            if (range.get()==7) {
                doRotatingBreakPacketWithSwing();
                List.of(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26).forEach(i ->
                        doStartStopBreakPacket(bPos[i])
                );
            }
        } else {
            ticks++;
        }
    }

    private boolean shouldBreak(BlockPos pos) {
        if (mc.world == null || mc.player == null) return false;
        Block block = mc.world.getBlockState(pos).getBlock();

        boolean listCheck;
        if (listmode.get() == listModes.whitelist) {
            listCheck = nonskippableBlox.get().contains(block);
        } else {
            listCheck = !skippableBlox.get().contains(block);
        }

        boolean toolCheck;
        if (mc.player.getAbilities().creativeMode || !isTool(mc.player.getMainHandStack())) {
            toolCheck = BlockUtils.canBreak(pos);
        } else {
            toolCheck = mc.player.getMainHandStack().isSuitableFor(mc.world.getBlockState(pos));
        }

        return listCheck && toolCheck;
    }
    public static boolean isTool(ItemStack itemStack) {
        return itemStack.isIn(ItemTags.AXES) || itemStack.isIn(ItemTags.HOES) || itemStack.isIn(ItemTags.PICKAXES) || itemStack.isIn(ItemTags.SHOVELS) || itemStack.getItem() instanceof ShearsItem;
    }
    private void doStartStopBreakPacket(BlockPos bp){
        if (!shouldBreak(bp)) return;
        startBreak=true;
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, bp, direction));
        startBreak=false;
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, bp, direction));
    }
    private void doRotatingBreakPacketWithSwing(){
        if (!shouldBreak(bPos[0])) return;
        if (swing.get()) {
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            mc.player.swingHand(Hand.MAIN_HAND);
        }
        if (rotate.get())Rotations.rotate(Rotations.getYaw(bPos[0]), Rotations.getPitch(bPos[0]), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, bPos[0], direction)));
        else if (!rotate.get())mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, bPos[0], direction));
        if (rotate.get())Rotations.rotate(Rotations.getYaw(bPos[0]), Rotations.getPitch(bPos[0]), () -> {
            startBreak=true;
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, bPos[0], direction));
            startBreak=false;
        });
        else if (!rotate.get()){
            startBreak=true;
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, bPos[0], direction));
            startBreak=false;
        }
    }
    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get() || bPos[0].getY() == -128 || mc.world == null || mc.player == null) return;
        if (shouldBreak(bPos[0]))event.renderer.box(bPos[0], sColor.get(), lColor.get(), shape.get(), 0);
        if (((range.get()==-1 && playerMoveDir==Direction.SOUTH) || (range.get()==1 && playerMoveDir==Direction.NORTH) || (range.get()==2 && (playerMoveDir==Direction.NORTH || playerMoveDir==Direction.SOUTH))) && shouldBreak(bPos[1])) event.renderer.box(bPos[1], sColor.get(), lColor.get(), shape.get(), 0);
        if (((range.get()==-1 && playerMoveDir==Direction.NORTH) || (range.get()==1 && playerMoveDir==Direction.SOUTH) || (range.get()==2 && (playerMoveDir==Direction.NORTH || playerMoveDir==Direction.SOUTH))) && shouldBreak(bPos[2])) event.renderer.box(bPos[2], sColor.get(), lColor.get(), shape.get(), 0);
        if (((range.get()==-1 && playerMoveDir==Direction.WEST) || (range.get()==1 && playerMoveDir==Direction.EAST) || (range.get()==2 && (playerMoveDir==Direction.EAST || playerMoveDir==Direction.WEST))) && shouldBreak(bPos[3])) event.renderer.box(bPos[3], sColor.get(), lColor.get(), shape.get(), 0);
        if (((range.get()==-1 && playerMoveDir==Direction.EAST) || (range.get()==1 && playerMoveDir==Direction.WEST) || (range.get()==2 && (playerMoveDir==Direction.EAST || playerMoveDir==Direction.WEST))) && shouldBreak(bPos[4])) event.renderer.box(bPos[4], sColor.get(), lColor.get(), shape.get(), 0);
        if (range.get()==3){
            if ((aorient.get() && playerpitch<=30 && playerpitch>=-30) || (mode.get() == Modes.Vertical && !aorient.get())) {
                if ((playerMoveDir == Direction.NORTH || playerMoveDir == Direction.SOUTH)) {
                    List.of(1,2,9,18).forEach(i -> {
                        if (shouldBreak(bPos[i])) event.renderer.box(bPos[i], sColor.get(), lColor.get(), shape.get(), 0);
                    });
                }
                if ((playerMoveDir == Direction.EAST || playerMoveDir == Direction.WEST)) {
                    List.of(9,18,3,4).forEach(i -> {
                        if (shouldBreak(bPos[i])) event.renderer.box(bPos[i], sColor.get(), lColor.get(), shape.get(), 0);
                    });
                }
            }
            if ((aorient.get() && (playerpitch>30 || playerpitch<-30)) || (mode.get() == Modes.Horizontal && !aorient.get())){
                List.of(1,2,3,4).forEach(i -> {
                    if (shouldBreak(bPos[i])) event.renderer.box(bPos[i], sColor.get(), lColor.get(), shape.get(), 0);
                });
            }
        }
        if (range.get()==4){
            if ((aorient.get() && playerpitch<=30 && playerpitch>=-30) || (mode.get() == Modes.Vertical && !aorient.get())) {
                if (playerMoveDir == Direction.NORTH || playerMoveDir == Direction.SOUTH) {
                    List.of(1,2,9,18,10,11,19,20).forEach(i -> {
                        if (shouldBreak(bPos[i])) event.renderer.box(bPos[i], sColor.get(), lColor.get(), shape.get(), 0);
                    });
                }
                if (playerMoveDir == Direction.EAST || playerMoveDir == Direction.WEST) {
                    List.of(9,18,3,4,12,13,21,22).forEach(i -> {
                        if (shouldBreak(bPos[i])) event.renderer.box(bPos[i], sColor.get(), lColor.get(), shape.get(), 0);
                    });
                }
            }
            if ((aorient.get() && (playerpitch>30 || playerpitch<-30)) || (mode.get() == Modes.Horizontal && !aorient.get())){
                List.of(1,2,3,4,5,6,7,8).forEach(i -> {
                    if (shouldBreak(bPos[i])) event.renderer.box(bPos[i], sColor.get(), lColor.get(), shape.get(), 0);
                });
            }
        }
        if (range.get()==5){
            List.of(1,2,3,4,5,6,7,8,9,18).forEach(i -> {
                if (shouldBreak(bPos[i])) event.renderer.box(bPos[i], sColor.get(), lColor.get(), shape.get(), 0);
            });
        }
        if (range.get()==6){
            List.of(1,2,3,4,5,6,7,8,9,10,11,12,13,18,19,20,21,22).forEach(i -> {
                if (shouldBreak(bPos[i])) event.renderer.box(bPos[i], sColor.get(), lColor.get(), shape.get(), 0);
            });
        }
        if (range.get()==7){
            List.of(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26).forEach(i -> {
                if (shouldBreak(bPos[i])) event.renderer.box(bPos[i], sColor.get(), lColor.get(), shape.get(), 0);
            });
        }
    }
}