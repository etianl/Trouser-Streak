package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import pwn.noobs.trouserstreak.Trouser;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class InfiniteReach extends Module {
    public static InfiniteReach INSTANCE;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgMace = settings.createGroup("Mace Stuff");
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Boolean> swing = sgGeneral.add(
            new BoolSetting.Builder()
                    .name("swing arm")
                    .defaultValue(true)
                    .build()
    );
    public enum Mode {
        Vanilla,
        Paper
    }
    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("Compatibility Mode")
            .description("Vanilla = 22 blocks reach, Paper = 99 blocks reach and able to send more packets")
            .defaultValue(Mode.Vanilla)
            .build()
    );
    private final Setting<Boolean> phoneHome = sgGeneral.add(
            new BoolSetting.Builder()
                    .name("Home Teleport")
                    .description("Brings you back home so you never knew you teleported.")
                    .defaultValue(true)
                    .build()
    );
    private final Setting<Boolean> goUp = sgMace.add(
            new BoolSetting.Builder()
                    .name("Clip up")
                    .description("Clips upward to do a Mace Smash and get around obstacles. There isn't enough packets for this in vanilla mode.")
                    .defaultValue(true)
                    .visible(() -> mode.get() == Mode.Paper)
                    .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("")
            .defaultValue(new SettingColor(255, 0, 0, 40))
            .build()
    );
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("")
            .defaultValue(new SettingColor(255, 0, 0, 120))
            .build()
    );

    public Entity hoveredTarget;
    private double maxDistance = 99.0;

    public InfiniteReach() {
        super(Trouser.Main, "infinite-reach", "Gives you super long arms for attacking things. Lets you Mace Smash at long range in Paper servers.");
        INSTANCE = this;
    }
    @EventHandler
    private void onRender3D(Render3DEvent event) {
        assert mc.player != null;
        if (mode.get() == Mode.Vanilla) maxDistance = 22;
        else maxDistance = 99.0;
        Vec3d cameraPos = mc.player.getCameraPosVec(1.0f);
        Vec3d rotation = mc.player.getRotationVec(1.0f);
        Vec3d endVec = cameraPos.add(rotation.multiply(maxDistance));

        EntityHitResult hit = ProjectileUtil.raycast(
                mc.player,
                cameraPos,
                endVec,
                mc.player.getBoundingBox().expand(maxDistance),
                e -> e.isAlive() && e.isAttackable() && !e.isInvulnerable() && e != mc.player,
                maxDistance * maxDistance
        );

        if (hit != null) {
            hoveredTarget = hit.getEntity();
            double x = MathHelper.lerp(event.tickDelta, hoveredTarget.lastRenderX, hoveredTarget.getX()) - hoveredTarget.getX();
            double y = MathHelper.lerp(event.tickDelta, hoveredTarget.lastRenderY, hoveredTarget.getY()) - hoveredTarget.getY();
            double z = MathHelper.lerp(event.tickDelta, hoveredTarget.lastRenderZ, hoveredTarget.getZ()) - hoveredTarget.getZ();

            Box box = hoveredTarget.getBoundingBox();
            event.renderer.box(x + box.minX, y + box.minY, z + box.minZ, x + box.maxX, y + box.maxY, z + box.maxZ, sideColor.get(), lineColor.get(), ShapeMode.Both, 0);
        } else {
            hoveredTarget = null;
        }
    }

    public void hitEntity(Entity target) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        Vec3d startPos = mc.player.getVehicle() == null
                ? mc.player.getPos()
                : mc.player.getVehicle().getPos();

        Vec3d targetPos = target.getPos();

        Vec3d diff = startPos.subtract(targetPos);

        double flatUp = Math.sqrt(maxDistance * maxDistance - (diff.x * diff.x + diff.z * diff.z));
        double targetUp = flatUp + diff.y;
        int amountOfPackets = 9;
        if (mode.get() == Mode.Vanilla) amountOfPackets = 4;
        for (int i = 0; i < amountOfPackets; i++) moveTo(startPos);

        double yOffset = mc.player.getVehicle() != null
                ? target.getBoundingBox().maxY + 0.3
                : targetPos.y;

        Vec3d insideTarget = new Vec3d(targetPos.x, yOffset, targetPos.z);

        Vec3d finalPos = isValidTeleport(insideTarget)
                ? insideTarget
                : findNearestPos(insideTarget);

        if (finalPos == null) return;
        if (mode.get() == Mode.Paper && goUp.get()){
            moveTo(startPos.add(0, maxDistance, 0));
            moveTo(finalPos.add(0, targetUp, 0));
        }
        moveTo(finalPos);

        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));

        if (swing.get()) {
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            mc.player.swingHand(Hand.MAIN_HAND);
        }
        if (phoneHome.get()){
            if (mode.get() == Mode.Paper && goUp.get()){
                moveTo(finalPos.add(0, targetUp + 0.01, 0));
                moveTo(startPos.add(0, maxDistance + 0.01, 0));
            }
            moveTo(startPos);

            if (mc.player.getVehicle() == null) {
                Vec3d offset = getOffset(startPos);
                if (offset != null) {
                    moveTo(offset);
                }
            }
        }
    }

    private boolean isValidTeleport(Vec3d pos) {
        if (mc.world == null || mc.player == null) return false;

        Box box = mc.player.getBoundingBox().offset(
                pos.x - mc.player.getX(),
                pos.y - mc.player.getY(),
                pos.z - mc.player.getZ()
        );

        if (!mc.world.isSpaceEmpty(box)) return false;

        for (Entity e : mc.world.getOtherEntities(mc.player, box)) {
            if (e.isCollidable()) return false;
        }

        return true;
    }

    private Vec3d findNearestPos(Vec3d desired) {
        if (isValidTeleport(desired)) return desired;

        Vec3d best = null;
        double bestDist = Double.MAX_VALUE;

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                Vec3d test = desired.add(dx, 0, dz);
                if (!isValidTeleport(test)) continue;

                double dist = test.squaredDistanceTo(desired);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = test;
                }
            }
        }
        return best;
    }

    private void moveTo(Vec3d pos) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        if (mc.player.getVehicle() != null) {
            mc.getNetworkHandler().sendPacket(
                    new VehicleMoveC2SPacket(mc.player.getVehicle())
            );
            mc.player.getVehicle().setPosition(pos);
        } else {
            mc.player.setPosition(pos);
            mc.getNetworkHandler().sendPacket(
                    new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, false)
            );
        }
    }

    private Vec3d getOffset(Vec3d base) {
        double dx = 0.01;
        double dy = 0.01;

        Vec3d[] tests = new Vec3d[] {
                base.add( dx, dy,  0),
                base.add(-dx, dy,  0),
                base.add( 0, dy,  dx),
                base.add( 0, dy, -dx)
        };

        for (Vec3d pos : tests) {
            if (isValidTeleport(pos)) return pos;
        }

        info("No offsets");
        return null;
    }
}