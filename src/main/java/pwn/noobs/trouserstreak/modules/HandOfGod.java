/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package pwn.noobs.trouserstreak.modules;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
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

    public HandOfGod() {
        super(Trouser.Main, "HandOfGod", "God has no mercy.");
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
    public void onTick(TickEvent.Post event) {
        if (!(mc.player.hasPermissionLevel(4))) {
            toggle();
            error("must have op");
        }
            if (mc.options.forwardKey.isPressed()) {
                ChatUtils.sendPlayerMsg("/execute at @p run fill ~15 ~15 ~15 ~-15 ~-15 ~-15 air");
            }
            if (mc.options.jumpKey.isPressed()) {
                ChatUtils.sendPlayerMsg("/execute at @p run fill ~15 ~15 ~15 ~-15 ~-15 ~-15 air");
            }
            if (mc.options.sneakKey.isPressed()) {
                ChatUtils.sendPlayerMsg("/execute at @p run fill ~15 ~15 ~15 ~-15 ~-15 ~-15 air");
            }
        }
    }


