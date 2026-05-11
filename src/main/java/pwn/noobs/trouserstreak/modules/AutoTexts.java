//Credits to DedicateDev for making this!
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.PlaySoundEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import pwn.noobs.trouserstreak.Trouser;

import java.util.List;
import java.util.Random;

public class AutoTexts extends Module {
    public enum ColorModes {
        aqua, black, blue, dark_aqua, dark_blue, dark_gray, dark_green,
        dark_purple, dark_red, gold, gray, green, italic, light_purple,
        red, white, yellow
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgText = settings.createGroup("Text Options");
    private final SettingGroup sgSpawning = settings.createGroup("Spawn Settings");
    private final SettingGroup sgMisc = settings.createGroup("Miscellaneous");

    private final Setting<Boolean> disconnectdisable = sgGeneral.add(new BoolSetting.Builder()
            .name("Disable on Disconnect")
            .description("Disables module on disconnecting")
            .defaultValue(false)
            .build());

    private final Setting<List<String>> texts = sgText.add(new StringListSetting.Builder()
            .name("Texts")
            .description("Text lines to display")
            .defaultValue(List.of("Trolled by Mountains of Lava Inc", "www.youtube.com/@mountainsoflavainc.6913"))
            .build());

    private final Setting<Boolean> rainbow = sgText.add(new BoolSetting.Builder()
            .name("Rainbow")
            .description("Randomly cycles through all available colors")
            .defaultValue(false)
            .build());

    private final Setting<ColorModes> textColor = sgText.add(new EnumSetting.Builder<ColorModes>()
            .name("Text Color")
            .description("Color of the text")
            .defaultValue(ColorModes.red)
            .visible(() -> !rainbow.get())
            .build());

    private final Setting<Integer> radius = sgSpawning.add(new IntSetting.Builder()
            .name("Radius")
            .description("Spawn radius")
            .defaultValue(8)
            .min(1)
            .sliderMax(8)
            .build());

    private final Setting<Integer> height = sgSpawning.add(new IntSetting.Builder()
            .name("Height")
            .description("Base spawn height relative to player")
            .defaultValue(0)
            .sliderRange(-8, 8)
            .build());

    private final Setting<Boolean> heightVariation = sgSpawning.add(new BoolSetting.Builder()
            .name("Height Variation")
            .description("Enable random height variation")
            .defaultValue(false)
            .build());

    private final Setting<Integer> spawnDelay = sgSpawning.add(new IntSetting.Builder()
            .name("Spawn Delay")
            .description("Delay between spawns in ticks")
            .defaultValue(2)
            .min(0)
            .sliderMax(20)
            .build());

    private final Setting<Integer> spawnCount = sgSpawning.add(new IntSetting.Builder()
            .name("Spawn Count")
            .description("How many to spawn per tick")
            .defaultValue(1)
            .min(1)
            .sliderMax(100)
            .build());

    private final Setting<Boolean> muteSounds = sgMisc.add(new BoolSetting.Builder()
            .name("Mute Sounds")
            .description("Prevents playing armor stand placement sounds")
            .defaultValue(true)
            .build());

    private final Random random = new Random();
    private int ticks;
    private Vec3 origin;
    private String namecolour;

    public AutoTexts() {
        super(Trouser.operator, "auto-texts", "Spawns invisible armor stands with custom text. Requires creative mode.");
    }

    @Override
    public void onActivate() {
        if (mc.player == null) return;
        if (!mc.player.getAbilities().instabuild) {
            error("Creative mode required!");
            toggle();
            return;
        }
        ticks = 0;
        origin = null;
        namecolour = "";
    }

    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (disconnectdisable.get() && event.screen instanceof DisconnectedScreen) toggle();
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (disconnectdisable.get()) toggle();
    }

    @EventHandler
    private void onPlaySound(PlaySoundEvent event) {
        if (muteSounds.get() && event.sound.getIdentifier().getPath().contains("entity.armor_stand.place")) {
            event.cancel();
        }
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        origin = mc.player.position();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (ticks >= spawnDelay.get()) {
            updateNameColor();
            for (int i = 0; i < spawnCount.get(); i++) {
                spawnArmorStand();
            }
            ticks = 0;
        }
        ticks++;
    }

