//Skidded from Germanminer with their permission! Thank you for making this.
//Original: https://github.com/Germanminer/MeteorServerUtils/blob/master/src/main/java/com/example/addon/modules/AutoWither.java
package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import pwn.noobs.trouserstreak.Trouser;

import java.util.ArrayList;
import java.util.List;

public class AutoWither extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgVisuals = this.settings.createGroup("Visuals");
    private final SettingGroup sgColors = this.settings.createGroup("Colors");
    private final Setting<Boolean> airPlace = sgGeneral.add(new BoolSetting.Builder()
            .name("Air Place")
            .description("Allow air placing.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> placeCenterSkull = sgGeneral.add(new BoolSetting.Builder()
            .name("Center Skull")
            .description("Place the skull in the center of the wither.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> lockRotation = sgGeneral.add(new BoolSetting.Builder()
            .name("Lock Rotation")
            .description("Locks the rotation of the wither.")
            .defaultValue(false)
            .build()
    );
    private final Setting<CardinalDirection> chosenDirection = sgGeneral.add(new EnumSetting.Builder<CardinalDirection>()
            .name("Direction")
            .description("The direction to be locked to (North means the withers arms will be west-east)")
            .defaultValue(CardinalDirection.North)
            .visible(() -> lockRotation.get())
            .build()
    );
    private final Setting<Boolean> renderPreview = sgVisuals.add(new BoolSetting.Builder()
            .name("Preview")
            .description("If the blocks should be rendered")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> swingHand = sgVisuals.add(new BoolSetting.Builder()
            .name("Swing")
            .description("Swing hand.")
            .defaultValue(true)
            .build()
    );
    private final Setting<SettingColor> previewColor = sgColors.add(new ColorSetting.Builder()
            .name("preview-color")
            .description("fill color for the preview.")
            .defaultValue(new SettingColor(51, 207, 255, 50, false))
            .build()
    );

    private final Setting<SettingColor> previewOutlineColor = sgColors.add(new ColorSetting.Builder()
            .name("preview-outline-color")
            .description("outline color for the preview.")
            .defaultValue(new SettingColor(112, 136, 255, 255, false))
            .build()
    );
    public AutoWither() {
        super(Trouser.Main, "Auto Wither", "Automatically builds a wither.");
    }

    private BlockPos previewPos;
    private Boolean isBuilding = false;
    private Boolean hasMaterials = false;
    @Override
    public void onActivate() {
        info("Press Usekey (RightClick) to build a wither");
        int soulSandCount = 0;
        int skullCount = 0;

        // Check the entire
        assert mc.player != null;
        for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.getItem() == Items.SOUL_SAND) {
                soulSandCount += stack.getCount();
            } else if (stack.getItem() == Items.WITHER_SKELETON_SKULL) {
                skullCount += stack.getCount();
            }
        }

        int witherCount = Math.min(soulSandCount / 4, skullCount / 3);
        if(witherCount>0){
            info("You can build " + witherCount + " withers");
            hasMaterials = true;
        }else{
            info("You don't have the required materials to build a wither in your hotbar");
            hasMaterials = false;
        }
    }
    @EventHandler
    private void onMouseButton(MouseClickEvent event){
        if(mc.screen != null) return;//Stop working in GUI
        if(event.button() != 1) return;
        if (isBuilding) return;
        if(event.action == KeyAction.Press){
            event.cancel();
            if(!hasMaterials){
                info("You don't have the required materials to build a wither in your hotbar");
                return;
            }
        }else{
            return;
        }
        if(previewPos != null){
            isBuilding = true;
            placeWither(previewPos,()->isBuilding=false);
        }
    }
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        hasMaterials = hasWitherMaterials();
    }
    @EventHandler
    private void onRender3d(Render3DEvent event) {
        if(renderPreview.get() == false||!hasMaterials) return;
        if (previewPos != null) {
            Direction direction;
            if(lockRotation.get()){
                direction = chosenDirection.get().toMcDirection();
            }else{
                assert mc.player != null;
                direction = mc.player.getDirection();
            }
            event.renderer.box(previewPos, previewColor.get(), previewOutlineColor.get(), ShapeMode.Both, 0);
            event.renderer.box(previewPos.above(), previewColor.get(), previewOutlineColor.get(), ShapeMode.Both, 0);
            if(direction == Direction.NORTH||direction == Direction.SOUTH){
                event.renderer.box(previewPos.above().west(), previewColor.get(), previewOutlineColor.get(), ShapeMode.Both, 0);
                event.renderer.box(previewPos.above().east(), previewColor.get(), previewOutlineColor.get(), ShapeMode.Both, 0);
                renderSkull(event, previewPos.above(2).west());
                renderSkull(event, previewPos.above(2).east());
            }else{
                event.renderer.box(previewPos.above().south(), previewColor.get(), previewOutlineColor.get(), ShapeMode.Both, 0);
                event.renderer.box(previewPos.above().north(), previewColor.get(), previewOutlineColor.get(), ShapeMode.Both, 0);
                renderSkull(event, previewPos.above(2).south());
                renderSkull(event, previewPos.above(2).north());
            }
            if(placeCenterSkull.get()){
                renderSkull(event, previewPos.above(2));
            }
        }
    }
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if(!renderPreview.get()) return;
        if(mc.hitResult instanceof BlockHitResult hit) {
            BlockPos pos = hit.getBlockPos();
            assert mc.level != null;
            BlockState state = mc.level.getBlockState(pos);
            if (airPlace.get()){
                if (state.isRedstoneConductor(mc.level, pos)) {
                    previewPos = pos.above();
                } else {
                    previewPos = pos;
                }
            } else {
                if (state.isRedstoneConductor(mc.level, pos)) {
                    previewPos = pos.above();
                } else {
                    previewPos = null;
                }
            }
        } else {
            previewPos = null;
        }
    }
    private void placeWither(BlockPos basePos,Runnable onComplete){
        if(!checkWitherBlocks(basePos)) {
            info("Obstruction prevented wither from being placed");
            onComplete.run();
            return;
        }
        assert mc.player != null;
        int originalSlot = mc.player.getInventory().getSelectedSlot();
        Direction direction;
        if(lockRotation.get()){
            direction = chosenDirection.get().toMcDirection();
        }else{
            direction = mc.player.getDirection();
        }
        List<BlockPos> blockPositions = new ArrayList<>();
        List<BlockPos> skullPositions = new ArrayList<>();
        if(direction == Direction.NORTH||direction == Direction.SOUTH){
            blockPositions.add(basePos.above().west());
            blockPositions.add(basePos.above().east());
            skullPositions.add(basePos.above().west());
            skullPositions.add(basePos.above().east());
        }else{
            blockPositions.add(basePos.above().south());
            blockPositions.add(basePos.above().north());
            skullPositions.add(basePos.above().south());
            skullPositions.add(basePos.above().north());
        }
        blockPositions.add(basePos);
        blockPositions.add(basePos.above());
        if(placeCenterSkull.get()){
            skullPositions.add(basePos.above());
        }

        mc.execute(() -> {
            for (BlockPos pos : blockPositions) {
                placeSoulBlock(pos);
            }
            for (BlockPos pos : skullPositions) {
                placeBlock(pos, Items.WITHER_SKELETON_SKULL);
            }
            mc.player.getInventory().setSelectedSlot(originalSlot);
            onComplete.run();
        });

    }
    private boolean checkWitherBlocks(BlockPos basePos){
        List<BlockPos> blockPositions = new ArrayList<>();
        Direction direction;
        if(lockRotation.get()){
            direction = chosenDirection.get().toMcDirection();
        }else{
            assert mc.player != null;
            direction = mc.player.getDirection();
        }
        if(direction == Direction.NORTH||direction == Direction.SOUTH){
            blockPositions.add(basePos.above().west());
            blockPositions.add(basePos.above().east());
            blockPositions.add(basePos.above(2).west());
            blockPositions.add(basePos.above(2).east());
            blockPositions.add(basePos.west());
            blockPositions.add(basePos.east());
        }else{
            blockPositions.add(basePos.above().south());
            blockPositions.add(basePos.above().north());
            blockPositions.add(basePos.above(2).south());
            blockPositions.add(basePos.above(2).north());
            blockPositions.add(basePos.south());
            blockPositions.add(basePos.north());
        }
        blockPositions.add(basePos);
        blockPositions.add(basePos.above());
        if(placeCenterSkull.get()){
            blockPositions.add(basePos.above(2));
        }
        for(BlockPos pos : blockPositions){
            if(!checkBlockPlaceable(pos)) {
                return false;
            }
        }
        blockPositions.remove(4);
        blockPositions.remove(5);
        for(BlockPos pos : blockPositions){
            if(!checkBlockForEntity(pos)) {
                return false;
            }
        }
        return true;
    }
    private boolean checkBlockPlaceable(BlockPos pos){
        //returns false if no block can be placed
        assert mc.level != null;
        return mc.level.getBlockState(pos).isAir();
    }
    private boolean checkBlockForEntity(BlockPos pos){
        //returns false if no block can be placed
        assert mc.level != null;
        List<Entity> entities = mc.level.getEntitiesOfClass(
                Entity.class,
                new AABB(pos),
                e -> !(e instanceof ItemEntity || e instanceof ExperienceOrb)
        );
        return entities.isEmpty();
    }
    private void placeSoulBlock(BlockPos pos){
        Item soulBlock = hasSoulSandHotbar() ? Items.SOUL_SAND : Items.SOUL_SOIL;
        placeBlock(pos, soulBlock);
    }
    private void placeBlock(BlockPos pos, Item item){
        int slot = findHotbarSlot(item);
        if (slot == -1) return; // player doesn't have item
        assert mc.player != null;
        mc.player.getInventory().setSelectedSlot(slot);
        assert mc.gameMode != null;
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        if(!swingHand.get()) return;
        mc.player.swing(InteractionHand.MAIN_HAND);
    }
    private int findHotbarSlot(Item item){
        for (int i = 0; i < 9; i++) {
            assert mc.player != null;
            if (mc.player.getInventory().getItem(i).getItem() == item) return i;
        }
        return -1;
    }
    private boolean hasWitherMaterials() {
        assert mc.player != null;

        int soulBlockCount = 0;
        int skullCount = 0;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            Item item = stack.getItem();

            if (item == Items.SOUL_SAND || item == Items.SOUL_SOIL) {
                soulBlockCount += stack.getCount();
            } else if (item == Items.WITHER_SKELETON_SKULL) {
                skullCount += stack.getCount();
            }

            // Stop if we have enough
            if (mc.player.hasInfiniteMaterials()){
                if (soulBlockCount >= 1 && skullCount >= 1) return true;
            } else {
                if (soulBlockCount >= 4 && skullCount >= 3) return true;
            }
        }

        return false;
    }
    private boolean hasSoulSandHotbar() {
        assert mc.player != null;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).getItem() == Items.SOUL_SAND) return true;
        }
        return false;
    }
    private void renderSkull(Render3DEvent event, BlockPos pos) {
        double shrink = 0.23; // how small the skull appears
        event.renderer.box(
                pos.getX() + shrink,
                pos.getY(),
                pos.getZ() + shrink,
                pos.getX() + 1 - shrink,
                pos.getY() + 0.5, // shorter than a full block
                pos.getZ() + 1 - shrink,
                previewColor.get(),
                previewOutlineColor.get(),
                ShapeMode.Both,
                0
        );
    }
    private enum CardinalDirection {
        North(Direction.NORTH),
        South(Direction.SOUTH),
        East(Direction.EAST),
        West(Direction.WEST);

        private final Direction mcDirection;

        CardinalDirection(Direction mcDirection) {
            this.mcDirection = mcDirection;
        }

        public Direction toMcDirection() {
            return mcDirection;
        }
    }
}