package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.Cushion;
import net.minecraft.world.item.CushionItem;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.*;
import pwn.noobs.trouserstreak.Trouser;

import java.util.List;

import static com.mojang.blaze3d.platform.InputConstants.KEY_W;

public class CushionHopper extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Keybind> activationKey = sgGeneral.add(new KeybindSetting.Builder()
            .name("activation-key")
            .description("Key that must be held to place and use cushions.")
            .defaultValue(Keybind.fromKey(KEY_W))
            .build()
    );
    private final Setting<Integer> tickDelay = sgGeneral.add(new IntSetting.Builder()
            .name("tick-delay")
            .description("Delay in ticks between placing/using cushions.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 20)
            .build()
    );
    private final Setting<Boolean> breakCushions = sgGeneral.add(new BoolSetting.Builder()
            .name("break-cushions")
            .description("Breaks the cushion used before the most recently used cushion.")
            .defaultValue(true)
            .build()
    );

    private Cushion lastUsedCushion;
    private Cushion previousUsedCushion;
    private BlockPos lastPlacePos;
    private int placeDelayCounter;
    private int useDelayCounter;
    public CushionHopper() {
        super(Trouser.Main, "CushionHopper", "Places cushions while holding forward, uses looked-at cushions, and optionally breaks the prior used cushions.");
    }

    @Override
    public void onActivate() {
        lastUsedCushion = null;
        previousUsedCushion = null;
        lastPlacePos = null;
        placeDelayCounter = 0;
        useDelayCounter = 0;
    }

    @Override
    public void onDeactivate() {
        lastUsedCushion = null;
        previousUsedCushion = null;
        lastPlacePos = null;
        placeDelayCounter = 0;
        useDelayCounter = 0;
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (!activationKey.get().isPressed()) return;

        if (placeDelayCounter < tickDelay.get()) {
            placeDelayCounter++;
            return;
        }

        InteractionHand hand = InteractionHand.MAIN_HAND;

        if (!(mc.player.getItemInHand(hand).getItem() instanceof CushionItem)) return;
        if (!(mc.hitResult instanceof BlockHitResult hit)) return;

        if (hit.getType() != HitResult.Type.BLOCK) return;

        BlockPos targetPos = hit.getBlockPos();
        if (targetPos.equals(lastPlacePos)) return;

        BlockHitResult aimedHit = new BlockHitResult(
                hit.getLocation(),
                hit.getDirection(),
                targetPos,
                false
        );

        InteractionResult result = mc.gameMode.useItemOn(
                mc.player,
                hand,
                aimedHit
        );

        if (!result.consumesAction()) {
            BlockHitResult upHit = new BlockHitResult(
                    hit.getLocation(),
                    Direction.UP,
                    targetPos,
                    false
            );

            result = mc.gameMode.useItemOn(
                    mc.player,
                    hand,
                    upHit
            );
        }

        if (!result.consumesAction()) {
            Vec3 placeLocation = Vec3.atCenterOf(targetPos).add(0, 0.5, 0);

            BlockHitResult topHit = new BlockHitResult(
                    placeLocation,
                    Direction.UP,
                    targetPos,
                    false
            );

            result = mc.gameMode.useItemOn(
                    mc.player,
                    hand,
                    topHit
            );
        }

        if (result.consumesAction()) {
            placeDelayCounter = 0;
            lastPlacePos = targetPos;

            mc.player.swing(hand, SwingAnimation.DEFAULT, true);
        }
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (!activationKey.get().isPressed()) return;
        if (useDelayCounter < tickDelay.get()) {
            useDelayCounter++;
            return;
        }

        tryUseLookedAtCushion(InteractionHand.MAIN_HAND);
    }

    private void tryUseLookedAtCushion(InteractionHand hand) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;

        Cushion cushion = findCushionToUse();
        if (cushion == null) return;

        EntityHitResult entityHit = new EntityHitResult(
                cushion,
                cushion.position()
        );

        InteractionResult result = mc.gameMode.interact(
                mc.player,
                cushion,
                entityHit,
                hand
        );

        if (result.consumesAction()) {
            useDelayCounter = 0;
            previousUsedCushion = lastUsedCushion;
            lastUsedCushion = cushion;

            mc.player.swing(hand, SwingAnimation.DEFAULT, true);

            if (breakCushions.get()) {
                breakPreviousUsedCushion(hand);
            }
        }
    }

    private Cushion findCushionToUse() {
        HitResult crosshair = mc.hitResult;

        if (crosshair instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof Cushion directCushion
                && isUsable(directCushion)) {
            return directCushion;
        }

        if (!(crosshair instanceof BlockHitResult blockHit)) return null;
        if (blockHit.getType() != HitResult.Type.BLOCK) return null;

        BlockPos targetPos = blockHit.getBlockPos();

        AABB searchBox = new AABB(
                targetPos.getX() - 1,
                targetPos.getY() - 1,
                targetPos.getZ() - 1,
                targetPos.getX() + 2,
                targetPos.getY() + 3,
                targetPos.getZ() + 2
        );

        List<Cushion> cushions = mc.level.getEntitiesOfClass(
                Cushion.class,
                searchBox,
                cushion -> cushion != null
                        && !cushion.isRemoved()
                        && cushion.isAlive()
                        && cushion.getBoundingBox().intersects(searchBox)
        );

        if (cushions.isEmpty()) return null;

        Vec3 eyePos = mc.player.getEyePosition();

        Cushion nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Cushion candidate : cushions) {
            double distance = candidate.position().distanceToSqr(eyePos);

            if (distance < nearestDistance) {
                nearest = candidate;
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    private boolean isUsable(Cushion cushion) {
        return cushion != null
                && !cushion.isRemoved()
                && cushion.isAlive();
    }

    private void breakPreviousUsedCushion(InteractionHand hand) {
        if (previousUsedCushion == null) return;

        if (previousUsedCushion.isRemoved() || !previousUsedCushion.isAlive()) {
            previousUsedCushion = null;
            return;
        }

        mc.gameMode.attack(mc.player, previousUsedCushion);
        mc.player.swing(hand, SwingAnimation.DEFAULT, true);

        previousUsedCushion = null;
    }
}