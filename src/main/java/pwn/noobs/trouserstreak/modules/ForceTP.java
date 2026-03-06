//written by etianl
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import pwn.noobs.trouserstreak.Trouser;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class ForceTP extends Module {
    private final SettingGroup uuidOptions = settings.createGroup("UUID Options. Right Click entity to get UUID, or choose playername.");
    private final SettingGroup pearlOptions = settings.createGroup("Pearl Options. Left Click to summon a pearl at the location.");
    private final Setting<Boolean> allPlayers = uuidOptions.add(new BoolSetting.Builder()
            .name("All Players")
            .description("Spawn pearls for all players in tab list instead of single target.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> allPlayersAndSelf = uuidOptions.add(new BoolSetting.Builder()
            .name("All Players Include Self")
            .description("Spawn pearls for all players in tab list and yourself as well.")
            .defaultValue(false)
            .visible(() -> allPlayers.get())
            .build()
    );
    private final Setting<Boolean> onlyliving = uuidOptions.add(new BoolSetting.Builder()
            .name("Only Living Entities")
            .description("Only grab uuid of living entity when right clicking.")
            .defaultValue(true)
            .visible(() -> !allPlayers.get())
            .build()
    );
    private final Setting<Boolean> customUUID = uuidOptions.add(new BoolSetting.Builder()
            .name("Custom UUID")
            .description("Use custom player UUID instead of targeted entity")
            .defaultValue(false)
            .visible(() -> !allPlayers.get())
            .build()
    );
    private enum uuidModes {
        PlayerName,
        UUID
    }
    private final Setting<uuidModes> uuidMode = uuidOptions.add(new EnumSetting.Builder<uuidModes>()
            .name("uuid-mode")
            .description("UUID or PlayerName.")
            .defaultValue(uuidModes.PlayerName)
            .visible(() -> customUUID.get() && !allPlayers.get())
            .build());
    private final Setting<String> customPlayerName = uuidOptions.add(new StringSetting.Builder()
            .name("Player Name")
            .description("Player name to get UUID from")
            .defaultValue("")
            .visible(() -> uuidMode.get() == uuidModes.PlayerName && customUUID.get() && !allPlayers.get())
            .build()
    );
    private final Setting<String> customuuid = uuidOptions.add(new StringSetting.Builder()
            .name("Custom UUID")
            .defaultValue("")
            .visible(() -> uuidMode.get() == uuidModes.UUID && customUUID.get() && !allPlayers.get())
            .build()
    );
    private enum tModes {
        OnTarget,
        ToVoid,
        ToPlayer
    }
    private final Setting<tModes> teleportMode = pearlOptions.add(new EnumSetting.Builder<tModes>()
            .name("teleport-mode")
            .description("Where to send the player.")
            .defaultValue(tModes.OnTarget)
            .build());
    private enum targetplayeruuidModes {
        PlayerName,
        UUID
    }
    private final Setting<targetplayeruuidModes> targetplayeruuidMode = pearlOptions.add(new EnumSetting.Builder<targetplayeruuidModes>()
            .name("Target-Player-uuid-mode")
            .description("UUID or PlayerName.")
            .defaultValue(targetplayeruuidModes.PlayerName)
            .visible(() -> teleportMode.get() == tModes.ToPlayer)
            .build());
    private final Setting<String> targetplayercustomPlayerName = pearlOptions.add(new StringSetting.Builder()
            .name("Target Player Name")
            .description("Player name to get UUID from")
            .defaultValue("")
            .visible(() -> targetplayeruuidMode.get() == targetplayeruuidModes.PlayerName && teleportMode.get() == tModes.ToPlayer)
            .build()
    );
    private final Setting<String> targetplayercustomuuid = pearlOptions.add(new StringSetting.Builder()
            .name("Target Player UUID")
            .defaultValue("")
            .visible(() -> targetplayeruuidMode.get() == targetplayeruuidModes.UUID && teleportMode.get() == tModes.ToPlayer)
            .build()
    );
    private final Setting<Double> range = pearlOptions.add(new DoubleSetting.Builder()
            .name("range")
            .description("Max distance to spawn the ender pearl.")
            .defaultValue(512)
            .min(1)
            .sliderMax(512)
            .visible(() -> teleportMode.get() == tModes.OnTarget)
            .build()
    );
    private final Setting<Boolean> fluid = pearlOptions.add(new BoolSetting.Builder()
            .name("Target Fluid")
            .description("Summon pearl on fluid or not.")
            .defaultValue(false)
            .visible(() -> teleportMode.get() == tModes.OnTarget)
            .build()
    );
    private final Setting<Double> velocity = pearlOptions.add(new DoubleSetting.Builder()
            .name("velocity")
            .description("Initial pearl velocity towards target")
            .defaultValue(5)
            .min(0)
            .sliderMax(10)
            .build()
    );
    private final Setting<Boolean> auto = pearlOptions.add(new BoolSetting.Builder()
            .name("FULLAUTO")
            .description("FULL AUTO BABY!")
            .defaultValue(true)
            .build()
    );
    private final Setting<Integer> atickdelay = pearlOptions.add(new IntSetting.Builder()
            .name("FULLAUTOTickDelay")
            .description("How many ticks between summoning pearls.")
            .defaultValue(2)
            .min(0)
            .sliderMax(20)
            .visible(auto::get)
            .build()
    );

    public ForceTP() {
        super(Trouser.operator, "ForceTP", "Spawns ender pearls to teleport your target wherever you click. Requires Creative mode.");
    }

    private int aticks = 0;
    private UUID entityUUID = null;

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        if (!mc.player.getAbilities().creativeMode) {
            error("You need to be in creative mode.");
            toggle();
            return;
        }

        if (auto.get() && mc.options.attackKey.isPressed() && mc.currentScreen == null) {
            if (aticks <= atickdelay.get()) {
                aticks++;
            } else {
                spawnPearlAtTarget();
                aticks = 0;
            }
        }
    }

    @EventHandler
    private void onMouseButton(MouseButtonEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (mc.options.attackKey.isPressed() && mc.currentScreen == null && mc.player.getAbilities().creativeMode) {
            spawnPearlAtTarget();
        }
        if (customUUID.get() && mc.options.useKey.isPressed()) warning("Disable Custom UUID setting to use target's UUID");
        if (!customUUID.get() && mc.options.useKey.isPressed() && mc.currentScreen == null) {
            getEntityUUID();
        }
    }

    private UUID getEntityUUID() {
        Entity targetEntity = target();
        if (targetEntity != null && targetEntity.isAlive() && targetEntity != mc.player) {
            entityUUID = targetEntity.getUuid();
            info("Target entity UUID saved: " + targetEntity.getName().getString() + ". UUID: " + targetEntity.getUuid());
            return entityUUID;
        }

        info("No entity targeted. Resetting saved entity.");
        entityUUID = null;
        return null;
    }
    private Entity target() {
        if (mc.player == null || mc.world == null) return null;
        if (mc.crosshairTarget instanceof EntityHitResult hit) return hit.getEntity();

        double maxRange = 512;
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d lookVec = mc.player.getRotationVec(1.0f);

        HitResult blockHit = mc.world.raycast(new RaycastContext(eyePos,
                eyePos.add(lookVec.multiply(maxRange)), RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, mc.player));
        double rayLength = blockHit.getType() == HitResult.Type.MISS ? maxRange :
                eyePos.distanceTo(blockHit.getPos());

        List<Entity> candidates = mc.world.getOtherEntities(mc.player,
                mc.player.getBoundingBox().stretch(lookVec.multiply(rayLength)),
                e -> (onlyliving.get() && e instanceof LivingEntity && e.isAlive()) || !onlyliving.get());

        candidates.sort(Comparator.comparingDouble(e ->
                eyePos.squaredDistanceTo(e.getBoundingBox().getCenter())));

        double coneAngle = 0.999;
        for (Entity e : candidates) {
            double dist = eyePos.distanceTo(e.getBoundingBox().getCenter());
            if (dist > maxRange) break;

            Vec3d toEntity = e.getBoundingBox().getCenter().subtract(eyePos).normalize();

            if (lookVec.dotProduct(toEntity) > coneAngle) {
                return e;
            }
        }
        return null;
    }
    private void spawnPearlAtTarget() {
        ItemStack rst = mc.player.getMainHandStack();
        BlockHitResult bhr = new BlockHitResult(
                mc.player.getEyePos(),
                Direction.DOWN,
                BlockPos.ofFloored(mc.player.getEyePos()),
                false
        );

        if (allPlayers.get()) {
            if (mc.getNetworkHandler() != null) {
                for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
                    if (!allPlayersAndSelf.get() && entry.getProfile().getId().equals(mc.player.getUuid())) continue;

                    switch (targetplayeruuidMode.get()){
                        case PlayerName -> {
                            if (entry.getProfile().getName().equals(targetplayercustomPlayerName.get())) continue;
                        }
                        case UUID -> {
                            if (entry.getProfile().getId().toString().equals(targetplayercustomuuid.get())) continue;
                        }
                    }

                    entityUUID = entry.getProfile().getId();
                    spawnSinglePearl(rst, bhr);
                }
            }
        } else {
            spawnSinglePearl(rst, bhr);
        }
    }
    private void spawnSinglePearl(ItemStack rst, BlockHitResult bhr) {
        NbtCompound tag = new NbtCompound();

        NbtCompound entityTag = new NbtCompound();

        HitResult hr = mc.getCameraEntity().raycast(range.get(), 0, fluid.get());
        Vec3d hitPos = hr.getPos();

        BlockPos bp = BlockPos.ofFloored(hitPos);
        NbtList posList = new NbtList();
        switch (teleportMode.get()){
            case OnTarget -> {
                posList.add(NbtDouble.of(bp.getX() + 0.5));
                posList.add(NbtDouble.of(bp.getY() + 0.5));
                posList.add(NbtDouble.of(bp.getZ() + 0.5));
            }
            case ToVoid -> {
                posList.add(NbtDouble.of(mc.player.getX()));
                posList.add(NbtDouble.of(mc.world.getBottomY()-1));
                posList.add(NbtDouble.of(mc.player.getZ()));
            }
            case ToPlayer -> {
                AbstractClientPlayerEntity targetPlayer = null;
                for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
                    switch (targetplayeruuidMode.get()) {
                        case PlayerName -> {
                            if (player.getName().getString().equals(targetplayercustomPlayerName.get())) {
                                targetPlayer = player;
                            }
                        }
                        case UUID -> {
                            if (player.getUuid().toString().equals(targetplayercustomuuid.get())) {
                                targetPlayer = player;
                            }
                        }
                    }
                    if (targetPlayer != null) break;
                }
                if (targetPlayer != null) {
                    posList.add(NbtDouble.of(targetPlayer.getX()));
                    posList.add(NbtDouble.of(targetPlayer.getY()));
                    posList.add(NbtDouble.of(targetPlayer.getZ()));
                }
            }
        }
        entityTag.put("Pos", posList);

        Vec3d eyePos = mc.player.getEyePos();
        Vec3d dir = null;
        switch (teleportMode.get()){
            case OnTarget -> {
                dir = hitPos.subtract(eyePos);
                if (dir.lengthSquared() > 0) {
                    dir = dir.normalize().multiply(velocity.get());
                }
            }
            case ToVoid -> dir = new Vec3d(0, velocity.get(), 0);
            case ToPlayer -> dir = new Vec3d(0, -velocity.get(), 0);
        }
        NbtList motionList = new NbtList();
        if (dir != null){
            motionList.add(NbtDouble.of(dir.x));
            motionList.add(NbtDouble.of(dir.y));
            motionList.add(NbtDouble.of(dir.z));
        }
        entityTag.put("Motion", motionList);

        UUID entityuuid = null;
        if (customUUID.get()) {
            switch (uuidMode.get()){
                case PlayerName -> {
                    if (!customPlayerName.get().isEmpty()) {
                        entityuuid = getPlayerUUIDFromName(customPlayerName.get());
                    } else {
                        warning("No valid UUID - using self");
                        entityuuid = mc.player.getUuid();
                    }
                }
                case UUID -> {
                    if (!customuuid.get().isEmpty()) {
                        try {
                            entityuuid = UUID.fromString(customuuid.get());
                        } catch (IllegalArgumentException e) {
                            warning("Invalid custom UUID format: " + customuuid.get());
                            warning("No valid UUID - using self");
                            entityuuid = mc.player.getUuid();
                        }
                    } else {
                        warning("No valid UUID - using self");
                        entityuuid = mc.player.getUuid();
                    }
                }
            }
        } else if (entityUUID != null) {
            entityuuid = entityUUID;
        } else {
            entityuuid = mc.player.getUuid();
            warning("No target UUID - using self");
        }
        long most = entityuuid.getMostSignificantBits();
        long least = entityuuid.getLeastSignificantBits();

        int[] ownerInts = {
                (int) (most >> 32),
                (int) most,
                (int) (least >> 32),
                (int) least
        };

        entityTag.putIntArray("Owner", ownerInts);

        entityTag.putString("id", "minecraft:ender_pearl");

        tag.put("EntityTag", entityTag);

        ItemStack item = new ItemStack(Items.BEE_SPAWN_EGG);
        item.setNbt(tag);

        mc.interactionManager.clickCreativeStack(item, 36 + mc.player.getInventory().selectedSlot);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
        mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
    }
    private UUID getPlayerUUIDFromName(String playerName) {
        if (mc.getNetworkHandler() != null) {
            for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
                if (entry.getProfile().getName().equalsIgnoreCase(playerName)) {
                    UUID uuid = entry.getProfile().getId();
                    info("Found '" + playerName + "' in tab list: " + uuid);
                    return uuid;
                }
            }
        }

        for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            if (player.getGameProfile().getName().equalsIgnoreCase(playerName)) {
                info("Found '" + playerName + "' in world: " + player.getUuid());
                return player.getUuid();
            }
        }

        if (mc.player != null && mc.player.getGameProfile().getName().equalsIgnoreCase(playerName)) {
            return mc.player.getUuid();
        }

        warning("Player '" + playerName + "' not found anywhere - using self UUID");
        return mc.player != null ? mc.player.getUuid() : UUID.randomUUID();
    }
}