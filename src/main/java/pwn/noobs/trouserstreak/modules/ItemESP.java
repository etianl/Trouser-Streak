/**
 * ItemESP
 * =======
 *  - Written by Nataani
 *  - Pulled from the Meteorite module.
 *  This module highlights dropped item entities in the world.
 *  You can configure a list of item types to highlight,
 *  and customize the box appearance (fill opacity, outline, etc.)
 */

package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import pwn.noobs.trouserstreak.Trouser;

import java.util.List;

public class ItemESP extends Module {
    // ----------------------------------------------------------------
    // Setting Groups
    // ----------------------------------------------------------------
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColors = settings.createGroup("Colors");

    // ----------------------------------------------------------------
    // Settings
    // ----------------------------------------------------------------

    /**
     * List of items to highlight. Users can add items from the inventory,
     * making this module highlight only those specific item entities.
     */
    private final Setting<List<Item>> items = sgGeneral.add(new ItemListSetting.Builder()
            .name("items")
            .description("Items to highlight.")
            .defaultValue() // You can specify default items if you want
            .build()
    );

    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the highlight boxes are rendered: lines, sides, or both.")
            .defaultValue(ShapeMode.Both)
            .build()
    );

    private final Setting<Double> fillOpacity = sgGeneral.add(new DoubleSetting.Builder()
            .name("fill-opacity")
            .description("Opacity of the box fill (0 = transparent, 1 = fully opaque).")
            .defaultValue(0.3)
            .range(0, 1)
            .sliderMax(1)
            .build()
    );

    /**
     * The base color used for both the box lines and fill, with optional
     * alpha adjustments for the fill portion.
     */
    private final Setting<SettingColor> colorSetting = sgColors.add(new ColorSetting.Builder()
            .name("highlight-color")
            .description("Color of the highlighted items.")
            .defaultValue(new SettingColor(255, 255, 0, 255)) // default: yellow
            .build()
    );

    // ----------------------------------------------------------------
    // Internal Color Buffers
    // ----------------------------------------------------------------
    private final Color lineColor = new Color();
    private final Color sideColor = new Color();

    // Optional counter for item entities
    private int count = 0;

    // ----------------------------------------------------------------
    // Constructor
    // ----------------------------------------------------------------
    public ItemESP() {
        super(Trouser.Main, "item-esp", "Highlights dropped items on the ground that match a given list.");
    }

    // ----------------------------------------------------------------
    // Rendering
    // ----------------------------------------------------------------
    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (mc.world == null) return;

        count = 0; // Reset item counter each render cycle

        // Loop through all entities, checking for item entities
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof ItemEntity itemEntity) {
                if (shouldHighlight(itemEntity)) {
                    count++;
                    drawBoundingBox(event, itemEntity);
                }
            }
        }
    }

    /**
     * Checks if an ItemEntity's dropped item matches the user-defined highlight list.
     */
    private boolean shouldHighlight(ItemEntity itemEntity) {
        Item droppedItem = itemEntity.getStack().getItem();
        return items.get().contains(droppedItem);
    }

    /**
     * Draws a highlight box around the specified item entity.
     */
    private void drawBoundingBox(Render3DEvent event, ItemEntity entity) {
        // Apply color
        lineColor.set(colorSetting.get().r, colorSetting.get().g, colorSetting.get().b, colorSetting.get().a);
        sideColor.set(lineColor).a((int) (sideColor.a * fillOpacity.get()));

        // Compute interpolated position
        double x = MathHelper.lerp(event.tickDelta, entity.lastRenderX, entity.getX()) - entity.getX();
        double y = MathHelper.lerp(event.tickDelta, entity.lastRenderY, entity.getY()) - entity.getY();
        double z = MathHelper.lerp(event.tickDelta, entity.lastRenderZ, entity.getZ()) - entity.getZ();

        // Get bounding box
        Box box = entity.getBoundingBox();

        // Render
        event.renderer.box(
                x + box.minX, y + box.minY, z + box.minZ,
                x + box.maxX, y + box.maxY, z + box.maxZ,
                sideColor, lineColor, shapeMode.get(), 0
        );
    }

    /**
     * Displays the number of highlighted item entities in the HUD info string.
     */
    @Override
    public String getInfoString() {
        return Integer.toString(count);
    }
}
