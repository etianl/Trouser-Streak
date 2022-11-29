package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import pwn.noobs.trouserstreak.Trouser;

public class BoomPlus extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Modes> mode = sgGeneral.add(new EnumSetting.Builder<Modes>()
        .name("mode")
        .description("the mode")
        .defaultValue(Modes.Instant)
        .build());

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed")
        .description("fastness of thing")
        .defaultValue(10)
        .min(1)
        .sliderMax(10)
        .visible(() -> mode.get() != Modes.Lightning || mode.get() != Modes.Instant || mode.get() != Modes.Arrow || mode.get() == Modes.Creeper || mode.get() == Modes.TNT || mode.get() == Modes.WitherSkull || mode.get() == Modes.Spit || mode.get() == Modes.ShulkerBullet)
        .build());

    private final Setting<Integer> power = sgGeneral.add(new IntSetting.Builder()
        .name("power")
        .description("how big explosion")
        .defaultValue(10)
        .min(1)
        .sliderMax(127)
        .visible(() -> mode.get() == Modes.Instant || mode.get() == Modes.Motion || mode.get() == Modes.Creeper)
        .build());

    private final Setting<Integer> fuse = sgGeneral.add(new IntSetting.Builder()
            .name("Creeper/TNT fuse")
            .description("In ticks")
            .defaultValue(20)
            .sliderRange(0, 120)
            .visible(() -> mode.get() == Modes.TNT || mode.get() == Modes.Creeper)
            .build());
    public final Setting<Boolean> auto = sgGeneral.add(new BoolSetting.Builder()
            .name("FULLAUTO")
            .description("FULL AUTO BABY!")
            .defaultValue(false)
            .build()
    );

    public final Setting<Boolean> target = sgGeneral.add(new BoolSetting.Builder()
            .name("OnTarget")
            .description("spawns on target")
            .defaultValue(false)
            .build()
    );

    public BoomPlus() {
        super(Trouser.Main, "boom+", "shoots something where you click");
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (mc.player.getAbilities().creativeMode) {}
        else {
            error("You need to be in creative mode.");
            toggle();
        }
        if (auto.get() && mc.options.attackKey.isPressed() && mc.currentScreen == null && mc.player.getAbilities().creativeMode) {
            if (target.get()) {
                HitResult hr = mc.cameraEntity.raycast(600, 0, true);
                Vec3d owo = hr.getPos();
                BlockPos pos = new BlockPos(owo);
                ItemStack rst = mc.player.getMainHandStack();
                Vec3d sex = mc.player.getRotationVector().multiply(speed.get());
                BlockHitResult bhr = new BlockHitResult(mc.player.getEyePos(), Direction.DOWN, new BlockPos(mc.player.getEyePos()), false);
                switch (mode.get()) {
                    case Instant -> {
                        Vec3d aaa = mc.player.getRotationVector().multiply(100);
                        ItemStack Instant = new ItemStack(Items.SALMON_SPAWN_EGG);
                        NbtCompound tag = new NbtCompound();
                        NbtList Pos = new NbtList();
                        NbtList motion = new NbtList();
                        Pos.add(NbtDouble.of(pos.getX()));
                        Pos.add(NbtDouble.of(pos.getY()));
                        Pos.add(NbtDouble.of(pos.getZ()));
                        motion.add(NbtDouble.of(aaa.x));
                        motion.add(NbtDouble.of(aaa.y));
                        motion.add(NbtDouble.of(aaa.z));
                        tag.put("Pos", Pos);
                        tag.put("Motion", motion);
                        tag.putInt("ExplosionPower", power.get());
                        tag.putString("id", "minecraft:fireball");
                        Instant.setSubNbt("EntityTag", tag);
                        mc.interactionManager.clickCreativeStack(Instant, 36 + mc.player.getInventory().selectedSlot);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                        mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                    }
                    case Motion -> {
                        ItemStack Motion = new ItemStack(Items.SALMON_SPAWN_EGG);
                        NbtCompound tag = new NbtCompound();
                        NbtList motion = new NbtList();
                        motion.add(NbtDouble.of(sex.x));
                        motion.add(NbtDouble.of(sex.y));
                        motion.add(NbtDouble.of(sex.z));
                        tag.put("Motion", motion);
                        tag.putInt("ExplosionPower", power.get());
                        tag.putString("id", "minecraft:fireball");
                        Motion.setSubNbt("EntityTag", tag);
                        mc.interactionManager.clickCreativeStack(Motion, 36 + mc.player.getInventory().selectedSlot);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                        mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                    }
                    case Lightning -> {
                        ItemStack Lightning = new ItemStack(Items.SALMON_SPAWN_EGG);
                        NbtCompound tag = new NbtCompound();
                        NbtList Pos = new NbtList();
                        Pos.add(NbtDouble.of(pos.getX()));
                        Pos.add(NbtDouble.of(pos.getY()));
                        Pos.add(NbtDouble.of(pos.getZ()));
                        tag.put("Pos", Pos);
                        tag.putString("id", "minecraft:lightning_bolt");
                        Lightning.setSubNbt("EntityTag", tag);
                        mc.interactionManager.clickCreativeStack(Lightning, 36 + mc.player.getInventory().selectedSlot);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                        mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                    }
                    case Kitty -> {
                        ItemStack Kitty = new ItemStack(Items.CAT_SPAWN_EGG);
                        NbtCompound tag = new NbtCompound();
                        NbtList Pos = new NbtList();
                        Pos.add(NbtDouble.of(pos.getX()));
                        Pos.add(NbtDouble.of(pos.getY()));
                        Pos.add(NbtDouble.of(pos.getZ()));
                        tag.put("Pos", Pos);
                        Kitty.setSubNbt("EntityTag", tag);
                        mc.interactionManager.clickCreativeStack(Kitty, 36 + mc.player.getInventory().selectedSlot);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                        mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                    }
                    case Wither -> {
                        ItemStack Kitty = new ItemStack(Items.CAT_SPAWN_EGG);
                        NbtCompound tag = new NbtCompound();
                        NbtList Pos = new NbtList();
                        Pos.add(NbtDouble.of(pos.getX()));
                        Pos.add(NbtDouble.of(pos.getY()));
                        Pos.add(NbtDouble.of(pos.getZ()));
                        tag.put("Pos", Pos);
                        tag.putString("id", "minecraft:wither");
                        Kitty.setSubNbt("EntityTag", tag);
                        mc.interactionManager.clickCreativeStack(Kitty, 36 + mc.player.getInventory().selectedSlot);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                        mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                    }
                    case TNT -> {
                        ItemStack TNT = new ItemStack(Items.CAT_SPAWN_EGG);
                        NbtCompound tag = new NbtCompound();
                        NbtList Pos = new NbtList();
                        Pos.add(NbtDouble.of(pos.getX()));
                        Pos.add(NbtDouble.of(pos.getY()));
                        Pos.add(NbtDouble.of(pos.getZ()));
                        tag.put("Pos", Pos);
                        tag.putString("id", "minecraft:tnt");
                        tag.putInt("Fuse", (fuse.get()));
                        TNT.setSubNbt("EntityTag", tag);
                        mc.interactionManager.clickCreativeStack(TNT, 36 + mc.player.getInventory().selectedSlot);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                        mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                    }
                    case WitherSkull -> {
                        ItemStack WitherSkull = new ItemStack(Items.CAT_SPAWN_EGG);
                        NbtCompound tag = new NbtCompound();
                        NbtList Pos = new NbtList();
                        Pos.add(NbtDouble.of(pos.getX()));
                        Pos.add(NbtDouble.of(pos.getY()));
                        Pos.add(NbtDouble.of(pos.getZ()));
                        tag.put("Pos", Pos);
                        tag.putString("id", "minecraft:wither_skull");
                        WitherSkull.setSubNbt("EntityTag", tag);
                        mc.interactionManager.clickCreativeStack(WitherSkull, 36 + mc.player.getInventory().selectedSlot);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                        mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                    }
                    case Spit -> {
                        ItemStack Spit = new ItemStack(Items.CAT_SPAWN_EGG);
                        NbtCompound tag = new NbtCompound();
                        NbtList Pos = new NbtList();
                        Pos.add(NbtDouble.of(pos.getX()));
                        Pos.add(NbtDouble.of(pos.getY()));
                        Pos.add(NbtDouble.of(pos.getZ()));
                        tag.put("Pos", Pos);
                        tag.putString("id", "minecraft:llama_spit");
                        Spit.setSubNbt("EntityTag", tag);
                        mc.interactionManager.clickCreativeStack(Spit, 36 + mc.player.getInventory().selectedSlot);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                        mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                    }
                    case ShulkerBullet -> {
                        ItemStack ShulkerBullet = new ItemStack(Items.CAT_SPAWN_EGG);
                        NbtCompound tag = new NbtCompound();
                        NbtList Pos = new NbtList();
                        Pos.add(NbtDouble.of(pos.getX()));
                        Pos.add(NbtDouble.of(pos.getY()));
                        Pos.add(NbtDouble.of(pos.getZ()));
                        tag.put("Pos", Pos);
                        tag.putString("id", "minecraft:shulker_bullet");
                        ShulkerBullet.setSubNbt("EntityTag", tag);
                        mc.interactionManager.clickCreativeStack(ShulkerBullet, 36 + mc.player.getInventory().selectedSlot);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                        mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                    }
                    case Creeper -> {
                        ItemStack Creeper = new ItemStack(Items.CREEPER_SPAWN_EGG);
                        NbtCompound tag = new NbtCompound();
                        NbtList Pos = new NbtList();
                        Pos.add(NbtDouble.of(pos.getX()));
                        Pos.add(NbtDouble.of(pos.getY()));
                        Pos.add(NbtDouble.of(pos.getZ()));
                        tag.put("Pos", Pos);
                        tag.putInt("ignited", (1));
                        tag.putInt("Invulnerable", (1));
                        tag.putInt("Fuse", (fuse.get()));
                        tag.putInt("NoGravity", (1));
                        tag.putInt("ExplosionRadius", power.get());
                        Creeper.setSubNbt("EntityTag", tag);
                        mc.interactionManager.clickCreativeStack(Creeper, 36 + mc.player.getInventory().selectedSlot);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                        mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                    }
                    case Arrow -> {
                        ItemStack Arrow = new ItemStack(Items.SALMON_SPAWN_EGG);
                        NbtCompound tag = new NbtCompound();
                        NbtList Pos = new NbtList();
                        Pos.add(NbtDouble.of(pos.getX()));
                        Pos.add(NbtDouble.of(pos.getY()));
                        Pos.add(NbtDouble.of(pos.getZ()));
                        tag.put("Pos", Pos);
                        tag.putString("id", "minecraft:arrow");
                        Arrow.setSubNbt("EntityTag", tag);
                        mc.interactionManager.clickCreativeStack(Arrow, 36 + mc.player.getInventory().selectedSlot);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                        mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                    }
                }
            }
            else {
                HitResult hr = mc.cameraEntity.raycast(600, 0, true);
                Vec3d owo = hr.getPos();
                BlockPos pos = new BlockPos(owo);
                ItemStack rst = mc.player.getMainHandStack();
                Vec3d sex = mc.player.getRotationVector().multiply(speed.get());
                BlockHitResult bhr = new BlockHitResult(mc.player.getPos(), Direction.DOWN, new BlockPos(mc.player.getPos()), false);
                BlockHitResult bhr1 = new BlockHitResult(mc.player.getEyePos(), Direction.DOWN, new BlockPos(mc.player.getEyePos()), false);
                switch (mode.get()) {
                    case Instant -> {
                        Vec3d aaa = mc.player.getRotationVector().multiply(100);
                        ItemStack Instant = new ItemStack(Items.SALMON_SPAWN_EGG);
                        NbtCompound tag = new NbtCompound();
                        NbtList Pos = new NbtList();
                        NbtList motion = new NbtList();
                        Pos.add(NbtDouble.of(pos.getX()));
                        Pos.add(NbtDouble.of(pos.getY()));
                        Pos.add(NbtDouble.of(pos.getZ()));
                        motion.add(NbtDouble.of(aaa.x));
                        motion.add(NbtDouble.of(aaa.y));
                        motion.add(NbtDouble.of(aaa.z));
                        tag.put("Pos", Pos);
                        tag.put("Motion", motion);
                        tag.putInt("ExplosionPower", power.get());
                        tag.putString("id", "minecraft:fireball");
                        Instant.setSubNbt("EntityTag", tag);
                        mc.interactionManager.clickCreativeStack(Instant, 36 + mc.player.getInventory().selectedSlot);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                        mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                    }
                    case Motion -> {
                        ItemStack Motion = new ItemStack(Items.SALMON_SPAWN_EGG);
                        NbtCompound tag = new NbtCompound();
                        NbtList motion = new NbtList();
                        motion.add(NbtDouble.of(sex.x));
                        motion.add(NbtDouble.of(sex.y));
                        motion.add(NbtDouble.of(sex.z));
                        tag.put("Motion", motion);
                        tag.putInt("ExplosionPower", power.get());
                        tag.putString("id", "minecraft:fireball");
                        Motion.setSubNbt("EntityTag", tag);
                        mc.interactionManager.clickCreativeStack(Motion, 36 + mc.player.getInventory().selectedSlot);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                        mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                    }
                    case Lightning -> {
                        ItemStack Lightning = new ItemStack(Items.SALMON_SPAWN_EGG);
                        NbtCompound tag = new NbtCompound();
                        NbtList Pos = new NbtList();
                        Pos.add(NbtDouble.of(pos.getX()));
                        Pos.add(NbtDouble.of(pos.getY()));
                        Pos.add(NbtDouble.of(pos.getZ()));
                        tag.put("Pos", Pos);
                        tag.putString("id", "minecraft:lightning_bolt");
                        Lightning.setSubNbt("EntityTag", tag);
                        mc.interactionManager.clickCreativeStack(Lightning, 36 + mc.player.getInventory().selectedSlot);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                        mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                    }
                    case Kitty -> {
                        ItemStack Kitty = new ItemStack(Items.CAT_SPAWN_EGG);
                        NbtCompound tag = new NbtCompound();
                        NbtList motion = new NbtList();
                        motion.add(NbtDouble.of(sex.x));
                        motion.add(NbtDouble.of(sex.y));
                        motion.add(NbtDouble.of(sex.z));
                        tag.put("Motion", motion);
                        Kitty.setSubNbt("EntityTag", tag);
                        mc.interactionManager.clickCreativeStack(Kitty, 36 + mc.player.getInventory().selectedSlot);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                        mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                    }
                    case Wither -> {
                        ItemStack Kitty = new ItemStack(Items.CAT_SPAWN_EGG);
                        NbtCompound tag = new NbtCompound();
                        NbtList motion = new NbtList();
                        motion.add(NbtDouble.of(sex.x));
                        motion.add(NbtDouble.of(sex.y));
                        motion.add(NbtDouble.of(sex.z));
                        tag.put("Motion", motion);
                        tag.putString("id", "minecraft:wither");
                        Kitty.setSubNbt("EntityTag", tag);
                        mc.interactionManager.clickCreativeStack(Kitty, 36 + mc.player.getInventory().selectedSlot);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                        mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                    }
                    case TNT -> {
                        ItemStack TNT = new ItemStack(Items.CAT_SPAWN_EGG);
                        NbtCompound tag = new NbtCompound();
                        NbtList motion = new NbtList();
                        motion.add(NbtDouble.of(sex.x));
                        motion.add(NbtDouble.of(sex.y));
                        motion.add(NbtDouble.of(sex.z));
                        tag.put("Motion", motion);
                        tag.putString("id", "minecraft:tnt");
                        tag.putInt("Fuse", (fuse.get()));
                        TNT.setSubNbt("EntityTag", tag);
                        mc.interactionManager.clickCreativeStack(TNT, 36 + mc.player.getInventory().selectedSlot);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                        mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                    }
                    case WitherSkull -> {
                        ItemStack WitherSkull = new ItemStack(Items.CAT_SPAWN_EGG);
                        NbtCompound tag = new NbtCompound();
                        NbtList motion = new NbtList();
                        motion.add(NbtDouble.of(sex.x));
                        motion.add(NbtDouble.of(sex.y));
                        motion.add(NbtDouble.of(sex.z));
                        tag.put("Motion", motion);
                        tag.putString("id", "minecraft:wither_skull");
                        WitherSkull.setSubNbt("EntityTag", tag);
                        mc.interactionManager.clickCreativeStack(WitherSkull, 36 + mc.player.getInventory().selectedSlot);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr1);
                        mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                    }
                    case Spit -> {
                        ItemStack Spit = new ItemStack(Items.CAT_SPAWN_EGG);
                        NbtCompound tag = new NbtCompound();
                        NbtList motion = new NbtList();
                        motion.add(NbtDouble.of(sex.x));
                        motion.add(NbtDouble.of(sex.y));
                        motion.add(NbtDouble.of(sex.z));
                        tag.put("Motion", motion);
                        tag.putString("id", "minecraft:llama_spit");
                        Spit.setSubNbt("EntityTag", tag);
                        mc.interactionManager.clickCreativeStack(Spit, 36 + mc.player.getInventory().selectedSlot);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                        mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                    }
                    case ShulkerBullet -> {
                        ItemStack ShulkerBullet = new ItemStack(Items.CAT_SPAWN_EGG);
                        NbtCompound tag = new NbtCompound();
                        NbtList motion = new NbtList();
                        motion.add(NbtDouble.of(sex.x));
                        motion.add(NbtDouble.of(sex.y));
                        motion.add(NbtDouble.of(sex.z));
                        tag.put("Motion", motion);
                        tag.putString("id", "minecraft:shulker_bullet");
                        ShulkerBullet.setSubNbt("EntityTag", tag);
                        mc.interactionManager.clickCreativeStack(ShulkerBullet, 36 + mc.player.getInventory().selectedSlot);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr1);
                        mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                    }
                    case Creeper -> {
                        ItemStack Creeper = new ItemStack(Items.CREEPER_SPAWN_EGG);
                        NbtCompound tag = new NbtCompound();
                        NbtList motion = new NbtList();
                        motion.add(NbtDouble.of(sex.x));
                        motion.add(NbtDouble.of(sex.y));
                        motion.add(NbtDouble.of(sex.z));
                        tag.put("Motion", motion);
                        tag.putInt("ignited", (1));
                        tag.putInt("Invulnerable", (1));
                        tag.putInt("Fuse", (fuse.get()));
                        tag.putInt("ExplosionRadius", power.get());
                        Creeper.setSubNbt("EntityTag", tag);
                        mc.interactionManager.clickCreativeStack(Creeper, 36 + mc.player.getInventory().selectedSlot);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                        mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                    }
                    case Arrow -> {
                        ItemStack Arrow = new ItemStack(Items.SALMON_SPAWN_EGG);
                        NbtCompound tag = new NbtCompound();
                        NbtList speed = new NbtList();
                        speed.add(NbtDouble.of(sex.x));
                        speed.add(NbtDouble.of(sex.y));
                        speed.add(NbtDouble.of(sex.z));
                        tag.put("Motion", speed);
                        tag.putString("id", "minecraft:arrow");
                        Arrow.setSubNbt("EntityTag", tag);
                        mc.interactionManager.clickCreativeStack(Arrow, 36 + mc.player.getInventory().selectedSlot);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr1);
                        mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                    }
                }
            }
        }
    }

    @EventHandler
    private void onMouseButton(MouseButtonEvent event) {
        if (mc.options.attackKey.isPressed() && mc.currentScreen == null && mc.player.getAbilities().creativeMode) {
            if (target.get()) {
                HitResult hr = mc.cameraEntity.raycast(600, 0, true);
                Vec3d owo = hr.getPos();
                BlockPos pos = new BlockPos(owo);
                ItemStack rst = mc.player.getMainHandStack();
                Vec3d sex = mc.player.getRotationVector().multiply(speed.get());
                BlockHitResult bhr = new BlockHitResult(mc.player.getEyePos(), Direction.DOWN, new BlockPos(mc.player.getEyePos()), false);
                switch (mode.get()) {
                    case Instant -> {
                        Vec3d aaa = mc.player.getRotationVector().multiply(100);
                        ItemStack Instant = new ItemStack(Items.SALMON_SPAWN_EGG);
                        NbtCompound tag = new NbtCompound();
                        NbtList Pos = new NbtList();
                        NbtList motion = new NbtList();
                        Pos.add(NbtDouble.of(pos.getX()));
                        Pos.add(NbtDouble.of(pos.getY()));
                        Pos.add(NbtDouble.of(pos.getZ()));
                        motion.add(NbtDouble.of(aaa.x));
                        motion.add(NbtDouble.of(aaa.y));
                        motion.add(NbtDouble.of(aaa.z));
                        tag.put("Pos", Pos);
                        tag.put("Motion", motion);
                        tag.putInt("ExplosionPower", power.get());
                        tag.putString("id", "minecraft:fireball");
                        Instant.setSubNbt("EntityTag", tag);
                        mc.interactionManager.clickCreativeStack(Instant, 36 + mc.player.getInventory().selectedSlot);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                        mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                    }
                    case Motion -> {
                        ItemStack Motion = new ItemStack(Items.SALMON_SPAWN_EGG);
                        NbtCompound tag = new NbtCompound();
                        NbtList motion = new NbtList();
                        motion.add(NbtDouble.of(sex.x));
                        motion.add(NbtDouble.of(sex.y));
                        motion.add(NbtDouble.of(sex.z));
                        tag.put("Motion", motion);
                        tag.putInt("ExplosionPower", power.get());
                        tag.putString("id", "minecraft:fireball");
                        Motion.setSubNbt("EntityTag", tag);
                        mc.interactionManager.clickCreativeStack(Motion, 36 + mc.player.getInventory().selectedSlot);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                        mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                    }
                    case Lightning -> {
                        ItemStack Lightning = new ItemStack(Items.SALMON_SPAWN_EGG);
                        NbtCompound tag = new NbtCompound();
                        NbtList Pos = new NbtList();
                        Pos.add(NbtDouble.of(pos.getX()));
                        Pos.add(NbtDouble.of(pos.getY()));
                        Pos.add(NbtDouble.of(pos.getZ()));
                        tag.put("Pos", Pos);
                        tag.putString("id", "minecraft:lightning_bolt");
                        Lightning.setSubNbt("EntityTag", tag);
                        mc.interactionManager.clickCreativeStack(Lightning, 36 + mc.player.getInventory().selectedSlot);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                        mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                    }
                    case Kitty -> {
                        ItemStack Kitty = new ItemStack(Items.CAT_SPAWN_EGG);
                        NbtCompound tag = new NbtCompound();
                        NbtList Pos = new NbtList();
                        Pos.add(NbtDouble.of(pos.getX()));
                        Pos.add(NbtDouble.of(pos.getY()));
                        Pos.add(NbtDouble.of(pos.getZ()));
                        tag.put("Pos", Pos);
                        Kitty.setSubNbt("EntityTag", tag);
                        mc.interactionManager.clickCreativeStack(Kitty, 36 + mc.player.getInventory().selectedSlot);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                        mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                    }
                    case Wither -> {
                        ItemStack Kitty = new ItemStack(Items.CAT_SPAWN_EGG);
                        NbtCompound tag = new NbtCompound();
                        NbtList Pos = new NbtList();
                        Pos.add(NbtDouble.of(pos.getX()));
                        Pos.add(NbtDouble.of(pos.getY()));
                        Pos.add(NbtDouble.of(pos.getZ()));
                        tag.put("Pos", Pos);
                        tag.putString("id", "minecraft:wither");
                        Kitty.setSubNbt("EntityTag", tag);
                        mc.interactionManager.clickCreativeStack(Kitty, 36 + mc.player.getInventory().selectedSlot);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                        mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                    }
                    case TNT -> {
                        ItemStack TNT = new ItemStack(Items.CAT_SPAWN_EGG);
                        NbtCompound tag = new NbtCompound();
                        NbtList Pos = new NbtList();
                        Pos.add(NbtDouble.of(pos.getX()));
                        Pos.add(NbtDouble.of(pos.getY()));
                        Pos.add(NbtDouble.of(pos.getZ()));
                        tag.put("Pos", Pos);
                        tag.putString("id", "minecraft:tnt");
                        tag.putInt("Fuse", (fuse.get()));
                        TNT.setSubNbt("EntityTag", tag);
                        mc.interactionManager.clickCreativeStack(TNT, 36 + mc.player.getInventory().selectedSlot);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                        mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                    }
                    case WitherSkull -> {
                        ItemStack WitherSkull = new ItemStack(Items.CAT_SPAWN_EGG);
                        NbtCompound tag = new NbtCompound();
                        NbtList Pos = new NbtList();
                        Pos.add(NbtDouble.of(pos.getX()));
                        Pos.add(NbtDouble.of(pos.getY()));
                        Pos.add(NbtDouble.of(pos.getZ()));
                        tag.put("Pos", Pos);
                        tag.putString("id", "minecraft:wither_skull");
                        WitherSkull.setSubNbt("EntityTag", tag);
                        mc.interactionManager.clickCreativeStack(WitherSkull, 36 + mc.player.getInventory().selectedSlot);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                        mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                    }
                    case Spit -> {
                        ItemStack Spit = new ItemStack(Items.CAT_SPAWN_EGG);
                        NbtCompound tag = new NbtCompound();
                        NbtList Pos = new NbtList();
                        Pos.add(NbtDouble.of(pos.getX()));
                        Pos.add(NbtDouble.of(pos.getY()));
                        Pos.add(NbtDouble.of(pos.getZ()));
                        tag.put("Pos", Pos);
                        tag.putString("id", "minecraft:llama_spit");
                        Spit.setSubNbt("EntityTag", tag);
                        mc.interactionManager.clickCreativeStack(Spit, 36 + mc.player.getInventory().selectedSlot);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                        mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                    }
                    case ShulkerBullet -> {
                        ItemStack ShulkerBullet = new ItemStack(Items.CAT_SPAWN_EGG);
                        NbtCompound tag = new NbtCompound();
                        NbtList Pos = new NbtList();
                        Pos.add(NbtDouble.of(pos.getX()));
                        Pos.add(NbtDouble.of(pos.getY()));
                        Pos.add(NbtDouble.of(pos.getZ()));
                        tag.put("Pos", Pos);
                        tag.putString("id", "minecraft:shulker_bullet");
                        ShulkerBullet.setSubNbt("EntityTag", tag);
                        mc.interactionManager.clickCreativeStack(ShulkerBullet, 36 + mc.player.getInventory().selectedSlot);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                        mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                    }
                    case Creeper -> {
                        ItemStack Creeper = new ItemStack(Items.CREEPER_SPAWN_EGG);
                        NbtCompound tag = new NbtCompound();
                        NbtList Pos = new NbtList();
                        Pos.add(NbtDouble.of(pos.getX()));
                        Pos.add(NbtDouble.of(pos.getY()));
                        Pos.add(NbtDouble.of(pos.getZ()));
                        tag.put("Pos", Pos);
                        tag.putInt("ignited", (1));
                        tag.putInt("Invulnerable", (1));
                        tag.putInt("Fuse", (fuse.get()));
                        tag.putInt("NoGravity", (1));
                        tag.putInt("ExplosionRadius", power.get());
                        Creeper.setSubNbt("EntityTag", tag);
                        mc.interactionManager.clickCreativeStack(Creeper, 36 + mc.player.getInventory().selectedSlot);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                        mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                    }
                    case Arrow -> {
                        ItemStack Arrow = new ItemStack(Items.SALMON_SPAWN_EGG);
                        NbtCompound tag = new NbtCompound();
                        NbtList Pos = new NbtList();
                        Pos.add(NbtDouble.of(pos.getX()));
                        Pos.add(NbtDouble.of(pos.getY()));
                        Pos.add(NbtDouble.of(pos.getZ()));
                        tag.put("Pos", Pos);
                        tag.putString("id", "minecraft:arrow");
                        Arrow.setSubNbt("EntityTag", tag);
                        mc.interactionManager.clickCreativeStack(Arrow, 36 + mc.player.getInventory().selectedSlot);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                        mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                    }
                }
            }
            else {
            HitResult hr = mc.cameraEntity.raycast(600, 0, true);
            Vec3d owo = hr.getPos();
            BlockPos pos = new BlockPos(owo);
            ItemStack rst = mc.player.getMainHandStack();
            Vec3d sex = mc.player.getRotationVector().multiply(speed.get());
            BlockHitResult bhr = new BlockHitResult(mc.player.getPos(), Direction.DOWN, new BlockPos(mc.player.getPos()), false);
                BlockHitResult bhr1 = new BlockHitResult(mc.player.getEyePos(), Direction.DOWN, new BlockPos(mc.player.getEyePos()), false);
            switch (mode.get()) {
                case Instant -> {
                    Vec3d aaa = mc.player.getRotationVector().multiply(100);
                    ItemStack Instant = new ItemStack(Items.SALMON_SPAWN_EGG);
                    NbtCompound tag = new NbtCompound();
                    NbtList Pos = new NbtList();
                    NbtList motion = new NbtList();
                    Pos.add(NbtDouble.of(pos.getX()));
                    Pos.add(NbtDouble.of(pos.getY()));
                    Pos.add(NbtDouble.of(pos.getZ()));
                    motion.add(NbtDouble.of(aaa.x));
                    motion.add(NbtDouble.of(aaa.y));
                    motion.add(NbtDouble.of(aaa.z));
                    tag.put("Pos", Pos);
                    tag.put("Motion", motion);
                    tag.putInt("ExplosionPower", power.get());
                    tag.putString("id", "minecraft:fireball");
                    Instant.setSubNbt("EntityTag", tag);
                    mc.interactionManager.clickCreativeStack(Instant, 36 + mc.player.getInventory().selectedSlot);
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                    mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                }
                case Motion -> {
                    ItemStack Motion = new ItemStack(Items.SALMON_SPAWN_EGG);
                    NbtCompound tag = new NbtCompound();
                    NbtList motion = new NbtList();
                    motion.add(NbtDouble.of(sex.x));
                    motion.add(NbtDouble.of(sex.y));
                    motion.add(NbtDouble.of(sex.z));
                    tag.put("Motion", motion);
                    tag.putInt("ExplosionPower", power.get());
                    tag.putString("id", "minecraft:fireball");
                    Motion.setSubNbt("EntityTag", tag);
                    mc.interactionManager.clickCreativeStack(Motion, 36 + mc.player.getInventory().selectedSlot);
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                    mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                }
                case Lightning -> {
                    ItemStack Lightning = new ItemStack(Items.SALMON_SPAWN_EGG);
                    NbtCompound tag = new NbtCompound();
                    NbtList Pos = new NbtList();
                    Pos.add(NbtDouble.of(pos.getX()));
                    Pos.add(NbtDouble.of(pos.getY()));
                    Pos.add(NbtDouble.of(pos.getZ()));
                    tag.put("Pos", Pos);
                    tag.putString("id", "minecraft:lightning_bolt");
                    Lightning.setSubNbt("EntityTag", tag);
                    mc.interactionManager.clickCreativeStack(Lightning, 36 + mc.player.getInventory().selectedSlot);
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                    mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                }
                case Kitty -> {
                    ItemStack Kitty = new ItemStack(Items.CAT_SPAWN_EGG);
                    NbtCompound tag = new NbtCompound();
                    NbtList motion = new NbtList();
                    motion.add(NbtDouble.of(sex.x));
                    motion.add(NbtDouble.of(sex.y));
                    motion.add(NbtDouble.of(sex.z));
                    tag.put("Motion", motion);
                    Kitty.setSubNbt("EntityTag", tag);
                    mc.interactionManager.clickCreativeStack(Kitty, 36 + mc.player.getInventory().selectedSlot);
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                    mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                }
                case Wither -> {
                    ItemStack Kitty = new ItemStack(Items.CAT_SPAWN_EGG);
                    NbtCompound tag = new NbtCompound();
                    NbtList motion = new NbtList();
                    motion.add(NbtDouble.of(sex.x));
                    motion.add(NbtDouble.of(sex.y));
                    motion.add(NbtDouble.of(sex.z));
                    tag.put("Motion", motion);
                    tag.putString("id", "minecraft:wither");
                    Kitty.setSubNbt("EntityTag", tag);
                    mc.interactionManager.clickCreativeStack(Kitty, 36 + mc.player.getInventory().selectedSlot);
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                    mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                }
                case TNT -> {
                    ItemStack TNT = new ItemStack(Items.CAT_SPAWN_EGG);
                    NbtCompound tag = new NbtCompound();
                    NbtList motion = new NbtList();
                    motion.add(NbtDouble.of(sex.x));
                    motion.add(NbtDouble.of(sex.y));
                    motion.add(NbtDouble.of(sex.z));
                    tag.put("Motion", motion);
                    tag.putString("id", "minecraft:tnt");
                    tag.putInt("Fuse", (fuse.get()));
                    TNT.setSubNbt("EntityTag", tag);
                    mc.interactionManager.clickCreativeStack(TNT, 36 + mc.player.getInventory().selectedSlot);
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                    mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                }
                case WitherSkull -> {
                    ItemStack WitherSkull = new ItemStack(Items.CAT_SPAWN_EGG);
                    NbtCompound tag = new NbtCompound();
                    NbtList motion = new NbtList();
                    motion.add(NbtDouble.of(sex.x));
                    motion.add(NbtDouble.of(sex.y));
                    motion.add(NbtDouble.of(sex.z));
                    tag.put("Motion", motion);
                    tag.putString("id", "minecraft:wither_skull");
                    WitherSkull.setSubNbt("EntityTag", tag);
                    mc.interactionManager.clickCreativeStack(WitherSkull, 36 + mc.player.getInventory().selectedSlot);
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr1);
                    mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                }
                case Spit -> {
                    ItemStack Spit = new ItemStack(Items.CAT_SPAWN_EGG);
                    NbtCompound tag = new NbtCompound();
                    NbtList motion = new NbtList();
                    motion.add(NbtDouble.of(sex.x));
                    motion.add(NbtDouble.of(sex.y));
                    motion.add(NbtDouble.of(sex.z));
                    tag.put("Motion", motion);
                    tag.putString("id", "minecraft:llama_spit");
                    Spit.setSubNbt("EntityTag", tag);
                    mc.interactionManager.clickCreativeStack(Spit, 36 + mc.player.getInventory().selectedSlot);
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                    mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                }
                case ShulkerBullet -> {
                    ItemStack ShulkerBullet = new ItemStack(Items.CAT_SPAWN_EGG);
                    NbtCompound tag = new NbtCompound();
                    NbtList motion = new NbtList();
                    motion.add(NbtDouble.of(sex.x));
                    motion.add(NbtDouble.of(sex.y));
                    motion.add(NbtDouble.of(sex.z));
                    tag.put("Motion", motion);
                    tag.putString("id", "minecraft:shulker_bullet");
                    ShulkerBullet.setSubNbt("EntityTag", tag);
                    mc.interactionManager.clickCreativeStack(ShulkerBullet, 36 + mc.player.getInventory().selectedSlot);
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr1);
                    mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                }
                case Creeper -> {
                    ItemStack Creeper = new ItemStack(Items.CREEPER_SPAWN_EGG);
                    NbtCompound tag = new NbtCompound();
                    NbtList motion = new NbtList();
                    motion.add(NbtDouble.of(sex.x));
                    motion.add(NbtDouble.of(sex.y));
                    motion.add(NbtDouble.of(sex.z));
                    tag.put("Motion", motion);
                    tag.putInt("ignited", (1));
                    tag.putInt("Invulnerable", (1));
                    tag.putInt("Fuse", (fuse.get()));
                    tag.putInt("ExplosionRadius", power.get());
                    Creeper.setSubNbt("EntityTag", tag);
                    mc.interactionManager.clickCreativeStack(Creeper, 36 + mc.player.getInventory().selectedSlot);
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                    mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                }
                case Arrow -> {
                    ItemStack Arrow = new ItemStack(Items.SALMON_SPAWN_EGG);
                    NbtCompound tag = new NbtCompound();
                    NbtList speed = new NbtList();
                    speed.add(NbtDouble.of(sex.x));
                    speed.add(NbtDouble.of(sex.y));
                    speed.add(NbtDouble.of(sex.z));
                    tag.put("Motion", speed);
                    tag.putString("id", "minecraft:arrow");
                    Arrow.setSubNbt("EntityTag", tag);
                    mc.interactionManager.clickCreativeStack(Arrow, 36 + mc.player.getInventory().selectedSlot);
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr1);
                    mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                }
            }
        }}
    }
    public enum Modes {
        Instant, Motion, Lightning, Kitty, Creeper, Arrow, TNT, WitherSkull, Spit, ShulkerBullet, Wither
    }
}
