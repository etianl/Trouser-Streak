package pwn.noobs.trouserstreak.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.netty.buffer.Unpooled;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class AutoVaultClipCommand extends Command {
    public AutoVaultClipCommand() {
        super("autovaultclip", "Lets you clip through blocks vertically automatically, with vault clip bypass implemented. Works on Paper, Spigot, but not always on Vanilla.");
    }
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(ctx -> {
            error("Choose Up, Down or Highest");
            return SINGLE_SUCCESS;
        });
        builder.then(literal("up").executes(ctx -> {
            ClientPlayerEntity player = mc.player;
            assert player != null;
            for (int i = 0; i < 199; i++) {
                BlockPos isopenair1 = (player.getBlockPos().add(0,i+2,0));
                BlockPos isopenair2 = (player.getBlockPos().add(0,i+3,0));
                if (mc.world.getBlockState(isopenair1).isReplaceable() && mc.world.getFluidState(isopenair1).isEmpty() && !mc.world.getBlockState(isopenair1).isOf(Blocks.POWDER_SNOW) && mc.world.getBlockState(isopenair2).isReplaceable() && mc.world.getFluidState(isopenair2).isEmpty() && !mc.world.getBlockState(isopenair2).isOf(Blocks.POWDER_SNOW)){
                    int packetsRequired = computePacketsRequired(player.getY(), isopenair1.getY());
                    if (player.hasVehicle()) {
                        Entity vehicle = player.getVehicle();
                        for (int packetNumber = 0; packetNumber < (packetsRequired - 1); packetNumber++) {
                            player.networkHandler.sendPacket(new VehicleMoveC2SPacket(player.getVehicle()));
                        }
                        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                        buf.writeDouble(vehicle.getX());
                        buf.writeDouble(isopenair1.getY());
                        buf.writeDouble(vehicle.getZ());
                        buf.writeFloat(vehicle.getYaw());
                        buf.writeFloat(vehicle.getPitch());

                        VehicleMoveC2SPacket packet = new VehicleMoveC2SPacket(buf);
                        mc.getNetworkHandler().sendPacket(packet);
                        vehicle.setPosition(vehicle.getX(), isopenair1.getY(), vehicle.getZ());
                    } else {
                        for (int packetNumber = 0; packetNumber < (packetsRequired - 1); packetNumber++) {
                            player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
                        }
                        player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(player.getX(), isopenair1.getY(), player.getZ(), false));
                        player.setPosition(player.getX(), isopenair1.getY(), player.getZ());
                    }
                    return SINGLE_SUCCESS;
                }
            }
            error("No gap found to vclip into");
            return SINGLE_SUCCESS;
        }));
        builder.then(literal("down").executes(ctx -> {
            ClientPlayerEntity player = mc.player;
            assert player != null;
            for (int i = 0; i > -199; i--) {
                BlockPos isopenair1 = (player.getBlockPos().add(0,i,0));
                BlockPos isopenair2 = (player.getBlockPos().add(0,i-1,0));
                if (mc.world.getBlockState(isopenair1).isReplaceable() && mc.world.getFluidState(isopenair1).isEmpty() && !mc.world.getBlockState(isopenair1).isOf(Blocks.POWDER_SNOW) && mc.world.getBlockState(isopenair2).isReplaceable() && mc.world.getFluidState(isopenair2).isEmpty() && !mc.world.getBlockState(isopenair2).isOf(Blocks.POWDER_SNOW)){
                    int packetsRequired = computePacketsRequired(player.getY(), isopenair2.getY());
                    if (player.hasVehicle()) {
                        Entity vehicle = player.getVehicle();
                        for (int packetNumber = 0; packetNumber < (packetsRequired - 1); packetNumber++) {
                            player.networkHandler.sendPacket(new VehicleMoveC2SPacket(player.getVehicle()));
                        }
                        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                        buf.writeDouble(vehicle.getX());
                        buf.writeDouble(isopenair2.getY());
                        buf.writeDouble(vehicle.getZ());
                        buf.writeFloat(vehicle.getYaw());
                        buf.writeFloat(vehicle.getPitch());

                        VehicleMoveC2SPacket packet = new VehicleMoveC2SPacket(buf);
                        mc.getNetworkHandler().sendPacket(packet);
                        vehicle.setPosition(vehicle.getX(), isopenair2.getY(), vehicle.getZ());
                    } else {
                        for (int packetNumber = 0; packetNumber < (packetsRequired - 1); packetNumber++) {
                            player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
                        }
                        player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(player.getX(), isopenair2.getY(), player.getZ(), false));
                        player.setPosition(player.getX(), isopenair2.getY(), player.getZ());
                        double y = isopenair2.getY() + 0.0000000001;
                        player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(player.getX(), y, player.getZ(), false)); // we are slightly higher, resets fall distance to 0
                        player.setPosition(player.getX(), y, player.getZ());
                    }
                    return SINGLE_SUCCESS;
                }
            }
            error("No gap found to vclip into");
            return SINGLE_SUCCESS;
        }));
        builder.then(literal("highest").executes(ctx -> {
            ClientPlayerEntity player = mc.player;
            assert player != null;

            for (int i = 199; i > 0; i--) {
                BlockPos isopenair1 = (player.getBlockPos().add(0,i,0));
                BlockPos newopenair2 = isopenair1.up(1);
                if (!mc.world.getBlockState(isopenair1).isReplaceable() || mc.world.getBlockState(isopenair1).isOf(Blocks.POWDER_SNOW) || !mc.world.getFluidState(isopenair1).isEmpty()) {
                    int packetsRequired = computePacketsRequired(player.getY(), newopenair2.getY());
                    if (player.hasVehicle()) {
                        Entity vehicle = player.getVehicle();
                        for (int packetNumber = 0; packetNumber < (packetsRequired - 1); packetNumber++) {
                            player.networkHandler.sendPacket(new VehicleMoveC2SPacket(player.getVehicle()));
                        }
                        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                        buf.writeDouble(vehicle.getX());
                        buf.writeDouble(newopenair2.getY());
                        buf.writeDouble(vehicle.getZ());
                        buf.writeFloat(vehicle.getYaw());
                        buf.writeFloat(vehicle.getPitch());

                        VehicleMoveC2SPacket packet = new VehicleMoveC2SPacket(buf);
                        mc.getNetworkHandler().sendPacket(packet);
                        vehicle.setPosition(vehicle.getX(), newopenair2.getY(), vehicle.getZ());
                    } else {
                        for (int packetNumber = 0; packetNumber < (packetsRequired - 1); packetNumber++) {
                            player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
                        }
                        player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(player.getX(), newopenair2.getY(), player.getZ(), false));
                        player.setPosition(player.getX(), newopenair2.getY(), player.getZ());
                    }
                    return SINGLE_SUCCESS;
                }
            }
            error("No blocks above you found!");
            return SINGLE_SUCCESS;
        }));
    }
    private int computePacketsRequired(double fromY, double toY) {
        double blocks = toY - fromY;
        int packets = (int) Math.ceil(Math.abs(blocks / 10.0));
        return Math.max(packets, 1);
    }
}