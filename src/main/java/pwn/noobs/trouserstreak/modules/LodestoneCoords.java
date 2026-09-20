//written by https://github.com/Dylanvip2024
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;
import pwn.noobs.trouserstreak.Trouser;

import java.util.Optional;

/**
 * LodestoneCoords 模块
 * 开启后解析自己手中的磁石指南针坐标，显示在聊天栏后立即关闭。
 */
public class LodestoneCoords extends Module {

    public LodestoneCoords() {
        super(Trouser.baseHunting, "LodestoneCoords", "Decodes the coordinates bound to the lodestone compass in your hand.");
    }

    @Override
    public void onActivate() {
        if (mc.player == null || mc.level == null) {
            toggle();
            return;
        }

        parseOwnCompass();
        toggle();
    }

    private void parseOwnCompass() {
        // 检查主手
        ItemStack mainHand = mc.player.getMainHandItem();
        if (!mainHand.isEmpty() && mainHand.is(Items.COMPASS)) {
            String result = decodeCompass(mainHand);
            if (result != null) {
                info("§a[Decoded] §r" + result);
                return;
            }
        }

        // 检查副手
        ItemStack offHand = mc.player.getOffhandItem();
        if (!offHand.isEmpty() && offHand.is(Items.COMPASS)) {
            String result = decodeCompass(offHand);
            if (result != null) {
                info("§a[Decoded] §r" + result);
                return;
            }
        }

        info("§cNo bound Lodestone compass found in hand.\n");
    }

    /**
     * 从指南针 ItemStack 中提取磁石追踪器组件并格式化输出。
     *
     * @param stack 指南针物品
     * @return 格式化后的坐标信息，如果指南针未绑定磁石则返回 null
     */
    private String decodeCompass(ItemStack stack) {
        try {
            LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
            if (tracker == null) return null;

            Optional<GlobalPos> target = tracker.target();
            if (target.isEmpty()) return null;

            GlobalPos globalPos = target.get();
            int x = globalPos.pos().getX();
            int y = globalPos.pos().getY();
            int z = globalPos.pos().getZ();
            String dimension = globalPos.dimension().toString();

            return String.format("Dimension: §a%s §f| Coordinate: §eX:%d Y:%d Z:%d",
                    dimension, x, y, z);
        } catch (Exception e) {
            return null;
        }
    }
}