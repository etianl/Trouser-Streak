// Detects player activity using snitches/villagers level. (needs to be above level 1 as villager experience cannot yet be detected)
// - Dedicate

package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.village.VillagerData;
import pwn.noobs.trouserstreak.Trouser;

import java.util.HashSet;
import java.util.Set;

public final class SnitchDetector extends Module {
    private static final double MARKER_BOX_OFFSET = 1.5;
    private static final double MARKER_BOX_HEIGHT = 3.0;

    private final Set<Integer> detectedVillagers = new HashSet<>();
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> alert = sgGeneral.add(new BoolSetting.Builder()
            .name("Alert")
            .description("Alerts you in chat when a snitch(Villager) is found.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> marker = sgGeneral.add(new BoolSetting.Builder()
            .name("Marker")
            .description("Adds a marker above the villager to make it easier to see.")
            .defaultValue(true)
            .build());

    private final Setting<Double> markerHeight = sgGeneral.add(new DoubleSetting.Builder()
            .name("Marker Height")
            .description("The Y level where markers will be placed.")
            .defaultValue(120)
            .range(-64, 320)
            .sliderRange(-64, 320)
            .visible(marker::get)
            .build());

    private final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The side color of the rendering.")
            .defaultValue(new SettingColor(255, 0, 0, 75))
            .build());

    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The line color of the rendering.")
            .defaultValue(new SettingColor(255, 0, 0, 255))
            .build());

    public SnitchDetector() {
        super(Trouser.Main, "SnitchDetector", "Detects if snitches are around. (Detects player activity with villagers)");
    }

    @Override
    public void onActivate() {
        detectedVillagers.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!isValid()) return;

        detectedVillagers.removeIf(id -> mc.world.getEntityById(id) == null);
        processVillagers();
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!isValid()) return;

        mc.world.getEntities().forEach(entity -> {
            if (entity instanceof VillagerEntity villager) {
                VillagerData data = villager.getVillagerData();
                if (data.getLevel() > 1) {
                    renderVillagerHighlight(event, villager);
                }
            }
        });
    }

    private void processVillagers() {
        mc.world.getEntities().forEach(entity -> {
            if (entity instanceof VillagerEntity villager) {
                VillagerData data = villager.getVillagerData();
                if (data.getLevel() > 1 && !detectedVillagers.contains(villager.getId())) {
                    handleNewVillagerDetection(villager, data);
                }
            }
        });
    }

    private void handleNewVillagerDetection(VillagerEntity villager, VillagerData data) {
        if (alert.get()) {
            info(String.format("Found Snitch (Level %d) at [x%d, y%d, z%d]",
                    data.getLevel(),
                    villager.getBlockPos().getX(),
                    villager.getBlockPos().getY(),
                    villager.getBlockPos().getZ()));
        }
        detectedVillagers.add(villager.getId());
    }

    private void renderVillagerHighlight(Render3DEvent event, VillagerEntity villager) {
        event.renderer.box(villager.getBoundingBox(), sideColor.get(), lineColor.get(), ShapeMode.Both, 0);

        if (marker.get()) {
            renderMarker(event, villager);
        }
    }

    private void renderMarker(Render3DEvent event, VillagerEntity villager) {
        event.renderer.line(
                villager.getX(), villager.getY(), villager.getZ(),
                villager.getX(), markerHeight.get(), villager.getZ(),
                lineColor.get());

        Box markerBox = new Box(
                villager.getX() - MARKER_BOX_OFFSET,
                markerHeight.get(),
                villager.getZ() - MARKER_BOX_OFFSET,
                villager.getX() + MARKER_BOX_OFFSET,
                markerHeight.get() + MARKER_BOX_HEIGHT,
                villager.getZ() + MARKER_BOX_OFFSET
        );
        event.renderer.box(markerBox, sideColor.get(), lineColor.get(), ShapeMode.Both, 0);
    }

    private boolean isValid() {
        return mc.world != null && mc.player != null;
    }
}
