package dev.nanomango.utils;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public final class SpawnScan {
    private static final int MAX_POSITIONS = 256;

    private final List<BlockPos> positions = new ArrayList<>();
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    private BlockPos center = BlockPos.ZERO;
    private int radius;
    private int vertical;
    private int minLight;
    private int x;
    private int z;
    private int y;
    private boolean active;
    private Predicate<BlockPos> filter = pos -> true;

    public void cancel() {
        active = false;
        positions.clear();
    }

    public void start(BlockPos center, int radius, int vertical, int minLight, Predicate<BlockPos> filter) {
        this.center = center.immutable();
        this.radius = radius;
        this.vertical = vertical;
        this.minLight = minLight;
        this.filter = filter;

        x = -radius;
        z = -radius;
        y = -vertical;
        active = true;
        positions.clear();
    }

    public boolean active() {
        return active;
    }

    public List<BlockPos> positions() {
        return positions;
    }

    public boolean step(int budget) {
        if (!active) return true;

        BlockPos player = mc.player.blockPosition();
        int processed = 0;

        while (active && processed < budget) {
            if (x * x + z * z <= radius * radius) {
                cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);

                if (filter.test(cursor) && SpawnScanner.isSpawnable(cursor, minLight, player)) {
                    positions.add(cursor.immutable());
                    if (positions.size() >= MAX_POSITIONS) active = false;
                }
            }

            advance();
            processed++;
        }

        return !active;
    }

    private void advance() {
        if (++y > vertical) {
            y = -vertical;

            if (++z > radius) {
                z = -radius;

                if (++x > radius) active = false;
            }
        }
    }
}
