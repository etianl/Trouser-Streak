package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
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
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> toggle = sgGeneral.add(new BoolSetting.Builder()
        .name("toggle")
        .description("toggles after duping")
        .defaultValue(true)
        .build());

    public ShulkerDupe() {
        super(Trouser.Main, "shulker-dupe", "allah helps you duplicate when you open a shuker");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.currentScreen instanceof ShulkerBoxScreen && mc.player != null) {
            HitResult wow = mc.crosshairTarget;
            BlockHitResult a = (BlockHitResult) wow;
            mc.interactionManager.updateBlockBreakingProgress(a.getBlockPos(), Direction.DOWN);
        }
    }

    @EventHandler
    public void onSendPacket(PacketEvent.Sent event) {
        if (event.packet instanceof PlayerActionC2SPacket) {
            if (((PlayerActionC2SPacket) event.packet).getAction() == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK) {
                for (int i = 0; i < 27; i++) {
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                } if (toggle.get()) {
                    toggle();
                }
            }
        }
    }
}
