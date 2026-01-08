package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import pwn.noobs.trouserstreak.Trouser;

import java.util.*;

public class AutoTnt extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> useTickDelay = sgGeneral.add(new BoolSetting.Builder()
            .name("use-tick-delay")
            .description("Waits a configurable number of ticks before igniting each TNT.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> tickDelay = sgGeneral.add(new IntSetting.Builder()
            .name("tick-delay")
            .description("Delay in ticks before igniting TNT.")
            .defaultValue(5)
            .min(1)
            .sliderRange(1, 40)
            .visible(useTickDelay::get)
            .build()
    );
    private final Setting<Double> reach = sgGeneral.add(new DoubleSetting.Builder()
            .name("Reach (blocks)")
            .description("Tnt must still be within this range to attempt lighting")
            .defaultValue(5.5)
            .min(1)
            .sliderRange(1, 10)
            .visible(useTickDelay::get)
            .build()
    );

    public AutoTnt() {
        super(Trouser.Main, "auto-tnt", "Automatically ignites placed TNT with optional tick delay.");
    }

    private final Set<BlockPos> candidatePositions = new HashSet<>();
    private final Queue<IgnitionTask> ignitionQueue = new LinkedList<>();
    private BlockPos ignitePos;

    private boolean igniting = false;
    private int originalSlot = -1;
    private int flintSlot = -1;
    private record IgnitionTask(BlockPos pos, int ticksRemaining) {}

    @Override
    public void onActivate() {
        ignitePos = null;
        candidatePositions.clear();
        ignitionQueue.clear();
        igniting = false;
        originalSlot = -1;
        flintSlot = -1;
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (igniting) return;

        if (event.packet instanceof PlayerInteractBlockC2SPacket packet) {
            BlockHitResult hit = packet.getBlockHitResult();
            candidatePositions.add(hit.getBlockPos().offset(hit.getSide()));
            candidatePositions.add(hit.getBlockPos());
        }

        if (!candidatePositions.isEmpty() && ignitePos == null && !igniting) {
            Iterator<BlockPos> it = candidatePositions.iterator();
            while (it.hasNext()) {
                BlockPos pos = it.next();
                if (mc.world.getBlockState(pos).getBlock() == Blocks.TNT) {
                    it.remove();
                    if (useTickDelay.get()) {
                        ignitionQueue.add(new IgnitionTask(pos, tickDelay.get()));
                    } else {
                        ignitePos = pos;
                        originalSlot = mc.player.getInventory().selectedSlot;
                        candidatePositions.clear();
                        break;
                    }
                } else {
                    it.remove();
                }
            }
        }

        if (useTickDelay.get()) return;

        if (ignitePos != null && !igniting && flintSlot == -1) {
            flintSlot = InvUtils.findInHotbar(Items.FLINT_AND_STEEL).slot();
            if (flintSlot == -1) {
                error("No flint and steel found in hotbar!");
                ignitePos = null;
                return;
            }
            mc.player.getInventory().selectedSlot = flintSlot;
            originalSlot = originalSlot == -1 ? mc.player.getInventory().selectedSlot : originalSlot;
            return;
        }

        if (ignitePos != null && !igniting && mc.player.getInventory().selectedSlot == flintSlot) {
            igniting = true;
            Vec3d hitVec = Vec3d.ofCenter(ignitePos);
            BlockHitResult hit = new BlockHitResult(hitVec, Direction.UP, ignitePos, false);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
            mc.player.swingHand(Hand.MAIN_HAND);

            mc.player.getInventory().selectedSlot = originalSlot;
            ignitePos = null;
            flintSlot = -1;
            originalSlot = -1;
            igniting = false;
        }
    }
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!useTickDelay.get()) return;
        if (igniting) return;

        if (ignitionQueue.isEmpty()) return;

        List<IgnitionTask> readyList = new ArrayList<>();
        Queue<IgnitionTask> updated = new LinkedList<>();

        while (!ignitionQueue.isEmpty()) {
            IgnitionTask task = ignitionQueue.poll();
            int newTicks = task.ticksRemaining() - 1;
            if (newTicks <= 0) {
                readyList.add(task);
            } else {
                updated.add(new IgnitionTask(task.pos(), newTicks));
            }
        }

        ignitionQueue.addAll(updated);

        for (IgnitionTask task : readyList) {
            int flintSlot = InvUtils.findInHotbar(Items.FLINT_AND_STEEL).slot();
            if (flintSlot == -1) {
                error("No flint and steel found in hotbar!");
                readyList.clear();
                ignitionQueue.clear();
                break;
            } else igniteNow(task.pos(), flintSlot);
        }
    }

    private void igniteNow(BlockPos pos, int flint) {
        if (!mc.player.getBlockPos().isWithinDistance(pos, reach.get())) return;
        igniting = true;
        int prevSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = flint;

        Vec3d hitVec = Vec3d.ofCenter(pos);
        BlockHitResult hit = new BlockHitResult(hitVec, Direction.UP, pos, false);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        mc.player.swingHand(Hand.MAIN_HAND);

        mc.player.getInventory().selectedSlot = prevSlot;
        igniting = false;
    }
}