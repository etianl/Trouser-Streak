package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import pwn.noobs.trouserstreak.Trouser;

public class ExplosionAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> power = sgGeneral.add(new IntSetting.Builder()
        .name("power")
        .description("how big explosion")
        .defaultValue(10)
        .min(1)
        .sliderMax(127)
        .build());

    public ExplosionAura() {
        super(Trouser.Main, "ExplosionAura", "You explode as you move. Must be in creative.");
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (mc.options.forwardKey.isPressed()) {
            ItemStack rst = mc.player.getMainHandStack();
            BlockHitResult bhr = new BlockHitResult(mc.player.getEyePos(), Direction.DOWN, new BlockPos(mc.player.getEyePos()), false);
            ItemStack Creeper = new ItemStack(Items.CREEPER_SPAWN_EGG);
            NbtCompound tag = new NbtCompound();
            tag.putInt("ignited", (1));
            tag.putInt("Fuse", (1));
            tag.putInt("Invulnerable", (1));
            tag.putInt("NoGravity", (1));
            tag.putInt("ExplosionRadius", power.get());
            Creeper.setSubNbt("EntityTag", tag);
            mc.interactionManager.clickCreativeStack(Creeper, 36 + mc.player.getInventory().selectedSlot);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
            mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
        }
        if (mc.options.jumpKey.isPressed()) {
            ItemStack rst = mc.player.getMainHandStack();
            BlockHitResult bhr = new BlockHitResult(mc.player.getEyePos(), Direction.DOWN, new BlockPos(mc.player.getEyePos()), false);
            ItemStack Creeper = new ItemStack(Items.CREEPER_SPAWN_EGG);
            NbtCompound tag = new NbtCompound();
            tag.putInt("ignited", (1));
            tag.putInt("Fuse", (1));
            tag.putInt("Invulnerable", (1));
            tag.putInt("NoGravity", (1));
            tag.putInt("ExplosionRadius", power.get());
            Creeper.setSubNbt("EntityTag", tag);
            mc.interactionManager.clickCreativeStack(Creeper, 36 + mc.player.getInventory().selectedSlot);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
            mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
        }
        if (mc.options.sneakKey.isPressed()) {
            ItemStack rst = mc.player.getMainHandStack();
            BlockHitResult bhr = new BlockHitResult(mc.player.getEyePos(), Direction.DOWN, new BlockPos(mc.player.getEyePos()), false);
            ItemStack Creeper = new ItemStack(Items.CREEPER_SPAWN_EGG);
            NbtCompound tag = new NbtCompound();
            tag.putInt("ignited", (1));
            tag.putInt("Fuse", (1));
            tag.putInt("Invulnerable", (1));
            tag.putInt("NoGravity", (1));
            tag.putInt("ExplosionRadius", power.get());
            Creeper.setSubNbt("EntityTag", tag);
            mc.interactionManager.clickCreativeStack(Creeper, 36 + mc.player.getInventory().selectedSlot);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
            mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
        }
        if (mc.options.leftKey.isPressed()) {
            ItemStack rst = mc.player.getMainHandStack();
            BlockHitResult bhr = new BlockHitResult(mc.player.getEyePos(), Direction.DOWN, new BlockPos(mc.player.getEyePos()), false);
            ItemStack Creeper = new ItemStack(Items.CREEPER_SPAWN_EGG);
            NbtCompound tag = new NbtCompound();
            tag.putInt("ignited", (1));
            tag.putInt("Fuse", (1));
            tag.putInt("Invulnerable", (1));
            tag.putInt("NoGravity", (1));
            tag.putInt("ExplosionRadius", power.get());
            Creeper.setSubNbt("EntityTag", tag);
            mc.interactionManager.clickCreativeStack(Creeper, 36 + mc.player.getInventory().selectedSlot);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
            mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
        }
        if (mc.options.rightKey.isPressed()) {
            ItemStack rst = mc.player.getMainHandStack();
            BlockHitResult bhr = new BlockHitResult(mc.player.getEyePos(), Direction.DOWN, new BlockPos(mc.player.getEyePos()), false);
            ItemStack Creeper = new ItemStack(Items.CREEPER_SPAWN_EGG);
            NbtCompound tag = new NbtCompound();
            tag.putInt("ignited", (1));
            tag.putInt("Fuse", (1));
            tag.putInt("Invulnerable", (1));
            tag.putInt("NoGravity", (1));
            tag.putInt("ExplosionRadius", power.get());
            Creeper.setSubNbt("EntityTag", tag);
            mc.interactionManager.clickCreativeStack(Creeper, 36 + mc.player.getInventory().selectedSlot);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
            mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
        }
        if (mc.options.backKey.isPressed()) {
            ItemStack rst = mc.player.getMainHandStack();
            BlockHitResult bhr = new BlockHitResult(mc.player.getEyePos(), Direction.DOWN, new BlockPos(mc.player.getEyePos()), false);
            ItemStack Creeper = new ItemStack(Items.CREEPER_SPAWN_EGG);
            NbtCompound tag = new NbtCompound();
            tag.putInt("ignited", (1));
            tag.putInt("Fuse", (1));
            tag.putInt("Invulnerable", (1));
            tag.putInt("NoGravity", (1));
            tag.putInt("ExplosionRadius", power.get());
            Creeper.setSubNbt("EntityTag", tag);
            mc.interactionManager.clickCreativeStack(Creeper, 36 + mc.player.getInventory().selectedSlot);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
            mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
        }
    }
}