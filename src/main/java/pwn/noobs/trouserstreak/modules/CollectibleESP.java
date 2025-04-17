package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BannerBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.WallBannerBlock;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.WorldChunk;
import pwn.noobs.trouserstreak.Trouser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class CollectibleESP extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
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
            Items.TRIDENT,
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
            Items.RED_SHULKER_BOX,
            Items.ORANGE_SHULKER_BOX,
            Items.YELLOW_SHULKER_BOX,
            Items.LIME_SHULKER_BOX,
            Items.GREEN_SHULKER_BOX,
            Items.CYAN_SHULKER_BOX,
            Items.LIGHT_BLUE_SHULKER_BOX,
            Items.BLUE_SHULKER_BOX,
            Items.PURPLE_SHULKER_BOX,
            Items.MAGENTA_SHULKER_BOX,
            Items.PINK_SHULKER_BOX,
            Items.WHITE_SHULKER_BOX,
            Items.LIGHT_GRAY_SHULKER_BOX,
            Items.GRAY_SHULKER_BOX,
            Items.BROWN_SHULKER_BOX,
            Items.BLACK_SHULKER_BOX
    ));
    private final Setting<Boolean> highlightBanners = sgGeneral.add(new BoolSetting.Builder()
            .name("Find Banners")
            .description("highlights banners.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> highlightMaps = sgGeneral.add(new BoolSetting.Builder()
            .name("Find Maps")
            .description("highlights item frames that contain maps.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> highlightItems = sgGeneral.add(new BoolSetting.Builder()
            .name("Find Items in Frames")
            .description("highlights item frames that contain items.")
            .defaultValue(true)
            .build()
    );
    private final Setting<List<Item>> items = sgGeneral.add(new ItemListSetting.Builder()
            .name("item-checker")
            .description("Items to check for.")
            .defaultValue(defaultPlayerItems)
            .visible(() -> highlightItems.get())
            .build()
    );
    public final Setting<Boolean> enchants = sgGeneral.add(new BoolSetting.Builder()
            .name("enforce-item-enchants")
            .description("Requires that armor and tools must be enchanted for module to detect.")
            .defaultValue(true)
            .visible(() -> highlightItems.get())
            .build()
    );
    public final Setting<Boolean> certainenchants = sgGeneral.add(new BoolSetting.Builder()
            .name("find-certain-item-enchants")
            .description("Requires that armor and tools must be enchanted with these enchants.")
            .defaultValue(false)
            .visible(() -> enchants.get())
            .build()
    );
    private final Setting<List<Enchantment>> toolenchants = sgGeneral.add(new EnchantmentListSetting.Builder()
            .name("Mining Tool Enchants")
            .description("List of enchantments required.")
            .visible(() -> enchants.get() && certainenchants.get())
            .defaultValue(Enchantments.EFFICIENCY, Enchantments.UNBREAKING, Enchantments.MENDING)
            .build()
    );
    private final Setting<List<Enchantment>> swordenchants = sgGeneral.add(new EnchantmentListSetting.Builder()
            .name("Sword Enchants")
            .description("List of enchantments required.")
            .visible(() -> enchants.get() && certainenchants.get())
            .defaultValue(Enchantments.UNBREAKING, Enchantments.MENDING)
            .build()
    );
    private final Setting<List<Enchantment>> armorenchants = sgGeneral.add(new EnchantmentListSetting.Builder()
            .name("Armor Enchants")
            .description("List of enchantments required.")
            .visible(() -> enchants.get() && certainenchants.get())
            .defaultValue(Enchantments.UNBREAKING, Enchantments.MENDING)
            .build()
    );
    private final Setting<List<Enchantment>> tridentenchants = sgGeneral.add(new EnchantmentListSetting.Builder()
            .name("Trident Enchants")
            .description("List of enchantments required.")
            .visible(() -> enchants.get() && certainenchants.get())
            .defaultValue(Enchantments.UNBREAKING, Enchantments.MENDING)
            .build()
    );
    private final Setting<SettingColor> mapColor = sgColors.add(new ColorSetting.Builder()
            .name("map-color")
            .description("fill color for item frames containing maps.")
            .defaultValue(new SettingColor(255, 255, 0, 50, true))
            .build()
    );

    private final Setting<SettingColor> mapOutlineColor = sgColors.add(new ColorSetting.Builder()
            .name("map-outline-color")
            .description("outline color for item frames containing maps.")
            .defaultValue(new SettingColor(255, 255, 0, 255, true))
            .build()
    );
    private final Setting<SettingColor> itemColor = sgColors.add(new ColorSetting.Builder()
            .name("item-color")
            .description("fill color for item frames containing items.")
            .defaultValue(new SettingColor(255, 255, 0, 50))
            .build()
    );

    private final Setting<SettingColor> itemOutlineColor = sgColors.add(new ColorSetting.Builder()
            .name("item-outline-color")
            .description("outline color for item frames containing items.")
            .defaultValue(new SettingColor(255, 255, 0, 255))
            .build()
    );
    private final Setting<SettingColor> bannerColor = sgColors.add(new ColorSetting.Builder()
            .name("banner-fill")
            .description("fill color for banners.")
            .defaultValue(new SettingColor(255, 0, 0, 50))
            .build()
    );

    private final Setting<SettingColor> bannerOutline = sgColors.add(new ColorSetting.Builder()
            .name("banner-outline")
            .description("outline color for banners.")
            .defaultValue(new SettingColor(255, 0, 0, 255))
            .build()
    );

    public CollectibleESP() {
        super(Trouser.baseHunting,"CollectibleESP", "Highlights collectible items in item frames and banners!");
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.world == null || mc.player == null) return;
        if (highlightMaps.get() || highlightItems.get()) {
            for (Entity frame : mc.world.getEntities()) {
                if (!(frame instanceof ItemFrameEntity itemframe)) continue;
                boolean renderedFrame = false;
                Box box;
                if (highlightMaps.get() && itemframe.getHeldItemStack().getItem().getTranslationKey().equals("item.minecraft.filled_map")){
                    float pitch = itemframe.getPitch();
                    if (pitch == 90 || pitch == -90) {
                        box = itemframe.getBoundingBox().expand(0.12, 0.01, 0.12);
                    } else {
                        if (itemframe.getHorizontalFacing() == Direction.EAST || itemframe.getHorizontalFacing() == Direction.WEST)
                            box = itemframe.getBoundingBox().expand(0.01, 0.12, 0.12);
                        else box = itemframe.getBoundingBox().expand(0.12, 0.12, 0.01);
                    }
                    Color fill = new Color(mapColor.get());
                    Color outline = new Color(mapOutlineColor.get());
                    event.renderer.box(box, fill, outline, ShapeMode.Both, 0);
                    renderedFrame = true;
                }
                if (!renderedFrame && highlightItems.get()) {
                    if (shouldSkip(itemframe.getHeldItemStack())) continue;
                    float pitch = itemframe.getPitch();
                    if (pitch == 90 || pitch == -90) {
                        box = itemframe.getBoundingBox().expand(0.12, 0.01, 0.12);
                    } else {
                        if (itemframe.getHorizontalFacing() == Direction.EAST || itemframe.getHorizontalFacing() == Direction.WEST)
                            box = itemframe.getBoundingBox().expand(0.01, 0.12, 0.12);
                        else box = itemframe.getBoundingBox().expand(0.12, 0.12, 0.01);
                    }
                    Color fill = new Color(itemColor.get());
                    Color outline = new Color(itemOutlineColor.get());
                    event.renderer.box(box, fill, outline, ShapeMode.Both, 0);
                }
            }
        }
        if (highlightBanners.get()) {
            AtomicReferenceArray<WorldChunk> chunks = mc.world.getChunkManager().chunks.chunks;

            for (int i = 0; i < chunks.length(); i++) {
                WorldChunk chunk = chunks.get(i);
                if (chunk != null) {
                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                        if (!(be instanceof BannerBlockEntity banner)) continue;

                        BlockPos pos = banner.getPos();
                        BlockState state = mc.world.getBlockState(pos);
                        Box box;

                        Color fill = new Color(bannerColor.get());
                        Color outline = new Color(bannerOutline.get());

                        if (state.contains(WallBannerBlock.FACING)) {
                            Direction facing = state.get(WallBannerBlock.FACING);
                            double centerX = pos.getX() + 0.5;
                            double centerZ = pos.getZ() + 0.5;
                            double offset = 0.1;
                            double depth = 0.03;
                            double width = 0.45;
                            double y1 = pos.getY() - 0.95;
                            double y2 = pos.getY() + 0.85;

                            switch (facing) {
                                case NORTH:
                                    box = new Box(centerX - width, y1, pos.getZ() + 1 - offset - depth, centerX + width, y2, pos.getZ() + 1 - offset);
                                    break;
                                case SOUTH:
                                    box = new Box(centerX - width, y1, pos.getZ() + offset, centerX + width, y2, pos.getZ() + offset + depth);
                                    break;
                                case WEST:
                                    box = new Box(pos.getX() + 1 - offset - depth, y1, centerZ - width, pos.getX() + 1 - offset, y2, centerZ + width);
                                    break;
                                case EAST:
                                    box = new Box(pos.getX() + offset, y1, centerZ - width, pos.getX() + offset + depth, y2, centerZ + width);
                                    break;
                                default:
                                    continue;
                            }

                            event.renderer.box(box, fill, outline, ShapeMode.Both, 0);
                        } else if (state.contains(BannerBlock.ROTATION)) {
                            int rotation = state.get(BannerBlock.ROTATION);
                            double centerX = pos.getX() + 0.5;
                            double centerZ = pos.getZ() + 0.5;
                            double y1 = pos.getY();
                            double y2 = pos.getY() + 1.85;
                            double angle = Math.toRadians((rotation % 16) * 22.5);
                            double width = 0.45;
                            double depth = -0.05;
                            double outlineOffset = 0.02;

                            double dxx = depth * Math.sin(angle);
                            double dzz = depth * Math.cos(angle);

                            double x1 = centerX - width * Math.cos(angle) + dxx;
                            double z1 = centerZ - width * Math.sin(angle) - dzz;
                            double x2 = centerX + width * Math.cos(angle) + dxx;
                            double z2 = centerZ + width * Math.sin(angle) - dzz;

                            event.renderer.quad(
                                    x1, y1, z1,
                                    x1, y2, z1,
                                    x2, y2, z2,
                                    x2, y1, z2,
                                    fill
                            );

                            double ox = outlineOffset * Math.sin(angle);
                            double oz = outlineOffset * Math.cos(angle);
                            double ox1 = x1 - ox;
                            double oz1 = z1 + oz;
                            double ox2 = x2 - ox;
                            double oz2 = z2 + oz;
                            double ox3 = x2 + ox;
                            double oz3 = z2 - oz;
                            double ox4 = x1 + ox;
                            double oz4 = z1 - oz;

                            event.renderer.quad(
                                    ox1, y2, oz1,
                                    ox2, y2, oz2,
                                    ox3, y2, oz3,
                                    ox4, y2, oz4,
                                    outline
                            );
                            event.renderer.quad(
                                    ox4, y1, oz4,
                                    ox3, y1, oz3,
                                    ox2, y1, oz2,
                                    ox1, y1, oz1,
                                    outline
                            );
                            event.renderer.quad(
                                    ox1, y1, oz1,
                                    ox1, y2, oz1,
                                    ox4, y2, oz4,
                                    ox4, y1, oz4,
                                    outline
                            );
                            event.renderer.quad(
                                    ox2, y1, oz2,
                                    ox3, y1, oz3,
                                    ox3, y2, oz3,
                                    ox2, y2, oz2,
                                    outline
                            );
                        }
                    }
                }
            }
        }
    }
    public boolean shouldSkip(ItemStack stack) {
        boolean skip = false;
        if (enchants.get()) {
            if (!certainenchants.get() && (stack.getItem() instanceof MiningToolItem || stack.getItem() instanceof ArmorItem || stack.getItem() instanceof SwordItem || stack.getItem() instanceof FishingRodItem || stack.getItem() instanceof FlintAndSteelItem || stack.getItem() instanceof ShearsItem || stack.getItem() instanceof ShieldItem || stack.getItem() instanceof TridentItem) && stack.isEnchantable() && stack.getEnchantments().isEmpty()) skip = true;
            else if (certainenchants.get()){
                if (stack.getItem() instanceof ToolItem && !(stack.getItem() instanceof SwordItem)){
                    skip = compareEnchants(stack, toolenchants);
                } else if (stack.getItem() instanceof SwordItem){
                    skip = compareEnchants(stack, swordenchants);
                } else if (stack.getItem() instanceof ArmorItem){
                    skip = compareEnchants(stack, armorenchants);
                } else if (stack.getItem() instanceof TridentItem){
                    skip = compareEnchants(stack, tridentenchants);
                }
            }
        }
        if (!items.get().contains(stack.getItem())) skip = true;
        return skip;
    }
    private boolean compareEnchants(ItemStack stack, Setting<List<Enchantment>> enchantsetting) {
        boolean skip = false;
        Set<Enchantment> itemenchants = new HashSet<>();
        stack.getEnchantments().forEach(enchantmentNbt -> {
            if (enchantmentNbt instanceof NbtCompound nbt) {
                Identifier enchantmentId = new Identifier(nbt.getString("id"));
                Enchantment enchantment = Registries.ENCHANTMENT.get(enchantmentId);
                if (enchantment != null) {
                    itemenchants.add(enchantment);
                }
            }
        });
        for (Enchantment enchantKey : enchantsetting.get()) {
            if (!itemenchants.contains(enchantKey)) {
                skip = true;
                break;
            }
        }
        return skip;
    }
}