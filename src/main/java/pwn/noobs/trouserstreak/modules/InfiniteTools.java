/*This module was made based on the AttributeSwap Module made by [DonKisser](https://github.com/DonKisser)
using ideas from therandomdude https://github.com/etianl/Trouser-Streak/issues/134
        Their inspiration was this Youtube video by @scilangaming:
        https://www.youtube.com/watch?v=q99eqD_fBqo
        This module has been edited and had features added to it by etianl :)*/

package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.entity.player.BreakBlockEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import pwn.noobs.trouserstreak.Trouser;

import java.util.List;

public class InfiniteTools extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> autoSlot = sgGeneral.add(new BoolSetting.Builder().name("Swap to Weakest Tool").description("Swaps to the weakest tool on your hotbar of the same variety as what you are using.").defaultValue(true).build());
    private final Setting<Integer> targetSlot = sgGeneral.add(new IntSetting.Builder().name("target-slot").description("The hotbar slot to swap to when breaking blocks.").sliderRange(1, 9).defaultValue(1).min(1).visible(() -> !autoSlot.get()).build());
    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder().name("swap-back").description("Swap back to the original slot after a short delay.").defaultValue(true).build());
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("swap-back-delay").description("Delay in ticks before swapping back to the previous slot.").sliderRange(1, 20).defaultValue(1).min(1).visible(swapBack::get).build());
    private final Setting<List<Block>> blacklist = sgGeneral.add(new BlockListSetting.Builder()
            .name("No swap on these blocks")
            .description("If mining these blocks do not swap tools.")
            .build()
    );
    private int prevSlot = -1;
    private int dDelay = 0;
    public InfiniteTools() {
        super(Trouser.Main, "InfiniteTools", "Swaps to a junk version of the same tool you are using to conserve durability of the good tool. Stone is recommended for junk tools. Supports Pickaxes, Axes, Shovels, Hoes, and Swords.");
    }
    @EventHandler
    private void onBreakBlock(BreakBlockEvent event) {
        if (mc.player == null || mc.level == null || blacklist.get().contains(mc.level.getBlockState(event.blockPos).getBlock())) return;

        if (swapBack.get()) prevSlot = mc.player.getInventory().getSelectedSlot();
        if (autoSlot.get()) InvUtils.swap(findToolSlot(mc.player.getInventory().getSelectedSlot(), mc.level.getBlockState(event.blockPos)), false);
        else InvUtils.swap(targetSlot.get()-1, false);
        if (swapBack.get() && prevSlot != -1) {
            dDelay = delay.get();
        }
    }
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (dDelay > 0) {
            dDelay--;
            if (dDelay == 0 && prevSlot != -1) {
                InvUtils.swap(prevSlot, false);
                prevSlot = -1;
            }
        }
    }
    private int findToolSlot(int itemNumber, BlockState state) {
        int finalnumber = itemNumber;
        ItemStack workingitem = mc.player.getInventory().getItem(itemNumber);
        float lowestminingspeed = workingitem.getDestroySpeed(state);

        TagKey<Item> pickaxeTag = ItemTags.PICKAXES;
        TagKey<Item> axeTag = ItemTags.AXES;
        TagKey<Item> shovelTag = ItemTags.SHOVELS;
        TagKey<Item> hoeTag = ItemTags.HOES;
        TagKey<Item> swordTag = ItemTags.SWORDS;

        TagKey<Item> toolTag = null;
        if (workingitem.tags().toList().contains(pickaxeTag)) toolTag = pickaxeTag;
        else if (workingitem.tags().toList().contains(axeTag)) toolTag = axeTag;
        else if (workingitem.tags().toList().contains(shovelTag)) toolTag = shovelTag;
        else if (workingitem.tags().toList().contains(hoeTag)) toolTag = hoeTag;
        else if (workingitem.tags().toList().contains(swordTag)) toolTag = swordTag;

        for (int i = 0; i < 9; i++) {
            ItemStack item = mc.player.getInventory().getItem(i);
            if (item.tags().toList().contains(toolTag)) {
                float miningSpeed = item.getDestroySpeed(state);
                if (miningSpeed < lowestminingspeed) {
                    lowestminingspeed = miningSpeed;
                    finalnumber = i;
                }
            }
        }
        return finalnumber;
    }
}