package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerInteractEntityC2SPacket;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import pwn.noobs.trouserstreak.Trouser;

import java.util.Arrays;
import java.util.Collections;

public class MaceKill extends Module {
    private final SettingGroup specialGroup2 = settings.createGroup("Disable \"Smash Attack\" in the Criticals module to make this module work.");
    private final SettingGroup specialGroup = settings.createGroup("Values higher than 22 only work on Paper/Spigot");
    private final SettingGroup totem = settings.createGroup("Totem Bypass");

    private final Setting<Boolean> preventDeath = specialGroup.add(new BoolSetting.Builder()
            .name("Prevent Fall damage")
            .description("Attempts to prevent fall damage even on packet hiccups.")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> maxPower = specialGroup.add(new BoolSetting.Builder()
            .name("Maximum Mace Power (Paper/Spigot servers only)")
            .description("Simulates a fall from the highest air gap within 170 blocks")
            .defaultValue(false)
            .build());
    private final Setting<Integer> fallHeight = specialGroup.add(new IntSetting.Builder()
            .name("Mace Power (Fall height)")
            .description("Simulates a fall from this distance")
            .defaultValue(22)
            .sliderRange(1, 170)
            .min(1)
            .max(170)
            .visible(() -> !maxPower.get())
            .build());
    private final Setting<Boolean> packetDisable = specialGroup.add(new BoolSetting.Builder()
            .name("Disable When Blocked")
            .description("Does not send movement packets if the attack was blocked. (prevents death)")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> randomizeHeight = totem.add(new BoolSetting.Builder()
            .name("Randomize height")
            .description("Randomizes the fall height below the set limit.")
            .defaultValue(true)
            .build());
    private final Setting<Integer> deviation = totem.add(new IntSetting.Builder()
            .name("Height deviation")
            .description("Max blocks to subtract from max height (e.g. 40)")
            .defaultValue(16)
            .sliderRange(0, 169)
            .min(0)
            .max(169)
            .visible(() -> randomizeHeight.get())
            .build()
    );
    private final Setting<Boolean> attackFast = totem.add(new BoolSetting.Builder()
            .name("Attack fast")
            .description("Attacks very fast to increase the chance of bypass totem.")
            .defaultValue(false)
            .build());
    private final Setting<Integer> attackdelay = totem.add(new IntSetting.Builder()
            .name("Attack Delay")
            .description("This many ticks per attack.")
            .defaultValue(0)
            .sliderRange(0, 20)
            .min(0)
            .visible(() -> attackFast.get())
            .build()
    );
    private final Setting<Integer> attacks = totem.add(new IntSetting.Builder()
            .name("# of Attacks")
            .description("This many teleport attacks per delay.")
            .defaultValue(1)
            .sliderRange(1, 10)
            .min(0)
            .visible(() -> attackFast.get())
            .build()
    );
    private final Setting<Double> offsethorizontal = specialGroup.add(new DoubleSetting.Builder()
            .name("Horizontal Offset")
            .description("How much to offset the player after teleports.")
            .defaultValue(0.05)
            .min(0.01)
            .sliderMax(0.99)
            .build()
    );
    private final Setting<Double> offsetY = specialGroup.add(new DoubleSetting.Builder()
            .name("Y Offset")
            .description("How much to offset the player after teleports.")
            .defaultValue(0.01)
            .min(0.01)
            .sliderMax(0.99)
            .build()
    );

    public MaceKill() {
        super(Trouser.Main, "MaceKill", "Makes the Mace powerful when swung. Can also bypass totem usage.");
    }

    private Vec3d previouspos;
    private int attackDelay = 0;
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!attackFast.get() || mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;
        attackDelay++;
        HitResult target = mc.crosshairTarget;
        if (mc.options.attackKey.isPressed() && target instanceof EntityHitResult ehr && attackDelay>=attackdelay.get()){
            PlayerInteractEntityC2SPacket attack = PlayerInteractEntityC2SPacket.attack(ehr.getEntity(), mc.player.isSneaking());
            for (int i = 0; i < attacks.get(); i++) {
                mc.player.swingHand(Hand.MAIN_HAND);
                mc.player.networkHandler.sendPacket(attack);
            }
            attackDelay = 0;
        }
    }
    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (mc.player == null) return;
        if (mc.player.hasVehicle() || mc.player.getMainHandStack().getItem() != Items.MACE) return;
        if (!(event.packet instanceof IPlayerInteractEntityC2SPacket packet)) return;
        if (packet.meteor$getType() != PlayerInteractEntityC2SPacket.InteractType.ATTACK) return;

        if (!(packet.meteor$getEntity() instanceof LivingEntity)) return;
        LivingEntity targetEntity = (LivingEntity) packet.meteor$getEntity();
        if (packetDisable.get() && (targetEntity.isBlocking() || targetEntity.isInvulnerable() || targetEntity.isInCreativeMode()))
            return;

        previouspos = mc.player.getPos();
        int blocks = getMaxHeightAbovePlayer();
        int packetsRequired = (int) Math.ceil(Math.abs(blocks / 10.0));
        if (packetsRequired > 20) packetsRequired = 1;

        Vec3d targetPos = new Vec3d(mc.player.getX(), mc.player.getY() + blocks, mc.player.getZ());
        if (invalid(targetPos)) return;

        if (blocks <= 22) {
            for (int i = 0; i < 4; i++) {
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(false, mc.player.horizontalCollision));
            }
            double heightY = Math.min(mc.player.getY() + 22, mc.player.getY() + blocks);
            doPlayerTeleports(heightY);
        } else {
            for (int i = 0; i < packetsRequired - 1; i++) {
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(false, mc.player.horizontalCollision));
            }
            double heightY = mc.player.getY() + blocks;
            doPlayerTeleports(heightY);
        }
    }
    private void doPlayerTeleports(double height) {
        Vec3d offsetHome = getOffset(previouspos);
        PlayerMoveC2SPacket movepacket = new PlayerMoveC2SPacket.PositionAndOnGround(
                mc.player.getX(), height, mc.player.getZ(), false, mc.player.horizontalCollision);
        PlayerMoveC2SPacket homepacket = new PlayerMoveC2SPacket.PositionAndOnGround(
                offsetHome.getX(), offsetHome.getY(), offsetHome.getZ(),
                false, mc.player.horizontalCollision);
        if (preventDeath.get()) {
            mc.player.fallDistance = 0;
            homepacket = new PlayerMoveC2SPacket.PositionAndOnGround(
                    offsetHome.getX(), offsetHome.getY() + 0.0000000001, offsetHome.getZ(),
                    false, mc.player.horizontalCollision);
        }
        ((IPlayerMoveC2SPacket) homepacket).meteor$setTag(1337);
        ((IPlayerMoveC2SPacket) movepacket).meteor$setTag(1337);
        mc.player.networkHandler.sendPacket(movepacket);
        mc.player.networkHandler.sendPacket(homepacket);
    }
    private Vec3d getOffset(Vec3d base) {
        double dx = offsethorizontal.get();
        double dy = offsetY.get();

        Vec3d[] shuffledOffsets = new Vec3d[] {
                base.add( dx, dy,  0),
                base.add(-dx, dy,  0),
                base.add( 0, dy,  dx),
                base.add( 0, dy, -dx),
                base.add( dx, dy,  dx),
                base.add(-dx, dy,  -dx),
                base.add(-dx, dy,  dx),
                base.add(dx, dy,  -dx)
        };

        Collections.shuffle(Arrays.asList(shuffledOffsets));

        for (Vec3d pos : shuffledOffsets) {
            if (!invalid(pos)) return pos;
        }

        Vec3d noHorizontal = base.add(0, dy, 0);
        if (!invalid(noHorizontal)) return noHorizontal;

        return base;
    }
    private boolean invalid(Vec3d pos) {
        if (mc.world.getChunk(BlockPos.ofFloored(pos)) == null) return true;
        Entity entity = mc.player;
        Box box = entity.getBoundingBox().offset(
                pos.x - entity.getX(),
                pos.y - entity.getY(),
                pos.z - entity.getZ()
        );
        BlockPos blockPos = BlockPos.ofFloored(pos);
        for (int x = blockPos.getX() - 1; x <= blockPos.getX() + 1; x++) {
            for (int y = blockPos.getY() - 1; y <= blockPos.getY() + 1; y++) {
                for (int z = blockPos.getZ() - 1; z <= blockPos.getZ() + 1; z++) {
                    BlockPos checkPos = new BlockPos(x, y, z);
                    BlockState state = mc.world.getBlockState(checkPos);

                    if (state.isOf(Blocks.LAVA)) {
                        return true;
                    }
                }
            }
        }
        for (Entity e : mc.world.getOtherEntities(mc.player, box)) {
            if (e.isCollidable()) return true;
        }
        Vec3d delta = pos.subtract(entity.getPos());
        return mc.world.getBlockCollisions(entity, entity.getBoundingBox().offset(delta)).iterator().hasNext();
    }
    private int getMaxHeightAbovePlayer() {
        BlockPos playerPos = mc.player.getBlockPos();
        int maxHeight = playerPos.getY() + (maxPower.get() ? 170 : fallHeight.get());

        int scanStart = maxHeight;
        if (randomizeHeight.get()) {
            int randomSubtract = (int) (Math.random() * (deviation.get() + 1));
            scanStart = Math.max(playerPos.getY() + 1, maxHeight - randomSubtract);
        }

        for (int i = scanStart; i > playerPos.getY(); i--) {
            Vec3d testPos = new Vec3d(playerPos.getX(), playerPos.getY() + (i - playerPos.getY()), playerPos.getZ());
            if (!invalid(testPos)) return i - playerPos.getY();
        }
        return 0;
    }
}