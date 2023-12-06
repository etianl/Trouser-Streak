//Written By etianll, using just a bit of code from banana's autobuild. Thanks to them for the idea and framework for the codes
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
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
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.item.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import pwn.noobs.trouserstreak.Trouser;

public class TrouserBuild extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
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
            .visible(() -> render.get())
            .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The color of the sides of the blocks being rendered.")
            .defaultValue(new SettingColor(255, 0, 255, 15))
            .visible(() -> render.get())
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The color of the lines of the blocks being rendered.")
            .defaultValue(new SettingColor(255, 0, 255, 255))
            .visible(() -> render.get())
            .build()
    );

    public TrouserBuild() {
        super(Trouser.Main, "TrouserBuild", "Can build either horizontally and vertically according to a 5x5 grid centered on the block you are aiming at.");
    }
    boolean ett = false, tva = false, tree = false, fyra = false, fem = false;
    boolean ett1 = false,tva1 = true, tree1 = true, fyra1 = true, fem1 = false;
    boolean ett2 = false, tva2 = false, tree2 = true, fyra2 = false, fem2 = false;
    boolean ett3 = false, tva3 = false, tree3 = true, fyra3 = false, fem3 = false;
    boolean ett4 = false, tva4 = false, tree4 = false, fyra4 = false, fem4 = false;

    private boolean pause = true;
    private BlockPos lava;
    private Direction playerdir;
    private float playerpitch;
    private int loops=0;
    private int blockticks=0;

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
    private void onMouseButton(MouseButtonEvent event) {
        if (mc.options.useKey.isPressed()){
            lava = cast();
            playerdir=mc.player.getHorizontalFacing();
            playerpitch=mc.player.getPitch();
            pause = pause ? false : true;
        }
    }
    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (pause==true) {
            lava=cast();
            blockticks=0;
            loops=0;
        }
        if (pause==false){
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
        if (((orientation.get() && playerpitch>40 | playerpitch<-40) || (mode.get() == Modes.Horizontal && !orientation.get())) && pause==false){
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
            if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Nettpos).isReplaceable()){
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Nettpos), Direction.DOWN, Nettpos, false));
                mc.player.swingHand(Hand.MAIN_HAND);}
        }
        if (tva){
            if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Ntvapos).isReplaceable()){
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Ntvapos), Direction.DOWN, Ntvapos, false));
                mc.player.swingHand(Hand.MAIN_HAND);}
        }
        if (tree){
            if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Ntreepos).isReplaceable()){
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Ntreepos), Direction.DOWN, Ntreepos, false));
                mc.player.swingHand(Hand.MAIN_HAND);}
        }
        if (fyra){
            if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Nfyrapos).isReplaceable()){
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Nfyrapos), Direction.DOWN, Nfyrapos, false));
                mc.player.swingHand(Hand.MAIN_HAND);}
        }
        if (fem){
            if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Nfempos).isReplaceable()){
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Nfempos), Direction.DOWN, Nfempos, false));
                mc.player.swingHand(Hand.MAIN_HAND);}
        }

            //2nd row
        if (ett1){
            if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Nett1pos).isReplaceable()){
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Nett1pos), Direction.DOWN, Nett1pos, false));
                mc.player.swingHand(Hand.MAIN_HAND);}
        }
        if (tva1){
            if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Ntva1pos).isReplaceable()){
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Ntva1pos), Direction.DOWN, Ntva1pos, false));
                mc.player.swingHand(Hand.MAIN_HAND);}
        }
        if (tree1){
            if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Ntree1pos).isReplaceable()){
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Ntree1pos), Direction.DOWN, Ntree1pos, false));
                mc.player.swingHand(Hand.MAIN_HAND);}
        }
        if (fyra1){
            if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Nfyra1pos).isReplaceable()){
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Nfyra1pos), Direction.DOWN, Nfyra1pos, false));
                mc.player.swingHand(Hand.MAIN_HAND);}
        }
        if (fem1){
            if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Nfem1pos).isReplaceable()){
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Nfem1pos), Direction.DOWN, Nfem1pos, false));
                mc.player.swingHand(Hand.MAIN_HAND);}
        }

            //3rd row
        if (ett2){
            if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Nett2pos).isReplaceable()){
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Nett2pos), Direction.DOWN, Nett2pos, false));
                mc.player.swingHand(Hand.MAIN_HAND);}
        }
        if (tva2){
            if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Ntva2pos).isReplaceable()){
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Ntva2pos), Direction.DOWN, Ntva2pos, false));
                mc.player.swingHand(Hand.MAIN_HAND);}
        }
        if (tree2){
            if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Ntree2pos).isReplaceable()){
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Ntree2pos), Direction.DOWN, Ntree2pos, false));
                mc.player.swingHand(Hand.MAIN_HAND);}
        }
        if (fyra2){
            if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Nfyra2pos).isReplaceable()){
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Nfyra2pos), Direction.DOWN, Nfyra2pos, false));
                mc.player.swingHand(Hand.MAIN_HAND);}
        }
        if (fem2){
            if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Nfem2pos).isReplaceable()){
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Nfem2pos), Direction.DOWN, Nfem2pos, false));
                mc.player.swingHand(Hand.MAIN_HAND);}
        }

            //4th row
        if (ett3){
            if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Nett3pos).isReplaceable()){
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Nett3pos), Direction.DOWN, Nett3pos, false));
                mc.player.swingHand(Hand.MAIN_HAND);}
        }
        if (tva3){
            if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Ntva3pos).isReplaceable()){
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Ntva3pos), Direction.DOWN, Ntva3pos, false));
                mc.player.swingHand(Hand.MAIN_HAND);}
        }
        if (tree3){
            if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Ntree3pos).isReplaceable()){
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Ntree3pos), Direction.DOWN, Ntree3pos, false));
                mc.player.swingHand(Hand.MAIN_HAND);}
        }
        if (fyra3){
            if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Nfyra3pos).isReplaceable()){
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Nfyra3pos), Direction.DOWN, Nfyra3pos, false));
                mc.player.swingHand(Hand.MAIN_HAND);}
        }
        if (fem3){
            if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Nfem3pos).isReplaceable()){
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Nfem3pos), Direction.DOWN, Nfem3pos, false));
                mc.player.swingHand(Hand.MAIN_HAND);}
        }

            //5th row
        if (ett4){
            if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Nett4pos).isReplaceable()){
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Nett4pos), Direction.DOWN, Nett4pos, false));
                mc.player.swingHand(Hand.MAIN_HAND);}
        }
        if (tva4){
            if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Ntva4pos).isReplaceable()){
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Ntva4pos), Direction.DOWN, Ntva4pos, false));
                mc.player.swingHand(Hand.MAIN_HAND);}
        }
        if (tree4){
            if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Ntree4pos).isReplaceable()){
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Ntree4pos), Direction.DOWN, Ntree4pos, false));
                mc.player.swingHand(Hand.MAIN_HAND);}
        }
        if (fyra4){
            if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Nfyra4pos).isReplaceable()){
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Nfyra4pos), Direction.DOWN, Nfyra4pos, false));
                mc.player.swingHand(Hand.MAIN_HAND);}
        }
        if (fem4){
            if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Nfem4pos).isReplaceable()){
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Nfem4pos), Direction.DOWN, Nfem4pos, false));
                mc.player.swingHand(Hand.MAIN_HAND);}
        }
}

        if (playerdir == Direction.SOUTH){
            //1st row
            if (ett){
                if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Settpos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Settpos), Direction.DOWN, Settpos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (tva){
                if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Stvapos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Stvapos), Direction.DOWN, Stvapos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (tree){
                if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Streepos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Streepos), Direction.DOWN, Streepos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (fyra){
                if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Sfyrapos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Sfyrapos), Direction.DOWN, Sfyrapos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (fem){
                if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Sfempos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Sfempos), Direction.DOWN, Sfempos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }

            //2nd row
            if (ett1){
                if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Sett1pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Sett1pos), Direction.DOWN, Sett1pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (tva1){
                if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Stva1pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Stva1pos), Direction.DOWN, Stva1pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (tree1){
                if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Stree1pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Stree1pos), Direction.DOWN, Stree1pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (fyra1){
                if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Sfyra1pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Sfyra1pos), Direction.DOWN, Sfyra1pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (fem1){
                if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Sfem1pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Sfem1pos), Direction.DOWN, Sfem1pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }

            //3rd row
            if (ett2){
                if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Sett2pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Sett2pos), Direction.DOWN, Sett2pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (tva2){
                if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Stva2pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Stva2pos), Direction.DOWN, Stva2pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (tree2){
                if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Stree2pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Stree2pos), Direction.DOWN, Stree2pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (fyra2){
                if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Sfyra2pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Sfyra2pos), Direction.DOWN, Sfyra2pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (fem2){
                if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Sfem2pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Sfem2pos), Direction.DOWN, Sfem2pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }

            //4th row
            if (ett3){
                if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Sett3pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Sett3pos), Direction.DOWN, Sett3pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (tva3){
                if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Stva3pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Stva3pos), Direction.DOWN, Stva3pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (tree3){
                if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Stree3pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Stree3pos), Direction.DOWN, Stree3pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (fyra3){
                if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Sfyra3pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Sfyra3pos), Direction.DOWN, Sfyra3pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (fem3){
                if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Sfem3pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Sfem3pos), Direction.DOWN, Sfem3pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }

            //5th row
            if (ett4){
                if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Sett4pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Sett4pos), Direction.DOWN, Sett4pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (tva4){
                if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Stva4pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Stva4pos), Direction.DOWN, Stva4pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (tree4){
                if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Stree4pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Stree4pos), Direction.DOWN, Stree4pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (fyra4){
                if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Sfyra4pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Sfyra4pos), Direction.DOWN, Sfyra4pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (fem4){
                if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Sfem4pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Sfem4pos), Direction.DOWN, Sfem4pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
        }

        if (playerdir == Direction.EAST){
            //1st row
            if (ett){
                if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Eettpos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Eettpos), Direction.DOWN, Eettpos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (tva){
                if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Etvapos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Etvapos), Direction.DOWN, Etvapos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (tree){
                if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Etreepos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Etreepos), Direction.DOWN, Etreepos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (fyra){
                if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Efyrapos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Efyrapos), Direction.DOWN, Efyrapos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (fem){
                if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Efempos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Efempos), Direction.DOWN, Efempos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }

            //2nd row
            if (ett1){
                if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Eett1pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Eett1pos), Direction.DOWN, Eett1pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (tva1){
                if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Etva1pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Etva1pos), Direction.DOWN, Etva1pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (tree1){
                if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Etree1pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Etree1pos), Direction.DOWN, Etree1pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (fyra1){
                if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Efyra1pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Efyra1pos), Direction.DOWN, Efyra1pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (fem1){
                if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Efem1pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Efem1pos), Direction.DOWN, Efem1pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }

            //3rd row
            if (ett2){
                if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Eett2pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Eett2pos), Direction.DOWN, Eett2pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (tva2){
                if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Etva2pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Etva2pos), Direction.DOWN, Etva2pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (tree2){
                if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Etree2pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Etree2pos), Direction.DOWN, Etree2pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (fyra2){
                if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Efyra2pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Efyra2pos), Direction.DOWN, Efyra2pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (fem2){
                if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Efem2pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Efem2pos), Direction.DOWN, Efem2pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }

            //4th row
            if (ett3){
                if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Eett3pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Eett3pos), Direction.DOWN, Eett3pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (tva3){
                if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Etva3pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Etva3pos), Direction.DOWN, Etva3pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (tree3){
                if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Etree3pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Etree3pos), Direction.DOWN, Etree3pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (fyra3){
                if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Efyra3pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Efyra3pos), Direction.DOWN, Efyra3pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (fem3){
                if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Efem3pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Efem3pos), Direction.DOWN, Efem3pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }

            //5th row
            if (ett4){
                if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Eett4pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Eett4pos), Direction.DOWN, Eett4pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (tva4){
                if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Etva4pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Etva4pos), Direction.DOWN, Etva4pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (tree4){
                if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Etree4pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Etree4pos), Direction.DOWN, Etree4pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (fyra4){
                if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Efyra4pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Efyra4pos), Direction.DOWN, Efyra4pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (fem4){
                if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Efem4pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Efem4pos), Direction.DOWN, Efem4pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
        }

        if (playerdir == Direction.WEST){
            //1st row
            if (ett){
                if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Wettpos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wettpos), Direction.DOWN, Wettpos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (tva){
                if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Wtvapos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wtvapos), Direction.DOWN, Wtvapos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (tree){
                if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Wtreepos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wtreepos), Direction.DOWN, Wtreepos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (fyra){
                if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Wfyrapos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wfyrapos), Direction.DOWN, Wfyrapos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (fem){
                if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Wfempos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wfempos), Direction.DOWN, Wfempos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }

            //2nd row
            if (ett1){
                if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Wett1pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wett1pos), Direction.DOWN, Wett1pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (tva1){
                if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Wtva1pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wtva1pos), Direction.DOWN, Wtva1pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (tree1){
                if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Wtree1pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wtree1pos), Direction.DOWN, Wtree1pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (fyra1){
                if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Wfyra1pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wfyra1pos), Direction.DOWN, Wfyra1pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (fem1){
                if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Wfem1pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wfem1pos), Direction.DOWN, Wfem1pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }

            //3rd row
            if (ett2){
                if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Wett2pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wett2pos), Direction.DOWN, Wett2pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (tva2){
                if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Wtva2pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wtva2pos), Direction.DOWN, Wtva2pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (tree2){
                if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Wtree2pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wtree2pos), Direction.DOWN, Wtree2pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (fyra2){
                if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Wfyra2pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wfyra2pos), Direction.DOWN, Wfyra2pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (fem2){
                if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Wfem2pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wfem2pos), Direction.DOWN, Wfem2pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }

            //4th row
            if (ett3){
                if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Wett3pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wett3pos), Direction.DOWN, Wett3pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (tva3){
                if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Wtva3pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wtva3pos), Direction.DOWN, Wtva3pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (tree3){
                if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Wtree3pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wtree3pos), Direction.DOWN, Wtree3pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (fyra3){
                if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Wfyra3pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wfyra3pos), Direction.DOWN, Wfyra3pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (fem3){
                if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Wfem3pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wfem3pos), Direction.DOWN, Wfem3pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }

            //5th row
            if (ett4){
                if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Wett4pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wett4pos), Direction.DOWN, Wett4pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (tva4){
                if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Wtva4pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wtva4pos), Direction.DOWN, Wtva4pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (tree4){
                if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Wtree4pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wtree4pos), Direction.DOWN, Wtree4pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (fyra4){
                if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Wfyra4pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wfyra4pos), Direction.DOWN, Wfyra4pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
            if (fem4){
                if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Wfem4pos).isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wfem4pos), Direction.DOWN, Wfem4pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);}
            }
        }
        }
        if (((orientation.get() && playerpitch<=40 && playerpitch>=-40) || (mode.get() == Modes.Vertical && !orientation.get())) && pause==false){
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
                    if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Nettpos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Nettpos), Direction.DOWN, Nettpos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tva){
                    if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Ntvapos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Ntvapos), Direction.DOWN, Ntvapos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tree){
                    if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Ntreepos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Ntreepos), Direction.DOWN, Ntreepos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fyra){
                    if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Nfyrapos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Nfyrapos), Direction.DOWN, Nfyrapos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fem){
                    if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Nfempos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Nfempos), Direction.DOWN, Nfempos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }

                //2nd row
                if (ett1){
                    if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Nett1pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Nett1pos), Direction.DOWN, Nett1pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tva1){
                    if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Ntva1pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Ntva1pos), Direction.DOWN, Ntva1pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tree1){
                    if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Ntree1pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Ntree1pos), Direction.DOWN, Ntree1pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fyra1){
                    if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Nfyra1pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Nfyra1pos), Direction.DOWN, Nfyra1pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fem1){
                    if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Nfem1pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Nfem1pos), Direction.DOWN, Nfem1pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }

                //3rd row
                if (ett2){
                    if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Nett2pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Nett2pos), Direction.DOWN, Nett2pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tva2){
                    if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Ntva2pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Ntva2pos), Direction.DOWN, Ntva2pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tree2){
                    if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Ntree2pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Ntree2pos), Direction.DOWN, Ntree2pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fyra2){
                    if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Nfyra2pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Nfyra2pos), Direction.DOWN, Nfyra2pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fem2){
                    if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Nfem2pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Nfem2pos), Direction.DOWN, Nfem2pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }

                //4th row
                if (ett3){
                    if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Nett3pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Nett3pos), Direction.DOWN, Nett3pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tva3){
                    if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Ntva3pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Ntva3pos), Direction.DOWN, Ntva3pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tree3){
                    if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Ntree3pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Ntree3pos), Direction.DOWN, Ntree3pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fyra3){
                    if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Nfyra3pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Nfyra3pos), Direction.DOWN, Nfyra3pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fem3){
                    if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Nfem3pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Nfem3pos), Direction.DOWN, Nfem3pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }

                //5th row
                if (ett4){
                    if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Nett4pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Nett4pos), Direction.DOWN, Nett4pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tva4){
                    if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Ntva4pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Ntva4pos), Direction.DOWN, Ntva4pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tree4){
                    if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Ntree4pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Ntree4pos), Direction.DOWN, Ntree4pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fyra4){
                    if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Nfyra4pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Nfyra4pos), Direction.DOWN, Nfyra4pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fem4){
                    if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Nfem4pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Nfem4pos), Direction.DOWN, Nfem4pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
            }

            if (playerdir == Direction.SOUTH){
                //1st row
                if (ett){
                    if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Settpos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Settpos), Direction.DOWN, Settpos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tva){
                    if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Stvapos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Stvapos), Direction.DOWN, Stvapos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tree){
                    if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Streepos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Streepos), Direction.DOWN, Streepos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fyra){
                    if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Sfyrapos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Sfyrapos), Direction.DOWN, Sfyrapos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fem){
                    if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Sfempos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Sfempos), Direction.DOWN, Sfempos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }

                //2nd row
                if (ett1){
                    if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Sett1pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Sett1pos), Direction.DOWN, Sett1pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tva1){
                    if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Stva1pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Stva1pos), Direction.DOWN, Stva1pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tree1){
                    if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Stree1pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Stree1pos), Direction.DOWN, Stree1pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fyra1){
                    if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Sfyra1pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Sfyra1pos), Direction.DOWN, Sfyra1pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fem1){
                    if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Sfem1pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Sfem1pos), Direction.DOWN, Sfem1pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }

                //3rd row
                if (ett2){
                    if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Sett2pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Sett2pos), Direction.DOWN, Sett2pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tva2){
                    if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Stva2pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Stva2pos), Direction.DOWN, Stva2pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tree2){
                    if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Stree2pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Stree2pos), Direction.DOWN, Stree2pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fyra2){
                    if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Sfyra2pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Sfyra2pos), Direction.DOWN, Sfyra2pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fem2){
                    if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Sfem2pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Sfem2pos), Direction.DOWN, Sfem2pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }

                //4th row
                if (ett3){
                    if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Sett3pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Sett3pos), Direction.DOWN, Sett3pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tva3){
                    if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Stva3pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Stva3pos), Direction.DOWN, Stva3pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tree3){
                    if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Stree3pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Stree3pos), Direction.DOWN, Stree3pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fyra3){
                    if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Sfyra3pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Sfyra3pos), Direction.DOWN, Sfyra3pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fem3){
                    if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Sfem3pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Sfem3pos), Direction.DOWN, Sfem3pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }

                //5th row
                if (ett4){
                    if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Sett4pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Sett4pos), Direction.DOWN, Sett4pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tva4){
                    if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Stva4pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Stva4pos), Direction.DOWN, Stva4pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tree4){
                    if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Stree4pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Stree4pos), Direction.DOWN, Stree4pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fyra4){
                    if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Sfyra4pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Sfyra4pos), Direction.DOWN, Sfyra4pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fem4){
                    if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Sfem4pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Sfem4pos), Direction.DOWN, Sfem4pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
            }

            if (playerdir == Direction.EAST){
                //1st row
                if (ett){
                    if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Eettpos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Eettpos), Direction.DOWN, Eettpos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tva){
                    if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Etvapos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Etvapos), Direction.DOWN, Etvapos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tree){
                    if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Etreepos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Etreepos), Direction.DOWN, Etreepos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fyra){
                    if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Efyrapos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Efyrapos), Direction.DOWN, Efyrapos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fem){
                    if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Efempos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Efempos), Direction.DOWN, Efempos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }

                //2nd row
                if (ett1){
                    if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Eett1pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Eett1pos), Direction.DOWN, Eett1pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tva1){
                    if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Etva1pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Etva1pos), Direction.DOWN, Etva1pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tree1){
                    if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Etree1pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Etree1pos), Direction.DOWN, Etree1pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fyra1){
                    if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Efyra1pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Efyra1pos), Direction.DOWN, Efyra1pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fem1){
                    if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Efem1pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Efem1pos), Direction.DOWN, Efem1pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }

                //3rd row
                if (ett2){
                    if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Eett2pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Eett2pos), Direction.DOWN, Eett2pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tva2){
                    if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Etva2pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Etva2pos), Direction.DOWN, Etva2pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tree2){
                    if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Etree2pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Etree2pos), Direction.DOWN, Etree2pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fyra2){
                    if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Efyra2pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Efyra2pos), Direction.DOWN, Efyra2pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fem2){
                    if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Efem2pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Efem2pos), Direction.DOWN, Efem2pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }

                //4th row
                if (ett3){
                    if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Eett3pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Eett3pos), Direction.DOWN, Eett3pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tva3){
                    if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Etva3pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Etva3pos), Direction.DOWN, Etva3pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tree3){
                    if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Etree3pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Etree3pos), Direction.DOWN, Etree3pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fyra3){
                    if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Efyra3pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Efyra3pos), Direction.DOWN, Efyra3pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fem3){
                    if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Efem3pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Efem3pos), Direction.DOWN, Efem3pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }

                //5th row
                if (ett4){
                    if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Eett4pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Eett4pos), Direction.DOWN, Eett4pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tva4){
                    if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Etva4pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Etva4pos), Direction.DOWN, Etva4pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tree4){
                    if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Etree4pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Etree4pos), Direction.DOWN, Etree4pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fyra4){
                    if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Efyra4pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Efyra4pos), Direction.DOWN, Efyra4pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fem4){
                    if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Efem4pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Efem4pos), Direction.DOWN, Efem4pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
            }

            if (playerdir == Direction.WEST){
                //1st row
                if (ett){
                    if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Wettpos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wettpos), Direction.DOWN, Wettpos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tva){
                    if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Wtvapos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wtvapos), Direction.DOWN, Wtvapos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tree){
                    if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Wtreepos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wtreepos), Direction.DOWN, Wtreepos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fyra){
                    if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Wfyrapos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wfyrapos), Direction.DOWN, Wfyrapos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fem){
                    if (blockticks==tickdelay.get()*1 && mc.world.getBlockState(Wfempos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wfempos), Direction.DOWN, Wfempos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }

                //2nd row
                if (ett1){
                    if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Wett1pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wett1pos), Direction.DOWN, Wett1pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tva1){
                    if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Wtva1pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wtva1pos), Direction.DOWN, Wtva1pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tree1){
                    if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Wtree1pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wtree1pos), Direction.DOWN, Wtree1pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fyra1){
                    if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Wfyra1pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wfyra1pos), Direction.DOWN, Wfyra1pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fem1){
                    if (blockticks==tickdelay.get()*2 && mc.world.getBlockState(Wfem1pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wfem1pos), Direction.DOWN, Wfem1pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }

                //3rd row
                if (ett2){
                    if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Wett2pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wett2pos), Direction.DOWN, Wett2pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tva2){
                    if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Wtva2pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wtva2pos), Direction.DOWN, Wtva2pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tree2){
                    if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Wtree2pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wtree2pos), Direction.DOWN, Wtree2pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fyra2){
                    if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Wfyra2pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wfyra2pos), Direction.DOWN, Wfyra2pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fem2){
                    if (blockticks==tickdelay.get()*3 && mc.world.getBlockState(Wfem2pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wfem2pos), Direction.DOWN, Wfem2pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }

                //4th row
                if (ett3){
                    if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Wett3pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wett3pos), Direction.DOWN, Wett3pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tva3){
                    if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Wtva3pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wtva3pos), Direction.DOWN, Wtva3pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tree3){
                    if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Wtree3pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wtree3pos), Direction.DOWN, Wtree3pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fyra3){
                    if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Wfyra3pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wfyra3pos), Direction.DOWN, Wfyra3pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fem3){
                    if (blockticks==tickdelay.get()*4 && mc.world.getBlockState(Wfem3pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wfem3pos), Direction.DOWN, Wfem3pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }

                //5th row
                if (ett4){
                    if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Wett4pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wett4pos), Direction.DOWN, Wett4pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tva4){
                    if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Wtva4pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wtva4pos), Direction.DOWN, Wtva4pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (tree4){
                    if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Wtree4pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wtree4pos), Direction.DOWN, Wtree4pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fyra4){
                    if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Wfyra4pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wfyra4pos), Direction.DOWN, Wfyra4pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
                }
                if (fem4){
                    if (blockticks==tickdelay.get()*5 && mc.world.getBlockState(Wfem4pos).isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(Wfem4pos), Direction.DOWN, Wfem4pos, false));
                            mc.player.swingHand(Hand.MAIN_HAND);}
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
        HitResult blockHit = mc.cameraEntity.raycast(reach.get(), 0, false);
        return ((BlockHitResult) blockHit).getBlockPos();
    }
    private void cascadingpileof() {
        if (!(mc.player.getInventory().getMainHandStack().getItem() instanceof BlockItem) || mc.player.getInventory().getMainHandStack().getItem() instanceof BedItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PowderSnowBucketItem || mc.player.getInventory().getMainHandStack().getItem() instanceof ScaffoldingItem || mc.player.getInventory().getMainHandStack().getItem() instanceof TallBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof VerticallyAttachableBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PlaceableOnWaterItem || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TorchBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRedstoneGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof RedstoneWireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRailBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractSignBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BellBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CarpetBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ConduitBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CoralParentBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireHookBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PointedDripstoneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PressurePlateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallMountedBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ShulkerBoxBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AmethystClusterBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BuddingAmethystBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusFlowerBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusPlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof LanternBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CandleBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CakeBlock  || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SugarCaneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SporeBlossomBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof KelpBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof GlowLichenBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CactusBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof  BambooBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof Waterloggable || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FallingBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FlowerPotBlock ||  ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof LadderBlock){
            mc.player.getInventory().selectedSlot = 0;
            if (!(mc.player.getInventory().getMainHandStack().getItem() instanceof BlockItem) || mc.player.getInventory().getMainHandStack().getItem() instanceof BedItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PowderSnowBucketItem || mc.player.getInventory().getMainHandStack().getItem() instanceof ScaffoldingItem || mc.player.getInventory().getMainHandStack().getItem() instanceof TallBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof VerticallyAttachableBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PlaceableOnWaterItem || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TorchBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRedstoneGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof RedstoneWireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRailBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractSignBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BellBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CarpetBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ConduitBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CoralParentBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireHookBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PointedDripstoneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PressurePlateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallMountedBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ShulkerBoxBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AmethystClusterBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BuddingAmethystBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusFlowerBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusPlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof LanternBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CandleBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CakeBlock  || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SugarCaneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SporeBlossomBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof KelpBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof GlowLichenBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CactusBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof  BambooBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof Waterloggable || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FallingBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FlowerPotBlock ||  ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof LadderBlock){
                mc.player.getInventory().selectedSlot = 1;
                if (!(mc.player.getInventory().getMainHandStack().getItem() instanceof BlockItem) || mc.player.getInventory().getMainHandStack().getItem() instanceof BedItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PowderSnowBucketItem || mc.player.getInventory().getMainHandStack().getItem() instanceof ScaffoldingItem || mc.player.getInventory().getMainHandStack().getItem() instanceof TallBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof VerticallyAttachableBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PlaceableOnWaterItem || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TorchBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRedstoneGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof RedstoneWireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRailBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractSignBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BellBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CarpetBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ConduitBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CoralParentBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireHookBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PointedDripstoneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PressurePlateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallMountedBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ShulkerBoxBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AmethystClusterBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BuddingAmethystBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusFlowerBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusPlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof LanternBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CandleBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CakeBlock  || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SugarCaneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SporeBlossomBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof KelpBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof GlowLichenBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CactusBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof  BambooBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof Waterloggable || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FallingBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FlowerPotBlock ||  ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof LadderBlock){
                    mc.player.getInventory().selectedSlot = 2;
                    if (!(mc.player.getInventory().getMainHandStack().getItem() instanceof BlockItem) || mc.player.getInventory().getMainHandStack().getItem() instanceof BedItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PowderSnowBucketItem || mc.player.getInventory().getMainHandStack().getItem() instanceof ScaffoldingItem || mc.player.getInventory().getMainHandStack().getItem() instanceof TallBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof VerticallyAttachableBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PlaceableOnWaterItem || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TorchBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRedstoneGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof RedstoneWireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRailBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractSignBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BellBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CarpetBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ConduitBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CoralParentBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireHookBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PointedDripstoneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PressurePlateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallMountedBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ShulkerBoxBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AmethystClusterBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BuddingAmethystBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusFlowerBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusPlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof LanternBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CandleBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CakeBlock  || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SugarCaneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SporeBlossomBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof KelpBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof GlowLichenBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CactusBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof  BambooBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof Waterloggable || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FallingBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FlowerPotBlock ||  ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof LadderBlock){
                        mc.player.getInventory().selectedSlot = 3;
                        if (!(mc.player.getInventory().getMainHandStack().getItem() instanceof BlockItem) || mc.player.getInventory().getMainHandStack().getItem() instanceof BedItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PowderSnowBucketItem || mc.player.getInventory().getMainHandStack().getItem() instanceof ScaffoldingItem || mc.player.getInventory().getMainHandStack().getItem() instanceof TallBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof VerticallyAttachableBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PlaceableOnWaterItem || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TorchBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRedstoneGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof RedstoneWireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRailBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractSignBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BellBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CarpetBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ConduitBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CoralParentBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireHookBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PointedDripstoneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PressurePlateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallMountedBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ShulkerBoxBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AmethystClusterBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BuddingAmethystBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusFlowerBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusPlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof LanternBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CandleBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CakeBlock  || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SugarCaneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SporeBlossomBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof KelpBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof GlowLichenBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CactusBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof  BambooBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof Waterloggable || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FallingBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FlowerPotBlock ||  ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof LadderBlock){
                            mc.player.getInventory().selectedSlot = 4;
                            if (!(mc.player.getInventory().getMainHandStack().getItem() instanceof BlockItem) || mc.player.getInventory().getMainHandStack().getItem() instanceof BedItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PowderSnowBucketItem || mc.player.getInventory().getMainHandStack().getItem() instanceof ScaffoldingItem || mc.player.getInventory().getMainHandStack().getItem() instanceof TallBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof VerticallyAttachableBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PlaceableOnWaterItem || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TorchBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRedstoneGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof RedstoneWireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRailBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractSignBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BellBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CarpetBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ConduitBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CoralParentBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireHookBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PointedDripstoneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PressurePlateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallMountedBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ShulkerBoxBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AmethystClusterBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BuddingAmethystBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusFlowerBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusPlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof LanternBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CandleBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CakeBlock  || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SugarCaneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SporeBlossomBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof KelpBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof GlowLichenBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CactusBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof  BambooBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof Waterloggable || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FallingBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FlowerPotBlock ||  ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof LadderBlock){
                                mc.player.getInventory().selectedSlot = 5;
                                if (!(mc.player.getInventory().getMainHandStack().getItem() instanceof BlockItem) || mc.player.getInventory().getMainHandStack().getItem() instanceof BedItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PowderSnowBucketItem || mc.player.getInventory().getMainHandStack().getItem() instanceof ScaffoldingItem || mc.player.getInventory().getMainHandStack().getItem() instanceof TallBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof VerticallyAttachableBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PlaceableOnWaterItem || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TorchBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRedstoneGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof RedstoneWireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRailBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractSignBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BellBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CarpetBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ConduitBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CoralParentBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireHookBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PointedDripstoneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PressurePlateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallMountedBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ShulkerBoxBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AmethystClusterBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BuddingAmethystBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusFlowerBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusPlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof LanternBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CandleBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CakeBlock  || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SugarCaneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SporeBlossomBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof KelpBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof GlowLichenBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CactusBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof  BambooBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof Waterloggable || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FallingBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FlowerPotBlock ||  ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof LadderBlock){
                                    mc.player.getInventory().selectedSlot = 6;
                                    if (!(mc.player.getInventory().getMainHandStack().getItem() instanceof BlockItem) || mc.player.getInventory().getMainHandStack().getItem() instanceof BedItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PowderSnowBucketItem || mc.player.getInventory().getMainHandStack().getItem() instanceof ScaffoldingItem || mc.player.getInventory().getMainHandStack().getItem() instanceof TallBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof VerticallyAttachableBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PlaceableOnWaterItem || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TorchBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRedstoneGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof RedstoneWireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRailBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractSignBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BellBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CarpetBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ConduitBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CoralParentBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireHookBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PointedDripstoneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PressurePlateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallMountedBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ShulkerBoxBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AmethystClusterBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BuddingAmethystBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusFlowerBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusPlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof LanternBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CandleBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CakeBlock  || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SugarCaneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SporeBlossomBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof KelpBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof GlowLichenBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CactusBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof  BambooBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof Waterloggable || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FallingBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FlowerPotBlock ||  ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof LadderBlock){
                                        mc.player.getInventory().selectedSlot = 7;
                                        if (!(mc.player.getInventory().getMainHandStack().getItem() instanceof BlockItem) || mc.player.getInventory().getMainHandStack().getItem() instanceof BedItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PowderSnowBucketItem || mc.player.getInventory().getMainHandStack().getItem() instanceof ScaffoldingItem || mc.player.getInventory().getMainHandStack().getItem() instanceof TallBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof VerticallyAttachableBlockItem || mc.player.getInventory().getMainHandStack().getItem() instanceof PlaceableOnWaterItem || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TorchBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRedstoneGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof RedstoneWireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FenceGateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractRailBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AbstractSignBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BellBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CarpetBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ConduitBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CoralParentBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireHookBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PointedDripstoneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof TripwireBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof PressurePlateBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof WallMountedBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ShulkerBoxBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof AmethystClusterBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof BuddingAmethystBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusFlowerBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof ChorusPlantBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof LanternBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CandleBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CakeBlock  || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SugarCaneBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof SporeBlossomBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof KelpBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof GlowLichenBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof CactusBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof  BambooBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof Waterloggable || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FallingBlock || ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof FlowerPotBlock ||  ((BlockItem) mc.player.getInventory().getMainHandStack().getItem()).getBlock() instanceof LadderBlock){
                                            mc.player.getInventory().selectedSlot = 8;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    public enum Modes {
        Horizontal, Vertical
    }
}