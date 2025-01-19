package pwn.noobs.trouserstreak.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.util.math.BlockPos;

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
                BlockPos isopenair1 = (player.getBlockPos().add(0, i + 2, 0));
                BlockPos isopenair2 = (player.getBlockPos().add(0, i + 3, 0));
                if (mc.world.getBlockState(isopenair1).isReplaceable() && mc.world.getFluidState(isopenair1).isEmpty() && !mc.world.getBlockState(isopenair1).isOf(Blocks.POWDER_SNOW) && mc.world.getBlockState(isopenair2).isReplaceable() && mc.world.getFluidState(isopenair2).isEmpty() && !mc.world.getBlockState(isopenair2).isOf(Blocks.POWDER_SNOW)) {
                    int packetsRequired = 20;
                    if (player.hasVehicle()) {
                        Entity vehicle = player.getVehicle();
                        for (int packetNumber = 0; packetNumber < (packetsRequired - 1); packetNumber++) {
                            mc.player.networkHandler.sendPacket(VehicleMoveC2SPacket.fromVehicle(mc.player.getVehicle()));
                        }
                        vehicle.setPosition(vehicle.getX(), isopenair1.getY(), vehicle.getZ());
                    }
                    for (int packetNumber = 0; packetNumber < (packetsRequired - 1); packetNumber++) {
                        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, mc.player.horizontalCollision));
                    }
                    player.setPosition(player.getX(), isopenair1.getY(), player.getZ());
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
                BlockPos isopenair1 = (player.getBlockPos().add(0, i, 0));
                BlockPos isopenair2 = (player.getBlockPos().add(0, i - 1, 0));
                if (mc.world.getBlockState(isopenair1).isReplaceable() && mc.world.getFluidState(isopenair1).isEmpty() && !mc.world.getBlockState(isopenair1).isOf(Blocks.POWDER_SNOW) && mc.world.getBlockState(isopenair2).isReplaceable() && mc.world.getFluidState(isopenair2).isEmpty() && !mc.world.getBlockState(isopenair2).isOf(Blocks.POWDER_SNOW)) {
                    int packetsRequired = 20;
                    if (player.hasVehicle()) {
                        Entity vehicle = player.getVehicle();
                        for (int packetNumber = 0; packetNumber < (packetsRequired - 1); packetNumber++) {
                            mc.player.networkHandler.sendPacket(VehicleMoveC2SPacket.fromVehicle(mc.player.getVehicle()));
                        }
                        vehicle.setPosition(vehicle.getX(), isopenair2.getY(), vehicle.getZ());
                    }
                    for (int packetNumber = 0; packetNumber < (packetsRequired - 1); packetNumber++) {
                        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, mc.player.horizontalCollision));
                    }
                    player.setPosition(player.getX(), isopenair2.getY(), player.getZ());
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
                BlockPos isopenair1 = (player.getBlockPos().add(0, i, 0));
                BlockPos newopenair2 = isopenair1.up(1);
                if (!mc.world.getBlockState(isopenair1).isReplaceable() || mc.world.getBlockState(isopenair1).isOf(Blocks.POWDER_SNOW) || !mc.world.getFluidState(isopenair1).isEmpty()) {
                    int packetsRequired = 20;
                    if (player.hasVehicle()) {
                        Entity vehicle = player.getVehicle();
                        for (int packetNumber = 0; packetNumber < (packetsRequired - 1); packetNumber++) {
                            mc.player.networkHandler.sendPacket(VehicleMoveC2SPacket.fromVehicle(mc.player.getVehicle()));
                        }

                        vehicle.setPosition(vehicle.getX(), newopenair2.getY(), vehicle.getZ());
                    }
                    for (int packetNumber = 0; packetNumber < (packetsRequired - 1); packetNumber++) {
                        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, mc.player.horizontalCollision));
                    }
                    player.setPosition(player.getX(), newopenair2.getY(), player.getZ());
                    return SINGLE_SUCCESS;
                }
            }
            error("No blocks above you found!");
            return SINGLE_SUCCESS;
        }));
    }
}