package dev.nanomango.modules;

import dev.nanomango.NanoFutureAddon;
import dev.nanomango.utils.ReachHelper;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AutoTerraform extends Module {
    public enum Mode {
        Flatten,
        Clear,
        Fill
    }

    public enum MineMode {
        Legit,
        Instant
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Flatten breaks and fills, Clear only breaks, Fill only places.")
        .defaultValue(Mode.Flatten)
        .build()
    );

    private final Setting<Integer> offset = sgGeneral.add(new IntSetting.Builder()
        .name("offset")
        .description("Target layer relative to the block you stand on.")
        .defaultValue(0)
        .range(-8, 8)
        .build()
    );

    private final Setting<Integer> radius = sgGeneral.add(new IntSetting.Builder()
        .name("radius")
        .description("Horizontal radius around you.")
        .defaultValue(4)
        .range(1, 6)
        .build()
    );

    private final Setting<Integer> height = sgGeneral.add(new IntSetting.Builder()
        .name("height")
        .description("Layers above the target to clear.")
        .defaultValue(3)
        .range(1, 16)
        .visible(() -> mode.get() != Mode.Fill)
        .build()
    );

    private final Setting<MineMode> mineMode = sgGeneral.add(new EnumSetting.Builder<MineMode>()
        .name("mine-mode")
        .description("Instant stops at 0.75 progress. Legit drives vanilla mining.")
        .defaultValue(MineMode.Instant)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Ticks to wait after a block is finished.")
        .defaultValue(0)
        .range(0, 20)
        .build()
    );

    private final Setting<Integer> placePerTick = sgGeneral.add(new IntSetting.Builder()
        .name("place-per-tick")
        .description("Blocks to place per tick.")
        .defaultValue(4)
        .range(1, 16)
        .visible(() -> mode.get() != Mode.Clear)
        .build()
    );

    private final Setting<Boolean> useInfiniteReach = sgGeneral.add(new BoolSetting.Builder()
        .name("use-infinite-reach")
        .description("Uses InfiniteReach to break and place blocks up to its max distance.")
        .defaultValue(true)
        .build()
    );

    private final Setting<List<Block>> placeBlocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("place-blocks")
        .description("Blocks used to fill holes, in order of preference. Must be in your hotbar or offhand.")
        .defaultValue(Blocks.DIRT, Blocks.COBBLESTONE, Blocks.STONE)
        .visible(() -> mode.get() != Mode.Clear)
        .build()
    );

    private final Setting<Boolean> skipBlockEntities = sgGeneral.add(new BoolSetting.Builder()
        .name("skip-block-entities")
        .description("Does not break chests, spawners, signs, and other blocks with data.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
        .name("swing")
        .description("Swings your hand while breaking.")
        .defaultValue(true)
        .build()
    );

    private final Set<BlockPos> ignored = new HashSet<>();

    private BlockPos target;
    private Direction targetDirection;
    private int timer;
    private int targetTicks;
    private int targetLimit = 400;
    private int breakWait;
    private int teleportCooldown;

    public AutoTerraform() {
        super(NanoFutureAddon.CATEGORY, "auto-terraform",
            "Clears, fills, or levels the blocks around you to a target layer.");
    }

    @Override
    public void onActivate() {
        target = null;
        timer = 0;
        targetTicks = 0;
        breakWait = 0;
        teleportCooldown = 0;
        ignored.clear();
    }

    @Override
    public void onDeactivate() {
        clearTarget();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!Utils.canUpdate()) {
            clearTarget();
            return;
        }

        if (teleportCooldown > 0) teleportCooldown--;

        int layer = groundLevel() + offset.get();

        if (target != null) {
            tickTarget();
            return;
        }

        if (timer > 0) {
            timer--;
            return;
        }

        if (mode.get() != Mode.Fill) {
            BlockPos pos = findBreak(layer);

            if (pos != null) {
                startTarget(pos);
                return;
            }
        }

        if (mode.get() != Mode.Clear) placeBatch(layer);
    }

    private void tickTarget() {
        BlockState state = mc.level.getBlockState(target);

        if (state.isAir()) {
            clearTarget();
            timer = delay.get();
            return;
        }

        if (mineMode.get() == MineMode.Legit) {
            targetTicks++;

            if (targetTicks > targetLimit || !canBreak(target)) {
                if (targetTicks > targetLimit) ignored.add(target);
                clearTarget();
                timer = delay.get();
                return;
            }

            BlockUtils.breakBlock(target, swing.get());
            return;
        }

        if (--breakWait <= 0) {
            ReachHelper.stopBreak(target, targetDirection);
            clearTarget();
            timer = Math.max(1, delay.get());
        }
    }

    private void startTarget(BlockPos pos) {
        if (mineMode.get() == MineMode.Legit) {
            target = pos;
            targetTicks = 0;
            targetLimit = breakLimit(pos);
            BlockUtils.breakBlock(pos, swing.get());
            return;
        }

        boolean far = !ReachHelper.inServerReach(pos);
        Direction direction = ReachHelper.startBreak(pos, far && canTeleport());

        if (direction == null) {
            ignored.add(pos);
            return;
        }

        target = pos;
        targetDirection = direction;
        breakWait = ReachHelper.breakTicks(pos);

        if (far) teleportCooldown = 5;
        if (swing.get()) mc.player.swing(InteractionHand.MAIN_HAND);
    }

    private BlockPos findBreak(int layer) {
        int radius = this.radius.get();
        BlockPos feet = mc.player.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > radius * radius) continue;

                for (int y = 1; y <= height.get(); y++) {
                    BlockPos pos = new BlockPos(feet.getX() + x, layer + y, feet.getZ() + z);

                    if (ignored.contains(pos) || !canBreak(pos)) continue;

                    double distance = mc.player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos));

                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = pos;
                    }
                }
            }
        }

        return best;
    }

    private void placeBatch(int layer) {
        int radius = this.radius.get();
        BlockPos feet = mc.player.blockPosition();
        List<BlockPos> positions = new ArrayList<>();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > radius * radius) continue;

                BlockPos pos = new BlockPos(feet.getX() + x, layer, feet.getZ() + z);

                if (!mc.level.getBlockState(pos).canBeReplaced()) continue;
                if (!ReachHelper.inReach(pos, canTeleport())) continue;

                positions.add(pos);
            }
        }

        positions.sort(Comparator.comparingDouble(pos ->
            mc.player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos))));

        int placed = 0;
        boolean teleported = false;

        for (BlockPos pos : positions) {
            if (placed >= placePerTick.get()) break;

            boolean far = !ReachHelper.inServerReach(pos);
            if (far && (teleported || !canTeleport())) continue;

            for (Block block : placeBlocks.get()) {
                FindItemResult item = InvUtils.findInHotbar(block.asItem());
                if (!item.found()) continue;

                if (ReachHelper.place(pos, item, far && canTeleport())) {
                    placed++;

                    if (far) {
                        teleported = true;
                        teleportCooldown = 5;
                    }

                    break;
                }
            }
        }
    }

    private boolean canTeleport() {
        return useInfiniteReach.get() && teleportCooldown == 0;
    }

    private boolean canBreak(BlockPos pos) {
        BlockState state = mc.level.getBlockState(pos);

        if (state.isAir() || !BlockUtils.canBreak(pos, state)) return false;
        if (skipBlockEntities.get() && state.hasBlockEntity()) return false;

        return ReachHelper.inReach(pos, mineMode.get() == MineMode.Instant && canTeleport());
    }

    private int breakLimit(BlockPos pos) {
        float delta = mc.level.getBlockState(pos).getDestroyProgress(mc.player, mc.level, pos);
        if (delta <= 0) return 40;

        return Math.min(600, (int) Math.ceil(1 / delta) + 40);
    }

    private int groundLevel() {
        BlockPos pos = mc.player.blockPosition();

        while (pos.getY() > mc.level.getMinY()
            && mc.level.getBlockState(pos).getCollisionShape(mc.level, pos).isEmpty()) {
            pos = pos.below();
        }

        return pos.getY();
    }

    private void clearTarget() {
        targetTicks = 0;

        if (mineMode.get() == MineMode.Legit && target != null
            && mc.gameMode != null && mc.gameMode.isDestroying()) {
            mc.gameMode.stopDestroyBlock();
        }

        target = null;
    }
}
