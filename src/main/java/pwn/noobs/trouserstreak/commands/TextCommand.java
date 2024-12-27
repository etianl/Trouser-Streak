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
    public TextCommand() {
        super("text", "Spawns a text hologram with custom text in front of you. Use | for new lines.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("message", StringArgumentType.greedyString()).executes(context -> {
            String text = context.getArgument("message", String.class);
            String[] lines = text.split("\\|");
            for (int i = lines.length - 1; i >= 0; i--) {
                spawnText(lines[i].trim(), ((lines.length - 1 - i) * 0.3) + 1);
            }
            return SINGLE_SUCCESS;
        }));
    }

    private void spawnText(String message, double yOffset) {
        if (!mc.player.getAbilities().creativeMode) {
            error("Creative mode required!");
            return;
        }

        ItemStack armorStand = new ItemStack(Items.ARMOR_STAND);
        ItemStack current = mc.player.getMainHandStack();
        Vec3d pos = mc.player.getPos().add(mc.player.getRotationVector().multiply(2)).add(0, yOffset, 0);

        var changes = ComponentChanges.builder()
                .add(DataComponentTypes.CUSTOM_NAME, Text.literal(message).formatted(Formatting.WHITE))
                .add(DataComponentTypes.ENTITY_DATA, createEntityData(pos, message))
                .build();

        armorStand.applyChanges(changes);

        BlockHitResult bhr = new BlockHitResult(pos, Direction.UP, BlockPos.ofFloored(pos), false);
        mc.interactionManager.clickCreativeStack(armorStand, 36 + mc.player.getInventory().selectedSlot);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
        mc.interactionManager.clickCreativeStack(current, 36 + mc.player.getInventory().selectedSlot);
    }

    private NbtComponent createEntityData(Vec3d pos, String text) {
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
        entityTag.putString("CustomName", "{\"text\":\"" + text + "\",\"color\":\"white\"}");

        return NbtComponent.of(entityTag);
    }
}
