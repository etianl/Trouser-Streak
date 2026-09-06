/*Thank you to [DonKisser](https://github.com/DonKisser) for making the original module for us!
        Their inspiration was this Youtube video by @scilangaming:
        https://www.youtube.com/watch?v=q99eqD_fBqo*/

package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import pwn.noobs.trouserstreak.Trouser;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

public class AttributeSwap extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> autoLunge = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-lunge-swap")
            .description("Swap to a lunge enchanted spear from your hotbar when pressing the attack key for a lunge forward.")
            .defaultValue(false)
            .build());
    private final Setting<Boolean> shieldBreaker = sgGeneral.add(new BoolSetting.Builder()
            .name("shield-breaker")
            .description("Swap to an axe from your hotbar when attacking to disable the shield of someone who is blocking.")
            .defaultValue(false)
            .visible(() -> !autoLunge.get())
            .build());
    private final Setting<Boolean> noswap = sgGeneral.add(new BoolSetting.Builder()
            .name("Shield Breaker No Swap")
            .description("Do not attribute swap to another item after shield is broken")
            .defaultValue(true)
            .visible(() -> !autoLunge.get() && shieldBreaker.get())
            .build());
    private final Setting<Integer> targetSlot = sgGeneral.add(new IntSetting.Builder()
            .name("target-slot")
            .description("The hotbar slot to swap to when attacking.")
            .sliderRange(1, 9)
            .defaultValue(1)
            .min(1)
            .visible(() -> !(noswap.get() && shieldBreaker.get()) && !autoLunge.get())
            .build());
    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder()
            .name("swap-back")
            .description("Swap back to the original slot after a short delay.")
            .defaultValue(true)
            .build());
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("swap-back-delay")
            .description("Delay in ticks before swapping back to the previous slot.")
            .sliderRange(1, 20)
            .defaultValue(1)
            .min(1)
            .visible(swapBack::get)
            .build());

    private int prevSlot = -1;
    private int dDelay = 0;
    private boolean didSwap = false;
    private boolean isSwapping = false;
    private Registry<Enchantment> enchantmentRegistry;

    public AttributeSwap() {
        super(Trouser.Main, "AttributeSwap", "Swaps attributes of the main hand item with the target slot on attack");
    }

    @EventHandler
    private void onMouseButton(MouseClickEvent event) {
        if (mc.player == null || mc.level == null || !autoLunge.get()) return;
        if (!mc.options.keyAttack.isDown()) return;
        if (swapBack.get()) {
            prevSlot = mc.player.getInventory().getSelectedSlot();
        }

        didSwap = false;

        if (enchantmentRegistry == null) enchantmentRegistry = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

        int bestSlot = -1;
        int bestLevel = 0;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getNonEquipmentItems().get(i);

            int level = EnchantmentHelper.getItemEnchantmentLevel(enchantmentRegistry.getOrThrow(Enchantments.LUNGE), stack);
            if (level > 0 && level >= bestLevel) {
                bestSlot = i;
                bestLevel = level;
            }
        }

        if (bestSlot != -1) {
            InvUtils.swap(bestSlot, false);
            didSwap = true;
            mc.player.connection.send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.STAB,
                    mc.player.blockPosition(),
                    mc.player.getNearestViewDirection()
            ));
        }

        if (swapBack.get() && didSwap) {
            dDelay = delay.get();
        }
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (mc.player == null || mc.level == null || isSwapping || autoLunge.get()) return;

        if (event.packet instanceof ServerboundAttackPacket interact) {
            int entityId = interact.entityId();

            Entity entity = mc.level.getEntity(entityId);

            if (entity != null) {
                performAttributeSwap(entity, false);
            }
        }

        if (event.packet instanceof ServerboundPlayerActionPacket action) {
            if (action.getAction() == ServerboundPlayerActionPacket.Action.STAB) {
                Entity targetEntity = (mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit) ? hit.getEntity() : null;

                if (targetEntity != null) {
                    performAttributeSwap(targetEntity,true);
                }
            }
        }
    }

    private void performAttributeSwap(Entity targetEntity, Boolean stabbed) {
        if (mc.player == null || mc.level == null) return;

        isSwapping = true;
        try {
            if (swapBack.get()) {
                prevSlot = mc.player.getInventory().getSelectedSlot();
            }
            didSwap = false;

            if (shieldBreaker.get()) {
                if (targetEntity instanceof Player player) {
                    if (player.isBlocking()) {
                        for (int i = 0; i < 9; i++) {
                            ItemStack stack = mc.player.getInventory().getNonEquipmentItems().get(i);
                            if (stack.getItem() instanceof AxeItem) {
                                InvUtils.swap(i, false);
                                if (stabbed) {
                                    mc.getConnection().send(new ServerboundAttackPacket(targetEntity.getId()));
                                }
                                didSwap = true;
                                break;
                            }
                        }
                    } else if (!noswap.get()) {
                        InvUtils.swap(targetSlot.get() - 1, false);
                        if (stabbed) {
                            mc.getConnection().send(new ServerboundAttackPacket(targetEntity.getId()));
                        }
                        didSwap = true;
                    }
                } else {
                    InvUtils.swap(targetSlot.get() - 1, false);
                    if (stabbed) {
                        mc.getConnection().send(new ServerboundAttackPacket(targetEntity.getId()));
                    }
                    didSwap = true;
                }
            } else {
                InvUtils.swap(targetSlot.get() - 1, false);
                if (stabbed) {
                    mc.getConnection().send(new ServerboundAttackPacket(targetEntity.getId()));
                }
                didSwap = true;
            }

            if (swapBack.get() && didSwap) {
                dDelay = delay.get();
            }
        } finally {
            isSwapping = false;
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
}