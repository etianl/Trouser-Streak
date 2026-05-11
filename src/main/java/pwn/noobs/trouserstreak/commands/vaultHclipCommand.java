//written by [agreed](https://github.com/aisiaiiad)
package pwn.noobs.trouserstreak.commands;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class vaultHclipCommand extends Command {
    public vaultHclipCommand() {
        super("vault-hclip", "Uses vault clip to \"hclip\" through blocks");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.then(argument("blocks", DoubleArgumentType.doubleArg()).executes(ctx -> {
            double blocks = ctx.getArgument("blocks", Double.class);
            assert mc.player != null;
            assert mc.level != null;
            if (blocks > 69) {
                error("Distances greater than 69 do not work.");
                return SINGLE_SUCCESS;
            }
            Entity entity = mc.player.isPassenger()
                    ? mc.player.getVehicle()
                    : mc.player;

            if (entity == null) {
                return SINGLE_SUCCESS;
            }

            Vec3 start = entity.position();
            Vec3 forward = Vec3.directionFromRotation(0, mc.player.getYRot()).normalize();

            Vec3 upPos = start.add(0, 129.0, 0);
            Vec3 aboveTarget = upPos.add(forward.x * blocks, 0, forward.z * blocks);
            Vec3 downPos = new Vec3(aboveTarget.x, start.y, aboveTarget.z);
            Vec3 downUp = downPos.add(0, 0.01, 0);

            if (invalid(entity, upPos)
                    || invalid(entity, aboveTarget)
                    || invalid(entity, downPos)
                    || invalid(entity, downUp)) {
                error("At least one of the teleports are invalid.");
                return SINGLE_SUCCESS;
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

            return SINGLE_SUCCESS;
        }));
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