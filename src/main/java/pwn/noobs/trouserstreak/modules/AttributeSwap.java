/*Thank you to [DonKisser](https://github.com/DonKisser) for making the original module for us!
        Their inspiration was this Youtube video by @scilangaming:
        https://www.youtube.com/watch?v=q99eqD_fBqo*/

package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerInteractEntityC2SPacket;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import pwn.noobs.trouserstreak.Trouser;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;

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
        if (mc.player == null || mc.world == null || !autoLunge.get()) return;
        if (!mc.options.attackKey.isPressed()) return;
        if (swapBack.get()) {
            prevSlot = mc.player.getInventory().selectedSlot;
        }

        didSwap = false;

        if (enchantmentRegistry == null) enchantmentRegistry = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);

        int bestSlot = -1;
        int bestLevel = 0;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getMainStacks().get(i);

            int level = EnchantmentHelper.getLevel(enchantmentRegistry.getOrThrow(Enchantments.LUNGE), stack);
            if (level > 0 && level >= bestLevel) {
                bestSlot = i;
                bestLevel = level;
            }
        }

        if (bestSlot != -1) {
            InvUtils.swap(bestSlot, false);
            didSwap = true;
            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.STAB,
                    mc.player.getBlockPos(),
                    mc.player.getFacing()
            ));
        }

        if (swapBack.get() && didSwap) {
            dDelay = delay.get();
        }
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (mc.player == null || mc.world == null || isSwapping || autoLunge.get()) return;

        if (event.packet instanceof PlayerInteractEntityC2SPacket interact) {
            int entityId = ((IPlayerInteractEntityC2SPacket) interact).meteor$getEntity().getId();

            Entity entity = mc.world.getEntityById(entityId);

            if (entity != null) {
                interact.handle(new PlayerInteractEntityC2SPacket.Handler() {
                    @Override
                    public void interact(net.minecraft.util.Hand hand) {}

                    @Override
                    public void interactAt(net.minecraft.util.Hand hand, net.minecraft.util.math.Vec3d pos) {}

                    @Override
                    public void attack() {
                        performAttributeSwap(entity, false);
                    }
                });
            }
        }

        if (event.packet instanceof PlayerActionC2SPacket action) {
            if (action.getAction() == PlayerActionC2SPacket.Action.STAB) {
                Entity targetEntity = (mc.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult hit) ? hit.getEntity() : null;

                if (targetEntity != null) {
                    performAttributeSwap(targetEntity,true);
                }
            }
        }
    }

    private void performAttributeSwap(Entity targetEntity, Boolean stabbed) {
        if (mc.player == null || mc.world == null) return;

        isSwapping = true;
        try {
            if (swapBack.get()) {
                prevSlot = mc.player.getInventory().selectedSlot;
            }
            didSwap = false;

            if (shieldBreaker.get()) {
                if (targetEntity instanceof PlayerEntity player) {
                    if (player.isBlocking()) {
                        for (int i = 0; i < 9; i++) {
                            ItemStack stack = mc.player.getInventory().getMainStacks().get(i);
                            if (stack.getItem() instanceof AxeItem) {
                                InvUtils.swap(i, false);
                                if (stabbed) {
                                    mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(targetEntity, mc.player.isSneaking()));
                                }
                                didSwap = true;
                                break;
                            }
                        }
                    } else if (!noswap.get()) {
                        InvUtils.swap(targetSlot.get() - 1, false);
                        if (stabbed) {
                            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(targetEntity, mc.player.isSneaking()));
                        }
                        didSwap = true;
                    }
                } else {
                    InvUtils.swap(targetSlot.get() - 1, false);
                    if (stabbed) {
                        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(targetEntity, mc.player.isSneaking()));
                    }
                    didSwap = true;
                }
            } else {
                InvUtils.swap(targetSlot.get() - 1, false);
                if (stabbed) {
                    mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(targetEntity, mc.player.isSneaking()));
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