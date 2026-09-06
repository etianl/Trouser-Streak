package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.entity.player.InteractEntityEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.item.Items;
import pwn.noobs.trouserstreak.Trouser;

public class CubePrimer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder()
            .name("swap-back")
            .description("Switch to your previous slot after using the Shears.")
            .defaultValue(true)
            .build()
    );
    public CubePrimer() {
        super(Trouser.Main, "CubePrimer", "Use a Flint and Steel on a TNT Sulfur Cube while Shears are in your hotbar to make it primed and ready to explode the moment it absorbs a TNT item. If it absorbs any other dropped block in this state it will become unkillable.");
    }
    private int previousslot;
    private boolean interacting;

    @Override
    public void onActivate() {
        if (chatFeedback)error("Use a Flint and Steel while Shears are in your hotbar to prime a TNT Sulfur Cube.");
        previousslot = -1;
        interacting = false;
    }
    @EventHandler
    private void onInteract(InteractEntityEvent event) {
        if (!(event.entity instanceof SulfurCube sulfurCube) || interacting) return;
        if (sulfurCube.getBodyArmorItem().getItem() != Items.TNT) return;
        InteractionHand hand = event.hand;
        if (hand == null) return;

        var stack = mc.player.getItemInHand(hand);
        if (stack.getItem() != Items.FLINT_AND_STEEL) return;


        previousslot = mc.player.getInventory().getSelectedSlot();
        FindItemResult shearsResult = InvUtils.findInHotbar(Items.SHEARS);
        if (!shearsResult.found()) {
            if (chatFeedback)error("You need Shears in your hotbar to finish the priming method.");
            event.cancel();
            return;
        }
        try {
            InvUtils.swap(shearsResult.slot(), false);
            interacting = true;
            mc.getConnection().send(new ServerboundInteractPacket(
                    sulfurCube.getId(),
                    mc.player.getUsedItemHand(),
                    sulfurCube.position(),
                    true
            ));
        } finally {
            interacting = false;
            if (swapBack.get()) InvUtils.swap(previousslot, false);
        }
    }
}
