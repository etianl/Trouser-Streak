/*
 *  This file is part of the Meteor Tweaks distribution (https://github.com/Declipsonator/Meteor-Tweaks/).
 *  Copyright (c) 2022 Meteor Tweaks.
 *  Licensed Under the GNU Lesser General Public License v3.0
 */
//plz come back meteor tweaks I miss you

package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.AbstractSignEditScreenAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.gui.screen.ingame.HangingSignEditScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.item.HangingSignItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.SignItem;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import pwn.noobs.trouserstreak.Trouser;

import java.util.ArrayList;
import java.util.List;

public class BetterAutoSign extends Module {
    final SettingGroup sgSign = settings.createGroup("Normal Sign Text");
    final SettingGroup sgHang = settings.createGroup("Hanging Sign Text");
    final SettingGroup sgSignAura = settings.createGroup("Sign Aura");
    final SettingGroup sgExtra = settings.createGroup("Visible");

    private final Setting<Boolean> bothside = sgExtra.add(new BoolSetting.Builder()
            .name("both-sides")
            .description("Write on the rear of the signs as well.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> differentText = sgExtra.add(new BoolSetting.Builder()
            .name("Different Text On Rear")
            .description("Writes different text on the rear of the sign.")
            .defaultValue(false)
            .build()
    );
    private final Setting<String> lineOne = sgSign.add(new StringSetting.Builder()
            .name("line-one")
            .description("What to put on the first line of the sign.")
            .defaultValue("Steve")
            .build()
    );
    private final Setting<String> lineTwo = sgSign.add(new StringSetting.Builder()
            .name("line-two")
            .description("What to put on the second line of the sign.")
            .defaultValue("did")
            .build()
    );
    private final Setting<String> lineThree = sgSign.add(new StringSetting.Builder()
            .name("line-three")
            .description("What to put on the third line of the sign.")
            .defaultValue("nothing")
            .build()
    );
    private final Setting<String> lineFour = sgSign.add(new StringSetting.Builder()
            .name("line-four")
            .description("What to put on the fourth line of the sign.")
            .defaultValue("wrong.")
            .build()
    );
    private final Setting<String> lineOnedif = sgSign.add(new StringSetting.Builder()
            .name("rear-line-one")
            .description("What to put on the first line of the sign.")
            .defaultValue("WATCH")
            .visible(differentText::get)
            .build()
    );
    private final Setting<String> lineTwodif = sgSign.add(new StringSetting.Builder()
            .name("rear-line-two")
            .description("What to put on the second line of the sign.")
            .defaultValue("MOUNTAINS")
            .visible(differentText::get)
            .build()
    );
    private final Setting<String> lineThreedif = sgSign.add(new StringSetting.Builder()
            .name("rear-line-three")
            .description("What to put on the third line of the sign.")
            .defaultValue("OF LAVA INC")
            .visible(differentText::get)
            .build()
    );
    private final Setting<String> lineFourdif = sgSign.add(new StringSetting.Builder()
            .name("rear-line-four")
            .description("What to put on the fourth line of the sign.")
            .defaultValue("ON YOUTUBE")
            .visible(differentText::get)
            .build()
    );
    private final Setting<String> HlineOne = sgHang.add(new StringSetting.Builder()
            .name("line-one")
            .description("What to put on the first line of the hanging sign.")
            .defaultValue("Steve")
            .build()
    );
    private final Setting<String> HlineTwo = sgHang.add(new StringSetting.Builder()
            .name("line-two")
            .description("What to put on the second line of the hanging sign.")
            .defaultValue("did")
            .build()
    );
    private final Setting<String> HlineThree = sgHang.add(new StringSetting.Builder()
            .name("line-three")
            .description("What to put on the third line of the hanging sign.")
            .defaultValue("nothing")
            .build()
    );
    private final Setting<String> HlineFour = sgHang.add(new StringSetting.Builder()
            .name("line-four")
            .description("What to put on the fourth line of the hanging sign.")
            .defaultValue("wrong.")
            .build()
    );
    private final Setting<String> HlineOnedif = sgHang.add(new StringSetting.Builder()
            .name("rear-line-one")
            .description("What to put on the first line of the hanging sign.")
            .defaultValue("WATCH")
            .visible(differentText::get)
            .build()
    );
    private final Setting<String> HlineTwodif = sgHang.add(new StringSetting.Builder()
            .name("rear-line-two")
            .description("What to put on the second line of the hanging sign.")
            .defaultValue("MOUNTAINS")
            .visible(differentText::get)
            .build()
    );
    private final Setting<String> HlineThreedif = sgHang.add(new StringSetting.Builder()
            .name("rear-line-three")
            .description("What to put on the third line of the hanging sign.")
            .defaultValue("OF LAVA INC")
            .visible(differentText::get)
            .build()
    );
    private final Setting<String> HlineFourdif = sgHang.add(new StringSetting.Builder()
            .name("rear-line-four")
            .description("What to put on the fourth line of the hanging sign.")
            .defaultValue("ON YOUTUBE")
            .visible(differentText::get)
            .build()
    );
    // based on ChestAura from Meteor Rejects
    private final Setting<Boolean> signAura = sgSignAura.add(new BoolSetting.Builder()
            .name("sign-aura")
            .description("Automatically edits signs for you")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> signAuraRotate = sgSignAura.add(new BoolSetting.Builder()
            .name("sign-aura-rotate")
            .description("Rotates to signs")
            .defaultValue(true)
            .visible(signAura::get)
            .build()
    );
    private final Setting<Double> signAuraRange = sgSignAura.add(new DoubleSetting.Builder()
            .name("sign-aura-range")
            .description("The interact range")
            .defaultValue(4)
            .min(0)
            .max(6)
            .sliderRange(0,6)
            .visible(signAura::get)
            .build()
    );
    private final Setting<Integer> signAuraDelay = sgSignAura.add(new IntSetting.Builder()
            .name("sign-aura-delay")
            .description("Delay between editing signs, in ticks")
            .defaultValue(5)
            .sliderMax(20)
            .visible(signAura::get)
            .build()
    );
    private final Setting<Boolean> autoDye = sgExtra.add(new BoolSetting.Builder()
            .name("auto-dye")
            .description("Dye signs that you place")
            .defaultValue(false)
            .build()
    );
    private final Setting<List<Item>> dyeColors = sgExtra.add(new ItemListSetting.Builder()
            .name("dye-colors")
            .description("What color dyes to dye the sign with.")
            .visible(autoDye::get)
            .filter(this::filter)
            .build()
    );
    private final Setting<Boolean> autoGlow = sgExtra.add(new BoolSetting.Builder()
            .name("auto-glow")
            .description("Makes your signs glow")
            .defaultValue(false)
            .build()
    );

    private boolean editrear = false;
    private BlockPos signPos = new BlockPos(99999999,99999999,99999999);
    private BlockPos prevsignPos = new BlockPos(99999999,99999999,99999999);
    private final ArrayList<BlockPos> openedSigns = new ArrayList<>();
    private int timer = 0;
    private int warningticks = 0;

    public BetterAutoSign() {
        super(Trouser.Main, "Better-auto-sign", "Automatically writes signs and can dye them as well. Credits to MeteorTweaks.");
    }

    @Override
    public void onActivate() {
        warningticks = 0;
        timer = 0;
        openedSigns.clear();
        editrear=false;
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mc.player == null) return;
        if (signAura.get() && mc.player.getMainHandStack().getItem() instanceof HangingSignItem && warningticks<=3){
            warningticks++;
            if (warningticks==2)error("Sign Aura does not work properly with hanging signs when holding a hanging sign.");
        }
        timer--;
        if(!signAura.get() || timer > 0) return;

        for(BlockEntity block : Utils.blockEntities()) {
            if(!(block instanceof SignBlockEntity) || mc.player.getEyePos().distanceTo(Vec3d.ofCenter(block.getPos())) >= signAuraRange.get()) continue;

            BlockPos pos = block.getPos();
            if(openedSigns.contains(pos)) continue;

            Runnable click = () -> mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(pos.getX(), pos.getY(), pos.getZ()), Direction.UP, pos, false));
            if(signAuraRotate.get()) Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), click);
            else click.run();

            openedSigns.add(pos);
            timer = signAuraDelay.get();
            break;
        }
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        if(!(event.screen instanceof SignEditScreen) && !(event.screen instanceof HangingSignEditScreen)) return;

        SignBlockEntity sign = ((AbstractSignEditScreenAccessor) event.screen).getSign();
        if (!(mc.world.getBlockState(sign.getPos()).getBlock().asItem() instanceof HangingSignItem) && mc.world.getBlockState(sign.getPos()).getBlock().asItem() instanceof SignItem){
            mc.player.networkHandler.sendPacket(new UpdateSignC2SPacket(sign.getPos(),true,
                    lineOne.get(),
                    lineTwo.get(),
                    lineThree.get(),
                    lineFour.get()
            ));
            if (bothside.get()){
                editrear = true;
                if (prevsignPos != sign.getPos())signPos = sign.getPos();
            }
        } else if (mc.world.getBlockState(sign.getPos()).getBlock().asItem() instanceof HangingSignItem){
            mc.player.networkHandler.sendPacket(new UpdateSignC2SPacket(sign.getPos(),true,
                    HlineOne.get(),
                    HlineTwo.get(),
                    HlineThree.get(),
                    HlineFour.get()
            ));
            if (bothside.get()){
                editrear = true;
                if (prevsignPos != sign.getPos())signPos = sign.getPos();
            }
        }

        event.cancel();

        BlockHitResult thesign = new BlockHitResult (
                new Vec3d(sign.getPos().getX(), sign.getPos().getY(), sign.getPos().getZ()),
                Direction.UP,
                sign.getPos(),
                true
        );
        if(autoDye.get()) {
            int slot = -1;
            for (int i = 0; i < 36; i++) {
                if (dyeColors.get().contains(mc.player.getInventory().getStack(i).getItem())) {
                    slot = i;
                    break;
                }
            }

            if (slot == -1 && dyeColors.get().contains(mc.player.getOffHandStack().getItem())) slot = 45;
            if (slot != -1) {
                InvUtils.move().from(slot).to(mc.player.getInventory().selectedSlot);

                mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, thesign, 1));

                InvUtils.move().from(mc.player.getInventory().selectedSlot).toHotbar(slot);

            }
        }
        if(autoGlow.get()) {

            int slot = -1;
            for (int i = 0; i < 36; i++) {
                if (mc.player.getInventory().getStack(i).getItem() == Items.GLOW_INK_SAC) {
                    slot = i;
                    break;
                }
            }

            if (slot == -1 && mc.player.getOffHandStack().getItem() == Items.GLOW_INK_SAC) slot = 45;
            if (slot != -1) {
                InvUtils.move().from(slot).to(mc.player.getInventory().selectedSlot);

                mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, thesign, 2));

                InvUtils.move().from(mc.player.getInventory().selectedSlot).toHotbar(slot);

            }
        }
    }
    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        if (!editrear || !bothside.get() || prevsignPos == signPos) return;
        if (!(mc.world.getBlockState(signPos).getBlock().asItem() instanceof HangingSignItem) && mc.world.getBlockState(signPos).getBlock().asItem() instanceof SignItem){
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(signPos.getX(), signPos.getY(), signPos.getZ()), Direction.DOWN, signPos, false));
            if (differentText.get())
                mc.player.networkHandler.sendPacket(new UpdateSignC2SPacket(signPos,false,
                        lineOnedif.get(),
                        lineTwodif.get(),
                        lineThreedif.get(),
                        lineFourdif.get()
                ));
            else
                mc.player.networkHandler.sendPacket(new UpdateSignC2SPacket(signPos,false,
                        lineOne.get(),
                        lineTwo.get(),
                        lineThree.get(),
                        lineFour.get()
                ));
            prevsignPos = signPos;

            editrear=false;
        } else if (mc.world.getBlockState(signPos).getBlock().asItem() instanceof HangingSignItem){
            BlockState blockState = mc.world.getBlockState(signPos);
            if (blockState.getBlock() instanceof WallHangingSignBlock) {
                Direction facing = blockState.get(WallHangingSignBlock.FACING);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(signPos.getX(), signPos.getY(), signPos.getZ()), facing, signPos, false));
            } else if (blockState.getBlock() instanceof HangingSignBlock) {
                int rotation = blockState.get(HangingSignBlock.ROTATION);
                Direction direction = Direction.fromHorizontal(rotation);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(signPos.getX(), signPos.getY(), signPos.getZ()), direction, signPos, false));
            }
            if (differentText.get())
                mc.player.networkHandler.sendPacket(new UpdateSignC2SPacket(signPos,false,
                        HlineOnedif.get(),
                        HlineTwodif.get(),
                        HlineThreedif.get(),
                        HlineFourdif.get()
                ));
            else
                mc.player.networkHandler.sendPacket(new UpdateSignC2SPacket(signPos,false,
                        HlineOne.get(),
                        HlineTwo.get(),
                        HlineThree.get(),
                        HlineFour.get()
                ));
            prevsignPos = signPos;
            editrear=false;
        }
    }
    private boolean filter(Item item) {
        return Items.WHITE_DYE.equals(item)
                || Items.BLACK_DYE.equals(item)
                || Items.BLUE_DYE.equals(item)
                || Items.BROWN_DYE.equals(item)
                || Items.CYAN_DYE.equals(item)
                || Items.GRAY_DYE.equals(item)
                || Items.YELLOW_DYE.equals(item)
                || Items.RED_DYE.equals(item)
                || Items.GREEN_DYE.equals(item)
                || Items.LIGHT_BLUE_DYE.equals(item)
                || Items.ORANGE_DYE.equals(item)
                || Items.LIME_DYE.equals(item)
                || Items.PURPLE_DYE.equals(item)
                || Items.PINK_DYE.equals(item)
                || Items.MAGENTA_DYE.equals(item)
                || Items.LIGHT_GRAY_DYE.equals(item);
    }
}