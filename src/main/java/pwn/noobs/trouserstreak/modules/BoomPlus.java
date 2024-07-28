package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.*;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
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
    private final Setting<String> nomcolor = sgGeneral.add(new StringSetting.Builder()
            .name("Custom Name Color")
            .description("Color the Name")
            .defaultValue("red")
            .build());
    public final Setting<Boolean> customname = sgOptions.add(new BoolSetting.Builder()
            .name("CustomNameVisible")
            .description("CustomNameVisible or not.")
            .defaultValue(true)
            .build()
    );
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
            .build()
    );
    public final Setting<Boolean> persist = sgOptions.add(new BoolSetting.Builder()
            .name("Never Despawn")
            .description("adds PersistenceRequired tag.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> noAI = sgOptions.add(new BoolSetting.Builder()
            .name("NoAI")
            .description("NoAI")
            .defaultValue(false)
            .build()
    );
    public final Setting<Boolean> falsefire = sgOptions.add(new BoolSetting.Builder()
            .name("HasVisualFire")
            .description("HasVisualFire or not")
            .defaultValue(false)
            .build()
    );
    public final Setting<Boolean> nograv = sgOptions.add(new BoolSetting.Builder()
            .name("NoGravity")
            .description("NoGravity or not")
            .defaultValue(false)
            .build()
    );
    public final Setting<Boolean> silence = sgOptions.add(new BoolSetting.Builder()
            .name("Silent")
            .description("adds Silent tag.")
            .defaultValue(false)
            .build()
    );
    public final Setting<Boolean> glow = sgOptions.add(new BoolSetting.Builder()
            .name("Glowing")
            .description("Glowing or not")
            .defaultValue(false)
            .build()
    );
    public final Setting<Boolean> ignite = sgOptions.add(new BoolSetting.Builder()
            .name("Ignited")
            .description("Pre-ignite creeper or not.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> powah = sgOptions.add(new BoolSetting.Builder()
            .name("Charged Creeper")
            .description("powered creeper or not.")
            .defaultValue(false)
            .build()
    );
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
            .build()
    );

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
            .build()
    );
    public final Setting<Integer> atickdelay = sgGeneral.add(new IntSetting.Builder()
            .name("FULLAUTOTickDelay")
            .description("Tick Delay for FULLAUTO option.")
            .defaultValue(2)
            .min(0)
            .sliderMax(20)
            .visible(() -> auto.get())
            .build()
    );

    public BoomPlus() {
        super(Trouser.Main, "boom+", "shoots something where you click");
    }
    private int aticks=0;
    private String namecolour = nomcolor.get();
    private String customName = nom.get();

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (!mc.player.getAbilities().creativeMode) {
            error("You need to be in creative mode.");
            toggle();
        }

        if (auto.get() && mc.options.attackKey.isPressed() && mc.currentScreen == null && mc.player.getAbilities().creativeMode) {
            if (aticks<=atickdelay.get()){
                aticks++;
            } else if (aticks>atickdelay.get()) {
                customName = nom.get();
                namecolour = nomcolor.get();
                ItemStack rst = mc.player.getMainHandStack();
                BlockHitResult bhr = new BlockHitResult(mc.player.getEyePos(), Direction.DOWN, BlockPos.ofFloored(mc.player.getEyePos()), false);
                ItemStack item = new ItemStack(Items.BEE_SPAWN_EGG);
                var changes = ComponentChanges.builder()
                        .add(DataComponentTypes.CUSTOM_NAME, Text.literal(customName).formatted(Formatting.valueOf(namecolour.toUpperCase())))
                        .add(DataComponentTypes.ITEM_NAME, Text.literal(customName).formatted(Formatting.valueOf(namecolour.toUpperCase())))
                        .add(DataComponentTypes.ENTITY_DATA, createEntityData())
                        .build();
                item.applyChanges(changes);
                mc.interactionManager.clickCreativeStack(item, 36 + mc.player.getInventory().selectedSlot);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                aticks=0;
            }
        }
    }

    @EventHandler
    private void onMouseButton(MouseButtonEvent event) {
        if (mc.options.attackKey.isPressed() && mc.currentScreen == null && mc.player.getAbilities().creativeMode) {
            customName = nom.get();
            namecolour = nomcolor.get();
            ItemStack rst = mc.player.getMainHandStack();
            BlockHitResult bhr = new BlockHitResult(mc.player.getEyePos(), Direction.DOWN, BlockPos.ofFloored(mc.player.getEyePos()), false);
            ItemStack item = new ItemStack(Items.BEE_SPAWN_EGG);
            var changes = ComponentChanges.builder()
                    .add(DataComponentTypes.CUSTOM_NAME, Text.literal(customName).formatted(Formatting.valueOf(namecolour.toUpperCase())))
                    .add(DataComponentTypes.ITEM_NAME, Text.literal(customName).formatted(Formatting.valueOf(namecolour.toUpperCase())))
                    .add(DataComponentTypes.ENTITY_DATA, createEntityData())
                    .build();
            item.applyChanges(changes);
            mc.interactionManager.clickCreativeStack(item, 36 + mc.player.getInventory().selectedSlot);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
            mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
        }
    }
    private NbtComponent createEntityData() {
        String fullString = blockstate.get().toString();
        String[] parts = fullString.split(":");
        String block = parts[1];
        String blockName = block.replace("}", "");
        NbtList motion = new NbtList();
        NbtList Pos = new NbtList();
        HitResult hr = mc.cameraEntity.raycast(900, 0, true);
        Vec3d owo = hr.getPos();
        BlockPos pos = BlockPos.ofFloored(owo);
        Vec3d sex = mc.player.getRotationVector().multiply(speed.get());
        String entityName = entity.get().trim().replace(" ", "_");

        NbtCompound entityTag = new NbtCompound();
        if (target.get()) {
            Pos.add(NbtDouble.of(pos.getX()));
            Pos.add(NbtDouble.of(pos.getY()));
            Pos.add(NbtDouble.of(pos.getZ()));
            entityTag.put("Pos", Pos);
        } else {
            motion.add(NbtDouble.of(sex.x));
            motion.add(NbtDouble.of(sex.y));
            motion.add(NbtDouble.of(sex.z));
            entityTag.put("Motion", motion);
        }
        entityTag.putString("id", "minecraft:" + entityName);
        entityTag.putInt("Health", health.get());
        entityTag.putInt("AbsorptionAmount", absorption.get());
        entityTag.putInt("Age", age.get());
        entityTag.putInt("ExplosionPower", exppower.get());
        entityTag.putInt("ExplosionRadius", exppower.get());
        NbtCompound blockState = new NbtCompound();
        blockState.putString("Name", "minecraft:" + blockName);
        entityTag.put("BlockState", blockState);
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
        return NbtComponent.of(entityTag);
    }
}