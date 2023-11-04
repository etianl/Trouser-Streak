package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import pwn.noobs.trouserstreak.Trouser;

import java.util.Random;

public class AirstrikePlus extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgOptions = settings.createGroup("Nbt Options");

    private final Setting<Boolean> disconnectdisable = sgGeneral.add(new BoolSetting.Builder()
            .name("Disable on Disconnect")
            .description("Disables module on disconnecting")
            .defaultValue(false)
            .build());
    private final Setting<String> entity = sgGeneral.add(new StringSetting.Builder()
            .name("Entity to Spawn")
            .description("What is created. Ex: fireball, villager, minecart, lightning, magma_cube, tnt")
            .defaultValue("fireball")
            .build());
    private final Setting<Boolean> mixer = sgGeneral.add(new BoolSetting.Builder()
            .name("Mixer")
            .description("Mixes entities.")
            .defaultValue(false)
            .build());
    private final Setting<String> entity2 = sgGeneral.add(new StringSetting.Builder()
            .name("Entity2 to Spawn")
            .description("What is created. Ex: fireball, villager, minecart, lightning, magma_cube, tnt")
            .defaultValue("wither")
            .visible(() -> mixer.get())
            .build());
    private final Setting<String> nom = sgGeneral.add(new StringSetting.Builder()
            .name("Custom Name")
            .description("Name the Entity")
            .defaultValue("MOUNTAINSOFLAVAINC").build());
    private final Setting<String> nomcolor = sgGeneral.add(new StringSetting.Builder()
            .name("Custom Name Color")
            .description("Color the Name")
            .defaultValue("red")
            .build());
    public final Setting<Boolean> customname = sgOptions.add(new BoolSetting.Builder()
            .name("CustomNameVisible")
            .description("CustomNameVisible or not.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Integer> health = sgOptions.add(new IntSetting.Builder()
            .name("Health Points")
            .description("How much health.")
            .defaultValue(100)
            .min(0)
            .sliderRange(0, 100)
            .build());
    private final Setting<Integer> absorption = sgOptions.add(new IntSetting.Builder()
            .name("Absorption Points")
            .description("How much absorption.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 100)
            .build());
    private final Setting<Integer> age = sgOptions.add(new IntSetting.Builder()
            .name("Age")
            .description("It's age, 0 is baby.")
            .defaultValue(1)
            .min(0)
            .sliderRange(0, 100)
            .build());
    public final Setting<Boolean> invincible = sgOptions.add(new BoolSetting.Builder()
            .name("Invulnerable")
            .description("Invulnerable or not")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> persist = sgOptions.add(new BoolSetting.Builder()
            .name("Never Despawn")
            .description("adds PersistenceRequired tag.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> noAI = sgOptions.add(new BoolSetting.Builder()
            .name("NoAI")
            .description("NoAI")
            .defaultValue(false)
            .build()
    );
    public final Setting<Boolean> falsefire = sgOptions.add(new BoolSetting.Builder()
            .name("HasVisualFire")
            .description("HasVisualFire or not")
            .defaultValue(false)
            .build()
    );
    public final Setting<Boolean> nograv = sgOptions.add(new BoolSetting.Builder()
            .name("NoGravity")
            .description("NoGravity or not")
            .defaultValue(false)
            .build()
    );
    public final Setting<Boolean> silence = sgOptions.add(new BoolSetting.Builder()
            .name("Silent")
            .description("adds Silent tag.")
            .defaultValue(false)
            .build()
    );
    public final Setting<Boolean> glow = sgOptions.add(new BoolSetting.Builder()
            .name("Glowing")
            .description("Glowing or not")
            .defaultValue(false)
            .build()
    );
    public final Setting<Boolean> ignite = sgOptions.add(new BoolSetting.Builder()
            .name("Ignited")
            .description("Pre-ignite creeper or not.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> powah = sgOptions.add(new BoolSetting.Builder()
            .name("Charged Creeper")
            .description("powered creeper or not.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Integer> fuse = sgOptions.add(new IntSetting.Builder()
            .name("Creeper/TNT Fuse")
            .description("In ticks")
            .defaultValue(20)
            .min(0)
            .sliderRange(0, 120)
            .build());
    private final Setting<Integer> exppower = sgOptions.add(new IntSetting.Builder()
            .name("ExplosionPower/Radius")
            .description("For Creepers and Fireballs")
            .defaultValue(10)
            .min(1)
            .sliderMax(127)
            .build());
    private final Setting<Integer> size = sgOptions.add(new IntSetting.Builder()
            .name("Slime/Magma Cube Size")
            .description("It's size, 100 is really big.")
            .defaultValue(1)
            .min(0)
            .sliderRange(0, 100)
            .build());
    private final Setting<Integer> radius = sgGeneral.add(new IntSetting.Builder()
            .name("radius")
            .description("radius they spawn from the player")
            .defaultValue(30)
            .sliderRange(1, 100)
            .min(1)
            .build());

    private final Setting<Integer> height = sgGeneral.add(new IntSetting.Builder()
            .name("HeightAboveHead")
            .description("How far from your Characters Y level to spawn at.")
            .defaultValue(20)
            .sliderRange(-63, 319)
            .build());

    private final Setting<Integer> speed = sgGeneral.add(new IntSetting.Builder()
            .name("speed")
            .description("speed of entities")
            .defaultValue(5)
            .sliderRange(1, 10)
            .min(1)
            .max(10)
            .build());

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("its in ticks")
            .defaultValue(2)
            .sliderRange(0, 20)
            .min(0)
            .build());


    public AirstrikePlus() {
        super(Trouser.Main, "Airstrike+", "Rains things down from the sky");
    }

    final Random r = new Random();
    Vec3d origin = null;
    int i = 0;
    private int mix=0;

    private Vec3d pickRandomPos() {
        double x = r.nextDouble(radius.get() * 2) - radius.get() + origin.x;
        double y = mc.player.getY()+height.get();
        double z = r.nextDouble(radius.get() * 2) - radius.get() + origin.z;
        return new Vec3d(x, y, z);
    }
    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (disconnectdisable.get() && event.screen instanceof DisconnectedScreen) {
            toggle();
        }
    }
    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (disconnectdisable.get())toggle();
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        origin = mc.player.getPos();
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        ItemStack bomb = new ItemStack(Items.SALMON_SPAWN_EGG);
        ItemStack bfr = mc.player.getMainHandStack();
        BlockHitResult bhr = new BlockHitResult(mc.player.getPos(), Direction.DOWN, new BlockPos(mc.player.getBlockPos()), false);
        Vec3d cpos = pickRandomPos();
        NbtCompound tag = new NbtCompound();
        NbtList speedlist = new NbtList();
        NbtList pos = new NbtList();
        i++;
        if (mc.player.getAbilities().creativeMode) {
            if (i >= delay.get()) {
                if (!mixer.get()){
                    String entityName = entity.get().trim().replace(" ", "_");
                    NbtCompound display = new NbtCompound();
                    display.putString("Name", "{\"text\":\"" + nom.get() + "\",\"color\":\"" + nomcolor.get() + "\"}");
                    tag.put("display", display);
                    NbtCompound entityTag = new NbtCompound();
                    speedlist.add(NbtDouble.of(0));
                    speedlist.add(NbtDouble.of(-speed.get()));
                    speedlist.add(NbtDouble.of(0));
                    pos.add(NbtDouble.of(cpos.x));
                    pos.add(NbtDouble.of(mc.player.getY()+height.get()));
                    pos.add(NbtDouble.of(cpos.z));
                    entityTag.put("power", speedlist);
                    entityTag.put("Motion", speedlist);
                    entityTag.put("Pos", pos);
                    entityTag.putString("id", "minecraft:" + entityName);
                    entityTag.putInt("Health", health.get());
                    entityTag.putInt("AbsorptionAmount", absorption.get());
                    entityTag.putInt("Age", age.get());
                    entityTag.putInt("ExplosionPower", exppower.get());
                    entityTag.putInt("ExplosionRadius", exppower.get());
                    if (invincible.get())entityTag.putBoolean("Invulnerable", invincible.get());
                    if (silence.get())entityTag.putBoolean("Silent", silence.get());
                    if (glow.get())entityTag.putBoolean("Glowing", glow.get());
                    if (persist.get())entityTag.putBoolean("PersistenceRequired", persist.get());
                    if (nograv.get())entityTag.putBoolean("NoGravity", nograv.get());
                    if(noAI.get())entityTag.putBoolean("NoAI", noAI.get());
                    if(falsefire.get())entityTag.putBoolean("HasVisualFire", falsefire.get());
                    if(powah.get())entityTag.putBoolean("powered", powah.get());
                    if(ignite.get())entityTag.putBoolean("ignited", ignite.get());
                    entityTag.putInt("Fuse", fuse.get());
                    entityTag.putInt("Size", size.get());
                    if(customname.get())entityTag.putBoolean("CustomNameVisible", customname.get());
                    entityTag.putString("CustomName", "{\"text\":\"" + nom.get() + "\",\"color\":\"" + nomcolor.get() + "\"}");
                    tag.put("EntityTag", entityTag);
                    bomb.setNbt(tag);
                    mc.interactionManager.clickCreativeStack(bomb, 36 + mc.player.getInventory().selectedSlot);
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                    mc.interactionManager.clickCreativeStack(bfr, 36 + mc.player.getInventory().selectedSlot);
                    i = 0;
                } else if (mixer.get()){
                    mix++;
                    if (mix<=1) {
                        String entityName = entity.get().trim().replace(" ", "_");
                        NbtCompound display = new NbtCompound();
                        display.putString("Name", "{\"text\":\"" + nom.get() + "\",\"color\":\"" + nomcolor.get() + "\"}");
                        tag.put("display", display);
                        NbtCompound entityTag = new NbtCompound();
                        speedlist.add(NbtDouble.of(0));
                        speedlist.add(NbtDouble.of(-speed.get()));
                        speedlist.add(NbtDouble.of(0));
                        pos.add(NbtDouble.of(cpos.x));
                        pos.add(NbtDouble.of(mc.player.getY()+height.get()));
                        pos.add(NbtDouble.of(cpos.z));
                        entityTag.put("power", speedlist);
                        entityTag.put("Motion", speedlist);
                        entityTag.put("Pos", pos);
                        entityTag.putString("id", "minecraft:" + entityName);
                        entityTag.putInt("Health", health.get());
                        entityTag.putInt("AbsorptionAmount", absorption.get());
                        entityTag.putInt("Age", age.get());
                        entityTag.putInt("ExplosionPower", exppower.get());
                        entityTag.putInt("ExplosionRadius", exppower.get());
                        if (invincible.get())entityTag.putBoolean("Invulnerable", invincible.get());
                        if (silence.get())entityTag.putBoolean("Silent", silence.get());
                        if (glow.get())entityTag.putBoolean("Glowing", glow.get());
                        if (persist.get())entityTag.putBoolean("PersistenceRequired", persist.get());
                        if (nograv.get())entityTag.putBoolean("NoGravity", nograv.get());
                        if(noAI.get())entityTag.putBoolean("NoAI", noAI.get());
                        if(falsefire.get())entityTag.putBoolean("HasVisualFire", falsefire.get());
                        if(powah.get())entityTag.putBoolean("powered", powah.get());
                        if(ignite.get())entityTag.putBoolean("ignited", ignite.get());
                        entityTag.putInt("Fuse", fuse.get());
                        entityTag.putInt("Size", size.get());
                        if(customname.get())entityTag.putBoolean("CustomNameVisible", customname.get());
                        entityTag.putString("CustomName", "{\"text\":\"" + nom.get() + "\",\"color\":\"" + nomcolor.get() + "\"}");
                        tag.put("EntityTag", entityTag);
                        bomb.setNbt(tag);
                        mc.interactionManager.clickCreativeStack(bomb, 36 + mc.player.getInventory().selectedSlot);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                        mc.interactionManager.clickCreativeStack(bfr, 36 + mc.player.getInventory().selectedSlot);
                        i = 0;
                    } else if (mix>=2){
                        String entityName = entity2.get().trim().replace(" ", "_");
                        NbtCompound display = new NbtCompound();
                        display.putString("Name", "{\"text\":\"" + nom.get() + "\",\"color\":\"" + nomcolor.get() + "\"}");
                        tag.put("display", display);
                        NbtCompound entityTag = new NbtCompound();
                        speedlist.add(NbtDouble.of(0));
                        speedlist.add(NbtDouble.of(-speed.get()));
                        speedlist.add(NbtDouble.of(0));
                        pos.add(NbtDouble.of(cpos.x));
                        pos.add(NbtDouble.of(mc.player.getY()+height.get()));
                        pos.add(NbtDouble.of(cpos.z));
                        entityTag.put("power", speedlist);
                        entityTag.put("Motion", speedlist);
                        entityTag.put("Pos", pos);
                        entityTag.putString("id", "minecraft:" + entityName);
                        entityTag.putInt("Health", health.get());
                        entityTag.putInt("AbsorptionAmount", absorption.get());
                        entityTag.putInt("Age", age.get());
                        entityTag.putInt("ExplosionPower", exppower.get());
                        entityTag.putInt("ExplosionRadius", exppower.get());
                        if (invincible.get())entityTag.putBoolean("Invulnerable", invincible.get());
                        if (silence.get())entityTag.putBoolean("Silent", silence.get());
                        if (glow.get())entityTag.putBoolean("Glowing", glow.get());
                        if (persist.get())entityTag.putBoolean("PersistenceRequired", persist.get());
                        if (nograv.get())entityTag.putBoolean("NoGravity", nograv.get());
                        if(noAI.get())entityTag.putBoolean("NoAI", noAI.get());
                        if(falsefire.get())entityTag.putBoolean("HasVisualFire", falsefire.get());
                        if(powah.get())entityTag.putBoolean("powered", powah.get());
                        if(ignite.get())entityTag.putBoolean("ignited", ignite.get());
                        entityTag.putInt("Fuse", fuse.get());
                        entityTag.putInt("Size", size.get());
                        if(customname.get())entityTag.putBoolean("CustomNameVisible", customname.get());
                        entityTag.putString("CustomName", "{\"text\":\"" + nom.get() + "\",\"color\":\"" + nomcolor.get() + "\"}");
                        tag.put("EntityTag", entityTag);
                        bomb.setNbt(tag);
                        mc.interactionManager.clickCreativeStack(bomb, 36 + mc.player.getInventory().selectedSlot);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                        mc.interactionManager.clickCreativeStack(bfr, 36 + mc.player.getInventory().selectedSlot);
                        i = 0;
                        mix=0;
                    }
                }
            }
        } else {
            error("You need to be in creative mode.");
            toggle();
        }
    }
}