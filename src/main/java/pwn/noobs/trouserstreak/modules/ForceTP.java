//written by etianl
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
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
        if (!clicksummon.get() && mc.screen == null) {
            spawnPearlAtTarget();
        }
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.level == null) return;

        if (!mc.player.getAbilities().instabuild) {
            error("You need to be in creative mode.");
            toggle();
            return;
        }

        if (clicksummon.get() && auto.get() && mc.options.keyAttack.isDown() && mc.screen == null) {
            if (aticks <= atickdelay.get()) {
                aticks++;
            } else {
                spawnPearlAtTarget();
                aticks = 0;
            }
        }
        if (!clicksummon.get() && mc.screen == null) {
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
        if (mc.player == null || mc.level == null) return;

        if (clicksummon.get() && mc.options.keyAttack.isDown() && mc.screen == null && mc.player.getAbilities().instabuild) {
            spawnPearlAtTarget();
        }
        if (uuidCollectionMode.get() == uuidCollectionModes.RightClick && mc.options.keyUse.isDown() && mc.screen == null) {
            Entity targetEntity = target();
            if (targetEntity != null && targetEntity.isAlive() && targetEntity != mc.player) {
                entityUUID = targetEntity.getUUID();
                if (chatFeedback)info("Target entity UUID saved: " + targetEntity.getName().getString() + ". UUID: " + targetEntity.getUUID());
            }
        }
        if (uuidCollectionMode.get() == uuidCollectionModes.RightClick && resetUUID.get().isPressed() && mc.screen == null){
            if (chatFeedback)info("Resetting saved entity.");
            entityUUID = null;
        }
    }

    private Entity target() {
        if (mc.player == null || mc.level == null) return null;
        if (mc.hitResult instanceof EntityHitResult hit) return hit.getEntity();

        double maxRange = 512;
        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 lookVec = mc.player.getViewVector(1.0f);

        HitResult blockHit = mc.level.clip(new ClipContext(eyePos,
                eyePos.add(lookVec.scale(maxRange)), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, mc.player));
        double rayLength = blockHit.getType() == HitResult.Type.MISS ? maxRange :
                eyePos.distanceTo(blockHit.getLocation());

        List<Entity> candidates = mc.level.getEntities(mc.player,
                mc.player.getBoundingBox().expandTowards(lookVec.scale(rayLength)),
                e -> (onlyliving.get() && e instanceof LivingEntity && e.isAlive()) || !onlyliving.get());

        candidates.sort(Comparator.comparingDouble(e ->
                eyePos.distanceToSqr(e.getBoundingBox().getCenter())));

        double coneAngle = 0.999;
        for (Entity e : candidates) {
            double dist = eyePos.distanceTo(e.getBoundingBox().getCenter());
            if (dist > maxRange) break;

            Vec3 toEntity = e.getBoundingBox().getCenter().subtract(eyePos).normalize();

            if (lookVec.dot(toEntity) > coneAngle) {
                return e;
            }
        }
        return null;
    }
    private void spawnPearlAtTarget() {
        ItemStack rst = mc.player.getMainHandItem();
        BlockHitResult bhr = new BlockHitResult(
                mc.player.getEyePosition(),
                Direction.DOWN,
                BlockPos.containing(mc.player.getEyePosition()),
                false
        );

        if (uuidCollectionMode.get() == uuidCollectionModes.AllPlayers) {
            if (mc.getConnection() != null) {
                for (PlayerInfo entry : mc.getConnection().getOnlinePlayers()) {
                    if (!allPlayersAndSelf.get() && entry.getProfile().id().equals(mc.player.getUUID())) continue;
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
        var changes = DataComponentPatch.builder()
                .set(DataComponents.ENTITY_DATA, createEnderPearlData(entityuuid))
                .build();
        item.applyComponentsAndValidate(changes);

        mc.gameMode.handleCreativeModeItemAdd(item, 36 + mc.player.getInventory().getSelectedSlot());
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, bhr);
        mc.gameMode.handleCreativeModeItemAdd(rst, 36 + mc.player.getInventory().getSelectedSlot());
    }
    private TypedEntityData<EntityType<?>> createEnderPearlData(UUID entityuuid) {
        CompoundTag entityTag = new CompoundTag();

        HitResult hr = mc.getCameraEntity().pick(range.get(), 0, fluid.get());
        Vec3 hitPos = hr.getLocation();

        BlockPos bp = BlockPos.containing(hitPos);
        ListTag posList = new ListTag();
        switch (teleportMode.get()){
            case OnTarget -> {
                posList.add(DoubleTag.valueOf(bp.getX() + 0.5));
                posList.add(DoubleTag.valueOf(bp.getY() + 0.5));
                posList.add(DoubleTag.valueOf(bp.getZ() + 0.5));
            }
            case ToVoid -> {
                posList.add(DoubleTag.valueOf(mc.player.getX()));
                posList.add(DoubleTag.valueOf(mc.level.getMinY()-1));
                posList.add(DoubleTag.valueOf(mc.player.getZ()));
            }
            case ToPlayer -> {
                AbstractClientPlayer targetPlayer = null;
                for (AbstractClientPlayer player : mc.level.players()) {
                    switch (targetplayeruuidMode.get()) {
                        case PlayerName -> {
                            if (player.getName().getString().equals(targetplayercustomPlayerName.get())) {
                                targetPlayer = player;
                            }
                        }
                        case UUID -> {
                            if (player.getUUID().toString().equals(targetplayercustomuuid.get())) {
                                targetPlayer = player;
                            }
                        }
                    }
                    if (targetPlayer != null) break;
                }
                if (targetPlayer != null) {
                    posList.add(DoubleTag.valueOf(targetPlayer.getX()));
                    posList.add(DoubleTag.valueOf(targetPlayer.getY()));
                    posList.add(DoubleTag.valueOf(targetPlayer.getZ()));
                }
            }
        }
        entityTag.put("Pos", posList);

        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 dir = null;
        switch (teleportMode.get()){
            case OnTarget -> {
                dir = hitPos.subtract(eyePos);
                if (dir.lengthSqr() > 0) {
                    dir = dir.normalize().scale(velocity.get());
                }
            }
            case ToVoid -> dir = new Vec3(0, velocity.get(), 0);
            case ToPlayer -> dir = new Vec3(0, -velocity.get(), 0);
        }
        ListTag motionList = new ListTag();
        if (dir != null){
            motionList.add(DoubleTag.valueOf(dir.x));
            motionList.add(DoubleTag.valueOf(dir.y));
            motionList.add(DoubleTag.valueOf(dir.z));  
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
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getValue(entityId);
        if (entityType == null) entityType = EntityType.ENDER_PEARL;

        return TypedEntityData.of(entityType, entityTag);
    }
    private UUID getPlayerUUIDFromName(String playerName) {
        if (mc.getConnection() != null) {
            for (PlayerInfo entry : mc.getConnection().getOnlinePlayers()) {
                if (entry.getProfile().name().equalsIgnoreCase(playerName)) {
                    UUID uuid = entry.getProfile().id();
                    return uuid;
                }
            }
        }

        for (AbstractClientPlayer player : mc.level.players()) {
            if (player.getGameProfile().name().equalsIgnoreCase(playerName)) {
                return player.getUUID();
            }
        }

        if (mc.player != null && mc.player.getGameProfile().name().equalsIgnoreCase(playerName)) {
            return mc.player.getUUID();
        }

        if (chatFeedback)warning("Player '" + playerName + "' not found anywhere.");
        return null;
    }
}