// made by [Dylanvip2024](https://github.com/Dylanvip2024) with the help of AI.
// Heavily modified by etianl to fix the bugs and add new features! :)
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import net.minecraft.util.math.Vec3d;
import pwn.noobs.trouserstreak.Trouser;
import pwn.noobs.trouserstreak.utils.PermissionUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArmorStandImages extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPosition = settings.createGroup("Position Settings");
    private final SettingGroup sgTiming = settings.createGroup("Timing Settings");
    private final SettingGroup sgRender = settings.createGroup("Render Settings");

    private final Setting<String> imagePath = sgGeneral.add(new StringSetting.Builder()
            .name("image-path")
            .description("Full path to the PNG image file.")
            .defaultValue("")
            .build()
    );
    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
            .name("scale")
            .description("Image scaling (1.0 = original size, 0.5 = half size).")
            .defaultValue(1.0)
            .min(0.1)
            .max(5.0)
            .sliderRange(0.1, 2.0)
            .build()
    );
    private final Setting<Keybind> placeKey = sgGeneral.add(new KeybindSetting.Builder()
            .name("place-keybind")
            .description("Keybind used to confirm and start/stop placement.")
            .defaultValue(Keybind.fromKey(-1))
            .build()
    );

    public enum StartPosition {
        CENTER,
        TOP_RIGHT,
        TOP_LEFT,
        BOTTOM_RIGHT,
        BOTTOM_LEFT
    }
    private final Setting<StartPosition> startPosition = sgPosition.add(new EnumSetting.Builder<StartPosition>()
            .name("start-position")
            .description("Starting position of the image relative to the player.")
            .defaultValue(StartPosition.CENTER)
            .build()
    );
    private final Setting<Double> rotation = sgPosition.add(new DoubleSetting.Builder()
            .name("rotation")
            .description("Overall rotation angle of the drawing (degrees).")
            .defaultValue(0.0)
            .min(0.0)
            .max(360.0)
            .sliderMax(360.0)
            .build()
    );
    private final Setting<Double> yOffset = sgPosition.add(new DoubleSetting.Builder()
            .name("y-offset")
            .description("Vertical offset")
            .defaultValue(0.0)
            .min(-5.0)
            .max(5.0)
            .sliderRange(-5.0, 5.0)
            .build()
    );
    private final Setting<Double> pixelSpacing = sgPosition.add(new DoubleSetting.Builder()
            .name("pixel-spacing")
            .description("Spacing of each pixel.")
            .defaultValue(0.62)
            .min(0.1)
            .max(1.0)
            .sliderRange(0.1, 1.0)
            .build()
    );

    private final Setting<Integer> delayTicks = sgTiming.add(new IntSetting.Builder()
            .name("command-delay")
            .description("Delay between each summon command (ticks, 20 ticks = 1 second).")
            .defaultValue(0)
            .min(0)
            .max(10)
            .sliderRange(0, 10)
            .build()
    );
    private final Setting<Integer> maxPerTick = sgTiming.add(new IntSetting.Builder()
            .name("max-per-tick")
            .description("Max armor stands summoned per tick (0 = 1 per tick)")
            .defaultValue(1)
            .min(1)
            .max(10)
            .sliderRange(1, 10)
            .build()
    );

    private final Setting<Boolean> showPreview = sgRender.add(new BoolSetting.Builder()
            .name("show-preview")
            .description("Shows a 3D preview of the image.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> renderPixelBoxes = sgRender.add(new BoolSetting.Builder()
            .name("render-pixel-boxes")
            .description("Renders a box for each individual pixel.")
            .defaultValue(true)
            .visible(showPreview::get)
            .build()
    );
    private final Setting<Boolean> useRealColors = sgRender.add(new BoolSetting.Builder()
            .name("use-real-colors")
            .description("Uses the actual image colors for the preview.")
            .defaultValue(true)
            .visible(showPreview::get)
            .build()
    );
    private final Setting<Integer> useRealColorsTransparency = sgRender.add(new IntSetting.Builder()
            .name("transparency")
            .description("Transparency of the render.")
            .defaultValue(50)
            .min(0)
            .max(255)
            .sliderRange(0, 255)
            .visible(useRealColors::get)
            .build()
    );
    private final Setting<SettingColor> previewColor = sgRender.add(new ColorSetting.Builder()
            .name("preview-color")
            .description("Color of the pixel preview boxes.")
            .defaultValue(new SettingColor(255, 255, 255, 50))
            .visible(() -> showPreview.get() && renderPixelBoxes.get() && !useRealColors.get())
            .build()
    );
    private final Setting<Double> pixelSize = sgRender.add(new DoubleSetting.Builder()
            .name("rendered-pixel-size")
            .description("Render size of each pixel (blocks).")
            .defaultValue(0.62)
            .min(0.1)
            .max(1.0)
            .sliderRange(0.1, 1.0)
            .build()
    );

    private BufferedImage scaledImage = null;
    private final List<PixelPosition> pixelPositions = new ArrayList<>();
    private boolean isPlacing = false;
    private boolean wasKeyPressed = false;
    private int currentPixelIndex = 0;
    private int tickCounter = 0;

    private static final Map<Integer, String> COLOR_PALETTE = new HashMap<>();
    static {
        COLOR_PALETTE.put(0xCFD5D6, "minecraft:white_concrete");
        COLOR_PALETTE.put(0xE06101, "minecraft:orange_concrete");
        COLOR_PALETTE.put(0xA9309F, "minecraft:magenta_concrete");
        COLOR_PALETTE.put(0x2489C7, "minecraft:light_blue_concrete");
        COLOR_PALETTE.put(0xF1AF15, "minecraft:yellow_concrete");
        COLOR_PALETTE.put(0x5EA919, "minecraft:lime_concrete");
        COLOR_PALETTE.put(0xD5658F, "minecraft:pink_concrete");
        COLOR_PALETTE.put(0x373A3E, "minecraft:gray_concrete");
        COLOR_PALETTE.put(0x7D7D73, "minecraft:light_gray_concrete");
        COLOR_PALETTE.put(0x157788, "minecraft:cyan_concrete");
        COLOR_PALETTE.put(0x64209C, "minecraft:purple_concrete");
        COLOR_PALETTE.put(0x2D2F8F, "minecraft:blue_concrete");
        COLOR_PALETTE.put(0x603C20, "minecraft:brown_concrete");
        COLOR_PALETTE.put(0x495B24, "minecraft:green_concrete");
        COLOR_PALETTE.put(0x8E2121, "minecraft:red_concrete");
        COLOR_PALETTE.put(0x080A0F, "minecraft:black_concrete");

        COLOR_PALETTE.put(0x985E44, "minecraft:terracotta");
        COLOR_PALETTE.put(0xD2B2A1, "minecraft:white_terracotta");
        COLOR_PALETTE.put(0xA25426, "minecraft:orange_terracotta");
        COLOR_PALETTE.put(0x96586D, "minecraft:magenta_terracotta");
        COLOR_PALETTE.put(0x726D8A, "minecraft:light_blue_terracotta");
        COLOR_PALETTE.put(0xBA8523, "minecraft:yellow_terracotta");
        COLOR_PALETTE.put(0x687635, "minecraft:lime_terracotta");
        COLOR_PALETTE.put(0xA34F4F, "minecraft:pink_terracotta");
        COLOR_PALETTE.put(0x3A2A24, "minecraft:gray_terracotta");
        COLOR_PALETTE.put(0x876B62, "minecraft:light_gray_terracotta");
        COLOR_PALETTE.put(0x575B5B, "minecraft:cyan_terracotta");
        COLOR_PALETTE.put(0x764656, "minecraft:purple_terracotta");
        COLOR_PALETTE.put(0x4A3C5B, "minecraft:blue_terracotta");
        COLOR_PALETTE.put(0x4D3324, "minecraft:brown_terracotta");
        COLOR_PALETTE.put(0x4C532A, "minecraft:green_terracotta");
        COLOR_PALETTE.put(0x8F3D2F, "minecraft:red_terracotta");
        COLOR_PALETTE.put(0x251710, "minecraft:black_terracotta");

        COLOR_PALETTE.put(0xEAEDED, "minecraft:white_wool");
        COLOR_PALETTE.put(0xF17716, "minecraft:orange_wool");
        COLOR_PALETTE.put(0xBE46B5, "minecraft:magenta_wool");
        COLOR_PALETTE.put(0x3CB0DA, "minecraft:light_blue_wool");
        COLOR_PALETTE.put(0xF9C629, "minecraft:yellow_wool");
        COLOR_PALETTE.put(0x71BA1A, "minecraft:lime_wool");
        COLOR_PALETTE.put(0xEE90AD, "minecraft:pink_wool");
        COLOR_PALETTE.put(0x3F4548, "minecraft:gray_wool");
        COLOR_PALETTE.put(0x8E8F87, "minecraft:light_gray_wool");
        COLOR_PALETTE.put(0x158A91, "minecraft:cyan_wool");
        COLOR_PALETTE.put(0x7B2BAD, "minecraft:purple_wool");
        COLOR_PALETTE.put(0x353A9E, "minecraft:blue_wool");
        COLOR_PALETTE.put(0x734829, "minecraft:brown_wool");
        COLOR_PALETTE.put(0x556E1C, "minecraft:green_wool");
        COLOR_PALETTE.put(0xA12823, "minecraft:red_wool");
        COLOR_PALETTE.put(0x16161B, "minecraft:black_wool");

        COLOR_PALETTE.put(0xE2E4E4, "minecraft:white_concrete_powder");
        COLOR_PALETTE.put(0xE38522, "minecraft:orange_concrete_powder");
        COLOR_PALETTE.put(0xC155B9, "minecraft:magenta_concrete_powder");
        COLOR_PALETTE.put(0x4BB5D6, "minecraft:light_blue_concrete_powder");
        COLOR_PALETTE.put(0xE9C739, "minecraft:yellow_concrete_powder");
        COLOR_PALETTE.put(0x7EBE2A, "minecraft:lime_concrete_powder");
        COLOR_PALETTE.put(0xA59BB6, "minecraft:pink_concrete_powder");
        COLOR_PALETTE.put(0x4D5155, "minecraft:gray_concrete_powder");
        COLOR_PALETTE.put(0x9B9B95, "minecraft:light_gray_concrete_powder");
        COLOR_PALETTE.put(0x25959D, "minecraft:cyan_concrete_powder");
        COLOR_PALETTE.put(0x8438B2, "minecraft:purple_concrete_powder");
        COLOR_PALETTE.put(0x464AA7, "minecraft:blue_concrete_powder");
        COLOR_PALETTE.put(0x7E5536, "minecraft:brown_concrete_powder");
        COLOR_PALETTE.put(0x61782D, "minecraft:green_concrete_powder");
        COLOR_PALETTE.put(0xA93633, "minecraft:red_concrete_powder");
        COLOR_PALETTE.put(0x1B1C21, "minecraft:black_concrete_powder");

        COLOR_PALETTE.put(0xDBD0A4, "minecraft:sand");
        COLOR_PALETTE.put(0xBF6721, "minecraft:red_sand");
        COLOR_PALETTE.put(0xA1A7B4, "minecraft:clay");
        COLOR_PALETTE.put(0xF9FEFE, "minecraft:snow_block");
    }

    public ArmorStandImages() {
        super(Trouser.operator, "ArmorStandImages", "Draws PNG images in the world using armor stands. Images greater than 100x100 pixels will cause big lag.");
    }

    @Override
    public void onActivate() {
        if (mc.player == null || mc.world == null) return;
        if (PermissionUtils.getPermissionLevel(mc.player) < 2 && mc.world.isChunkLoaded(mc.player.getChunkPos().x, mc.player.getChunkPos().z)) {
            error("Requires Operator permissions!");
            toggle();
            return;
        }
        scaledImage = null;
        pixelPositions.clear();
        isPlacing = false;
        wasKeyPressed = false;
        currentPixelIndex = 0;
        tickCounter = 0;
        loadAndProcessImage();
    }

    @Override
    public void onDeactivate() {
        scaledImage = null;
        pixelPositions.clear();
        isPlacing = false;
        wasKeyPressed = false;
        currentPixelIndex = 0;
        tickCounter = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        boolean currentKeyPressed = isPlaceKeyPressed();

        if (currentKeyPressed && !wasKeyPressed) {
            if (isPlacing) {
                stopPlacing();
            } else if (!pixelPositions.isEmpty()) {
                startPlacing();
            }
            wasKeyPressed = true;
        } else if (!currentKeyPressed && wasKeyPressed) {
            wasKeyPressed = false;
        }

        if (!isPlacing || mc.player == null || mc.world == null) return;

        if (tickCounter > 0) {
            tickCounter--;
            return;
        }

        if (currentPixelIndex < pixelPositions.size()) {
            int perTick = Math.max(1, maxPerTick.get());
            for (int i = 0; i < perTick && currentPixelIndex < pixelPositions.size(); i++) {
                PixelPosition pixel = pixelPositions.get(currentPixelIndex);
                spawnArmorStand(pixel);
                currentPixelIndex++;
            }
            tickCounter = delayTicks.get();
            if (currentPixelIndex % 50 == 0) {
                info("Placement progress: %d/%d", currentPixelIndex, pixelPositions.size());
            }
        } else {
            isPlacing = false;
            info("Image placement complete! Placed %d pixels.", pixelPositions.size());
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!showPreview.get() || mc.player == null) return;
        if (pixelPositions.isEmpty()) return;

        double size = pixelSize.get();

        for (PixelPosition pixel : pixelPositions) {
            SettingColor colorToUse;

            if (useRealColors.get()) {
                int red = (pixel.color >> 16) & 0xFF;
                int green = (pixel.color >> 8) & 0xFF;
                int blue = pixel.color & 0xFF;
                colorToUse = new SettingColor(red, green, blue, useRealColorsTransparency.get());
            } else {
                colorToUse = previewColor.get();
            }

            if (renderPixelBoxes.get()) {
                event.renderer.box(
                        pixel.x - size / 2,
                        pixel.y+1.5 - size / 2,
                        pixel.z - size / 2,
                        pixel.x + size / 2,
                        pixel.y+1.5 + size / 2,
                        pixel.z + size / 2,
                        colorToUse,
                        colorToUse,
                        ShapeMode.Both,
                        0
                );
            } else {
                event.renderer.box(
                        pixel.x - size / 2,
                        pixel.y+1.5 - size / 2,
                        pixel.z - size / 2,
                        pixel.x + size / 2,
                        pixel.y+1.5 + size / 2,
                        pixel.z + size / 2,
                        colorToUse,
                        colorToUse,
                        ShapeMode.Lines,
                        0
                );
            }
        }
    }

    private void loadAndProcessImage() {
        String path = imagePath.get().replace("\"", "").trim();
        if (path.isEmpty()) {
            error("Please set an image path first!");
            return;
        }

        try {
            File file = new File(path);
            if (!file.exists()) {
                error("Image file does not exist: " + path);
                return;
            }

            BufferedImage loadedImage = ImageIO.read(file);
            if (loadedImage == null) {
                error("Could not read image file. Please ensure it is a valid PNG.");
                return;
            }

            double scaleValue = scale.get();
            int origWidth = loadedImage.getWidth();
            int origHeight = loadedImage.getHeight();
            int newWidth = (int) (origWidth * scaleValue);
            int newHeight = (int) (origHeight * scaleValue);

            double rotationRad = 0;
            if (mc.player != null) {
                float playerYaw = mc.player.getYaw();
                playerYaw = Math.round(playerYaw / 90f) * 90f;
                rotationRad = Math.toRadians(playerYaw + 180);
            }

            double cosRad = Math.abs(Math.cos(rotationRad));
            double sinRad = Math.abs(Math.sin(rotationRad));
            int rotatedWidth = (int) Math.round(newWidth * cosRad + newHeight * sinRad);
            int rotatedHeight = (int) Math.round(newWidth * sinRad + newHeight * cosRad);

            scaledImage = new BufferedImage(rotatedWidth, rotatedHeight, BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = scaledImage.createGraphics();

            double centerX = rotatedWidth / 2.0;
            double centerY = rotatedHeight / 2.0;
            g.rotate(rotationRad, centerX, centerY);
            g.drawImage(loadedImage,
                    (int)(centerX - newWidth / 2), (int)(centerY - newHeight / 2),
                    (int)(centerX + newWidth / 2), (int)(centerY + newHeight / 2),
                    0, 0, origWidth, origHeight, null);
            g.dispose();

            info("Successfully loaded and scaled image!");

            calculatePixelPositions();
        } catch (IOException e) {
            error("Error loading image: " + e.getMessage());
        }
    }
    private void calculatePixelPositions() {
        if (scaledImage == null || mc.player == null) return;

        pixelPositions.clear();

        int width = scaledImage.getWidth();
        int height = scaledImage.getHeight();

        double spacing = pixelSpacing.get();
        double rotationRad = Math.toRadians(rotation.get());
        double yOffsetValue = yOffset.get();

        Vec3d playerPos = mc.player.getEntityPos();

        double offsetX = 0;
        double offsetZ = 0;

        switch (startPosition.get()) {
            case TOP_RIGHT:
                offsetX = 0;
                offsetZ = 0;
                break;
            case TOP_LEFT:
                offsetX = -(width - 1) * spacing;
                offsetZ = 0;
                break;
            case BOTTOM_RIGHT:
                offsetX = 0;
                offsetZ = (height - 1) * spacing;
                break;
            case BOTTOM_LEFT:
                offsetX = -(width - 1) * spacing;
                offsetZ = (height - 1) * spacing;
                break;
            case CENTER:
                offsetX = -(width - 1) * spacing / 2.0;
                offsetZ = -(height - 1) * spacing / 2.0;
                break;
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = scaledImage.getRGB(x, y);
                int alpha = (rgb >> 24) & 0xFF;

                if (alpha < 128) continue;

                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;

                int pixelColor = (red << 16) | (green << 8) | blue | 0xFF000000;

                double relX = x * spacing + offsetX;
                double relZ = y * spacing + offsetZ;

                double rotX = relX * Math.cos(rotationRad) - relZ * Math.sin(rotationRad);
                double rotZ = relX * Math.sin(rotationRad) + relZ * Math.cos(rotationRad);

                double posX = playerPos.x + rotX;
                double posY = playerPos.y + yOffsetValue;
                double posZ = playerPos.z + rotZ;

                pixelPositions.add(new PixelPosition(posX, posY, posZ, pixelColor));
            }
        }
        info("Calculation complete. Total pixels: %d", pixelPositions.size());
        warning("Press " + placeKey.get() + " to start building the image!");
    }
    private boolean isPlaceKeyPressed() {
        return placeKey.get().isPressed();
    }
    public void startPlacing() {
        if (pixelPositions.isEmpty()) {
            error("No pixels to place. Please load an image first.");
            return;
        }

        isPlacing = true;
        currentPixelIndex = 0;
        tickCounter = 0;
        info("Started placing image. Total pixels: %d", pixelPositions.size());
    }
    public void stopPlacing() {
        isPlacing = false;
        info("Placement stopped.");
    }
    private void spawnArmorStand(PixelPosition pixel) {
        String concreteId = findClosestConcreteColor(pixel.color);
        if (concreteId == null) {
            concreteId = "minecraft:white_concrete";
        }

        String command = String.format(
                "summon armor_stand %.2f %.2f %.2f {Invisible:1b,NoGravity:1b,equipment:{head:{id:\"%s\"}}}",
                pixel.x, pixel.y, pixel.z, concreteId
        );

        if (mc.player != null) {
            mc.player.networkHandler.sendChatCommand(command);
        }
    }
    private String findClosestConcreteColor(int color) {
        int targetRed = (color >> 16) & 0xFF;
        int targetGreen = (color >> 8) & 0xFF;
        int targetBlue = color & 0xFF;

        String closestId = null;
        double minDistance = Double.MAX_VALUE;

        for (Map.Entry<Integer, String> entry : COLOR_PALETTE.entrySet()) {
            int concreteColor = entry.getKey();
            int red = (concreteColor >> 16) & 0xFF;
            int green = (concreteColor >> 8) & 0xFF;
            int blue = concreteColor & 0xFF;

            double distance = Math.sqrt(
                    Math.pow(red - targetRed, 2) +
                            Math.pow(green - targetGreen, 2) +
                            Math.pow(blue - targetBlue, 2)
            );

            if (distance < minDistance) {
                minDistance = distance;
                closestId = entry.getValue();
            }
        }

        return closestId;
    }
    private record PixelPosition(double x, double y, double z, int color) {}
}