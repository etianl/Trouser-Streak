package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
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
        if (mc.player == null || mc.level == null || igniting) return;

        if (event.packet instanceof ServerboundUseItemOnPacket packet) {
            BlockHitResult hit = packet.getHitResult();
            BlockPos clicked = hit.getBlockPos();
            BlockPos offset  = clicked.relative(hit.getDirection());

            boolean clickedIsTnt = mc.level.getBlockState(clicked).is(Blocks.TNT);
            boolean offsetIsTnt  = mc.level.getBlockState(offset).is(Blocks.TNT);

            if (clickedIsTnt) candidatePositions.add(clicked);
            if (offsetIsTnt)  candidatePositions.add(offset);
        }

        if (!candidatePositions.isEmpty() && ignitePos == null && !igniting) {
            Iterator<BlockPos> it = candidatePositions.iterator();
            while (it.hasNext()) {
                BlockPos pos = it.next();
                if (mc.level.getBlockState(pos).getBlock() == Blocks.TNT) {
                    if (ignitionQueue.stream().anyMatch(task -> task.pos().equals(pos))) {
                        it.remove();
                        continue;
                    }

                    it.remove();
                    if (useTickDelay.get()) {
                        ignitionQueue.add(new IgnitionTask(pos, tickDelay.get()));
                    } else {
                        ignitePos = pos;
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
            if (originalSlot == -1) {
                originalSlot = mc.player.getInventory().getSelectedSlot();
            }

            flintSlot = InvUtils.findInHotbar(Items.FLINT_AND_STEEL).slot();
            if (flintSlot == -1 || flintSlot == 40) {
                error("No flint and steel found in hotbar!");
                ignitePos = null;
                originalSlot = -1;
                flintSlot = -1;
                return;
            }

            mc.player.getInventory().setSelectedSlot(flintSlot);
            return;
        }

        if (ignitePos != null && !igniting && mc.player.getInventory().getSelectedSlot() == flintSlot) {
            igniting = true;
            Vec3 hitVec = Vec3.atCenterOf(ignitePos);
            BlockHitResult hit = new BlockHitResult(hitVec, Direction.UP, ignitePos, false);
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
            mc.player.swing(InteractionHand.MAIN_HAND);

            mc.player.getInventory().setSelectedSlot(originalSlot);
            ignitePos = null;
            flintSlot = -1;
            originalSlot = -1;
            igniting = false;
        }
    }
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.level == null) return;
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

        int flintSlot = InvUtils.findInHotbar(Items.FLINT_AND_STEEL).slot();
        if (flintSlot == -1 || flintSlot == 40) {
            error("No flint and steel found in hotbar!");
            readyList.clear();
            ignitionQueue.clear();
            return;
        }
        for (IgnitionTask task : readyList) igniteNow(task.pos(), flintSlot);
    }

    private void igniteNow(BlockPos pos, int flint) {
        if (mc.player.position().distanceTo(Vec3.atCenterOf(pos)) > reach.get()) return;
        igniting = true;
        int prevSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(flint);

        Vec3 hitVec = Vec3.atCenterOf(pos);
        BlockHitResult hit = new BlockHitResult(hitVec, Direction.UP, pos, false);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        mc.player.swing(InteractionHand.MAIN_HAND);

        mc.player.getInventory().setSelectedSlot(prevSlot);
        igniting = false;
    }
}