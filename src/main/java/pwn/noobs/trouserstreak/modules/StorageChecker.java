package pwn.noobs.trouserstreak.modules;

import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.*;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.*;
import net.minecraft.block.enums.ChestType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import pwn.noobs.trouserstreak.Trouser;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class StorageChecker extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Set<BlockPos> openedStorages = new HashSet<>();
    private final Color lineColor = new Color(0, 0, 0, 0);
    private final Color sideColor = new Color(0, 0, 0, 0);
    private boolean shouldRender;

    private final Setting<List<BlockEntityType<?>>> storageBlocks = sgGeneral.add(new StorageBlockListSetting.Builder()
            .name("storage-blocks")
            .description("Select storage blocks to display.")
            .defaultValue(StorageBlockListSetting.STORAGE_BLOCKS)
            .build()
    );

    private final Setting<Double> fadeDistance = sgGeneral.add(new DoubleSetting.Builder()
            .name("fade-distance")
            .description("The distance at which the color will fade.")
            .defaultValue(6)
            .min(0)
            .sliderMax(12)
            .build()
    );

    private final Setting<Boolean> tracers = sgRender.add(new BoolSetting.Builder()
            .name("tracers")
            .description("Draws tracers to storage blocks.")
            .defaultValue(false)
            .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build()
    );

    private final Setting<Integer> fillOpacity = sgRender.add(new IntSetting.Builder()
            .name("fill-opacity")
            .description("The opacity of the shape fill.")
            .defaultValue(50)
            .range(0, 255)
            .sliderMax(255)
            .build()
    );

    private final Setting<SettingColor> chestColor = sgRender.add(new ColorSetting.Builder()
            .name("chest-color")
            .description("Color for chests.")
            .defaultValue(new SettingColor(255, 160, 0, 255))
            .build()
    );

    private final Setting<SettingColor> trappedChestColor = sgRender.add(new ColorSetting.Builder()
            .name("trapped-chest-color")
            .description("Color for trapped chests.")
            .defaultValue(new SettingColor(255, 0, 0, 255))
            .build()
    );

    private final Setting<SettingColor> barrelColor = sgRender.add(new ColorSetting.Builder()
            .name("barrel-color")
            .description("Color for barrels.")
            .defaultValue(new SettingColor(255, 160, 0, 255))
            .build()
    );

    private final Setting<SettingColor> shulkerColor = sgRender.add(new ColorSetting.Builder()
            .name("shulker-color")
            .description("Color for shulker boxes.")
            .defaultValue(new SettingColor(255, 160, 0, 255))
            .build()
    );

    private final Setting<SettingColor> enderChestColor = sgRender.add(new ColorSetting.Builder()
            .name("ender-chest-color")
            .description("Color for ender chests.")
            .defaultValue(new SettingColor(120, 0, 255, 255))
            .build()
    );

    private final Setting<SettingColor> otherColor = sgRender.add(new ColorSetting.Builder()
            .name("other-color")
            .description("Color for other storage blocks.")
            .defaultValue(new SettingColor(140, 140, 140, 255))
            .build()
    );

    public StorageChecker() {
        super(Trouser.Main, "storage-checker", "Highlights unopened storage blocks. Able to be used alongside StorageESP.");
    }

    @Override
    public void onActivate() {
        openedStorages.clear();
    }

    private void getStorageColor(BlockEntity blockEntity) {
        shouldRender = false;
        if (!storageBlocks.get().contains(blockEntity.getType())) return;

        if (blockEntity instanceof TrappedChestBlockEntity) lineColor.set(trappedChestColor.get());
        else if (blockEntity instanceof ChestBlockEntity) lineColor.set(chestColor.get());
        else if (blockEntity instanceof BarrelBlockEntity) lineColor.set(barrelColor.get());
        else if (blockEntity instanceof ShulkerBoxBlockEntity) lineColor.set(shulkerColor.get());
        else if (blockEntity instanceof EnderChestBlockEntity) lineColor.set(enderChestColor.get());
        else if (blockEntity instanceof AbstractFurnaceBlockEntity
                || blockEntity instanceof DispenserBlockEntity
                || blockEntity instanceof HopperBlockEntity) lineColor.set(otherColor.get());
        else return;

        shouldRender = true;
        if (shapeMode.get() == ShapeMode.Sides || shapeMode.get() == ShapeMode.Both) {
            sideColor.set(lineColor);
            sideColor.a = fillOpacity.get();
        }
    }

    @EventHandler
    private void onBlockInteract(InteractBlockEvent event) {
        BlockPos pos = event.result.getBlockPos();
        BlockEntity blockEntity = mc.world.getBlockEntity(pos);
        if (blockEntity == null) return;

        openedStorages.add(pos);

        if (blockEntity instanceof ChestBlockEntity) {
            handleDoubleChest(pos);
        }
    }

    private void handleDoubleChest(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        ChestType chestType = state.get(ChestBlock.CHEST_TYPE);
        if (chestType != ChestType.SINGLE) {
            Direction facing = state.get(ChestBlock.FACING);
            BlockPos otherPos = pos.offset(chestType == ChestType.LEFT ?
                    facing.rotateYClockwise() :
                    facing.rotateYCounterclockwise());
            openedStorages.add(otherPos);
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        for (BlockEntity blockEntity : Utils.blockEntities()) {
            if (openedStorages.contains(blockEntity.getPos())) continue;

            getStorageColor(blockEntity);
            if (!shouldRender) continue;

            renderTracer(event, blockEntity);
            renderBox(event, blockEntity);
        }
    }

    private void renderTracer(Render3DEvent event, BlockEntity blockEntity) {
        if (!tracers.get()) return;

        double dist = PlayerUtils.squaredDistanceTo(
                blockEntity.getPos().getX() + 0.5,
                blockEntity.getPos().getY() + 0.5,
                blockEntity.getPos().getZ() + 0.5
        );

        double alpha = 1;
        if (dist <= fadeDistance.get() * fadeDistance.get()) {
            alpha = dist / (fadeDistance.get() * fadeDistance.get());
        }

        if (alpha >= 0.075) {
            int prevLineA = lineColor.a;
            lineColor.a *= alpha;

            event.renderer.line(
                    RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z,
                    blockEntity.getPos().getX() + 0.5,
                    blockEntity.getPos().getY() + 0.5,
                    blockEntity.getPos().getZ() + 0.5,
                    lineColor
            );

            lineColor.a = prevLineA;
        }
    }

    private void renderBox(Render3DEvent event, BlockEntity blockEntity) {
        BlockPos pos = blockEntity.getPos();
        double x1 = pos.getX();
        double y1 = pos.getY();
        double z1 = pos.getZ();
        double x2 = x1 + 1;
        double y2 = y1 + 1;
        double z2 = z1 + 1;

        int excludeDir = getExcludeDirection(blockEntity);
        event.renderer.box(x1, y1, z1, x2, y2, z2, sideColor, lineColor, shapeMode.get(), excludeDir);
    }

    private int getExcludeDirection(BlockEntity blockEntity) {
        if (blockEntity instanceof ChestBlockEntity) {
            BlockState state = mc.world.getBlockState(blockEntity.getPos());
            if (state.getBlock() instanceof ChestBlock && state.get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE) {
                return Direction.UP.getId();
            }
        }
        return 0;
    }
}
