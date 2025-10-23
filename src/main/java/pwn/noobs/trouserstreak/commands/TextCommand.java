//Credits to DedicateDev for making this! I (etianl) only added functions for deleting presets and regenerating the default presets.
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
import net.minecraft.entity.EntityType;
import net.minecraft.entity.TypedEntityData;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
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
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class TextCommand extends Command {
    private static final double LINE_SPACING = 0.3;
    private static final double INITIAL_HEIGHT_OFFSET = 1.0;
    private static final String PRESETS_DIRECTORY = "TrouserStreak/TextPresets";

    public TextCommand() {
        super("text", "Spawns a text hologram with custom text in front of you. Use | for new lines and #color for text color.");
        createDefaultPresets();
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder
                .then(argument("message", StringArgumentType.greedyString())
                        .executes(context -> {
                            spawnTextLines(context.getArgument("message", String.class));
                            return SINGLE_SUCCESS;
                        }))
                .then(literal("save")
                        .then(argument("presetName", StringArgumentType.word())
                                .then(argument("text", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            String presetName = context.getArgument("presetName", String.class);
                                            String text = context.getArgument("text", String.class);
                                            savePreset(presetName, text);
                                            info("Saved preset: " + presetName);
                                            return SINGLE_SUCCESS;
                                        }))))
                .then(literal("load")
                        .then(argument("presetName", StringArgumentType.word())
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
                                })))
                .then(literal("regeneratedefaults")
                        .executes(context -> {
                            regenDefaultPresets();
                            info("Default presets regenerated.");
                            return SINGLE_SUCCESS;
                        }))
                .then(literal("delete")
                        .then(argument("presetName", StringArgumentType.word())
                                .suggests((context, suggestionsBuilder) -> suggestPresets(suggestionsBuilder))
                                .executes(context -> {
                                    String presetName = context.getArgument("presetName", String.class);
                                    deletePreset(presetName);
                                    return SINGLE_SUCCESS;
                                })));
    }

    private CompletableFuture<Suggestions> suggestPresets(SuggestionsBuilder builder) {
        try {
            Path presetsDir = Paths.get(PRESETS_DIRECTORY);
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
            NbtList nbt = formatTextWithColors(line);
            double heightOffset = ((lines.length - 1 - i) * LINE_SPACING) + INITIAL_HEIGHT_OFFSET;
            spawnText(nbt, heightOffset);
        }
    }

    private NbtList formatTextWithColors(String line) {
        NbtList nbt = new NbtList();
        String currentColor = "white";
        boolean wasObfuscated = false;

        Pattern pattern = Pattern.compile("#([a-zA-Z_]+) ");
        Matcher matcher = pattern.matcher(line);

        int offset = 0;

        while (matcher.find()) {
            if (offset != matcher.start())
                nbt.add(makePart(line.substring(offset, matcher.start()), currentColor, false, wasObfuscated));

            String word = matcher.group(1);
            if (word.equalsIgnoreCase("obfuscated")) {
                offset = matcher.end();

                int end = line.substring(offset).indexOf(" ") + 1;
                nbt.add(makePart(line.substring(offset, offset + end), currentColor, true, wasObfuscated));

                wasObfuscated = true;
                offset += end;
            } else {
                currentColor = word;
                offset = matcher.end();
            }
        }

        if (offset != line.length())
            nbt.add(makePart(line.substring(offset), currentColor, false, wasObfuscated));

        return nbt;
    }

    private NbtCompound makePart(String text, String color, boolean obfuscated, boolean wasObfuscated) {
        NbtCompound part = new NbtCompound();
        part.putString("text", text);
        part.putString("color", color);
        if (obfuscated && !wasObfuscated) part.putBoolean("obfuscated", true);
        if (!obfuscated && wasObfuscated) part.putBoolean("obfuscated", false);
        return part;
    }

    private void savePreset(String presetName, String text) {
        try {
            Path dirPath = Paths.get(PRESETS_DIRECTORY);
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
            Path filePath = Paths.get(PRESETS_DIRECTORY, presetName + ".txt");
            if (Files.exists(filePath)) {
                return Files.readString(filePath);
            }
        } catch (IOException e) {
            error("Failed to load preset: " + e.getMessage());
        }
        return null;
    }
    private void deletePreset(String presetName) {
        Path filePath = Paths.get(PRESETS_DIRECTORY, presetName + ".txt");
        try {
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                info("Deleted preset: " + presetName);
            } else {
                error("Preset not found: " + presetName);
            }
        } catch (IOException e) {
            error("Failed to delete preset: " + e.getMessage());
        }
    }
    private void createDefaultPresets() {
        String[] defaultPresets = {
                "trolled=#green [ #dark_red Trolled! #green ]|#gold Mountains of Lava Inc.|#red Youtube: #blue www.youtube.com/@mountainsoflavainc.6913|#green [ #dark_red Trolled! #green ]",
                "mountains=#red Mountains Of Lava Inc.|#gold youtube.com/@mountainsoflavainc.6913",
                "genesis=#gold THE BOOK OF GENESIS|#gold Chapter 1|#green [1:1] In the beginning when God created the heavens and the earth,|#green [1:2] the earth was a formless void and darkness covered the face of the deep, while a wind from God swept over the face of the waters.|#green [1:3] Then God said, 'Let there be light'; and there was light.|#green [1:4] And God saw that the light was good; and God separated the light from the darkness.|#green [1:5] God called the light Day, and the darkness he called Night. And there was evening and there was morning, the first day.|#green [1:6] And God said, 'Let there be a dome in the midst of the waters, and let it separate the waters from the waters.'|#green [1:7] So God made the dome and separated the waters that were under the dome from the waters that were above the dome. And it was so.|#green [1:8] God called the dome Sky. And there was evening and there was morning, the second day.|#green [1:9] And God said, 'Let the waters under the sky be gathered together into one place, and let the dry land appear.' And it was so.|#green [1:10] God called the dry land Earth, and the waters that were gathered together he called Seas. And God saw that it was good.|#gold Chapter 2|#green [2:1] Thus the heavens and the earth were finished, and all their multitude.|#green [2:2] And on the seventh day God finished the work that he had done, and he rested on the seventh day from all the work that he had done.",
        };

        for (String preset : defaultPresets) {
            String[] parts = preset.split("=", 2);
            if (!Files.exists(Paths.get(PRESETS_DIRECTORY, parts[0] + ".txt"))) {
                savePreset(parts[0], parts[1]);
            }
        }
    }
    private void regenDefaultPresets() {
        String[] defaultPresetNames = {
                "trolled",
                "mountains",
                "genesis"
        };

        try {
            Path dirPath = Paths.get(PRESETS_DIRECTORY);
            if (Files.exists(dirPath)) {
                try (Stream<Path> files = Files.list(dirPath)) {
                    files.filter(path -> {
                        String fileName = path.getFileName().toString();
                        return Arrays.stream(defaultPresetNames)
                                .anyMatch(name -> fileName.equals(name + ".txt"));
                    }).forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            error("Failed to delete preset: " + path + " - " + e.getMessage());
                        }
                    });
                } catch (IOException e) {
                    error("Failed to list directory: " + e.getMessage());
                }
            }
            Files.createDirectories(dirPath);

            String[] defaultPresets = {
                    "trolled=#green [ #dark_red Trolled! #green ]|#gold Mountains of Lava Inc.|#red Youtube: #blue www.youtube.com/@mountainsoflavainc.6913|#green [ #dark_red Trolled! #green ]",
                    "mountains=#red Mountains Of Lava Inc.|#gold youtube.com/@mountainsoflavainc.6913",
                    "genesis=#gold THE BOOK OF GENESIS|#gold Chapter 1|#green [1:1] In the beginning when God created the heavens and the earth,|#green [1:2] the earth was a formless void and darkness covered the face of the deep, while a wind from God swept over the face of the waters.|#green [1:3] Then God said, 'Let there be light'; and there was light.|#green [1:4] And God saw that the light was good; and God separated the light from the darkness.|#green [1:5] God called the light Day, and the darkness he called Night. And there was evening and there was morning, the first day.|#green [1:6] And God said, 'Let there be a dome in the midst of the waters, and let it separate the waters from the waters.'|#green [1:7] So God made the dome and separated the waters that were under the dome from the waters that were above the dome. And it was so.|#green [1:8] God called the dome Sky. And there was evening and there was morning, the second day.|#green [1:9] And God said, 'Let the waters under the sky be gathered together into one place, and let the dry land appear.' And it was so.|#green [1:10] God called the dry land Earth, and the waters that were gathered together he called Seas. And God saw that it was good.|#gold Chapter 2|#green [2:1] Thus the heavens and the earth were finished, and all their multitude.|#green [2:2] And on the seventh day God finished the work that he had done, and he rested on the seventh day from all the work that he had done.",
            };

            for (String preset : defaultPresets) {
                String[] parts = preset.split("=", 2);
                savePreset(parts[0], parts[1]);
            }
        } catch (IOException e) {
            error("Failed to regenerate presets: " + e.getMessage());
        }
    }

    private void spawnText(NbtList nbt, double yOffset) {
        if (!mc.player.getAbilities().creativeMode) {
            error("Creative mode required!");
            return;
        }

        ItemStack armorStand = new ItemStack(Items.ARMOR_STAND);
        ItemStack current = mc.player.getMainHandStack();
        Vec3d pos = mc.player.getEntityPos().add(mc.player.getRotationVector().multiply(2)).add(0, yOffset, 0);

        var changes = ComponentChanges.builder()
                .add(DataComponentTypes.ENTITY_DATA, createEntityData(yOffset, nbt))
                .build();

        armorStand.applyChanges(changes);

        BlockHitResult bhr = new BlockHitResult(pos, Direction.UP, BlockPos.ofFloored(pos), false);
        mc.interactionManager.clickCreativeStack(armorStand, 36 + mc.player.getInventory().selectedSlot);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
        mc.interactionManager.clickCreativeStack(current, 36 + mc.player.getInventory().selectedSlot);
    }

    private TypedEntityData<EntityType<?>> createEntityData(double yOffset, NbtList nbt) {
        Vec3d pos = mc.player.getEntityPos().add(mc.player.getRotationVector().multiply(2)).add(0, yOffset, 0);
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
        entityTag.put("CustomName", nbt);

        return TypedEntityData.create(EntityType.ARMOR_STAND, entityTag);
    }
}