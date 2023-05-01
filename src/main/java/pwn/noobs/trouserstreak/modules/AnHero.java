/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Flight;
import meteordevelopment.meteorclient.systems.modules.movement.NoFall;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import pwn.noobs.trouserstreak.Trouser;

public class AnHero extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public final Setting<Double> toasterbath = sgGeneral.add(new DoubleSetting.Builder()
            .name("Hero Multiplier")
            .description("The multiplier value for how fast to become an hero.")
            .defaultValue(1)
            .sliderRange(0.5,10)
            .min(0)
            .build()
    );
    public final Setting<Boolean> chatmsg = sgGeneral.add(new BoolSetting.Builder()
            .name("SendAMessage")
            .description("Sends a message before you become an hero.")
            .defaultValue(false)
            .build()
    );
    private final Setting<String> message = sgGeneral.add(new StringSetting.Builder()
            .name("TheMessage")
            .description("What is said before you become an hero.")
            .defaultValue("I Regret Nothing.")
            .visible(() -> chatmsg.get())
            .build());

    public AnHero() {
        super(Trouser.Main, "AnHero", "Become An Hero!");
    }
    private int ticks;
    private boolean nofallwason;

    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (event.screen instanceof DisconnectedScreen) {
            if (nofallwason==true && !Modules.get().get(NoFall.class).isActive()){
                Modules.get().get(NoFall.class).toggle();
            }
            Modules.get().get(Timer.class).setOverride(Timer.OFF);
            toggle();
        }
        if (event.screen instanceof DeathScreen) {
            if (nofallwason==true && !Modules.get().get(NoFall.class).isActive()){
                Modules.get().get(NoFall.class).toggle();
            }
            Modules.get().get(Timer.class).setOverride(Timer.OFF);
            toggle();
        }
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (nofallwason==true && !Modules.get().get(NoFall.class).isActive()){
            Modules.get().get(NoFall.class).toggle();
        }
        Modules.get().get(Timer.class).setOverride(Timer.OFF);
            toggle();
    }
    @Override
    public void onActivate() {
        if (chatmsg.get()){
            ChatUtils.sendPlayerMsg(message.get());
        }
        if (Modules.get().get(Flight.class).isActive()) {
            Modules.get().get(Flight.class).toggle();
        }
        if (Modules.get().get(FlightAntikick.class).isActive()) {
            Modules.get().get(FlightAntikick.class).toggle();
        }
        if (Modules.get().get(TPFly.class).isActive()) {
            Modules.get().get(TPFly.class).toggle();
        }
        if (Modules.get().get(NoFall.class).isActive()) {
            nofallwason=true;
            Modules.get().get(NoFall.class).toggle();
        } else if (!Modules.get().get(NoFall.class).isActive()){
            nofallwason=false;
        }
        ticks=0;
    }
    @Override
    public void onDeactivate() {
        if (nofallwason==true && !Modules.get().get(NoFall.class).isActive()){
            Modules.get().get(NoFall.class).toggle();
        }
        Modules.get().get(Timer.class).setOverride(Timer.OFF);
        ticks=0;
    }
    @EventHandler
    public void onPreTick(TickEvent.Pre event) {
        ticks++;
        Modules.get().get(Timer.class).setOverride(toasterbath.get());
        if (ticks==1){
            mc.player.move(MovementType.SELF, new Vec3d(0, +7,0));
        }else if (ticks==2){
            mc.player.move(MovementType.SELF, new Vec3d(0, +7,0));
        }else if (ticks==3){
            mc.player.move(MovementType.SELF, new Vec3d(0, +7,0));
        }else if (ticks==4){
            mc.player.move(MovementType.SELF, new Vec3d(0, +7,0));
        }else if (ticks==5){
            mc.player.move(MovementType.SELF, new Vec3d(0, +7,0));
        }else if (ticks>=6 && ticks<20){
            mc.player.setVelocity(0.01,-10,0);
        }else if (ticks>=20){
            ticks=0;
        }
        }
    }


