package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.mixininterface.IServerboundMovePlayerPacket;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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

    private Vec3 previouspos;
    private boolean sendingAttacks = false;
    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (sendingAttacks) return;
        if (mc.player == null) return;
        if (mc.player.isPassenger() || mc.player.getMainHandItem().getItem() != Items.MACE) return;
        if (!(event.packet instanceof ServerboundAttackPacket packet)) return;

        Entity targetentity = mc.level.getEntity(packet.entityId());
        if (!(targetentity instanceof LivingEntity)) return;
        LivingEntity targetEntity = (LivingEntity) targetentity;
        if (packetDisable.get() && (targetEntity.isBlocking() || targetEntity.isInvulnerable() || targetEntity.hasInfiniteMaterials())) return;
        if (!targetEntity.isAlive()) return;

        int baseBlocks = getMaxHeightAbovePlayer();
        if (baseBlocks == 0) {
            error("No valid space above you to attack from.");
            return;
        }

        event.cancel();

        previouspos = mc.player.position();
        int currentHeight = baseBlocks;
        int attackCount = attacks.get();
        if (!attackSpam.get()) attackCount = 1;
        for (int i2 = 0; i2 < paperpackets.get(); i2++) {
            mc.player.connection.send(new ServerboundMovePlayerPacket.Rot(mc.player.getYRot(), mc.player.getXRot(), false, mc.player.horizontalCollision));
        }

        try {
            boolean targetposvalid = true;
            for (int i = 0; i < attackCount; i++) {
                int blocks = (i == 0) ? baseBlocks : currentHeight;

                if (mc.level == null || mc.player.getY() + blocks > mc.level.getMaxY() - 1) {
                    targetposvalid = false;
                    continue;
                }

                Vec3 targetPos = new Vec3(mc.player.getX(), mc.player.getY() + blocks, mc.player.getZ());

                sendMove(targetPos);
                sendMove(previouspos);
                mc.player.setPos(previouspos);

                sendingAttacks = true;
                if (swing.get()) {
                    mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
                    mc.player.swing(InteractionHand.MAIN_HAND);
                }
                mc.player.connection.send(
                        new ServerboundAttackPacket(targetEntity.getId())
                );

                currentHeight += increase.get();
            }

            positionCache.clear();

            if (targetposvalid && useOffset.get()){
                Vec3 offsetHome = getOffset(previouspos);
                sendMove(offsetHome);
                mc.player.setPos(offsetHome);
            }
        } finally {
            sendingAttacks = false;
        }
    }
    private void sendMove(Vec3 pos) {
        if (mc.getConnection() == null) return;
        ServerboundMovePlayerPacket movepacket = new ServerboundMovePlayerPacket.PosRot(pos,mc.player.getYRot(),mc.player.getXRot(), false, mc.player.horizontalCollision);
        ((IServerboundMovePlayerPacket) movepacket).meteor$setTag(1337);
        mc.player.connection.send(movepacket);
    }
    private Vec3 getOffset(Vec3 base) {
        double dx = offsethorizontal.get();
        double dy = offsetY.get();

        Vec3[] shuffledOffsets = new Vec3[] {
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

        for (Vec3 pos : shuffledOffsets) {
            if (!invalid(pos)) return pos;
        }

        Vec3 noHorizontal = base.add(0, dy, 0);
        if (!invalid(noHorizontal)) return noHorizontal;

        return base;
    }
    private final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
    private final Map<Vec3, Boolean> positionCache = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Vec3, Boolean> eldest) {
            return size() > 256;
        }
    };
    private boolean invalid(Vec3 pos) {
        if (mc.level == null) return true;

        double clampedY = Mth.clamp(pos.y, mc.level.getMinY(), mc.level.getMaxY() - 1);
        if (clampedY != pos.y) return true;

        BlockPos floored = BlockPos.containing(pos);
        int chunkX = floored.getX() >> 4;
        int chunkZ = floored.getZ() >> 4;
        if (mc.level.getChunkSource().getChunkNow(chunkX, chunkZ) == null) return true;

        if (positionCache.containsKey(pos)) return positionCache.get(pos);

        Entity entity = mc.player;
        Vec3 delta = pos.subtract(entity.position());
        AABB box = entity.getBoundingBox().move(delta);

        mutablePos.set(floored);
        for (int x = -1; x <= 1; x++) {
            mutablePos.setX(floored.getX() + x);
            for (int y = -1; y <= 1; y++) {
                mutablePos.setY(floored.getY() + y);
                for (int z = -1; z <= 1; z++) {
                    mutablePos.setZ(floored.getZ() + z);
                    BlockState state = mc.level.getBlockState(mutablePos);
                    if (state.is(Blocks.LAVA) || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)
                            || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CAMPFIRE)
                            || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.POWDER_SNOW)) {
                        positionCache.put(pos, true);
                        return true;
                    }
                }
            }
        }

        for (Entity e : mc.level.getEntities(entity, box)) {
            if (e.canBeCollidedWith(entity)) {
                positionCache.put(pos, true);
                return true;
            }
        }

        boolean collides = mc.level.getBlockCollisions(entity, box).iterator().hasNext();
        positionCache.put(pos, collides);
        return collides;
    }
    private int getMaxHeightAbovePlayer() {
        if (mc.level == null) return 0;
        int worldTop = mc.level.getMaxY() - 1;
        int maxBlocks = (int)(worldTop - mc.player.getY());
        return Math.min(fallHeight.get(), maxBlocks);
    }
}