package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import pwn.noobs.trouserstreak.Trouser;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT;

public class RemoteEnderChest extends Module {
    private final SettingGroup sgGeneral = settings.createGroup("RemoteEnderChest");

    private final Setting<Keybind> toggleGui = sgGeneral.add(new KeybindSetting(
            "GUI Key (Press it)",
            "Key to toggle Ender Chest GUI.",
            Keybind.fromKey(GLFW_KEY_LEFT_ALT),
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
    private void onPreTick(TickEvent.Pre event) {
        if (mc.crosshairTarget instanceof BlockHitResult bhr) {
            potentialEChestPos = bhr.getBlockPos();
            if (mc.world.getBlockState(bhr.getBlockPos()).getBlock() == Blocks.ENDER_CHEST
                    && mc.options.useKey.isPressed() && !isEnderChestScreen(potentialEChestPos) && !guiHidden) {
                mc.doItemUse();
                mc.doItemUse();
            }
        }

        if (isEnderChestScreen(potentialEChestPos) && savedScreen == null && !guiWasOpen && !guiHidden) {
            savedScreen = (GenericContainerScreen) mc.currentScreen;
            savedSyncId = mc.player.currentScreenHandler.syncId;
            mc.setScreen(null);
            guiHidden = true;
            guiWasOpen = true;
            lastWorld = mc.world;
            if (chatFeedback) info("EChest link created! Press " + toggleGui.get().toString() + " to toggle the Ender Chest GUI.");
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

        if (savedScreen != null && mc.currentScreen == null && !guiHidden && guiWasOpen) {
            resetStuff();
            if (chatFeedback) error("Ender Chest GUI closed. EChest link broken.");
            return;
        }

        if (savedScreen != null && savedSyncId != -1) {
            boolean handlerValid = mc.player.currentScreenHandler != null &&
                    mc.player.currentScreenHandler.syncId == savedSyncId;
            if (!handlerValid) {
                resetStuff();
                if (chatFeedback) error("Ender chest handler invalid. EChest link broken.");
                return;
            }
        }

        if (savedScreen != null && mc.world != lastWorld) {
            resetStuff();
            lastWorld = mc.world;
            if (chatFeedback) error("World changed. EChest link broken.");
            return;
        }

        lastWorld = mc.world;
    }

    private boolean isEnderChestScreen(BlockPos echest) {
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