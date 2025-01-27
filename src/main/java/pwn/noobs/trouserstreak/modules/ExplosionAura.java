package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import pwn.noobs.trouserstreak.Trouser;

public class ExplosionAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public final Setting<Integer> tickdelay = sgGeneral.add(new IntSetting.Builder()
            .name("ExplosionTickDelayAroundPlayer")
            .description("Tick Delay for exploding around the player.")
            .defaultValue(5)
            .min(0)
            .sliderMax(100)
            .build()
    );
    public final Setting<Boolean> click = sgGeneral.add(new BoolSetting.Builder()
            .name("ClickExplosion")
            .description("spawns on target")
            .defaultValue(false)
            .build()
    );
    private final Setting<Integer> cpower = sgGeneral.add(new IntSetting.Builder()
            .name("ClickPower")
            .description("how big explosion")
            .defaultValue(10)
            .min(1)
            .sliderMax(127)
            .visible(click::get)
            .build());
    public final Setting<Boolean> auto = sgGeneral.add(new BoolSetting.Builder()
            .name("FULLAUTO")
            .description("FULL AUTO BABY!")
            .defaultValue(false)
            .visible(click::get)
            .build()
    );
    public final Setting<Integer> atickdelay = sgGeneral.add(new IntSetting.Builder()
            .name("FULLAUTOTickDelay")
            .description("Tick Delay for FULLAUTO option.")
            .defaultValue(2)
            .min(0)
            .sliderMax(20)
            .visible(() -> auto.get() && click.get())
            .build()
    );
    private final Setting<Integer> power = sgGeneral.add(new IntSetting.Builder()
            .name("ExplosionPower")
            .description("Explosion Power at the character position.")
            .defaultValue(10)
            .min(1)
            .sliderMax(127)
            .build());
    private int ticks = 0;
    private int aticks = 0;
    public ExplosionAura() {
        super(Trouser.Main, "ExplosionAura", "You explode as you move. Must be in creative.");
    }

    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (event.screen instanceof DisconnectedScreen) {
            toggle();
        }
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        toggle();
    }

    @EventHandler
    private void onMouseButton(MouseButtonEvent event) {
        if (mc.options.attackKey.isPressed() && mc.currentScreen == null && mc.player.getAbilities().creativeMode) {
            if (click.get()) {
                ItemStack rst = mc.player.getMainHandStack();
                BlockHitResult bhr = new BlockHitResult(mc.player.getEyePos(), Direction.DOWN, BlockPos.ofFloored(mc.player.getEyePos()), false);
                ItemStack Creeper = new ItemStack(Items.CREEPER_SPAWN_EGG);
                var changes = ComponentChanges.builder()
                        .add(DataComponentTypes.ENTITY_DATA, createEntityData(true))
                        .build();
                Creeper.applyChanges(changes);

                mc.interactionManager.clickCreativeStack(Creeper, 36 + mc.player.getInventory().selectedSlot);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (mc.player != null && mc.interactionManager != null && mc.player.getAbilities().creativeMode) {
            if (auto.get() && mc.options.attackKey.isPressed() && mc.currentScreen == null && mc.player.getAbilities().creativeMode) {
                if (click.get()) {
                    if (aticks <= atickdelay.get()) {
                        aticks++;
                    } else if (aticks > atickdelay.get()) {
                        ItemStack rst = mc.player.getMainHandStack();
                        BlockHitResult bhr = new BlockHitResult(mc.player.getEyePos(), Direction.DOWN, BlockPos.ofFloored(mc.player.getEyePos()), false);
                        ItemStack Creeper = new ItemStack(Items.CREEPER_SPAWN_EGG);
                        var changes = ComponentChanges.builder()
                                .add(DataComponentTypes.ENTITY_DATA, createEntityData(true))
                                .build();
                        Creeper.applyChanges(changes);

                        mc.interactionManager.clickCreativeStack(Creeper, 36 + mc.player.getInventory().selectedSlot);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                        mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                        aticks = 0;
                    }
                }
            }
            if (mc.options.forwardKey.isPressed() || mc.options.jumpKey.isPressed() || mc.options.sneakKey.isPressed() || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed() || mc.options.backKey.isPressed()) {
                if (ticks <= tickdelay.get()) {
                    ticks++;
                } else if (ticks > tickdelay.get()) {
                    ItemStack rst = mc.player.getMainHandStack();
                    BlockHitResult bhr = new BlockHitResult(mc.player.getPos(), Direction.DOWN, BlockPos.ofFloored(mc.player.getPos()), false);
                    ItemStack Creeper = new ItemStack(Items.CREEPER_SPAWN_EGG);
                    var changes = ComponentChanges.builder()
                            .add(DataComponentTypes.ENTITY_DATA, createEntityData(false))
                            .build();
                    Creeper.applyChanges(changes);

                    mc.interactionManager.clickCreativeStack(Creeper, 36 + mc.player.getInventory().selectedSlot);
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                    mc.interactionManager.clickCreativeStack(rst, 36 + mc.player.getInventory().selectedSlot);
                    ticks = 0;
                }
            }
        } else {
            error("You need to be in creative mode.");
            toggle();
        }
    }

    private NbtComponent createEntityData(boolean click) {
        NbtCompound entityTag = new NbtCompound();
        entityTag.putString("id", "minecraft:creeper");
        if (click) {
            HitResult hr = mc.cameraEntity.raycast(600, 0, true);
            Vec3d owo = hr.getPos();
            BlockPos pos = BlockPos.ofFloored(owo);
            NbtList Pos = new NbtList();
            Pos.add(NbtDouble.of(pos.getX()));
            Pos.add(NbtDouble.of(pos.getY()));
            Pos.add(NbtDouble.of(pos.getZ()));
            entityTag.put("Pos", Pos);
        }
        entityTag.putBoolean("ignited", true);
        entityTag.putBoolean("Invulnerable", true);
        entityTag.putInt("Fuse", 0);
        entityTag.putBoolean("NoGravity", true);
        entityTag.putInt("ExplosionRadius", cpower.get());
        return NbtComponent.of(entityTag);
    }
}