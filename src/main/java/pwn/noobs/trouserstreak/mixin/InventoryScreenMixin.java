package pwn.noobs.trouserstreak.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import pwn.noobs.trouserstreak.modules.*;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookProvider;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractInventoryScreen<PlayerScreenHandler> implements RecipeBookProvider {
	public InventoryScreenMixin(PlayerScreenHandler container, PlayerInventory playerInventory, Text name) {
		super(container, playerInventory, name);
	}

	@Inject(method = {"init"}, at = { @At("TAIL") })
	protected void init(final CallbackInfo ci) {
		if(Modules.get().isActive(InvDupeModule.class)) {
			addDrawableChild(new ButtonWidget(x + 124, height / 2 - 24, 48, 20, Text.literal("1.17Dupe"), b -> dupe()));
		}
	}

	private void dupe()
	{
		Slot outputSlot = handler.slots.get(0);
		onMouseClick(outputSlot, outputSlot.id, 0, SlotActionType.THROW);
	}
}