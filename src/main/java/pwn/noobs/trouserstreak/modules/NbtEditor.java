package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.block.entity.Sherds;
import net.minecraft.component.*;
import net.minecraft.component.type.*;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.inventory.ContainerLock;
import net.minecraft.item.*;
import net.minecraft.item.trim.ArmorTrim;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import pwn.noobs.trouserstreak.Trouser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NbtEditor extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgOptions = settings.createGroup("Nbt Options");
    private final SettingGroup sgAEC = settings.createGroup("Area Effect Cloud Options");

    private final Setting<Modes> mode = sgGeneral.add(new EnumSetting.Builder<Modes>()
            .name("mode")
            .description("the mode")
            .defaultValue(Modes.Entity)
            .build());
    public final Setting<Boolean> copyStack = sgOptions.add(new BoolSetting.Builder()
            .name("Copy Itemstack")
            .description("Copies the item as well as NBT data.")
            .defaultValue(false)
            .visible(() -> mode.get() == Modes.Copy)
            .build()
    );
    private final Setting<String> nom = sgGeneral.add(new StringSetting.Builder()
            .name("Custom Name")
            .description("Name the Thing")
            .defaultValue("MOUNTAINSOFLAVAINC")
            .visible(() -> mode.get() == Modes.Entity || mode.get() == Modes.Entity)
            .build());
    private final Setting<String> nomcolor = sgGeneral.add(new StringSetting.Builder()
            .name("Custom Name Color")
            .description("Color the Name")
            .defaultValue("red")
            .visible(() -> mode.get() == Modes.Entity || mode.get() == Modes.Entity)
            .build());
    private final Setting<String> entity = sgOptions.add(new StringSetting.Builder()
            .name("Entity to Spawn")
            .description("What is created. Ex: fireball, villager, minecart, lightning_bolt, magma cube, area effect cloud")
            .defaultValue("wither")
            .visible(() -> mode.get() == Modes.Entity)
            .build());
    /* Item mode doesn't work
    private final Setting<Item> itemlist = sgOptions.add(new ItemSetting.Builder()
            .name("Item to create.")
            .description("Pick one. If you aren't already holding an item this is what you get.")
            .defaultValue(Items.COD)
            .visible(() -> (mode.get() == Modes.Item))
            .build());
     */
    public final Setting<Boolean> customname = sgOptions.add(new BoolSetting.Builder()
            .name("CustomNameVisible")
            .description("CustomNameVisible or not.")
            .defaultValue(true)
            .visible(() -> mode.get() == Modes.Entity)
            .build()
    );

    private final Setting<Integer> health = sgOptions.add(new IntSetting.Builder()
            .name("Health Points")
            .description("How much health.")
            .defaultValue(1000)
            .min(0)
            .sliderRange(0, 10000)
            .visible(() -> mode.get() == Modes.Entity)
            .build());
    private final Setting<Integer> absorption = sgOptions.add(new IntSetting.Builder()
            .name("Absorption Points")
            .description("How much absorption.")
            .defaultValue(100)
            .min(0)
            .sliderRange(0, 10000)
            .visible(() -> mode.get() == Modes.Entity)
            .build());
    private final Setting<Integer> age = sgOptions.add(new IntSetting.Builder()
            .name("Age")
            .description("It's age, 0 is baby.")
            .defaultValue(1)
            .min(0)
            .sliderRange(0, 100)
            .visible(() -> mode.get() == Modes.Entity)
            .build());
    public final Setting<Boolean> invincible = sgOptions.add(new BoolSetting.Builder()
            .name("Invulnerable")
            .description("Invulnerable or not")
            .defaultValue(false)
            .visible(() -> mode.get() == Modes.Entity)
            .build()
    );
    public final Setting<Boolean> persist = sgOptions.add(new BoolSetting.Builder()
            .name("Never Despawn")
            .description("adds PersistenceRequired tag.")
            .defaultValue(false)
            .visible(() -> mode.get() == Modes.Entity)
            .build()
    );
    public final Setting<Boolean> noAI = sgOptions.add(new BoolSetting.Builder()
            .name("NoAI")
            .description("NoAI")
            .defaultValue(false)
            .visible(() -> mode.get() == Modes.Entity)
            .build()
    );
    public final Setting<Boolean> falsefire = sgOptions.add(new BoolSetting.Builder()
            .name("HasVisualFire")
            .description("HasVisualFire or not")
            .defaultValue(false)
            .visible(() -> mode.get() == Modes.Entity)
            .build()
    );
    public final Setting<Boolean> nograv = sgOptions.add(new BoolSetting.Builder()
            .name("NoGravity")
            .description("NoGravity or not")
            .defaultValue(false)
            .visible(() -> mode.get() == Modes.Entity)
            .build()
    );
    public final Setting<Boolean> silence = sgOptions.add(new BoolSetting.Builder()
            .name("Silent")
            .description("adds Silent tag.")
            .defaultValue(false)
            .visible(() -> mode.get() == Modes.Entity)
            .build()
    );
    public final Setting<Boolean> glow = sgOptions.add(new BoolSetting.Builder()
            .name("Glowing")
            .description("Glowing or not")
            .defaultValue(false)
            .visible(() -> mode.get() == Modes.Entity)
            .build()
    );
    public final Setting<Boolean> ignite = sgOptions.add(new BoolSetting.Builder()
            .name("Ignited")
            .description("Pre-ignite creeper or not.")
            .defaultValue(false)
            .visible(() -> mode.get() == Modes.Entity)
            .build()
    );
    public final Setting<Boolean> powah = sgOptions.add(new BoolSetting.Builder()
            .name("Charged Creeper")
            .description("powered creeper or not.")
            .defaultValue(false)
            .visible(() -> mode.get() == Modes.Entity)
            .build()
    );
    private final Setting<Integer> fuse = sgOptions.add(new IntSetting.Builder()
            .name("Creeper/TNT Fuse")
            .description("In ticks")
            .defaultValue(20)
            .min(0)
            .sliderRange(0, 120)
            .visible(() -> mode.get() == Modes.Entity)
            .build());
    private final Setting<Integer> exppower = sgOptions.add(new IntSetting.Builder()
            .name("ExplosionPower/Radius")
            .description("For Creepers and Fireballs")
            .defaultValue(10)
            .min(1)
            .sliderMax(127)
            .visible(() -> mode.get() == Modes.Entity)
            .build());
    private final Setting<Integer> size = sgOptions.add(new IntSetting.Builder()
            .name("Slime/Magma Cube Size")
            .description("It's size, 100 is really big.")
            .defaultValue(1)
            .min(0)
            .sliderRange(0, 100)
            .visible(() -> mode.get() == Modes.Entity)
            .build());
    private final Setting<pModes> potionmode = sgOptions.add(new EnumSetting.Builder<pModes>()
            .name("Potion Options")
            .description("the mode")
            .defaultValue(pModes.Splash)
            .visible(() -> mode.get() == Modes.Potion)
            .build());
    private final Setting<List<StatusEffect>> effects = sgOptions.add(new StatusEffectListSetting.Builder()
            .name("Effects")
            .description("List of potion effects.")
            .defaultValue(StatusEffects.INSTANT_HEALTH.value())
            .visible(() -> mode.get() == Modes.Potion)
            .build()
    );
    private final Setting<String> ceffect = sgAEC.add(new StringSetting.Builder()
            .name("Area Effect Cloud Effects")
            .description("Cloud Potion Effect. Examples: harming, strong_harming, healing, healing, invisibility, long_invisibility, poison, long_poison, strong_poison")
            .defaultValue("strong_harming")
            .visible(() -> mode.get() == Modes.Entity)
            .build());
    private final Setting<Integer> cloudduration = sgAEC.add(new IntSetting.Builder()
            .name("Cloud Duration(ticks)")
            .description("Cloud Duration in ticks")
            .defaultValue(10000)
            .min(0)
            .sliderRange(0, 20000)
            .visible(() -> mode.get() == Modes.Entity)
            .build());
    private final Setting<Integer> cloudradius = sgAEC.add(new IntSetting.Builder()
            .name("Cloud Radius")
            .description("Cloud Radius in blocks")
            .defaultValue(100)
            .min(0)
            .sliderRange(0, 200)
            .visible(() -> mode.get() == Modes.Entity)
            .build());
    private final Setting<String> particle = sgAEC.add(new StringSetting.Builder()
            .name("Cloud Particle")
            .description("Cloud particles. Examples: note, flame, block cobblestone, item apple, item lava_bucket, block_marker dirt, falling_dust sand")
            .defaultValue("block_marker fire")
            .visible(() -> mode.get() == Modes.Entity)
            .build());
    private final Setting<Integer> duration = sgOptions.add(new IntSetting.Builder()
            .name("Potion Duration(ticks)")
            .description("Potion Duration in ticks")
            .defaultValue(10000)
            .min(0)
            .sliderRange(0, 20000)
            .visible(() -> mode.get() == Modes.Potion)
            .build());
    private final Setting<Integer> amplifier = sgOptions.add(new IntSetting.Builder()
            .name("Potion Amplifier")
            .description("Potion Amplifier")
            .defaultValue(125)
            .min(0)
            .sliderRange(0, 255)
            .visible(() -> mode.get() == Modes.Potion)
            .build());
    /*Item mode doesn't work
    private final Setting<List<Enchantment>> enchants = sgOptions.add(new EnchantmentListSetting.Builder()
            .name("Enchants")
            .description("List of enchantments.")
            .defaultValue(Enchantments.KNOCKBACK)
            .visible(() -> mode.get() == Modes.Item)
            .build());
    private final Setting<Integer> level = sgOptions.add(new IntSetting.Builder()
            .name("Enchantment Level")
            .description("Enchantment Level.")
            .defaultValue(255)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());
     */

    public NbtEditor() {
        super(Trouser.Main, "NbtEditor", " CREATIVE MODE REQUIRED. Creates custom entities (spawn eggs) and enchanted items based on your specified options.");
    }
    @Override
    public void onActivate() {
        if (mc.player.getAbilities().creativeMode) {
            switch (mode.get()) {
                case Entity -> {
                    ItemStack item = new ItemStack(Items.BEE_SPAWN_EGG);
                    var changes = ComponentChanges.builder()
                            .add(DataComponentTypes.CUSTOM_NAME, Text.literal(nom.get()).formatted(Formatting.valueOf(nomcolor.get().toUpperCase())))
                            .add(DataComponentTypes.ITEM_NAME, Text.literal(nom.get()).formatted(Formatting.valueOf(nomcolor.get().toUpperCase())))
                            .add(DataComponentTypes.ENTITY_DATA, createEntityData())
                            .build();
                    item.applyChanges(changes);
                    mc.interactionManager.clickCreativeStack(item, 36 + mc.player.getInventory().selectedSlot);
                }
                /* This doesn't work but it's the closest I got to making something work
                case Item -> {
                    ItemStack item = new ItemStack(Items.CARROT_ON_A_STICK);

                    if (!mc.player.getMainHandStack().isEmpty()) {
                        item = mc.player.getMainHandStack().copy();
                    } else {
                        item = new ItemStack(itemlist.get());
                    }

                    // Add enchantments to the item
                    ComponentChanges.Builder enchantmentsBuilder = ComponentChanges.builder();
                    for (Enchantment enchant : enchants.get()) {
                        String enchantmentKey = enchant.getTranslationKey();
                        String enchantmentName = enchantmentKey.substring(enchantmentKey.lastIndexOf(".") + 1);
                        ItemEnchantmentsComponent enchantmentComponent = new ItemEnchantmentsComponent("minecraft:" + enchantmentName, level.get());
                        enchantmentsBuilder.add(DataComponentTypes.ENCHANTMENTS, enchantmentComponent);
                    }
                    item.applyChanges(enchantmentsBuilder.build());

                    // Set the custom name
                    item.set(DataComponentTypes.CUSTOM_NAME, Text.literal(nom.get()).formatted(Formatting.valueOf(nomcolor.get().toUpperCase())));

                    mc.interactionManager.clickCreativeStack(item, 36 + mc.player.getInventory().selectedSlot);
                }*/
                case Potion -> {
                    ItemStack item;

                    if (!mc.player.getMainHandStack().isEmpty()) {
                        if (mc.player.getMainHandStack().getItem() != Items.SPLASH_POTION && potionmode.get() == pModes.Splash) item =  new ItemStack(Items.SPLASH_POTION);
                        else if (mc.player.getMainHandStack().getItem() != Items.LINGERING_POTION && potionmode.get() == pModes.Lingering) item =  new ItemStack(Items.LINGERING_POTION);
                        else if (mc.player.getMainHandStack().getItem() != Items.POTION && potionmode.get() == pModes.Normal) item =  new ItemStack(Items.POTION);
                        else item = mc.player.getMainHandStack().copy();
                        var changes = ComponentChanges.builder()
                                .add(DataComponentTypes.ITEM_NAME, Text.literal(nom.get()).formatted(Formatting.valueOf(nomcolor.get().toUpperCase())))
                                .add(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.empty(), Optional.empty(), pileOfStatusEffects()))
                                .build();
                        item.applyChanges(changes);
                        mc.interactionManager.clickCreativeStack(item, 36 + mc.player.getInventory().selectedSlot);
                    }
                    else switch (potionmode.get()) {
                        case Normal -> {
                            item =  new ItemStack(Items.POTION);
                            var changes = ComponentChanges.builder()
                                    .add(DataComponentTypes.ITEM_NAME, Text.literal(nom.get()).formatted(Formatting.valueOf(nomcolor.get().toUpperCase())))
                                    .add(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.empty(), Optional.empty(), pileOfStatusEffects()))
                                    .build();
                            item.applyChanges(changes);
                            mc.interactionManager.clickCreativeStack(item, 36 + mc.player.getInventory().selectedSlot);
                        }
                        case Splash -> {
                            item =  new ItemStack(Items.SPLASH_POTION);
                            var changes = ComponentChanges.builder()
                                    .add(DataComponentTypes.ITEM_NAME, Text.literal(nom.get()).formatted(Formatting.valueOf(nomcolor.get().toUpperCase())))
                                    .add(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.empty(), Optional.empty(), pileOfStatusEffects()))
                                    .build();
                            item.applyChanges(changes);
                            mc.interactionManager.clickCreativeStack(item, 36 + mc.player.getInventory().selectedSlot);
                        }
                        case Lingering -> {
                            item =  new ItemStack(Items.LINGERING_POTION);
                            var changes = ComponentChanges.builder()
                                    .add(DataComponentTypes.ITEM_NAME, Text.literal(nom.get()).formatted(Formatting.valueOf(nomcolor.get().toUpperCase())))
                                    .add(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.empty(), Optional.empty(), pileOfStatusEffects()))
                                    .build();
                            item.applyChanges(changes);
                            mc.interactionManager.clickCreativeStack(item, 36 + mc.player.getInventory().selectedSlot);
                        }
                    }
                }
                case Copy -> {
                    // Get the item stack from the main hand
                    ItemStack mainHandStack = mc.player.getMainHandStack();

                    // If the main hand is empty, use a new item stack
                    if (mainHandStack.isEmpty()) {
                        error("Put an item in your main hand.");
                        return;
                    }

                    // Get the components from the main hand item stack
                    ComponentMap mainHandComponents = mainHandStack.getComponents();
                    ItemStack offHandStack = mc.player.getOffHandStack();

                    if (copyStack.get()){
                        // Get the item stack from the offhand
                        offHandStack = mainHandStack;
                    }
                    else if (!copyStack.get()){
                        // If the offhand is empty, use a new item stack
                        if (offHandStack.isEmpty()) {
                            offHandStack = new ItemStack(Items.CARROT_ON_A_STICK);
                        }
                    }

                    // Copy the components from the main hand to the offhand
                    for (Component<?> component : mainHandComponents) {
                        DataComponentType<?> componentType = component.type();
                        Object componentValue = mainHandComponents.get(componentType);

                        if (componentType == DataComponentTypes.ATTRIBUTE_MODIFIERS) {
                            offHandStack.set((DataComponentType<AttributeModifiersComponent>)componentType, (AttributeModifiersComponent)componentValue);
                        } else if (componentType == DataComponentTypes.BANNER_PATTERNS) {
                            offHandStack.set((DataComponentType<BannerPatternsComponent>)componentType, (BannerPatternsComponent)componentValue);
                        } else if (componentType == DataComponentTypes.BASE_COLOR) {
                            offHandStack.set((DataComponentType<DyeColor>)componentType, (DyeColor)componentValue);
                        } else if (componentType == DataComponentTypes.BEES) {
                            offHandStack.set((DataComponentType<List<BeehiveBlockEntity.BeeData>>)componentType, (List<BeehiveBlockEntity.BeeData>)componentValue);
                        } else if (componentType == DataComponentTypes.BLOCK_ENTITY_DATA) {
                            offHandStack.set((DataComponentType<NbtComponent>)componentType, (NbtComponent)componentValue);
                        } else if (componentType == DataComponentTypes.BLOCK_STATE) {
                            offHandStack.set((DataComponentType<BlockStateComponent>)componentType, (BlockStateComponent)componentValue);
                        } else if (componentType == DataComponentTypes.BUCKET_ENTITY_DATA) {
                            offHandStack.set((DataComponentType<NbtComponent>)componentType, (NbtComponent)componentValue);
                        } else if (componentType == DataComponentTypes.BUNDLE_CONTENTS) {
                            offHandStack.set((DataComponentType<BundleContentsComponent>)componentType, (BundleContentsComponent)componentValue);
                        } else if (componentType == DataComponentTypes.CAN_BREAK) {
                            offHandStack.set((DataComponentType<BlockPredicatesChecker>)componentType, (BlockPredicatesChecker)componentValue);
                        } else if (componentType == DataComponentTypes.CAN_PLACE_ON) {
                            offHandStack.set((DataComponentType<BlockPredicatesChecker>)componentType, (BlockPredicatesChecker)componentValue);
                        } else if (componentType == DataComponentTypes.CHARGED_PROJECTILES) {
                            offHandStack.set((DataComponentType<ChargedProjectilesComponent>)componentType, (ChargedProjectilesComponent)componentValue);
                        } else if (componentType == DataComponentTypes.CONTAINER) {
                            offHandStack.set((DataComponentType<ContainerComponent>)componentType, (ContainerComponent)componentValue);
                        } else if (componentType == DataComponentTypes.CONTAINER_LOOT) {
                            offHandStack.set((DataComponentType<ContainerLootComponent>)componentType, (ContainerLootComponent)componentValue);
                        } else if (componentType == DataComponentTypes.CREATIVE_SLOT_LOCK) {
                            offHandStack.set((DataComponentType<Unit>)componentType, (Unit)componentValue);
                        } else if (componentType == DataComponentTypes.CUSTOM_DATA) {
                            offHandStack.set((DataComponentType<NbtComponent>)componentType, (NbtComponent)componentValue);
                        } else if (componentType == DataComponentTypes.CUSTOM_MODEL_DATA) {
                            offHandStack.set((DataComponentType<CustomModelDataComponent>)componentType, (CustomModelDataComponent)componentValue);
                        } else if (componentType == DataComponentTypes.CUSTOM_NAME) {
                            offHandStack.set((DataComponentType<Text>)componentType, (Text)componentValue);
                        } else if (componentType == DataComponentTypes.DAMAGE) {
                            offHandStack.set((DataComponentType<Integer>)componentType, (Integer)componentValue);
                        } else if (componentType == DataComponentTypes.DEBUG_STICK_STATE) {
                            offHandStack.set((DataComponentType<DebugStickStateComponent>)componentType, (DebugStickStateComponent)componentValue);
                        } else if (componentType == DataComponentTypes.DYED_COLOR) {
                            offHandStack.set((DataComponentType<DyedColorComponent>)componentType, (DyedColorComponent)componentValue);
                        } else if (componentType == DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE) {
                            offHandStack.set((DataComponentType<Boolean>)componentType, (Boolean)componentValue);
                        } else if (componentType == DataComponentTypes.ENCHANTMENTS) {
                            offHandStack.set((DataComponentType<ItemEnchantmentsComponent>)componentType, (ItemEnchantmentsComponent)componentValue);
                        } else if (componentType == DataComponentTypes.ENTITY_DATA) {
                            offHandStack.set((DataComponentType<NbtComponent>)componentType, (NbtComponent)componentValue);
                        } else if (componentType == DataComponentTypes.FIRE_RESISTANT) {
                            offHandStack.set((DataComponentType<Unit>)componentType, (Unit)componentValue);
                        } else if (componentType == DataComponentTypes.FIREWORK_EXPLOSION) {
                            offHandStack.set((DataComponentType<FireworkExplosionComponent>)componentType, (FireworkExplosionComponent)componentValue);
                        } else if (componentType == DataComponentTypes.FIREWORKS) {
                            offHandStack.set((DataComponentType<FireworksComponent>)componentType, (FireworksComponent)componentValue);
                        } else if (componentType == DataComponentTypes.FOOD) {
                            offHandStack.set((DataComponentType<FoodComponent>)componentType, (FoodComponent)componentValue);
                        } else if (componentType == DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP) {
                            offHandStack.set((DataComponentType<Unit>)componentType, (Unit)componentValue);
                        } else if (componentType == DataComponentTypes.HIDE_TOOLTIP) {
                            offHandStack.set((DataComponentType<Unit>)componentType, (Unit)componentValue);
                        } else if (componentType == DataComponentTypes.INSTRUMENT) {
                            offHandStack.set((DataComponentType<RegistryEntry<Instrument>>)componentType, (RegistryEntry<Instrument>)componentValue);
                        } else if (componentType == DataComponentTypes.INTANGIBLE_PROJECTILE) {
                            offHandStack.set((DataComponentType<Unit>)componentType, (Unit)componentValue);
                        } else if (componentType == DataComponentTypes.ITEM_NAME) {
                            offHandStack.set((DataComponentType<Text>)componentType, (Text)componentValue);
                        } else if (componentType == DataComponentTypes.LOCK) {
                            offHandStack.set((DataComponentType<ContainerLock>)componentType, (ContainerLock)componentValue);
                        } else if (componentType == DataComponentTypes.LODESTONE_TRACKER) {
                            offHandStack.set((DataComponentType<LodestoneTrackerComponent>)componentType, (LodestoneTrackerComponent)componentValue);
                        } else if (componentType == DataComponentTypes.LORE) {
                            offHandStack.set((DataComponentType<LoreComponent>)componentType, (LoreComponent)componentValue);
                        } else if (componentType == DataComponentTypes.MAP_COLOR) {
                            offHandStack.set((DataComponentType<MapColorComponent>)componentType, (MapColorComponent)componentValue);
                        } else if (componentType == DataComponentTypes.MAP_DECORATIONS) {
                            offHandStack.set((DataComponentType<MapDecorationsComponent>)componentType, (MapDecorationsComponent)componentValue);
                        } else if (componentType == DataComponentTypes.MAP_ID) {
                            offHandStack.set((DataComponentType<MapIdComponent>)componentType, (MapIdComponent)componentValue);
                        } else if (componentType == DataComponentTypes.MAP_POST_PROCESSING) {
                            offHandStack.set((DataComponentType<MapPostProcessingComponent>)componentType, (MapPostProcessingComponent)componentValue);
                        } else if (componentType == DataComponentTypes.MAX_DAMAGE) {
                            offHandStack.set((DataComponentType<Integer>)componentType, (Integer)componentValue);
                        } else if (componentType == DataComponentTypes.MAX_STACK_SIZE) {
                            offHandStack.set((DataComponentType<Integer>)componentType, (Integer)componentValue);
                        } else if (componentType == DataComponentTypes.NOTE_BLOCK_SOUND) {
                            offHandStack.set((DataComponentType<Identifier>)componentType, (Identifier)componentValue);
                        } else if (componentType == DataComponentTypes.OMINOUS_BOTTLE_AMPLIFIER) {
                            offHandStack.set((DataComponentType<Integer>)componentType, (Integer)componentValue);
                        } else if (componentType == DataComponentTypes.POT_DECORATIONS) {
                            offHandStack.set((DataComponentType<Sherds>)componentType, (Sherds)componentValue);
                        } else if (componentType == DataComponentTypes.POTION_CONTENTS) {
                            offHandStack.set((DataComponentType<PotionContentsComponent>)componentType, (PotionContentsComponent)componentValue);
                        } else if (componentType == DataComponentTypes.PROFILE) {
                            offHandStack.set((DataComponentType<ProfileComponent>)componentType, (ProfileComponent)componentValue);
                        } else if (componentType == DataComponentTypes.RARITY) {
                            offHandStack.set((DataComponentType<Rarity>)componentType, (Rarity)componentValue);
                        } else if (componentType == DataComponentTypes.RECIPES) {
                            offHandStack.set((DataComponentType<List<Identifier>>)componentType, (List<Identifier>)componentValue);
                        } else if (componentType == DataComponentTypes.REPAIR_COST) {
                            offHandStack.set((DataComponentType<Integer>)componentType, (Integer)componentValue);
                        } else if (componentType == DataComponentTypes.STORED_ENCHANTMENTS) {
                            offHandStack.set((DataComponentType<ItemEnchantmentsComponent>)componentType, (ItemEnchantmentsComponent)componentValue);
                        } else if (componentType == DataComponentTypes.SUSPICIOUS_STEW_EFFECTS) {
                            offHandStack.set((DataComponentType<SuspiciousStewEffectsComponent>)componentType, (SuspiciousStewEffectsComponent)componentValue);
                        } else if (componentType == DataComponentTypes.TOOL) {
                            offHandStack.set((DataComponentType<ToolComponent>)componentType, (ToolComponent)componentValue);
                        } else if (componentType == DataComponentTypes.TRIM) {
                            offHandStack.set((DataComponentType<ArmorTrim>)componentType, (ArmorTrim)componentValue);
                        } else if (componentType == DataComponentTypes.UNBREAKABLE) {
                            offHandStack.set((DataComponentType<UnbreakableComponent>)componentType, (UnbreakableComponent)componentValue);
                        } else if (componentType == DataComponentTypes.WRITABLE_BOOK_CONTENT) {
                            offHandStack.set((DataComponentType<WritableBookContentComponent>)componentType, (WritableBookContentComponent)componentValue);
                        } else if (componentType == DataComponentTypes.WRITTEN_BOOK_CONTENT) {
                            offHandStack.set((DataComponentType<WrittenBookContentComponent>)componentType, (WrittenBookContentComponent)componentValue);
                        }

                        // Apply the changes to the offhand item stack
                        mc.interactionManager.clickCreativeStack(offHandStack, 45); // 45 is the offhand slot
                    }
                }
            }
            ChatUtils.sendMsg(Text.of("Modified item created."));
            toggle();
        } else {
            error("You need to be in creative mode.");
            toggle();
        }
    }
    private List<StatusEffectInstance> pileOfStatusEffects() {     //hopefully someone can find a better way of doing this
        List<StatusEffect> Effects = effects.get();
        List<StatusEffectInstance> effectInstances = new ArrayList<>();
        for (StatusEffect effect : Effects) {
            if (effect.getTranslationKey().contains("absorption")) {
                effectInstances.add(new StatusEffectInstance(StatusEffects.ABSORPTION, duration.get(), amplifier.get()));
            } else if (effect.getTranslationKey().contains("bad_omen")) {
                effectInstances.add(new StatusEffectInstance(StatusEffects.BAD_OMEN, duration.get(), amplifier.get()));
            } else if (effect.getTranslationKey().contains("blindness")) {
                effectInstances.add(new StatusEffectInstance(StatusEffects.BLINDNESS, duration.get(), amplifier.get()));
            } else if (effect.getTranslationKey().contains("conduit_power")) {
                effectInstances.add(new StatusEffectInstance(StatusEffects.CONDUIT_POWER, duration.get(), amplifier.get()));
            } else if (effect.getTranslationKey().contains("darkness")) {
                effectInstances.add(new StatusEffectInstance(StatusEffects.DARKNESS, duration.get(), amplifier.get()));
            } else if (effect.getTranslationKey().contains("dolphins_grace")) {
                effectInstances.add(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, duration.get(), amplifier.get()));
            } else if (effect.getTranslationKey().contains("fire_resistance")) {
                effectInstances.add(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, duration.get(), amplifier.get()));
            } else if (effect.getTranslationKey().contains("glowing")) {
                effectInstances.add(new StatusEffectInstance(StatusEffects.GLOWING, duration.get(), amplifier.get()));
            } else if (effect.getTranslationKey().contains("haste")) {
                effectInstances.add(new StatusEffectInstance(StatusEffects.HASTE, duration.get(), amplifier.get()));
            } else if (effect.getTranslationKey().contains("health_boost")) {
                effectInstances.add(new StatusEffectInstance(StatusEffects.HEALTH_BOOST, duration.get(), amplifier.get()));
            } else if (effect.getTranslationKey().contains("hero_of_the_village")) {
                effectInstances.add(new StatusEffectInstance(StatusEffects.HERO_OF_THE_VILLAGE, duration.get(), amplifier.get()));
            } else if (effect.getTranslationKey().contains("hunger")) {
                effectInstances.add(new StatusEffectInstance(StatusEffects.HUNGER, duration.get(), amplifier.get()));
            } else if (effect.getTranslationKey().contains("infested")) {
                effectInstances.add(new StatusEffectInstance(StatusEffects.INFESTED, duration.get(), amplifier.get()));
            } else if (effect.getTranslationKey().contains("instant_damage")) {
                effectInstances.add(new StatusEffectInstance(StatusEffects.INSTANT_DAMAGE, duration.get(), amplifier.get()));
            } else if (effect.getTranslationKey().contains("instant_health")) {
                effectInstances.add(new StatusEffectInstance(StatusEffects.INSTANT_HEALTH, duration.get(), amplifier.get()));
            } else if (effect.getTranslationKey().contains("invisibility")) {
                effectInstances.add(new StatusEffectInstance(StatusEffects.INVISIBILITY, duration.get(), amplifier.get()));
            } else if (effect.getTranslationKey().contains("jump_boost")) {
                effectInstances.add(new StatusEffectInstance(StatusEffects.JUMP_BOOST, duration.get(), amplifier.get()));
            } else if (effect.getTranslationKey().contains("levitation")) {
                effectInstances.add(new StatusEffectInstance(StatusEffects.LEVITATION, duration.get(), amplifier.get()));
            } else if (effect.getTranslationKey().contains("luck")) {
                effectInstances.add(new StatusEffectInstance(StatusEffects.LUCK, duration.get(), amplifier.get()));
            } else if (effect.getTranslationKey().contains("mining_fatigue")) {
                effectInstances.add(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, duration.get(), amplifier.get()));
            } else if (effect.getTranslationKey().contains("nausea")) {
                effectInstances.add(new StatusEffectInstance(StatusEffects.NAUSEA, duration.get(), amplifier.get()));
            } else if (effect.getTranslationKey().contains("night_vision")) {
                effectInstances.add(new StatusEffectInstance(StatusEffects.NIGHT_VISION, duration.get(), amplifier.get()));
            } else if (effect.getTranslationKey().contains("oozing")) {
                effectInstances.add(new StatusEffectInstance(StatusEffects.OOZING, duration.get(), amplifier.get()));
            } else if (effect.getTranslationKey().contains("poison")) {
                effectInstances.add(new StatusEffectInstance(StatusEffects.POISON, duration.get(), amplifier.get()));
            } else if (effect.getTranslationKey().contains("raid_omen")) {
                effectInstances.add(new StatusEffectInstance(StatusEffects.RAID_OMEN, duration.get(), amplifier.get()));
            } else if (effect.getTranslationKey().contains("regeneration")) {
                effectInstances.add(new StatusEffectInstance(StatusEffects.REGENERATION, duration.get(), amplifier.get()));
            } else if (effect.getTranslationKey().contains("resistance")) {
                effectInstances.add(new StatusEffectInstance(StatusEffects.RESISTANCE, duration.get(), amplifier.get()));
            } else if (effect.getTranslationKey().contains("saturation")) {
                effectInstances.add(new StatusEffectInstance(StatusEffects.SATURATION, duration.get(), amplifier.get()));
            } else if (effect.getTranslationKey().contains("slow_falling")) {
                effectInstances.add(new StatusEffectInstance(StatusEffects.SLOW_FALLING, duration.get(), amplifier.get()));
            } else if (effect.getTranslationKey().contains("slowness")) {
                effectInstances.add(new StatusEffectInstance(StatusEffects.SLOWNESS, duration.get(), amplifier.get()));
            } else if (effect.getTranslationKey().contains("speed")) {
                effectInstances.add(new StatusEffectInstance(StatusEffects.SPEED, duration.get(), amplifier.get()));
            } else if (effect.getTranslationKey().contains("strength")) {
                effectInstances.add(new StatusEffectInstance(StatusEffects.STRENGTH, duration.get(), amplifier.get()));
            } else if (effect.getTranslationKey().contains("trial_omen")) {
                effectInstances.add(new StatusEffectInstance(StatusEffects.TRIAL_OMEN, duration.get(), amplifier.get()));
            } else if (effect.getTranslationKey().contains("unluck")) {
                effectInstances.add(new StatusEffectInstance(StatusEffects.UNLUCK, duration.get(), amplifier.get()));
            } else if (effect.getTranslationKey().contains("water_breathing")) {
                effectInstances.add(new StatusEffectInstance(StatusEffects.WATER_BREATHING, duration.get(), amplifier.get()));
            } else if (effect.getTranslationKey().contains("weakness")) {
                effectInstances.add(new StatusEffectInstance(StatusEffects.WEAKNESS, duration.get(), amplifier.get()));
            } else if (effect.getTranslationKey().contains("weaving")) {
                effectInstances.add(new StatusEffectInstance(StatusEffects.WEAVING, duration.get(), amplifier.get()));
            } else if (effect.getTranslationKey().contains("wind_charged")) {
                effectInstances.add(new StatusEffectInstance(StatusEffects.WIND_CHARGED, duration.get(), amplifier.get()));
            } else if (effect.getTranslationKey().contains("wither")) {
                effectInstances.add(new StatusEffectInstance(StatusEffects.WITHER, duration.get(), amplifier.get()));
            }
        }
        return effectInstances;
    }
    private NbtComponent createEntityData() {
        String entityName = entity.get().trim().replace(" ", "_");
        NbtCompound entityTag = new NbtCompound();
        entityTag.putString("id", "minecraft:" + entityName);
        entityTag.putInt("Health", health.get());
        entityTag.putInt("AbsorptionAmount", absorption.get());
        entityTag.putInt("Age", age.get());
        entityTag.putInt("ExplosionPower", exppower.get());
        entityTag.putInt("ExplosionRadius", exppower.get());
        if (invincible.get())entityTag.putBoolean("Invulnerable", invincible.get());
        if (silence.get())entityTag.putBoolean("Silent", silence.get());
        if (glow.get())entityTag.putBoolean("Glowing", glow.get());
        if (persist.get())entityTag.putBoolean("PersistenceRequired", persist.get());
        if (nograv.get())entityTag.putBoolean("NoGravity", nograv.get());
        if(noAI.get())entityTag.putBoolean("NoAI", noAI.get());
        if(falsefire.get())entityTag.putBoolean("HasVisualFire", falsefire.get());
        if(powah.get())entityTag.putBoolean("powered", powah.get());
        if(ignite.get())entityTag.putBoolean("ignited", ignite.get());
        entityTag.putInt("Fuse", fuse.get());
        entityTag.putInt("Size", size.get());
        if(customname.get())entityTag.putBoolean("CustomNameVisible", customname.get());
        entityTag.putString("CustomName", "{\"text\":\"" + nom.get() + "\",\"color\":\"" + nomcolor.get() + "\"}");
        entityTag.putInt("Radius", cloudradius.get());
        entityTag.putInt("Duration", cloudduration.get());
        entityTag.putString("Particle", particle.get());
        entityTag.putString("Potion", ceffect.get());
        return NbtComponent.of(entityTag);
    }
    public enum Modes {
        Entity, Potion, Copy
        //Entity, Item, Potion, Copy       //Item mode doesn't work
    }
    public enum pModes {
        Normal, Splash, Lingering
    }
}