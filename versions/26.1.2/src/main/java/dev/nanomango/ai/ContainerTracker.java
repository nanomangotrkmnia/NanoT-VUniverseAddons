package dev.nanomango.ai;

import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public final class ContainerTracker {
    private static BlockPos lastPos;
    private static String lastDimension;
    private boolean recorded;

    @EventHandler
    private void onInteract(InteractBlockEvent event) {
        if (event.result == null || mc.level == null) return;

        BlockPos pos = event.result.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);

        if (state.getMenuProvider(mc.level, pos) == null) return;

        lastPos = pos.immutable();
        lastDimension = mc.level.dimension().identifier().toString();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;

        if (mc.player.containerMenu == mc.player.inventoryMenu) {
            recorded = false;
            return;
        }

        if (recorded) return;

        AwarenessTools.container(new JsonObject());
        recorded = true;
    }

    public static BlockPos lastPos() {
        return lastPos;
    }

    public static String lastDimension() {
        return lastDimension;
    }
}
