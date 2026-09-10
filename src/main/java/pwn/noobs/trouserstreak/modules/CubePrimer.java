package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.entity.player.InteractEntityEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import pwn.noobs.trouserstreak.Trouser;

import java.util.List;

public class CubePrimer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder()
            .name("swap-back")
            .description("Switch to your previous slot after using the items.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> insertBlock = sgGeneral.add(new BoolSetting.Builder()
            .name("insert-block")
            .description("Inserts a block into the cube after priming to make it unkillable. Magma Blocks make it burn people. Soul Soil and Soul Sand make it hard to move.")
            .defaultValue(true)
            .build()
    );
    private final Setting<List<Item>> blocks = sgGeneral.add(new ItemListSetting.Builder()
            .name("blocks")
            .description("Blocks to insert into sulfur cubes.")
            .defaultValue(Items.MAGMA_BLOCK, Items.SOUL_SAND, Items.SOUL_SOIL)
            .filter(item -> item.getDefaultInstance().is(ItemTags.SULFUR_CUBE_SWALLOWABLE))
            .visible(insertBlock::get)
            .build()
    );

    public CubePrimer() {
        super(Trouser.Main, "CubePrimer", "Use a Flint and Steel or Fire Charge on a TNT Sulfur Cube while Shears are in your hotbar to make it primed and ready to explode the moment it absorbs a TNT item. If it absorbs any other dropped block in this state it will become unkillable.");
    }
    
    private int previousslot;
    private boolean interacting;

    @Override
    public void onActivate() {
        if (chatFeedback)error("Use a Flint and Steel or Fire Charge while Shears are in your hotbar to prime a TNT Sulfur Cube.");
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
        if (stack.getItem() != Items.FLINT_AND_STEEL && stack.getItem() != Items.FIRE_CHARGE) return;

        previousslot = mc.player.getInventory().getSelectedSlot();

        FindItemResult shearsResult = InvUtils.findInHotbar(Items.SHEARS);

        if (!shearsResult.found()) {
            if (chatFeedback)error("You need Shears in your hotbar to finish the priming method.");
            event.cancel();
            return;
        }

        FindItemResult blockResult = null;

        if (insertBlock.get()){
            for (Item block : blocks.get()){
                blockResult = InvUtils.findInHotbar(block);
                if (blockResult.found()) break;
            }
            if (blockResult == null || !blockResult.found()) {
                if (chatFeedback)error("Block for Sulfur Cube not found.");
            }
        }

        try {
            interacting = true;
            InvUtils.swap(shearsResult.slot(), false);
            mc.getConnection().send(new ServerboundInteractPacket(
                    sulfurCube.getId(),
                    mc.player.getUsedItemHand(),
                    sulfurCube.position(),
                    true
            ));
            if (insertBlock.get() && blockResult.found()){
                InvUtils.swap(blockResult.slot(), false);
                mc.getConnection().send(new ServerboundInteractPacket(
                        sulfurCube.getId(),
                        mc.player.getUsedItemHand(),
                        sulfurCube.position(),
                        true
                ));
            }
        } finally {
            interacting = false;
            if (swapBack.get()) InvUtils.swap(previousslot, false);
        }
    }
}