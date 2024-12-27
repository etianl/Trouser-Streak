package pwn.noobs.trouserstreak.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class TextCommand extends Command {
    public enum ColorModes {
        aqua, black, blue, dark_aqua, dark_blue, dark_gray, dark_green,
        dark_purple, dark_red, gold, gray, green, italic, light_purple,
        red, white, yellow
    }

    public TextCommand() {
        super("text", "Spawns a text hologram with custom text in front of you. Use | for new lines and #color for text color.");
        createDefaultPresets();
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("message", StringArgumentType.greedyString()).executes(context -> {
            String text = context.getArgument("message", String.class);
            spawnTextLines(text);
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("save").then(argument("presetName", StringArgumentType.word())
                .then(argument("text", StringArgumentType.greedyString()).executes(context -> {
                    String presetName = context.getArgument("presetName", String.class);
                    String text = context.getArgument("text", String.class);
                    savePreset(presetName, text);
                    info("Saved preset: " + presetName);
                    return SINGLE_SUCCESS;
                }))));

        builder.then(literal("load").then(argument("presetName", StringArgumentType.word())
                .suggests((context, suggestionsBuilder) -> suggestPresets(suggestionsBuilder))
                .executes(context -> {
                    String presetName = context.getArgument("presetName", String.class);
                    String text = loadPreset(presetName);
                    if (text != null) {
                        spawnTextLines(text);
                        info("Loaded preset: " + presetName);
                    } else {
                        error("Preset not found: " + presetName);
                    }
                    return SINGLE_SUCCESS;
                })));
    }

    private CompletableFuture<Suggestions> suggestPresets(SuggestionsBuilder builder) {
        try {
            Path presetsDir = Paths.get("TrouserStreak", "TextPresets");
            if (Files.exists(presetsDir)) {
                try (Stream<Path> files = Files.list(presetsDir)) {
                    files.filter(path -> path.toString().endsWith(".txt"))
                            .map(path -> path.getFileName().toString().replace(".txt", ""))
                            .forEach(builder::suggest);
                }
            }
        } catch (IOException ignored) {}
        return builder.buildFuture();
    }

    private void spawnTextLines(String text) {
        String[] lines = text.split("\\|");
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();
            StringBuilder formattedText = new StringBuilder();
            String currentColor = "white";

            String[] words = line.split(" ");
            for (String word : words) {
                if (word.startsWith("#")) {
                    try {
                        ColorModes.valueOf(word.substring(1).toLowerCase());
                        currentColor = word.substring(1).toLowerCase();
                    } catch (IllegalArgumentException ignored) {
                        formattedText.append(word).append(" ");
                    }
                } else {
                    formattedText.append("{\"text\":\"").append(word).append(" \",\"color\":\"").append(currentColor).append("\"},");
                }
            }

            String finalText = "[" + formattedText.substring(0, formattedText.length() - 1) + "]";
            spawnText(finalText, ((lines.length - 1 - i) * 0.3) + 1, true);
        }
    }

    private void savePreset(String presetName, String text) {
        try {
            Path dirPath = Paths.get("TrouserStreak", "TextPresets");
            Files.createDirectories(dirPath);
            Path filePath = dirPath.resolve(presetName + ".txt");
            Files.write(filePath, text.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            error("Failed to save preset: " + e.getMessage());
        }
    }

    private String loadPreset(String presetName) {
        try {
            Path filePath = Paths.get("TrouserStreak", "TextPresets", presetName + ".txt");
            if (Files.exists(filePath)) {
                return Files.readString(filePath);
            }
        } catch (IOException e) {
            error("Failed to load preset: " + e.getMessage());
        }
        return null;
    }

    private void createDefaultPresets() {
        String[] defaultPresets = {
                "trolled=#green [ #dark_red Trolled! #green ]|#gold Mountains of Lava Inc.|#red Youtube: #blue www.youtube.com/@mountainsoflavainc.6913|#green [ #dark_red Trolled! #green ]",
                "mountains=#red Mountains Of Lava Inc.|#gold youtube.com/@mountainsoflavainc.6913"
        };

        for (String preset : defaultPresets) {
            String[] parts = preset.split("=", 2);
            if (!Files.exists(Paths.get("TrouserStreak", "TextPresets", parts[0] + ".txt"))) {
                savePreset(parts[0], parts[1]);
            }
        }
    }

    private void spawnText(String message, double yOffset, boolean isJson) {
        if (!mc.player.getAbilities().creativeMode) {
            error("Creative mode required!");
            return;
        }

        ItemStack armorStand = new ItemStack(Items.ARMOR_STAND);
        ItemStack current = mc.player.getMainHandStack();
        Vec3d pos = mc.player.getPos().add(mc.player.getRotationVector().multiply(2)).add(0, yOffset, 0);

        var changes = ComponentChanges.builder()
                .add(DataComponentTypes.ENTITY_DATA, createEntityData(pos, message, isJson))
                .build();

        armorStand.applyChanges(changes);

        BlockHitResult bhr = new BlockHitResult(pos, Direction.UP, BlockPos.ofFloored(pos), false);
        mc.interactionManager.clickCreativeStack(armorStand, 36 + mc.player.getInventory().selectedSlot);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
        mc.interactionManager.clickCreativeStack(current, 36 + mc.player.getInventory().selectedSlot);
    }

    private NbtComponent createEntityData(Vec3d pos, String text, boolean isJson) {
        NbtCompound entityTag = new NbtCompound();
        NbtList position = new NbtList();

        position.add(NbtDouble.of(pos.x));
        position.add(NbtDouble.of(pos.y));
        position.add(NbtDouble.of(pos.z));

        entityTag.putString("id", "minecraft:armor_stand");
        entityTag.put("Pos", position);
        entityTag.putBoolean("Invisible", true);
        entityTag.putBoolean("Marker", true);
        entityTag.putBoolean("NoGravity", true);
        entityTag.putBoolean("CustomNameVisible", true);
        entityTag.putString("CustomName", isJson ? text : "{\"text\":\"" + text + "\"}");

        return NbtComponent.of(entityTag);
    }
}
