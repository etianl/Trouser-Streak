package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldEvents;
import pwn.noobs.trouserstreak.Trouser;

import java.util.ArrayDeque;
import java.util.Deque;

public class WorldEventCoords extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Boolean> wither = sgGeneral.add(new BoolSetting.Builder()
            .name("Detect WITHER_SPAWNS")
            .defaultValue(true)
            .build()
    );

    public final Setting<Boolean> dragon = sgGeneral.add(new BoolSetting.Builder()
            .name("Detect ENDER_DRAGON_DIES")
            .defaultValue(false)
            .build()
    );

    public final Setting<Boolean> portal = sgGeneral.add(new BoolSetting.Builder()
            .name("Detect END_PORTAL_OPENED")
            .defaultValue(false)
            .build()
    );

    public final Setting<Boolean> unknown = sgGeneral.add(new BoolSetting.Builder()
            .name("Detect unknown global events")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> triangulationEvents = sgGeneral.add(new IntSetting.Builder()
            .name("Triangulation events")
            .description("Number of world events required to trigger triangulation.")
            .defaultValue(2)
            .min(2)
            .sliderMax(100)
            .build()
    );

    private final Setting<Integer> triangulationWindowTicks = sgGeneral.add(new IntSetting.Builder()
            .name("Triangulation window (ticks)")
            .description("Events must occur within this many ticks to be used for triangulation. Run the triangulation after this window has expired.")
            .defaultValue(2400)
            .min(1)
            .sliderMax(10000)
            .build()
    );

    private final Setting<Double> minBearingShift = sgGeneral.add(new DoubleSetting.Builder()
            .name("Min bearing shift (°)")
            .description("Minimum bearing spread between events to allow triangulation.")
            .defaultValue(0.5)
            .min(0.01)
            .sliderMax(180)
            .build()
    );

    private final Setting<Double> minMovementDistance = sgGeneral.add(new DoubleSetting.Builder()
            .name("Min movement distance")
            .description("Minimum distance the player must move between any two events to allow triangulation.")
            .defaultValue(20.0)
            .min(0.0)
            .sliderMax(200.0)
            .build()
    );

    private final Setting<Double> maxBearingFromFirst = sgGeneral.add(new DoubleSetting.Builder()
            .name("Max bearing from first (°)")
            .description("Ignore events whose bearing differs from the first event by more than this angle (helps ignore unrelated events).")
            .defaultValue(22.5)
            .min(1.0)
            .sliderMax(180.0)
            .build()
    );

    public final Setting<Boolean> extraInfo = sgGeneral.add(new BoolSetting.Builder()
            .name("Extra Info")
            .defaultValue(false)
            .build()
    );

    public final Setting<Boolean> chatFeedback = sgGeneral.add(new BoolSetting.Builder()
            .name("Chat Feedback")
            .defaultValue(true)
            .build()
    );

    public WorldEventCoords() {
        super(Trouser.baseHunting, "WorldEventCoords", "Displays the positions of global world events.");
    }

    private static class EventSample {
        final int tick;
        final double playerX;
        final double playerZ;
        final double bearingDeg;

        EventSample(int tick, double playerX, double playerZ, double bearingDeg) {
            this.tick = tick;
            this.playerX = playerX;
            this.playerZ = playerZ;
            this.bearingDeg = bearingDeg;
        }
    }

    private final Deque<EventSample> recentEvents = new ArrayDeque<>();
    private int lastTick = -1;
    private boolean didTriangulationThisWindow = false;
    private boolean hasSeenFirstEvent = false;
    private Double firstBearingDeg = null;

    private int currentTick = 0;

    @Override
    public void onActivate() {
        clearState();
        currentTick = 0;
    }

    @Override
    public void onDeactivate() {
        if (recentEvents.size() >= triangulationEvents.get()) {
            triangulateAndLog();
        }
        clearState();
    }

    private void clearState() {
        recentEvents.clear();
        didTriangulationThisWindow = false;
        hasSeenFirstEvent = false;
        firstBearingDeg = null;
        lastTick = -1;
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (mc.player == null) return;
        if (!(event.packet instanceof WorldEventS2CPacket packet)) return;
        if (!packet.isGlobal()) return;

        BlockPos pos = packet.getPos();
        int eventId = packet.getEventId();
        String eventString = "unknown global event";

        if (eventId == WorldEvents.ENDER_DRAGON_DIES) {
            eventString = "ENDER_DRAGON_DIES";
            if (!dragon.get()) return;
        }
        if (eventId == WorldEvents.END_PORTAL_OPENED) {
            eventString = "END_PORTAL_OPENED";
            if (!portal.get()) return;
        }
        if (eventId == WorldEvents.WITHER_SPAWNS) {
            eventString = "WITHER_SPAWNS";
            if (!wither.get()) return;
        }
        if (eventString.equals("unknown global event")) {
            if (!unknown.get()) return;
        }

        int eventX = pos.getX();
        int eventZ = pos.getZ();
        double playerX = mc.player.getX();
        double playerZ = mc.player.getZ();

        double dx = eventX - playerX;
        double dz = eventZ - playerZ;

        double yawRad = Math.atan2(dx, dz);
        double yawDeg = Math.toDegrees(yawRad);
        if (yawDeg < 0) yawDeg += 360.0;

        if (hasSeenFirstEvent && firstBearingDeg != null) {
            double diff = Math.abs(yawDeg - firstBearingDeg);
            if (diff > 180.0) diff = 360.0 - diff;
            if (diff > maxBearingFromFirst.get()) {
                if (chatFeedback.get() && extraInfo.get()) {
                    info(String.format(
                            "§7[WorldEvent] §fIgnored event §e%s §f(bearing diff §e%.2f° §f> §e%.2f°§f)",
                            eventString, diff, maxBearingFromFirst.get()
                    ));
                }
                return;
            }
        }

        String yawStr = Double.toString(yawDeg);

        if (chatFeedback.get() && extraInfo.get()) info(String.format(
                "§a[WorldEvent] §fEventID: §e%s §f| EventPos: §bX:%d Y:%d Z:%d §f| PlayerPos: §dX:%.2f Y:%.2f Z:%.2f §f| Bearing: §e%s° §f",
                eventString,
                eventX, pos.getY(), eventZ,
                playerX, mc.player.getY(), playerZ,
                yawStr
        ));

        if (!hasSeenFirstEvent) {
            firstBearingDeg = yawDeg;
            String dir = getOptimalMovementDirection(yawDeg);
            if (chatFeedback.get()) {
                info("§6§l[TRIANGULATION] §r§e" + eventString + "§e detected! For best accuracy, move §l§b" + dir + "§r§e and wait for more events.");
                info("The triangulation will calculate after your Triangulation window has elapsed. " + triangulationWindowTicks.get() + " ticks. You may deactivate the module to trigger triangulation.");
            }
            hasSeenFirstEvent = true;
        }

        recentEvents.addLast(new EventSample(currentTick, playerX, playerZ, yawDeg));
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        currentTick++;

        if (lastTick == -1) {
            lastTick = currentTick;
            return;
        }

        int window = triangulationWindowTicks.get();

        while (!recentEvents.isEmpty() && (currentTick - recentEvents.getFirst().tick) > window) {
            recentEvents.removeFirst();
        }

        if (recentEvents.isEmpty()) {
            didTriangulationThisWindow = false;
            hasSeenFirstEvent = false;
            firstBearingDeg = null;
            lastTick = currentTick;
            return;
        }

        if (!didTriangulationThisWindow
                && recentEvents.size() >= triangulationEvents.get()
                && (currentTick - recentEvents.getFirst().tick) >= window) {
            triangulateAndLog();
            didTriangulationThisWindow = true;
            clearState();
        }

        lastTick = currentTick;
    }

    private double computeBearingSpread(EventSample[] samples) {
        if (samples.length < 2) return 0.0;

        double[] bearings = new double[samples.length];
        for (int i = 0; i < samples.length; i++) {
            double b = samples[i].bearingDeg % 360.0;
            if (b < 0) b += 360.0;
            bearings[i] = b;
        }

        java.util.Arrays.sort(bearings);

        double maxGap = 0.0;
        int n = bearings.length;
        for (int i = 0; i < n; i++) {
            double b1 = bearings[i];
            double b2 = bearings[(i + 1) % n];
            double gap = (i == n - 1)
                    ? (360.0 - b1 + b2)
                    : (b2 - b1);
            if (gap > maxGap) maxGap = gap;
        }

        return 360.0 - maxGap;
    }

    private void triangulateAndLog() {
        if (recentEvents.size() < 2) return;

        EventSample[] samples = recentEvents.toArray(new EventSample[0]);

        double actualSpread = computeBearingSpread(samples);

        if (actualSpread < minBearingShift.get()) {
            if (chatFeedback.get()) {
                info(String.format(
                        "§c[Triangulation] Bearing spread too small: §e%.2f° §c< §e%.2f° §c minimum. Move more perpendicular to the bearing next time.",
                        actualSpread, minBearingShift.get()
                ));
            }
            return;
        }

        double maxDistSq = 0;
        int n = samples.length;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                EventSample a = samples[i];
                EventSample b = samples[j];
                double dx = a.playerX - b.playerX;
                double dz = a.playerZ - b.playerZ;
                double distSq = dx * dx + dz * dz;
                if (distSq > maxDistSq) maxDistSq = distSq;
            }
        }
        double minDist = minMovementDistance.get();
        if (maxDistSq < minDist * minDist) {
            if (chatFeedback.get()) {
                info(String.format(
                        "§c[Triangulation] Player movement too small: max distance §e%.2f §c< §e%.2f§c. Move more between events.",
                        Math.sqrt(maxDistSq), minDist
                ));
            }
            return;
        }

        double A11 = 0, A12 = 0, A22 = 0;
        double b1 = 0, b2 = 0;

        for (EventSample s : samples) {
            double thetaRad = Math.toRadians(s.bearingDeg);

            double nx = Math.cos(thetaRad);
            double nz = -Math.sin(thetaRad);

            A11 += nx * nx;
            A12 += nx * nz;
            A22 += nz * nz;

            double dot = nx * s.playerX + nz * s.playerZ;
            b1 += dot * nx;
            b2 += dot * nz;
        }

        double det = A11 * A22 - A12 * A12;

        if (Math.abs(det) < 1e-12) {
            if (chatFeedback.get()) {
                info("§c[Triangulation] Bearings are nearly parallel; cannot triangulate reliably. Move more perpendicular to the bearing.");
            }
            return;
        }

        double invDet = 1.0 / det;
        double x = invDet * (A22 * b1 - A12 * b2);
        double z = invDet * (A11 * b2 - A12 * b1);

        if (chatFeedback.get()) {
            info(String.format(
                    "§a[Triangulation] §fEstimated position (WLS): §bX:%.3f Z:%.3f §ffrom §e%d §fevents.",
                    x, z, samples.length
            ));
        }
    }

    private static String getOptimalMovementDirection(double bearingDeg) {
        bearingDeg = bearingDeg % 360.0;
        if (bearingDeg < 0) bearingDeg += 360.0;

        double dir = (bearingDeg + 90.0) % 360.0;

        if (dir >= 337.5 || dir < 22.5) {
            return "North";
        } else if (dir >= 22.5 && dir < 67.5) {
            return "North-East";
        } else if (dir >= 67.5 && dir < 112.5) {
            return "East";
        } else if (dir >= 112.5 && dir < 157.5) {
            return "South-East";
        } else if (dir >= 157.5 && dir < 202.5) {
            return "South";
        } else if (dir >= 202.5 && dir < 247.5) {
            return "South-West";
        } else if (dir >= 247.5 && dir < 292.5) {
            return "West";
        } else if (dir >= 292.5 && dir < 337.5) {
            return "North-West";
        }

        return "North";
    }
}