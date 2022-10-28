package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.utils.player.ChatUtils;
import pwn.noobs.trouserstreak.Trouser;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;


public class FireballClicker extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Modes> mode = sgGeneral.add(new EnumSetting.Builder<Modes>()
        .name("mode")
        .description("Fireball mode.")
        .defaultValue(Modes.Motion)
        .build());

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("fireball-speed")
        .description("How fast the fireballs should be.")
        .defaultValue(10)
        .min(1)
        .sliderMax(10)
        .build());

    private final Setting<Integer> power = sgGeneral.add(new IntSetting.Builder()
        .name("fireball-power")
        .description("How powerful the fireball should be.")
        .defaultValue(10)
        .min(1)
        .sliderMax(127)
        .visible(() -> mode.get() == Modes.Motion)
        .build());

    public final Setting<Boolean> larp = sgGeneral.add(new BoolSetting.Builder()
            .name("LARP")
            .description("LARP on/off")
            .defaultValue(true)
            .build()
    );

    public FireballClicker() {
        super(Trouser.Main, "Fireball", "Shoots a fireball at where you're clicking.");
    }

    @EventHandler
    private void onMouseButton(MouseButtonEvent event) {
        if (mc.options.attackKey.isPressed() && mc.currentScreen == null) {
            if (larp.get()) {
                ChatUtils.sendPlayerMsg("Fireball!");
            }
            if (mc.player.getAbilities().creativeMode) {
                HitResult hr = mc.cameraEntity.raycast(300, 0, true);
                Vec3d owo = hr.getPos();
                BlockPos pos = new BlockPos(owo);
                ItemStack rst = mc.player.getMainHandStack();
                Vec3d sex = mc.player.getRotationVector().multiply(speed.get());
                BlockHitResult bhr = new BlockHitResult(mc.player.getEyePos(), Direction.DOWN, new BlockPos(mc.player.getEyePos()), false);
                switch (mode.get()) {
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

                }
            } else {
                error("You need to be in creative mode.");
                toggle();
            }
        }
    }
    public enum Modes {
        Motion
    }
}
