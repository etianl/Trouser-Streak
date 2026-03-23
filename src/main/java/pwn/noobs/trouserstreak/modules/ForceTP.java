//written by etianl
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TypedEntityData;
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
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.world.RaycastContext;
import pwn.noobs.trouserstreak.Trouser;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class ForceTP extends Module {
    private final SettingGroup uuidOptions = settings.createGroup("UUID Options. Right Click entity to get UUID, or choose playername.");
    private final SettingGroup pearlOptions = settings.createGroup("Pearl Options.");

    private enum uuidCollectionModes {
        RightClick,
        AllPlayers,
        CustomUUID
    }
    private final Setting<uuidCollectionModes> uuidCollectionMode = uuidOptions.add(new EnumSetting.Builder<uuidCollectionModes>()
            .name("uuid-collection-mode")
            .description("How to get the uuid. RightClick = grab uuid of aimed at entity, AllPlayers = All players online, CustomUUID = Pick a UUID or name.")
            .defaultValue(uuidCollectionModes.RightClick)
            .build());
    private final Setting<Boolean> allPlayersAndSelf = uuidOptions.add(new BoolSetting.Builder()
            .name("All Players Include Self")
            .description("Spawn pearls for all players in tab list and yourself as well.")
            .defaultValue(false)
            .visible(() -> uuidCollectionMode.get() == uuidCollectionModes.AllPlayers)
            .build()
    );
    private final Setting<Boolean> ignorefrend = uuidOptions.add(new BoolSetting.Builder()
            .name("All Players Ignore Friends")
            .description("Spawn pearls for all players in tab list excluding friends.")
            .defaultValue(false)
            .visible(() -> uuidCollectionMode.get() == uuidCollectionModes.AllPlayers)
            .build()
    );
    private final Setting<Boolean> onlyliving = uuidOptions.add(new BoolSetting.Builder()
            .name("Only Living Entities")
            .description("Only grab uuid of living entity when right clicking.")
            .defaultValue(true)
            .visible(() -> uuidCollectionMode.get() == uuidCollectionModes.RightClick)
            .build()
    );
    private final Setting<Keybind> resetUUID = uuidOptions.add(new KeybindSetting.Builder()
            .name("reset-uuid-keybind")
            .description("Keybind used to reset the uuid to null.")
            .visible(() -> uuidCollectionMode.get() == uuidCollectionModes.RightClick)
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
            .visible(() -> uuidCollectionMode.get() == uuidCollectionModes.CustomUUID)
            .build());
    private final Setting<String> customPlayerName = uuidOptions.add(new StringSetting.Builder()
            .name("Player Name")
            .description("Player name to get UUID from")
            .defaultValue("")
            .visible(() -> uuidMode.get() == uuidModes.PlayerName && uuidCollectionMode.get() == uuidCollectionModes.CustomUUID)
            .build()
    );
    private final Setting<String> customuuid = uuidOptions.add(new StringSetting.Builder()
            .name("Custom UUID")
            .defaultValue("")
            .visible(() -> uuidMode.get() == uuidModes.UUID && uuidCollectionMode.get() == uuidCollectionModes.CustomUUID)
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
    private final Setting<Boolean> clicksummon = pearlOptions.add(new BoolSetting.Builder()
            .name("Left Click Summon")
            .description("Summon pearls when you click. Other wise use a tick delay")
            .defaultValue(true)
            .build()
    );
    private final Setting<Integer> tickdelay = pearlOptions.add(new IntSetting.Builder()
            .name("TickDelay")
            .description("How many ticks between summoning pearls.")
            .defaultValue(2)
            .min(0)
            .sliderMax(20)
            .visible(() -> !clicksummon.get())
            .build()
    );
    public ForceTP() {
        super(Trouser.operator, "ForceTP", "Spawns ender pearls to teleport your target. Requires Creative mode.");
    }

    private int aticks = 0;
    private int ticks = 0;
    private UUID entityUUID = null;

    @Override
    public void onActivate() {
        entityUUID = null;
        aticks = 0;
        ticks = 0;
        if (!clicksummon.get() && mc.currentScreen == null) {
            spawnPearlAtTarget();
        }
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        if (!mc.player.getAbilities().creativeMode) {
            error("You need to be in creative mode.");
            toggle();
            return;
        }

        if (clicksummon.get() && auto.get() && mc.options.attackKey.isPressed() && mc.currentScreen == null) {
            if (aticks <= atickdelay.get()) {
                aticks++;
            } else {
                spawnPearlAtTarget();
                aticks = 0;
            }
        }
        if (!clicksummon.get() && mc.currentScreen == null) {
            if (ticks <= tickdelay.get()) {
                ticks++;
            } else {
                spawnPearlAtTarget();
                ticks = 0;
            }
        }
    }

    @EventHandler
    private void onMouseButton(MouseClickEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (clicksummon.get() && mc.options.attackKey.isPressed() && mc.currentScreen == null && mc.player.getAbilities().creativeMode) {
            spawnPearlAtTarget();
        }
        if (uuidCollectionMode.get() == uuidCollectionModes.RightClick && mc.options.useKey.isPressed() && mc.currentScreen == null) {
            Entity targetEntity = target();
            if (targetEntity != null && targetEntity.isAlive() && targetEntity != mc.player) {
                entityUUID = targetEntity.getUuid();
                if (chatFeedback)info("Target entity UUID saved: " + targetEntity.getName().getString() + ". UUID: " + targetEntity.getUuid());
            }
        }
        if (uuidCollectionMode.get() == uuidCollectionModes.RightClick && resetUUID.get().isPressed() && mc.currentScreen == null){
            if (chatFeedback)info("Resetting saved entity.");
            entityUUID = null;
        }
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

        if (uuidCollectionMode.get() == uuidCollectionModes.AllPlayers) {
            if (mc.getNetworkHandler() != null) {
                for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
                    if (!allPlayersAndSelf.get() && entry.getProfile().id().equals(mc.player.getUuid())) continue;
                    if (ignorefrend.get() && Friends.get().isFriend(entry)) continue;

                    switch (targetplayeruuidMode.get()){
                        case PlayerName -> {
                            if (entry.getProfile().name().equals(targetplayercustomPlayerName.get())) continue;
                        }
                        case UUID -> {
                            if (entry.getProfile().id().toString().equals(targetplayercustomuuid.get())) continue;
                        }
                    }

                    entityUUID = entry.getProfile().id();
                    spawnSinglePearl(rst, bhr);
                }
            }
        } else {
            spawnSinglePearl(rst, bhr);
        }
    }
    private void spawnSinglePearl(ItemStack rst, BlockHitResult bhr) {
        UUID entityuuid = null;
        if (uuidCollectionMode.get() == uuidCollectionModes.CustomUUID) {
            switch (uuidMode.get()){
                case PlayerName -> {
                    if (!customPlayerName.get().isEmpty()) {
                        entityuuid = getPlayerUUIDFromName(customPlayerName.get());
                    } else {
                        if (chatFeedback)warning("No valid UUID");
                        return;
                    }
                }
                case UUID -> {
                    if (!customuuid.get().isEmpty()) {
                        try {
                            entityuuid = UUID.fromString(customuuid.get());
                        } catch (IllegalArgumentException e) {
                            if (chatFeedback)warning("No valid UUID");
                            return;
                        }
                    } else {
                        if (chatFeedback)warning("No valid UUID");
                        return;
                    }
                }
            }
            if (entityuuid == null){
                if (chatFeedback)warning("No valid UUID");
                return;
            }
        } else if (entityUUID != null) {
            entityuuid = entityUUID;
        } else {
            if (chatFeedback)warning("No target UUID");
            return;
        }
        ItemStack item = new ItemStack(Items.BEE_SPAWN_EGG);
        var changes = ComponentChanges.builder()
                .add(DataComponentTypes.ENTITY_DATA, createEnderPearlData(entityuuid))
                .build();
        item.applyChanges(changes);

        mc.interactionManager.clickCreativeStack(item, 36 + mc.player.getInventory().selectedSlot);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
        mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
    }
    private TypedEntityData<EntityType<?>> createEnderPearlData(UUID entityuuid) {
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

        if (entityuuid != null) {
            long most = entityuuid.getMostSignificantBits();
            long least = entityuuid.getLeastSignificantBits();

            int[] ownerInts = {
                    (int) (most >> 32),
                    (int) most,
                    (int) (least >> 32),
                    (int) least
            };

            entityTag.putIntArray("Owner", ownerInts);
        }

        entityTag.putString("id", "minecraft:ender_pearl");

        Identifier entityId = Identifier.tryParse("minecraft:ender_pearl");
        EntityType<?> entityType = Registries.ENTITY_TYPE.get(entityId);
        if (entityType == null) entityType = EntityType.ENDER_PEARL;

        return TypedEntityData.create(entityType, entityTag);
    }
    private UUID getPlayerUUIDFromName(String playerName) {
        if (mc.getNetworkHandler() != null) {
            for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
                if (entry.getProfile().name().equalsIgnoreCase(playerName)) {
                    UUID uuid = entry.getProfile().id();
                    return uuid;
                }
            }
        }

        for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            if (player.getGameProfile().name().equalsIgnoreCase(playerName)) {
                return player.getUuid();
            }
        }

        if (mc.player != null && mc.player.getGameProfile().name().equalsIgnoreCase(playerName)) {
            return mc.player.getUuid();
        }

        if (chatFeedback)warning("Player '" + playerName + "' not found anywhere.");
        return null;
    }
}