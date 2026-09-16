package pwn.noobs.trouserstreak.hud;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import pwn.noobs.trouserstreak.Trouser;
import pwn.noobs.trouserstreak.modules.AutoNames;

public class Censor extends HudElement {
    public static final HudElementInfo<Censor> INFO = new HudElementInfo<>(Trouser.HUD_GROUP, "Censor", "Place a box on your screen to cover parts of it.", Censor::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Integer> width = sgGeneral.add(new IntSetting.Builder()
            .name("width")
            .description("width of the box")
            .defaultValue(100)
            .range(1, 5000)
            .sliderRange(1, 5000)
            .build()
    );
    public final Setting<Integer> height = sgGeneral.add(new IntSetting.Builder()
            .name("height")
            .description("height of the box")
            .defaultValue(100)
            .range(1, 5000)
            .sliderRange(1, 5000)
            .build()
    );

    private final Setting<SettingColor> backgroundColor = sgGeneral.add(new ColorSetting.Builder()
            .name("background-color")
            .description("Color used for the background.")
            .defaultValue(new SettingColor(0, 162, 232, 255))
            .build()
    );

    private final Setting<SettingColor> textColor = sgGeneral.add(new ColorSetting.Builder()
            .name("text-color")
            .description("Color used for the text.")
            .defaultValue(new SettingColor(255,255,255, 255))
            .build()
    );

    private final Setting<String> text_thing = sgGeneral.add(new StringSetting.Builder()
            .name("text")
            .description("Text to render inside the box")
            .defaultValue("trouser")
            .build()
    );

    public Censor() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        setSize(width.get(), height.get());
        renderer.quad(x, y, width.get(), height.get(), backgroundColor.get());

        // TODO: maybe add trouser streaks logo later
        renderer.text(text_thing.get(),
                x,
                y,
                textColor.get(),
                true
        );


    }
}