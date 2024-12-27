package pwn.noobs.trouserstreak.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
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
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class TextCommand extends Command {
    public enum ColorModes {
        aqua, black, blue, dark_aqua, dark_blue, dark_gray, dark_green,
        dark_purple, dark_red, gold, gray, green, italic, light_purple,
        red, white, yellow
    }

    public TextCommand() {
        super("text", "Spawns a text hologram with custom text in front of you. Use | for new lines and #color for text color.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("message", StringArgumentType.greedyString()).executes(context -> {
            String text = context.getArgument("message", String.class);
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
            return SINGLE_SUCCESS;
        }));
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
