package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.friends.Friend;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import pwn.noobs.trouserstreak.Trouser;
import pwn.noobs.trouserstreak.utils.PermissionUtils;

import java.util.*;
import java.util.stream.Collectors;

public class EffectsGiver extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> permissionLevel = sgGeneral.add(new IntSetting.Builder()
            .name("permission-level")
            .description("Permission level required before running commands.")
            .defaultValue(2)
            .min(1)
            .max(4)
            .sliderMax(4)
            .build()
    );

    private final Setting<Boolean> clearInventory = sgGeneral.add(new BoolSetting.Builder()
            .name("clear-inventory")
            .description("Clear the targeted players' inventories before applying effects.")
            .defaultValue(true)
            .build()
    );

    private final Setting<List<MobEffect>> effects = sgGeneral.add(new StatusEffectListSetting.Builder()
            .name("effects")
            .description("Effects to apply to targeted players.")
            .defaultValue(BuiltInRegistries.MOB_EFFECT.stream().collect(Collectors.toList()))
            .build()
    );

    private final Setting<Boolean> vanillaOnly = sgGeneral.add(new BoolSetting.Builder()
            .name("vanilla-effects-only")
            .description("Skip effects added by mods (like Xaero's Map) that the server may not recognize.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> infiniteDuration = sgGeneral.add(new BoolSetting.Builder()
            .name("infinite-duration")
            .description("Apply effects with infinite duration instead of using the duration setting. Minecraft 1.19.4+")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> duration = sgGeneral.add(new IntSetting.Builder()
            .name("duration")
            .description("Duration of the effects in seconds.")
            .defaultValue(1000000)
            .min(1)
            .sliderMax(1000000)
            .visible(() -> !infiniteDuration.get())
            .build()
    );

    private final Setting<Integer> amplifier = sgGeneral.add(new IntSetting.Builder()
            .name("amplifier")
            .description("Amplifier level of the effects (0 = level 1).")
            .defaultValue(255)
            .min(0)
            .sliderMax(255)
            .build()
    );

    private final Setting<Boolean> showParticles = sgGeneral.add(new BoolSetting.Builder()
            .name("show-particles")
            .description("Whether or not to show particles.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> targetEntities = sgGeneral.add(new BoolSetting.Builder()
            .name("target-entities")
            .description("Target all entities instead of just players.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> trollfriends = sgGeneral.add(new BoolSetting.Builder()
            .name("Effects for Friends")
            .description("Whether or not to affect friends.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> trollyourself = sgGeneral.add(new BoolSetting.Builder()
            .name("Effects for Yourself")
            .description("Whether or not to affect yourself.")
            .defaultValue(false)
            .build()
    );

    public enum Mode {
        Loop,
        DisableAfterRun
    }

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("Tick delay between effect commands.")
            .defaultValue(1)
            .min(0)
            .sliderMax(100)
            .build()
    );

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("Reapply the effects every cycle or disable the module after applying effects once.")
            .defaultValue(Mode.Loop)
            .build()
    );

    private final Queue<String> queue = new LinkedList<>();
    private int tickCounter = 0;
    private boolean sent = false;

    public EffectsGiver() {
        super(Trouser.operator, "EffectsGiver", "Applies potion effects to all players or entities, optionally excluding friends and yourself. Requires operator status.");
    }

    @Override
    public void onActivate() {
        queue.clear();
        sent = false;
        tickCounter = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.level == null) return;
        if (PermissionUtils.getPermissionLevel(mc.player) < permissionLevel.get()) return;

        if (queue.isEmpty()) {
            if (sent && mode.get() == Mode.DisableAfterRun) {
                toggle();
                return;
            }

            buildQueue();
            sent = true;
            tickCounter = 0;
        }

        if (!queue.isEmpty()) {
            tickCounter++;
            if (tickCounter >= delay.get()) {
                ChatUtils.sendPlayerMsg(Objects.requireNonNull(queue.poll()));
                tickCounter = 0;
            }
        }
    }

    private String buildSelector(String base) {
        List<String> args = new ArrayList<>();

        if (!trollyourself.get()) {
            args.add("name=!" + mc.player.getGameProfile().name());
        }

        if (!trollfriends.get()) {
            for (Friend friend : Friends.get()) {
                args.add("name=!" + friend.getName());
            }
        }

        if (args.isEmpty()) return base;

        return base + "[" + String.join(",", args) + "]";
    }

    private void buildQueue() {
        queue.clear();

        String effectSelector = buildSelector(targetEntities.get() ? "@e" : "@a");
        String clearSelector = buildSelector("@a");

        if (clearInventory.get()) {
            queue.add("/clear " + clearSelector);
        }

        for (MobEffect effect : effects.get()) {
            Identifier effectId = BuiltInRegistries.MOB_EFFECT.getKey(effect);

            if (vanillaOnly.get() && !effectId.getNamespace().equals("minecraft")) continue;

            String durationArg = infiniteDuration.get() ? "infinite" : String.valueOf(duration.get());

            String command = "/effect give " + effectSelector + " " + effectId.getPath() + " "
                    + durationArg + " " + amplifier.get() + " " + showParticles.get();
            queue.add(command);
        }

        if (queue.isEmpty()) warning("Nothing to do — no effects selected and clear-inventory is off.");
    }
}