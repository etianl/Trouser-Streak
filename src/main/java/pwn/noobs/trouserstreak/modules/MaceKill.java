package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerInteractEntityC2SPacket;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import pwn.noobs.trouserstreak.Trouser;

public class MaceKill extends Module {
    private final SettingGroup specialGroup = settings.createGroup("Values higher than 22 only work on Paper/Spigot");
    private final Setting<Boolean> maxPower = specialGroup.add(new BoolSetting.Builder()
        .name("Maximum Mace Power (Paper/Spigot servers only)")
        .description("Simulates a fall from the highest air gap within 170 blocks")
        .defaultValue(false)
        .build());
    private final Setting<Integer> fallHeight = specialGroup.add(new IntSetting.Builder()
        .name("Mace Power (Fall height)")
        .description("Simulates a fall from this distance")
        .defaultValue(22)
        .sliderRange(1, 170)
        .min(1)
        .max(170)
        .visible(() -> !maxPower.get())
        .build());
    private final Setting<Boolean> packetDisable = specialGroup.add(new BoolSetting.Builder()
        .name("Disable When Blocked")
        .description("Does not send movement packets if the attack was blocked. (prevents death)")
        .defaultValue(true)
        .build());

    public MaceKill() {
        super(Trouser.Main, "MaceKill", "Makes the Mace powerful when swung.");
    }

    private Vec3d previouspos;

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (mc.player == null) return;
        if (mc.player.getMainHandStack().getItem() != Items.MACE) return;
        if (!(event.packet instanceof IPlayerInteractEntityC2SPacket packet)) return;
        if (packet.meteor$getType() != PlayerInteractEntityC2SPacket.InteractType.ATTACK) return;

        LivingEntity targetEntity = (LivingEntity) packet.meteor$getEntity();
        if (packetDisable.get() && (targetEntity.isBlocking() || targetEntity.isInvulnerable() || targetEntity.isInCreativeMode()))
            return;

        previouspos = mc.player.getPos();
        int blocks = getMaxHeightAbovePlayer();
        int packetsRequired = (int) Math.ceil(Math.abs(blocks / 10.0));
        if (packetsRequired > 20) packetsRequired = 1;

        BlockPos air1 = mc.player.getBlockPos().add(0, blocks, 0);
        BlockPos air2 = air1.up(1);
        if (!isSafeBlock(air1) || !isSafeBlock(air2)) return;

        if (mc.player.hasVehicle()) {
            // —— ORIGINAL VEHICLE BRANCH, zero fixes here —— :contentReference[oaicite:0]{index=0}&#8203;:contentReference[oaicite:1]{index=1}
            for (int i = 0; i < 4; i++) {
                mc.player.networkHandler.sendPacket(VehicleMoveC2SPacket.fromVehicle(mc.player.getVehicle()));
            }
            double maxH = Math.min(mc.player.getVehicle().getY() + 22, mc.player.getVehicle().getY() + blocks);
            mc.player.getVehicle().setPosition(
                mc.player.getVehicle().getX(),
                maxH + blocks,
                mc.player.getVehicle().getZ()
            );
            mc.player.networkHandler.sendPacket(VehicleMoveC2SPacket.fromVehicle(mc.player.getVehicle()));
            mc.player.getVehicle().setPosition(previouspos);
            mc.player.networkHandler.sendPacket(VehicleMoveC2SPacket.fromVehicle(mc.player.getVehicle()));
        } else {
            // —— HALF-BLOCK LAND + UPWARD THRUST for players only —— 
            for (int i = 0; i < (blocks <= 22 ? 4 : packetsRequired - 1); i++) {
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(false, mc.player.horizontalCollision));
            }
            double targetY = mc.player.getY() + blocks;
            PlayerMoveC2SPacket up = new PlayerMoveC2SPacket.PositionAndOnGround(
                mc.player.getX(), targetY, mc.player.getZ(), false, mc.player.horizontalCollision
            );
            PlayerMoveC2SPacket back = new PlayerMoveC2SPacket.PositionAndOnGround(
                previouspos.getX(), previouspos.getY() + 0.5, previouspos.getZ(), false, mc.player.horizontalCollision
            );
            ((IPlayerMoveC2SPacket) up).meteor$setTag(1337);
            ((IPlayerMoveC2SPacket) back).meteor$setTag(1337);
            mc.player.networkHandler.sendPacket(up);
            mc.player.networkHandler.sendPacket(back);

            // tiny upward velocity & clear fallDistance
            mc.player.setVelocity(mc.player.getVelocity().x, 0.1, mc.player.getVelocity().z);
            mc.player.fallDistance = 0;
        }
    }

    private int getMaxHeightAbovePlayer() {
        BlockPos p = mc.player.getBlockPos();
        int top = p.getY() + (maxPower.get() ? 170 : fallHeight.get());
        for (int y = top; y > p.getY(); y--) {
            BlockPos a = new BlockPos(p.getX(), y, p.getZ());
            if (isSafeBlock(a) && isSafeBlock(a.up())) return y - p.getY();
        }
        return 0; // Return 0 if no suitable position is found
    }

    private boolean isSafeBlock(BlockPos pos) {
        return mc.world.getBlockState(pos).isReplaceable()
            && mc.world.getFluidState(pos).isEmpty()
            && !mc.world.getBlockState(pos).isOf(Blocks.POWDER_SNOW);
    }
}
