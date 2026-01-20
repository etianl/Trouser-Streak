//made based on "vault-hclip" written by [agreed](https://github.com/aisiaiiad)
package pwn.noobs.trouserstreak.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class AutoVaultHclipCommand extends Command {
    public AutoVaultHclipCommand() {
        super("autovault-hclip", "Automatically finds closest open 2-block space ahead and vault-hclips to it");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(ctx -> {
            executeAutoVaultHclip();
            return SINGLE_SUCCESS;
        });
    }

    private void executeAutoVaultHclip() {
        ClientPlayerEntity player = mc.player;
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;

        Entity entity = player.hasVehicle() ? player.getVehicle() : player;
        Vec3d start = entity.getEntityPos();
        Vec3d forward = Vec3d.fromPolar(0, player.getYaw()).normalize();

        BlockPos closestSpace = findClosestOpenSpace(entity, start, forward);
        if (closestSpace == null) {
            error("No open space found ahead");
            return;
        }
        double distance = start.distanceTo(new Vec3d(closestSpace.getX() + 0.5, start.y, closestSpace.getZ() + 0.5));
        if (distance > 80) {
            error("Found space %.1f blocks away - too far (>80 unreliable).".formatted(distance));
            return;
        }
        Vec3d targetPos = new Vec3d(closestSpace.getX() + 0.5, closestSpace.getY(), closestSpace.getZ() + 0.5);

        executeVaultClip(entity, start, targetPos);
    }

    private BlockPos findClosestOpenSpace(Entity entity, Vec3d start, Vec3d forward) {
        for (double dist = 1.0; dist <= 99.0; dist += 0.5) {
            Vec3d checkPos = start.add(forward.multiply(dist));
            BlockPos feetPos = BlockPos.ofFloored(checkPos.x, checkPos.y, checkPos.z);

            Vec3d targetCenter = new Vec3d(feetPos.getX() + 0.5, checkPos.y, feetPos.getZ() + 0.5);

            Box targetBox = entity.getBoundingBox().offset(
                    targetCenter.x - entity.getX(),
                    targetCenter.y - entity.getY(),
                    targetCenter.z - entity.getZ()
            );

            if (checkPos.distanceTo(start) >= 1.0 &&
                    Math.abs(checkPos.y - start.y) < 2.0 &&
                    !mc.world.getBlockCollisions(entity, targetBox).iterator().hasNext() &&
                    mc.world.getOtherEntities(entity, targetBox, e -> e.isCollidable(entity)).isEmpty()) {
                return feetPos;
            }
        }
        return null;
    }

    private boolean executeVaultClip(Entity entity, Vec3d start, Vec3d target) {
        Vec3d upPos = start.add(0, 149.0, 0);
        Vec3d aboveTarget = upPos.add(target.x - start.x, 0, target.z - start.z);
        Vec3d downPos = new Vec3d(target.x, start.y, target.z);
        Vec3d downUp = downPos.add(0, 0.01, 0);

        if (invalid(entity, upPos) || invalid(entity, aboveTarget) ||
                invalid(entity, downPos) || invalid(entity, downUp)) {
            return false;
        }

        for (int i = 0; i < 15; i++) {
            if (mc.player.hasVehicle()) mc.player.networkHandler.sendPacket(VehicleMoveC2SPacket.fromVehicle(mc.player.getVehicle()));
            else mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(false, mc.player.horizontalCollision));
        }
        sendMove(entity, upPos);
        sendMove(entity, aboveTarget);
        sendMove(entity, downPos);
        sendMove(entity, downUp);
        entity.setPosition(downUp);

        return true;
    }

    private void sendMove(Entity entity, Vec3d pos) {
        if (mc.getNetworkHandler() == null) return;
        if (entity == mc.player) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, false, false));
        } else {
            mc.getNetworkHandler().sendPacket(new VehicleMoveC2SPacket(pos, mc.player.getVehicle().getYaw(), mc.player.getVehicle().getPitch(), false));
        }
    }

    private boolean invalid(Entity entity, Vec3d pos) {
        Box box = entity.getBoundingBox().offset(
                pos.x - entity.getX(),
                pos.y - entity.getY(),
                pos.z - entity.getZ()
        );
        for (Entity e : mc.world.getOtherEntities(mc.player, box)) {
            if (e.isCollidable(entity)) return true;
        }
        Vec3d delta = pos.subtract(entity.getEntityPos());
        return mc.world.getBlockCollisions(entity, entity.getBoundingBox().offset(delta)).iterator().hasNext();
    }
}