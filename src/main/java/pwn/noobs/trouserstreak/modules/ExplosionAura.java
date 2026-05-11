package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
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
    private final Setting<Integer> power = sgGeneral.add(new IntSetting.Builder()
            .name("ExplosionPower")
            .description("Explosion Power at the character position.")
            .defaultValue(10)
            .min(1)
            .sliderMax(127)
            .build());
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

    public ExplosionAura() {
        super(Trouser.operator, "ExplosionAura", "You explode as you move. Must be in creative.");
    }

    private int ticks=0;
    private int aticks=0;

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
    private void onMouseButton(MouseClickEvent event) {
        if (mc.options.keyAttack.isDown() && mc.screen == null && mc.player.getAbilities().instabuild) {
            if (click.get()) {
                ItemStack rst = mc.player.getMainHandItem();
                BlockHitResult bhr = new BlockHitResult(mc.player.getEyePosition(), Direction.DOWN, BlockPos.containing(mc.player.getEyePosition()), false);
                ItemStack Creeper = new ItemStack(Items.CREEPER_SPAWN_EGG);
                var changes = DataComponentPatch.builder()
                        .set(DataComponents.ENTITY_DATA, createEntityData(true))
                        .build();
                Creeper.applyComponentsAndValidate(changes);

                mc.gameMode.handleCreativeModeItemAdd(Creeper, 36 + mc.player.getInventory().getSelectedSlot());
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, bhr);
                mc.gameMode.handleCreativeModeItemAdd(rst, 36 + mc.player.getInventory().getSelectedSlot());
            }
        }
    }
    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (mc.player != null && mc.gameMode != null && mc.player.getAbilities().instabuild) {
            if (auto.get() && mc.options.keyAttack.isDown() && mc.screen == null && mc.player.getAbilities().instabuild) {
                if (click.get()) {
                    if (aticks<=atickdelay.get()){
                        aticks++;
                    } else if (aticks>atickdelay.get()) {
                        ItemStack rst = mc.player.getMainHandItem();
                        BlockHitResult bhr = new BlockHitResult(mc.player.getEyePosition(), Direction.DOWN, BlockPos.containing(mc.player.getEyePosition()), false);
                        ItemStack Creeper = new ItemStack(Items.CREEPER_SPAWN_EGG);
                        var changes = DataComponentPatch.builder()
                                .set(DataComponents.ENTITY_DATA, createEntityData(true))
                                .build();
                        Creeper.applyComponentsAndValidate(changes);

                        mc.gameMode.handleCreativeModeItemAdd(Creeper, 36 + mc.player.getInventory().getSelectedSlot());
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, bhr);
                        mc.gameMode.handleCreativeModeItemAdd(rst, 36 + mc.player.getInventory().getSelectedSlot());
                        aticks=0;
                    }
                }
            }
            if (mc.options.keyUp.isDown()||mc.options.keyJump.isDown()||mc.options.keyShift.isDown()||mc.options.keyLeft.isDown()||mc.options.keyRight.isDown()||mc.options.keyDown.isDown()) {
                if (ticks<=tickdelay.get()){
                    ticks++;
                } else if (ticks>tickdelay.get()){
                    ItemStack rst = mc.player.getMainHandItem();
                    BlockHitResult bhr = new BlockHitResult(mc.player.position(), Direction.DOWN, BlockPos.containing(mc.player.position()), false);
                    ItemStack Creeper = new ItemStack(Items.CREEPER_SPAWN_EGG);
                    var changes = DataComponentPatch.builder()
                            .set(DataComponents.ENTITY_DATA, createEntityData(false))
                            .build();
                    Creeper.applyComponentsAndValidate(changes);

                    mc.gameMode.handleCreativeModeItemAdd(Creeper, 36 + mc.player.getInventory().getSelectedSlot());
                    mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, bhr);
                    mc.gameMode.handleCreativeModeItemAdd(rst, 36 + mc.player.getInventory().getSelectedSlot());
                    ticks=0;
                }
            }
        } else {
            error("You need to be in creative mode.");
            toggle();
        }
    }
    private TypedEntityData<EntityType<?>> createEntityData(boolean click) {
        CompoundTag entityTag = new CompoundTag();
        entityTag.putString("id", "minecraft:creeper");
        if (click) {
            HitResult hr = mc.getCameraEntity().pick(600, 0, true);
            Vec3 owo = hr.getLocation();
            BlockPos pos = BlockPos.containing(owo);
            ListTag Pos = new ListTag();
            Pos.add(DoubleTag.valueOf(pos.getX()));
            Pos.add(DoubleTag.valueOf(pos.getY()));
            Pos.add(DoubleTag.valueOf(pos.getZ()));
            entityTag.put("Pos", Pos);
        }
        entityTag.putBoolean("ignited", true);
        entityTag.putBoolean("Invulnerable", true);
        entityTag.putInt("Fuse", 0);
        entityTag.putBoolean("NoGravity", true);
        entityTag.putInt("ExplosionRadius", click ? cpower.get() : power.get());
        return TypedEntityData.of(EntityType.CREEPER, entityTag);
    }
}