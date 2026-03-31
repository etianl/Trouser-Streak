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

import java.util.*;

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
    private final Setting<Integer> fallHeight = specialGroup.add(new IntSetting.Builder()
            .name("Fall height")
            .description("Simulates a fall from this distance")
            .defaultValue(22)
            .sliderRange(1, 384)
            .min(1)
            .max(384)
            .build());
    private final Setting<Integer> paperpackets = specialGroup.add(new IntSetting.Builder()
            .name("# spam packets")
            .description("Paper allows ~10 blocks of movement per spam packet, 4 packets gets you 22 blocks, 39+ gets you 384. Safe to crank up since MaceKill fires once per manual swing, not continuously, unless you're pairing it with KillAura, in which case, good luck.")
            .defaultValue(4)
            .min(1)
            .sliderRange(1,40)
            .build()
    );
    private final Setting<Boolean> attackSpam = totem.add(new BoolSetting.Builder()
            .name("Bypass totems")
            .description("Settings example: 49 fall height, 8 spam packet, 3 attack, 9 height increase.")
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
            .description("Blocks to add to fall height for each follow-up attack. Each hit must deal more damage than the last to beat invulnerability frames.")
            .defaultValue(9)
            .sliderRange(1, 100)
            .min(1)
            .max(100)
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
        if (mc.player.hasVehicle() || mc.player.getMainHandStack().getItem() != Items.MACE) return;
        if (!(event.packet instanceof IPlayerInteractEntityC2SPacket packet)) return;
        if (packet.meteor$getType() != PlayerInteractEntityC2SPacket.InteractType.ATTACK) return;

        if (!(packet.meteor$getEntity() instanceof LivingEntity)) return;
        LivingEntity targetEntity = (LivingEntity) packet.meteor$getEntity();
        if (packetDisable.get() && (targetEntity.isBlocking() || targetEntity.isInvulnerable() || targetEntity.isInCreativeMode())) return;
        if (!targetEntity.isAlive()) return;

        int baseBlocks = getMaxHeightAbovePlayer();
        if (baseBlocks == 0) {
            error("No valid space above you to attack from.");
            return;
        }

        event.cancel();

        previouspos = mc.player.getEntityPos();
        int currentHeight = baseBlocks;
        int attackCount = attacks.get();
        if (!attackSpam.get()) attackCount = 1;
        for (int i2 = 0; i2 < paperpackets.get(); i2++) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(mc.player.getYaw(), mc.player.getPitch(), false, mc.player.horizontalCollision));
        }

        try {
            boolean targetposvalid = true;
            for (int i = 0; i < attackCount; i++) {
                int blocks = (i == 0) ? baseBlocks : currentHeight;

                if (mc.world == null || mc.player.getY() + blocks > mc.world.getTopYInclusive() - 1) {
                    targetposvalid = false;
                    continue;
                }

                Vec3d targetPos = new Vec3d(mc.player.getX(), mc.player.getY() + blocks, mc.player.getZ());

                sendMove(targetPos);
                sendMove(previouspos);
                mc.player.setPosition(previouspos);

                sendingAttacks = true;
                if (swing.get()) {
                    mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                    mc.player.swingHand(Hand.MAIN_HAND);
                }
                mc.player.networkHandler.sendPacket(
                        PlayerInteractEntityC2SPacket.attack(targetEntity, mc.player.isSneaking())
                );

                currentHeight += increase.get();
            }

            positionCache.clear();

            if (targetposvalid && useOffset.get()){
                Vec3d offsetHome = getOffset(previouspos);
                sendMove(offsetHome);
                mc.player.setPosition(offsetHome);
            }
        } finally {
            sendingAttacks = false;
        }
    }
    private void sendMove(Vec3d pos) {
        if (mc.getNetworkHandler() == null) return;
        PlayerMoveC2SPacket movepacket = new PlayerMoveC2SPacket.Full(pos,mc.player.getYaw(),mc.player.getPitch(), false, mc.player.horizontalCollision);
        ((IPlayerMoveC2SPacket) movepacket).meteor$setTag(1337);
        mc.player.networkHandler.sendPacket(movepacket);
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
        if (mc.world == null) return 0;
        int worldTop = mc.world.getTopYInclusive() - 1;
        int maxBlocks = (int)(worldTop - mc.player.getY());
        return Math.min(fallHeight.get(), maxBlocks);
    }
}
