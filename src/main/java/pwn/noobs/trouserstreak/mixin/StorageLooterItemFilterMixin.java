package pwn.noobs.trouserstreak.mixin;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pwn.noobs.trouserstreak.modules.StorageLooter;

import java.util.Locale;

/**
 * Minecraft 26.2 binds item components later during startup than 26.1.x.
 * StorageLooter's item-list filter used Item#getDefaultInstance(), which can
 * therefore throw "Components not bound yet" while Meteor is loading settings.
 *
 * Keep the existing filter behaviour without constructing ItemStacks during
 * startup. Runtime item scoring still uses real ItemStacks once the game is
 * fully initialized.
 */
@Mixin(value = StorageLooter.class, remap = false)
public abstract class StorageLooterItemFilterMixin {
    @Inject(method = "isValidLootItem", at = @At("HEAD"), cancellable = true, remap = false)
    private void trouserStreak$filterWithoutEarlyItemStacks(Item item, CallbackInfoReturnable<Boolean> cir) {
        if (item == null) {
            cir.setReturnValue(false);
            return;
        }

        String itemName = item.toString().toLowerCase(Locale.ROOT);

        // Preserve the original behaviour: only the uncoloured shulker box is
        // accepted when the filter is evaluating shulker-box BlockItems.
        if (item instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
            cir.setReturnValue(itemName.equals("minecraft:shulker_box") || itemName.equals("shulker_box"));
            return;
        }

        // The old filter created a default ItemStack solely to test the tool and
        // armour tags, then allowed only the vanilla diamond variants. Name-based
        // classification avoids creating an ItemStack before components are bound.
        boolean toolOrArmor = itemName.endsWith("_pickaxe")
                || itemName.endsWith("_shovel")
                || itemName.endsWith("_axe")
                || itemName.endsWith("_hoe")
                || itemName.endsWith("_helmet")
                || itemName.endsWith("_chestplate")
                || itemName.endsWith("_leggings")
                || itemName.endsWith("_boots")
                || itemName.equals("minecraft:shears")
                || itemName.equals("shears")
                || itemName.equals("minecraft:flint_and_steel")
                || itemName.equals("flint_and_steel");

        if (toolOrArmor) {
            cir.setReturnValue(itemName.startsWith("minecraft:diamond_") || itemName.startsWith("diamond_"));
            return;
        }

        cir.setReturnValue(true);
    }
}