package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerInteractEntityC2SPacket;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import pwn.noobs.trouserstreak.Trouser;
import java.util.Objects;

public class MaceKill extends Module {
    private final SettingGroup specialGroup = settings.createGroup("Values higher than 22 only work on Paper/Spigot");
    private final Setting<Integer> fallHeight = specialGroup.add(new IntSetting.Builder()
            .name("Mace Power (Fall height)")
            .description("general attack")
            .defaultValue(23)
            .sliderRange(1, 170)
            .min(1)
            .build());
    private final Setting<Integer> attack1 = specialGroup.add(new IntSetting.Builder()
            .name("First hit")
            .description("expect pop")
            .defaultValue(23)
            .sliderRange(1, 170)
            .min(1)
            .build());
    private final Setting<Integer> attack2 = specialGroup.add(new IntSetting.Builder()
            .name("Second hit")
            .description("expect kill")
            .defaultValue(40)
            .sliderRange(1, 170)
            .min(1)
            .build());
    private final Setting<Integer> debug2 = specialGroup.add(new IntSetting.Builder()
            .name("resetDebug")
            .description("Recommend nine, or find a better value and tell me")
            .defaultValue(9)
            .sliderRange(0, 20)
            .min(0)
            .build());
    private final Setting<Boolean> miss = specialGroup.add(new BoolSetting.Builder()
            .name("anti-totem")
            .description("turn on and grief totem noobs")
            .defaultValue(false)
            .build());
    private final Setting<Boolean> maxPower = specialGroup.add(new BoolSetting.Builder()
            .name("Maximum Mace Power (Paper/Spigot servers only)")
            .description("Simulates a fall from the highest air gap within 170 blocks")
            .defaultValue(false)
            .build());
    private final Setting<Boolean> packetDisable = specialGroup.add(new BoolSetting.Builder()
            .name("Disable When Blocked")
            .description("Does not send movement packets if the attack was blocked. (prevents death)")
            .defaultValue(true)
            .build());

    private Vec3d previouspos;

