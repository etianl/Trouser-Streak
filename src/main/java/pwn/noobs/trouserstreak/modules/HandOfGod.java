/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
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
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.DisconnectedScreen;

import java.util.List;

public class HandOfGod extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> disableOnLeave = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-on-leave")
        .description("Disables it when you log out.")
        .defaultValue(true)
        .build()
    );


    private final Setting<Boolean> disableOnDisconnect = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-on-disconnect")
        .description("Disables it when you are disconnected.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Integer> pRadius = sgGeneral.add(new IntSetting.Builder()
            .name("PlayerRadius")
            .description("Radius around the player which is removed.")
            .defaultValue(10)
            .min(1)
            .sliderMax(15)
            .build()
    );

    private final Setting<String> block = sgGeneral.add(new StringSetting.Builder()
            .name("ClickBlock")
            .description("What is created when clicking")
            .defaultValue("air")
            .build());

    private final Setting<Integer> cRadius = sgGeneral.add(new IntSetting.Builder()
            .name("ClickRadius")
            .description("The radius of the click fill")
            .defaultValue(10)
            .min(1)
            .sliderMax(15)
            .build());

    public final Setting<Boolean> lightning = sgGeneral.add(new BoolSetting.Builder()
            .name("Lightning")
            .description("Lightning on/off")
            .defaultValue(true)
            .build()
    );

    public HandOfGod() {
        super(Trouser.Main, "HandOfGod", "Deletes the world as you fly around, and replaces blocks with whatever you please when you click. Must be OP");
    }

    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (disableOnDisconnect.get() && event.screen instanceof DisconnectedScreen) {
            toggle();
        }
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (disableOnLeave.get()) toggle();
    }

    @EventHandler
    private void onMouseButton(MouseButtonEvent event) {
        if (mc.options.attackKey.isPressed() && mc.currentScreen == null) {
            HitResult hr = mc.cameraEntity.raycast(300, 0, true);
            Vec3d god = hr.getPos();
            BlockPos pos = new BlockPos(god);
            if (lightning.get()) {
                ItemStack rst = mc.player.getMainHandStack();
                BlockHitResult bhr = new BlockHitResult(mc.player.getEyePos(), Direction.DOWN, new BlockPos(mc.player.getEyePos()), false);
                ItemStack Lightning = new ItemStack(Items.SALMON_SPAWN_EGG);
                NbtCompound tag = new NbtCompound();
                NbtList Pos = new NbtList();
                Pos.add(NbtDouble.of(pos.getX()));
                Pos.add(NbtDouble.of(pos.getY()));
                Pos.add(NbtDouble.of(pos.getZ()));
                tag.put("Pos", Pos);
                tag.putString("id", "minecraft:lightning_bolt");
                Lightning.setSubNbt("EntityTag", tag);
                mc.interactionManager.clickCreativeStack(Lightning, 36 + mc.player.getInventory().selectedSlot);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
            }
            int x1 = Math.round(pos.getX()) + cRadius.get();
            int y1 = Math.round(pos.getY()) + cRadius.get();
            int z1 = Math.round(pos.getZ()) + cRadius.get();
            int x2 = Math.round(pos.getX()) - cRadius.get();
            int y2 = Math.round(pos.getY()) - cRadius.get();
            int z2 = Math.round(pos.getZ()) - cRadius.get();
            ChatUtils.sendPlayerMsg("/fill " + x1 + " " + y1 + " " + z1 + " " + x2 + " " + y2 + " " + z2 + " " + block);
        }
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (!(mc.player.hasPermissionLevel(4))) {
            toggle();
            error("must have op");
        }
            if (mc.options.forwardKey.isPressed()) {
                ChatUtils.sendPlayerMsg("/execute at @p run fill ~"+pRadius.get()+" ~"+pRadius.get()+" ~"+pRadius.get()+" ~-"+pRadius.get()+" ~-"+pRadius.get()+" ~-"+pRadius.get()+" air");
            }
            if (mc.options.jumpKey.isPressed()) {
                ChatUtils.sendPlayerMsg("/execute at @p run fill ~"+pRadius.get()+" ~"+pRadius.get()+" ~"+pRadius.get()+" ~-"+pRadius.get()+" ~-"+pRadius.get()+" ~-"+pRadius.get()+" air");
            }
            if (mc.options.sneakKey.isPressed()) {
                ChatUtils.sendPlayerMsg("/execute at @p run fill ~"+pRadius.get()+" ~"+pRadius.get()+" ~"+pRadius.get()+" ~-"+pRadius.get()+" ~-"+pRadius.get()+" ~-"+pRadius.get()+" air");
            }
            if (mc.options.leftKey.isPressed()) {
                ChatUtils.sendPlayerMsg("/execute at @p run fill ~"+pRadius.get()+" ~"+pRadius.get()+" ~"+pRadius.get()+" ~-"+pRadius.get()+" ~-"+pRadius.get()+" ~-"+pRadius.get()+" air");
            }
            if (mc.options.rightKey.isPressed()) {
                ChatUtils.sendPlayerMsg("/execute at @p run fill ~"+pRadius.get()+" ~"+pRadius.get()+" ~"+pRadius.get()+" ~-"+pRadius.get()+" ~-"+pRadius.get()+" ~-"+pRadius.get()+" air");
            }
            if (mc.options.backKey.isPressed()) {
                ChatUtils.sendPlayerMsg("/execute at @p run fill ~"+pRadius.get()+" ~"+pRadius.get()+" ~"+pRadius.get()+" ~-"+pRadius.get()+" ~-"+pRadius.get()+" ~-"+pRadius.get()+" air");
            }
        }
    }


