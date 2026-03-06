//written by etianl
//Based on the method described by Marlow here: https://www.youtube.com/watch?v=7t0PyqYsac8
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerInteractEntityC2SPacket;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import pwn.noobs.trouserstreak.Trouser;

public class KnockBackModifier extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public enum directionMode {
        TowardSelf,
        Left, Right
    }
    private final Setting<directionMode> mode = sgGeneral.add(new EnumSetting.Builder<directionMode>()
            .name("Direction")
            .description("Direction of the Knockback that is inflicted")
            .defaultValue(directionMode.TowardSelf)
            .build()
    );
    public KnockBackModifier() {
        super(Trouser.Main, "KnockBackModifier", "Modifies the direction of the knockback that you inflict. Requires you to be sprinting or using a knockback enchanted item.");
    }

    private boolean attacking;
    private float originalYaw;

    @Override
    public void onActivate() {
        attacking = false;
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (attacking || mc.player == null || mc.world == null) return;
        if (!(event.packet instanceof IPlayerInteractEntityC2SPacket packet)) return;
        if (packet.getType() != PlayerInteractEntityC2SPacket.InteractType.ATTACK) return;
        ItemStack mainHand = mc.player.getMainHandStack();
        if (!mc.player.isSprinting() && EnchantmentHelper.getLevel(Enchantments.KNOCKBACK, mainHand) == 0) return;
        attacking = true;

        event.cancel(); //cancel original attack

        originalYaw = mc.player.getYaw();
        float modifiedYaw = mc.player.getYaw();

        switch (mode.get()) {
            case TowardSelf -> modifiedYaw = originalYaw + 180f;
            case Left -> modifiedYaw = originalYaw - 90f;
            case Right -> modifiedYaw = originalYaw + 90f;
        }

        mc.getNetworkHandler().sendPacket(
                new PlayerMoveC2SPacket.LookAndOnGround(modifiedYaw, mc.player.getPitch(), mc.player.isOnGround())
        );

        mc.getNetworkHandler().sendPacket(event.packet); //send attack after we look for modified knockback
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        if (attacking) {
            mc.player.setYaw(originalYaw); //incase sending the modified yaw packet affects you clientside
            mc.player.headYaw = originalYaw;
            attacking = false;
        }
    }
}