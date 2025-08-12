package pwn.noobs.trouserstreak.hud;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import pwn.noobs.trouserstreak.modules.NewerNewChunks;

import java.util.ArrayList;
import java.util.List;

public class ChunkFollowStatsHud extends HudElement {
    public static final HudElementInfo<ChunkFollowStatsHud> INFO = new HudElementInfo<>(
        Hud.GROUP,
        "chunk-follow-stats",
        "Shows NewerNewChunks auto-follow stats (heading, apex, retraced chunks, target, config).",
        ChunkFollowStatsHud::new
    );

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgStyle = settings.createGroup("Style");

    private final Setting<Boolean> background = sgStyle.add(new BoolSetting.Builder()
        .name("background")
        .description("Draw a translucent background.")
        .defaultValue(false)
        .build()
    );

    private final Setting<SettingColor> backgroundColor = sgStyle.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Background color.")
        .defaultValue(new SettingColor(25, 25, 25, 50))
        .visible(background::get)
        .build()
    );

    private final Setting<Double> scale = sgStyle.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Text scale.")
        .defaultValue(1.0)
        .min(0.7)
        .sliderRange(0.7, 2.0)
        .build()
    );

    private final Setting<Boolean> hideWhenDisabled = sgGeneral.add(new BoolSetting.Builder()
        .name("hide-when-disabled")
        .description("Hide all lines when auto-follow is disabled.")
        .defaultValue(false)
        .build()
    );

    public ChunkFollowStatsHud() {
        super(INFO);
        setSize(140, 72);
    }

    @Override
    public void render(HudRenderer renderer) {
        NewerNewChunks mod = Modules.get().get(NewerNewChunks.class);
        if (mod == null) {
            drawLines(renderer, List.of(new HUDLine("NewerNewChunks not loaded", Color.WHITE)));
            return;
        }

        // Hide when auto-follow disabled (optional)
        if (hideWhenDisabled.get() && !mod.hudAutoFollowEnabled()) {
            // Keep a small placeholder in editor for positioning
            if (isInEditor()) {
                drawLines(renderer, List.of(new HUDLine("chunk-follow-stats (hidden)", Color.GRAY)));
            } else {
                setSize(60, 12);
            }
            return;
        }

        ChunkPos target = mod.hudCurrentTarget();
        Direction heading = mod.hudHeading();
        ChunkPos apex = mod.hudBacktrackApex();

        String followType = String.valueOf(mod.hudFollowType());
        String targetStr = target != null ? (target.x + "," + target.z) : "-";
        String headStr = heading != null ? heading.asString() : "-";
        String apexStr = apex != null ? (apex.x + "," + apex.z) : "-";

        int retraced = mod.hudRetracedChunks();
        int gap = mod.hudGapAllowance();
        int limit = mod.hudBacktrackLimit();
        int pool = mod.hudPoolSize();

        List<HUDLine> lines = new ArrayList<>();
        lines.add(new HUDLine("Follow: " + followType + (mod.hudAutoFollowEnabled() ? "" : " [OFF]") + " (pool=" + pool + ")", Color.WHITE));
        lines.add(new HUDLine("Target: " + targetStr, Color.WHITE));
        lines.add(new HUDLine("Heading: " + headStr, Color.WHITE));
        lines.add(new HUDLine("Apex: " + apexStr, Color.WHITE));
        // Retraced coloring: green when 0, yellow mid, red at/over limit
        Color retracedColor = retraced <= 0 ? Color.GREEN : (retraced >= limit ? Color.RED : Color.YELLOW);
        lines.add(new HUDLine("Retraced: " + retraced + "/" + limit + " chunks", retracedColor));
        lines.add(new HUDLine("Gap: " + gap + " chunks", Color.WHITE));
        // Oscillation indicator
        boolean oscillating = mod.hudOscillating();
        lines.add(new HUDLine("Oscillation: " + (oscillating ? "YES" : "no"), oscillating ? Color.RED : Color.WHITE));

        drawLines(renderer, lines);
    }

    private void drawLines(HudRenderer renderer, List<HUDLine> lines) {
        double sx = x;
        double sy = y;
        double w = 0;
        double h = 0;

        // Measure
        for (HUDLine line : lines) {
            double tw = renderer.textWidth(line.text, true, scale.get());
            w = Math.max(w, tw);
            h += renderer.textHeight(true, scale.get());
        }
        setSize(w + 4, h + 4);

        if (background.get()) renderer.quad(sx, sy, getWidth(), getHeight(), backgroundColor.get());

        double cy = sy + 2;
        for (HUDLine line : lines) {
            renderer.text(line.text, sx + 2, cy, line.color, true, scale.get());
            cy += renderer.textHeight(true, scale.get());
        }
    }

    private static final class HUDLine {
        final String text; final Color color;
        HUDLine(String t, Color c) { text = t; color = c; }
    }
}
