//written by etianl
//Based on the method described by Marlow here: https://www.youtube.com/watch?v=7t0PyqYsac8
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import pwn.noobs.trouserstreak.Trouser;

public class KnockBackModifier extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public enum directionMode {
        Degrees,
        TowardSelf,
        Left, Right
    }
    private final Setting<directionMode> mode = sgGeneral.add(new EnumSetting.Builder<directionMode>()
            .name("Direction")
            .description("Direction of the Knockback that is inflicted")
            .defaultValue(directionMode.Degrees)
            .build()
    );
    private final Setting<Integer> degrees = sgGeneral.add(new IntSetting.Builder()
            .name("Knockback Direction (degrees)")
            .description("Relative to your Yaw. 90 = Right, 270 = Left, 180 = TowardSelf, 0 = Regular Knockback.")
            .defaultValue(180)
            .min(0)
            .max(360)
            .sliderRange(0, 360)
            .visible(() -> mode.get() == directionMode.Degrees)
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
        if (attacking || mc.player == null || mc.level == null) return;
        if (!(event.packet instanceof ServerboundAttackPacket)) return;
        Registry<Enchantment> enchantmentRegistry = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        if (!mc.player.isSprinting() && mc.player.getMainHandItem().getEnchantments().getLevel(enchantmentRegistry.getOrThrow(Enchantments.KNOCKBACK)) == 0) return;
        attacking = true;

        event.cancel(); //cancel original attack

        originalYaw = mc.player.getYRot();
        float modifiedYaw = mc.player.getYRot();

        switch (mode.get()) {
            case TowardSelf -> modifiedYaw = originalYaw + 180f;
            case Left -> modifiedYaw = originalYaw - 90f;
            case Right -> modifiedYaw = originalYaw + 90f;
            case Degrees -> modifiedYaw = (originalYaw + degrees.get()) % 360f;
        }

        mc.getConnection().send(
                new ServerboundMovePlayerPacket.Rot(modifiedYaw, mc.player.getXRot(), mc.player.onGround(), mc.player.horizontalCollision)
        );

        mc.getConnection().send(event.packet); //send attack after we look for modified knockback
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        if (attacking) {
            mc.player.setYRot(originalYaw); //incase sending the modified yaw packet affects you clientside
            mc.player.yHeadRot = originalYaw;
            attacking = false;
        }
    }
}