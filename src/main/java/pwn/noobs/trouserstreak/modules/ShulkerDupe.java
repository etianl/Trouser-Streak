package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import pwn.noobs.trouserstreak.Trouser;

public class ShulkerDupe extends Module {

    public ShulkerDupe() {
        super(Trouser.Main, "shulker-dupe", "ShulkerDupe only works in vanilla, forge, and fabric servers version 1.19 and below.");
    }
    public static boolean shouldDupe;
    public static boolean shouldDupeAll;

    @Override
    public void onActivate() {
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
    private void onTick(TickEvent.Post event) {
        if (mc.currentScreen instanceof ShulkerBoxScreen && mc.player != null) {
            HitResult wow = mc.crosshairTarget;
            BlockHitResult a = (BlockHitResult) wow;
            if (shouldDupe|shouldDupeAll==true){
            mc.interactionManager.updateBlockBreakingProgress(a.getBlockPos(), Direction.DOWN);
        }
        }
    }

    @EventHandler
    public void onSendPacket(PacketEvent.Sent event) {
        if (event.packet instanceof PlayerActionC2SPacket) {
            if (shouldDupeAll==true){
            if (((PlayerActionC2SPacket) event.packet).getAction() == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK) {
                for (int i = 0; i < 27; i++) {
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                }
                shouldDupeAll=false;
            }
            } else if (shouldDupe==true){
            if (((PlayerActionC2SPacket) event.packet).getAction() == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK) {
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 0, 0, SlotActionType.QUICK_MOVE, mc.player);
                    shouldDupe=false;
            }
            }
        }
    }}
