//Credits to DedicateDev for making this!
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.PlaySoundEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
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
    private Vec3d origin;
    private String namecolour;

    public AutoTexts() {
        super(Trouser.Main, "auto-texts", "Spawns invisible armor stands with custom text. Requires creative mode.");
    }

    @Override
    public void onActivate() {
        if (!mc.player.getAbilities().creativeMode) {
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
        if (muteSounds.get() && event.sound.getId().getPath().contains("entity.armor_stand.place")) {
            event.cancel();
        }
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        origin = mc.player.getPos();
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

    private Vec3d pickRandomPos() {
        double x = random.nextDouble(radius.get() * 2) - radius.get() + origin.x;
        double y = mc.player.getY() + height.get() + (heightVariation.get() ? random.nextDouble(8) - 4 : 0);
        double z = random.nextDouble(radius.get() * 2) - radius.get() + origin.z;
        return new Vec3d(x, y, z);
    }

    private void spawnArmorStand() {
        ItemStack armorStand = new ItemStack(Items.ARMOR_STAND);
        ItemStack current = mc.player.getMainHandStack();
        Vec3d pos = pickRandomPos();
        String selectedText = texts.get().get(random.nextInt(texts.get().size()));

        NbtCompound tag = new NbtCompound();

        NbtCompound entityTag = new NbtCompound();

        entityTag.putString("id", "minecraft:armor_stand");

        NbtList positionList = new NbtList();
        positionList.add(NbtDouble.of(pos.x));
        positionList.add(NbtDouble.of(pos.y));
        positionList.add(NbtDouble.of(pos.z));

        entityTag.put("Pos", positionList);
        entityTag.putBoolean("Invisible", true);
        entityTag.putBoolean("Marker", true);
        entityTag.putBoolean("NoGravity", true);
        entityTag.putBoolean("CustomNameVisible",true);
        entityTag.putString("CustomName", "{\"text\":\"" + selectedText + "\",\"color\":\"" + namecolour + "\"}");

        tag.put("EntityTag", entityTag);

        armorStand.setNbt(tag);

        BlockHitResult bhr = new BlockHitResult(pos, Direction.UP, BlockPos.ofFloored(pos), false);
        mc.interactionManager.clickCreativeStack(armorStand, 36 + mc.player.getInventory().selectedSlot);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
        mc.interactionManager.clickCreativeStack(current, 36 + mc.player.getInventory().selectedSlot);
    }
}