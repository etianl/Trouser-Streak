package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
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
import net.minecraft.world.phys.Vec3;
import pwn.noobs.trouserstreak.Trouser;

import java.util.Random;

public class AirstrikePlus extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgeveryone = settings.createGroup("AIRSTRIKE EVERYONE Command Options");
    private final SettingGroup sgnormal = settings.createGroup("NORMAL Spawn Egg Options");

    private final Setting<Boolean> disconnectdisable = sgGeneral.add(new BoolSetting.Builder()
            .name("Disable on Disconnect")
            .description("Disables module on disconnecting")
            .defaultValue(false)
            .build());
    private final Setting<String> entity = sgGeneral.add(new StringSetting.Builder()
            .name("Entity to Spawn")
            .description("What is created. Ex: fireball, villager, minecart, lightning_bolt, magma_cube, tnt")
            .defaultValue("fireball")
            .build());
    private final Setting<Boolean> mixer = sgGeneral.add(new BoolSetting.Builder()
            .name("Mixer")
            .description("Mixes entities.")
            .defaultValue(false)
            .build());
    private final Setting<String> entity2 = sgGeneral.add(new StringSetting.Builder()
            .name("Entity2 to Spawn")
            .description("What is created. Ex: fireball, villager, minecart, lightning_bolt, magma_cube, tnt")
            .defaultValue("wither")
            .visible(mixer::get)
            .build());
    private final Setting<Boolean> randomPrefix = sgGeneral.add(new BoolSetting.Builder()
            .name("Random Prefix for Name")
            .description("Makes Boss Stacker module not work.")
            .defaultValue(false)
            .build());
    private final Setting<String> nom = sgGeneral.add(new StringSetting.Builder()
            .name("Custom Name")
            .description("Name the Entity")
            .defaultValue("MOUNTAINSOFLAVAINC")
            .build());
    private final Setting<BoomPlus.ColorModes> nomcolor = sgGeneral.add(new EnumSetting.Builder<BoomPlus.ColorModes>()
            .name("Custom Name Color")
            .description("Color the Name")
            .defaultValue(BoomPlus.ColorModes.red)
            .build());
    public enum ColorModes { aqua, black, blue, dark_aqua, dark_blue, dark_gray, dark_green, dark_purple, dark_red, gold, gray, green, italic, light_purple, red, white, yellow }
    public final Setting<Boolean> randomnomcolor = sgGeneral.add(new BoolSetting.Builder()
            .name("Rainbow Name Colors")
            .description("Name Colors are randomly selected.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Integer> minrange = sgGeneral.add(new IntSetting.Builder()
            .name("min-range")
            .description("radius they spawn from the player")
            .defaultValue(0)
            .sliderRange(0, 100)
            .min(0)
            .build()
    );
    private final Setting<Integer> maxrange = sgGeneral.add(new IntSetting.Builder()
            .name("max-range")
            .description("radius they spawn from the player")
            .defaultValue(30)
            .sliderRange(1, 100)
            .min(1)
            .build()
    );
    private final Setting<Integer> height = sgGeneral.add(new IntSetting.Builder()
            .name("HeightAboveHead")
            .description("How far from your Characters Y level to spawn at.")
            .defaultValue(20)
            .sliderRange(-63, 319)
            .build());

    private final Setting<Integer> speed = sgGeneral.add(new IntSetting.Builder()
            .name("speed")
            .description("speed of entities")
            .defaultValue(5)
            .sliderRange(1, 10)
            .min(1)
            .max(10)
            .build());

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("its in ticks")
            .defaultValue(2)
            .sliderRange(0, 20)
            .min(0)
            .build());
    private final Setting<Integer> grief = sgGeneral.add(new IntSetting.Builder()
            .name("Concurrent Spawns")
            .description("How many entities to spawn per tick.")
            .defaultValue(1)
            .sliderRange(1, 100)
            .min(1)
            .build());
    private final Setting<Boolean> airstrikeEveryone = sgGeneral.add(new BoolSetting.Builder()
            .name("Airstrike Everyone (OP)")
            .description("REQUIRES OP mode.")
            .defaultValue(false)
            .build());
    public final Setting<Boolean> customname = sgnormal.add(new BoolSetting.Builder()
            .name("CustomNameVisible")
            .description("CustomNameVisible or not.")
            .defaultValue(true)
            .visible(() -> !airstrikeEveryone.get())
            .build()
    );
    private final Setting<Integer> health = sgnormal.add(new IntSetting.Builder()
            .name("Health Points")
            .description("How much health.")
            .defaultValue(1000)
            .min(0)
            .sliderRange(0, 10000)
            .visible(() -> !airstrikeEveryone.get())
            .build());
    private final Setting<Integer> absorption = sgnormal.add(new IntSetting.Builder()
            .name("Absorption Points")
            .description("How much absorption.")
            .defaultValue(1000)
            .min(0)
            .sliderRange(0, 10000)
            .visible(() -> !airstrikeEveryone.get())
            .build());
    public final Setting<Boolean> ageSpecify = sgnormal.add(new BoolSetting.Builder()
            .name("Specify Age")
            .description("Add an Age NBT tag.")
            .defaultValue(false)
            .visible(() -> !airstrikeEveryone.get())
            .build()
    );
    private final Setting<Integer> age = sgnormal.add(new IntSetting.Builder()
            .name("Age")
            .description("It's age, 0 is baby.")
            .defaultValue(1)
            .min(0)
            .sliderRange(0, 100)
            .visible(() -> !airstrikeEveryone.get() && ageSpecify.get())
            .build());
    public final Setting<Boolean> invincible = sgnormal.add(new BoolSetting.Builder()
            .name("Invulnerable")
            .description("Invulnerable or not")
            .defaultValue(true)
            .visible(() -> !airstrikeEveryone.get())
            .build()
    );
    public final Setting<Boolean> persist = sgnormal.add(new BoolSetting.Builder()
            .name("Never Despawn")
            .description("adds PersistenceRequired tag.")
            .defaultValue(true)
            .visible(() -> !airstrikeEveryone.get())
            .build()
    );
    public final Setting<Boolean> noAI = sgnormal.add(new BoolSetting.Builder()
            .name("NoAI")
            .description("NoAI")
            .defaultValue(false)
            .visible(() -> !airstrikeEveryone.get())
            .build()
    );
    public final Setting<Boolean> falsefire = sgnormal.add(new BoolSetting.Builder()
            .name("HasVisualFire")
            .description("HasVisualFire or not")
            .defaultValue(false)
            .visible(() -> !airstrikeEveryone.get())
            .build()
    );
    public final Setting<Boolean> nograv = sgnormal.add(new BoolSetting.Builder()
            .name("NoGravity")
            .description("NoGravity or not")
            .defaultValue(false)
            .visible(() -> !airstrikeEveryone.get())
            .build()
    );
    public final Setting<Boolean> silence = sgnormal.add(new BoolSetting.Builder()
            .name("Silent")
            .description("adds Silent tag.")
            .defaultValue(false)
            .visible(() -> !airstrikeEveryone.get())
            .build()
    );
    public final Setting<Boolean> glow = sgnormal.add(new BoolSetting.Builder()
            .name("Glowing")
            .description("Glowing or not")
            .defaultValue(false)
            .visible(() -> !airstrikeEveryone.get())
            .build()
    );
    public final Setting<Boolean> ignite = sgnormal.add(new BoolSetting.Builder()
            .name("Ignited")
            .description("Pre-ignite creeper or not.")
            .defaultValue(true)
            .visible(() -> !airstrikeEveryone.get())
            .build()
    );
    public final Setting<Boolean> powah = sgnormal.add(new BoolSetting.Builder()
            .name("Charged Creeper")
            .description("powered creeper or not.")
            .defaultValue(false)
            .visible(() -> !airstrikeEveryone.get())
            .build()
    );
    private final Setting<Integer> fuse = sgnormal.add(new IntSetting.Builder()
            .name("Creeper/TNT Fuse")
            .description("In ticks")
            .defaultValue(20)
            .min(0)
            .sliderRange(0, 120)
            .visible(() -> !airstrikeEveryone.get())
            .build());
    private final Setting<Integer> exppower = sgnormal.add(new IntSetting.Builder()
            .name("ExplosionPower/Radius")
            .description("For Creepers and Fireballs")
            .defaultValue(10)
            .min(1)
            .sliderMax(127)
            .visible(() -> !airstrikeEveryone.get())
            .build());
    private final Setting<Integer> size = sgnormal.add(new IntSetting.Builder()
            .name("Slime/Magma Cube Size")
            .description("It's size, 100 is really big.")
            .defaultValue(1)
            .min(0)
            .sliderRange(0, 100)
            .visible(() -> !airstrikeEveryone.get())
            .build());
    public final Setting<Boolean> blockstateSpecify = sgnormal.add(new BoolSetting.Builder()
            .name("Specify falling_block")
            .description("Add an NBT tag defining what is the falling block.")
            .defaultValue(false)
            .visible(() -> !airstrikeEveryone.get())
            .build()
    );
    private final Setting<Block> blockstate = sgnormal.add(new BlockSetting.Builder()
            .name("falling_block entity block")
            .description("What is created when specifying falling_block as the entity.")
            .defaultValue(Blocks.BEDROCK)
            .visible(() -> !airstrikeEveryone.get() && blockstateSpecify.get())
            .build());
    public final Setting<Boolean> Ecustomname = sgeveryone.add(new BoolSetting.Builder()
            .name("CustomNameVisible")
            .description("CustomNameVisible or not.")
            .defaultValue(false)
            .visible(airstrikeEveryone::get)
            .build()
    );
    public final Setting<Boolean> EcustomHP = sgeveryone.add(new BoolSetting.Builder()
            .name("Modify Health Points")
            .defaultValue(false)
            .visible(airstrikeEveryone::get)
            .build()
    );
    private final Setting<Integer> Ehealth = sgeveryone.add(new IntSetting.Builder()
            .name("Health Points")
            .description("How much health.")
            .defaultValue(100)
            .min(0)
            .sliderRange(0, 10000)
            .visible(() -> airstrikeEveryone.get() && EcustomHP.get())
            .build());
    private final Setting<Integer> Eabsorption = sgeveryone.add(new IntSetting.Builder()
            .name("Absorption Points")
            .description("How much absorption.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 10000)
            .visible(airstrikeEveryone::get)
            .build());
    public final Setting<Boolean> EageSpecify = sgeveryone.add(new BoolSetting.Builder()
            .name("Specify Age")
            .description("Add an Age NBT tag.")
            .defaultValue(false)
            .visible(airstrikeEveryone::get)
            .build()
    );
    private final Setting<Integer> Eage = sgeveryone.add(new IntSetting.Builder()
            .name("Age")
            .description("It's age, 0 is baby.")
            .defaultValue(1)
            .min(0)
            .sliderRange(0, 100)
            .visible(() -> airstrikeEveryone.get() && EageSpecify.get())
            .build());
    public final Setting<Boolean> Einvincible = sgeveryone.add(new BoolSetting.Builder()
            .name("Invulnerable")
            .description("Invulnerable or not")
            .defaultValue(true)
            .visible(airstrikeEveryone::get)
            .build()
    );
    public final Setting<Boolean> Epersist = sgeveryone.add(new BoolSetting.Builder()
            .name("Never Despawn")
            .description("adds PersistenceRequired tag.")
            .defaultValue(false)
            .visible(airstrikeEveryone::get)
            .build()
    );
    public final Setting<Boolean> EnoAI = sgeveryone.add(new BoolSetting.Builder()
            .name("NoAI")
            .description("NoAI")
            .defaultValue(false)
            .visible(airstrikeEveryone::get)
            .build()
    );
    public final Setting<Boolean> Efalsefire = sgeveryone.add(new BoolSetting.Builder()
            .name("HasVisualFire")
            .description("HasVisualFire or not")
            .defaultValue(false)
            .visible(airstrikeEveryone::get)
            .build()
    );
    public final Setting<Boolean> Enograv = sgeveryone.add(new BoolSetting.Builder()
            .name("NoGravity")
            .description("NoGravity or not")
            .defaultValue(false)
            .visible(airstrikeEveryone::get)
            .build()
    );
    public final Setting<Boolean> Esilence = sgeveryone.add(new BoolSetting.Builder()
            .name("Silent")
            .description("adds Silent tag.")
            .defaultValue(false)
            .visible(airstrikeEveryone::get)
            .build()
    );
    public final Setting<Boolean> Eglow = sgeveryone.add(new BoolSetting.Builder()
            .name("Glowing")
            .description("Glowing or not")
            .defaultValue(false)
            .visible(airstrikeEveryone::get)
            .build()
    );
    public final Setting<Boolean> Eignite = sgeveryone.add(new BoolSetting.Builder()
            .name("Ignited")
            .description("Pre-ignite creeper or not.")
            .defaultValue(true)
            .visible(airstrikeEveryone::get)
            .build()
    );
    public final Setting<Boolean> Epowah = sgeveryone.add(new BoolSetting.Builder()
            .name("Charged Creeper")
            .description("powered creeper or not.")
            .defaultValue(false)
            .visible(airstrikeEveryone::get)
            .build()
    );
    private final Setting<Integer> Efuse = sgeveryone.add(new IntSetting.Builder()
            .name("Creeper/TNT Fuse")
            .description("In ticks")
            .defaultValue(20)
            .min(0)
            .sliderRange(0, 120)
            .visible(airstrikeEveryone::get)
            .build());
    private final Setting<Integer> Eexppower = sgeveryone.add(new IntSetting.Builder()
            .name("ExplosionPower/Radius")
            .description("For Creepers and Fireballs")
            .defaultValue(10)
            .min(1)
            .sliderMax(127)
            .visible(airstrikeEveryone::get)
            .build());
    private final Setting<Integer> Esize = sgeveryone.add(new IntSetting.Builder()
            .name("Slime/Magma Cube Size")
            .description("It's size, 100 is really big.")
            .defaultValue(1)
            .min(0)
            .sliderRange(0, 100)
            .visible(airstrikeEveryone::get)
            .build());
    public final Setting<Boolean> EblockstateSpecify = sgeveryone.add(new BoolSetting.Builder()
            .name("Specify falling_block")
            .description("Add an NBT tag defining what is the falling block.")
            .defaultValue(false)
            .visible(airstrikeEveryone::get)
            .build()
    );
    private final Setting<Block> Eblockstate = sgeveryone.add(new BlockSetting.Builder()
            .name("falling_block entity block")
            .description("What is created when specifying falling_block as the entity.")
            .defaultValue(Blocks.BEDROCK)
            .visible(() -> airstrikeEveryone.get() && EblockstateSpecify.get())
            .build());

    public AirstrikePlus() {
        super(Trouser.operator, "Airstrike+", "Rains things down from the sky");
    }

    final Random r = new Random();
    Vec3 origin = null;
    int i = 0;
    private int mix=0;
    private String namecolour = nomcolor.get().toString();
    private String entityName = entity.get().trim().replace(" ", "_");
    private String customName = nom.get();

    private final String[] prefixes = {
            "§k111 §r| ",
            "§k222 §r| ",
            "§k333 §r| ",
            "§k444 §r| ",
            "§k555 §r| ",
            "§k666 §r| ",
            "§k777 §r| ",
            "§k888 §r| ",
            "§k999 §r| "
    };
    private Vec3 pickRandomPos() {
        double minR = minrange.get();
        double maxR = maxrange.get();

        double angle = r.nextDouble(Math.PI * 2);

        double dist = (maxR > minR)
                ? minR + r.nextDouble(maxR - minR)
                : minR;

        double x = origin.x + Math.cos(angle) * dist;
        double z = origin.z + Math.sin(angle) * dist;
        double y = mc.player.getY() + height.get();

        return new Vec3(x, y, z);
    }
    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (disconnectdisable.get() && event.screen instanceof DisconnectedScreen) {
            toggle();
        }
    }
    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (disconnectdisable.get())toggle();
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        origin = mc.player.position();
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (randomPrefix.get()) {
            String randomPrefix = prefixes[r.nextInt(prefixes.length)];
            customName = randomPrefix + nom.get();
        } else {
            customName = nom.get();
        }
        if (mixer.get()) {
            mix = (mix + 1) % 2;
            entityName = (mix == 0 ? entity.get() : entity2.get()).trim().replace(" ", "_");
        } else entityName = entity.get().trim().replace(" ", "_");
        for (int griefs = 0; griefs < grief.get(); griefs++) {
            if (airstrikeEveryone.get()) executeCommandsToCreateEntities();
            else {
                if (randomnomcolor.get()) {
                    String[] colorCodes = {"black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white"};
                    int index = r.nextInt(colorCodes.length);
                    namecolour = colorCodes[index];
                } else namecolour = nomcolor.get().toString();
                ItemStack bomb = new ItemStack(Items.SALMON_SPAWN_EGG);
                ItemStack bfr = mc.player.getMainHandItem();
                BlockHitResult bhr = new BlockHitResult(mc.player.position().add(0, 1, 0), Direction.UP, new BlockPos(mc.player.blockPosition().offset(0, 1, 0)), false);
                i++;
                if (mc.player.getAbilities().instabuild) {
                    if (i >= delay.get()) {
                        var changes = DataComponentPatch.builder()
                                .set(DataComponents.CUSTOM_NAME, Component.literal(customName).withStyle(ChatFormatting.valueOf(namecolour.toUpperCase())))
                                .set(DataComponents.ITEM_NAME, Component.literal(customName).withStyle(ChatFormatting.valueOf(namecolour.toUpperCase())))
                                .set(DataComponents.ENTITY_DATA, createEntityData())
                                .build();
                        bomb.applyComponentsAndValidate(changes);
                        mc.gameMode.handleCreativeModeItemAdd(bomb, 36 + mc.player.getInventory().getSelectedSlot());
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, bhr);
                        mc.gameMode.handleCreativeModeItemAdd(bfr, 36 + mc.player.getInventory().getSelectedSlot());
                        i = 0;
                    }
                } else {
                    error("You need to be in creative mode.");
                    toggle();
                }
            }
        }
    }
    private TypedEntityData<EntityType<?>> createEntityData() {
        String fullString = blockstate.get().toString();
        String[] parts = fullString.split(":");
        String block = parts[1];
        String blockName = block.replace("}", "");
        CompoundTag entityTag = new CompoundTag();
        ListTag pos = new ListTag();
        ListTag speedlist = new ListTag();
        Vec3 cpos = pickRandomPos();

        speedlist.add(DoubleTag.valueOf(0));
        speedlist.add(DoubleTag.valueOf(-speed.get()));
        speedlist.add(DoubleTag.valueOf(0));
        pos.add(DoubleTag.valueOf(cpos.x));
        pos.add(DoubleTag.valueOf(mc.player.getY() + height.get()));
        pos.add(DoubleTag.valueOf(cpos.z));

        entityTag.putString("id", "minecraft:" + entityName);
        if (entity.get().equals("dragon_fireball") || entity2.get().equals("dragon_fireball") || entity.get().equals("fireball") || entity2.get().equals("fireball") || entity.get().equals("small_fireball") || entity2.get().equals("small_fireball") || entity.get().equals("wither_skull") || entity2.get().equals("wither_skull") || entity.get().equals("wind_projectile") || entity2.get().equals("wind_projectile")) {
            entityTag.put("power", speedlist);
        }
        entityTag.put("Motion", speedlist);
        entityTag.put("Pos", pos);
        entityTag.putInt("Health", health.get());
        entityTag.putInt("AbsorptionAmount", absorption.get());
        if (ageSpecify.get()) entityTag.putInt("Age", age.get());
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
        EntityType<?> entityType = (entityId != null)
                ? BuiltInRegistries.ENTITY_TYPE.getValue(entityId)
                : EntityType.FIREBALL;

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
    private void executeCommandsToCreateEntities() {
        ListTag speedlist = new ListTag();
        if (randomnomcolor.get()){
            String[] colorCodes = {"black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white"};
            int index = r.nextInt(colorCodes.length);
            namecolour = colorCodes[index];
        } else namecolour = nomcolor.get().toString();
        speedlist.add(DoubleTag.valueOf(0));
        speedlist.add(DoubleTag.valueOf(-speed.get()));
        speedlist.add(DoubleTag.valueOf(0));
        String nameColor = namecolour;
        int healthPoints = Ehealth.get();
        int absorptionPoints = Eabsorption.get();
        int ageValue = Eage.get();
        int explosionPower = Eexppower.get();
        int explosionRadius = Eexppower.get();
        boolean isInvulnerable = Einvincible.get();
        boolean isSilent = Esilence.get();
        boolean isGlowing = Eglow.get();
        boolean isPersistent = Epersist.get();
        boolean hasNoGravity = Enograv.get();
        boolean hasNoAI = EnoAI.get();
        boolean hasVisualFire = Efalsefire.get();
        boolean isPowered = Epowah.get();
        boolean isIgnited = Eignite.get();
        int fuseTicks = Efuse.get();
        int sizeValue = Esize.get();
        boolean isCustomNameVisible = Ecustomname.get();
        String fullString = Eblockstate.get().toString();
        String[] parts = fullString.split(":");
        String block = parts[1];
        String blockName = block.replace("}", "");
        CompoundTag blockState = new CompoundTag();
        blockState.putString("Name", "minecraft:" + blockName);

        String command = "/execute as @a at @s run summon " + entityName + " ";
        double angle = r.nextDouble(Math.PI * 2);
        double dist = minrange.get() + r.nextDouble(maxrange.get() - minrange.get());
        int xOffset = (int) (Math.cos(angle) * dist);
        int zOffset = (int) (Math.sin(angle) * dist);
        command += String.format("~%d ~%d ~%d", xOffset, height.get(), zOffset);
        command += " {";
        if (Ecustomname.get()) {
            String serverVersion;
            if (mc.hasSingleplayerServer()) {
                serverVersion = mc.getSingleplayerServer().getServerVersion();
            } else {
                serverVersion = mc.getCurrentServer().version.tryCollapseToString();
            }
            if (serverVersion == null) {
                command += "\"CustomName\":[{\"text\":\"" + customName + "\",\"color\":\"" + nameColor + "\"}],";
            } else {
                if (isVersionLessThan(serverVersion, 1, 21, 5)) {
                    command += "\"CustomName\":\"{\\\"text\\\":\\\"" + customName + "\\\",\\\"color\\\":\\\"" + nameColor + "\\\"}\",";
                } else {
                    command += "\"CustomName\":[{\"text\":\"" + customName + "\",\"color\":\"" + nameColor + "\"}],";
                }
            }
        }
        if (EcustomHP.get()) command += "\"Health\":" + healthPoints + ",";
        if (Eabsorption.get() > 0) command += "\"AbsorptionAmount\":" + absorptionPoints + ",";
        if (EageSpecify.get()) command += "\"Age\":" + ageValue + ",";
        if (EblockstateSpecify.get()) command += "\"BlockState\":" + blockState + ",";
        if (entity.get().equals("fireball") || entity2.get().equals("fireball")) command += "\"ExplosionPower\":" + explosionPower + ",";
        if (entity.get().equals("creeper") || entity2.get().equals("creeper")) command += "\"ExplosionRadius\":" + explosionRadius + ",";
        if (Einvincible.get()) command += "\"Invulnerable\":" + isInvulnerable + ",";
        if (Esilence.get()) command += "\"Silent\":" + isSilent + ",";
        if (Eglow.get()) command += "\"Glowing\":" + isGlowing + ",";
        if (Epersist.get()) command += "\"PersistenceRequired\":" + isPersistent + ",";
        if (Enograv.get()) command += "\"NoGravity\":" + hasNoGravity + ",";
        if (EnoAI.get()) command += "\"NoAI\":" + hasNoAI + ",";
        if (Efalsefire.get()) command += "\"HasVisualFire\":" + hasVisualFire + ",";
        if (Epowah.get()) command += "\"powered\":" + isPowered + ",";
        if (Eignite.get() && (entity.get().equals("creeper") || entity2.get().equals("creeper"))) command += "\"ignited\":" + isIgnited + ",";
        if (entity.get().equals("tnt") || entity2.get().equals("tnt") || entity.get().equals("creeper") || entity2.get().equals("creeper")) command += "\"Fuse\":" + fuseTicks + ",";
        if (entity.get().equals("slime") || entity2.get().equals("slime") || entity.get().equals("magma_cube") || entity2.get().equals("magma_cube")) command += "\"Size\":" + sizeValue + ",";
        if (Ecustomname.get()) command += "\"CustomNameVisible\":" + isCustomNameVisible + ",";
        if (entity.get().equals("dragon_fireball") || entity2.get().equals("dragon_fireball") || entity.get().equals("fireball") || entity2.get().equals("fireball") || entity.get().equals("small_fireball") || entity2.get().equals("small_fireball") || entity.get().equals("wither_skull") || entity2.get().equals("wither_skull") || entity.get().equals("wind_projectile") || entity2.get().equals("wind_projectile")) command += "\"power\":" + speedlist + ",";
        command += "\"Motion\":" + speedlist;
        command += "}";
        if (command.length()<=256)ChatUtils.sendPlayerMsg(command);
        else {
            int characterstodelete = command.length() - 256;
            error("The command is too long (" + command + ").");
            error("Shorten it by " + characterstodelete + " characters.");

            toggle();
        }
        i = 0;
    }
}