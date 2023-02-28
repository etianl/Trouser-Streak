package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.InfinityMiner;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.BambooBlock;
import net.minecraft.block.BambooSaplingBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShearsItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import pwn.noobs.trouserstreak.Trouser;

import java.util.function.Predicate;

public class ShulkerDupe extends Module {

    private final SettingGroup sgAutoTool = settings.createGroup("AutoTool");

    public ShulkerDupe() {
        super(Trouser.Main, "shulker-dupe", "ShulkerDupe only works in vanilla, forge, and fabric servers version 1.19 and below.");
    }
    //AutoTool
    private final Setting<Boolean> autoT = sgAutoTool.add(new BoolSetting.Builder()
            .name("AutoToolWhenDupe")
            .description("Uses AutoTool code when breaking shulker.")
            .defaultValue(false)
            .build()
    );
    private final Setting<EnchantPreference> prefer = sgAutoTool.add(new EnumSetting.Builder<EnchantPreference>()
            .name("prefer")
            .description("Either to prefer Silk Touch, Fortune, or none.")
            .defaultValue(EnchantPreference.None)
            .visible(() -> autoT.get())
            .build()
    );

    private final Setting<Boolean> antiBreak = sgAutoTool.add(new BoolSetting.Builder()
            .name("anti-break")
            .description("Stops you from breaking your tool.")
            .defaultValue(false)
            .visible(() -> autoT.get())
            .build()
    );

    private final Setting<Integer> breakDurability = sgAutoTool.add(new IntSetting.Builder()
            .name("anti-break-percentage")
            .description("The durability percentage to stop using a tool.")
            .defaultValue(10)
            .range(1, 100)
            .sliderRange(1, 100)
            .visible(() -> autoT.get() && antiBreak.get())
            .build()
    );

    private final Setting<Boolean> switchBack = sgAutoTool.add(new BoolSetting.Builder()
            .name("switch-back")
            .description("Switches your hand to whatever was selected when releasing your attack key.")
            .defaultValue(false)
            .visible(() -> autoT.get())
            .build()
    );

    private final Setting<Integer> switchDelay = sgAutoTool.add((new IntSetting.Builder()
            .name("switch-delay")
            .description("Delay in ticks before switching tools.")
            .defaultValue(0)
            .visible(() -> autoT.get())
            .build()
    ));
    private boolean silkTouchForEnderChest=false;
    private boolean wasPressed;
    private boolean shouldSwitch;
    private int ticks;
    private int bestSlot;
    public static boolean shouldDupe;
    public static boolean shouldDupeAll;
    private boolean timerWASon=false;

