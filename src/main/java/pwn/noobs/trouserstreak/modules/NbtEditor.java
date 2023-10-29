package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.*;
import net.minecraft.text.Text;
import pwn.noobs.trouserstreak.Trouser;

import java.util.List;

public class NbtEditor extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgOptions = settings.createGroup("Nbt Options");

    private final Setting<Modes> mode = sgGeneral.add(new EnumSetting.Builder<Modes>()
            .name("mode")
            .description("the mode")
            .defaultValue(Modes.Entity)
            .build());
    private final Setting<String> nom = sgGeneral.add(new StringSetting.Builder()
            .name("Custom Name")
            .description("Name the Thing")
            .defaultValue("MOUNTAINSOFLAVAINC")
            .build());
    private final Setting<String> nomcolor = sgGeneral.add(new StringSetting.Builder()
            .name("Custom Name Color")
            .description("Color the Name")
            .defaultValue("red")
            .build());
    private final Setting<String> entity = sgOptions.add(new StringSetting.Builder()
            .name("Entity to Spawn")
            .description("What is created. Ex: fireball, villager, minecart, lightning, magma_cube")
            .defaultValue("wither")
            .visible(() -> mode.get() == Modes.Entity)
            .build());
    private final Setting<List<Item>> itemlist = sgOptions.add(new ItemListSetting.Builder()
            .name("Item to create. (Pick only 1)")
            .description("The first one in the list will be used.")
            .defaultValue()
            .visible(() -> (mode.get() == Modes.Item))
            .build()
    );

    private final Setting<Integer> health = sgOptions.add(new IntSetting.Builder()
            .name("Health Points")
            .description("How much health.")
            .defaultValue(100)
            .min(0)
            .sliderRange(0, 100)
            .visible(() -> mode.get() == Modes.Entity)
            .build());
    private final Setting<Integer> absorption = sgOptions.add(new IntSetting.Builder()
            .name("Absorption Points")
            .description("How much absorption.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 100)
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
    private final Setting<Integer> sharpness = sgOptions.add(new IntSetting.Builder()
            .name("sharpness Level")
            .description("Sharpness Enchantment.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());
    private final Setting<Integer> smite = sgOptions.add(new IntSetting.Builder()
            .name("smite Level")
            .description("smite Enchantment.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());
    private final Setting<Integer> bane_of_arthropods = sgOptions.add(new IntSetting.Builder()
            .name("bane_of_arthropods Level")
            .description("bane_of_arthropods Enchantment.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());
    private final Setting<Integer> knockback = sgOptions.add(new IntSetting.Builder()
            .name("knockback Level")
            .description("knockback Enchantment.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());
    private final Setting<Integer> fire_aspect = sgOptions.add(new IntSetting.Builder()
            .name("fire_aspect Level")
            .description("fire_aspect Enchantment.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());
    private final Setting<Integer> looting = sgOptions.add(new IntSetting.Builder()
            .name("looting Level")
            .description("looting Enchantment.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());
    private final Setting<Integer> sweeping_edge = sgOptions.add(new IntSetting.Builder()
            .name("sweeping_edge Level")
            .description("sweeping_edge Enchantment.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());
    private final Setting<Integer> efficiency = sgOptions.add(new IntSetting.Builder()
            .name("efficiency Level")
            .description("efficiency Enchantment.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());
    private final Setting<Integer> mending = sgOptions.add(new IntSetting.Builder()
            .name("mending Level")
            .description("mending Enchantment.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());
    private final Setting<Integer> silk_touch = sgOptions.add(new IntSetting.Builder()
            .name("silk_touch Level")
            .description("silk_touch Enchantment.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());
    private final Setting<Integer> unbreaking = sgOptions.add(new IntSetting.Builder()
            .name("unbreaking Level")
            .description("unbreaking Enchantment.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());
    private final Setting<Integer> fortune = sgOptions.add(new IntSetting.Builder()
            .name("fortune Level")
            .description("fortune Enchantment.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());
    private final Setting<Integer> power = sgOptions.add(new IntSetting.Builder()
            .name("power Level")
            .description("power Enchantment.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());
    private final Setting<Integer> punch = sgOptions.add(new IntSetting.Builder()
            .name("punch Level")
            .description("punch Enchantment.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());
    private final Setting<Integer> flame = sgOptions.add(new IntSetting.Builder()
            .name("flame Level")
            .description("flame Enchantment.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());
    private final Setting<Integer> infinity = sgOptions.add(new IntSetting.Builder()
            .name("infinity Level")
            .description("infinity Enchantment.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());
    private final Setting<Integer> luck_of_the_sea = sgOptions.add(new IntSetting.Builder()
            .name("luck_of_the_sea Level")
            .description("luck_of_the_sea Enchantment.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());
    private final Setting<Integer> lure = sgOptions.add(new IntSetting.Builder()
            .name("lure Level")
            .description("lure Enchantment.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());
    private final Setting<Integer> loyalty = sgOptions.add(new IntSetting.Builder()
            .name("loyalty Level")
            .description("loyalty Enchantment.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());
    private final Setting<Integer> impaling = sgOptions.add(new IntSetting.Builder()
            .name("impaling Level")
            .description("impaling Enchantment.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());
    private final Setting<Integer> riptide = sgOptions.add(new IntSetting.Builder()
            .name("riptide Level")
            .description("riptide Enchantment.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());
    private final Setting<Integer> channeling = sgOptions.add(new IntSetting.Builder()
            .name("channeling Level")
            .description("channeling Enchantment.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());
    private final Setting<Integer> multishot = sgOptions.add(new IntSetting.Builder()
            .name("multishot Level")
            .description("multishot Enchantment.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());
    private final Setting<Integer> piercing = sgOptions.add(new IntSetting.Builder()
            .name("piercing Level")
            .description("piercing Enchantment.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());
    private final Setting<Integer> quick_charge = sgOptions.add(new IntSetting.Builder()
            .name("quick_charge Level")
            .description("quick_charge Enchantment.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());
    private final Setting<Integer> respiration = sgOptions.add(new IntSetting.Builder()
            .name("respiration Level")
            .description("respiration Enchantment.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());
    private final Setting<Integer> protection = sgOptions.add(new IntSetting.Builder()
            .name("protection Level")
            .description("protection Enchantment.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());
    private final Setting<Integer> fire_protection = sgOptions.add(new IntSetting.Builder()
            .name("fire_protection Level")
            .description("fire_protection Enchantment.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());
    private final Setting<Integer> feather_falling = sgOptions.add(new IntSetting.Builder()
            .name("feather_falling Level")
            .description("feather_falling Enchantment.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());
    private final Setting<Integer> blast_protection = sgOptions.add(new IntSetting.Builder()
            .name("blast_protection Level")
            .description("blast_protection Enchantment.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());
    private final Setting<Integer> projectile_protection = sgOptions.add(new IntSetting.Builder()
            .name("projectile_protection Level")
            .description("projectile_protection Enchantment.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());
    private final Setting<Integer> thorns = sgOptions.add(new IntSetting.Builder()
            .name("thorns Level")
            .description("thorns Enchantment.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());
    private final Setting<Integer> aqua_affinity = sgOptions.add(new IntSetting.Builder()
            .name("aqua_affinity Level")
            .description("aqua_affinity Enchantment.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());
    private final Setting<Integer> depth_strider = sgOptions.add(new IntSetting.Builder()
            .name("depth_strider Level")
            .description("depth_strider Enchantment.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());
    private final Setting<Integer> soul_speed = sgOptions.add(new IntSetting.Builder()
            .name("soul_speed Level")
            .description("soul_speed Enchantment.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());
    private final Setting<Integer> swift_sneak = sgOptions.add(new IntSetting.Builder()
            .name("swift_sneak Level")
            .description("swift_sneak Enchantment.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());
    private final Setting<Integer> frost_walker = sgOptions.add(new IntSetting.Builder()
            .name("frost_walker Level")
            .description("frost_walker Enchantment.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());
    private final Setting<Integer> binding_curse = sgOptions.add(new IntSetting.Builder()
            .name("binding_curse Level")
            .description("binding_curse Enchantment.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());
    private final Setting<Integer> vanishing_curse = sgOptions.add(new IntSetting.Builder()
            .name("vanishing_curse Level")
            .description("vanishing_curse Enchantment.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 32767)
            .visible(() -> mode.get() == Modes.Item)
            .build());

    public NbtEditor() {
        super(Trouser.Main, "NbtEditor", " CREATIVE MODE REQUIRED. Creates custom entities (spawn eggs) and enchanted items based on your specified options.");
    }

    @Override
    public void onActivate() {
        if (mc.player.getAbilities().creativeMode) {
            switch (mode.get()) {
                case Entity -> {
                    String entityName = entity.get().trim().replace(" ", "_");
                    NbtCompound tag = new NbtCompound();
                    ItemStack item = new ItemStack(Items.BEE_SPAWN_EGG);
                    NbtCompound display = new NbtCompound();
                    display.putString("Name", "{\"text\":\"" + nom.get() + "\",\"color\":\"" + nomcolor.get() + "\"}");
                    tag.put("display", display);
                    NbtCompound entityTag = new NbtCompound();
                    entityTag.putString("id", "minecraft:" + entityName);
                    entityTag.putInt("Health", health.get());
                    entityTag.putInt("AbsorptionAmount", absorption.get());
                    entityTag.putInt("Age", age.get());
                    entityTag.putInt("ExplosionPower", exppower.get());
                    entityTag.putBoolean("Invulnerable", invincible.get());
                    entityTag.putBoolean("Silent", silence.get());
                    entityTag.putBoolean("Glowing", glow.get());
                    entityTag.putBoolean("PersistenceRequired", persist.get());
                    entityTag.putBoolean("NoGravity", nograv.get());
                    entityTag.putBoolean("NoAI", noAI.get());
                    entityTag.putBoolean("HasVisualFire", falsefire.get());
                    entityTag.putBoolean("powered", powah.get());
                    entityTag.putBoolean("ignited", ignite.get());
                    entityTag.putInt("ExplosionRadius", exppower.get());
                    entityTag.putInt("Fuse", fuse.get());
                    entityTag.putInt("Size", size.get());
                    entityTag.putBoolean("CustomNameVisible", true);
                    entityTag.putString("CustomName", "{\"text\":\"" + nom.get() + "\",\"color\":\"" + nomcolor.get() + "\"}");
                    tag.put("EntityTag", entityTag);
                    item.setNbt(tag);
                    mc.interactionManager.clickCreativeStack(item, 36 + mc.player.getInventory().selectedSlot);
                }
                case Item -> {
                    NbtCompound tag = new NbtCompound();
                    ItemStack item = new ItemStack(Items.CARROT_ON_A_STICK);

                    if (!itemlist.get().isEmpty()) {
                        item = new ItemStack(itemlist.get().get(0).asItem());
                    }
                    NbtList enchantments = new NbtList(); // Add enchantments to items
                    if (sharpness.get()>0){
                    enchantments.add(new NbtCompound());
                    enchantments.getCompound(0).putString("id", "minecraft:sharpness");
                    enchantments.getCompound(0).putInt("lvl", sharpness.get());
                    }
                    if (smite.get()>0){
                    enchantments.add(new NbtCompound()); // Add another enchantment
                    enchantments.getCompound(1).putString("id", "minecraft:smite");
                    enchantments.getCompound(1).putInt("lvl", smite.get());
                    }
                    if (bane_of_arthropods.get() > 0) {
                    enchantments.add(new NbtCompound());
                    enchantments.getCompound(2).putString("id", "minecraft:bane_of_arthropods");
                    enchantments.getCompound(2).putInt("lvl", bane_of_arthropods.get());
                    }
                    if (knockback.get() > 0){
                    enchantments.add(new NbtCompound());
                    enchantments.getCompound(3).putString("id", "minecraft:knockback");
                    enchantments.getCompound(3).putInt("lvl", knockback.get());
                    }
                    if (fire_aspect.get() > 0) {
                    enchantments.add(new NbtCompound());
                    enchantments.getCompound(4).putString("id", "minecraft:fire_aspect");
                    enchantments.getCompound(4).putInt("lvl", fire_aspect.get());
                    }
                    if (looting.get() > 0) {
                    enchantments.add(new NbtCompound());
                    enchantments.getCompound(5).putString("id", "minecraft:looting");
                    enchantments.getCompound(5).putInt("lvl", looting.get());
                    }
                    if (sweeping_edge.get() > 0) {
                    enchantments.add(new NbtCompound());
                    enchantments.getCompound(6).putString("id", "minecraft:sweeping_edge");
                    enchantments.getCompound(6).putInt("lvl", sweeping_edge.get());
                    }
                    if (efficiency.get() > 0) {
                    enchantments.add(new NbtCompound());
                    enchantments.getCompound(7).putString("id", "minecraft:efficiency");
                    enchantments.getCompound(7).putInt("lvl", efficiency.get());
                    }
                    if (silk_touch.get() > 0) {
                    enchantments.add(new NbtCompound());
                    enchantments.getCompound(8).putString("id", "minecraft:silk_touch");
                    enchantments.getCompound(8).putInt("lvl", silk_touch.get());
                    }
                    if (unbreaking.get() > 0) {
                    enchantments.add(new NbtCompound());
                    enchantments.getCompound(9).putString("id", "minecraft:unbreaking");
                    enchantments.getCompound(9).putInt("lvl", unbreaking.get());
                    }
                    if (fortune.get() > 0) {
                    enchantments.add(new NbtCompound());
                    enchantments.getCompound(10).putString("id", "minecraft:fortune");
                    enchantments.getCompound(10).putInt("lvl", fortune.get());
                    }
                    if (power.get() > 0) {
                    enchantments.add(new NbtCompound());
                    enchantments.getCompound(11).putString("id", "minecraft:power");
                    enchantments.getCompound(11).putInt("lvl", power.get());
                    }
                    if (punch.get() > 0) {
                    enchantments.add(new NbtCompound());
                    enchantments.getCompound(12).putString("id", "minecraft:punch");
                    enchantments.getCompound(12).putInt("lvl", punch.get());
                    }
                    if (flame.get() > 0) {
                    enchantments.add(new NbtCompound());
                    enchantments.getCompound(13).putString("id", "minecraft:flame");
                    enchantments.getCompound(13).putInt("lvl", flame.get());
                    }
                    if (infinity.get() > 0) {
                    enchantments.add(new NbtCompound());
                    enchantments.getCompound(14).putString("id", "minecraft:infinity");
                    enchantments.getCompound(14).putInt("lvl", infinity.get());
                    }
                    if (luck_of_the_sea.get() > 0) {
                    enchantments.add(new NbtCompound());
                    enchantments.getCompound(15).putString("id", "minecraft:luck_of_the_sea");
                    enchantments.getCompound(15).putInt("lvl", luck_of_the_sea.get());
                    }
                    if (lure.get() > 0) {
                    enchantments.add(new NbtCompound());
                    enchantments.getCompound(16).putString("id", "minecraft:lure");
                    enchantments.getCompound(16).putInt("lvl", lure.get());
                    }
                    if (loyalty.get() > 0) {
                    enchantments.add(new NbtCompound());
                    enchantments.getCompound(17).putString("id", "minecraft:loyalty");
                    enchantments.getCompound(17).putInt("lvl", loyalty.get());
                    }
                    if (impaling.get() > 0) {
                    enchantments.add(new NbtCompound());
                    enchantments.getCompound(18).putString("id", "minecraft:impaling");
                    enchantments.getCompound(18).putInt("lvl", impaling.get());
                    }
                    if (riptide.get() > 0) {
                    enchantments.add(new NbtCompound());
                    enchantments.getCompound(19).putString("id", "minecraft:riptide");
                    enchantments.getCompound(19).putInt("lvl", riptide.get());
                    }
                    if (channeling.get() > 0) {
                    enchantments.add(new NbtCompound());
                    enchantments.getCompound(20).putString("id", "minecraft:channeling");
                    enchantments.getCompound(20).putInt("lvl", channeling.get());
                    }
                    if (multishot.get() > 0) {
                    enchantments.add(new NbtCompound());
                    enchantments.getCompound(21).putString("id", "minecraft:multishot");
                    enchantments.getCompound(21).putInt("lvl", multishot.get());
                    }
                    if (piercing.get() > 0) {
                    enchantments.add(new NbtCompound());
                    enchantments.getCompound(22).putString("id", "minecraft:piercing");
                    enchantments.getCompound(22).putInt("lvl", piercing.get());
                    }
                    if (quick_charge.get() > 0) {
                    enchantments.add(new NbtCompound());
                    enchantments.getCompound(23).putString("id", "minecraft:quick_charge");
                    enchantments.getCompound(23).putInt("lvl", quick_charge.get());
                    }
                    if (respiration.get() > 0) {
                    enchantments.add(new NbtCompound());
                    enchantments.getCompound(24).putString("id", "minecraft:respiration");
                    enchantments.getCompound(24).putInt("lvl", respiration.get());
                    }
                    if (protection.get() > 0) {
                    enchantments.add(new NbtCompound());
                    enchantments.getCompound(25).putString("id", "minecraft:protection");
                    enchantments.getCompound(25).putInt("lvl", protection.get());
                    }
                    if (fire_protection.get() > 0) {
                    enchantments.add(new NbtCompound());
                    enchantments.getCompound(26).putString("id", "minecraft:fire_protection");
                    enchantments.getCompound(26).putInt("lvl", fire_protection.get());
                    }
                    if (feather_falling.get() > 0) {
                    enchantments.add(new NbtCompound());
                    enchantments.getCompound(27).putString("id", "minecraft:feather_falling");
                    enchantments.getCompound(27).putInt("lvl", feather_falling.get());
                    }
                    if (blast_protection.get() > 0) {
                    enchantments.add(new NbtCompound());
                    enchantments.getCompound(28).putString("id", "minecraft:blast_protection");
                    enchantments.getCompound(28).putInt("lvl", blast_protection.get());
                    }
                    if (projectile_protection.get() > 0) {
                    enchantments.add(new NbtCompound());
                    enchantments.getCompound(29).putString("id", "minecraft:projectile_protection");
                    enchantments.getCompound(29).putInt("lvl", projectile_protection.get());
                    }
                    if (thorns.get() > 0) {
                    enchantments.add(new NbtCompound());
                    enchantments.getCompound(30).putString("id", "minecraft:thorns");
                    enchantments.getCompound(30).putInt("lvl", thorns.get());
                    }
                    if (aqua_affinity.get() > 0) {
                    enchantments.add(new NbtCompound());
                    enchantments.getCompound(31).putString("id", "minecraft:aqua_affinity");
                    enchantments.getCompound(31).putInt("lvl", aqua_affinity.get());
                    }
                    if (depth_strider.get() > 0) {
                    enchantments.add(new NbtCompound());
                    enchantments.getCompound(32).putString("id", "minecraft:depth_strider");
                    enchantments.getCompound(32).putInt("lvl", depth_strider.get());
                    }
                    if (mending.get() > 0) {
                    enchantments.add(new NbtCompound());
                    enchantments.getCompound(33).putString("id", "minecraft:mending");
                    enchantments.getCompound(33).putInt("lvl", mending.get());
                    }
                    if (soul_speed.get() > 0) {
                    enchantments.add(new NbtCompound());
                    enchantments.getCompound(34).putString("id", "minecraft:soul_speed");
                    enchantments.getCompound(34).putInt("lvl", soul_speed.get());
                    }
                    if (swift_sneak.get() > 0) {
                    enchantments.add(new NbtCompound());
                    enchantments.getCompound(35).putString("id", "minecraft:swift_sneak");
                    enchantments.getCompound(35).putInt("lvl", swift_sneak.get());
                    }
                    if (frost_walker.get() > 0) {
                        enchantments.add(new NbtCompound());
                        enchantments.getCompound(36).putString("id", "minecraft:frost_walker");
                        enchantments.getCompound(36).putInt("lvl", frost_walker.get());
                    }
                    if (binding_curse.get() > 0) {
                        enchantments.add(new NbtCompound());
                        enchantments.getCompound(37).putString("id", "minecraft:binding_curse");
                        enchantments.getCompound(37).putInt("lvl", binding_curse.get());
                    }
                    if (vanishing_curse.get() > 0) {
                        enchantments.add(new NbtCompound());
                        enchantments.getCompound(38).putString("id", "minecraft:vanishing_curse");
                        enchantments.getCompound(38).putInt("lvl", vanishing_curse.get());
                    }
                    tag.put("Enchantments", enchantments);
                    NbtCompound display = new NbtCompound();
                    display.putString("Name", "{\"text\":\"" + nom.get() + "\",\"color\":\"" + nomcolor.get() + "\"}");
                    tag.put("display", display);
                    item.setNbt(tag);
                    mc.interactionManager.clickCreativeStack(item, 36 + mc.player.getInventory().selectedSlot);

                }
            }
            ChatUtils.sendMsg(Text.of("Modified item created."));
            toggle();
        } else {
            error("You need to be in creative mode.");
            toggle();
        }
    }

    public enum Modes {
        Entity, Item
    }
}