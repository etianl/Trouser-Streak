//made based on "vault-hclip" written by [agreed](https://github.com/aisiaiiad)
package pwn.noobs.trouserstreak.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class AutoVaultHclipCommand extends Command {
    public AutoVaultHclipCommand() {
        super("autovault-hclip", "Automatically finds closest open 2-block space ahead and vault-hclips to it");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(ctx -> {
            executeAutoVaultHclip();
            return SINGLE_SUCCESS;
        });
    }

    private void executeAutoVaultHclip() {
        LocalPlayer player = mc.player;
        if (mc.player == null || mc.level == null || mc.getConnection() == null) return;

        Entity entity = player.isPassenger() ? player.getVehicle() : player;
        Vec3 start = entity.position();
        Vec3 forward = Vec3.directionFromRotation(0, player.getYRot()).normalize();

        BlockPos closestSpace = findClosestOpenSpace(entity, start, forward);
        if (closestSpace == null) {
            error("No open space found ahead");
            return;
        }
        double distance = start.distanceTo(new Vec3(closestSpace.getX() + 0.5, start.y, closestSpace.getZ() + 0.5));
        if (distance > 69) {
            error("Found space %.1f blocks away - too far (>69 unreliable).".formatted(distance));
            return;
        }
        Vec3 targetPos = new Vec3(closestSpace.getX() + 0.5, closestSpace.getY(), closestSpace.getZ() + 0.5);

        executeVaultClip(entity, start, targetPos);
    }

    private BlockPos findClosestOpenSpace(Entity entity, Vec3 start, Vec3 forward) {
        for (double dist = 1.0; dist <= 69.0; dist += 0.5) {
            Vec3 checkPos = start.add(forward.scale(dist));
            BlockPos feetPos = BlockPos.containing(checkPos.x, checkPos.y, checkPos.z);

            Vec3 targetCenter = new Vec3(feetPos.getX() + 0.5, checkPos.y, feetPos.getZ() + 0.5);

            AABB targetBox = entity.getBoundingBox().move(
                    targetCenter.x - entity.getX(),
                    targetCenter.y - entity.getY(),
                    targetCenter.z - entity.getZ()
            );

            if (checkPos.distanceTo(start) >= 1.0 &&
                    Math.abs(checkPos.y - start.y) < 2.0 &&
                    !mc.level.getBlockCollisions(entity, targetBox).iterator().hasNext() &&
                    mc.level.getEntities(entity, targetBox, e -> e.canBeCollidedWith(entity)).isEmpty()) {
                return feetPos;
            }
        }
        return null;
    }

    private boolean executeVaultClip(Entity entity, Vec3 start, Vec3 target) {
        Vec3 upPos = start.add(0, 129.0, 0);
        Vec3 aboveTarget = upPos.add(target.x - start.x, 0, target.z - start.z);
        Vec3 downPos = new Vec3(target.x, start.y, target.z);
        Vec3 downUp = downPos.add(0, 0.01, 0);

        if (invalid(entity, upPos) || invalid(entity, aboveTarget) ||
                invalid(entity, downPos) || invalid(entity, downUp)) {
            return false;
        }

        for (int i = 0; i < 13; i++) {
            if (mc.player.isPassenger()) mc.player.connection.send(ServerboundMoveVehiclePacket.fromEntity(mc.player.getVehicle()));
            else mc.player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(false, mc.player.horizontalCollision));
        }
        sendMove(entity, upPos);
        sendMove(entity, aboveTarget);
        sendMove(entity, downPos);
        sendMove(entity, downUp);
        entity.setPos(downUp);

        return true;
    }

    private void sendMove(Entity entity, Vec3 pos) {
        if (mc.getConnection() == null) return;
        if (entity == mc.player) {
            mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(pos.x, pos.y, pos.z, false, false));
        } else {
            mc.getConnection().send(new ServerboundMoveVehiclePacket(pos, mc.player.getVehicle().getYRot(), mc.player.getVehicle().getXRot(), false));
        }
    }

    private boolean invalid(Entity entity, Vec3 pos) {
        AABB box = entity.getBoundingBox().move(
                pos.x - entity.getX(),
                pos.y - entity.getY(),
                pos.z - entity.getZ()
        );
        for (Entity e : mc.level.getEntities(mc.player, box)) {
            if (e.canBeCollidedWith(entity)) return true;
        }
        Vec3 delta = pos.subtract(entity.position());
        return mc.level.getBlockCollisions(entity, entity.getBoundingBox().move(delta)).iterator().hasNext();
    }
}