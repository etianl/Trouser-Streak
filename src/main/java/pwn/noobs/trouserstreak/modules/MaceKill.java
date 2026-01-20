package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
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
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import pwn.noobs.trouserstreak.Trouser;

import java.util.Arrays;
import java.util.Collections;

public class MaceKill extends Module {
    private final SettingGroup specialGroup2 = settings.createGroup("Disable \"Smash Attack\" in Criticals to make this module work.");
    private final SettingGroup specialGroup = settings.createGroup("Values higher than 22fall/4spam only work on Paper/Spigot");
    private final SettingGroup totem = settings.createGroup("Totem Bypass (PAPER ONLY)");
    private final Setting<Boolean> swing = specialGroup.add(
            new BoolSetting.Builder()
                    .name("swing arm")
                    .defaultValue(true)
                    .build()
    );
    private final Setting<Boolean> preventDeath = specialGroup.add(new BoolSetting.Builder()
            .name("Prevent Fall damage")
            .description("Attempts to prevent fall damage even on packet hiccups.")
            .defaultValue(true)
            .build());
    private final Setting<Integer> fallHeight = specialGroup.add(new IntSetting.Builder()
            .name("Fall height")
            .description("Simulates a fall from this distance")
            .defaultValue(22)
            .sliderRange(1, 169)
            .min(1)
            .max(169)
            .build());
    private final Setting<Integer> paperpackets = specialGroup.add(new IntSetting.Builder()
            .name("# spam packets")
            .description("4 required for max vanilla teleport of 22 blocks. 10 blocks distance per packet allowed in Paper.")
            .defaultValue(4)
            .min(1)
            .sliderRange(1,17)
            .build()
    );
    private final Setting<Boolean> packetDisable = specialGroup.add(new BoolSetting.Builder()
            .name("Disable When Blocked")
            .description("Does not send movement packets if the attack was blocked. (prevents death)")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> attackSpam = totem.add(new BoolSetting.Builder()
            .name("Bypass totems")
            .description("Max ~9 spam packets for this to work. Settings example: 49 fall height, 8 spam packet, 3 attack, 20 height increase.")
            .defaultValue(false)
            .build());
    private final Setting<Integer> attacks = totem.add(new IntSetting.Builder()
            .name("# of Attacks")
            .description("This many attacks.")
            .defaultValue(3)
            .sliderRange(1, 3)
            .min(0)
            .build()
    );
    private final Setting<Integer> increase = totem.add(new IntSetting.Builder()
            .name("Height Increase")
            .description("Blocks distance to increase from the last attack")
            .defaultValue(9)
            .sliderRange(1, 100)
            .min(1)
            .max(100)
            .build()
    );

    private final Setting<Double> offsethorizontal = specialGroup.add(new DoubleSetting.Builder()
            .name("Horizontal Offset")
            .description("How much to offset the player after teleports.")
            .defaultValue(0.05)
            .min(0.001)
            .sliderMax(0.99)
            .build()
    );
    private final Setting<Double> offsetY = specialGroup.add(new DoubleSetting.Builder()
            .name("Y Offset")
            .description("How much to offset the player after teleports.")
            .defaultValue(0.01)
            .min(0.001)
            .sliderMax(0.99)
            .build()
    );

    public MaceKill() {
        super(Trouser.Main, "MaceKill", "Makes the Mace powerful when swung. Can also bypass totem usage.");
    }

    private Vec3d previouspos;
    private boolean sendingAttacks = false;
    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (sendingAttacks) return;
        if (mc.player == null) return;
        if (mc.player.hasVehicle() || mc.player.getMainHandStack().getItem() != Items.MACE) return;
        if (!(event.packet instanceof IPlayerInteractEntityC2SPacket packet)) return;
        if (packet.meteor$getType() != PlayerInteractEntityC2SPacket.InteractType.ATTACK) return;

        if (!(packet.meteor$getEntity() instanceof LivingEntity)) return;
        LivingEntity targetEntity = (LivingEntity) packet.meteor$getEntity();
        if (packetDisable.get() && (targetEntity.isBlocking() || targetEntity.isInvulnerable() || targetEntity.isInCreativeMode())) return;
        if (!targetEntity.isAlive()) return;
        event.cancel();

        previouspos = mc.player.getEntityPos();
        PlayerInteractEntityC2SPacket attack = PlayerInteractEntityC2SPacket.attack(targetEntity, mc.player.isSneaking());

        int baseBlocks = getMaxHeightAbovePlayer();
        int currentHeight = baseBlocks;
        int attackCount = attacks.get();
        if (!attackSpam.get()) attackCount = 1;
        Vec3d firstTargetPos = new Vec3d(mc.player.getX(), mc.player.getY() + baseBlocks, mc.player.getZ());
        if (invalid(firstTargetPos)) return;
        for (int i2 = 0; i2 < paperpackets.get(); i2++) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(false, mc.player.horizontalCollision));
        }
        for (int i = 0; i < attackCount; i++) {
            int blocks = (i == 0) ? baseBlocks : currentHeight;
            Vec3d targetPos = new Vec3d(mc.player.getX(), mc.player.getY() + blocks, mc.player.getZ());
            if (invalid(targetPos)) continue;

            PlayerMoveC2SPacket movepacket = new PlayerMoveC2SPacket.PositionAndOnGround(targetPos.getX(), targetPos.getY(), targetPos.getZ(), false, mc.player.horizontalCollision);
            PlayerMoveC2SPacket homepacket = new PlayerMoveC2SPacket.PositionAndOnGround(previouspos.getX(), previouspos.getY(), previouspos.getZ(), false, mc.player.horizontalCollision);
            ((IPlayerMoveC2SPacket) homepacket).meteor$setTag(1337);
            ((IPlayerMoveC2SPacket) movepacket).meteor$setTag(1337);
            mc.player.networkHandler.sendPacket(movepacket);
            mc.player.networkHandler.sendPacket(homepacket);
            mc.player.setPosition(previouspos);

            sendingAttacks = true;
            if (swing.get()) {
                mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                mc.player.swingHand(Hand.MAIN_HAND);
            }
            mc.player.networkHandler.sendPacket(attack);
            sendingAttacks = false;

            currentHeight += increase.get();
        }

        if (preventDeath.get()){
            Vec3d offsetHome = getOffset(previouspos);
            PlayerMoveC2SPacket offsethomepacket = new PlayerMoveC2SPacket.PositionAndOnGround(offsetHome.getX(), offsetHome.getY(), offsetHome.getZ(), false, mc.player.horizontalCollision);
            ((IPlayerMoveC2SPacket) offsethomepacket).meteor$setTag(1337);
            mc.player.networkHandler.sendPacket(offsethomepacket);
            mc.player.setPosition(offsetHome);
        }
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
            if (e.isCollidable(entity)) return true;
        }
        Vec3d delta = pos.subtract(entity.getEntityPos());
        return mc.world.getBlockCollisions(entity, entity.getBoundingBox().offset(delta)).iterator().hasNext();
    }
    private int getMaxHeightAbovePlayer() {
        BlockPos playerPos = mc.player.getBlockPos();

        int scanStart = playerPos.getY() + fallHeight.get();

        for (int i = scanStart; i > playerPos.getY(); i--) {
            Vec3d testPos = new Vec3d(playerPos.getX(), playerPos.getY() + (i - playerPos.getY()), playerPos.getZ());
            if (!invalid(testPos)) return i - playerPos.getY();
        }
        return 0;
    }
}