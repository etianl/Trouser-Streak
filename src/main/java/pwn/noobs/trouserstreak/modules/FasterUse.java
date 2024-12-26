package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import pwn.noobs.trouserstreak.Trouser;

public class FasterUse extends Module {
    private final SettingGroup sgGeneral = settings.createGroup("Rate");

    public FasterUse() {
        super(Trouser.Main, "Faster-use", "Fast use but faster... WAY faster");
    }

    private final Setting<Integer> uses = sgGeneral.add(new IntSetting.Builder()
            .name("Times per tick")
            .description("Amount of uses each tick")
            .defaultValue(5)
            .min(1)
            .sliderMax(15)
            .build()
    );

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        for (int i = 0; i < uses.get(); i++) {
            if(mc.options.useKey.isPressed()) {
                BlockHitResult bhr = new BlockHitResult(mc.player.getEyePos(), Direction.DOWN, BlockPos.ofFloored(mc.player.getEyePos()), false);
                mc.interactionManager.interactBlock(this.mc.player, Hand.MAIN_HAND, bhr);
                mc.interactionManager.interactItem(this.mc.player, Hand.MAIN_HAND);
            }
        }
    }
}
