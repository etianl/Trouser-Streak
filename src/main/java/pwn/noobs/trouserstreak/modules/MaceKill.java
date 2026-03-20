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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import pwn.noobs.trouserstreak.Trouser;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class MaceKill extends Module {
    private final SettingGroup specialGroup2 = settings.createGroup("Disable \"Smash Attack\" in Criticals to make this module work.");
    private final SettingGroup specialGroup = settings.createGroup("Values higher than 22fall/4spam only work on Paper/Spigot");
    private final SettingGroup totem = settings.createGroup("Totem Bypass (PAPER ONLY)");
    private final Setting<Boolean> swing = specialGroup.add(new BoolSetting.Builder()
            .name("swing arm")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> packetDisable = specialGroup.add(new BoolSetting.Builder()
            .name("Disable When Blocked")
            .description("Does not send movement packets if the attack was blocked. (prevents death)")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> requireCooldown = specialGroup.add(new BoolSetting.Builder()
            .name("Require Full Cooldown")
            .description("Only fire when attack cooldown is 100%. Useful for max base damage, but breaks totem bypass.")
            .defaultValue(false)
            .build()
    );
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
    private final Setting<Boolean> attackSpam = totem.add(new BoolSetting.Builder()
            .name("Bypass totems")
            .description("Max ~9 spam packets for this to work. Settings example: 49 fall height, 8 spam packets, 3 attacks, 3 follow-up height.")
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
    private final Setting<Integer> followupHeight = totem.add(new IntSetting.Builder()
            .name("Follow-up Height")
            .description("Fall height for attacks 2 and 3. 2 blocks is the minimum to kill through Absorption IV after a totem pops.")
            .defaultValue(3)
            .sliderRange(2, 15)
            .min(2)
            .max(15)
            .build()
    );
    private final Setting<Boolean> useOffset = specialGroup.add(new BoolSetting.Builder()
            .name("Use Offset")
            .description("Attempts to prevent fall damage even on packet hiccups.")
            .defaultValue(true)
            .build());
    private final Setting<Double> offsethorizontal = specialGroup.add(new DoubleSetting.Builder()
            .name("Horizontal Offset")
            .description("How much to offset the player after teleports.")
            .defaultValue(0.05)
            .min(0)
            .sliderMax(0.99)
            .visible(() -> useOffset.get())
            .build()
    );
    private final Setting<Double> offsetY = specialGroup.add(new DoubleSetting.Builder()
            .name("Y Offset")
            .description("You need some of this to prevent death.")
            .defaultValue(0.01)
            .min(0)
            .sliderMax(0.99)
            .visible(() -> useOffset.get())
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
        boolean holdingMace = mc.player.getMainHandStack().getItem() == Items.MACE
                || mc.player.getOffHandStack().getItem() == Items.MACE;
        if (mc.player.hasVehicle() || !holdingMace) return;
        if (!(event.packet instanceof IPlayerInteractEntityC2SPacket packet)) return;
        if (packet.meteor$getType() != PlayerInteractEntityC2SPacket.InteractType.ATTACK) return;

        if (!(packet.meteor$getEntity() instanceof LivingEntity)) return;
        LivingEntity targetEntity = (LivingEntity) packet.meteor$getEntity();
        if (packetDisable.get() && (targetEntity.isBlocking() || targetEntity.isInvulnerable() || targetEntity.isInCreativeMode())) return;
        if (!targetEntity.isAlive()) return;

        if (requireCooldown.get() && mc.player.getAttackCooldownProgress(1f) < 1.0f) return;

        int baseBlocks = getMaxHeightAbovePlayer();
        if (baseBlocks == 0) {
            error("No space above you to simulate a fall from.");
            return;
        }
        Vec3d firstTargetPos = new Vec3d(mc.player.getX(), mc.player.getY() + baseBlocks, mc.player.getZ());
        if (invalid(firstTargetPos)){
            error("No valid space above you to attack from.");
            return;
        }

        event.cancel();

        previouspos = mc.player.getEntityPos();
        int attackCount = attacks.get();
        if (!attackSpam.get()) attackCount = 1;
        for (int i2 = 0; i2 < paperpackets.get(); i2++) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(false, mc.player.horizontalCollision));
        }

        // try/finally so the flag always clears — if something throws mid-sequence without
        // this it stays true permanently and the module silently stops working until relog
        try {
            boolean targetposvalid = true;
            Hand maceHand = mc.player.getMainHandStack().getItem() == Items.MACE ? Hand.MAIN_HAND : Hand.OFF_HAND;
            for (int i = 0; i < attackCount; i++) {
                // re-prime the server's movement budget before each follow-up.
                // after processing the home packet from hit 1, Paper resets movement tolerance
                // to baseline — the next UP packet gets rejected without this. one OnGroundOnly
                // is all it takes for a 3-block jump, we don't need the full spam again.
                if (i > 0 && attackSpam.get()) {
                    mc.player.networkHandler.sendPacket(
                            new PlayerMoveC2SPacket.OnGroundOnly(false, mc.player.horizontalCollision));
                }

                int blocks = (i == 0) ? baseBlocks : followupHeight.get();
                Vec3d targetPos = new Vec3d(mc.player.getX(), mc.player.getY() + blocks, mc.player.getZ());
                if (invalid(targetPos)) {
                    targetposvalid = false;
                    continue;
                }

                PlayerMoveC2SPacket movepacket = new PlayerMoveC2SPacket.PositionAndOnGround(targetPos.getX(), targetPos.getY(), targetPos.getZ(), false, mc.player.horizontalCollision);
                PlayerMoveC2SPacket homepacket = new PlayerMoveC2SPacket.PositionAndOnGround(previouspos.getX(), previouspos.getY(), previouspos.getZ(), false, mc.player.horizontalCollision);
                ((IPlayerMoveC2SPacket) homepacket).meteor$setTag(1337);
                ((IPlayerMoveC2SPacket) movepacket).meteor$setTag(1337);
                mc.player.networkHandler.sendPacket(movepacket);
                mc.player.networkHandler.sendPacket(homepacket);
                mc.player.setPosition(previouspos);

                sendingAttacks = true;
                if (swing.get()) {
                    mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(maceHand));
                    mc.player.swingHand(maceHand);
                }
                mc.player.networkHandler.sendPacket(
                        PlayerInteractEntityC2SPacket.attack(targetEntity, mc.player.isSneaking())
                );
            }

            positionCache.clear();

            if (targetposvalid && useOffset.get()){
                Vec3d offsetHome = getOffset(previouspos);
                PlayerMoveC2SPacket offsethomepacket = new PlayerMoveC2SPacket.PositionAndOnGround(offsetHome.getX(), offsetHome.getY(), offsetHome.getZ(), false, mc.player.horizontalCollision);
                ((IPlayerMoveC2SPacket) offsethomepacket).meteor$setTag(1337);
                mc.player.networkHandler.sendPacket(offsethomepacket);
                mc.player.setPosition(offsetHome);
            }
        } finally {
            sendingAttacks = false;
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
    private final BlockPos.Mutable mutablePos = new BlockPos.Mutable();

    // bounded at 256 entries with LRU eviction — at high fall heights this grows pretty large
    // without a cap since getMaxHeightAbovePlayer scans up to 169 positions per call
    private final Map<Vec3d, Boolean> positionCache = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Vec3d, Boolean> eldest) {
            return size() > 256;
        }
    };

    private boolean invalid(Vec3d pos) {
        if (mc.world == null) return true;

        double clampedY = MathHelper.clamp(pos.y, mc.world.getBottomY(), mc.world.getTopYInclusive() - 1);
        if (clampedY != pos.y) return true;

        BlockPos floored = BlockPos.ofFloored(pos);
        int chunkX = floored.getX() >> 4;
        int chunkZ = floored.getZ() >> 4;
        if (mc.world.getChunkManager().getWorldChunk(chunkX, chunkZ) == null) return true;

        if (positionCache.containsKey(pos)) return positionCache.get(pos);

        Entity entity = mc.player;
        Vec3d delta = pos.subtract(entity.getEntityPos());
        Box box = entity.getBoundingBox().offset(delta);

        mutablePos.set(floored);
        for (int x = -1; x <= 1; x++) {
            mutablePos.setX(floored.getX() + x);
            for (int y = -1; y <= 1; y++) {
                mutablePos.setY(floored.getY() + y);
                for (int z = -1; z <= 1; z++) {
                    mutablePos.setZ(floored.getZ() + z);
                    BlockState state = mc.world.getBlockState(mutablePos);
                    if (state.isOf(Blocks.LAVA) || state.isOf(Blocks.FIRE) || state.isOf(Blocks.SOUL_FIRE)
                            || state.isOf(Blocks.MAGMA_BLOCK) || state.isOf(Blocks.CAMPFIRE)
                            || state.isOf(Blocks.SWEET_BERRY_BUSH) || state.isOf(Blocks.POWDER_SNOW)) {
                        positionCache.put(pos, true);
                        return true;
                    }
                }
            }
        }

        for (Entity e : mc.world.getOtherEntities(entity, box)) {
            if (e.isCollidable(entity)) {
                positionCache.put(pos, true);
                return true;
            }
        }

        boolean collides = mc.world.getBlockCollisions(entity, box).iterator().hasNext();
        positionCache.put(pos, collides);
        return collides;
    }

    private int getMaxHeightAbovePlayer() {
        BlockPos playerPos = mc.player.getBlockPos();
        int scanStart = (int) MathHelper.clamp(playerPos.getY() + fallHeight.get(),
                playerPos.getY() + 1, mc.world.getTopYInclusive() - 1);

        positionCache.clear();
        for (int i = scanStart; i > playerPos.getY(); i--) {
            Vec3d testPos = new Vec3d(playerPos.getX(), i, playerPos.getZ());
            if (!invalid(testPos)) return i - playerPos.getY();
        }
        return 0;
    }
}