    public MaceKill() {super(Trouser.Main, "MaceKill", "Makes the Mace more powerful.");}

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (miss.get()) {
            if (event.packet instanceof EntityStatusS2CPacket packet) {
                if (Objects.equals(fallHeight.get(), attack1.get())
                        && packet.getStatus() == EntityStatuses.USE_TOTEM_OF_UNDYING) {
                    fallHeight.set(attack2.get());
                }
            }
            if (!Objects.equals(fallHeight.get(), attack1.get())) {
                if (mc.world != null) {
                    for (PlayerEntity player : mc.world.getPlayers()) {
                        if (player.hurtTime > debug2.get()) {
                            fallHeight.set(attack1.get());
                        }
                    }
                }
            }
        }
    }
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (miss.get()){
            if (!Objects.equals(fallHeight.get(), attack1.get())){
                if (mc.world != null) {
                    for (PlayerEntity player : mc.world.getPlayers()) {
                        if (player.deathTime > 0 || player.isDead()) {
                            fallHeight.set(attack1.get());
                        }
                    }
                }
            }
        }
    }
    //Packet send part by etianl:D
    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (mc.player != null && mc.player.getInventory().getMainHandStack().getItem() == Items.MACE && event.packet instanceof IPlayerInteractEntityC2SPacket packet && packet.meteor$getType() == PlayerInteractEntityC2SPacket.InteractType.ATTACK) {
            try {
                if (packet.meteor$getEntity() instanceof LivingEntity) {
                    LivingEntity targetEntity = (LivingEntity) packet.meteor$getEntity();
                    if (packetDisable.get()
                            && ((targetEntity.isBlocking()
                            && targetEntity.blockedByShield(targetEntity.getRecentDamageSource()))
                            || targetEntity.isInvulnerable()
                            || targetEntity.isInCreativeMode())
                    ) return;
                    previouspos = mc.player.getPos();
                    int blocks = getMaxHeightAbovePlayer();

                    int packetsRequired = (int) Math.ceil(Math.abs(blocks / 10));

                    if (packetsRequired > 20) {
                        packetsRequired = 1;
                    }
                    BlockPos isopenair1 = (mc.player.getBlockPos().add(0, blocks, 0));
                    BlockPos isopenair2 = (mc.player.getBlockPos().add(0, blocks + 1, 0));
                    if (isSafeBlock(isopenair1) && isSafeBlock(isopenair2)) {
                        if (blocks <= 22) {
                            if (mc.player.hasVehicle()) {
                                for (int i = 0; i < 4; i++) {
                                    mc.player.networkHandler.sendPacket(VehicleMoveC2SPacket.fromVehicle(mc.player.getVehicle()));
                                }
                                double maxHeight = Math.min(mc.player.getVehicle().getY() + 22, mc.player.getVehicle().getY() + blocks);
                                mc.player.getVehicle().setPosition(mc.player.getVehicle().getX(), maxHeight + blocks, mc.player.getVehicle().getZ());
                                mc.player.networkHandler.sendPacket(VehicleMoveC2SPacket.fromVehicle(mc.player.getVehicle()));
                                mc.player.getVehicle().setPosition(previouspos);
                                mc.player.networkHandler.sendPacket(VehicleMoveC2SPacket.fromVehicle(mc.player.getVehicle()));
                            } else {
                                for (int i = 0; i < 4; i++) {
                                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(false, mc.player.horizontalCollision));
                                }
                                double maxHeight = Math.min(mc.player.getY() + 22, mc.player.getY() + blocks);
                                PlayerMoveC2SPacket movepacket = new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), maxHeight, mc.player.getZ(), false, mc.player.horizontalCollision);
                                PlayerMoveC2SPacket homepacket = new PlayerMoveC2SPacket.PositionAndOnGround(previouspos.getX(), previouspos.getY(), previouspos.getZ(), false, mc.player.horizontalCollision);
                                ((IPlayerMoveC2SPacket) homepacket).meteor$setTag(1337);
                                ((IPlayerMoveC2SPacket) movepacket).meteor$setTag(1337);
                                mc.player.networkHandler.sendPacket(movepacket);
                                mc.player.networkHandler.sendPacket(homepacket);
                            }
                        } else {
                            if (mc.player.hasVehicle()) {
                                for (int packetNumber = 0; packetNumber < (packetsRequired - 1); packetNumber++) {
                                    mc.player.networkHandler.sendPacket(VehicleMoveC2SPacket.fromVehicle(mc.player.getVehicle()));
                                }
                                double maxHeight = mc.player.getVehicle().getY() + blocks;
                                mc.player.getVehicle().setPosition(mc.player.getVehicle().getX(), maxHeight + blocks, mc.player.getVehicle().getZ());
                                mc.player.networkHandler.sendPacket(VehicleMoveC2SPacket.fromVehicle(mc.player.getVehicle()));
                                mc.player.getVehicle().setPosition(previouspos);
                                mc.player.networkHandler.sendPacket(VehicleMoveC2SPacket.fromVehicle(mc.player.getVehicle()));
                            } else {
                                for (int packetNumber = 0; packetNumber < (packetsRequired - 1); packetNumber++) {
                                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(false, mc.player.horizontalCollision));
                                }
                                double maxHeight = mc.player.getY() + blocks;
                                PlayerMoveC2SPacket movepacket = new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), maxHeight, mc.player.getZ(), false, mc.player.horizontalCollision);
                                PlayerMoveC2SPacket homepacket = new PlayerMoveC2SPacket.PositionAndOnGround(previouspos.getX(), previouspos.getY(), previouspos.getZ(), false, mc.player.horizontalCollision);
                                ((IPlayerMoveC2SPacket) homepacket).meteor$setTag(1337);
                                ((IPlayerMoveC2SPacket) movepacket).meteor$setTag(1337);
                                mc.player.networkHandler.sendPacket(movepacket);
                                mc.player.networkHandler.sendPacket(homepacket);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private int getMaxHeightAbovePlayer() {
        BlockPos playerPos = mc.player.getBlockPos();
        int maxHeight = playerPos.getY() + (maxPower.get() ? 170 : fallHeight.get());

        for (int i = maxHeight; i > playerPos.getY(); i--) {
            BlockPos isopenair1 = new BlockPos(playerPos.getX(), i, playerPos.getZ());
            BlockPos isopenair2 = isopenair1.up(1);
            if (isSafeBlock(isopenair1) && isSafeBlock(isopenair2)) {
                return i - playerPos.getY();
            }
        }
        return 0; // Return 0 if no suitable position is found
    }

    private boolean isSafeBlock(BlockPos pos) {
        return mc.world.getBlockState(pos).isReplaceable()
                && mc.world.getFluidState(pos).isEmpty()
                && !mc.world.getBlockState(pos).isOf(Blocks.POWDER_SNOW);
    }
}
