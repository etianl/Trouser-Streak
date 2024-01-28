package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.RaycastContext;
import pwn.noobs.trouserstreak.Trouser;

import java.util.HashSet;
import java.util.Set;

public class LavaAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Set<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Entities to Lava.")
            .onlyAttackable()
            .defaultValue(EntityType.PLAYER, EntityType.VILLAGER)
            .build()
    );
    public final Setting<Boolean> trollfriends = sgGeneral.add(new BoolSetting.Builder()
            .name("Lava Friends")
            .description("Lava bucket your friends too")
            .defaultValue(false)
            .build()
    );
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("Lava placement range.")
            .defaultValue(6)
                    .min(2)
            .sliderRange(2, 10)
            .build()
    );
    public final Setting<Boolean> pickup = sgGeneral.add(new BoolSetting.Builder()
            .name("Pickup Lava")
            .description("pickup lava after placing")
            .defaultValue(true)
            .build()
    );
    public final Setting<Integer> pickuptickdelay = sgGeneral.add(new IntSetting.Builder()
            .name("Lava Pickup Tick Delay")
            .description("Tick Delay for lava pickup")
            .defaultValue(1)
            .min(0)
            .sliderMax(20)
            .visible(() -> pickup.get())
            .build()
    );

    private final Setting<Boolean> lavaeverything = sgGeneral.add(new BoolSetting.Builder()
            .name("Lava-Everything")
            .description("Lava all the blocks. Creative mode recommended.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> pauseOnLag = sgGeneral.add(new BoolSetting.Builder()
            .name("pause-on-lag")
            .description("Pauses if the server is lagging.")
            .defaultValue(true)
            .build()
    );

    public LavaAura() {
        super(Trouser.Main, "lava-aura", "Places lava buckets around you repeatedly.");
    }
    private Set<BlockPos> lavaPlaced = new HashSet<>();
    private int ticks = 0;
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!mc.player.isAlive() || PlayerUtils.getGameMode() == GameMode.SPECTATOR) return;
        if (pauseOnLag.get() && TickRate.INSTANCE.getTimeSinceLastTick() >= 1f) return;
        float originalYaw = mc.player.getYaw();
        float originalPitch = mc.player.getPitch();
        for (Entity entity : this.mc.world.getEntities()) {
            if (entity instanceof LivingEntity && entity != mc.player) {
                if (!entities.get().contains(entity.getType()) || (!trollfriends.get() && entity instanceof PlayerEntity && Friends.get().isFriend((PlayerEntity) entity))) continue;
                LivingEntity livingEntity = (LivingEntity) entity;
                Vec3d targetPos = livingEntity.getPos();

                double distance = mc.player.getPos().distanceTo(entity.getPos());
                BlockHitResult blockHitResult = mc.world.raycast(new RaycastContext(
                        mc.player.getCameraPosVec(1.0f),
                        targetPos.add(0,0.05,0),
                        RaycastContext.ShapeType.COLLIDER,
                        RaycastContext.FluidHandling.ANY,
                        mc.player
                ));

                if (blockHitResult.getType() == HitResult.Type.MISS) {
                    if (distance <= range.get()) {
                        //small area around player to not lava
                        BlockPos targetBlockPos = BlockPos.ofFloored(targetPos);
                        BlockPos targetBlockPos2 = BlockPos.ofFloored(targetPos).add(0,1,0);
                        BlockPos targetBlockPos3 = BlockPos.ofFloored(targetPos).add(0,-1,0);
                        BlockPos targetBlockPos4 = BlockPos.ofFloored(targetPos).add(0,2,0);
                        BlockPos targetBlockPos5 = BlockPos.ofFloored(targetPos).add(1,1,0);
                        BlockPos targetBlockPos6 = BlockPos.ofFloored(targetPos).add(0,1,1);
                        BlockPos targetBlockPos7 = BlockPos.ofFloored(targetPos).add(-1,1,0);
                        BlockPos targetBlockPos8 = BlockPos.ofFloored(targetPos).add(0,1,-1);
                        BlockPos targetBlockPos9 = BlockPos.ofFloored(targetPos).add(1,1,1);
                        BlockPos targetBlockPos10 = BlockPos.ofFloored(targetPos).add(-1,1,-1);
                        BlockPos targetBlockPos11 = BlockPos.ofFloored(targetPos).add(1,1,-1);
                        BlockPos targetBlockPos12 = BlockPos.ofFloored(targetPos).add(-1,1,1);
                        BlockPos targetBlockPos13 = BlockPos.ofFloored(targetPos).add(1,0,0);
                        BlockPos targetBlockPos14 = BlockPos.ofFloored(targetPos).add(0,0,1);
                        BlockPos targetBlockPos15 = BlockPos.ofFloored(targetPos).add(-1,0,0);
                        BlockPos targetBlockPos16 = BlockPos.ofFloored(targetPos).add(0,0,-1);
                        BlockPos targetBlockPos17 = BlockPos.ofFloored(targetPos).add(1,0,1);
                        BlockPos targetBlockPos18 = BlockPos.ofFloored(targetPos).add(-1,0,-1);
                        BlockPos targetBlockPos19 = BlockPos.ofFloored(targetPos).add(1,0,-1);
                        BlockPos targetBlockPos20 = BlockPos.ofFloored(targetPos).add(-1,0,1);
                        BlockPos targetBlockPos21 = BlockPos.ofFloored(targetPos).add(1,-1,0);
                        BlockPos targetBlockPos22 = BlockPos.ofFloored(targetPos).add(0,-1,1);
                        BlockPos targetBlockPos23 = BlockPos.ofFloored(targetPos).add(-1,-1,0);
                        BlockPos targetBlockPos24 = BlockPos.ofFloored(targetPos).add(0,-1,-1);
                        BlockPos targetBlockPos25 = BlockPos.ofFloored(targetPos).add(1,-1,1);
                        BlockPos targetBlockPos26 = BlockPos.ofFloored(targetPos).add(-1,-1,-1);
                        BlockPos targetBlockPos27 = BlockPos.ofFloored(targetPos).add(1,-1,-1);
                        BlockPos targetBlockPos28 = BlockPos.ofFloored(targetPos).add(-1,-1,1);


                        if (!targetBlockPos.equals(mc.player.getBlockPos()) &&
                                !targetBlockPos2.equals(mc.player.getBlockPos()) &&
                                !targetBlockPos3.equals(mc.player.getBlockPos()) &&
                                !targetBlockPos4.equals(mc.player.getBlockPos()) &&
                                !targetBlockPos5.equals(mc.player.getBlockPos()) &&
                                !targetBlockPos6.equals(mc.player.getBlockPos()) &&
                                !targetBlockPos7.equals(mc.player.getBlockPos()) &&
                                !targetBlockPos8.equals(mc.player.getBlockPos()) &&
                                !targetBlockPos9.equals(mc.player.getBlockPos()) &&
                                !targetBlockPos10.equals(mc.player.getBlockPos()) &&
                                !targetBlockPos11.equals(mc.player.getBlockPos()) &&
                                !targetBlockPos12.equals(mc.player.getBlockPos()) &&
                                !targetBlockPos13.equals(mc.player.getBlockPos()) &&
                                !targetBlockPos14.equals(mc.player.getBlockPos()) &&
                                !targetBlockPos15.equals(mc.player.getBlockPos()) &&
                                !targetBlockPos16.equals(mc.player.getBlockPos()) &&
                                !targetBlockPos17.equals(mc.player.getBlockPos()) &&
                                !targetBlockPos18.equals(mc.player.getBlockPos()) &&
                                !targetBlockPos19.equals(mc.player.getBlockPos()) &&
                                !targetBlockPos20.equals(mc.player.getBlockPos()) &&
                                !targetBlockPos21.equals(mc.player.getBlockPos()) &&
                                !targetBlockPos22.equals(mc.player.getBlockPos()) &&
                                !targetBlockPos23.equals(mc.player.getBlockPos()) &&
                                !targetBlockPos24.equals(mc.player.getBlockPos()) &&
                                !targetBlockPos25.equals(mc.player.getBlockPos()) &&
                                !targetBlockPos26.equals(mc.player.getBlockPos()) &&
                                !targetBlockPos27.equals(mc.player.getBlockPos()) &&
                                !targetBlockPos28.equals(mc.player.getBlockPos())
                        ) {
                            if (mc.world.getBlockState(targetBlockPos).getBlock() != Blocks.WATER && mc.world.getBlockState(targetBlockPos).getBlock() != Blocks.LAVA) {
                                mc.player.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, targetPos.add(0,0.05,0));
                                placeLava();
                            }
                        }
                    }
                }
            }
        }

        if (lavaeverything.get()) {
            BlockPos playerPos = mc.player.getBlockPos();

            for (int x = (int) -Math.round(range.get()); x <= range.get(); x++) {
                for (int y = (int) -Math.round(range.get()); y <= range.get(); y++) {
                    for (int z = (int) -Math.round(range.get()); z <= range.get(); z++) {
                        BlockPos blockPos = playerPos.add(x, y, z);

                        if (mc.world.getBlockState(blockPos).getBlock() != Blocks.AIR && mc.world.getBlockState(blockPos).getBlock() != Blocks.WATER && mc.world.getBlockState(blockPos).getBlock() != Blocks.LAVA) {
                            // Check if the block has not had lava placed on it
                            if (!lavaPlaced.contains(blockPos)) {
                                mc.player.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, new Vec3d(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
                                placeLava();

                                // Add the block to the set to indicate that lava has been placed on it
                                lavaPlaced.add(blockPos);
                            }
                        }
                    }
                }
            }
        }
        if (pickup.get() && !lavaeverything.get()){
            if (ticks<pickuptickdelay.get()){
                ticks++;
            } else if (ticks>=pickuptickdelay.get()){
                pickUpLavaOnTick();
                ticks=0;
            }
        }
        mc.player.setYaw(originalYaw);
        mc.player.setPitch(originalPitch);
    }
    private void placeLava() {
        FindItemResult findItemResult = InvUtils.findInHotbar(Items.LAVA_BUCKET);
        if (!findItemResult.found()) {
            return;
        }
        int prevSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = findItemResult.slot();
        mc.interactionManager.interactItem(mc.player,Hand.MAIN_HAND);
        mc.player.getInventory().selectedSlot = prevSlot;
    }
    private void pickUpLavaOnTick() {
        BlockPos playerPos = mc.player.getBlockPos();

        for (int x = (int) -Math.round(range.get()); x <= range.get(); x++) {
            for (int y = (int) -Math.round(range.get()); y <= range.get(); y++) {
                for (int z = (int) -Math.round(range.get()); z <= range.get(); z++) {
                    BlockPos blockPos = playerPos.add(x, y, z);
                    BlockState blockState = mc.world.getBlockState(blockPos);

                    if (blockState.getBlock() == Blocks.LAVA) {
                        // Perform a raycast to check for obstructions
                        BlockHitResult blockHitResult = mc.world.raycast(new RaycastContext(
                                mc.player.getCameraPosVec(1.0f),
                                new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5),
                                RaycastContext.ShapeType.COLLIDER,
                                RaycastContext.FluidHandling.NONE,
                                mc.player
                        ));

                        if (blockHitResult.getType() == HitResult.Type.MISS) {
                            mc.player.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, new Vec3d(blockPos.getX(), blockPos.getY()+0.25, blockPos.getZ()));
                            pickupLiquid();
                        }
                    }
                }
            }
        }
    }
    private void pickupLiquid() {
        FindItemResult findItemResult = InvUtils.findInHotbar(Items.BUCKET);
        if (!findItemResult.found()) {
            return;
        }
        int prevSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = findItemResult.slot();
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        mc.player.getInventory().selectedSlot = prevSlot;
    }

}
