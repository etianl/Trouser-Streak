package pwn.noobs.trouserstreak.hud;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import pwn.noobs.trouserstreak.modules.WaypointCoordExploit;

import java.util.*;

public class WaypointTriangulationHud extends HudElement {
    public static final HudElementInfo<WaypointTriangulationHud> INFO =
            new HudElementInfo<>(Hud.GROUP, "waypoint-triangulations", "Shows triangulated player positions from WayPointScanner.", WaypointTriangulationHud::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> showBackground = sgGeneral.add(new BoolSetting.Builder()
            .name("background")
            .description("Show a background behind the HUD.")
            .defaultValue(true)
            .build()
    );
    private final Setting<SettingColor> backgroundColor = sgGeneral.add(new ColorSetting.Builder()
            .name("background-color")
            .description("Background color.")
            .defaultValue(new SettingColor(25, 25, 25, 100))
            .visible(showBackground::get)
            .build()
    );
    private final Setting<Integer> maxHudEntries = sgGeneral.add(new IntSetting.Builder()
            .name("max-hud-entries")
            .description("Maximum number of triangulated players shown in the HUD.")
            .defaultValue(20)
            .min(1)
            .sliderRange(1, 1000)
            .build()
    );

    public WaypointTriangulationHud() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        Map<UUID, WaypointCoordExploit.TriangulationResult> results = WaypointCoordExploit.getLastResults();

        int maxEntries = maxHudEntries.get();

        List<WaypointCoordExploit.TriangulationResult> sortedResults = new ArrayList<>(results.values());
        sortedResults.sort((a, b) -> Long.compare(b.lastUpdated, a.lastUpdated));

        List<WaypointCoordExploit.TriangulationResult> toDisplay = sortedResults.size() > maxEntries
                ? sortedResults.subList(0, maxEntries)
                : sortedResults;

        double yOffset = 0;
        double maxWidth = 0;

        if (toDisplay.isEmpty()) {
            String placeholder = "No triangulations yet";
            renderer.text(placeholder, x, y, Color.GRAY, true);
            maxWidth = renderer.textWidth(placeholder, true);
            yOffset = renderer.textHeight(true);
        } else {
            long now = System.currentTimeMillis();
            for (WaypointCoordExploit.TriangulationResult result : toDisplay) {
                long ageSeconds = (now - result.lastUpdated) / 1000;
                String text = String.format("%s: (%.1f, %.1f) [%ds ago]", result.playerName, result.wx, result.wz, ageSeconds);
                renderer.text(text, x, y + yOffset, Color.WHITE, true);
                maxWidth = Math.max(maxWidth, renderer.textWidth(text, true));
                yOffset += renderer.textHeight(true) + 2;
            }
        }

        setSize(maxWidth, yOffset);

        if (showBackground.get()) {
            renderer.quad(x, y, getWidth(), getHeight(), backgroundColor.get());
        }
    }
}