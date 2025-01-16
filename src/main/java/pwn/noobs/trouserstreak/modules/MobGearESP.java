// module written by windoid,
// sorta based on the esp and tracer code from meteor client

package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pwn.noobs.trouserstreak.Trouser;

import java.util.*;

public class MobGearESP extends Module {
    private static final Logger log = LoggerFactory.getLogger(MobGearESP.class);
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgColors = settings.createGroup("Colors");

    private final List<Item> defaultPlayerItems = new ArrayList<>(List.of(
            Items.DIAMOND_HELMET,
            Items.DIAMOND_CHESTPLATE,
            Items.DIAMOND_LEGGINGS,
            Items.DIAMOND_BOOTS,
            Items.NETHERITE_HELMET,
            Items.NETHERITE_CHESTPLATE,
            Items.NETHERITE_LEGGINGS,
            Items.NETHERITE_BOOTS,
            Items.ELYTRA,
            Items.DIAMOND_SWORD,
            Items.DIAMOND_AXE,
            Items.DIAMOND_PICKAXE,
            Items.DIAMOND_SHOVEL,
            Items.DIAMOND_HOE,
            Items.NETHERITE_SWORD,
            Items.NETHERITE_AXE,
            Items.NETHERITE_PICKAXE,
            Items.NETHERITE_SHOVEL,
            Items.NETHERITE_HOE,
            Items.ENCHANTED_GOLDEN_APPLE,
            Items.END_CRYSTAL,
            Items.ENDER_CHEST,
            Items.TOTEM_OF_UNDYING,
            Items.EXPERIENCE_BOTTLE,
            Items.SHULKER_BOX,
            Items.CHORUS_FRUIT
    ));

    public MobGearESP() {
        super(Trouser.Main, "MobGearESP", "ESP Module that highlights mobs likely wearing player gear.");
    }

