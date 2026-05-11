package pwn.noobs.trouserstreak.hud;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

public class HorseInfo extends HudElement {
    public static final HudElementInfo<HorseInfo> INFO = new HudElementInfo<>(
            Hud.GROUP, "HorseInfo", "Displays horse stats while riding.", HorseInfo::new
    );

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<SettingColor> textColor = sgGeneral.add(new ColorSetting.Builder()
            .name("text-color")
            .description("Color of the stat text.")
            .defaultValue(new SettingColor(255, 255, 255))
            .build()
    );

    private final Setting<SettingColor> placeholderColor = sgGeneral.add(new ColorSetting.Builder()
            .name("placeholder-color")
            .description("Color of the 'not on a horse' text.")
            .defaultValue(new SettingColor(150, 150, 150))
            .build()
    );

    private final Setting<Boolean> showHealth = sgGeneral.add(new BoolSetting.Builder()
            .name("show-health")
            .description("Show horse health.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> showSpeed = sgGeneral.add(new BoolSetting.Builder()
            .name("show-speed")
            .description("Show horse speed in blocks per second.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> showJump = sgGeneral.add(new BoolSetting.Builder()
            .name("show-jump")
            .description("Show horse jump height in blocks.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> showPlaceholder = sgGeneral.add(new BoolSetting.Builder()
            .name("show-placeholder")
            .description("Show 'Not on a horse' when not riding.")
            .defaultValue(true)
            .build()
    );

    public HorseInfo() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {

        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || !(mc.player.getVehicle() instanceof AbstractHorse horse)) {
            if (showPlaceholder.get()) {
                renderer.text("Not on a horse", x, y, placeholderColor.get(), true);
                setSize(renderer.textWidth("Not on a horse", true), renderer.textHeight(true));
            }
            return;
        }

        double speed = horse.getAttributeValue(Attributes.MOVEMENT_SPEED) * 43.17;
        double jump = horse.getAttributeValue(Attributes.JUMP_STRENGTH);
        double jumpBlocks = -0.1817584952 * Math.pow(jump, 3) + 3.689713992 * Math.pow(jump, 2) + 2.128599134 * jump - 0.343930367;
        double health = horse.getHealth();
        double maxHealth = horse.getAttributeValue(Attributes.MAX_HEALTH);

        java.util.List<String> lines = new java.util.ArrayList<>();
        if (showHealth.get()) lines.add(String.format("Health: %.1f / %.1f", health, maxHealth));
        if (showSpeed.get()) lines.add(String.format("Speed:  %.2f bps", speed));
        if (showJump.get()) lines.add(String.format("Jump:   %.2f blocks", jumpBlocks));

        if (lines.isEmpty()) return;

        double lineH = renderer.textHeight(true);
        double maxW = 0;
        for (String line : lines) maxW = Math.max(maxW, renderer.textWidth(line, true));
        setSize(maxW, lineH * lines.size());

        for (int i = 0; i < lines.size(); i++) {
            renderer.text(lines.get(i), x, y + i * lineH, textColor.get(), true);
        }
    }
}