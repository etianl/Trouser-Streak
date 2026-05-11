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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class AutoVclipCommand extends Command {
    public AutoVclipCommand() {
        super("autovclip", "Lets you clip through blocks vertically automatically.");
    }
    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(ctx -> {
            error("Choose Up, Down or Highest");
            return SINGLE_SUCCESS;
        });
        builder.then(literal("up").executes(ctx -> {
            LocalPlayer player = mc.player;
            assert player != null;
            for (int i = 0; i < 21; i++) {
                BlockPos isopenair1 = (player.blockPosition().offset(0,i+2,0));
                BlockPos isopenair2 = (player.blockPosition().offset(0,i+3,0));
                if (mc.level.getBlockState(isopenair1).canBeReplaced() && mc.level.getFluidState(isopenair1).isEmpty() && !mc.level.getBlockState(isopenair1).is(Blocks.POWDER_SNOW) && mc.level.getBlockState(isopenair2).canBeReplaced() && mc.level.getFluidState(isopenair2).isEmpty() && !mc.level.getBlockState(isopenair2).is(Blocks.POWDER_SNOW)){
                    if (player.isPassenger()) {
                        Entity vehicle = player.getVehicle();
                        for (int packetNumber = 0; packetNumber < 4; packetNumber++) {
                            player.connection.send(ServerboundMoveVehiclePacket.fromEntity(player.getVehicle()));
                        }
                        mc.getConnection().send(new ServerboundMoveVehiclePacket(new Vec3(vehicle.getX(), isopenair1.getY(), vehicle.getZ()), vehicle.getYRot(), vehicle.getXRot(), false));
                        vehicle.setPos(vehicle.getX(), isopenair1.getY(), vehicle.getZ());
                    } else {
                        for (int packetNumber = 0; packetNumber < 4; packetNumber++) {
                            player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(true, player.horizontalCollision));
                        }
                        player.connection.send(new ServerboundMovePlayerPacket.Pos(player.getX(), isopenair1.getY(), player.getZ(), false, player.horizontalCollision));
                        player.setPos(player.getX(), isopenair1.getY(), player.getZ());
                    }
                    return SINGLE_SUCCESS;
                }
            }
            error("No gap found to vclip into");
            return SINGLE_SUCCESS;
        }));
        builder.then(literal("down").executes(ctx -> {
            LocalPlayer player = mc.player;
            assert player != null;
            for (int i = 0; i > -22; i--) {
                BlockPos isopenair1 = (player.blockPosition().offset(0,i,0));
                BlockPos isopenair2 = (player.blockPosition().offset(0,i-1,0));
                if (mc.level.getBlockState(isopenair1).canBeReplaced() && mc.level.getFluidState(isopenair1).isEmpty() && !mc.level.getBlockState(isopenair1).is(Blocks.POWDER_SNOW) && mc.level.getBlockState(isopenair2).canBeReplaced() && mc.level.getFluidState(isopenair2).isEmpty() && !mc.level.getBlockState(isopenair2).is(Blocks.POWDER_SNOW)){
                    if (player.isPassenger()) {
                        Entity vehicle = player.getVehicle();
                        for (int packetNumber = 0; packetNumber < 4; packetNumber++) {
                            player.connection.send(ServerboundMoveVehiclePacket.fromEntity(player.getVehicle()));
                        }
                        mc.getConnection().send(new ServerboundMoveVehiclePacket(new Vec3(vehicle.getX(), isopenair2.getY(), vehicle.getZ()), vehicle.getYRot(), vehicle.getXRot(), false));
                        vehicle.setPos(vehicle.getX(), isopenair2.getY(), vehicle.getZ());
                    } else {
                        for (int packetNumber = 0; packetNumber < 4; packetNumber++) {
                            player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(true, player.horizontalCollision));
                        }
                        player.connection.send(new ServerboundMovePlayerPacket.Pos(player.getX(), isopenair2.getY(), player.getZ(), false, player.horizontalCollision));
                        player.setPos(player.getX(), isopenair2.getY(), player.getZ());
                        double y = isopenair2.getY() + 0.0000000001;
                        player.connection.send(new ServerboundMovePlayerPacket.Pos(player.getX(), y, player.getZ(), false, player.horizontalCollision));
                        player.setPos(player.getX(), y, player.getZ());
                    }
                    return SINGLE_SUCCESS;
                }
            }
            error("No gap found to vclip into");
            return SINGLE_SUCCESS;
        }));
        builder.then(literal("highest").executes(ctx -> {
            LocalPlayer player = mc.player;
            assert player != null;
            for (int i = 21; i > 0; i--) {
                BlockPos isopenair1 = (player.blockPosition().offset(0,i,0));
                BlockPos newopenair2 = isopenair1.above(1);
                if (!mc.level.getBlockState(isopenair1).canBeReplaced() || mc.level.getBlockState(isopenair1).is(Blocks.POWDER_SNOW) || !mc.level.getFluidState(isopenair1).isEmpty()) {
                    if (player.isPassenger()) {
                        Entity vehicle = player.getVehicle();
                        for (int packetNumber = 0; packetNumber < 4; packetNumber++) {
                            player.connection.send(ServerboundMoveVehiclePacket.fromEntity(player.getVehicle()));
                        }
                        mc.getConnection().send(new ServerboundMoveVehiclePacket(new Vec3(vehicle.getX(), newopenair2.getY(), vehicle.getZ()), vehicle.getYRot(), vehicle.getXRot(), false));
                        vehicle.setPos(vehicle.getX(), newopenair2.getY(), vehicle.getZ());
                    } else {
                        for (int packetNumber = 0; packetNumber < 4; packetNumber++) {
                            player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(true, player.horizontalCollision));
                        }
                        player.connection.send(new ServerboundMovePlayerPacket.Pos(player.getX(), newopenair2.getY(), player.getZ(), false, player.horizontalCollision));
                        player.setPos(player.getX(), newopenair2.getY(), player.getZ());
                    }
                    return SINGLE_SUCCESS;
                }
            }
            error("No blocks above you found!");
            return SINGLE_SUCCESS;
        }));
    }
}