    @Override
    public void onActivate() {
        timerWASon=false;
        shouldDupeAll=false;
        shouldDupe=false;
    }
    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (event.screen instanceof ShulkerBoxScreen) {
            shouldDupeAll=false;
            shouldDupe=false;
        }
    }    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (shouldDupe|shouldDupeAll==true){
            if (Modules.get().get(Timer.class).isActive()) {
                timerWASon=true;
                Modules.get().get(Timer.class).toggle();
            }
        } else if (!shouldDupe|!shouldDupeAll==true){
            if (!Modules.get().get(Timer.class).isActive() && timerWASon==true) {
                timerWASon=false;
                Modules.get().get(Timer.class).toggle();
            }
        }

    }
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.currentScreen instanceof ShulkerBoxScreen && mc.player != null) {
            HitResult wow = mc.crosshairTarget;
            BlockHitResult a = (BlockHitResult) wow;
            if (shouldDupe|shouldDupeAll==true){
            mc.interactionManager.updateBlockBreakingProgress(a.getBlockPos(), Direction.DOWN);
        }
        }
        //autotool
        if (switchBack.get() && !mc.options.attackKey.isPressed() && wasPressed && InvUtils.previousSlot != -1) {
            InvUtils.swapBack();
            wasPressed = false;
            return;
        }

        if (ticks <= 0 && shouldSwitch && bestSlot != -1) {
            InvUtils.swap(bestSlot, switchBack.get());
            shouldSwitch = false;
        } else {
            ticks--;
        }

        wasPressed = mc.options.attackKey.isPressed();
    }

    @EventHandler
    public void onSendPacket(PacketEvent.Sent event) {
        if (event.packet instanceof PlayerActionC2SPacket) {
            if (shouldDupeAll==true){
            if (((PlayerActionC2SPacket) event.packet).getAction() == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK) {
                for (int i = 0; i < 27; i++) {
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                }
                shouldDupeAll=false;
            }
            } else if (shouldDupe==true){
            if (((PlayerActionC2SPacket) event.packet).getAction() == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK) {
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 0, 0, SlotActionType.QUICK_MOVE, mc.player);
                    shouldDupe=false;
            }
            }
        }
    }

    //Autotool code credits meteorclient
    @EventHandler(priority = EventPriority.HIGH)
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        if (autoT.get() && shouldDupe|shouldDupeAll==true){
        // Get blockState
        BlockState blockState = mc.world.getBlockState(event.blockPos);
        if (!BlockUtils.canBreak(event.blockPos, blockState)) return;

        // Check if we should switch to a better tool
        ItemStack currentStack = mc.player.getMainHandStack();

        double bestScore = -1;
        bestSlot = -1;

        for (int i = 0; i < 9; i++) {
            double score = getScore(mc.player.getInventory().getStack(i), blockState, silkTouchForEnderChest, prefer.get(), itemStack -> !shouldStopUsing(itemStack));
            if (score < 0) continue;

            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }

        if ((bestSlot != -1 && (bestScore > getScore(currentStack, blockState, silkTouchForEnderChest, prefer.get(), itemStack -> !shouldStopUsing(itemStack))) || shouldStopUsing(currentStack) || !isTool(currentStack))) {
            ticks = switchDelay.get();

            if (ticks == 0) InvUtils.swap(bestSlot, true);
            else shouldSwitch = true;
        }

        // Anti break
        currentStack = mc.player.getMainHandStack();

        if (shouldStopUsing(currentStack) && isTool(currentStack)) {
            mc.options.attackKey.setPressed(false);
            event.setCancelled(true);
        }
        }
    }
    private boolean shouldStopUsing(ItemStack itemStack) {
        return antiBreak.get() && (itemStack.getMaxDamage() - itemStack.getDamage()) < (itemStack.getMaxDamage() * breakDurability.get() / 100);
    }

    public static double getScore(ItemStack itemStack, BlockState state, boolean silkTouchEnderChest, EnchantPreference enchantPreference, Predicate<ItemStack> good) {
        if (!good.test(itemStack) || !isTool(itemStack)) return -1;

        if (silkTouchEnderChest
                && state.getBlock() == Blocks.ENDER_CHEST
                && EnchantmentHelper.getLevel(Enchantments.SILK_TOUCH, itemStack) == 0) {
            return -1;
        }

        double score = 0;

        score += itemStack.getMiningSpeedMultiplier(state) * 1000;
        score += EnchantmentHelper.getLevel(Enchantments.UNBREAKING, itemStack);
        score += EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, itemStack);
        score += EnchantmentHelper.getLevel(Enchantments.MENDING, itemStack);

        if (enchantPreference == EnchantPreference.Fortune) score += EnchantmentHelper.getLevel(Enchantments.FORTUNE, itemStack);
        if (enchantPreference == EnchantPreference.SilkTouch) score += EnchantmentHelper.getLevel(Enchantments.SILK_TOUCH, itemStack);

        if (itemStack.getItem() instanceof SwordItem item && (state.getBlock() instanceof BambooBlock || state.getBlock() instanceof BambooSaplingBlock))
            score += 9000 + (item.getMaterial().getMiningLevel() * 1000);


        return score;
    }

    public static boolean isTool(ItemStack itemStack) {
        return itemStack.getItem() instanceof ToolItem || itemStack.getItem() instanceof ShearsItem;
    }
    public enum EnchantPreference {
        None,
        Fortune,
        SilkTouch
    }}
