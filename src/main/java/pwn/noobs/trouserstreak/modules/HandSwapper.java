package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.entity.HumanoidArm;
import pwn.noobs.trouserstreak.Trouser;

public class HandSwapper extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> swapMainHand = sgGeneral.add(new BoolSetting.Builder()
            .name("swap-main-hand")
            .description("Toggles the main hand between left and right.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> swapItems = sgGeneral.add(new BoolSetting.Builder()
            .name("swap-items")
            .description("Swaps the selected hotbar item with the off-hand item, like pressing F.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> loop = sgGeneral.add(new BoolSetting.Builder()
            .name("loop")
            .description("Continuously repeats the selected actions while the module is enabled.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> interval = sgGeneral.add(new IntSetting.Builder()
            .name("interval")
            .description("Ticks between each repeat when loop is enabled. 20 ticks = 1 second.")
            .defaultValue(1)
            .min(1)
            .sliderMax(100)
            .visible(loop::get)
            .build()
    );

    private int ticksLeft = 0;

    public HandSwapper() {
        super(Trouser.Main, "hand-swapper", "Swaps main hand and/or swaps the main hand item with the off-hand.");
    }

    @Override
    public void onActivate() {
        if (!swapMainHand.get() && !swapItems.get()) {
            error("Enable at least one action.");
            toggle();
            return;
        }

        ticksLeft = 0;
        runActions();

        if (!loop.get()) toggle();
    }

    @Override
    public void onDeactivate() {
        if (mc.player != null) {
            mc.options.save();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!loop.get()) return;

        if (++ticksLeft >= interval.get()) {
            ticksLeft = 0;
            runActions();
        }
    }

    private void runActions() {
        if (mc.player == null || mc.getConnection() == null) return;

        if (swapMainHand.get()) {
            HumanoidArm current = mc.options.mainHand().get();
            HumanoidArm next = current == HumanoidArm.RIGHT
                    ? HumanoidArm.LEFT
                    : HumanoidArm.RIGHT;

            mc.options.mainHand().set(next);
            mc.player.setMainArm(next);
        }

        if (swapItems.get()) {
            mc.getConnection().send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                    mc.player.blockPosition(),
                    mc.player.getDirection()
            ));
        }
    }
}