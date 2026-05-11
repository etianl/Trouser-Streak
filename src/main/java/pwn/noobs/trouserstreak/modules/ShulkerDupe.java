package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import pwn.noobs.trouserstreak.Trouser;

public class ShulkerDupe extends Module {

    private final SettingGroup sgAutoTool = settings.createGroup("AutoTool");

    public ShulkerDupe() {
        super(Trouser.Main, "shulker-dupe", "ShulkerDupe only works in vanilla, forge, and fabric servers version 1.19 and below.");
    }
    private final Setting<Boolean> autoT = sgAutoTool.add(new BoolSetting.Builder()
            .name("UsePickaxeWhenDupe")
            .description("Uses Pickaxe when breaking shulker.")
            .defaultValue(true)
            .build()
    );
    public static boolean shouldDupe;
    public static boolean shouldDupeAll;
    private boolean timerWASon=false;

    @Override
    public void onActivate() {
        timerWASon=false;
        shouldDupeAll=false;
        shouldDupe=false;
    }

    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (event.screen instanceof ShulkerBoxScreen) {
            shouldDupeAll=false;
            shouldDupe=false;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;
        if (shouldDupe| shouldDupeAll){
            if (Modules.get().get(Timer.class).isActive()) {
                timerWASon=true;
                Modules.get().get(Timer.class).toggle();
            }
            for (int i = 0; i < 8; i++) {
                if (autoT.get() && (mc.player.getInventory().getItem(0).is(ItemTags.PICKAXES) || mc.player.getInventory().getItem(1).is(ItemTags.PICKAXES) ||mc.player.getInventory().getItem(2).is(ItemTags.PICKAXES) ||mc.player.getInventory().getItem(3).is(ItemTags.PICKAXES) ||mc.player.getInventory().getItem(4).is(ItemTags.PICKAXES) ||mc.player.getInventory().getItem(5).is(ItemTags.PICKAXES) ||mc.player.getInventory().getItem(6).is(ItemTags.PICKAXES) ||mc.player.getInventory().getItem(7).is(ItemTags.PICKAXES) ||mc.player.getInventory().getItem(8).is(ItemTags.PICKAXES)) && !(mc.player.getMainHandItem().is(ItemTags.PICKAXES))){
                    mc.player.getInventory().setSelectedSlot(mc.player.getInventory().getSelectedSlot() + 1);
                    if (mc.player.getInventory().getSelectedSlot()>8) mc.player.getInventory().setSelectedSlot(0);
                }
            }
        } else if (!shouldDupe| !shouldDupeAll){
            if (!Modules.get().get(Timer.class).isActive() && timerWASon) {
                timerWASon=false;
                Modules.get().get(Timer.class).toggle();
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.screen instanceof ShulkerBoxScreen && mc.player != null && mc.gameMode != null) {
            HitResult wow = mc.hitResult;
            BlockHitResult a = (BlockHitResult) wow;
            if (shouldDupe| shouldDupeAll){
                mc.gameMode.continueDestroyBlock(a.getBlockPos(), Direction.DOWN);
            }
        }
    }

    @EventHandler
    public void onSendPacket(PacketEvent.Sent event) {
        if (event.packet instanceof ServerboundPlayerActionPacket && mc.gameMode != null && mc.player != null) {
            if (shouldDupeAll){
                if (((ServerboundPlayerActionPacket) event.packet).getAction() == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK) {
                    for (int i = 0; i < 27; i++) {
                        mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, i, 0, ContainerInput.QUICK_MOVE, mc.player);
                    }
                    shouldDupeAll=false;
                }
            } else if (shouldDupe){
                if (((ServerboundPlayerActionPacket) event.packet).getAction() == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK) {
                    mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, 0, 0, ContainerInput.QUICK_MOVE, mc.player);
                    shouldDupe=false;
                }
            }
        }
    }
}