package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import pwn.noobs.trouserstreak.Trouser;

public class BoomPlus extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgOptions = settings.createGroup("Nbt Options");

    private final Setting<String> entity = sgGeneral.add(new StringSetting.Builder()
            .name("Entity to Spawn")
            .description("What is created. Ex: fireball, villager, minecart, lightning_bolt, magma_cube, tnt")
            .defaultValue("fireball")
            .build());
    private final Setting<String> nom = sgGeneral.add(new StringSetting.Builder()
            .name("Custom Name")
            .description("Name the Entity")
            .defaultValue("MOUNTAINSOFLAVAINC").build());
    private final Setting<ColorModes> nomcolor = sgGeneral.add(new EnumSetting.Builder<ColorModes>()
            .name("Custom Name Color")
            .description("Color the Name")
            .defaultValue(ColorModes.red)
            .build());
    public enum ColorModes { aqua, black, blue, dark_aqua, dark_blue, dark_gray, dark_green, dark_purple, dark_red, gold, gray, green, italic, light_purple, red, white, yellow }
    public final Setting<Boolean> customname = sgOptions.add(new BoolSetting.Builder()
            .name("CustomNameVisible")
            .description("CustomNameVisible or not.")
            .defaultValue(true)
            .build());
    private final Setting<Integer> health = sgOptions.add(new IntSetting.Builder()
            .name("Health Points")
            .description("How much health.")
            .defaultValue(100)
            .min(0)
            .sliderRange(0, 100)
            .build());
    private final Setting<Integer> absorption = sgOptions.add(new IntSetting.Builder()
            .name("Absorption Points")
            .description("How much absorption.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 100)
            .build());
    private final Setting<Integer> age = sgOptions.add(new IntSetting.Builder()
            .name("Age")
            .description("It's age, 0 is baby.")
            .defaultValue(1)
            .min(0)
            .sliderRange(0, 100)
            .build());
    public final Setting<Boolean> invincible = sgOptions.add(new BoolSetting.Builder()
            .name("Invulnerable")
            .description("Invulnerable or not")
            .defaultValue(true)
            .build());
    public final Setting<Boolean> persist = sgOptions.add(new BoolSetting.Builder()
            .name("Never Despawn")
            .description("adds PersistenceRequired tag.")
            .defaultValue(true)
            .build());
    public final Setting<Boolean> noAI = sgOptions.add(new BoolSetting.Builder()
            .name("NoAI")
            .description("NoAI")
            .defaultValue(false)
            .build());
    public final Setting<Boolean> falsefire = sgOptions.add(new BoolSetting.Builder()
            .name("HasVisualFire")
            .description("HasVisualFire or not")
            .defaultValue(false)
            .build());
    public final Setting<Boolean> nograv = sgOptions.add(new BoolSetting.Builder()
            .name("NoGravity")
            .description("NoGravity or not")
            .defaultValue(false)
            .build());
    public final Setting<Boolean> silence = sgOptions.add(new BoolSetting.Builder()
            .name("Silent")
            .description("adds Silent tag.")
            .defaultValue(false)
            .build());
    public final Setting<Boolean> glow = sgOptions.add(new BoolSetting.Builder()
            .name("Glowing")
            .description("Glowing or not")
            .defaultValue(false)
            .build());
    public final Setting<Boolean> ignite = sgOptions.add(new BoolSetting.Builder()
            .name("Ignited")
            .description("Pre-ignite creeper or not.")
            .defaultValue(true)
            .build());
    public final Setting<Boolean> powah = sgOptions.add(new BoolSetting.Builder()
            .name("Charged Creeper")
            .description("powered creeper or not.")
            .defaultValue(false)
            .build());
    private final Setting<Integer> fuse = sgOptions.add(new IntSetting.Builder()
            .name("Creeper/TNT Fuse")
            .description("In ticks")
            .defaultValue(20)
            .min(0)
            .sliderRange(0, 120)
            .build());
    private final Setting<Integer> exppower = sgOptions.add(new IntSetting.Builder()
            .name("ExplosionPower/Radius")
            .description("For Creepers and Fireballs")
            .defaultValue(10)
            .min(1)
            .sliderMax(127)
            .build());
    private final Setting<Integer> size = sgOptions.add(new IntSetting.Builder()
            .name("Slime/Magma Cube Size")
            .description("It's size, 100 is really big.")
            .defaultValue(1)
            .min(0)
            .sliderRange(0, 100)
            .build());
    private final Setting<Block> blockstate = sgOptions.add(new BlockSetting.Builder()
            .name("falling_block entity block")
            .description("What is created when specifying falling_block as the entity.")
            .defaultValue(Blocks.BEDROCK)
            .build());
    public final Setting<Boolean> target = sgGeneral.add(new BoolSetting.Builder()
            .name("OnTarget")
            .description("spawns on target")
            .defaultValue(false)
            .build());
    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
            .name("speed")
            .description("fastness of thing")
            .defaultValue(5)
            .min(1)
            .sliderMax(10)
            .build());
    public final Setting<Boolean> auto = sgGeneral.add(new BoolSetting.Builder()
            .name("FULLAUTO")
            .description("FULL AUTO BABY!")
            .defaultValue(false)
            .build());
    public final Setting<Integer> atickdelay = sgGeneral.add(new IntSetting.Builder()
            .name("FULLAUTOTickDelay")
            .description("Tick Delay for FULLAUTO option.")
            .defaultValue(2)
            .min(0)
            .sliderMax(20)
            .visible(auto::get)
            .build());

    public BoomPlus() {
        super(Trouser.operator, "boom+", "shoots something where you click");
    }
    private int aticks=0;
    private String namecolour = nomcolor.get().toString();
    private String customName = nom.get();

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (!mc.player.getAbilities().instabuild) {
            error("You need to be in creative mode.");
            toggle();
        }

        if (auto.get() && mc.options.keyAttack.isDown() && mc.screen == null && mc.player.getAbilities().instabuild) {
            if (aticks<=atickdelay.get()){
                aticks++;
            } else if (aticks>atickdelay.get()) {
                customName = nom.get();
                namecolour = nomcolor.get().toString();
                ItemStack rst = mc.player.getMainHandItem();
                BlockHitResult bhr = new BlockHitResult(mc.player.getEyePosition(), Direction.DOWN, BlockPos.containing(mc.player.getEyePosition()), false);
                ItemStack item = new ItemStack(Items.BEE_SPAWN_EGG);
                var changes = DataComponentPatch.builder()
                        .set(DataComponents.CUSTOM_NAME, Component.literal(customName).withStyle(ChatFormatting.valueOf(namecolour.toUpperCase())))
                        .set(DataComponents.ITEM_NAME, Component.literal(customName).withStyle(ChatFormatting.valueOf(namecolour.toUpperCase())))
                        .set(DataComponents.ENTITY_DATA, createEntityData())
                        .build();
                item.applyComponentsAndValidate(changes);
                mc.gameMode.handleCreativeModeItemAdd(item, 36 + mc.player.getInventory().getSelectedSlot());
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, bhr);
                mc.gameMode.handleCreativeModeItemAdd(rst, 36 + mc.player.getInventory().getSelectedSlot());
                aticks=0;
            }
        }
    }

    @EventHandler
    private void onMouseButton(MouseClickEvent event) {
        if (mc.options.keyAttack.isDown() && mc.screen == null && mc.player.getAbilities().instabuild) {
            customName = nom.get();
            namecolour = nomcolor.get().toString();
            ItemStack rst = mc.player.getMainHandItem();
            BlockHitResult bhr = new BlockHitResult(mc.player.getEyePosition(), Direction.DOWN, BlockPos.containing(mc.player.getEyePosition()), false);
            ItemStack item = new ItemStack(Items.BEE_SPAWN_EGG);
            var changes = DataComponentPatch.builder()
                    .set(DataComponents.CUSTOM_NAME, Component.literal(customName).withStyle(ChatFormatting.valueOf(namecolour.toUpperCase())))
                    .set(DataComponents.ITEM_NAME, Component.literal(customName).withStyle(ChatFormatting.valueOf(namecolour.toUpperCase())))
                    .set(DataComponents.ENTITY_DATA, createEntityData())
                    .build();
            item.applyComponentsAndValidate(changes);
            mc.gameMode.handleCreativeModeItemAdd(item, 36 + mc.player.getInventory().getSelectedSlot());
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, bhr);
            mc.gameMode.handleCreativeModeItemAdd(rst, 36 + mc.player.getInventory().getSelectedSlot());
        }
    }
    private TypedEntityData<EntityType<?>> createEntityData() {
        String fullString = blockstate.get().toString();
        String[] parts = fullString.split(":");
        String block = parts[1];
        String blockName = block.replace("}", "");
        ListTag motion = new ListTag();
        ListTag Pos = new ListTag();
        HitResult hr = mc.getCameraEntity().pick(900, 0, true);
        Vec3 owo = hr.getLocation();
        BlockPos pos = BlockPos.containing(owo);
        Vec3 sex = mc.player.getLookAngle().scale(speed.get());
        String entityName = entity.get().trim().replace(" ", "_");

        CompoundTag entityTag = new CompoundTag();
        if (target.get()) {
            Pos.add(DoubleTag.valueOf(pos.getX()));
            Pos.add(DoubleTag.valueOf(pos.getY()));
            Pos.add(DoubleTag.valueOf(pos.getZ()));
            entityTag.put("Pos", Pos);
        } else {
            Pos.add(DoubleTag.valueOf(mc.player.getX()));
            Pos.add(DoubleTag.valueOf(mc.player.getY()+1));
            Pos.add(DoubleTag.valueOf(mc.player.getZ()));
            entityTag.put("Pos", Pos);
            motion.add(DoubleTag.valueOf(sex.x));
            motion.add(DoubleTag.valueOf(sex.y));
            motion.add(DoubleTag.valueOf(sex.z));
            entityTag.put("Motion", motion);
        }

        entityTag.putString("id", "minecraft:" + entityName);
        entityTag.putInt("Health", health.get());
        entityTag.putInt("AbsorptionAmount", absorption.get());
        entityTag.putInt("Age", age.get());
        entityTag.putInt("ExplosionPower", exppower.get());
        entityTag.putInt("ExplosionRadius", exppower.get());
        CompoundTag blockState = new CompoundTag();
        blockState.putString("Name", "minecraft:" + blockName);
        entityTag.put("BlockState", blockState);
        CompoundTag CustomNameNBT = new CompoundTag();
        CustomNameNBT.putString("text", customName);
        CustomNameNBT.putString("color", namecolour);

        if (invincible.get()) entityTag.putBoolean("Invulnerable", invincible.get());
        if (silence.get()) entityTag.putBoolean("Silent", silence.get());
        if (glow.get()) entityTag.putBoolean("Glowing", glow.get());
        if (persist.get()) entityTag.putBoolean("PersistenceRequired", persist.get());
        if (nograv.get()) entityTag.putBoolean("NoGravity", nograv.get());
        if (noAI.get()) entityTag.putBoolean("NoAI", noAI.get());
        if (falsefire.get()) entityTag.putBoolean("HasVisualFire", falsefire.get());
        if (powah.get()) entityTag.putBoolean("powered", powah.get());
        if (ignite.get()) entityTag.putBoolean("ignited", ignite.get());
        entityTag.putInt("Fuse", fuse.get());
        entityTag.putInt("Size", size.get());
        if (customname.get()) entityTag.putBoolean("CustomNameVisible", customname.get());
        String serverVersion;
        if (mc.hasSingleplayerServer()) {
            serverVersion = mc.getSingleplayerServer().getServerVersion();
        } else {
            serverVersion = mc.getCurrentServer().version.tryCollapseToString();
        }
        if (serverVersion == null) {
            entityTag.put("CustomName", CustomNameNBT);
        } else {
            if (isVersionLessThan(serverVersion, 1, 21, 5)) {
                entityTag.putString("CustomName", "{\"text\":\"" + nom.get() + "\",\"color\":\"" + nomcolor.get().name() + "\"}");
            } else {
                entityTag.put("CustomName", CustomNameNBT);
            }
        }

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
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }
}