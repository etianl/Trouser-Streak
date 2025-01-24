/**
 * ElytraCount
 * =======
 *  - Written by Nataani
 *  - Pulled from the Meteorite module.
 *  This hud feature displays a count of elytra's in your inventory.
 *  You can configure the minimum durability for the item to be counted.
 *  This is helpful when autoswapping Elytra's to have a visual count of available Elytra to swap.
 */

package pwn.noobs.trouserstreak.hud;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class ElytraCount extends HudElement {
    public static final HudElementInfo<ElytraCount> INFO = new HudElementInfo<>(Hud.GROUP, "elytra-count", "Displays a count of elytra's in inventory with configurable minimum durability.", ElytraCount::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgBackground = settings.createGroup("Background");

    // General

    public final Setting<Integer> minDurability = sgGeneral.add(new IntSetting.Builder()
        .name("min-durability")
        .description("Durability threshold to count elytras.")
        .defaultValue(300)
        .range(1, Items.ELYTRA.getComponents().get(DataComponentTypes.MAX_DAMAGE) - 1)
        .sliderRange(1, Items.ELYTRA.getComponents().get(DataComponentTypes.MAX_DAMAGE) - 1)
        .build()
    );

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("none-mode")
        .description("How to render the item when you don't have the specified item in your inventory.")
        .defaultValue(Mode.HideCount)
        .build()
    );

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Scale of the item.")
        .defaultValue(2)
        .onChanged(aDouble -> calculateSize())
        .min(1)
        .sliderRange(1, 4)
        .build()
    );

    private final Setting<Integer> border = sgGeneral.add(new IntSetting.Builder()
        .name("border")
        .description("How much space to add around the element.")
        .defaultValue(0)
        .onChanged(integer -> calculateSize())
        .build()
    );

    // Background

    private final Setting<Boolean> background = sgBackground.add(new BoolSetting.Builder()
        .name("background")
        .description("Displays background.")
        .defaultValue(false)
        .build()
    );

    private final Setting<SettingColor> backgroundColor = sgBackground.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Color used for the background.")
        .visible(background::get)
        .defaultValue(new SettingColor(25, 25, 25, 50))
        .build()
    );

    public ElytraCount() {
        super(INFO);

        calculateSize();
    }

    @Override
    public void setSize(double width, double height) {
        super.setSize(width + border.get() * 2, height + border.get() * 2);
    }

    private void calculateSize() {
        setSize(17 * scale.get(), 17 * scale.get());
    }

    @Override
    public void render(HudRenderer renderer) {

        ItemStack itemStack = new ItemStack(Items.ELYTRA, InvUtils.find(stack ->
            stack.getItem() == Items.ELYTRA &&
                (stack.getMaxDamage() - stack.getDamage()) > minDurability.get()
        ).count());

        if (mode.get() == Mode.HideItem && itemStack.isEmpty()) {
            if (isInEditor()) {
                renderer.line(x, y, x + getWidth(), y + getHeight(), Color.GRAY);
                renderer.line(x, y + getHeight(), x + getWidth(), y, Color.GRAY);
            }
        } else {
            renderer.post(() -> {
                double x = this.x + border.get();
                double y = this.y + border.get();

                render(renderer, itemStack, (int) x, (int) y);
            });
        }

        if (background.get()) renderer.quad(x, y, getWidth(), getHeight(), backgroundColor.get());
    }

    private void render(HudRenderer renderer, ItemStack itemStack, int x, int y) {
        if (mode.get() == Mode.HideItem) {
            renderer.item(itemStack, x, y, scale.get().floatValue(), true);
            return;
        }

        String countOverride = null;
        boolean resetToZero = false;

        if (itemStack.isEmpty()) {
            if (mode.get() == Mode.ShowCount)
                countOverride = "0";

            itemStack.setCount(1);
            resetToZero = true;
        }

        renderer.item(itemStack, x, y, scale.get().floatValue(), true, countOverride);

        if (resetToZero)
            itemStack.setCount(0);
    }

    public enum Mode {
        HideItem,
        HideCount,
        ShowCount;

        @Override
        public String toString() {
            return switch (this) {
                case HideItem -> "Hide Item";
                case HideCount -> "Hide Count";
                case ShowCount -> "Show Count";
            };
        }
    }
}
