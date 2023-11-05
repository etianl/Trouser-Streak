package pwn.noobs.trouserstreak.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class AutoVclipCommand extends Command {
    public AutoVclipCommand() {
        super("autovclip", "Lets you clip through blocks vertically automatically.");
    }
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(ctx -> {
            error("Choose Up or Down.");
            return SINGLE_SUCCESS;
        });
        builder.then(literal("up").executes(ctx -> {
            ClientPlayerEntity player = mc.player;
            assert player != null;
            for (int i = 0; i < 9; i++) {
                BlockPos isopenair1 = (player.getBlockPos().add(0,i+2,0));
                BlockPos isopenair2 = (player.getBlockPos().add(0,i+3,0));
                if (mc.world.getBlockState(isopenair1).isReplaceable() && mc.world.getFluidState(isopenair1).isEmpty() && !mc.world.getBlockState(isopenair1).isOf(Blocks.POWDER_SNOW) && mc.world.getBlockState(isopenair2).isReplaceable() && mc.world.getFluidState(isopenair2).isEmpty() && !mc.world.getBlockState(isopenair2).isOf(Blocks.POWDER_SNOW)){
                    if (player.hasVehicle()) {
                        Entity vehicle = player.getVehicle();
                        vehicle.setPosition(vehicle.getX(), isopenair1.getY(), vehicle.getZ());
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
            for (int i = 0; i > -10; i--) {
                BlockPos isopenair1 = (player.getBlockPos().add(0,i,0));
                BlockPos isopenair2 = (player.getBlockPos().add(0,i-1,0));
                if (mc.world.getBlockState(isopenair1).isReplaceable() && mc.world.getFluidState(isopenair1).isEmpty() && !mc.world.getBlockState(isopenair1).isOf(Blocks.POWDER_SNOW) && mc.world.getBlockState(isopenair2).isReplaceable() && mc.world.getFluidState(isopenair2).isEmpty() && !mc.world.getBlockState(isopenair2).isOf(Blocks.POWDER_SNOW)){
                    if (player.hasVehicle()) {
                        Entity vehicle = player.getVehicle();
                        vehicle.setPosition(vehicle.getX(), isopenair2.getY(), vehicle.getZ());
                    }
                    player.setPosition(player.getX(), isopenair2.getY(), player.getZ());
                    return SINGLE_SUCCESS;
                }
            }
            error("No gap found to vclip into");
            return SINGLE_SUCCESS;
        }));
    }
}