    public final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build()
    );

    public final Setting<Double> fillOpacity = sgGeneral.add(new DoubleSetting.Builder()
            .name("fill-opacity")
            .description("The opacity of the shape fill.")
            .defaultValue(0.3)
            .range(0, 1)
            .sliderMax(1)
            .build()
    );

    private final Setting<Double> fadeDistance = sgGeneral.add(new DoubleSetting.Builder()
            .name("fade-distance")
            .description("The distance from an entity where the color begins to fade.")
            .defaultValue(3)
            .min(0)
            .sliderMax(12)
            .build()
    );

    private final Setting<List<Item>> items = sgGeneral.add(new ItemListSetting.Builder()
            .name("item-checker")
            .description("Player-like items to check for on mob")
            .defaultValue(defaultPlayerItems)
            .build()
    );

    public final Setting<Boolean> enchants = sgGeneral.add(new BoolSetting.Builder()
            .name("enforce-item-enchants")
            .description("Requires that armor and tools must be enchanted for module to detect.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> chatFeedback = sgGeneral.add(new BoolSetting.Builder()
            .name("Chat feedback")
            .description("Display info about mobs holding gear in chat")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> coordsInChat = sgGeneral.add(new BoolSetting.Builder()
            .name("Display coords in chat")
            .description("Display coords of a detected mob")
            .visible(chatFeedback::get)
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> itemsInChat = sgGeneral.add(new BoolSetting.Builder()
            .name("Display found items in chat")
            .description("Display items detected by mod in chat")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> tracers = sgGeneral.add(new BoolSetting.Builder()
            .name("Tracers")
            .description("Add tracers to mobs detected")
            .defaultValue(true)
            .build()
    );
    private final Setting<SettingColor> monstersColor = sgColors.add(new ColorSetting.Builder()
            .name("monsters-color")
            .description("The mob's bounding box and tracer color.")
            .defaultValue(new SettingColor(255, 25, 25, 255))
            .build()
    );
    public final Setting<Boolean> distance = sgColors.add(new BoolSetting.Builder()
            .name("distance-colors")
            .description("Changes the color of tracers depending on distance.")
            .defaultValue(true)
            .build()
    );
    private final Setting<SettingColor> distantColor = sgColors.add(new ColorSetting.Builder()
            .name("distant-color")
            .description("The mob's bounding box and tracer color when you are far away.")
            .defaultValue(new SettingColor(25, 25, 255, 255))
            .visible(distance::get)
            .build()
    );
    public final Setting<Integer> distanceInt = sgColors.add(new IntSetting.Builder()
            .name("distance-colors-threshold")
            .description("The max distance for colors to change.")
            .defaultValue(128)
            .min(1)
            .sliderRange(1, 1024)
            .visible(distance::get)
            .build()
    );

    private final Color lineColor = new Color();
    private final Color sideColor = new Color();
    private final Color baseColor = new Color();

    private int count;
    private final Set<Entity> scannedEntities = Collections.synchronizedSet(new HashSet<>());

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        count = 0;
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity livingEntity)) continue;
            if (shouldSkip(livingEntity)) continue;
            if (!scannedEntities.contains(entity)) { // send chat msg if we haven't scanned mob before
                StringBuilder message = new StringBuilder(entity.getType().getName().getString() + " found most likely wearing player gear");
                if (coordsInChat.get()) message.append(" at ").append(entity.getBlockX()).append(", ").append(entity.getBlockY()).append(", ").append(entity.getBlockZ());
                if (itemsInChat.get()) {
                    ArrayList<Item> playerItems = getPlayerItems(livingEntity);
                    message.append(" holding ");
                    for (Item item : playerItems) {
                        message.append(item.getTranslationKey().split("\\.")[2]).append(", ");
                    }
                    message.setLength(message.length() - 2); // chop off ", " from end of chat message
                }
                ChatUtils.sendMsg(Text.of(message.toString()));
            }
            scannedEntities.add(entity);
            drawBoundingBox(event, entity);
            if (tracers.get()) drawTracer(event, entity);
            count++;
        }
    }

    @Override
    public void onActivate() {
        scannedEntities.clear();
    }
    @Override
    public void onDeactivate() {
        scannedEntities.clear();
    }

    private void drawBoundingBox(Render3DEvent event, Entity entity) {
        Color color = getColor(entity);
        if (color != null) {
            lineColor.set(color);
            sideColor.set(color).a((int) (sideColor.a * fillOpacity.get()));
        }

        double x = MathHelper.lerp(event.tickDelta, entity.lastRenderX, entity.getX()) - entity.getX();
        double y = MathHelper.lerp(event.tickDelta, entity.lastRenderY, entity.getY()) - entity.getY();
        double z = MathHelper.lerp(event.tickDelta, entity.lastRenderZ, entity.getZ()) - entity.getZ();
        Box box = entity.getBoundingBox();
        event.renderer.box(x + box.minX, y + box.minY, z + box.minZ, x + box.maxX, y + box.maxY, z + box.maxZ, sideColor, lineColor, shapeMode.get(), 0);
    }

    private void drawTracer(Render3DEvent event, Entity entity) {
        if (mc.options.hudHidden) return;

        Color baseColor = monstersColor.get();
        if (distance.get()){
            baseColor = getOpposingColor(baseColor, entity);
        }

        double x = entity.prevX + (entity.getX() - entity.prevX) * event.tickDelta;
        double y = entity.prevY + (entity.getY() - entity.prevY) * event.tickDelta;
        double z = entity.prevZ + (entity.getZ() - entity.prevZ) * event.tickDelta;
        double height = entity.getBoundingBox().maxY - entity.getBoundingBox().minY;
        y += height / 2;

        event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z, x, y, z, baseColor);
    }
    private Color getOpposingColor(Color c, Entity e) {
        Color interpolatedColor;
        Color oppositeColor = distantColor.get();

        double distance = Math.sqrt(mc.player.squaredDistanceTo(e));

        double maxDistance = distanceInt.get();
        double percent = MathHelper.clamp(distance / maxDistance, 0, 1);

        int r = (int) (c.r + (oppositeColor.r - c.r) * percent);
        int g = (int) (c.g + (oppositeColor.g - c.g) * percent);
        int b = (int) (c.b + (oppositeColor.b - c.b) * percent);
        int a = c.a;

        interpolatedColor = new Color(r, g, b, a);
        return interpolatedColor;
    }

    private ArrayList<Item> getPlayerItems(LivingEntity livingEntity) {
        ArrayList<Item> playerItems = new ArrayList<>();
        for (ItemStack item  : livingEntity.getArmorItems()) {
            if (enchants.get() && item.isEnchantable() && item.getEnchantments().isEmpty()) continue;
            if (items.get().contains(item.getItem())) playerItems.add(item.getItem());

        }
        for (ItemStack item : livingEntity.getHandItems()) {
            if (enchants.get() && item.isEnchantable() && item.getEnchantments().isEmpty()) continue;
            if (items.get().contains(item.getItem())) playerItems.add(item.getItem());
        }
        return playerItems;
    }

    public boolean shouldSkip(LivingEntity entity) {
        if (entity.isPlayer()) return true;
        ArrayList<Item> playerItems = getPlayerItems(entity);
        if (entity == mc.cameraEntity && mc.options.getPerspective().isFirstPerson()) return true;
        return playerItems.isEmpty() || !EntityUtils.isInRenderDistance(entity);
    }

    public Color getColor(Entity entity) {
        double alpha = getFadeAlpha(entity);
        if (alpha == 0) return null;
        Color color = monstersColor.get();
        if (distance.get()){
            color = getOpposingColor(color, entity);
        }
        return baseColor.set(color.r, color.g, color.b, (int) (color.a * alpha));
    }

    private double getFadeAlpha(Entity entity) {
        double dist = PlayerUtils.squaredDistanceToCamera(entity.getX() + entity.getWidth() / 2, entity.getY() + entity.getEyeHeight(entity.getPose()), entity.getZ() + entity.getWidth() / 2);
        double fadeDist = Math.pow(fadeDistance.get(), 2);
        double alpha = 1;
        if (dist <= fadeDist * fadeDist) alpha = (float) (Math.sqrt(dist) / fadeDist);
        if (alpha <= 0.075) alpha = 0;
        return alpha;
    }

    @Override
    public String getInfoString() {
        return Integer.toString(count);
    }
}