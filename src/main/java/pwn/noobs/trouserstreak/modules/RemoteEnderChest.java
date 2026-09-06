package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.item.Item;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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
    private GenericContainerScreen savedScreen = null;
    private int savedSyncId = -1;
    private World lastWorld = null;
    private BlockPos potentialEChestPos = null;

    @Override
    public void onDeactivate() {
        resetStuff();
    }

    @EventHandler
    private void onKey(KeyEvent event) {
        if (savedScreen == null) return;
        if (event.action != KeyAction.Press) return;

        int inventoryKeyCode = mc.options.inventoryKey.getDefaultKey().getCode();

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
        if (mc.player == null || mc.world == null) return;

        if (mc.crosshairTarget instanceof BlockHitResult bhr) {
            potentialEChestPos = bhr.getBlockPos();
            if (mc.world.getBlockState(bhr.getBlockPos()).getBlock() == Blocks.ENDER_CHEST
                    && mc.options.useKey.isPressed() && !isEnderChestScreen(potentialEChestPos) && !guiHidden) {
                mc.doItemUse();
                mc.doItemUse();
            }
        } else potentialEChestPos = null;

        if (isEnderChestScreen(potentialEChestPos) && savedScreen == null && !guiWasOpen && !guiHidden) {
            savedScreen = (GenericContainerScreen) mc.currentScreen;
            savedSyncId = mc.player.currentScreenHandler.syncId;
            mc.setScreen(null);
            guiHidden = true;
            guiWasOpen = true;
            lastWorld = mc.world;
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
            if (mc.currentScreen == null && !guiHidden && guiWasOpen) {
                resetStuff();
                if (chatFeedback) error("Ender Chest GUI closed. EChest link broken.");
                return;
            }

            if (savedSyncId != -1) {
                boolean handlerValid = mc.player.currentScreenHandler != null &&
                        mc.player.currentScreenHandler.syncId == savedSyncId;
                if (!handlerValid) {
                    resetStuff();
                    if (chatFeedback) error("Ender chest handler invalid. EChest link broken.");
                    return;
                }
            }

            if (mc.world != lastWorld) {
                resetStuff();
                lastWorld = mc.world;
                if (chatFeedback) error("World changed. EChest link broken.");
                return;
            }

            checkAndSaveItems();
        }

        lastWorld = mc.world;
    }

    private void checkAndSaveItems() {
        if (!enableItemSaver.get()) return;

        boolean triggerAuto = mc.player.getHealth() <= healthThreshold.get();
        boolean triggerManual = itemSaverHotkey.get().isPressed();

        if (!triggerAuto && !triggerManual) return;

        if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler)) return;
        if (handler.syncId != savedSyncId) return;

        List<Item> targetItems = items.get();
        if (targetItems.isEmpty()) return;

        int containerSlots = handler.getRows() * 9;
        boolean movedAny = false;

        for (int i = containerSlots; i < handler.slots.size(); i++) {
            var stack = handler.slots.get(i).getStack();
            if (stack == null || stack.isEmpty()) continue;

            if (targetItems.contains(stack.getItem()) || keepInv.get()) {
                mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                movedAny = true;
            }
        }
        if (keepInv.get()){
            for (int i = containerSlots; i < handler.slots.size(); i++) {
                var stack = handler.slots.get(i).getStack();
                if (stack == null || stack.isEmpty()) continue;

                mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                movedAny = true;
            }
        }

        if (movedAny) {
            if (chatFeedback) warning("Items saved into Ender Chest!");
        }
    }

    private boolean isEnderChestScreen(BlockPos echest) {
        if (echest == null || mc.world == null) return false;
        return mc.currentScreen instanceof GenericContainerScreen screen &&
                screen.getScreenHandler().getType() == ScreenHandlerType.GENERIC_9X3 &&
                mc.world.getBlockState(echest).getBlock() == Blocks.ENDER_CHEST;
    }

    private void resetStuff() {
        guiHidden = false;
        guiWasOpen = false;
        lastKeyState = false;
        potentialEChestPos = null;
        if (savedSyncId != -1) {
            mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(savedSyncId));
            savedSyncId = -1;
        }
        savedScreen = null;
    }
}