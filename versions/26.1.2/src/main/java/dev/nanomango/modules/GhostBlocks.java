package dev.nanomango.modules;

import dev.nanomango.NanoFutureAddon;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BlockSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

public class GhostBlocks extends Module {
    public enum PlaceMode {
        RightClick,
        SneakRightClick
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Block> block = sgGeneral.add(new BlockSetting.Builder()
        .name("block")
        .description("The block to place as a ghost.")
        .defaultValue(Blocks.STONE)
        .build()
    );

    private final Setting<PlaceMode> placeMode = sgGeneral.add(new EnumSetting.Builder<PlaceMode>()
        .name("place-mode")
        .description("When right click places a ghost block.")
        .defaultValue(PlaceMode.RightClick)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Ticks to wait between placements.")
        .defaultValue(2)
        .range(0, 20)
        .build()
    );

    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
        .name("swing")
        .description("Swings your hand when placing or breaking.")
        .defaultValue(true)
        .build()
    );

    private record Ghost(BlockState state, BlockState previous) {
    }

    private final Map<BlockPos, Ghost> ghosts = new HashMap<>();

    private int timer;

    public GhostBlocks() {
        super(NanoFutureAddon.CATEGORY, "ghost-blocks",
            "Places client-side ghost blocks that stay until broken in one hit.");
    }

    @Override
    public void onActivate() {
        ghosts.clear();
        timer = 0;
    }

    @Override
    public void onDeactivate() {
        if (mc.level != null) {
            for (Map.Entry<BlockPos, Ghost> entry : ghosts.entrySet()) {
                mc.level.setBlockAndUpdate(entry.getKey(), entry.getValue().previous());
            }
        }

        ghosts.clear();
        timer = 0;
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        ghosts.clear();
        timer = 0;
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        ghosts.clear();
        timer = 0;
    }

    @EventHandler
    private void onInteractBlock(InteractBlockEvent event) {
        if (!Utils.canUpdate() || mc.screen != null) return;
        if (placeMode.get() == PlaceMode.SneakRightClick && !mc.player.isShiftKeyDown()) return;
        if (!mc.options.keyUse.isDown()) return;

        BlockPos pos = placementPos(event.result);
        if (pos == null) return;

        event.cancel();

        if (timer > 0) return;

        ghosts.put(pos, new Ghost(block.get().defaultBlockState(), mc.level.getBlockState(pos)));
        mc.level.setBlockAndUpdate(pos, block.get().defaultBlockState());

        if (swing.get()) mc.player.swing(event.hand);
        timer = delay.get();
    }

    @EventHandler
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        Ghost ghost = ghosts.remove(event.blockPos);
        if (ghost == null) return;

        event.cancel();

        if (mc.gameMode.isDestroying()) mc.gameMode.stopDestroyBlock();
        mc.level.setBlockAndUpdate(event.blockPos, ghost.previous());

        if (swing.get()) mc.player.swing(InteractionHand.MAIN_HAND);
        timer = delay.get();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (timer > 0) timer--;
        if (ghosts.isEmpty() || mc.level == null) return;

        for (Map.Entry<BlockPos, Ghost> entry : ghosts.entrySet()) {
            BlockState state = entry.getValue().state();
            if (mc.level.getBlockState(entry.getKey()) != state) {
                mc.level.setBlockAndUpdate(entry.getKey(), state);
            }
        }
    }

    private BlockPos placementPos(BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos().relative(hit.getDirection());

        if (!mc.level.isInWorldBounds(pos)) return null;
        if (!mc.level.getBlockState(pos).canBeReplaced()) return null;
        if (mc.player.getBoundingBox().intersects(new AABB(pos))) return null;

        double range = mc.player.blockInteractionRange();
        if (mc.player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) > range * range) return null;

        return pos.immutable();
    }
}
