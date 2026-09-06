/*
 *  This file was part of the Meteor Tweaks distribution (https://github.com/Declipsonator/Meteor-Tweaks/).
 *  Copyright (c) 2022 Meteor Tweaks.
 *  Licensed Under the GNU Lesser General Public License v3.0
 */
//plz come back meteor tweaks I miss you

package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.AbstractSignEditScreenAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
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
    private final SettingGroup sgPlace = settings.createGroup("Auto Place");

    private final Setting<Boolean> autoPlace = sgPlace.add(new BoolSetting.Builder()
            .name("auto-place")
            .description("Places a sign on the last block you placed or interacted with.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Integer> placeDelay = sgPlace.add(new IntSetting.Builder()
            .name("place-delay")
            .description("Delay between sign placements in ticks.")
            .defaultValue(0)
            .min(0)
            .sliderMax(10)
            .visible(autoPlace::get)
            .build()
    );
    private final Setting<List<Item>> signTypes = sgPlace.add(new ItemListSetting.Builder()
            .name("do-not-place-these-signs")
            .description("Which signs to NOT use when auto-placing.")
            .filter(item -> item instanceof SignItem)
            .visible(autoPlace::get)
            .build()
    );
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
            .defaultValue("Steve")
            .build()
    );
    private final Setting<String> lineTwo = sgSign.add(new StringSetting.Builder()
            .name("line-two")
            .defaultValue("did")
            .build()
    );
    private final Setting<String> lineThree = sgSign.add(new StringSetting.Builder()
            .name("line-three")
            .defaultValue("nothing")
            .build()
    );
    private final Setting<String> lineFour = sgSign.add(new StringSetting.Builder()
            .name("line-four")
            .defaultValue("wrong.")
            .build()
    );
    private final Setting<String> lineOnedif = sgSign.add(new StringSetting.Builder()
            .name("rear-line-one")
            .defaultValue("WATCH")
            .visible(differentText::get)
            .build()
    );
    private final Setting<String> lineTwodif = sgSign.add(new StringSetting.Builder()
            .name("rear-line-two")
            .defaultValue("MOUNTAINS")
            .visible(differentText::get)
            .build()
    );
    private final Setting<String> lineThreedif = sgSign.add(new StringSetting.Builder()
            .name("rear-line-three")
            .defaultValue("OF LAVA INC")
            .visible(differentText::get)
            .build()
    );
    private final Setting<String> lineFourdif = sgSign.add(new StringSetting.Builder()
            .name("rear-line-four")
            .defaultValue("ON YOUTUBE")
            .visible(differentText::get)
            .build()
    );
    private final Setting<String> HlineOne = sgHang.add(new StringSetting.Builder()
            .name("line-one")
            .defaultValue("Steve")
            .build()
    );
    private final Setting<String> HlineTwo = sgHang.add(new StringSetting.Builder()
            .name("line-two")
            .defaultValue("did")
            .build()
    );
    private final Setting<String> HlineThree = sgHang.add(new StringSetting.Builder()
            .name("line-three")
            .defaultValue("nothing")
            .build()
    );
    private final Setting<String> HlineFour = sgHang.add(new StringSetting.Builder()
            .name("line-four")
            .defaultValue("wrong.")
            .build()
    );
    private final Setting<String> HlineOnedif = sgHang.add(new StringSetting.Builder()
            .name("rear-line-one")
            .defaultValue("WATCH")
            .visible(differentText::get)
            .build()
    );
    private final Setting<String> HlineTwodif = sgHang.add(new StringSetting.Builder()
            .name("rear-line-two")
            .defaultValue("MOUNTAINS")
            .visible(differentText::get)
            .build()
    );
    private final Setting<String> HlineThreedif = sgHang.add(new StringSetting.Builder()
            .name("rear-line-three")
            .defaultValue("OF LAVA INC")
            .visible(differentText::get)
            .build()
    );
    private final Setting<String> HlineFourdif = sgHang.add(new StringSetting.Builder()
            .name("rear-line-four")
            .defaultValue("ON YOUTUBE")
            .visible(differentText::get)
            .build()
    );
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
            .defaultValue(4.0)
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

    private BlockPos editRearPos = null;
    private final ArrayList<BlockPos> openedSigns = new ArrayList<>();
    private int timer = 0;
    private Boolean warned = false;
    private BlockPos lastPlacedBlock = null;
    private Direction lastPlacedSide = Direction.UP;
    private int placeTimer = 0;
    private boolean interactingsign = false;

    public BetterAutoSign() {
        super(Trouser.Main, "Better-auto-sign", "Automatically writes signs and can dye them as well. Credits to MeteorTweaks for the original!");
    }

    @Override
    public void onActivate() {
        resetState();
    }

    @Override
    public void onDeactivate() {
        resetState();
    }

    private void resetState() {
        lastPlacedBlock = null;
        lastPlacedSide = Direction.UP;
        interactingsign = false;
        placeTimer = 0;
        warned = false;
        timer = 0;
        openedSigns.clear();
        editRearPos = null;
    }

    private void placeSign(BlockPos targetPos, Direction lastHitSide) {
        if (mc.player == null || mc.interactionManager == null) return;

        FindItemResult signSlot = InvUtils.findInHotbar(itemStack ->
                itemStack.getItem() instanceof SignItem && !(itemStack.getItem() instanceof HangingSignItem) && !signTypes.get().contains(itemStack.getItem()));

        if (!signSlot.found()) {
            signSlot = InvUtils.findInHotbar(itemStack ->
                    itemStack.getItem() instanceof HangingSignItem && !signTypes.get().contains(itemStack.getItem()));
        }

        if (!signSlot.found()) {
            info("No sign in hotbar!");
            return;
        }

        Item itemInSlot = mc.player.getInventory().getStack(signSlot.slot()).getItem();
        boolean isHangingItem = itemInSlot instanceof HangingSignItem;

        Direction sideToClick;
        BlockPos placeOnPos;

        if (isHangingItem) {
            placeOnPos = targetPos;
            if (lastHitSide == Direction.UP) {
                sideToClick = Direction.DOWN;
            } else {
                sideToClick = lastHitSide;
            }
        } else {
            placeOnPos = targetPos.up();
            sideToClick = Direction.UP;
        }

        int oldSlot = mc.player.getInventory().selectedSlot;
        InvUtils.swap(signSlot.slot(), true);

        Vec3d hitVec = isHangingItem
                ? Vec3d.ofCenter(placeOnPos).add(
                sideToClick.getOffsetX() * 0.5,
                sideToClick.getOffsetY() * 0.5,
                sideToClick.getOffsetZ() * 0.5)
                : Vec3d.ofCenter(placeOnPos).add(0, 1, 0);

        BlockHitResult hitResult = new BlockHitResult(hitVec, sideToClick, placeOnPos, false);

        interactingsign = true;
        mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult, 0));
        interactingsign = false;

        InvUtils.swap(oldSlot, true);
    }

    @EventHandler
    private void onBlockPlace(PacketEvent.Send event) {
        if (mc.world == null || !autoPlace.get() || interactingsign) return;

        if (event.packet instanceof PlayerInteractBlockC2SPacket packet) {
            BlockHitResult hit = packet.getBlockHitResult();
            if (hit.getType() == BlockHitResult.Type.BLOCK) {
                lastPlacedBlock = hit.getBlockPos();
                lastPlacedSide = hit.getSide();
                placeTimer = placeDelay.get();
            }
        }
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mc.player == null) return;
        if (signAura.get() && mc.player.getMainHandStack().getItem() instanceof HangingSignItem && !warned) {
            error("Sign Aura does not work properly with hanging signs when holding a hanging sign.");
            warned = true;
        } else if (!(mc.player.getMainHandStack().getItem() instanceof HangingSignItem)) warned = false;

        if (autoPlace.get()) {
            placeTimer--;
            if (placeTimer <= 0 && lastPlacedBlock != null) {
                placeSign(lastPlacedBlock, lastPlacedSide);
                lastPlacedBlock = null;
                placeTimer = placeDelay.get();
            }
        }

        timer--;
        if (!signAura.get() || timer > 0) return;

        for (BlockEntity block : Utils.blockEntities()) {
            if (!(block instanceof SignBlockEntity) || mc.player.getEyePos().distanceTo(Vec3d.ofCenter(block.getPos())) >= signAuraRange.get()) continue;

            BlockPos pos = block.getPos();
            if (openedSigns.contains(pos)) continue;

            interactingsign = true;
            Runnable click = () -> mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(pos.getX(), pos.getY(), pos.getZ()), Direction.UP, pos, false));
            if (signAuraRotate.get()) Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), click);
            else click.run();
            interactingsign = false;

            openedSigns.add(pos);
            timer = signAuraDelay.get();
            break;
        }
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        if (!(event.screen instanceof SignEditScreen) && !(event.screen instanceof HangingSignEditScreen)) return;

        SignBlockEntity sign = ((AbstractSignEditScreenAccessor) event.screen).meteor$getSign();
        BlockPos pos = sign.getPos();
        boolean isHanging = mc.world.getBlockState(pos).getBlock().asItem() instanceof HangingSignItem;

        boolean isFront = (editRearPos == null || !editRearPos.equals(pos));

        if (isFront) {
            mc.player.networkHandler.sendPacket(new UpdateSignC2SPacket(pos, true,
                    MeteorStarscript.run(MeteorStarscript.compile(isHanging ? HlineOne.get() : lineOne.get())),
                    MeteorStarscript.run(MeteorStarscript.compile(isHanging ? HlineTwo.get() : lineTwo.get())),
                    MeteorStarscript.run(MeteorStarscript.compile(isHanging ? HlineThree.get() : lineThree.get())),
                    MeteorStarscript.run(MeteorStarscript.compile(isHanging ? HlineFour.get() : lineFour.get()))
            ));

            if (bothside.get()) {
                editRearPos = pos;

                BlockState state = mc.world.getBlockState(pos);
                Direction interactSide = Direction.DOWN;

                if (state.getBlock() instanceof WallHangingSignBlock) {
                    interactSide = state.get(WallHangingSignBlock.FACING).getOpposite();
                } else if (state.getBlock() instanceof HangingSignBlock) {
                    int rotation = state.get(HangingSignBlock.ROTATION);
                    interactSide = Direction.fromHorizontalDegrees(rotation).getOpposite();
                }

                interactingsign = true;
                mc.interactionManager.interactBlock(
                        mc.player,
                        Hand.MAIN_HAND,
                        new BlockHitResult(new Vec3d(pos.getX(), pos.getY(), pos.getZ()), interactSide, pos, false)
                );
                interactingsign = false;
            }
        } else {
            boolean diff = differentText.get();
            mc.player.networkHandler.sendPacket(new UpdateSignC2SPacket(pos, false,
                    MeteorStarscript.run(MeteorStarscript.compile(isHanging ? (diff ? HlineOnedif.get() : HlineOne.get()) : (diff ? lineOnedif.get() : lineOne.get()))),
                    MeteorStarscript.run(MeteorStarscript.compile(isHanging ? (diff ? HlineTwodif.get() : HlineTwo.get()) : (diff ? lineTwodif.get() : lineTwo.get()))),
                    MeteorStarscript.run(MeteorStarscript.compile(isHanging ? (diff ? HlineThreedif.get() : HlineThree.get()) : (diff ? lineThreedif.get() : lineThree.get()))),
                    MeteorStarscript.run(MeteorStarscript.compile(isHanging ? (diff ? HlineFourdif.get() : HlineFour.get()) : (diff ? lineFourdif.get() : lineFour.get())))
            ));

            editRearPos = null;
        }

        event.cancel();

        BlockHitResult signHitResult = new BlockHitResult(
                new Vec3d(pos.getX(), pos.getY(), pos.getZ()),
                Direction.UP,
                pos,
                true
        );

        if (autoDye.get()) {
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
                interactingsign = true;
                mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, signHitResult, 1));
                interactingsign = false;
                InvUtils.move().from(mc.player.getInventory().selectedSlot).toHotbar(slot);
            }
        }

        if (autoGlow.get()) {
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
                interactingsign = true;
                mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, signHitResult, 2));
                interactingsign = false;
                InvUtils.move().from(mc.player.getInventory().selectedSlot).toHotbar(slot);
            }
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