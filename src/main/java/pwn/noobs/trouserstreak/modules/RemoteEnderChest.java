package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.meteor.KeyInputEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import pwn.noobs.trouserstreak.Trouser;

import java.util.List;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_ALT;

public class RemoteEnderChest extends Module {
    private final SettingGroup sgGeneral = settings.createGroup("RemoteEnderChest");
    private final SettingGroup sgItemSaver = settings.createGroup("ItemSaver");

    private final Setting<Keybind> toggleGui = sgGeneral.add(new KeybindSetting(
            "GUI Key (Press it)",
            "Key to toggle Ender Chest GUI.",
            Keybind.fromKey(GLFW_KEY_LEFT_ALT),
            value -> {},
            value -> {},
            null,
            () -> {}
    ));
    private final Setting<Boolean> invKeyBlocker = sgGeneral.add(new BoolSetting.Builder()
            .name("inventory-key-modifier")
            .description("Make the Inventory Key open the EnderChest GUI when you have one saved. This helps to prevent you accidentally breaking the link.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> enableItemSaver = sgItemSaver.add(new BoolSetting.Builder()
            .name("enable-item-saver")
            .description("Automatically moves items in your inventory into the linked Ender Chest when low health.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Double> healthThreshold = sgItemSaver.add(new DoubleSetting.Builder()
            .name("health-threshold")
            .description("Move the items to the open Ender Chest when health is equal to or less than this value (2.0 = 1 heart).")
            .defaultValue(4.0)
            .min(0.0)
            .max(20.0)
            .sliderRange(0.0, 20.0)
            .visible(enableItemSaver::get)
            .build()
    );
    private final Setting<Boolean> totemCheck = sgItemSaver.add(new BoolSetting.Builder()
            .name("check-for-totem")
            .description("If you are low health and are holding a Totem of Undying, do not store the items.")
            .defaultValue(true)
            .visible(enableItemSaver::get)
            .build()
    );
    private final Setting<List<Item>> items = sgItemSaver.add(new ItemListSetting.Builder()
            .name("Items to save")
            .description("These items will be stored.")
            .visible(enableItemSaver::get)
            .build()
    );
    private final Setting<Boolean> keepInv = sgItemSaver.add(new BoolSetting.Builder()
            .name("keep-inventory")
            .description("Try to save the whole inventory. The Items to save list will be prioritized.")
            .defaultValue(true)
            .visible(enableItemSaver::get)
            .build()
    );
    private final Setting<Keybind> itemSaverHotkey = sgItemSaver.add(new KeybindSetting(
            "Item Saver HotKey",
            "You can quickly store items when pressing this button.",
            Keybind.fromKey(GLFW_KEY_RIGHT_ALT),
            value -> {},
            value -> {},
            null,
            () -> {}
    ));

    public RemoteEnderChest() {
        super(Trouser.Main, "RemoteEnderChest", "Access your enderchest anywhere and move freely while it is open.");
    }
    private boolean guiHidden = false;
    private boolean guiWasOpen = false;
    private boolean lastKeyState = false;
    private ContainerScreen savedScreen = null;
    private int savedSyncId = -1;
    private Level lastWorld = null;
    private BlockPos potentialEChestPos = null;

    @Override
    public void onDeactivate() {
        resetStuff();
    }

    @EventHandler
    private void onKey(KeyInputEvent event) {
        if (savedScreen == null) return;
        if (event.action != KeyAction.Press) return;

        int inventoryKeyCode = mc.options.keyInventory.getDefaultKey().getValue();

        if (!guiHidden) {
            if (event.key() == inventoryKeyCode) {
                event.cancel();
                mc.setScreen(null);
                guiHidden = true;
                return;
            }
        }

        if (guiHidden && invKeyBlocker.get()) {
            if (event.key() == inventoryKeyCode) {
                event.cancel();
                mc.setScreen(savedScreen);
                guiHidden = false;
            }
        }
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null) return;

        if (mc.hitResult instanceof BlockHitResult bhr) {
            potentialEChestPos = bhr.getBlockPos();
            if (mc.level.getBlockState(bhr.getBlockPos()).getBlock() == Blocks.ENDER_CHEST
                    && mc.options.keyUse.isDown() && !isEnderChestScreen(potentialEChestPos) && !guiHidden) {
                mc.startUseItem();
                mc.startUseItem();
            }
        } else potentialEChestPos = null;

        if (isEnderChestScreen(potentialEChestPos) && savedScreen == null && !guiWasOpen && !guiHidden) {
            savedScreen = (ContainerScreen) mc.screen;
            savedSyncId = mc.player.containerMenu.containerId;
            mc.setScreen(null);
            guiHidden = true;
            guiWasOpen = true;
            lastWorld = mc.level;
            if (chatFeedback) info("EChest link created! §ePress " + toggleGui.get().toString() + " to toggle the Ender Chest GUI.§r");
        }

        boolean keyDown = toggleGui.get().isPressed();
        boolean keyJustPressed = keyDown && !lastKeyState;
        lastKeyState = keyDown;

        if (keyJustPressed && savedScreen != null) {
            if (guiHidden) {
                mc.setScreen(savedScreen);
                guiHidden = false;
            } else {
                mc.setScreen(null);
                guiHidden = true;
            }
        }

        if (savedScreen != null){
            if (mc.screen == null && !guiHidden && guiWasOpen) {
                resetStuff();
                if (chatFeedback) error("Ender Chest GUI closed. EChest link broken.");
                return;
            }

            if (savedSyncId != -1) {
                boolean handlerValid = mc.player.containerMenu != null &&
                        mc.player.containerMenu.containerId == savedSyncId;
                if (!handlerValid) {
                    resetStuff();
                    if (chatFeedback) error("Ender chest handler invalid. EChest link broken.");
                    return;
                }
            }

            if (mc.level != lastWorld) {
                resetStuff();
                lastWorld = mc.level;
                if (chatFeedback) error("World changed. EChest link broken.");
                return;
            }

            checkAndSaveItems();
        }

        lastWorld = mc.level;
    }

    private void checkAndSaveItems() {
        if (!enableItemSaver.get()) return;

        boolean hasTotem = mc.player.getMainHandItem().is(Items.TOTEM_OF_UNDYING) ||
                mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING);

        boolean shouldCheckTotem = totemCheck.get() && hasTotem;

        boolean triggerAuto = mc.player.getHealth() <= healthThreshold.get() && !shouldCheckTotem;
        boolean triggerManual = itemSaverHotkey.get().isPressed();

        if (!triggerAuto && !triggerManual) return;

        if (!(mc.player.containerMenu instanceof ChestMenu handler)) return;
        if (handler.containerId != savedSyncId) return;

        List<Item> targetItems = items.get();
        if (targetItems.isEmpty()) return;

        int containerSlots = handler.getRowCount() * 9;
        boolean movedAny = false;

        for (int i = containerSlots; i < handler.slots.size(); i++) {
            var stack = handler.slots.get(i).getItem();
            if (stack == null || stack.isEmpty()) continue;

            if (targetItems.contains(stack.getItem())) {
                mc.gameMode.handleContainerInput(handler.containerId, i, 0, ContainerInput.QUICK_MOVE, mc.player);
                movedAny = true;
            }
        }
        if (keepInv.get()){
            for (int i = containerSlots; i < handler.slots.size(); i++) {
                var stack = handler.slots.get(i).getItem();
                if (stack == null || stack.isEmpty()) continue;

                mc.gameMode.handleContainerInput(handler.containerId, i, 0, ContainerInput.QUICK_MOVE, mc.player);
                movedAny = true;
            }
        }

        if (movedAny) {
            if (chatFeedback) warning("Items saved into Ender Chest!");
        }
    }

    private boolean isEnderChestScreen(BlockPos echest) {
        if (echest == null || mc.level == null) return false;
        return mc.screen instanceof ContainerScreen screen &&
                screen.getMenu().getType() == MenuType.GENERIC_9x3 &&
                mc.level.getBlockState(echest).getBlock() == Blocks.ENDER_CHEST;
    }

    private void resetStuff() {
        guiHidden = false;
        guiWasOpen = false;
        lastKeyState = false;
        potentialEChestPos = null;
        if (savedSyncId != -1) {
            mc.getConnection().send(new ServerboundContainerClosePacket(savedSyncId));
            savedSyncId = -1;
        }
        savedScreen = null;
    }
}