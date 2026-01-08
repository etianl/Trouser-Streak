//written by [agreed](https://github.com/aisiaiiad)
package pwn.noobs.trouserstreak.commands;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class vaultHclipCommand extends Command {
    public vaultHclipCommand() {
        super("vault-hclip", "Uses vault clip to \"hclip\" through blocks");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("blocks", DoubleArgumentType.doubleArg()).executes(ctx -> {
            double blocks = ctx.getArgument("blocks", Double.class);
            assert mc.player != null;
            assert mc.world != null;

            Entity entity = mc.player.hasVehicle()
                    ? mc.player.getVehicle()
                    : mc.player;

            if (entity == null) {
                return SINGLE_SUCCESS;
            }

            Vec3d start = entity.getEntityPos();
            Vec3d forward = Vec3d.fromPolar(0, mc.player.getYaw()).normalize();

            Vec3d upPos = start.add(0, 99.0, 0);
            Vec3d aboveTarget = upPos.add(forward.x * blocks, 0, forward.z * blocks);
            Vec3d downPos = new Vec3d(aboveTarget.x, start.y, aboveTarget.z);
            Vec3d downUp = downPos.add(0, 0.01, 0);

            if (invalid(entity, upPos)
                    || invalid(entity, aboveTarget)
                    || invalid(entity, downPos)
                    || invalid(entity, downUp)) {
                error("At least one of the teleports are invalid.");
                return SINGLE_SUCCESS;
            }

            for (int i = 0; i < 9; i++) {
                if (mc.player.hasVehicle()) mc.player.networkHandler.sendPacket(VehicleMoveC2SPacket.fromVehicle(mc.player.getVehicle()));
                else mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(false, mc.player.horizontalCollision));
            }

            sendMove(entity, upPos);
            sendMove(entity, aboveTarget);
            sendMove(entity, downPos);
            sendMove(entity, downUp);
            entity.setPosition(downUp);

            return SINGLE_SUCCESS;
        }));
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