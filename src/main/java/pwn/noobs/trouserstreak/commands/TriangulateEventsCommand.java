package pwn.noobs.trouserstreak.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import pwn.noobs.trouserstreak.Trouser;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TriangulateEventsCommand extends Command {
    private enum ProxyCoordinateMode {
        PAPER_CAST_TO_INT,
        VANILLA_BLOCKPOS_CONTAINING
    }
    private static final double EPSILON = 1.0e-9;
    private static final double SEARCH_RADIUS = 30_000_000.0;
    private static final int MAP_IMAGE_WIDTH = 1400;
    private static final int MAP_IMAGE_HEIGHT = 1000;
    private static final int MAP_LEFT_MARGIN = 110;
    private static final int MAP_RIGHT_MARGIN = 35;
    private static final int MAP_TOP_MARGIN = 70;
    private static final int MAP_BOTTOM_MARGIN = 100;
    private static final double MAP_PADDING_FRACTION = 0.1;
    private static final Path POLYGON_EXPORT_DIRECTORY = Path.of("TrouserStreak", "EventTriangulation", "PolygonExports");
    private static final DateTimeFormatter MAP_FILENAME_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").withLocale(Locale.ROOT).withZone(ZoneOffset.UTC);
    private static final Path TEXT_EXPORT_DIRECTORY = Path.of("TrouserStreak", "EventTriangulation", "TextExports");
    private static final Path TEXT_EXPORT_FILE = TEXT_EXPORT_DIRECTORY.resolve("triangulation-results.txt");
    private record Vec2(double x, double z){}
    private record HalfPlane(Vec2 origin, double dirX, double dirZ) {
        boolean contains(Vec2 point) {
            double relativeX = point.x() - origin.x();
            double relativeZ = point.z() - origin.z();
            double side = cross(dirX, dirZ, relativeX, relativeZ);
            double tolerance = orientationTolerance(dirX, dirZ, relativeX, relativeZ);

            return side >= -tolerance;
        }
    }

    private static double orientationTolerance(double firstX, double firstZ, double secondX, double secondZ) {
        double scale = Math.abs(firstX * secondZ) + Math.abs(firstZ * secondX);
        return Math.max(EPSILON, 64.0 * Math.ulp(Math.max(1.0, scale)));
    }

    private record Observation(
            long timestamp,
            String observerUuid, String observerName,
            String dimension,
            String event, int eventId,
            double playerX, double playerZ, int packetX, int packetZ,
            Path sourceFile
    ){}

    private record Wedge(double lowerAngle, double upperAngle, double halfAngleDegrees, List<HalfPlane> halfPlanes){}

    private record TriangulationResult(
            String event, int eventId,
            String dimension,
            long firstTimestamp, long lastTimestamp,
            int sampleCount, int observerCount,
            double averageWedgeHalfAngle, double maximumWedgeHalfAngle,
            double averageProxyDistance, double minimumProxyDistance, double maximumProxyDistance,
            double searchCenterX, double searchCenterZ, double radiusX, double radiusZ,
            double minX, double maxX, double minZ, double maxZ,
            int polygonVertices,
            boolean touchesSearchBoundary,
            List<Vec2> polygon
    ) {}

    public TriangulateEventsCommand() {
        super("triangulate-events", "Triangulates global-event observations from JSON file paths.");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.then(
        argument("time-window-ms", IntegerArgumentType.integer(1)).then(
        argument("max-results", IntegerArgumentType.integer(1)).then(
        argument("proxy-mode", StringArgumentType.word()).suggests((context, suggestionsBuilder) -> {
        suggestionsBuilder.suggest("vanilla");
        suggestionsBuilder.suggest("paper");

        return suggestionsBuilder.buildFuture();
        }).then(argument("export-images", BoolArgumentType.bool()).then(
        argument("export-text", BoolArgumentType.bool()).then(
        argument("triangulation-files", StringArgumentType.greedyString()).suggests((context, suggestionsBuilder) -> {
            suggestionsBuilder.suggest("Example: userealpath\\first.json|userealpath\\second.json|userealpath\\third.json");
            return suggestionsBuilder.buildFuture();
        }).executes(context -> {
        int timeWindowMs = context.getArgument("time-window-ms", Integer.class);
        int maxResults = context.getArgument("max-results", Integer.class);
        String rawMode = context.getArgument("proxy-mode", String.class);
        boolean exportImages = context.getArgument("export-images", Boolean.class);
        boolean exportText = context.getArgument("export-text", Boolean.class);
        String rawFiles = context.getArgument("triangulation-files", String.class);

        ProxyCoordinateMode proxyMode = parseProxyCoordinateMode(rawMode);

        if (proxyMode == null) {
            error("Unknown proxy mode §e" + rawMode + "§c. Use §evanilla§c or §epaper§c.");
            return 0;
        }

        triangulateFromFiles(timeWindowMs, maxResults, proxyMode, parseFiles(rawFiles), exportImages, exportText);
        return SINGLE_SUCCESS;
        })))))));
    }

    private List<Path> parseFiles(String rawFiles) {
        List<Path> files = new ArrayList<>();

        for (String rawFile : rawFiles.split("\\|")) {
            String file = rawFile.trim();
            if (!file.isEmpty()) files.add(Path.of(file));
        }

        return files;
    }

    private ProxyCoordinateMode parseProxyCoordinateMode(String rawMode) {
        return switch (rawMode.toLowerCase(java.util.Locale.ROOT)) {
            case "vanilla" -> ProxyCoordinateMode.VANILLA_BLOCKPOS_CONTAINING;
            case "paper" -> ProxyCoordinateMode.PAPER_CAST_TO_INT;
            default -> null;
        };
    }

    private void triangulateFromFiles(long timeWindowMs, int maxResults, ProxyCoordinateMode proxyMode, List<Path> triangulationFiles, boolean exportImages, boolean exportText) {
        if (triangulationFiles.isEmpty()) {
            error("No triangulation JSON files were provided.");
            return;
        }

        List<Observation> allObservations = new ArrayList<>();
        int filesLoaded = 0;

        for (Path dataFile : triangulationFiles) {
            if (!Files.isRegularFile(dataFile)) {
                warning("Could not find JSON file!");
                continue;
            }

            try {
                int before = allObservations.size();

                loadObservations(dataFile, allObservations);

                int loaded = allObservations.size() - before;
                filesLoaded++;

                info(String.format(Locale.ROOT,"Loaded %d observation(s)!", loaded));
            } catch (IOException | RuntimeException e) {
                error("Could not read JSON file!");
                Trouser.LOG.error("Failed to load triangulation JSON from {}", dataFile.toAbsolutePath(), e);
            }
        }

        if (filesLoaded == 0) {
            error("No triangulation JSON files could be loaded.");
            return;
        }

        if (allObservations.size() < 2) {
            error("Need at least 2 valid observations. Loaded " + allObservations.size() + ".");
            return;
        }

        allObservations.sort(Comparator.comparingLong(Observation::timestamp));
        List<List<Observation>> eventGroups = groupObservations(allObservations, timeWindowMs);
        List<TriangulationResult> validResults = new ArrayList<>();

        for (List<Observation> eventGroup : eventGroups) {
            TriangulationResult result = triangulateGroup(eventGroup, proxyMode);
            if (result != null) validResults.add(result);
        }

        validResults.sort(Comparator.comparingLong(TriangulationResult::lastTimestamp));

        if (validResults.isEmpty()) {
            error("No valid global-event proxy wedge triangulations found from " + eventGroups.size() + " event group(s).");
            return;
        }

        int shownResults = Math.min(maxResults, validResults.size());

        if (exportText) {
            try {
                writeTextReport(proxyMode, validResults, shownResults);
                info("§aSaved triangulation text report.");
            } catch (IOException e) {
                error("Failed to save triangulation text report: " + e.getMessage());
                Trouser.LOG.error("Failed to write triangulation text report to {}", TEXT_EXPORT_FILE.toAbsolutePath(), e);
            }
        }

        info(String.format(Locale.ROOT,"Mode: %s | Found %d valid global-event proxy wedge triangulation(s); showing %d.", displayProxyCoordinateMode(proxyMode), validResults.size(), shownResults));

        for (int i = 0; i < shownResults; i++) {
            TriangulationResult result = validResults.get(i);

            if (result.touchesSearchBoundary()) {
                warning(String.format(Locale.ROOT,"#%d §e%s §f| %s | The search region is unbounded. Get more observations from a different position.", i + 1, result.event(), result.dimension()));
                info(String.format(Locale.ROOT,"Current visible bounds: X %.0f to %.0f, Z %.0f to %.0f | %d observations from %d observers.", result.minX(), result.maxX(), result.minZ(), result.maxZ(), result.sampleCount(), result.observerCount()));
                continue;
            }

            info(String.format(Locale.ROOT,"#%d §e%s §f| %s | §bSearch bounds: X %.0f to %.0f, Z %.0f to %.0f§f. Search Center: §bX %.0f Z %.0f §f| Range: §bX ±%.0f Z ±%.0f §f| %d observations from %d observers.", i + 1, result.event(), result.dimension(), result.minX(), result.maxX(), result.minZ(), result.maxZ(), result.searchCenterX(), result.searchCenterZ(), result.radiusX(), result.radiusZ(), result.sampleCount(), result.observerCount()));

            if (exportImages) {
                try {
                    writePolygonMap(result, i + 1);
                    info("§aSaved polygon map.");
                } catch (IOException e) {
                    error("Failed to save polygon map: " + e.getMessage());
                    Trouser.LOG.error("Failed to write polygon map to {}", POLYGON_EXPORT_DIRECTORY.toAbsolutePath(), e);
                }
            }
        }
    }
    private void writeTextReport(ProxyCoordinateMode proxyMode, List<TriangulationResult> validResults, int shownResults) throws IOException {
        Files.createDirectories(TEXT_EXPORT_DIRECTORY);

        StringBuilder report = new StringBuilder();
        report.append(System.lineSeparator()).append("================================================================").append(System.lineSeparator()).append(System.lineSeparator());
        report.append("Global Event Search Areas").append(System.lineSeparator());
        report.append("Generated: ").append(java.time.Instant.now()).append(System.lineSeparator());
        report.append("Proxy mode: ").append(displayProxyCoordinateMode(proxyMode)).append(System.lineSeparator());
        report.append("Results exported: ").append(shownResults).append(" of ").append(validResults.size()).append(System.lineSeparator());
        report.append(System.lineSeparator());

        for (int i = 0; i < shownResults; i++) {
            appendTextResult(report, validResults.get(i), i + 1);
            report.append(System.lineSeparator()).append("------------------------------------------------------------").append(System.lineSeparator()).append(System.lineSeparator());
        }

        Files.writeString(TEXT_EXPORT_FILE, report.toString(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
    }
    private void appendTextResult(StringBuilder report, TriangulationResult result, int resultNumber) {
        report.append("Result ").append(resultNumber).append(": ").append(result.event()).append(System.lineSeparator());
        report.append("Dimension: ").append(result.dimension()).append(System.lineSeparator());

        if (result.touchesSearchBoundary()) {
            report.append("Status: The search region is unbounded. ").append("Collect more observations from a different position.").append(System.lineSeparator());
            report.append(String.format(Locale.ROOT, "Current visible bounds: X %.0f to %.0f, Z %.0f to %.0f%n", result.minX(), result.maxX(), result.minZ(), result.maxZ()));
        } else {
            report.append("Status: Bounded search region.").append(System.lineSeparator());
            report.append(String.format(Locale.ROOT, "Search bounds: X %.0f to %.0f, Z %.0f to %.0f%n", result.minX(), result.maxX(), result.minZ(), result.maxZ()));
            report.append(String.format(Locale.ROOT, "Search center: X %.0f, Z %.0f%n", result.searchCenterX(), result.searchCenterZ()));
            report.append(String.format(Locale.ROOT, "Directional range: X +/- %.0f, Z +/- %.0f%n", result.radiusX(), result.radiusZ()));
        }

        report.append("Observations: ").append(result.sampleCount()).append(" from ").append(result.observerCount()).append(" observers").append(System.lineSeparator());
        report.append(String.format(Locale.ROOT, "Proxy distance: about %.1f blocks%n", result.averageProxyDistance()));
        report.append("Polygon corners: ").append(result.polygonVertices()).append(System.lineSeparator());
        report.append(System.lineSeparator()).append("Exact polygon points:").append(System.lineSeparator());

        for (int i = 0; i < result.polygon().size(); i++) {
            Vec2 point = result.polygon().get(i);
            report.append(String.format(Locale.ROOT, "  %d. X %.3f, Z %.3f%n", i + 1, point.x(), point.z()));
        }
    }
    private void writePolygonMap(TriangulationResult result, int resultNumber) throws IOException {
        Files.createDirectories(POLYGON_EXPORT_DIRECTORY);

        BufferedImage image = new BufferedImage(MAP_IMAGE_WIDTH, MAP_IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);

        Graphics2D graphics = image.createGraphics();

        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setColor(new Color(25, 25, 30));
            graphics.fillRect(0, 0, MAP_IMAGE_WIDTH, MAP_IMAGE_HEIGHT);

            int plotX = MAP_LEFT_MARGIN;
            int plotY = MAP_TOP_MARGIN;
            int plotWidth = MAP_IMAGE_WIDTH - MAP_LEFT_MARGIN - MAP_RIGHT_MARGIN;
            int plotHeight = MAP_IMAGE_HEIGHT - MAP_TOP_MARGIN - MAP_BOTTOM_MARGIN;

            MapViewport viewport = createViewport(result, plotWidth, plotHeight);

            drawMapTitle(graphics, result, resultNumber);
            drawGrid(graphics, viewport, plotX, plotY, plotWidth, plotHeight);
            drawBoundsRectangle(graphics, result, viewport, plotX, plotY);
            drawPolygon(graphics, result.polygon(), viewport, plotX, plotY);
            drawCenterMarker(graphics, result, viewport, plotX, plotY);
            drawMapLegend(graphics, result, plotX, plotY);
        } finally {
            graphics.dispose();
        }

        String safeEventName = result.event().replaceAll("[^A-Za-z0-9._-]", "_");
        String filename = String.format(Locale.ROOT, "%03d_%s_%s.png", resultNumber, safeEventName, MAP_FILENAME_TIME.format(Instant.ofEpochMilli(result.lastTimestamp())));

        Path outputFile = POLYGON_EXPORT_DIRECTORY.resolve(filename);

        if (!ImageIO.write(image, "png", outputFile.toFile())) throw new IOException("No PNG image writer is available.");
    }
    private record MapViewport(double minX, double maxX, double minZ, double maxZ, double scale){}
    private MapViewport createViewport(TriangulationResult result, int plotWidth, int plotHeight) {
        double spanX = result.maxX() - result.minX();
        double spanZ = result.maxZ() - result.minZ();
        double minimumSpan = 1.0;

        spanX = Math.max(spanX, minimumSpan);
        spanZ = Math.max(spanZ, minimumSpan);

        double paddingX = spanX * MAP_PADDING_FRACTION;
        double paddingZ = spanZ * MAP_PADDING_FRACTION;
        double minX = result.minX() - paddingX;
        double maxX = result.maxX() + paddingX;
        double minZ = result.minZ() - paddingZ;
        double maxZ = result.maxZ() + paddingZ;
        double visibleSpanX = maxX - minX;
        double visibleSpanZ = maxZ - minZ;
        double scale = Math.min(plotWidth / visibleSpanX, plotHeight / visibleSpanZ);
        double expandedSpanX = plotWidth / scale;
        double expandedSpanZ = plotHeight / scale;
        double centerX = (minX + maxX) * 0.5;
        double centerZ = (minZ + maxZ) * 0.5;

        minX = centerX - expandedSpanX * 0.5;
        maxX = centerX + expandedSpanX * 0.5;
        minZ = centerZ - expandedSpanZ * 0.5;
        maxZ = centerZ + expandedSpanZ * 0.5;

        return new MapViewport(minX, maxX, minZ, maxZ, scale);
    }
    private void drawMapTitle(Graphics2D graphics, TriangulationResult result, int resultNumber) {
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        graphics.drawString(String.format(Locale.ROOT, "#%d %s — %s", resultNumber, result.event(), result.dimension()), MAP_LEFT_MARGIN, 30);
        graphics.setColor(new Color(190, 190, 190));
        graphics.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        graphics.drawString(String.format(Locale.ROOT, "Search bounds: X %.0f to %.0f, Z %.0f to %.0f", result.minX(), result.maxX(), result.minZ(), result.maxZ()), MAP_LEFT_MARGIN, 52);
    }
    private void drawGrid(Graphics2D graphics, MapViewport viewport, int plotX, int plotY, int plotWidth, int plotHeight) {
        graphics.setColor(new Color(40, 40, 48));
        graphics.fillRect(plotX, plotY, plotWidth, plotHeight);
        graphics.setColor(new Color(130, 130, 140));
        graphics.setStroke(new BasicStroke(1.5f));
        graphics.drawRect(plotX, plotY, plotWidth, plotHeight);

        double gridStep = chooseGridStep(Math.max(viewport.maxX() - viewport.minX(), viewport.maxZ() - viewport.minZ()));
        double firstGridX = Math.ceil(viewport.minX() / gridStep) * gridStep;
        double firstGridZ = Math.ceil(viewport.minZ() / gridStep) * gridStep;

        graphics.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        for (double worldX = firstGridX; worldX <= viewport.maxX() + EPSILON; worldX += gridStep) {
            int screenX = worldToScreenX(worldX, viewport, plotX);

            graphics.setColor(new Color(75, 75, 85));
            graphics.drawLine(screenX, plotY, screenX, plotY + plotHeight);
            graphics.setColor(new Color(195, 195, 205));
            graphics.drawString(formatGridCoordinate(worldX), screenX - 30, plotY + plotHeight + 20);
        }

        for (double worldZ = firstGridZ; worldZ <= viewport.maxZ() + EPSILON; worldZ += gridStep) {
            int screenY = worldToScreenY(worldZ, viewport, plotY);

            graphics.setColor(new Color(75, 75, 85));
            graphics.drawLine(plotX, screenY, plotX + plotWidth, screenY);
            graphics.setColor(new Color(195, 195, 205));
            graphics.drawString(formatGridCoordinate(worldZ), 8, screenY + 4);
        }

        graphics.setColor(new Color(210, 210, 210));
        graphics.drawString("X", plotX + plotWidth / 2, plotY + plotHeight + 55);
        graphics.drawString("Z", 45, plotY - 18);
    }
    private void drawBoundsRectangle(Graphics2D graphics, TriangulationResult result, MapViewport viewport, int plotX, int plotY) {
        int left = worldToScreenX(result.minX(), viewport, plotX);
        int right = worldToScreenX(result.maxX(), viewport, plotX);
        int top = worldToScreenY(result.maxZ(), viewport, plotY);
        int bottom = worldToScreenY(result.minZ(), viewport, plotY);

        graphics.setColor(new Color(255, 205, 80, 190));
        graphics.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{6.0f, 6.0f}, 0.0f));
        graphics.drawRect(left, top, right - left, bottom - top);
    }
    private void drawPolygon(Graphics2D graphics, List<Vec2> polygon, MapViewport viewport, int plotX, int plotY) {
        if (polygon.size() < 3) return;

        Path2D.Double shape = new Path2D.Double();

        Vec2 firstPoint = polygon.get(0);

        shape.moveTo(worldToScreenX(firstPoint.x(), viewport, plotX), worldToScreenY(firstPoint.z(), viewport, plotY));

        for (int i = 1; i < polygon.size(); i++) {
            Vec2 point = polygon.get(i);

            shape.lineTo(worldToScreenX(point.x(), viewport, plotX), worldToScreenY(point.z(), viewport, plotY));
        }

        shape.closePath();

        graphics.setColor(new Color(45, 190, 255, 85));
        graphics.fill(shape);
        graphics.setColor(new Color(45, 205, 255));
        graphics.setStroke(new BasicStroke(3.0f));
        graphics.draw(shape);
    }
    private void drawCenterMarker(Graphics2D graphics, TriangulationResult result, MapViewport viewport, int plotX, int plotY) {
        int x = worldToScreenX(result.searchCenterX(), viewport, plotX);
        int y = worldToScreenY(result.searchCenterZ(), viewport, plotY);
        graphics.setColor(new Color(255, 90, 90));
        graphics.setStroke(new BasicStroke(2.5f));
        graphics.drawLine(x - 9, y, x + 9, y);
        graphics.drawLine(x, y - 9, x, y + 9);
        graphics.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        graphics.drawString(String.format(Locale.ROOT, "Center: X %.0f, Z %.0f", result.searchCenterX(), result.searchCenterZ()), x + 12, y - 10);
    }
    private void drawMapLegend(Graphics2D graphics, TriangulationResult result, int plotX, int plotY) {
        int legendX = plotX + 14;
        int legendY = plotY + 22;
        graphics.setColor(new Color(15, 15, 20, 210));
        graphics.fillRoundRect(legendX - 8, legendY - 16, 440, 96, 8, 8);
        graphics.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        graphics.setColor(new Color(45, 205, 255));
        graphics.drawString("Cyan area: search area", legendX, legendY);
        graphics.setColor(new Color(255, 205, 80));
        graphics.drawString("Yellow box: search bounds", legendX, legendY + 18);
        graphics.setColor(new Color(255, 90, 90));
        graphics.drawString(String.format(Locale.ROOT, "Red cross: search center | Range: X +/- %.0f, Z +/- %.0f", result.radiusX(), result.radiusZ()), legendX, legendY + 36);
        graphics.setColor(new Color(210, 210, 210));
        graphics.drawString(String.format(Locale.ROOT, "%d observations from %d observers", result.sampleCount(), result.observerCount()), legendX, legendY + 54);
    }
    private int worldToScreenX(double worldX, MapViewport viewport, int plotX) {
        return plotX + (int) Math.round((worldX - viewport.minX()) * viewport.scale());
    }
    private int worldToScreenY(double worldZ, MapViewport viewport, int plotY) {
        return plotY + (int) Math.round((viewport.maxZ() - worldZ) * viewport.scale());
    }
    private double chooseGridStep(double largestSpan) {
        double desiredStep = largestSpan / 8.0;

        if (desiredStep <= 0.0 || !Double.isFinite(desiredStep)) return 1.0;

        double magnitude = Math.pow(10.0, Math.floor(Math.log10(desiredStep)));
        double normalized = desiredStep / magnitude;
        double niceNormalized;

        if (normalized <= 1.0) niceNormalized = 1.0;
        else if (normalized <= 2.0) niceNormalized = 2.0;
        else if (normalized <= 5.0) niceNormalized = 5.0;
        else niceNormalized = 10.0;

        return niceNormalized * magnitude;
    }

    private String formatGridCoordinate(double coordinate) {
        return String.format(Locale.ROOT, "%.0f", coordinate);
    }

    private String displayProxyCoordinateMode(ProxyCoordinateMode proxyMode) {
        return switch (proxyMode) {
            case VANILLA_BLOCKPOS_CONTAINING -> "Vanilla BlockPos.containing";
            case PAPER_CAST_TO_INT -> "Paper (int) cast";
        };
    }

    private void loadObservations(Path dataFile, List<Observation> output) throws IOException {
        String rawJson = Files.readString(dataFile, StandardCharsets.UTF_8);
        JsonElement root = JsonParser.parseString(rawJson);

        if (!root.isJsonArray()) throw new IllegalStateException("Expected JSON root to be an array.");

        JsonArray observations = root.getAsJsonArray();

        for (JsonElement element : observations) {
            if (!element.isJsonObject()) continue;

            JsonObject object = element.getAsJsonObject();

            try {
                long timestamp = object.get("timestamp").getAsLong();
                String observerUuid = object.get("observerUuid").getAsString();
                String observerName = object.get("observerName").getAsString();
                String dimension = object.get("dimension").getAsString();
                String event = object.get("event").getAsString();
                int eventId = object.get("eventId").getAsInt();
                double playerX = object.get("playerX").getAsDouble();
                double playerZ = object.get("playerZ").getAsDouble();
                int packetX = object.get("packetX").getAsInt();
                int packetZ = object.get("packetZ").getAsInt();

                output.add(new Observation(timestamp, observerUuid, observerName, dimension, event, eventId, playerX, playerZ, packetX, packetZ, dataFile));
            } catch (Exception ignored) {
                warning("Skipped malformed observation!");
            }
        }
    }

    private List<List<Observation>> groupObservations(List<Observation> sortedObservations, long timeWindowMs) {
        List<List<Observation>> groups = new ArrayList<>();

        for (Observation observation : sortedObservations) {
            List<Observation> matchingGroup = null;

            for (List<Observation> candidateGroup : groups) {
                Observation firstObservation = candidateGroup.get(0);

                long timeDifference = Math.abs(observation.timestamp() - firstObservation.timestamp());

                if (timeDifference > timeWindowMs) continue;

                if (sameEventFamily(observation, firstObservation)) {
                    matchingGroup = candidateGroup;
                    break;
                }
            }

            if (matchingGroup == null) {
                matchingGroup = new ArrayList<>();
                groups.add(matchingGroup);
            }

            boolean duplicate = false;

            for (Observation existing : matchingGroup) {
                if (sameGeometry(existing, observation)) {
                    duplicate = true;
                    break;
                }
            }

            if (!duplicate) matchingGroup.add(observation);
        }
        return groups;
    }

    private boolean sameGeometry(Observation first, Observation second) {
        return first.playerX() == second.playerX() && first.playerZ() == second.playerZ() && first.packetX() == second.packetX() && first.packetZ() == second.packetZ();
    }

    private boolean sameEventFamily(Observation first, Observation second) {
        return first.eventId() == second.eventId() && first.event().equals(second.event()) && first.dimension().equals(second.dimension());
    }

    private TriangulationResult triangulateGroup(List<Observation> observations, ProxyCoordinateMode proxyMode) {
        if (observations.size() < 2) return null;

        Set<String> uniqueObservers = new HashSet<>();

        for (Observation observation : observations) {
            uniqueObservers.add(observation.observerUuid());
        }

        if (uniqueObservers.size() < 2) return null;

        Observation[] samples = observations.toArray(new Observation[0]);

        List<Vec2> feasibleRegion = new ArrayList<>();

        feasibleRegion.add(new Vec2(-SEARCH_RADIUS, -SEARCH_RADIUS));
        feasibleRegion.add(new Vec2(SEARCH_RADIUS, -SEARCH_RADIUS));
        feasibleRegion.add(new Vec2(SEARCH_RADIUS, SEARCH_RADIUS));
        feasibleRegion.add(new Vec2(-SEARCH_RADIUS, SEARCH_RADIUS));

        double wedgeHalfAngleSum = 0.0;
        double maximumWedgeHalfAngle = 0.0;
        double proxyDistanceSum = 0.0;
        double minimumProxyDistance = Double.POSITIVE_INFINITY;
        double maximumProxyDistance = Double.NEGATIVE_INFINITY;

        for (Observation sample : samples) {
            double proxyDistance = estimateProxyDistance(sample, proxyMode);
            proxyDistanceSum += proxyDistance;
            minimumProxyDistance = Math.min(minimumProxyDistance, proxyDistance);
            maximumProxyDistance = Math.max(maximumProxyDistance, proxyDistance);

            Wedge wedge = createGlobalEventProxyWedge(sample, proxyMode);

            if (wedge == null || wedge.halfPlanes().size() != 2) return null;

            wedgeHalfAngleSum += wedge.halfAngleDegrees();
            maximumWedgeHalfAngle = Math.max(maximumWedgeHalfAngle, wedge.halfAngleDegrees());

            for (HalfPlane boundary : wedge.halfPlanes()) {
                feasibleRegion = clipPolygon(feasibleRegion, boundary);
                if (feasibleRegion.size() < 3) return null;
            }
        }

        double areaTwice = Math.abs(signedAreaTwice(feasibleRegion));

        if (areaTwice < EPSILON) return null;

        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        for (Vec2 point : feasibleRegion) {
            minX = Math.min(minX, point.x());
            maxX = Math.max(maxX, point.x());
            minZ = Math.min(minZ, point.z());
            maxZ = Math.max(maxZ, point.z());
        }

        double searchCenterX = (minX + maxX) * 0.5;
        double searchCenterZ = (minZ + maxZ) * 0.5;
        double radiusX = (maxX - minX) * 0.5;
        double radiusZ = (maxZ - minZ) * 0.5;

        boolean touchesSearchBoundary = Math.abs(minX + SEARCH_RADIUS) <= 1.0 || Math.abs(maxX - SEARCH_RADIUS) <= 1.0 || Math.abs(minZ + SEARCH_RADIUS) <= 1.0 || Math.abs(maxZ - SEARCH_RADIUS) <= 1.0;

        Observation firstSample = samples[0];
        Observation lastSample = samples[samples.length - 1];

        return new TriangulationResult(
                firstSample.event(), firstSample.eventId(), firstSample.dimension(),
                firstSample.timestamp(), lastSample.timestamp(),
                samples.length, uniqueObservers.size(),
                wedgeHalfAngleSum / samples.length, maximumWedgeHalfAngle,
                proxyDistanceSum / samples.length, minimumProxyDistance, maximumProxyDistance,
                searchCenterX, searchCenterZ, radiusX, radiusZ,
                minX, maxX, minZ, maxZ,
                feasibleRegion.size(),
                touchesSearchBoundary,
                List.copyOf(feasibleRegion)
        );
    }

    private Wedge createGlobalEventProxyWedge(Observation observation, ProxyCoordinateMode proxyMode) {
        Vec2 player = new Vec2(observation.playerX(), observation.playerZ());

        double minProxyX = proxyIntervalMin(observation.packetX(), proxyMode);
        double minProxyZ = proxyIntervalMin(observation.packetZ(), proxyMode);
        double maxProxyX = proxyIntervalMax(observation.packetX(), proxyMode);
        double maxProxyZ = proxyIntervalMax(observation.packetZ(), proxyMode);

        Vec2[] proxyCorners = {
                new Vec2(minProxyX, minProxyZ),
                new Vec2(maxProxyX, minProxyZ),
                new Vec2(maxProxyX, maxProxyZ),
                new Vec2(minProxyX, maxProxyZ)
        };

        double[] angles = new double[proxyCorners.length];

        for (int i = 0; i < proxyCorners.length; i++) {
            double dx = proxyCorners[i].x() - player.x();
            double dz = proxyCorners[i].z() - player.z();

            if (Math.hypot(dx, dz) < EPSILON) return null;

            angles[i] = normalizeRadians(Math.atan2(dx, dz));
        }

        Arrays.sort(angles);

        double largestGap = -1.0;
        int largestGapIndex = -1;

        for (int i = 0; i < angles.length; i++) {
            double current = angles[i];
            double next = angles[(i + 1) % angles.length];
            double gap = i == angles.length - 1 ? next + Math.PI * 2.0 - current : next - current;

            if (gap > largestGap) {
                largestGap = gap;
                largestGapIndex = i;
            }
        }

        double lowerAngle = angles[(largestGapIndex + 1) % angles.length];
        double upperAngle = angles[largestGapIndex];

        if (upperAngle < lowerAngle) upperAngle += Math.PI * 2.0;

        double halfAngleRadians = (upperAngle - lowerAngle) * 0.5;
        double lowerDirectionX = Math.sin(lowerAngle);
        double lowerDirectionZ = Math.cos(lowerAngle);
        double upperDirectionX = Math.sin(upperAngle);
        double upperDirectionZ = Math.cos(upperAngle);
        double proxyCenterX = (minProxyX + maxProxyX) * 0.5;
        double proxyCenterZ = (minProxyZ + maxProxyZ) * 0.5;
        double centerDirectionX = proxyCenterX - player.x();
        double centerDirectionZ = proxyCenterZ - player.z();
        double centerDistance = Math.hypot(centerDirectionX, centerDirectionZ);

        if (centerDistance <= Math.sqrt(2.0)) return null;

        Vec2 knownInside = new Vec2(player.x() + centerDirectionX / centerDistance, player.z() + centerDirectionZ / centerDistance);
        List<HalfPlane> halfPlanes = new ArrayList<>(2);

        halfPlanes.add(createHalfPlaneContainingPoint(player, lowerDirectionX, lowerDirectionZ, knownInside));
        halfPlanes.add(createHalfPlaneContainingPoint(player, upperDirectionX, upperDirectionZ, knownInside));

        return new Wedge(lowerAngle, upperAngle, Math.toDegrees(halfAngleRadians), halfPlanes);
    }

    private double estimateProxyDistance(Observation observation, ProxyCoordinateMode proxyMode) {
        double minProxyX = proxyIntervalMin(observation.packetX(), proxyMode);
        double minProxyZ = proxyIntervalMin(observation.packetZ(), proxyMode);
        double maxProxyX = proxyIntervalMax(observation.packetX(), proxyMode);
        double maxProxyZ = proxyIntervalMax(observation.packetZ(), proxyMode);
        double proxyCenterX = (minProxyX + maxProxyX) * 0.5;
        double proxyCenterZ = (minProxyZ + maxProxyZ) * 0.5;

        return Math.hypot(proxyCenterX - observation.playerX(), proxyCenterZ - observation.playerZ());
    }

    private double proxyIntervalMin(int packetCoordinate, ProxyCoordinateMode proxyMode) {
        return switch (proxyMode) {
            case PAPER_CAST_TO_INT -> packetCoordinate >= 0 ? packetCoordinate : packetCoordinate - 1.0;
            case VANILLA_BLOCKPOS_CONTAINING -> packetCoordinate;
        };
    }

    private double proxyIntervalMax(int packetCoordinate, ProxyCoordinateMode proxyMode) {
        return switch (proxyMode) {
            case PAPER_CAST_TO_INT -> packetCoordinate >= 0 ? packetCoordinate + 1.0 : packetCoordinate;
            case VANILLA_BLOCKPOS_CONTAINING -> packetCoordinate + 1.0;
        };
    }

    private HalfPlane createHalfPlaneContainingPoint(Vec2 origin, double directionX, double directionZ, Vec2 knownInside) {
        double side = cross(directionX, directionZ, knownInside.x() - origin.x(), knownInside.z() - origin.z());

        if (side < 0.0) {
            directionX = -directionX;
            directionZ = -directionZ;
        }

        return new HalfPlane(origin, directionX, directionZ);
    }

    private List<Vec2> clipPolygon(List<Vec2> polygon, HalfPlane boundary) {
        List<Vec2> clipped = new ArrayList<>();

        if (polygon.isEmpty()) return clipped;

        Vec2 previous = polygon.get(polygon.size() - 1);
        boolean previousInside = boundary.contains(previous);

        for (Vec2 current : polygon) {
            boolean currentInside = boundary.contains(current);

            if (currentInside != previousInside) {
                Vec2 intersection = intersectSegmentWithBoundary(previous, current, boundary);
                if (intersection != null) clipped.add(intersection);
            }

            if (currentInside) clipped.add(current);

            previous = current;
            previousInside = currentInside;
        }

        return clipped;
    }

    private Vec2 intersectSegmentWithBoundary(Vec2 start, Vec2 end, HalfPlane boundary) {
        double startSide = cross(boundary.dirX(), boundary.dirZ(), start.x() - boundary.origin().x(), start.z() - boundary.origin().z());
        double endSide = cross(boundary.dirX(), boundary.dirZ(), end.x() - boundary.origin().x(), end.z() - boundary.origin().z());
        double denominator = startSide - endSide;
        double denominatorTolerance = Math.max(EPSILON, 64.0 * Math.ulp(Math.max(1.0, Math.max(Math.abs(startSide), Math.abs(endSide)))));

        if (Math.abs(denominator) <= denominatorTolerance) return null;

        double t = startSide / denominator;

        if (t < -EPSILON || t > 1.0 + EPSILON) return null;

        t = Math.max(0.0, Math.min(1.0, t));

        return new Vec2(start.x() + t * (end.x() - start.x()), start.z() + t * (end.z() - start.z()));
    }

    private double signedAreaTwice(List<Vec2> polygon) {
        double twiceArea = 0.0;

        for (int i = 0; i < polygon.size(); i++) {
            Vec2 current = polygon.get(i);
            Vec2 next = polygon.get((i + 1) % polygon.size());
            twiceArea += current.x() * next.z() - next.x() * current.z();
        }

        return twiceArea;
    }

    private static double cross(double firstX, double firstZ, double secondX, double secondZ) {
        return firstX * secondZ - firstZ * secondX;
    }

    private double normalizeRadians(double radians) {
        double normalized = radians % (Math.PI * 2.0);
        if (normalized < 0.0) normalized += Math.PI * 2.0;

        return normalized;
    }
}