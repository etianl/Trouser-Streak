package pwn.noobs.trouserstreak.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import pwn.noobs.trouserstreak.modules.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractContainerScreen<InventoryMenu> implements RecipeUpdateListener {
	public InventoryScreenMixin(InventoryMenu container, Inventory playerInventory, Component name) {
		super(container, playerInventory, name);
	}

	@Inject(method = {"init"}, at = { @At("TAIL") })
	protected void init(final CallbackInfo ci) {
		if(Modules.get().isActive(InvDupeModule.class)) {
			addRenderableWidget(new Button.Builder(Component.literal("1.17Dupe"), button -> dupe())
					.pos(leftPos + 124, height / 2 - 24)
					.size( 48, 20)
					.build()
			);
		}
	}

	private void dupe()
	{
		Slot outputSlot = menu.slots.get(0);
		slotClicked(outputSlot, outputSlot.index, 0, ContainerInput.THROW);
	}
}