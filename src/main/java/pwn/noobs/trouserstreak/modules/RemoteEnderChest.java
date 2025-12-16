package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.meteor.MouseScrollEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;
import pwn.noobs.trouserstreak.Trouser;

import static org.lwjgl.glfw.GLFW.*;

public class RemoteEnderChest extends Module {
    private final SettingGroup sgGUI = settings.createGroup("EnderChest GUI");
    private final SettingGroup sgGeneral = settings.createGroup("RemoteEnderChest");
    private final Setting<Boolean> hide = sgGUI.add(new BoolSetting.Builder()
            .name("hide-screen")
            .description("Ender Chest gui is hidden unless holding GUI Key.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Keybind> toggleKey = sgGUI.add(new KeybindSetting(
            "GUI Key (Hold it)",
            "Key to toggle Ender Chest GUI visibility/interactiveness.",
            Keybind.fromKey(GLFW_KEY_LEFT_ALT),
            value -> {},
            value -> {},
            null,
            () -> {}
    ));
    private final Setting<Double> screenX = sgGUI.add(new DoubleSetting.Builder()
            .name("screen-x")
            .description("X offset for ender chest screen.")
            .defaultValue(0)
            .sliderRange(0, 1024)
            .build()
    );
    private final Setting<Double> screenY = sgGUI.add(new DoubleSetting.Builder()
            .name("screen-y")
            .description("Y offset for ender chest screen.")
            .defaultValue(0)
            .sliderRange(0, 1024)
            .build()
    );
    private final Setting<Integer> lmbcooldown = sgGeneral.add(new IntSetting.Builder()
            .name("Left Mouse button cooldown.")
            .description("Restricts button usage to once every this many ticks")
            .defaultValue(0)
            .min(0)
            .sliderMax(60)
            .build()
    );
    private final Setting<Integer> rmbcooldown = sgGeneral.add(new IntSetting.Builder()
            .name("Right Mouse button cooldown.")
            .description("Restricts button usage to once every this many ticks")
            .defaultValue(5)
            .min(0)
            .sliderMax(60)
            .build()
    );
    private final Setting<Boolean> airplace = sgGeneral.add(new BoolSetting.Builder()
            .name("allow-airplace")
            .description("Place blocks at the end of your reach when aiming at the air and GUI is open.")
            .defaultValue(false)
            .build()
    );
    public RemoteEnderChest() {
        super(Trouser.Main, "RemoteEnderChest", "Access your enderchest anywhere and move freely while it is open.");
    }
    private boolean mouseGrabbed = false;
    private boolean guiWasOpen = false;
    private int lmbCooldown = 0;
    private int rmbCooldown = 0;
    private int ticks = 0;
    @Override
    public void onDeactivate() {
        mouseGrabbed = false;
        guiWasOpen = false;
        rmbCooldown = 0;
        lmbCooldown = 0;
        ticks=0;
    }
    @EventHandler
    private void onMouseScroll(MouseScrollEvent event) {
        if (mc.currentScreen instanceof GenericContainerScreen screen && screen.getScreenHandler().getType() == ScreenHandlerType.GENERIC_9X3) {
            double scrollY = event.value;
            if (scrollY != 0) {
                int current = mc.player.getInventory().selectedSlot;
                int direction = scrollY > 0 ? -1 : 1;  // Up = prev, Down = next
                int newSlot = ((current + direction + 9) % 9);
                mc.player.getInventory().selectedSlot = newSlot;
            }
        }
    }
    @EventHandler
    private void onKey(KeyEvent event) {
        if (mc.currentScreen instanceof GenericContainerScreen screen && screen.getScreenHandler().getType() == ScreenHandlerType.GENERIC_9X3) {
            handleMeteorHotkeys();
        }
    }
    @EventHandler
    private void onClick(MouseButtonEvent event) {
        if (mc.currentScreen instanceof GenericContainerScreen screen && screen.getScreenHandler().getType() == ScreenHandlerType.GENERIC_9X3) {
            handleMeteorHotkeys();
        }
    }
    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mc.currentScreen instanceof GenericContainerScreen screen && screen.getScreenHandler().getType() == ScreenHandlerType.GENERIC_9X3) {
            guiWasOpen = true;
            if (rmbCooldown < rmbcooldown.get()) rmbCooldown++;
            if (lmbCooldown < lmbcooldown.get()) lmbCooldown++;
            if (ticks<=0)ticks++;
            if (hide.get()){
                boolean altHeld = isKeyDown(toggleKey.get().getValue());
                if (!altHeld){
                    screen.x = -666;
                    screen.y = -666;
                    mc.inGameHud.setOverlayMessage(
                            net.minecraft.text.Text.literal("§eENDERCHEST is open! ("+toggleKey.get()+" to view)"),
                            true
                    );
                } else {
                    screen.x = (int) screenX.get().doubleValue();
                    screen.y = (int) screenY.get().doubleValue();
                }
            } else {
                screen.x = (int) screenX.get().doubleValue();
                screen.y = (int) screenY.get().doubleValue();
            }
            handleHotbarKeys();
            handleInput(mc.options.swapHandsKey);
            handleInput(mc.options.forwardKey);
            handleInput(mc.options.backKey);
            handleInput(mc.options.leftKey);
            handleInput(mc.options.rightKey);
            handleInput(mc.options.jumpKey);
            handleInput(mc.options.sneakKey);
            handleInput(mc.options.sprintKey);

            boolean altHeld = isKeyDown(toggleKey.get().getValue());
            boolean inEChest = mc.currentScreen instanceof GenericContainerScreen &&
                    screen.getScreenHandler().getType() == ScreenHandlerType.GENERIC_9X3;

            if (inEChest) {
                if (!altHeld && !mouseGrabbed) {
                    glfwSetInputMode(mc.getWindow().getHandle(), GLFW_CURSOR, GLFW_CURSOR_DISABLED);
                    mc.mouse.cursorLocked = true;
                    mouseGrabbed = true;
                } else if (altHeld && mouseGrabbed) {
                    glfwSetInputMode(mc.getWindow().getHandle(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                    mc.mouse.cursorLocked = false;
                    mouseGrabbed = false;
                }
            }
            if (mouseGrabbed) {
                if (lmbCooldown >= lmbcooldown.get()) {
                    int attackCode = mc.options.attackKey.boundKey.getCode();//these codes prevent silly error spam
                    if (attackCode <= 7){
                        if (isMouseButtonDown(mc.options.attackKey.boundKey.getCode())) doAttackStuff();
                    } else {
                        if (isKeyDown(mc.options.attackKey.boundKey.getCode())) doAttackStuff();
                    }
                }
                if (rmbCooldown >= rmbcooldown.get()) {
                    int useCode = mc.options.useKey.boundKey.getCode();//these codes prevent silly error spam
                    if (useCode <= 7){
                        if (isMouseButtonDown(mc.options.useKey.boundKey.getCode())) doitemusestuff();
                    } else {
                        if (isKeyDown(mc.options.useKey.boundKey.getCode())) doitemusestuff();
                    }
                }
            }
        } else {
            if (guiWasOpen) {
                guiWasOpen = false;
                error("Ender Chest GUI closed.");
            }
            mouseGrabbed = false;
            ticks = 0;
            rmbCooldown = 0;
            lmbCooldown = 0;
        }
        BlockHitResult bhr = null;
        if (mc.crosshairTarget instanceof BlockHitResult){
            bhr = (BlockHitResult) mc.crosshairTarget;

            if (mc.world.getBlockState(bhr.getBlockPos()).getBlock() == Blocks.ENDER_CHEST && mc.options.useKey.isPressed() && !(mc.currentScreen instanceof GenericContainerScreen screen && screen.getScreenHandler().getType() == ScreenHandlerType.GENERIC_9X3)) {
                mc.doItemUse();
                mc.doItemUse();
            }
        }
    }

    private boolean isKeyDown(int key) {
        return GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == GLFW_PRESS;
    }

    private boolean isMouseButtonDown(int button) {
        return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), button) == GLFW_PRESS;
    }
    private int getKey(KeyBinding keyBinding) {
        return keyBinding.boundKey.getCode();
    }
    private void doAttackStuff() {
        HitResult hr = mc.crosshairTarget;
        BlockHitResult bhr = null;
        EntityHitResult ehr = null;
        mc.player.swingHand(Hand.MAIN_HAND);

        if (hr instanceof BlockHitResult) {
            bhr = (BlockHitResult) hr;
        } else if (hr instanceof EntityHitResult) {
            ehr = (EntityHitResult) hr;
        }

        if (bhr != null) {
            mc.interactionManager.updateBlockBreakingProgress(bhr.getBlockPos(), bhr.getSide());
            return;
        }

        if (ehr != null && ehr.getEntity() != null) {
            mc.interactionManager.attackEntity(mc.player, ehr.getEntity());
        }

        lmbCooldown = 0;
    }
    private void doitemusestuff() {
        HitResult hr = mc.crosshairTarget;
        BlockHitResult bhr = hr instanceof BlockHitResult ? (BlockHitResult) hr : null;
        EntityHitResult ehr = hr instanceof EntityHitResult ? (EntityHitResult) hr : null;

        Hand hand = mc.player.getInventory().getStack(40).isEmpty() ? Hand.MAIN_HAND : Hand.OFF_HAND;

        mc.interactionManager.interactItem(mc.player, hand);

        if (bhr != null) {
            BlockState state = mc.world.getBlockState(bhr.getBlockPos());
            boolean isAirTarget = state.isAir();

            if (!isAirTarget || airplace.get()) {
                mc.interactionManager.interactBlock(mc.player, hand, bhr);
            }
        }

        if (ehr != null && ehr.getEntity() != null) {
            mc.interactionManager.interactEntity(mc.player, ehr.getEntity(), hand);
        }

        rmbCooldown = 0;
    }
    private void handleInput(KeyBinding key) {
        key.setPressed(isKeyDown(getKey(key)));
    }
    private void handleHotbarKeys() {
        int[] hotbarKeys = {
                GLFW_KEY_1, GLFW_KEY_2, GLFW_KEY_3, GLFW_KEY_4, GLFW_KEY_5,
                GLFW_KEY_6, GLFW_KEY_7, GLFW_KEY_8, GLFW_KEY_9
        };

        for (int i = 0; i < hotbarKeys.length; i++) {
            if (isKeyDown(hotbarKeys[i])) {
                mc.player.getInventory().selectedSlot = i;
            }
        }
    }
    private void handleMeteorHotkeys() {
        if (Modules.get().getAll().isEmpty()) return;
        for (Module module : Modules.get().getAll()) {
            Keybind keybind = module.keybind;
            if (keybind != null && keybind.isPressed()) {
                module.toggle();
                info(module.isActive() ? "Toggled " + module.title + " §aon" : "Toggled " + module.title + " §coff");
            }
        }
    }
}