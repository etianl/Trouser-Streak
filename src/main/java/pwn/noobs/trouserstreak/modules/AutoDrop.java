package pwn.noobs.trouserstreak.modules;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.sync.ComponentChangesHash;
import net.minecraft.screen.sync.ItemStackHash;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import pwn.noobs.trouserstreak.Trouser;

import meteordevelopment.orbit.EventHandler;
import java.util.ArrayList;
import java.util.List;

public class AutoDrop extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<DropMode> dropMode = sgGeneral.add(new EnumSetting.Builder<DropMode>()
            .name("drop-mode")
            .description("How many hotbar slots are dropped.")
            .defaultValue(DropMode.HeldItem)
            .build()
    );
    private final Setting<Boolean> throwmainhand = sgGeneral.add(new BoolSetting.Builder()
            .name("throw-mainhand")
            .defaultValue(true)
            .visible(() -> dropMode.get() == DropMode.HeldItem)
            .build()
    );
    private final Setting<Boolean> throwoffhand = sgGeneral.add(new BoolSetting.Builder()
            .name("throw-offhand")
            .defaultValue(false)
            .visible(() -> dropMode.get() == DropMode.HeldItem)
            .build()
    );
    private final Setting<DropMethod> dropMethod = sgGeneral.add(new EnumSetting.Builder<DropMethod>()
            .name("drop-method")
            .description("How hotbar slots are dropped each tick.")
            .defaultValue(DropMethod.BURST)
            .visible(() -> dropMode.get() == DropMode.SpecifySlots)
            .build()
    );
    private final Setting<Integer> dropsPerTick = sgGeneral.add(new IntSetting.Builder()
            .name("drops-per-tick")
            .description("Maximum number of hotbar slots to drop per tick (1-9).")
            .defaultValue(9)
            .min(1)
            .sliderMax(9)
            .visible(() -> dropMode.get() == DropMode.SpecifySlots)
            .build()
    );
    private final Setting<Boolean> tool = sgGeneral.add(new BoolSetting.Builder()
            .name("No Throw Tools")
            .description("No Throw tools")
            .defaultValue(false)
            .build()
    );

    private final List<Setting<Boolean>> slots = new ArrayList<>(9);

    public AutoDrop() {
        super(Trouser.Main, "auto-drop", "Drops the stack in your selected slot automatically");

        for (int i = 0; i < 9; i++) {
            final int slot = i;
            slots.add(sgGeneral.add(new BoolSetting.Builder()
                    .name("slot " + slot)
                    .description("Drop items from hotbar slot " + slot + ".")
                    .defaultValue(false)
                    .visible(() -> dropMode.get() == DropMode.SpecifySlots)
                    .build()
            ));
        }
    }

    private int nextSlot = 0;

    public static boolean isTool(ItemStack itemStack) {
        return itemStack.isIn(ItemTags.AXES) ||
                itemStack.isIn(ItemTags.HOES) ||
                itemStack.isIn(ItemTags.PICKAXES) ||
                itemStack.isIn(ItemTags.SHOVELS) ||
                itemStack.getItem() instanceof ShearsItem ||
                itemStack.getItem() instanceof FlintAndSteelItem ||
                itemStack.getItem() instanceof BucketItem;
    }

    private boolean isSlotEnabled(int slot) {
        return slot >= 0 && slot < 9 && slots.get(slot).get();
    }

    @Override
    public void onActivate() {
        nextSlot = 0;
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        switch (dropMode.get()){
            case HeldItem -> {
                if (mc.player == null) return;
                PlayerInventory inv = mc.player.getInventory();
                ItemStack offhandStack = inv.getStack(PlayerInventory.OFF_HAND_SLOT);

                if (throwmainhand.get() && (!tool.get() || !isTool(inv.getSelectedStack()))) {
                    mc.player.dropSelectedItem(true);
                }

                if (throwoffhand.get() && !offhandStack.isEmpty() && (!tool.get() || !isTool(offhandStack))) {
                    short offhandSlotId = (short) 45;
                    byte dropButton = (byte) 1;

                    Int2ObjectMap<ItemStackHash> modifiedStacks = new Int2ObjectOpenHashMap<>();

                    ComponentChangesHash.ComponentHasher hasher = mc.getNetworkHandler().getComponentHasher();

                    ItemStackHash offhandHash = ItemStackHash.fromItemStack(offhandStack, hasher);
                    modifiedStacks.put(offhandSlotId, offhandHash);

                    mc.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(
                            mc.player.currentScreenHandler.syncId,
                            mc.player.currentScreenHandler.getRevision(),
                            offhandSlotId,
                            dropButton,
                            SlotActionType.THROW,
                            modifiedStacks,
                            ItemStackHash.EMPTY
                    ));
                }
            }
            case SpecifySlots -> {
                int dropped = 0;
                int limit = Math.min(dropsPerTick.get(), 9);
                byte dropButton = (byte) 1;

                if (dropMethod.get() == DropMethod.BURST) {
                    for (int i = 0; i < 9 && dropped < limit; i++) {
                        if (!isSlotEnabled(i)) continue;
                        ItemStack stack = mc.player.getInventory().getStack(i);
                        if (stack.isEmpty()) continue;
                        if (tool.get() && isTool(stack)) continue;
                        short hotbarSlotId = (short) (36 + i);

                        Int2ObjectMap<ItemStackHash> modifiedStacks = new Int2ObjectOpenHashMap<>();

                        ComponentChangesHash.ComponentHasher hasher = mc.getNetworkHandler().getComponentHasher();

                        ItemStackHash hotbarHash = ItemStackHash.fromItemStack(stack, hasher);
                        modifiedStacks.put(hotbarSlotId, hotbarHash);

                        mc.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(
                                mc.player.currentScreenHandler.syncId,
                                mc.player.currentScreenHandler.getRevision(),
                                hotbarSlotId,
                                dropButton,
                                SlotActionType.THROW,
                                modifiedStacks,
                                ItemStackHash.EMPTY
                        ));
                        dropped++;
                    }
                } else {
                    int startSlot = nextSlot;
                    for (int i = 0; i < 9 && dropped < limit; i++) {
                        int slot = (startSlot + i) % 9;
                        if (!isSlotEnabled(slot)) continue;
                        ItemStack stack = mc.player.getInventory().getStack(slot);
                        if (stack.isEmpty()) continue;
                        if (tool.get() && isTool(stack)) continue;
                        short hotbarSlotId = (short) (36 + slot);

                        Int2ObjectMap<ItemStackHash> modifiedStacks = new Int2ObjectOpenHashMap<>();

                        ComponentChangesHash.ComponentHasher hasher = mc.getNetworkHandler().getComponentHasher();

                        ItemStackHash hotbarHash = ItemStackHash.fromItemStack(stack, hasher);
                        modifiedStacks.put(hotbarSlotId, hotbarHash);

                        mc.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(
                                mc.player.currentScreenHandler.syncId,
                                mc.player.currentScreenHandler.getRevision(),
                                hotbarSlotId,
                                dropButton,
                                SlotActionType.THROW,
                                modifiedStacks,
                                ItemStackHash.EMPTY
                        ));
                        nextSlot = (slot + 1) % 9;
                        dropped++;
                    }
                }
            }
        }
    }
    public enum DropMode {
        HeldItem,
        SpecifySlots
    }
    public enum DropMethod {
        BURST,
        SEQUENTIAL
    }
}