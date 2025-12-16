package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import pwn.noobs.trouserstreak.Trouser;

import java.util.Comparator;
import java.util.List;

public class SpearKill extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> spearcheattargetPlayers = sgGeneral.add(new BoolSetting.Builder()
            .name("SpearOnlyPlayers")
            .description("Only lock onto Player entities. False only targets LivingEntity.")
            .defaultValue(true)
            .build());
    public final Setting<Double> getLungeStrength = sgGeneral.add(new DoubleSetting.Builder()
            .name("SpearVelocity")
            .description("The amount of velocity applied to the player, in the direction of the target.")
            .defaultValue(5)
            .min(0)
            .sliderRange(1,10)
            .build()
    );
    public SpearKill() {
        super(Trouser.Main, "SpearKill", "Locks onto the target and lunges in their direction with a set velocity.");
    }

    private int spearticks = 0;
    private Entity crosshairTarget;
    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (isHoldingSpear() && mc.options.useKey.isPressed()) {
            spearticks++;
            if (crosshairTarget == null) crosshairTarget = target();
            if (crosshairTarget != null && !crosshairTarget.isAlive()) crosshairTarget = null;
            if (crosshairTarget == null || !(crosshairTarget instanceof LivingEntity)) return;
            if (spearcheattargetPlayers.get() && !(crosshairTarget instanceof PlayerEntity)) return;

            Vec3d playerPos = mc.player.getEyePos();
            Vec3d targetPos = crosshairTarget.getBoundingBox().getCenter();
            Vec3d toTarget = targetPos.subtract(playerPos).normalize();

            float yaw = (float) (Math.toDegrees(Math.atan2(toTarget.z, toTarget.x)) - 90.0);
            float pitch = (float) -Math.toDegrees(Math.asin(toTarget.y));

            mc.player.setYaw(yaw);
            mc.player.setHeadYaw(yaw);
            mc.player.setPitch(pitch);

            if (spearticks >= 10) {
                double lungeSpeed = getLungeStrength.get();
                Vec3d viewDir = Vec3d.fromPolar(mc.player.getPitch(), mc.player.getYaw());
                mc.player.setSprinting(true);
                mc.player.setVelocity(viewDir.multiply(lungeSpeed));
            }
        } else {
            spearticks = 0;
            crosshairTarget = null;
        }
    }
    private boolean isHoldingSpear() {
        return mc.player.isHolding(Items.WOODEN_SPEAR) ||
                mc.player.isHolding(Items.STONE_SPEAR) ||
                mc.player.isHolding(Items.COPPER_SPEAR) ||
                mc.player.isHolding(Items.GOLDEN_SPEAR) ||
                mc.player.isHolding(Items.IRON_SPEAR) ||
                mc.player.isHolding(Items.DIAMOND_SPEAR) ||
                mc.player.isHolding(Items.NETHERITE_SPEAR);
    }

    private Entity target() {
        if (mc.player == null || mc.world == null) return null;

        double maxRange = 256.0;
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
            if (eyePos.distanceTo(e.getBoundingBox().getCenter()) > rayLength) break;
            Vec3d toEntity = e.getBoundingBox().getCenter().subtract(eyePos).normalize();
            if (lookVec.dotProduct(toEntity) > coneAngle &&
                    (!spearcheattargetPlayers.get() || e instanceof PlayerEntity)) {
                return e;
            }
        }
        return null;
    }
}