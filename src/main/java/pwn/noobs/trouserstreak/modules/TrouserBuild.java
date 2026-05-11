//Written By etianll, using just a bit of code from banana's autobuild. Thanks to them for the idea and framework for the codes
package pwn.noobs.trouserstreak.modules;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.item.ScaffoldingBlockItem;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.BaseCoralFanBlock;
import net.minecraft.world.level.block.BaseCoralWallFanBlock;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BuddingAmethystBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.block.ChorusPlantBlock;
import net.minecraft.world.level.block.ConduitBlock;
import net.minecraft.world.level.block.CoralFanBlock;
import net.minecraft.world.level.block.CoralWallFanBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.GlowLichenBlock;
import net.minecraft.world.level.block.KelpBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.SporeBlossomBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import pwn.noobs.trouserstreak.Trouser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class TrouserBuild extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<List<Block>> skippableBlox = sgGeneral.add(new BlockListSetting.Builder()
            .name("Blocks to not use")
            .description("Do not use these blocks for building.")
            .build()
    );
    private final Setting<Boolean> orientation = sgGeneral.add(new BoolSetting.Builder()
            .name("AutoOrientation")
            .description("Automatically chooses whether to build upright or horizontal.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Modes> mode = sgGeneral.add(new EnumSetting.Builder<Modes>()
            .name("mode")
            .description("the mode")
            .defaultValue(Modes.Vertical)
            .visible(() -> !orientation.get())
            .build());
    private final Setting<Integer> reach = sgGeneral.add(new IntSetting.Builder()
            .name("Range")
            .description("Your Range, in blocks. Do not increase past max. Turn it down if not using the Reach module.")
            .defaultValue(3)
            .sliderRange(1,5)
            .min (1)
            .max (5)
            .build()
    );
    private final Setting<Integer> thislong = sgGeneral.add(new IntSetting.Builder()
            .name("AttemptPlacementLoops")
            .description("Loops placement at the unoccupied block positions this many times to ensure all blocks are placed serverside.")
            .defaultValue(3)
            .sliderRange(1, 10)
            .build()
    );
    private final Setting<Integer> tickdelay = sgGeneral.add(new IntSetting.Builder()
            .name("TickDelayPerRow")
            .description("Delays placement by this many ticks per row from top to bottom.")
            .defaultValue(1)
            .sliderRange(0, 10)
            .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
            .name("render")
            .description("Renders a block overlay where the center of the build will be.")
            .defaultValue(true)
            .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .visible(render::get)
            .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The color of the sides of the blocks being rendered.")
            .defaultValue(new SettingColor(255, 0, 255, 15))
            .visible(render::get)
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The color of the lines of the blocks being rendered.")
            .defaultValue(new SettingColor(255, 0, 255, 255))
            .visible(render::get)
            .build()
    );

    public TrouserBuild() {
        super(Trouser.Main, "TrouserBuild", "Can build either horizontally and vertically according to a 5x5 grid centered on the block you are aiming at.");
        configPath = Paths.get("TrouserStreak").resolve("trouserstreak-grid.json");
        loadGridConfig();
    }
    boolean ett = false, tva = false, tree = false, fyra = false, fem = false;
    boolean ett1 = false,tva1 = true, tree1 = true, fyra1 = true, fem1 = false;
    boolean ett2 = false, tva2 = false, tree2 = true, fyra2 = false, fem2 = false;
    boolean ett3 = false, tva3 = false, tree3 = true, fyra3 = false, fem3 = false;
    boolean ett4 = false, tva4 = false, tree4 = false, fyra4 = false, fem4 = false;
    private Path configPath;
    private JsonObject gridConfig = new JsonObject();
    private boolean pause = true;
    private BlockPos lava;
    private Direction playerdir;
    private float playerpitch;
    private int loops=0;
    private int blockticks=0;
    private void saveGridConfig() {
        try {
            Files.createDirectories(configPath.getParent());

            gridConfig.addProperty("ett", ett);
            gridConfig.addProperty("tva", tva);
            gridConfig.addProperty("tree", tree);
            gridConfig.addProperty("fyra", fyra);
            gridConfig.addProperty("fem", fem);

            gridConfig.addProperty("ett1", ett1);
            gridConfig.addProperty("tva1", tva1);
            gridConfig.addProperty("tree1", tree1);
            gridConfig.addProperty("fyra1", fyra1);
            gridConfig.addProperty("fem1", fem1);

            gridConfig.addProperty("ett2", ett2);
            gridConfig.addProperty("tva2", tva2);
            gridConfig.addProperty("tree2", tree2);
            gridConfig.addProperty("fyra2", fyra2);
            gridConfig.addProperty("fem2", fem2);

            gridConfig.addProperty("ett3", ett3);
            gridConfig.addProperty("tva3", tva3);
            gridConfig.addProperty("tree3", tree3);
            gridConfig.addProperty("fyra3", fyra3);
            gridConfig.addProperty("fem3", fem3);

            gridConfig.addProperty("ett4", ett4);
            gridConfig.addProperty("tva4", tva4);
            gridConfig.addProperty("tree4", tree4);
            gridConfig.addProperty("fyra4", fyra4);
            gridConfig.addProperty("fem4", fem4);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonString = gson.toJson(gridConfig);

            Files.write(configPath, jsonString.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void loadGridConfig() {
        try {
            if (Files.exists(configPath)) {
                String jsonString = Files.readString(configPath);
                gridConfig = new Gson().fromJson(jsonString, JsonObject.class);

                ett = gridConfig.get("ett").getAsBoolean();
                tva = gridConfig.get("tva").getAsBoolean();
                tree = gridConfig.get("tree").getAsBoolean();
                fyra = gridConfig.get("fyra").getAsBoolean();
                fem = gridConfig.get("fem").getAsBoolean();

                ett1 = gridConfig.get("ett1").getAsBoolean();
                tva1 = gridConfig.get("tva1").getAsBoolean();
                tree1 = gridConfig.get("tree1").getAsBoolean();
                fyra1 = gridConfig.get("fyra1").getAsBoolean();
                fem1 = gridConfig.get("fem1").getAsBoolean();

                ett2 = gridConfig.get("ett2").getAsBoolean();
                tva2 = gridConfig.get("tva2").getAsBoolean();
                tree2 = gridConfig.get("tree2").getAsBoolean();
                fyra2 = gridConfig.get("fyra2").getAsBoolean();
                fem2 = gridConfig.get("fem2").getAsBoolean();

                ett3 = gridConfig.get("ett3").getAsBoolean();
                tva3 = gridConfig.get("tva3").getAsBoolean();
                tree3 = gridConfig.get("tree3").getAsBoolean();
                fyra3 = gridConfig.get("fyra3").getAsBoolean();
                fem3 = gridConfig.get("fem3").getAsBoolean();

                ett4 = gridConfig.get("ett4").getAsBoolean();
                tva4 = gridConfig.get("tva4").getAsBoolean();
                tree4 = gridConfig.get("tree4").getAsBoolean();
                fyra4 = gridConfig.get("fyra4").getAsBoolean();
                fem4 = gridConfig.get("fem4").getAsBoolean();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();
        WTable table = theme.table();
        list.add(table);

        WCheckbox one = table.add(theme.checkbox(ett)).widget();
        one.action = () -> ett = one.checked;
        WCheckbox two = table.add(theme.checkbox(tva)).widget();
        two.action = () -> tva = two.checked;
        WCheckbox three = table.add(theme.checkbox(tree)).widget();
        three.action = () -> tree = three.checked;
        WCheckbox four = table.add(theme.checkbox(fyra)).widget();
        four.action = () -> fyra = four.checked;
        WCheckbox five = table.add(theme.checkbox(fem)).widget();
        five.action = () -> fem = five.checked;
        table.row();

        WCheckbox one1 = table.add(theme.checkbox(ett1)).widget();
        one1.action = () -> ett1 = one1.checked;
        WCheckbox two1 = table.add(theme.checkbox(tva1)).widget();
        two1.action = () -> tva1 = two1.checked;
        WCheckbox three1 = table.add(theme.checkbox(tree1)).widget();
        three1.action = () -> tree1 = three1.checked;
        WCheckbox four1 = table.add(theme.checkbox(fyra1)).widget();
        four1.action = () -> fyra1 = four1.checked;
        WCheckbox five1 = table.add(theme.checkbox(fem1)).widget();
        five1.action = () -> fem1 = five1.checked;
        table.row();

        WCheckbox one2 = table.add(theme.checkbox(ett2)).widget();
        one2.action = () -> ett2 = one2.checked;
        WCheckbox two2 = table.add(theme.checkbox(tva2)).widget();
        two2.action = () -> tva2 = two2.checked;
        WCheckbox three2 = table.add(theme.checkbox(tree2)).widget();
        three2.action = () -> tree2 = three2.checked;
        WCheckbox four2 = table.add(theme.checkbox(fyra2)).widget();
        four2.action = () -> fyra2 = four2.checked;
        WCheckbox five2 = table.add(theme.checkbox(fem2)).widget();
        five2.action = () -> fem2 = five2.checked;
        table.row();

        WCheckbox one3 = table.add(theme.checkbox(ett3)).widget();
        one3.action = () -> ett3 = one3.checked;
        WCheckbox two3 = table.add(theme.checkbox(tva3)).widget();
        two3.action = () -> tva3 = two3.checked;
        WCheckbox three3 = table.add(theme.checkbox(tree3)).widget();
        three3.action = () -> tree3 = three3.checked;
        WCheckbox four3 = table.add(theme.checkbox(fyra3)).widget();
        four3.action = () -> fyra3 = four3.checked;
        WCheckbox five3 = table.add(theme.checkbox(fem3)).widget();
        five3.action = () -> fem3 = five3.checked;
        table.row();

        WCheckbox one4 = table.add(theme.checkbox(ett4)).widget();
        one4.action = () -> ett4 = one4.checked;
        WCheckbox two4 = table.add(theme.checkbox(tva4)).widget();
        two4.action = () -> tva4 = two4.checked;
        WCheckbox three4 = table.add(theme.checkbox(tree4)).widget();
        three4.action = () -> tree4 = three4.checked;
        WCheckbox four4 = table.add(theme.checkbox(fyra4)).widget();
        four4.action = () -> fyra4 = four4.checked;
        WCheckbox five4 = table.add(theme.checkbox(fem4)).widget();
        five4.action = () -> fem4 = five4.checked;
        table.row();

        return list;
    }

    @Override
    public void onActivate() {
        error("Press useKey to build!");
    }
    @Override
    public void onDeactivate() {
        saveGridConfig();
        pause=true;
        blockticks=0;
        loops=0;
    }
    @EventHandler
    private void onRender(Render3DEvent event) {
        if (lava == null) return;
        double x1 = lava.getX();
        double y1 = lava.getY();
        double z1 = lava.getZ();
        double x2 = x1+1;
        double y2 = y1+1;
        double z2 = z1+1;


        event.renderer.box(x1, y1, z1, x2, y2, z2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }
    @EventHandler
    private void onMouseButton(MouseClickEvent event) {
        if (mc.player == null) return;
        if (mc.options.keyUse.isDown()){
            lava = cast();
            playerdir=mc.player.getDirection();
            playerpitch=mc.player.getXRot();
            pause = !pause;
        }
    }
    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (pause) {
            lava=cast();
            blockticks=0;
            loops=0;
        }
        if (!pause){
            blockticks++;
            if (blockticks>tickdelay.get()*5){
                blockticks=0;
                loops++;
            }
            if (loops>=thislong.get()){
                pause=true;
                loops=0;
            }
        }
        if (((orientation.get() && playerpitch>40 | playerpitch<-40) || (mode.get() == Modes.Horizontal && !orientation.get())) && !pause){
            cascadingpileof();
            //north
            BlockPos Nettpos = new BlockPos(lava.getX()-2,lava.getY(),lava.getZ()-2);
            BlockPos Nett1pos = new BlockPos(lava.getX()-2,lava.getY(),lava.getZ()-1);
            BlockPos Nett2pos = new BlockPos(lava.getX()-2,lava.getY(),lava.getZ());
            BlockPos Nett3pos = new BlockPos(lava.getX()-2,lava.getY(),lava.getZ()+1);
            BlockPos Nett4pos = new BlockPos(lava.getX()-2,lava.getY(),lava.getZ()+2);
            BlockPos Ntvapos = new BlockPos(lava.getX()-1,lava.getY(),lava.getZ()-2);
            BlockPos Ntva1pos = new BlockPos(lava.getX()-1,lava.getY(),lava.getZ()-1);
            BlockPos Ntva2pos = new BlockPos(lava.getX()-1,lava.getY(),lava.getZ());
            BlockPos Ntva3pos = new BlockPos(lava.getX()-1,lava.getY(),lava.getZ()+1);
            BlockPos Ntva4pos = new BlockPos(lava.getX()-1,lava.getY(),lava.getZ()+2);
            BlockPos Ntreepos = new BlockPos(lava.getX(),lava.getY(),lava.getZ()-2);
            BlockPos Ntree1pos = new BlockPos(lava.getX(),lava.getY(),lava.getZ()-1);
            BlockPos Ntree2pos = new BlockPos(lava.getX(),lava.getY(),lava.getZ());
            BlockPos Ntree3pos = new BlockPos(lava.getX(),lava.getY(),lava.getZ()+1);
            BlockPos Ntree4pos = new BlockPos(lava.getX(),lava.getY(),lava.getZ()+2);
            BlockPos Nfyrapos = new BlockPos(lava.getX()+1,lava.getY(),lava.getZ()-2);
            BlockPos Nfyra1pos = new BlockPos(lava.getX()+1,lava.getY(),lava.getZ()-1);
            BlockPos Nfyra2pos = new BlockPos(lava.getX()+1,lava.getY(),lava.getZ());
            BlockPos Nfyra3pos = new BlockPos(lava.getX()+1,lava.getY(),lava.getZ()+1);
            BlockPos Nfyra4pos = new BlockPos(lava.getX()+1,lava.getY(),lava.getZ()+2);
            BlockPos Nfempos = new BlockPos(lava.getX()+2,lava.getY(),lava.getZ()-2);
            BlockPos Nfem1pos = new BlockPos(lava.getX()+2,lava.getY(),lava.getZ()-1);
            BlockPos Nfem2pos = new BlockPos(lava.getX()+2,lava.getY(),lava.getZ());
            BlockPos Nfem3pos = new BlockPos(lava.getX()+2,lava.getY(),lava.getZ()+1);
            BlockPos Nfem4pos = new BlockPos(lava.getX()+2,lava.getY(),lava.getZ()+2);
            //south
            BlockPos Settpos = new BlockPos(lava.getX()+2,lava.getY(),lava.getZ()+2);
            BlockPos Sett1pos = new BlockPos(lava.getX()+2,lava.getY(),lava.getZ()+1);
            BlockPos Sett2pos = new BlockPos(lava.getX()+2,lava.getY(),lava.getZ());
            BlockPos Sett3pos = new BlockPos(lava.getX()+2,lava.getY(),lava.getZ()-1);
            BlockPos Sett4pos = new BlockPos(lava.getX()+2,lava.getY(),lava.getZ()-2);
            BlockPos Stvapos = new BlockPos(lava.getX()+1,lava.getY(),lava.getZ()+2);
            BlockPos Stva1pos = new BlockPos(lava.getX()+1,lava.getY(),lava.getZ()+1);
            BlockPos Stva2pos = new BlockPos(lava.getX()+1,lava.getY(),lava.getZ());
            BlockPos Stva3pos = new BlockPos(lava.getX()+1,lava.getY(),lava.getZ()-1);
            BlockPos Stva4pos = new BlockPos(lava.getX()+1,lava.getY(),lava.getZ()-2);
            BlockPos Streepos = new BlockPos(lava.getX(),lava.getY(),lava.getZ()+2);
            BlockPos Stree1pos = new BlockPos(lava.getX(),lava.getY(),lava.getZ()+1);
            BlockPos Stree2pos = new BlockPos(lava.getX(),lava.getY(),lava.getZ());
            BlockPos Stree3pos = new BlockPos(lava.getX(),lava.getY(),lava.getZ()-1);
            BlockPos Stree4pos = new BlockPos(lava.getX(),lava.getY(),lava.getZ()-2);
            BlockPos Sfyrapos = new BlockPos(lava.getX()-1,lava.getY(),lava.getZ()+2);
            BlockPos Sfyra1pos = new BlockPos(lava.getX()-1,lava.getY(),lava.getZ()+1);
            BlockPos Sfyra2pos = new BlockPos(lava.getX()-1,lava.getY(),lava.getZ());
            BlockPos Sfyra3pos = new BlockPos(lava.getX()-1,lava.getY(),lava.getZ()-1);
            BlockPos Sfyra4pos = new BlockPos(lava.getX()-1,lava.getY(),lava.getZ()-2);
            BlockPos Sfempos = new BlockPos(lava.getX()-2,lava.getY(),lava.getZ()+2);
            BlockPos Sfem1pos = new BlockPos(lava.getX()-2,lava.getY(),lava.getZ()+1);
            BlockPos Sfem2pos = new BlockPos(lava.getX()-2,lava.getY(),lava.getZ());
            BlockPos Sfem3pos = new BlockPos(lava.getX()-2,lava.getY(),lava.getZ()-1);
            BlockPos Sfem4pos = new BlockPos(lava.getX()-2,lava.getY(),lava.getZ()-2);
            //east
            BlockPos Eettpos = new BlockPos(lava.getX()+2,lava.getY(),lava.getZ()-2);
            BlockPos Eett1pos = new BlockPos(lava.getX()+1,lava.getY(),lava.getZ()-2);
            BlockPos Eett2pos = new BlockPos(lava.getX(),lava.getY(),lava.getZ()-2);
            BlockPos Eett3pos = new BlockPos(lava.getX()-1,lava.getY(),lava.getZ()-2);
            BlockPos Eett4pos = new BlockPos(lava.getX()-2,lava.getY(),lava.getZ()-2);
            BlockPos Etvapos = new BlockPos(lava.getX()+2,lava.getY(),lava.getZ()-1);
            BlockPos Etva1pos = new BlockPos(lava.getX()+1,lava.getY(),lava.getZ()-1);
            BlockPos Etva2pos = new BlockPos(lava.getX(),lava.getY(),lava.getZ()-1);
            BlockPos Etva3pos = new BlockPos(lava.getX()-1,lava.getY(),lava.getZ()-1);
            BlockPos Etva4pos = new BlockPos(lava.getX()-2,lava.getY(),lava.getZ()-1);
            BlockPos Etreepos = new BlockPos(lava.getX()+2,lava.getY(),lava.getZ());
            BlockPos Etree1pos = new BlockPos(lava.getX()+1,lava.getY(),lava.getZ());
            BlockPos Etree2pos = new BlockPos(lava.getX(),lava.getY(),lava.getZ());
            BlockPos Etree3pos = new BlockPos(lava.getX()-1,lava.getY(),lava.getZ());
            BlockPos Etree4pos = new BlockPos(lava.getX()-2,lava.getY(),lava.getZ());
            BlockPos Efyrapos = new BlockPos(lava.getX()+2,lava.getY(),lava.getZ()+1);
            BlockPos Efyra1pos = new BlockPos(lava.getX()+1,lava.getY(),lava.getZ()+1);
            BlockPos Efyra2pos = new BlockPos(lava.getX(),lava.getY(),lava.getZ()+1);
            BlockPos Efyra3pos = new BlockPos(lava.getX()-1,lava.getY(),lava.getZ()+1);
            BlockPos Efyra4pos = new BlockPos(lava.getX()-2,lava.getY(),lava.getZ()+1);
            BlockPos Efempos = new BlockPos(lava.getX()+2,lava.getY(),lava.getZ()+2);
            BlockPos Efem1pos = new BlockPos(lava.getX()+1,lava.getY(),lava.getZ()+2);
            BlockPos Efem2pos = new BlockPos(lava.getX(),lava.getY(),lava.getZ()+2);
            BlockPos Efem3pos = new BlockPos(lava.getX()-1,lava.getY(),lava.getZ()+2);
            BlockPos Efem4pos = new BlockPos(lava.getX()-2,lava.getY(),lava.getZ()+2);
            //west
            BlockPos Wettpos = new BlockPos(lava.getX()-2,lava.getY(),lava.getZ()+2);
            BlockPos Wett1pos = new BlockPos(lava.getX()-1,lava.getY(),lava.getZ()+2);
            BlockPos Wett2pos = new BlockPos(lava.getX(),lava.getY(),lava.getZ()+2);
            BlockPos Wett3pos = new BlockPos(lava.getX()+1,lava.getY(),lava.getZ()+2);
            BlockPos Wett4pos = new BlockPos(lava.getX()+2,lava.getY(),lava.getZ()+2);
            BlockPos Wtvapos = new BlockPos(lava.getX()-2,lava.getY(),lava.getZ()+1);
            BlockPos Wtva1pos = new BlockPos(lava.getX()-1,lava.getY(),lava.getZ()+1);
            BlockPos Wtva2pos = new BlockPos(lava.getX(),lava.getY(),lava.getZ()+1);
            BlockPos Wtva3pos = new BlockPos(lava.getX()+1,lava.getY(),lava.getZ()+1);
            BlockPos Wtva4pos = new BlockPos(lava.getX()+2,lava.getY(),lava.getZ()+1);
            BlockPos Wtreepos = new BlockPos(lava.getX()-2,lava.getY(),lava.getZ());
            BlockPos Wtree1pos = new BlockPos(lava.getX()-1,lava.getY(),lava.getZ());
            BlockPos Wtree2pos = new BlockPos(lava.getX(),lava.getY(),lava.getZ());
            BlockPos Wtree3pos = new BlockPos(lava.getX()+1,lava.getY(),lava.getZ());
            BlockPos Wtree4pos = new BlockPos(lava.getX()+2,lava.getY(),lava.getZ());
            BlockPos Wfyrapos = new BlockPos(lava.getX()-2,lava.getY(),lava.getZ()-1);
            BlockPos Wfyra1pos = new BlockPos(lava.getX()-1,lava.getY(),lava.getZ()-1);
            BlockPos Wfyra2pos = new BlockPos(lava.getX(),lava.getY(),lava.getZ()-1);
            BlockPos Wfyra3pos = new BlockPos(lava.getX()+1,lava.getY(),lava.getZ()-1);
            BlockPos Wfyra4pos = new BlockPos(lava.getX()+2,lava.getY(),lava.getZ()-1);
            BlockPos Wfempos = new BlockPos(lava.getX()-2,lava.getY(),lava.getZ()-2);
            BlockPos Wfem1pos = new BlockPos(lava.getX()-1,lava.getY(),lava.getZ()-2);
            BlockPos Wfem2pos = new BlockPos(lava.getX(),lava.getY(),lava.getZ()-2);
            BlockPos Wfem3pos = new BlockPos(lava.getX()+1,lava.getY(),lava.getZ()-2);
            BlockPos Wfem4pos = new BlockPos(lava.getX()+2,lava.getY(),lava.getZ()-2);

            if (playerdir == Direction.NORTH){
                //1st row
                if (ett){
                    if (blockticks== tickdelay.get() && mc.level.getBlockState(Nettpos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Nettpos), Direction.DOWN, Nettpos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva){
                    if (blockticks== tickdelay.get() && mc.level.getBlockState(Ntvapos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Ntvapos), Direction.DOWN, Ntvapos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree){
                    if (blockticks== tickdelay.get() && mc.level.getBlockState(Ntreepos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Ntreepos), Direction.DOWN, Ntreepos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra){
                    if (blockticks== tickdelay.get() && mc.level.getBlockState(Nfyrapos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Nfyrapos), Direction.DOWN, Nfyrapos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem){
                    if (blockticks== tickdelay.get() && mc.level.getBlockState(Nfempos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Nfempos), Direction.DOWN, Nfempos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }

                //2nd row
                if (ett1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Nett1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Nett1pos), Direction.DOWN, Nett1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Ntva1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Ntva1pos), Direction.DOWN, Ntva1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Ntree1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Ntree1pos), Direction.DOWN, Ntree1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Nfyra1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Nfyra1pos), Direction.DOWN, Nfyra1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Nfem1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Nfem1pos), Direction.DOWN, Nfem1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }

                //3rd row
                if (ett2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Nett2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Nett2pos), Direction.DOWN, Nett2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Ntva2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Ntva2pos), Direction.DOWN, Ntva2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Ntree2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Ntree2pos), Direction.DOWN, Ntree2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Nfyra2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Nfyra2pos), Direction.DOWN, Nfyra2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Nfem2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Nfem2pos), Direction.DOWN, Nfem2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }

                //4th row
                if (ett3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Nett3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Nett3pos), Direction.DOWN, Nett3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Ntva3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Ntva3pos), Direction.DOWN, Ntva3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Ntree3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Ntree3pos), Direction.DOWN, Ntree3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Nfyra3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Nfyra3pos), Direction.DOWN, Nfyra3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Nfem3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Nfem3pos), Direction.DOWN, Nfem3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }

                //5th row
                if (ett4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Nett4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Nett4pos), Direction.DOWN, Nett4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Ntva4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Ntva4pos), Direction.DOWN, Ntva4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Ntree4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Ntree4pos), Direction.DOWN, Ntree4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Nfyra4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Nfyra4pos), Direction.DOWN, Nfyra4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Nfem4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Nfem4pos), Direction.DOWN, Nfem4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
            }

            if (playerdir == Direction.SOUTH){
                //1st row
                if (ett){
                    if (blockticks==tickdelay.get() && mc.level.getBlockState(Settpos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Settpos), Direction.DOWN, Settpos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva){
                    if (blockticks==tickdelay.get() && mc.level.getBlockState(Stvapos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Stvapos), Direction.DOWN, Stvapos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree){
                    if (blockticks==tickdelay.get() && mc.level.getBlockState(Streepos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Streepos), Direction.DOWN, Streepos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra){
                    if (blockticks==tickdelay.get() && mc.level.getBlockState(Sfyrapos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Sfyrapos), Direction.DOWN, Sfyrapos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem){
                    if (blockticks==tickdelay.get() && mc.level.getBlockState(Sfempos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Sfempos), Direction.DOWN, Sfempos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }

                //2nd row
                if (ett1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Sett1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Sett1pos), Direction.DOWN, Sett1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Stva1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Stva1pos), Direction.DOWN, Stva1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Stree1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Stree1pos), Direction.DOWN, Stree1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Sfyra1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Sfyra1pos), Direction.DOWN, Sfyra1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Sfem1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Sfem1pos), Direction.DOWN, Sfem1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }

                //3rd row
                if (ett2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Sett2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Sett2pos), Direction.DOWN, Sett2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Stva2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Stva2pos), Direction.DOWN, Stva2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Stree2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Stree2pos), Direction.DOWN, Stree2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Sfyra2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Sfyra2pos), Direction.DOWN, Sfyra2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Sfem2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Sfem2pos), Direction.DOWN, Sfem2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }

                //4th row
                if (ett3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Sett3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Sett3pos), Direction.DOWN, Sett3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Stva3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Stva3pos), Direction.DOWN, Stva3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Stree3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Stree3pos), Direction.DOWN, Stree3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Sfyra3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Sfyra3pos), Direction.DOWN, Sfyra3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Sfem3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Sfem3pos), Direction.DOWN, Sfem3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }

                //5th row
                if (ett4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Sett4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Sett4pos), Direction.DOWN, Sett4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Stva4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Stva4pos), Direction.DOWN, Stva4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Stree4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Stree4pos), Direction.DOWN, Stree4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Sfyra4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Sfyra4pos), Direction.DOWN, Sfyra4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Sfem4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Sfem4pos), Direction.DOWN, Sfem4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
            }

            if (playerdir == Direction.EAST){
                //1st row
                if (ett){
                    if (blockticks==tickdelay.get() && mc.level.getBlockState(Eettpos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Eettpos), Direction.DOWN, Eettpos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva){
                    if (blockticks==tickdelay.get() && mc.level.getBlockState(Etvapos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Etvapos), Direction.DOWN, Etvapos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree){
                    if (blockticks==tickdelay.get() && mc.level.getBlockState(Etreepos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Etreepos), Direction.DOWN, Etreepos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra){
                    if (blockticks==tickdelay.get() && mc.level.getBlockState(Efyrapos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Efyrapos), Direction.DOWN, Efyrapos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem){
                    if (blockticks==tickdelay.get() && mc.level.getBlockState(Efempos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Efempos), Direction.DOWN, Efempos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }

                //2nd row
                if (ett1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Eett1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Eett1pos), Direction.DOWN, Eett1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Etva1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Etva1pos), Direction.DOWN, Etva1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Etree1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Etree1pos), Direction.DOWN, Etree1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Efyra1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Efyra1pos), Direction.DOWN, Efyra1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Efem1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Efem1pos), Direction.DOWN, Efem1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }

                //3rd row
                if (ett2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Eett2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Eett2pos), Direction.DOWN, Eett2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Etva2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Etva2pos), Direction.DOWN, Etva2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Etree2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Etree2pos), Direction.DOWN, Etree2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Efyra2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Efyra2pos), Direction.DOWN, Efyra2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Efem2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Efem2pos), Direction.DOWN, Efem2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }

                //4th row
                if (ett3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Eett3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Eett3pos), Direction.DOWN, Eett3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Etva3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Etva3pos), Direction.DOWN, Etva3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Etree3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Etree3pos), Direction.DOWN, Etree3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Efyra3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Efyra3pos), Direction.DOWN, Efyra3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Efem3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Efem3pos), Direction.DOWN, Efem3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }

                //5th row
                if (ett4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Eett4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Eett4pos), Direction.DOWN, Eett4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Etva4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Etva4pos), Direction.DOWN, Etva4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Etree4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Etree4pos), Direction.DOWN, Etree4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Efyra4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Efyra4pos), Direction.DOWN, Efyra4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Efem4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Efem4pos), Direction.DOWN, Efem4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
            }

            if (playerdir == Direction.WEST){
                //1st row
                if (ett){
                    if (blockticks==tickdelay.get() && mc.level.getBlockState(Wettpos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wettpos), Direction.DOWN, Wettpos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva){
                    if (blockticks==tickdelay.get() && mc.level.getBlockState(Wtvapos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wtvapos), Direction.DOWN, Wtvapos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree){
                    if (blockticks==tickdelay.get() && mc.level.getBlockState(Wtreepos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wtreepos), Direction.DOWN, Wtreepos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra){
                    if (blockticks==tickdelay.get() && mc.level.getBlockState(Wfyrapos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wfyrapos), Direction.DOWN, Wfyrapos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem){
                    if (blockticks==tickdelay.get() && mc.level.getBlockState(Wfempos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wfempos), Direction.DOWN, Wfempos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }

                //2nd row
                if (ett1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Wett1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wett1pos), Direction.DOWN, Wett1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Wtva1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wtva1pos), Direction.DOWN, Wtva1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Wtree1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wtree1pos), Direction.DOWN, Wtree1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Wfyra1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wfyra1pos), Direction.DOWN, Wfyra1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Wfem1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wfem1pos), Direction.DOWN, Wfem1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }

                //3rd row
                if (ett2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Wett2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wett2pos), Direction.DOWN, Wett2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Wtva2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wtva2pos), Direction.DOWN, Wtva2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Wtree2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wtree2pos), Direction.DOWN, Wtree2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Wfyra2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wfyra2pos), Direction.DOWN, Wfyra2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Wfem2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wfem2pos), Direction.DOWN, Wfem2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }

                //4th row
                if (ett3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Wett3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wett3pos), Direction.DOWN, Wett3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Wtva3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wtva3pos), Direction.DOWN, Wtva3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Wtree3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wtree3pos), Direction.DOWN, Wtree3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Wfyra3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wfyra3pos), Direction.DOWN, Wfyra3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Wfem3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wfem3pos), Direction.DOWN, Wfem3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }

                //5th row
                if (ett4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Wett4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wett4pos), Direction.DOWN, Wett4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Wtva4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wtva4pos), Direction.DOWN, Wtva4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Wtree4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wtree4pos), Direction.DOWN, Wtree4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Wfyra4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wfyra4pos), Direction.DOWN, Wfyra4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Wfem4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wfem4pos), Direction.DOWN, Wfem4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
            }
        }
        if (((orientation.get() && playerpitch<=40 && playerpitch>=-40) || (mode.get() == Modes.Vertical && !orientation.get())) && !pause){
            cascadingpileof();
            //north
            BlockPos Nettpos = new BlockPos(lava.getX()-2,lava.getY()+2,lava.getZ());
            BlockPos Nett1pos = new BlockPos(lava.getX()-2,lava.getY()+1,lava.getZ());
            BlockPos Nett2pos = new BlockPos(lava.getX()-2,lava.getY(),lava.getZ());
            BlockPos Nett3pos = new BlockPos(lava.getX()-2,lava.getY()-1,lava.getZ());
            BlockPos Nett4pos = new BlockPos(lava.getX()-2,lava.getY()-2,lava.getZ());
            BlockPos Ntvapos = new BlockPos(lava.getX()-1,lava.getY()+2,lava.getZ());
            BlockPos Ntva1pos = new BlockPos(lava.getX()-1,lava.getY()+1,lava.getZ());
            BlockPos Ntva2pos = new BlockPos(lava.getX()-1,lava.getY(),lava.getZ());
            BlockPos Ntva3pos = new BlockPos(lava.getX()-1,lava.getY()-1,lava.getZ());
            BlockPos Ntva4pos = new BlockPos(lava.getX()-1,lava.getY()-2,lava.getZ());
            BlockPos Ntreepos = new BlockPos(lava.getX(),lava.getY()+2,lava.getZ());
            BlockPos Ntree1pos = new BlockPos(lava.getX(),lava.getY()+1,lava.getZ());
            BlockPos Ntree2pos = new BlockPos(lava.getX(),lava.getY(),lava.getZ());
            BlockPos Ntree3pos = new BlockPos(lava.getX(),lava.getY()-1,lava.getZ());
            BlockPos Ntree4pos = new BlockPos(lava.getX(),lava.getY()-2,lava.getZ());
            BlockPos Nfyrapos = new BlockPos(lava.getX()+1,lava.getY()+2,lava.getZ());
            BlockPos Nfyra1pos = new BlockPos(lava.getX()+1,lava.getY()+1,lava.getZ());
            BlockPos Nfyra2pos = new BlockPos(lava.getX()+1,lava.getY(),lava.getZ());
            BlockPos Nfyra3pos = new BlockPos(lava.getX()+1,lava.getY()-1,lava.getZ());
            BlockPos Nfyra4pos = new BlockPos(lava.getX()+1,lava.getY()-2,lava.getZ());
            BlockPos Nfempos = new BlockPos(lava.getX()+2,lava.getY()+2,lava.getZ());
            BlockPos Nfem1pos = new BlockPos(lava.getX()+2,lava.getY()+1,lava.getZ());
            BlockPos Nfem2pos = new BlockPos(lava.getX()+2,lava.getY(),lava.getZ());
            BlockPos Nfem3pos = new BlockPos(lava.getX()+2,lava.getY()-1,lava.getZ());
            BlockPos Nfem4pos = new BlockPos(lava.getX()+2,lava.getY()-2,lava.getZ());
            //south
            BlockPos Settpos = new BlockPos(lava.getX()+2,lava.getY()+2,lava.getZ());
            BlockPos Sett1pos = new BlockPos(lava.getX()+2,lava.getY()+1,lava.getZ());
            BlockPos Sett2pos = new BlockPos(lava.getX()+2,lava.getY(),lava.getZ());
            BlockPos Sett3pos = new BlockPos(lava.getX()+2,lava.getY()-1,lava.getZ());
            BlockPos Sett4pos = new BlockPos(lava.getX()+2,lava.getY()-2,lava.getZ());
            BlockPos Stvapos = new BlockPos(lava.getX()+1,lava.getY()+2,lava.getZ());
            BlockPos Stva1pos = new BlockPos(lava.getX()+1,lava.getY()+1,lava.getZ());
            BlockPos Stva2pos = new BlockPos(lava.getX()+1,lava.getY(),lava.getZ());
            BlockPos Stva3pos = new BlockPos(lava.getX()+1,lava.getY()-1,lava.getZ());
            BlockPos Stva4pos = new BlockPos(lava.getX()+1,lava.getY()-2,lava.getZ());
            BlockPos Streepos = new BlockPos(lava.getX(),lava.getY()+2,lava.getZ());
            BlockPos Stree1pos = new BlockPos(lava.getX(),lava.getY()+1,lava.getZ());
            BlockPos Stree2pos = new BlockPos(lava.getX(),lava.getY(),lava.getZ());
            BlockPos Stree3pos = new BlockPos(lava.getX(),lava.getY()-1,lava.getZ());
            BlockPos Stree4pos = new BlockPos(lava.getX(),lava.getY()-2,lava.getZ());
            BlockPos Sfyrapos = new BlockPos(lava.getX()-1,lava.getY()+2,lava.getZ());
            BlockPos Sfyra1pos = new BlockPos(lava.getX()-1,lava.getY()+1,lava.getZ());
            BlockPos Sfyra2pos = new BlockPos(lava.getX()-1,lava.getY(),lava.getZ());
            BlockPos Sfyra3pos = new BlockPos(lava.getX()-1,lava.getY()-1,lava.getZ());
            BlockPos Sfyra4pos = new BlockPos(lava.getX()-1,lava.getY()-2,lava.getZ());
            BlockPos Sfempos = new BlockPos(lava.getX()-2,lava.getY()+2,lava.getZ());
            BlockPos Sfem1pos = new BlockPos(lava.getX()-2,lava.getY()+1,lava.getZ());
            BlockPos Sfem2pos = new BlockPos(lava.getX()-2,lava.getY(),lava.getZ());
            BlockPos Sfem3pos = new BlockPos(lava.getX()-2,lava.getY()-1,lava.getZ());
            BlockPos Sfem4pos = new BlockPos(lava.getX()-2,lava.getY()-2,lava.getZ());
            //east
            BlockPos Eettpos = new BlockPos(lava.getX(),lava.getY()+2,lava.getZ()-2);
            BlockPos Eett1pos = new BlockPos(lava.getX(),lava.getY()+1,lava.getZ()-2);
            BlockPos Eett2pos = new BlockPos(lava.getX(),lava.getY(),lava.getZ()-2);
            BlockPos Eett3pos = new BlockPos(lava.getX(),lava.getY()-1,lava.getZ()-2);
            BlockPos Eett4pos = new BlockPos(lava.getX(),lava.getY()-2,lava.getZ()-2);
            BlockPos Etvapos = new BlockPos(lava.getX(),lava.getY()+2,lava.getZ()-1);
            BlockPos Etva1pos = new BlockPos(lava.getX(),lava.getY()+1,lava.getZ()-1);
            BlockPos Etva2pos = new BlockPos(lava.getX(),lava.getY(),lava.getZ()-1);
            BlockPos Etva3pos = new BlockPos(lava.getX(),lava.getY()-1,lava.getZ()-1);
            BlockPos Etva4pos = new BlockPos(lava.getX(),lava.getY()-2,lava.getZ()-1);
            BlockPos Etreepos = new BlockPos(lava.getX(),lava.getY()+2,lava.getZ());
            BlockPos Etree1pos = new BlockPos(lava.getX(),lava.getY()+1,lava.getZ());
            BlockPos Etree2pos = new BlockPos(lava.getX(),lava.getY(),lava.getZ());
            BlockPos Etree3pos = new BlockPos(lava.getX(),lava.getY()-1,lava.getZ());
            BlockPos Etree4pos = new BlockPos(lava.getX(),lava.getY()-2,lava.getZ());
            BlockPos Efyrapos = new BlockPos(lava.getX(),lava.getY()+2,lava.getZ()+1);
            BlockPos Efyra1pos = new BlockPos(lava.getX(),lava.getY()+1,lava.getZ()+1);
            BlockPos Efyra2pos = new BlockPos(lava.getX(),lava.getY(),lava.getZ()+1);
            BlockPos Efyra3pos = new BlockPos(lava.getX(),lava.getY()-1,lava.getZ()+1);
            BlockPos Efyra4pos = new BlockPos(lava.getX(),lava.getY()-2,lava.getZ()+1);
            BlockPos Efempos = new BlockPos(lava.getX(),lava.getY()+2,lava.getZ()+2);
            BlockPos Efem1pos = new BlockPos(lava.getX(),lava.getY()+1,lava.getZ()+2);
            BlockPos Efem2pos = new BlockPos(lava.getX(),lava.getY(),lava.getZ()+2);
            BlockPos Efem3pos = new BlockPos(lava.getX(),lava.getY()-1,lava.getZ()+2);
            BlockPos Efem4pos = new BlockPos(lava.getX(),lava.getY()-2,lava.getZ()+2);
            //west
            BlockPos Wettpos = new BlockPos(lava.getX(),lava.getY()+2,lava.getZ()+2);
            BlockPos Wett1pos = new BlockPos(lava.getX(),lava.getY()+1,lava.getZ()+2);
            BlockPos Wett2pos = new BlockPos(lava.getX(),lava.getY(),lava.getZ()+2);
            BlockPos Wett3pos = new BlockPos(lava.getX(),lava.getY()-1,lava.getZ()+2);
            BlockPos Wett4pos = new BlockPos(lava.getX(),lava.getY()-2,lava.getZ()+2);
            BlockPos Wtvapos = new BlockPos(lava.getX(),lava.getY()+2,lava.getZ()+1);
            BlockPos Wtva1pos = new BlockPos(lava.getX(),lava.getY()+1,lava.getZ()+1);
            BlockPos Wtva2pos = new BlockPos(lava.getX(),lava.getY(),lava.getZ()+1);
            BlockPos Wtva3pos = new BlockPos(lava.getX(),lava.getY()-1,lava.getZ()+1);
            BlockPos Wtva4pos = new BlockPos(lava.getX(),lava.getY()-2,lava.getZ()+1);
            BlockPos Wtreepos = new BlockPos(lava.getX(),lava.getY()+2,lava.getZ());
            BlockPos Wtree1pos = new BlockPos(lava.getX(),lava.getY()+1,lava.getZ());
            BlockPos Wtree2pos = new BlockPos(lava.getX(),lava.getY(),lava.getZ());
            BlockPos Wtree3pos = new BlockPos(lava.getX(),lava.getY()-1,lava.getZ());
            BlockPos Wtree4pos = new BlockPos(lava.getX(),lava.getY()-2,lava.getZ());
            BlockPos Wfyrapos = new BlockPos(lava.getX(),lava.getY()+2,lava.getZ()-1);
            BlockPos Wfyra1pos = new BlockPos(lava.getX(),lava.getY()+1,lava.getZ()-1);
            BlockPos Wfyra2pos = new BlockPos(lava.getX(),lava.getY(),lava.getZ()-1);
            BlockPos Wfyra3pos = new BlockPos(lava.getX(),lava.getY()-1,lava.getZ()-1);
            BlockPos Wfyra4pos = new BlockPos(lava.getX(),lava.getY()-2,lava.getZ()-1);
            BlockPos Wfempos = new BlockPos(lava.getX(),lava.getY()+2,lava.getZ()-2);
            BlockPos Wfem1pos = new BlockPos(lava.getX(),lava.getY()+1,lava.getZ()-2);
            BlockPos Wfem2pos = new BlockPos(lava.getX(),lava.getY(),lava.getZ()-2);
            BlockPos Wfem3pos = new BlockPos(lava.getX(),lava.getY()-1,lava.getZ()-2);
            BlockPos Wfem4pos = new BlockPos(lava.getX(),lava.getY()-2,lava.getZ()-2);

            if (playerdir == Direction.NORTH){
                //1st row
                if (ett){
                    if (blockticks==tickdelay.get() && mc.level.getBlockState(Nettpos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Nettpos), Direction.DOWN, Nettpos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva){
                    if (blockticks==tickdelay.get() && mc.level.getBlockState(Ntvapos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Ntvapos), Direction.DOWN, Ntvapos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree){
                    if (blockticks==tickdelay.get() && mc.level.getBlockState(Ntreepos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Ntreepos), Direction.DOWN, Ntreepos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra){
                    if (blockticks==tickdelay.get() && mc.level.getBlockState(Nfyrapos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Nfyrapos), Direction.DOWN, Nfyrapos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem){
                    if (blockticks==tickdelay.get() && mc.level.getBlockState(Nfempos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Nfempos), Direction.DOWN, Nfempos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }

                //2nd row
                if (ett1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Nett1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Nett1pos), Direction.DOWN, Nett1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Ntva1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Ntva1pos), Direction.DOWN, Ntva1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Ntree1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Ntree1pos), Direction.DOWN, Ntree1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Nfyra1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Nfyra1pos), Direction.DOWN, Nfyra1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Nfem1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Nfem1pos), Direction.DOWN, Nfem1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }

                //3rd row
                if (ett2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Nett2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Nett2pos), Direction.DOWN, Nett2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Ntva2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Ntva2pos), Direction.DOWN, Ntva2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Ntree2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Ntree2pos), Direction.DOWN, Ntree2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Nfyra2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Nfyra2pos), Direction.DOWN, Nfyra2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Nfem2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Nfem2pos), Direction.DOWN, Nfem2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }

                //4th row
                if (ett3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Nett3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Nett3pos), Direction.DOWN, Nett3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Ntva3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Ntva3pos), Direction.DOWN, Ntva3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Ntree3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Ntree3pos), Direction.DOWN, Ntree3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Nfyra3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Nfyra3pos), Direction.DOWN, Nfyra3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Nfem3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Nfem3pos), Direction.DOWN, Nfem3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }

                //5th row
                if (ett4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Nett4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Nett4pos), Direction.DOWN, Nett4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Ntva4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Ntva4pos), Direction.DOWN, Ntva4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Ntree4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Ntree4pos), Direction.DOWN, Ntree4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Nfyra4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Nfyra4pos), Direction.DOWN, Nfyra4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Nfem4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Nfem4pos), Direction.DOWN, Nfem4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
            }

            if (playerdir == Direction.SOUTH){
                //1st row
                if (ett){
                    if (blockticks==tickdelay.get() && mc.level.getBlockState(Settpos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Settpos), Direction.DOWN, Settpos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva){
                    if (blockticks==tickdelay.get() && mc.level.getBlockState(Stvapos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Stvapos), Direction.DOWN, Stvapos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree){
                    if (blockticks==tickdelay.get() && mc.level.getBlockState(Streepos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Streepos), Direction.DOWN, Streepos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra){
                    if (blockticks==tickdelay.get() && mc.level.getBlockState(Sfyrapos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Sfyrapos), Direction.DOWN, Sfyrapos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem){
                    if (blockticks==tickdelay.get() && mc.level.getBlockState(Sfempos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Sfempos), Direction.DOWN, Sfempos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }

                //2nd row
                if (ett1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Sett1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Sett1pos), Direction.DOWN, Sett1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Stva1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Stva1pos), Direction.DOWN, Stva1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Stree1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Stree1pos), Direction.DOWN, Stree1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Sfyra1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Sfyra1pos), Direction.DOWN, Sfyra1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Sfem1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Sfem1pos), Direction.DOWN, Sfem1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }

                //3rd row
                if (ett2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Sett2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Sett2pos), Direction.DOWN, Sett2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Stva2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Stva2pos), Direction.DOWN, Stva2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Stree2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Stree2pos), Direction.DOWN, Stree2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Sfyra2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Sfyra2pos), Direction.DOWN, Sfyra2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Sfem2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Sfem2pos), Direction.DOWN, Sfem2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }

                //4th row
                if (ett3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Sett3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Sett3pos), Direction.DOWN, Sett3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Stva3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Stva3pos), Direction.DOWN, Stva3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Stree3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Stree3pos), Direction.DOWN, Stree3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Sfyra3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Sfyra3pos), Direction.DOWN, Sfyra3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Sfem3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Sfem3pos), Direction.DOWN, Sfem3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }

                //5th row
                if (ett4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Sett4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Sett4pos), Direction.DOWN, Sett4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Stva4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Stva4pos), Direction.DOWN, Stva4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Stree4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Stree4pos), Direction.DOWN, Stree4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Sfyra4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Sfyra4pos), Direction.DOWN, Sfyra4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Sfem4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Sfem4pos), Direction.DOWN, Sfem4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
            }

            if (playerdir == Direction.EAST){
                //1st row
                if (ett){
                    if (blockticks==tickdelay.get() && mc.level.getBlockState(Eettpos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Eettpos), Direction.DOWN, Eettpos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva){
                    if (blockticks==tickdelay.get() && mc.level.getBlockState(Etvapos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Etvapos), Direction.DOWN, Etvapos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree){
                    if (blockticks==tickdelay.get() && mc.level.getBlockState(Etreepos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Etreepos), Direction.DOWN, Etreepos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra){
                    if (blockticks==tickdelay.get() && mc.level.getBlockState(Efyrapos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Efyrapos), Direction.DOWN, Efyrapos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem){
                    if (blockticks==tickdelay.get() && mc.level.getBlockState(Efempos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Efempos), Direction.DOWN, Efempos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }

                //2nd row
                if (ett1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Eett1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Eett1pos), Direction.DOWN, Eett1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Etva1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Etva1pos), Direction.DOWN, Etva1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Etree1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Etree1pos), Direction.DOWN, Etree1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Efyra1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Efyra1pos), Direction.DOWN, Efyra1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Efem1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Efem1pos), Direction.DOWN, Efem1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }

                //3rd row
                if (ett2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Eett2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Eett2pos), Direction.DOWN, Eett2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Etva2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Etva2pos), Direction.DOWN, Etva2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Etree2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Etree2pos), Direction.DOWN, Etree2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Efyra2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Efyra2pos), Direction.DOWN, Efyra2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Efem2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Efem2pos), Direction.DOWN, Efem2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }

                //4th row
                if (ett3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Eett3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Eett3pos), Direction.DOWN, Eett3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Etva3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Etva3pos), Direction.DOWN, Etva3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Etree3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Etree3pos), Direction.DOWN, Etree3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Efyra3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Efyra3pos), Direction.DOWN, Efyra3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Efem3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Efem3pos), Direction.DOWN, Efem3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }

                //5th row
                if (ett4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Eett4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Eett4pos), Direction.DOWN, Eett4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Etva4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Etva4pos), Direction.DOWN, Etva4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Etree4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Etree4pos), Direction.DOWN, Etree4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Efyra4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Efyra4pos), Direction.DOWN, Efyra4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Efem4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Efem4pos), Direction.DOWN, Efem4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
            }

            if (playerdir == Direction.WEST){
                //1st row
                if (ett){
                    if (blockticks==tickdelay.get() && mc.level.getBlockState(Wettpos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wettpos), Direction.DOWN, Wettpos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva){
                    if (blockticks==tickdelay.get() && mc.level.getBlockState(Wtvapos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wtvapos), Direction.DOWN, Wtvapos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree){
                    if (blockticks==tickdelay.get() && mc.level.getBlockState(Wtreepos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wtreepos), Direction.DOWN, Wtreepos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra){
                    if (blockticks==tickdelay.get() && mc.level.getBlockState(Wfyrapos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wfyrapos), Direction.DOWN, Wfyrapos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem){
                    if (blockticks==tickdelay.get() && mc.level.getBlockState(Wfempos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wfempos), Direction.DOWN, Wfempos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }

                //2nd row
                if (ett1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Wett1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wett1pos), Direction.DOWN, Wett1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Wtva1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wtva1pos), Direction.DOWN, Wtva1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Wtree1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wtree1pos), Direction.DOWN, Wtree1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Wfyra1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wfyra1pos), Direction.DOWN, Wfyra1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem1){
                    if (blockticks==tickdelay.get()*2 && mc.level.getBlockState(Wfem1pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wfem1pos), Direction.DOWN, Wfem1pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }

                //3rd row
                if (ett2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Wett2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wett2pos), Direction.DOWN, Wett2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Wtva2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wtva2pos), Direction.DOWN, Wtva2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Wtree2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wtree2pos), Direction.DOWN, Wtree2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Wfyra2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wfyra2pos), Direction.DOWN, Wfyra2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem2){
                    if (blockticks==tickdelay.get()*3 && mc.level.getBlockState(Wfem2pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wfem2pos), Direction.DOWN, Wfem2pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }

                //4th row
                if (ett3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Wett3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wett3pos), Direction.DOWN, Wett3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Wtva3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wtva3pos), Direction.DOWN, Wtva3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Wtree3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wtree3pos), Direction.DOWN, Wtree3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Wfyra3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wfyra3pos), Direction.DOWN, Wfyra3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem3){
                    if (blockticks==tickdelay.get()*4 && mc.level.getBlockState(Wfem3pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wfem3pos), Direction.DOWN, Wfem3pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }

                //5th row
                if (ett4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Wett4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wett4pos), Direction.DOWN, Wett4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tva4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Wtva4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wtva4pos), Direction.DOWN, Wtva4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (tree4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Wtree4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wtree4pos), Direction.DOWN, Wtree4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fyra4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Wfyra4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wfyra4pos), Direction.DOWN, Wfyra4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
                if (fem4){
                    if (blockticks==tickdelay.get()*5 && mc.level.getBlockState(Wfem4pos).canBeReplaced()){
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(Wfem4pos), Direction.DOWN, Wfem4pos, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);}
                }
            }
        }
    }
    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (event.screen instanceof DisconnectedScreen) {
            toggle();
        }
        if (event.screen instanceof DeathScreen) {
            toggle();
        }
    }
    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        toggle();
    }

    private BlockPos cast() {
        HitResult blockHit = mc.getCameraEntity().pick(reach.get(), 0, false);
        return ((BlockHitResult) blockHit).getBlockPos();
    }
    private void cascadingpileof() {
        if (mc.player == null) return;
        FindItemResult findResult = InvUtils.findInHotbar(block -> !isInvalidBlock(block));
        if (!findResult.found()) {
            return;
        }
        mc.player.getInventory().setSelectedSlot(findResult.slot());
    }
    private boolean isInvalidBlock(ItemStack stack) {
        return !(stack.getItem() instanceof BlockItem)
                || stack.getItem() instanceof BedItem
                || stack.getItem() instanceof SolidBucketItem
                || stack.getItem() instanceof ScaffoldingBlockItem
                || stack.getItem() instanceof DoubleHighBlockItem
                || stack.getItem() instanceof StandingAndWallBlockItem
                || stack.getItem() instanceof PlaceOnWaterBlockItem
                || ((BlockItem) stack.getItem()).getBlock() instanceof VegetationBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof TorchBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof DiodeBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof RedStoneWireBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof FenceBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof WallBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof FenceGateBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof FallingBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof BaseRailBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof SignBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof BellBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof CarpetBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof ConduitBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof CoralFanBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof CoralWallFanBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof BaseCoralFanBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof BaseCoralWallFanBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof TripWireHookBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof PointedDripstoneBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof TripWireBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof PressurePlateBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof FaceAttachedHorizontalDirectionalBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof ShulkerBoxBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof AmethystClusterBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof BuddingAmethystBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof ChorusFlowerBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof ChorusPlantBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof LanternBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof CandleBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof CakeBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof SugarCaneBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof SporeBlossomBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof KelpBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof GlowLichenBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof CactusBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof BambooStalkBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof FlowerPotBlock
                || ((BlockItem) stack.getItem()).getBlock() instanceof LadderBlock
                || skippableBlox.get().contains(((BlockItem) stack.getItem()).getBlock());
    }
    public enum Modes {
        Horizontal, Vertical
    }
}