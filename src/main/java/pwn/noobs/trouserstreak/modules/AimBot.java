//skidded from SpearKill by etianl
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
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
    private final Setting<Boolean> onlyItem = sgGeneral.add(new BoolSetting.Builder()
            .name("Only if holding Item/s.")
            .description("Only aimbot if one of the selected items is in your Main Hand.")
            .defaultValue(false)
            .build()
    );
    private final Setting<List<Item>> items = sgGeneral.add(new ItemListSetting.Builder()
            .name("items")
            .description("Only aimbot if one of the selected items is in your Main Hand.")
            .visible(onlyItem::get)
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
    private void onRender(Render3DEvent event) {
        if (mc.player == null || mc.level == null) return;
        if (onlyItem.get() && !items.get().contains(mc.player.getMainHandItem().getItem())) return;
        if (seek.get() && crosshairTarget == null) crosshairTarget = target();
        if (crosshairTarget != null && !crosshairTarget.isAlive()) crosshairTarget = null;
        if (crosshairTarget == null || !(crosshairTarget instanceof LivingEntity)) return;
        if (targetPlayers.get() && !(crosshairTarget instanceof Player)) return;

        Vec3 playerPos = mc.player.getEyePosition();
        Vec3 targetPos = crosshairTarget.getBoundingBox().getCenter();
        Vec3 toTarget = targetPos.subtract(playerPos).normalize();

        float yaw = (float) (Math.toDegrees(Math.atan2(toTarget.z, toTarget.x)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.asin(toTarget.y));

        mc.player.setYRot(yaw);
        mc.player.setYHeadRot(yaw);
        mc.player.setXRot(pitch);
    }

    private Entity target() {
        if (mc.player == null || mc.level == null) return null;
        if (mc.hitResult instanceof EntityHitResult hit) {
            if (!targetPlayers.get() || hit.getEntity() instanceof Player) return hit.getEntity();
        }

        double maxRange = maxrange.get();
        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 lookVec = mc.player.getViewVector(1.0f);

        HitResult blockHit = mc.level.clip(new ClipContext(eyePos,
                eyePos.add(lookVec.scale(maxRange)), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, mc.player));
        double rayLength = blockHit.getType() == HitResult.Type.MISS ? maxRange :
                eyePos.distanceTo(blockHit.getLocation());

        List<Entity> candidates = mc.level.getEntities(mc.player,
                mc.player.getBoundingBox().expandTowards(lookVec.scale(rayLength)),
                e -> e instanceof LivingEntity && e.isAlive() && e != mc.player);

        candidates.sort(Comparator.comparingDouble(e ->
                eyePos.distanceToSqr(e.getBoundingBox().getCenter())));

        double coneAngle = 0.999;
        for (Entity e : candidates) {
            double dist = eyePos.distanceTo(e.getBoundingBox().getCenter());
            if (dist > maxRange) break;

            if (targetPlayers.get() && !(e instanceof Player)) continue;

            if (!canSeeTarget(e)) continue;

            Vec3 toEntity = e.getBoundingBox().getCenter().subtract(eyePos).normalize();

            if (lookVec.dot(toEntity) > coneAngle) {
                return e;
            }
        }
        return null;
    }
    private boolean canSeeTarget(Entity target) {
        if (mc.player == null || mc.level == null) return false;

        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 targetCenter = target.getBoundingBox().getCenter();

        HitResult result = mc.level.clip(new ClipContext(
                eyePos,
                targetCenter,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                mc.player
        ));

        if (result.getType() == HitResult.Type.MISS) return true;
        return eyePos.distanceTo(result.getLocation()) >= eyePos.distanceTo(targetCenter) - 0.5;
    }
}