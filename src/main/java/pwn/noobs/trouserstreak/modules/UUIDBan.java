// made by KI10
// Heavily modified by etianl :)
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import pwn.noobs.trouserstreak.Trouser;
import pwn.noobs.trouserstreak.utils.PermissionUtils;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class UUIDBan extends Module {
    private final SettingGroup uuidOptions = settings.createGroup("UUID Options.");
    private final SettingGroup banOptions = settings.createGroup("Ban Options.");

    private enum uuidCollectionModes {
        RightClick,
        CustomName,
        AllPlayers
    }
    private final Setting<uuidCollectionModes> uuidCollectionMode = uuidOptions.add(new EnumSetting.Builder<uuidCollectionModes>()
            .name("uuid-collection-mode")
            .description("How to get the uuid. RightClick = grab uuid of aimed at entity, AllPlayers = All players online, CustomName = Pick a UUID or name.")
            .defaultValue(uuidCollectionModes.RightClick)
            .build());
    private final Setting<String> customPlayerName = uuidOptions.add(new StringSetting.Builder()
            .name("Player Name")
            .description("Player name to get UUID from")
            .defaultValue("")
            .visible(() -> uuidCollectionMode.get() == uuidCollectionModes.CustomName)
            .build()
    );
    private final Setting<Boolean> allPlayersAndSelf = uuidOptions.add(new BoolSetting.Builder()
            .name("All Players Include Self")
            .description("Run commands for all players in tab list and yourself as well.")
            .defaultValue(false)
            .visible(() -> uuidCollectionMode.get() == uuidCollectionModes.AllPlayers)
            .build()
    );
    private final Setting<Boolean> ignorefrend = uuidOptions.add(new BoolSetting.Builder()
            .name("All Players Ignore Friends")
            .description("Run commands for all players in tab list excluding friends.")
            .defaultValue(true)
            .visible(() -> uuidCollectionMode.get() == uuidCollectionModes.AllPlayers)
            .build()
    );
    private final Setting<Keybind> resetUUID = uuidOptions.add(new KeybindSetting.Builder()
            .name("reset-uuid-keybind")
            .description("Keybind used to reset the uuid to null.")
            .build()
    );

    private final Setting<Integer> requiredOpLevel = banOptions.add(new IntSetting.Builder()
            .name("required-op-level")
            .description("Required operator permission level so that the module can send commands (0-4).")
            .defaultValue(2)
            .sliderRange(0,4)
            .min(0)
            .max(4)
            .build()
    );
    private final Setting<Integer> delay = banOptions.add(new IntSetting.Builder()
            .name("summon delay")
            .description("delay in ticks.)")
            .defaultValue(20)
            .sliderRange(0,40)
            .min(0)
            .build()
    );
    private final Setting<Boolean> entityGlow = banOptions.add(new BoolSetting.Builder()
            .name("Glowing Ban Entity")
            .description("The entity that enforces the ban glows.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> clickBan = banOptions.add(new BoolSetting.Builder()
            .name("Left Click Ban")
            .description("Runs Ban commands when you click. Other wise use a tick delay")
            .defaultValue(true)
            .build()
    );
    private final Setting<Integer> tickdelay = banOptions.add(new IntSetting.Builder()
            .name("TickDelay(Must be > summon delay)")
            .description("How many ticks between executing the ban commands.")
            .defaultValue(40)
            .min(0)
            .sliderMax(100)
            .visible(() -> !clickBan.get())
            .build()
    );

    public UUIDBan() {
        super(Trouser.operator, "UUIDBan", "Kicks players and summons an entity with their UUID, preventing them joining back. Original module made by KI10");
    }

    private Collection<PlayerInfo> playerlistattimeofkick = null;
    private String pendingCommand = null;
    private UUID entityUUID = null;
    private String entityName = null;
    private int ticks = 0;
    private int ticksLeft = 0;

    @Override
    public void onActivate() {
        playerlistattimeofkick = null;
        pendingCommand = null;
        entityUUID = null;
        entityName = null;
        ticksLeft = 0;
        ticks = 0;
        if (!clickBan.get()) {
            runBanCommands();
        }
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.level == null) return;

        if (PermissionUtils.getPermissionLevel(mc.player) < requiredOpLevel.get()) {
            if (chatFeedback)error("You do not have the required OP rights (required: %d).", requiredOpLevel.get());
            toggle();
            return;
        }

        if (!clickBan.get()) {
            if (ticks <= tickdelay.get()) {
                ticks++;
            } else {
                runBanCommands();
                ticks = 0;
            }
        }

        if (pendingCommand == null) return;
        if (ticksLeft > 0) {
            ticksLeft--;
            return;
        }

        if (uuidCollectionMode.get() == uuidCollectionModes.AllPlayers) {
            if (mc.getConnection() != null) {
                for (PlayerInfo entry : playerlistattimeofkick) {
                    if (!allPlayersAndSelf.get() && entry.getProfile().id().equals(mc.player.getUUID())) continue;
                    if (ignorefrend.get() && Friends.get().isFriend(entry)) continue;

                    String summonCommand = String.format("summon villager ~ ~ ~ " + makeEntityNBT(entry.getProfile().id()));
                    mc.player.connection.sendCommand(summonCommand);
                }
                playerlistattimeofkick = null;
            }
        } else {
            mc.player.connection.sendCommand(pendingCommand);
        }
        pendingCommand = null;
        ticksLeft = 0;
    }

    @EventHandler
    private void onMouseButton(MouseClickEvent event) {
        if (mc.player == null || mc.level == null) return;

        if (clickBan.get() && mc.options.keyAttack.isDown() && PermissionUtils.getPermissionLevel(mc.player) >= requiredOpLevel.get()) {
            runBanCommands();
        }
        if (uuidCollectionMode.get() == uuidCollectionModes.RightClick && mc.options.keyUse.isDown()) {
            Entity targetEntity = target();
            if (targetEntity != null && targetEntity.isAlive() && targetEntity != mc.player) {
                entityUUID = targetEntity.getUUID();
                entityName = targetEntity.getName().getString();
                if (chatFeedback)info("Target entity UUID saved: " + targetEntity.getName().getString() + ". UUID: " + targetEntity.getUUID());
            }
        }
        if (resetUUID.get().isPressed()){
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
                e -> e instanceof Player && e.isAlive() && e != mc.player);

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
    private void runBanCommands(){
        if (uuidCollectionMode.get() == uuidCollectionModes.AllPlayers) {
            if (mc.getConnection() != null) {
                playerlistattimeofkick = List.copyOf(mc.getConnection().getOnlinePlayers());
                for (PlayerInfo entry : playerlistattimeofkick) {
                    if (!allPlayersAndSelf.get() && entry.getProfile().id().equals(mc.player.getUUID())) continue;
                    if (ignorefrend.get() && Friends.get().isFriend(entry)) continue;
                    entityUUID = entry.getProfile().id();
                    entityName = entry.getProfile().name();
                    runBanCommand();
                }
                ticksLeft = delay.get();
            }
        } else {
            runBanCommand();
            ticksLeft = delay.get();
        }
    }
    private void runBanCommand(){
        UUID entityuuid;
        String playerName;
        if (uuidCollectionMode.get() == uuidCollectionModes.CustomName) {
            if (!customPlayerName.get().isEmpty()) {
                entityuuid = getPlayerUUIDFromName(customPlayerName.get());
                playerName = customPlayerName.get();
            } else {
                if (chatFeedback)warning("No valid UUID");
                return;
            }

            if (entityuuid == null){
                if (chatFeedback)warning("No valid UUID");
                return;
            }
        } else if (entityUUID != null && entityName != null) {
            entityuuid = entityUUID;
            playerName = entityName;
        } else {
            if (chatFeedback)warning("No target UUID");
            return;
        }

        String kickCommand = String.format("kick \"%s\"", playerName);
        String summonCommand = String.format("summon villager ~ ~ ~ " + makeEntityNBT(entityuuid));

        mc.player.connection.sendCommand(kickCommand);
        pendingCommand = summonCommand;
    }
    private UUID getPlayerUUIDFromName(String playerName) {
        if (mc.getConnection() != null) {
            for (PlayerInfo entry : mc.getConnection().getOnlinePlayers()) {
                if (entry.getProfile().name().equalsIgnoreCase(playerName)) {
                    return entry.getProfile().id();
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
    private String makeEntityNBT(UUID entityuuid){
        int[] uuidInts = uuidToIntArray(entityuuid);
        String uuidNbt = String.format("[I;%d,%d,%d,%d]", uuidInts[0], uuidInts[1], uuidInts[2], uuidInts[3]);

        if (entityGlow.get()) return "{Silent:1b,Glowing:1b,UUID:" + uuidNbt + ",NoAI:1b,Invulnerable:1b,active_effects:[{id:\"minecraft:invisibility\",amplifier:255,duration:-1,show_particles:0b,show_icon:0b,ambient:0b}],NoGravity:1b}";
        else return  "{Silent:1b,UUID:" + uuidNbt + ",NoAI:1b,Invulnerable:1b,active_effects:[{id:\"minecraft:invisibility\",amplifier:255,duration:-1,show_particles:0b,show_icon:0b,ambient:0b}],NoGravity:1b}";
    }
    private static int[] uuidToIntArray(UUID uuid) {
        long most = uuid.getMostSignificantBits();
        long least = uuid.getLeastSignificantBits();
        return new int[]{
                (int)(most >> 32),
                (int)most,
                (int)(least >> 32),
                (int)least
        };
    }
}