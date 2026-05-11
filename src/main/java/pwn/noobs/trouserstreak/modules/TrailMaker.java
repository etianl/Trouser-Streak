package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import pwn.noobs.trouserstreak.Trouser;

import java.util.List;

public class TrailMaker extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("Trail Blocks")
        .description("Selected blocks for building trails. If you are holding any of these, they will be placed.")
            .defaultValue(Blocks.TNT)
        .build()
    );

    private final Setting<Integer> placeheight = sgGeneral.add(new IntSetting.Builder()
        .name("Height")
        .description("How tall the block placement should go.")
        .defaultValue(1)
        .min(1)
            .max (8)
        .sliderMin(1)
        .sliderMax(8)
        .build()
    );
    private final Setting<Integer> placewidth = sgGeneral.add(new IntSetting.Builder()
            .name("Width")
            .description("How wide the block placement should be.")
            .defaultValue(1)
            .min(1)
            .sliderRange(1,4)
            .build()
    );
    private final Setting<Double> placetickdelay = sgGeneral.add(new DoubleSetting.Builder()
            .name("Placement Delay (Seconds)")
            .description("How long between placements.")
            .defaultValue(0)
            .min(0)
            .sliderMin(0)
            .sliderMax(60)
            .build()
    );
    private final Setting<Integer> attemptplaceticks = sgGeneral.add(new IntSetting.Builder()
            .name("How Long to Attempt Placement (Ticks)")
            .description("How many ticks to attempt block placing at the location")
            .defaultValue(5)
            .min(1)
            .sliderMin(1)
            .sliderMax(60)
            .build()
    );

    private BlockPos rememberedblock;
    private BlockPos currentblock;
    private int placeticks=0;
    public TrailMaker() {
        super(Trouser.Main, "TrailMaker", "Automatically places blocks behind you.");
    }

    @Override
    public void onActivate() {
        rememberedblock=mc.player.blockPosition();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        currentblock=mc.player.blockPosition();

        if (currentblock.getX()> rememberedblock.getX()+1 || currentblock.getX()<rememberedblock.getX()-1 ||currentblock.getY()> rememberedblock.getY()+1 || currentblock.getY()<rememberedblock.getY()-1 || currentblock.getZ()> rememberedblock.getZ()+1 || currentblock.getZ()<rememberedblock.getZ()-1){
            placeticks++;
            if (!blocks.get().isEmpty()) {
                    FindItemResult item = InvUtils.findInHotbar(itemStack -> validItem(itemStack));
                    if (!item.found()) return;
                    else {
                        mc.player.getInventory().setSelectedSlot(item.slot());
                    }
                    if (placeticks<=attemptplaceticks.get()){
                    for (int ph = 0; ph < placeheight.get(); ph++) {
                    if (placewidth.get()==1) {
                        if (mc.level.getBlockState(new BlockPos(rememberedblock.getX(), rememberedblock.getY() + ph, rememberedblock.getZ())).canBeReplaced())
                            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(rememberedblock.getX(), rememberedblock.getY() + ph, rememberedblock.getZ()), Direction.DOWN, new BlockPos(rememberedblock.getX(), rememberedblock.getY() + ph, rememberedblock.getZ()), false));
                    }
                    else if (placewidth.get()==2) {
                        if (mc.level.getBlockState(new BlockPos(rememberedblock.getX(), rememberedblock.getY() + ph, rememberedblock.getZ())).canBeReplaced())
                            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(rememberedblock.getX(), rememberedblock.getY() + ph, rememberedblock.getZ()), Direction.DOWN, new BlockPos(rememberedblock.getX(), rememberedblock.getY() + ph, rememberedblock.getZ()), false));
                        if (mc.level.getBlockState(new BlockPos(rememberedblock.getX()+1, rememberedblock.getY() + ph, rememberedblock.getZ())).canBeReplaced())
                            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(rememberedblock.getX()+1, rememberedblock.getY() + ph, rememberedblock.getZ()), Direction.DOWN, new BlockPos(rememberedblock.getX()+1, rememberedblock.getY() + ph, rememberedblock.getZ()), false));
                        if (mc.level.getBlockState(new BlockPos(rememberedblock.getX()-1, rememberedblock.getY() + ph, rememberedblock.getZ())).canBeReplaced())
                            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(rememberedblock.getX()-1, rememberedblock.getY() + ph, rememberedblock.getZ()), Direction.DOWN, new BlockPos(rememberedblock.getX()-1, rememberedblock.getY() + ph, rememberedblock.getZ()), false));
                        if (mc.level.getBlockState(new BlockPos(rememberedblock.getX(), rememberedblock.getY() + ph, rememberedblock.getZ()+1)).canBeReplaced())
                            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(rememberedblock.getX(), rememberedblock.getY() + ph, rememberedblock.getZ()+1), Direction.DOWN, new BlockPos(rememberedblock.getX(), rememberedblock.getY() + ph, rememberedblock.getZ()+1), false));
                        if (mc.level.getBlockState(new BlockPos(rememberedblock.getX(), rememberedblock.getY() + ph, rememberedblock.getZ()-1)).canBeReplaced())
                            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(rememberedblock.getX(), rememberedblock.getY() + ph, rememberedblock.getZ()-1), Direction.DOWN, new BlockPos(rememberedblock.getX(), rememberedblock.getY() + ph, rememberedblock.getZ()-1), false));
                    }
                    else if (placewidth.get()==3) {
                        if (mc.level.getBlockState(new BlockPos(rememberedblock.getX(), rememberedblock.getY() + ph, rememberedblock.getZ())).canBeReplaced())
                            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(rememberedblock.getX(), rememberedblock.getY() + ph, rememberedblock.getZ()), Direction.DOWN, new BlockPos(rememberedblock.getX(), rememberedblock.getY() + ph, rememberedblock.getZ()), false));
                        if (mc.level.getBlockState(new BlockPos(rememberedblock.getX()+1, rememberedblock.getY() + ph, rememberedblock.getZ())).canBeReplaced())
                            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(rememberedblock.getX()+1, rememberedblock.getY() + ph, rememberedblock.getZ()), Direction.DOWN, new BlockPos(rememberedblock.getX()+1, rememberedblock.getY() + ph, rememberedblock.getZ()), false));
                        if (mc.level.getBlockState(new BlockPos(rememberedblock.getX()-1, rememberedblock.getY() + ph, rememberedblock.getZ())).canBeReplaced())
                            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(rememberedblock.getX()-1, rememberedblock.getY() + ph, rememberedblock.getZ()), Direction.DOWN, new BlockPos(rememberedblock.getX()-1, rememberedblock.getY() + ph, rememberedblock.getZ()), false));
                        if (mc.level.getBlockState(new BlockPos(rememberedblock.getX(), rememberedblock.getY() + ph, rememberedblock.getZ()+1)).canBeReplaced())
                            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(rememberedblock.getX(), rememberedblock.getY() + ph, rememberedblock.getZ()+1), Direction.DOWN, new BlockPos(rememberedblock.getX(), rememberedblock.getY() + ph, rememberedblock.getZ()+1), false));
                        if (mc.level.getBlockState(new BlockPos(rememberedblock.getX(), rememberedblock.getY() + ph, rememberedblock.getZ()-1)).canBeReplaced())
                            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(rememberedblock.getX(), rememberedblock.getY() + ph, rememberedblock.getZ()-1), Direction.DOWN, new BlockPos(rememberedblock.getX(), rememberedblock.getY() + ph, rememberedblock.getZ()-1), false));
                        if (mc.level.getBlockState(new BlockPos(rememberedblock.getX()+1, rememberedblock.getY() + ph, rememberedblock.getZ()+1)).canBeReplaced())
                            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(rememberedblock.getX()+1, rememberedblock.getY() + ph, rememberedblock.getZ()+1), Direction.DOWN, new BlockPos(rememberedblock.getX()+1, rememberedblock.getY() + ph, rememberedblock.getZ()+1), false));
                        if (mc.level.getBlockState(new BlockPos(rememberedblock.getX()+1, rememberedblock.getY() + ph, rememberedblock.getZ()-1)).canBeReplaced())
                            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(rememberedblock.getX()+1, rememberedblock.getY() + ph, rememberedblock.getZ()-1), Direction.DOWN, new BlockPos(rememberedblock.getX()+1, rememberedblock.getY() + ph, rememberedblock.getZ()-1), false));
                        if (mc.level.getBlockState(new BlockPos(rememberedblock.getX()-1, rememberedblock.getY() + ph, rememberedblock.getZ()+1)).canBeReplaced())
                            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(rememberedblock.getX()-1, rememberedblock.getY() + ph, rememberedblock.getZ()+1), Direction.DOWN, new BlockPos(rememberedblock.getX()-1, rememberedblock.getY() + ph, rememberedblock.getZ()+1), false));
                        if (mc.level.getBlockState(new BlockPos(rememberedblock.getX()-1, rememberedblock.getY() + ph, rememberedblock.getZ()-1)).canBeReplaced())
                            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(rememberedblock.getX()-1, rememberedblock.getY() + ph, rememberedblock.getZ()-1), Direction.DOWN, new BlockPos(rememberedblock.getX()-1, rememberedblock.getY() + ph, rememberedblock.getZ()-1), false));
                    }
                    else if (placewidth.get()==4) {
                        if (mc.level.getBlockState(new BlockPos(rememberedblock.getX(), rememberedblock.getY() + ph, rememberedblock.getZ())).canBeReplaced())
                            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(rememberedblock.getX(), rememberedblock.getY() + ph, rememberedblock.getZ()), Direction.DOWN, new BlockPos(rememberedblock.getX(), rememberedblock.getY() + ph, rememberedblock.getZ()), false));
                        if (mc.level.getBlockState(new BlockPos(rememberedblock.getX()+1, rememberedblock.getY() + ph, rememberedblock.getZ())).canBeReplaced())
                            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(rememberedblock.getX()+1, rememberedblock.getY() + ph, rememberedblock.getZ()), Direction.DOWN, new BlockPos(rememberedblock.getX()+1, rememberedblock.getY() + ph, rememberedblock.getZ()), false));
                        if (mc.level.getBlockState(new BlockPos(rememberedblock.getX()-1, rememberedblock.getY() + ph, rememberedblock.getZ())).canBeReplaced())
                            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(rememberedblock.getX()-1, rememberedblock.getY() + ph, rememberedblock.getZ()), Direction.DOWN, new BlockPos(rememberedblock.getX()-1, rememberedblock.getY() + ph, rememberedblock.getZ()), false));
                        if (mc.level.getBlockState(new BlockPos(rememberedblock.getX(), rememberedblock.getY() + ph, rememberedblock.getZ()+1)).canBeReplaced())
                            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(rememberedblock.getX(), rememberedblock.getY() + ph, rememberedblock.getZ()+1), Direction.DOWN, new BlockPos(rememberedblock.getX(), rememberedblock.getY() + ph, rememberedblock.getZ()+1), false));
                        if (mc.level.getBlockState(new BlockPos(rememberedblock.getX(), rememberedblock.getY() + ph, rememberedblock.getZ()-1)).canBeReplaced())
                            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(rememberedblock.getX(), rememberedblock.getY() + ph, rememberedblock.getZ()-1), Direction.DOWN, new BlockPos(rememberedblock.getX(), rememberedblock.getY() + ph, rememberedblock.getZ()-1), false));
                        if (mc.level.getBlockState(new BlockPos(rememberedblock.getX()+1, rememberedblock.getY() + ph, rememberedblock.getZ()+1)).canBeReplaced())
                            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(rememberedblock.getX()+1, rememberedblock.getY() + ph, rememberedblock.getZ()+1), Direction.DOWN, new BlockPos(rememberedblock.getX()+1, rememberedblock.getY() + ph, rememberedblock.getZ()+1), false));
                        if (mc.level.getBlockState(new BlockPos(rememberedblock.getX()+1, rememberedblock.getY() + ph, rememberedblock.getZ()-1)).canBeReplaced())
                            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(rememberedblock.getX()+1, rememberedblock.getY() + ph, rememberedblock.getZ()-1), Direction.DOWN, new BlockPos(rememberedblock.getX()+1, rememberedblock.getY() + ph, rememberedblock.getZ()-1), false));
                        if (mc.level.getBlockState(new BlockPos(rememberedblock.getX()-1, rememberedblock.getY() + ph, rememberedblock.getZ()+1)).canBeReplaced())
                            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(rememberedblock.getX()-1, rememberedblock.getY() + ph, rememberedblock.getZ()+1), Direction.DOWN, new BlockPos(rememberedblock.getX()-1, rememberedblock.getY() + ph, rememberedblock.getZ()+1), false));
                        if (mc.level.getBlockState(new BlockPos(rememberedblock.getX()-1, rememberedblock.getY() + ph, rememberedblock.getZ()-1)).canBeReplaced())
                            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(rememberedblock.getX()-1, rememberedblock.getY() + ph, rememberedblock.getZ()-1), Direction.DOWN, new BlockPos(rememberedblock.getX()-1, rememberedblock.getY() + ph, rememberedblock.getZ()-1), false));
                        if (mc.level.getBlockState(new BlockPos(rememberedblock.getX()+2, rememberedblock.getY() + ph, rememberedblock.getZ())).canBeReplaced())
                            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(rememberedblock.getX()+2, rememberedblock.getY() + ph, rememberedblock.getZ()), Direction.DOWN, new BlockPos(rememberedblock.getX()+2, rememberedblock.getY() + ph, rememberedblock.getZ()), false));
                        if (mc.level.getBlockState(new BlockPos(rememberedblock.getX()-2, rememberedblock.getY() + ph, rememberedblock.getZ())).canBeReplaced())
                            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(rememberedblock.getX()-2, rememberedblock.getY() + ph, rememberedblock.getZ()), Direction.DOWN, new BlockPos(rememberedblock.getX()-2, rememberedblock.getY() + ph, rememberedblock.getZ()), false));
                        if (mc.level.getBlockState(new BlockPos(rememberedblock.getX(), rememberedblock.getY() + ph, rememberedblock.getZ()+2)).canBeReplaced())
                            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(rememberedblock.getX(), rememberedblock.getY() + ph, rememberedblock.getZ()+2), Direction.DOWN, new BlockPos(rememberedblock.getX(), rememberedblock.getY() + ph, rememberedblock.getZ()+2), false));
                        if (mc.level.getBlockState(new BlockPos(rememberedblock.getX(), rememberedblock.getY() + ph, rememberedblock.getZ()-2)).canBeReplaced())
                            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(rememberedblock.getX(), rememberedblock.getY() + ph, rememberedblock.getZ()-2), Direction.DOWN, new BlockPos(rememberedblock.getX(), rememberedblock.getY() + ph, rememberedblock.getZ()-2), false));
                    }
                    mc.player.swing(InteractionHand.MAIN_HAND);
                }
                    }
            if (placeticks>=Math.round(placetickdelay.get()*20)){
            rememberedblock=currentblock;
                placeticks=0;
            }
        }
        }
    }
    private boolean validItem(ItemStack itemStack) {
        if (!(itemStack.getItem() instanceof BlockItem)) return false;

        Block block = ((BlockItem) itemStack.getItem()).getBlock();

        if ( !blocks.get().contains(block)) return false;

        return true;
    }
}