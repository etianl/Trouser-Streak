package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
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
    private ContainerScreen savedScreen = null;
    private int savedSyncId = -1;
    private Level lastWorld = null;
    private BlockPos potentialEChestPos = null;

    @Override
    public void onDeactivate() {
        resetStuff();
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mc.hitResult instanceof BlockHitResult bhr) {
            potentialEChestPos = bhr.getBlockPos();
            if (mc.level.getBlockState(bhr.getBlockPos()).getBlock() == Blocks.ENDER_CHEST
                    && mc.options.keyUse.isDown() && !isEnderChestScreen(potentialEChestPos) && !guiHidden) {
                mc.startUseItem();
                mc.startUseItem();
            }
        }

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

        if (savedScreen != null && mc.screen == null && !guiHidden && guiWasOpen) {
            resetStuff();
            if (chatFeedback) error("Ender Chest GUI closed. EChest link broken.");
            return;
        }

        if (savedScreen != null && savedSyncId != -1) {
            boolean handlerValid = mc.player.containerMenu != null &&
                    mc.player.containerMenu.containerId == savedSyncId;
            if (!handlerValid) {
                resetStuff();
                if (chatFeedback) error("Ender chest handler invalid. EChest link broken.");
                return;
            }
        }

        if (savedScreen != null && mc.level != lastWorld) {
            resetStuff();
            lastWorld = mc.level;
            if (chatFeedback) error("World changed. EChest link broken.");
            return;
        }

        lastWorld = mc.level;
    }

    private boolean isEnderChestScreen(BlockPos echest) {
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