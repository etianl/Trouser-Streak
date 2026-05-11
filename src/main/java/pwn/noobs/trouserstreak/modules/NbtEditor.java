package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import pwn.noobs.trouserstreak.Trouser;

import java.util.*;

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
            .visible(() -> mode.get() == Modes.Entity || mode.get() == Modes.Item || mode.get() == Modes.Potion)
            .build());
    private final Setting<BoomPlus.ColorModes> nomcolor = sgGeneral.add(new EnumSetting.Builder<BoomPlus.ColorModes>()
            .name("Custom Name Color")
            .description("Color the Name")
            .defaultValue(BoomPlus.ColorModes.red)
            .build());
    public enum ColorModes { aqua, black, blue, dark_aqua, dark_blue, dark_gray, dark_green, dark_purple, dark_red, gold, gray, green, italic, light_purple, red, white, yellow }
    private final Setting<String> entity = sgOptions.add(new StringSetting.Builder()
            .name("Entity to Spawn")
            .description("What is created. Ex: fireball, villager, minecart, lightning_bolt, magma cube, area effect cloud")
            .defaultValue("wither")
            .visible(() -> mode.get() == Modes.Entity)
            .build());
    private final Setting<Item> itemlist = sgOptions.add(new ItemSetting.Builder()
            .name("Item to create.")
            .description("Pick one. If you aren't already holding an item this is what you get.")
            .defaultValue(Items.COD)
            .visible(() -> (mode.get() == Modes.Item))
            .build());
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
    private final Setting<List<MobEffect>> effects = sgOptions.add(new StatusEffectListSetting.Builder()
            .name("Effects")
            .description("List of potion effects.")
            .defaultValue(MobEffects.INSTANT_HEALTH.value())
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
    private final Setting<Set<ResourceKey<Enchantment>>> enchants = sgOptions.add(new EnchantmentListSetting.Builder()
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

    public NbtEditor() {
        super(Trouser.operator, "NbtEditor", " CREATIVE MODE REQUIRED. Creates custom entities (spawn eggs) and enchanted items based on your specified options.");
    }
    @Override
    public void onActivate() {
        if (mc.player != null  && mc.gameMode != null && mc.level != null && mc.player.getAbilities().instabuild) {
            switch (mode.get()) {
                case Entity -> {
                    ItemStack item = new ItemStack(Items.BEE_SPAWN_EGG);
                    var changes = DataComponentPatch.builder()
                            .set(DataComponents.CUSTOM_NAME, Component.literal(nom.get()).withStyle(ChatFormatting.valueOf(nomcolor.get().toString().toUpperCase())))
                            .set(DataComponents.ITEM_NAME, Component.literal(nom.get()).withStyle(ChatFormatting.valueOf(nomcolor.get().toString().toUpperCase())))
                            .set(DataComponents.ENTITY_DATA, createEntityData())
                            .build();
                    item.applyComponentsAndValidate(changes);
                    createItem(item);
                }
                case Item -> {
                    ItemStack item;

                    if (!mc.player.getMainHandItem().isEmpty()) {
                        item = mc.player.getMainHandItem().copy();
                    } else {
                        item = new ItemStack(itemlist.get());
                    }

                    Registry<Enchantment> enchantmentRegistry = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

                    for (ResourceKey<Enchantment> enchantKey : enchants.get()) {
                        Holder<Enchantment> enchantEntry = enchantmentRegistry.getOrThrow(enchantKey);
                        item.enchant(enchantEntry, level.get());
                    }

                    item.set(DataComponents.CUSTOM_NAME, Component.literal(nom.get()).withStyle(ChatFormatting.valueOf(nomcolor.get().toString().toUpperCase())));
                    createItem(item);
                }
                case Potion -> {
                    ItemStack item;

                    if (!mc.player.getMainHandItem().isEmpty()) {
                        if (mc.player.getMainHandItem().getItem() != Items.SPLASH_POTION && potionmode.get() == pModes.Splash) item =  new ItemStack(Items.SPLASH_POTION);
                        else if (mc.player.getMainHandItem().getItem() != Items.LINGERING_POTION && potionmode.get() == pModes.Lingering) item =  new ItemStack(Items.LINGERING_POTION);
                        else if (mc.player.getMainHandItem().getItem() != Items.POTION && potionmode.get() == pModes.Normal) item =  new ItemStack(Items.POTION);
                        else item = mc.player.getMainHandItem().copy();
                        var changes = DataComponentPatch.builder()
                                .set(DataComponents.CUSTOM_NAME, Component.literal(nom.get()).withStyle(ChatFormatting.valueOf(nomcolor.get().toString().toUpperCase())))
                                .set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), Optional.empty(), pileOfStatusEffects(), Optional.ofNullable(nom.get())))
                                .build();
                        item.applyComponentsAndValidate(changes);
                        createItem(item);
                    }
                    else switch (potionmode.get()) {
                        case Normal -> {
                            item =  new ItemStack(Items.POTION);
                            var changes = DataComponentPatch.builder()
                                    .set(DataComponents.CUSTOM_NAME, Component.literal(nom.get()).withStyle(ChatFormatting.valueOf(nomcolor.get().toString().toUpperCase())))
                                    .set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), Optional.empty(), pileOfStatusEffects(), Optional.ofNullable(nom.get())))
                                    .build();
                            item.applyComponentsAndValidate(changes);
                            createItem(item);
                        }
                        case Splash -> {
                            item =  new ItemStack(Items.SPLASH_POTION);
                            var changes = DataComponentPatch.builder()
                                    .set(DataComponents.CUSTOM_NAME, Component.literal(nom.get()).withStyle(ChatFormatting.valueOf(nomcolor.get().toString().toUpperCase())))
                                    .set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), Optional.empty(), pileOfStatusEffects(), Optional.ofNullable(nom.get())))
                                    .build();
                            item.applyComponentsAndValidate(changes);
                            createItem(item);
                        }
                        case Lingering -> {
                            item =  new ItemStack(Items.LINGERING_POTION);
                            var changes = DataComponentPatch.builder()
                                    .set(DataComponents.CUSTOM_NAME, Component.literal(nom.get()).withStyle(ChatFormatting.valueOf(nomcolor.get().toString().toUpperCase())))
                                    .set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), Optional.empty(), pileOfStatusEffects(), Optional.ofNullable(nom.get())))
                                    .build();
                            item.applyComponentsAndValidate(changes);
                            createItem(item);
                        }
                    }
                }
                case Copy -> {
                    ItemStack mainHandStack = mc.player.getMainHandItem();

                    if (mainHandStack.isEmpty()) {
                        error("Put an item in your main hand.");
                        return;
                    }

                    DataComponentMap mainHandComponents = mainHandStack.getComponents();
                    ItemStack offHandStack = mc.player.getOffhandItem();

                    if (copyStack.get()){
                        offHandStack = mainHandStack;
                    }
                    else if (!copyStack.get()){
                        if (offHandStack.isEmpty()) {
                            offHandStack = new ItemStack(Items.CARROT_ON_A_STICK);
                        }
                    }
                    offHandStack.applyComponents(mainHandComponents);
                    mc.gameMode.handleCreativeModeItemAdd(offHandStack, 45); // 45 is the offhand slot
                    //clickSlot twice to make the item actually appear clientside
                    mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, 45, 0, ContainerInput.PICKUP, mc.player);
                    mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, 45, 0, ContainerInput.PICKUP, mc.player);
                }
            }
            ChatUtils.sendMsg(Component.nullToEmpty("Modified item created."));
            toggle();
        } else {
            error("You need to be in creative mode.");
            toggle();
        }
    }
    private void createItem(ItemStack item){
        mc.gameMode.handleCreativeModeItemAdd(item, 36 + mc.player.getInventory().getSelectedSlot());
        //clickSlot twice to make the item actually appear clientside
        mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, 36 + mc.player.getInventory().getSelectedSlot(), 0, ContainerInput.PICKUP, mc.player);
        mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, 36 + mc.player.getInventory().getSelectedSlot(), 0, ContainerInput.PICKUP, mc.player);
    }
    private List<MobEffectInstance> pileOfStatusEffects() {
        List<MobEffectInstance> effectInstances = new ArrayList<>();

        for (MobEffect effect : effects.get()) {
            Holder<MobEffect> registryEntry = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
            effectInstances.add(new MobEffectInstance(registryEntry, duration.get(), amplifier.get()));
        }

        return effectInstances;
    }

    private TypedEntityData<EntityType<?>> createEntityData() {
        String entityName = entity.get().trim().replace(" ", "_");

        CompoundTag entityTag = new CompoundTag();
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
        CompoundTag customName = new CompoundTag();
        customName.putString("text", nom.get());
        customName.putString("color", nomcolor.get().name());
        String serverVersion;
        if (mc.hasSingleplayerServer()) {
            serverVersion = mc.getSingleplayerServer().getServerVersion();
        } else {
            serverVersion = mc.getCurrentServer().version.tryCollapseToString();
        }
        if (serverVersion == null) {
            entityTag.put("CustomName", customName);
        } else {
            if (isVersionLessThan(serverVersion, 1, 21, 5)) {
                entityTag.putString("CustomName", "{\"text\":\"" + nom.get() + "\",\"color\":\"" + nomcolor.get().name() + "\"}");
            } else {
                entityTag.put("CustomName", customName);
            }
        }
        entityTag.putInt("Radius", cloudradius.get());
        entityTag.putInt("Duration", cloudduration.get());
        entityTag.putString("Particle", particle.get());
        entityTag.putString("Potion", ceffect.get());
        Identifier entityId = Identifier.tryParse("minecraft:" + entityName);
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getValue(entityId);
        if (entityType == null) {
            entityType = EntityType.PIG;
        }

        return TypedEntityData.of(entityType, entityTag);
    }
    private boolean isVersionLessThan(String serverVersion, int major, int minor, int patch) {
        if (serverVersion == null) return false;

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(serverVersion);

        if (matcher.find()) {
            try {
                int serverMajor = Integer.parseInt(matcher.group(1));
                int serverMinor = Integer.parseInt(matcher.group(2));
                int serverPatch = Integer.parseInt(matcher.group(3));

                if (serverMajor < major) return true;
                if (serverMajor > major) return false;

                if (serverMinor < minor) return true;
                if (serverMinor > minor) return false;

                return serverPatch < patch;

            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    public enum Modes {
        Entity, Item, Potion, Copy
    }
    public enum pModes {
        Normal, Splash, Lingering
    }
}