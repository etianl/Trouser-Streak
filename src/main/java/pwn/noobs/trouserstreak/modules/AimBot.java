//skidded from SpearKill by etianl
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import pwn.noobs.trouserstreak.Trouser;

import java.util.Comparator;
import java.util.List;

public class AimBot extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> targetPlayers = sgGeneral.add(new BoolSetting.Builder()
            .name("TargetOnlyPlayers")
            .description("Only lock onto Player entities. False only targets LivingEntity.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> seek = sgGeneral.add(new BoolSetting.Builder()
            .name("Seek Target")
            .description("Waits until you have crosshair on a target to lock on.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Double> maxrange = sgGeneral.add(new DoubleSetting.Builder()
            .name("Targeting range")
            .description("How far in blocks that targeting is allowed.")
            .defaultValue(256)
            .min(0)
            .sliderRange(0,512)
            .build()
    );
    public AimBot() {
        super(Trouser.Main, "Aim Bot", "Locks onto the targeted entity while the module is on.");
    }

    private Entity crosshairTarget;
    @Override
    public void onActivate() {
        crosshairTarget = target();
    }
    @Override
    public void onDeactivate() {
        crosshairTarget = null;
    }
    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (seek.get() && crosshairTarget == null) crosshairTarget = target();
        if (crosshairTarget != null && !crosshairTarget.isAlive()) crosshairTarget = null;
        if (crosshairTarget == null || !(crosshairTarget instanceof LivingEntity)) return;
        if (targetPlayers.get() && !(crosshairTarget instanceof PlayerEntity)) return;

        Vec3d playerPos = mc.player.getEyePos();
        Vec3d targetPos = crosshairTarget.getBoundingBox().getCenter();
        Vec3d toTarget = targetPos.subtract(playerPos).normalize();

        float yaw = (float) (Math.toDegrees(Math.atan2(toTarget.z, toTarget.x)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.asin(toTarget.y));

        mc.player.setYaw(yaw);
        mc.player.setHeadYaw(yaw);
        mc.player.setPitch(pitch);
    }

    private Entity target() {
        if (mc.player == null || mc.world == null) return null;
        if (mc.crosshairTarget instanceof EntityHitResult hit) {
            if (!targetPlayers.get() || hit.getEntity() instanceof PlayerEntity) return hit.getEntity();
        }

        double maxRange = maxrange.get();
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d lookVec = mc.player.getRotationVec(1.0f);

        HitResult blockHit = mc.world.raycast(new RaycastContext(eyePos,
                eyePos.add(lookVec.multiply(maxRange)), RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, mc.player));
        double rayLength = blockHit.getType() == HitResult.Type.MISS ? maxRange :
                eyePos.distanceTo(blockHit.getPos());

        List<Entity> candidates = mc.world.getOtherEntities(mc.player,
                mc.player.getBoundingBox().stretch(lookVec.multiply(rayLength)),
                e -> e instanceof LivingEntity && e.isAlive() && e != mc.player);

        candidates.sort(Comparator.comparingDouble(e ->
                eyePos.squaredDistanceTo(e.getBoundingBox().getCenter())));

        double coneAngle = 0.999;
        for (Entity e : candidates) {
            double dist = eyePos.distanceTo(e.getBoundingBox().getCenter());
            if (dist > maxRange) break;

            if (targetPlayers.get() && !(e instanceof PlayerEntity)) continue;

            if (!canSeeTarget(e)) continue;

            Vec3d toEntity = e.getBoundingBox().getCenter().subtract(eyePos).normalize();

            if (lookVec.dotProduct(toEntity) > coneAngle) {
                return e;
            }
        }
        return null;
    }
    private boolean canSeeTarget(Entity target) {
        if (mc.player == null || mc.world == null) return false;

        Vec3d eyePos = mc.player.getEyePos();
        Vec3d targetCenter = target.getBoundingBox().getCenter();

        HitResult result = mc.world.raycast(new RaycastContext(
                eyePos,
                targetCenter,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));

        if (result.getType() == HitResult.Type.MISS) return true;
        return eyePos.distanceTo(result.getPos()) >= eyePos.distanceTo(targetCenter) - 0.5;
    }
}