    private void updateNameColor() {
        if (rainbow.get()) {
            ColorModes[] colors = ColorModes.values();
            namecolour = colors[random.nextInt(colors.length)].toString();
        } else {
            namecolour = textColor.get().toString();
        }
    }

    private Vec3 pickRandomPos() {
        double x = random.nextDouble(radius.get() * 2) - radius.get() + origin.x;
        double y = mc.player.getY() + height.get() + (heightVariation.get() ? random.nextDouble(8) - 4 : 0);
        double z = random.nextDouble(radius.get() * 2) - radius.get() + origin.z;
        return new Vec3(x, y, z);
    }

    private void spawnArmorStand() {
        ItemStack armorStand = new ItemStack(Items.ARMOR_STAND);
        ItemStack current = mc.player.getMainHandItem();
        Vec3 pos = pickRandomPos();
        String selectedText = texts.get().get(random.nextInt(texts.get().size()));

        var changes = DataComponentPatch.builder()
                .set(DataComponents.CUSTOM_NAME, Component.literal(selectedText).withStyle(ChatFormatting.valueOf(namecolour.toUpperCase())))
                .set(DataComponents.ENTITY_DATA, createEntityData(pos))
                .build();

        armorStand.applyComponentsAndValidate(changes);

        BlockHitResult bhr = new BlockHitResult(pos, Direction.UP, BlockPos.containing(pos), false);
        mc.gameMode.handleCreativeModeItemAdd(armorStand, 36 + mc.player.getInventory().getSelectedSlot());
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, bhr);
        mc.gameMode.handleCreativeModeItemAdd(current, 36 + mc.player.getInventory().getSelectedSlot());
    }

    private TypedEntityData<EntityType<?>> createEntityData(Vec3 pos) {
        CompoundTag entityTag = new CompoundTag();
        ListTag position = new ListTag();
        String selectedText = texts.get().get(random.nextInt(texts.get().size()));

        position.add(DoubleTag.valueOf(pos.x));
        position.add(DoubleTag.valueOf(pos.y));
        position.add(DoubleTag.valueOf(pos.z));

        entityTag.putString("id", "minecraft:armor_stand");
        entityTag.put("Pos", position);
        entityTag.putBoolean("Invisible", true);
        entityTag.putBoolean("Marker", true);
        entityTag.putBoolean("NoGravity", true);
        entityTag.putBoolean("CustomNameVisible", true);
        CompoundTag CustomNameNBT = new CompoundTag();
        CustomNameNBT.putString("text", selectedText);
        CustomNameNBT.putString("color", namecolour);
        String serverVersion;
        if (mc.hasSingleplayerServer()) {
            serverVersion = mc.getSingleplayerServer().getServerVersion();
        } else {
            serverVersion = mc.getCurrentServer().version.tryCollapseToString();
        }
        if (serverVersion == null) {
            entityTag.put("CustomName", CustomNameNBT);
        } else {
            if (isVersionLessThan(serverVersion, 1, 21, 5)) {
                entityTag.putString("CustomName", "{\"text\":\"" + selectedText + "\",\"color\":\"" + namecolour + "\"}");
            } else {
                entityTag.put("CustomName", CustomNameNBT);
            }
        }

        return TypedEntityData.of(EntityType.ARMOR_STAND, entityTag);
    }
    private boolean isVersionLessThan(String serverVersion, int major, int minor, int patch) {
        if (serverVersion == null) return false;

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(serverVersion);

        if (matcher.find()) {
            try {
                int serverMajor = Integer.parseInt(matcher.group(1));
                int serverMinor = Integer.parseInt(matcher.group(2));
                int serverPatch = Integer.parseInt(matcher.group(3));

                if (serverMajor < major) return true;
                if (serverMajor > major) return false;

                if (serverMinor < minor) return true;
                if (serverMinor > minor) return false;

                return serverPatch < patch;

            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }
}