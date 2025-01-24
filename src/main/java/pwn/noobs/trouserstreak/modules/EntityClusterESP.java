/**
 * ItemESP
 * =======
 *  - Written by Nataani
 *  - Pulled from the Meteorite module.
 *  This module highlights the center-point of entity clusters.
 *  You can configure a list of entities to consider to highlight,
 *  customize the box appearance (fill opacity, outline, etc.),
 *  and designate a range for the module to consider in its calculations.
 *  The render box method was adapted from Etlian's ActivatedSpawnerDetector.
 *  The list GUI and csv methods were adapted from Meteor's Stashfinder module.g
 *  This module is particularly useful for identifying stacked storage vehicles like Minecart with Chest.
 */

package pwn.noobs.trouserstreak.modules;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.MeteorToast;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import pwn.noobs.trouserstreak.Trouser;

import java.io.*;
import java.util.*;

public class EntityClusterESP extends Module {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Set<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Select specific entities.")
            .defaultValue(EntityType.CHEST_MINECART)
            .build()
    );

    private final Setting<Boolean> displayCoords = sgGeneral.add(new BoolSetting.Builder()
            .name("display-coords")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> notifications = sgGeneral.add(new BoolSetting.Builder()
            .name("notifications")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> minEntities = sgGeneral.add(new IntSetting.Builder()
            .name("number-of-entities")
            .defaultValue(5)
            .min(2)
            .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("block-range")
            .defaultValue(10.0)
            .min(1)
            .sliderMax(32)
            .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .defaultValue(ShapeMode.Both)
            .build()
    );

    private final Setting<SettingColor> sideColorSetting = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .defaultValue(new SettingColor(0, 255, 255, 50))
            .build()
    );

    private final Setting<SettingColor> lineColorSetting = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .defaultValue(new SettingColor(0, 255, 255, 255))
            .build()
    );

    private final List<Cluster> clusters = new ArrayList<>();
    private final Set<BlockPos> knownCenters = new HashSet<>();

    public EntityClusterESP() {
        super(Trouser.Main, "entity-cluster-ESP", "Highlights and logs clusters of chosen entities.");
    }

    @Override
    public void onActivate() {
        clusters.clear();
        knownCenters.clear();
        load();
    }

    @Override
    public void onDeactivate() {
        clusters.clear();
        knownCenters.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.world == null || mc.player == null) return;

        List<Entity> chosen = new ArrayList<>();
        for (Entity e : mc.world.getEntities()) {
            if (entities.get().contains(e.getType())) {
                chosen.add(e);
            }
        }

        if (!chosen.isEmpty()) {
            checkForClusters(chosen);
        }
    }


    @EventHandler
    private void onRender(Render3DEvent event) {
        Color sideColor = sideColorSetting.get();
        Color lineColor = lineColorSetting.get();
        double half = range.get() / 2.0;
        for (Cluster c : clusters) {
            if (!knownCenters.contains(c.centerPos)) continue;
            double x1 = c.centerPos.getX() - half;
            double y1 = c.centerPos.getY() - half;
            double z1 = c.centerPos.getZ() - half;
            double x2 = c.centerPos.getX() + half;
            double y2 = c.centerPos.getY() + half;
            double z2 = c.centerPos.getZ() + half;
            event.renderer.box(x1, y1, z1, x2, y2, z2, sideColor, lineColor, shapeMode.get(), 0);
        }
    }

    private void checkForClusters(List<Entity> vehicles) {
        double r = range.get();
        int needed = minEntities.get();
        for (int i = 0; i < vehicles.size(); i++) {
            Entity primary = vehicles.get(i);
            List<Vec3d> cluster = new ArrayList<>();
            cluster.add(primary.getPos());
            for (int j = 0; j < vehicles.size(); j++) {
                if (j == i) continue;
                Entity other = vehicles.get(j);
                if (primary.squaredDistanceTo(other) <= r * r) {
                    cluster.add(other.getPos());
                }
            }
            if (cluster.size() >= needed) {
                Vec3d center = averagePos(cluster);
                int cx = (int) center.x;
                int cy = (int) center.y;
                int cz = (int) center.z;
                BlockPos centerPos = new BlockPos(cx, cy, cz);
                if (!knownCenters.contains(centerPos)) {
                    knownCenters.add(centerPos);
                    int size = cluster.size();
                    clusters.add(new Cluster(cx, cy, cz, size));
                    if (notifications.get()) {
                        mc.getToastManager().add(new MeteorToast(Items.CHEST_MINECART, "EntityClusterESP", "Cluster of " + size + " @ " + centerPos));
                        ChatUtils.sendMsg(Text.of("Entity cluster of " + size + " found at " + centerPos));
                    }
                    if (displayCoords.get()) {
                        ChatUtils.info("EntityClusterESP", "Cluster of " + size + " found at " + centerPos);
                    }
                    saveCsv();
                    saveJson();
                }
            }
        }
    }

    private Vec3d averagePos(List<Vec3d> positions) {
        double sx = 0, sy = 0, sz = 0;
        for (Vec3d p : positions) {
            sx += p.x;
            sy += p.y;
            sz += p.z;
        }
        return new Vec3d(sx / positions.size(), sy / positions.size(), sz / positions.size());
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        clusters.sort(Comparator.comparingInt(a -> a.y));
        WVerticalList list = theme.verticalList();
        WButton clear = list.add(theme.button("Clear")).widget();
        WTable table = new WTable();
        if (!clusters.isEmpty()) list.add(table);
        clear.action = () -> {
            clusters.clear();
            knownCenters.clear();
            table.clear();
            saveJson();
            saveCsv();
        };
        fillTable(theme, table);
        return list;
    }

    private void fillTable(GuiTheme theme, WTable table) {
        for (Cluster c : clusters) {
            table.add(theme.label("Pos: " + c.x + ", " + c.y + ", " + c.z + " (Cnt: " + c.count + ")"));
            WButton gotoBtn = table.add(theme.button("Goto")).widget();
            gotoBtn.action = () -> {
                PathManagers.get().moveTo(new BlockPos(c.x, c.y, c.z), true);
            };
            WMinus delete = table.add(theme.minus()).widget();
            delete.action = () -> {
                clusters.remove(c);
                knownCenters.remove(c.centerPos);
                table.clear();
                fillTable(theme, table);
                saveJson();
                saveCsv();
            };
            table.row();
        }
    }

    private static class Cluster {
        public int x, y, z, count;
        public BlockPos centerPos;
        public Cluster(int x, int y, int z, int count) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.count = count;
            this.centerPos = new BlockPos(x, y, z);
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Cluster cluster = (Cluster) o;
            return x == cluster.x && y == cluster.y && z == cluster.z;
        }
        @Override
        public int hashCode() {
            return Objects.hash(x, y, z);
        }
    }

    private File getCsvFile() {
        return new File(new File(new File(MeteorClient.FOLDER, "EntityClusterESP"), Utils.getFileWorldName()), "EntityClusterESP.csv");
    }

    private File getJsonFile() {
        return new File(new File(new File(MeteorClient.FOLDER, "EntityClusterESP"), Utils.getFileWorldName()), "EntityClusterESP.json");
    }

    private void load() {
        File file = getJsonFile();
        if (file.exists()) {
            try {
                FileReader reader = new FileReader(file);
                List<Cluster> data = GSON.fromJson(reader, new TypeToken<List<Cluster>>(){}.getType());
                reader.close();
                if (data != null) {
                    clusters.addAll(data);
                    for (Cluster c : data) {
                        knownCenters.add(c.centerPos);
                    }
                }
            } catch (Exception ignored) {}
        } else {
            file = getCsvFile();
            if (file.exists()) {
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(file));
                    reader.readLine();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] values = line.split(",");
                        Cluster cluster = new Cluster(
                                Integer.parseInt(values[0]),
                                Integer.parseInt(values[1]),
                                Integer.parseInt(values[2]),
                                Integer.parseInt(values[3])
                        );
                        clusters.add(cluster);
                        knownCenters.add(cluster.centerPos);
                    }
                    reader.close();
                } catch (Exception ignored) {}
            }
        }
    }

    private void saveCsv() {
        try {
            File file = getCsvFile();
            file.getParentFile().mkdirs();
            Writer writer = new FileWriter(file);
            writer.write("X,Y,Z,Count\n");
            for (Cluster c : clusters) {
                writer.write(c.x + "," + c.y + "," + c.z + "," + c.count + "\n");
            }
            writer.close();
        } catch (IOException ignored) {}
    }

    private void saveJson() {
        try {
            File file = getJsonFile();
            file.getParentFile().mkdirs();
            Writer writer = new FileWriter(file);
            GSON.toJson(clusters, writer);
            writer.close();
        } catch (IOException ignored) {}
    }
}


