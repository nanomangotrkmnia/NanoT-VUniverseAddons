package dev.nanomango.modules;

import dev.nanomango.NanoFutureAddon;
import dev.nanomango.utils.SpawnScan;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class SpawnOverlay extends Module {
    private static final int SCAN_BUDGET = 2000;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> radius = sgGeneral.add(new IntSetting.Builder()
        .name("radius")
        .description("Horizontal radius to scan.")
        .defaultValue(16)
        .range(1, 32)
        .build()
    );

    private final Setting<Integer> vertical = sgGeneral.add(new IntSetting.Builder()
        .name("vertical")
        .description("Blocks above and below you to scan.")
        .defaultValue(8)
        .range(1, 16)
        .build()
    );

    private final Setting<Integer> minLight = sgGeneral.add(new IntSetting.Builder()
        .name("min-light")
        .description("Show blocks at or below this light level.")
        .defaultValue(7)
        .range(0, 15)
        .build()
    );

    private final SettingColor sideColor = new SettingColor(255, 80, 80, 55);
    private final SettingColor lineColor = new SettingColor(255, 100, 100, 230);

    private final SpawnScan scan = new SpawnScan();
    private final List<BlockPos> positions = new ArrayList<>();

    private int timer;
    private int idleTicks;
    private BlockPos lastCenter;
    private int lastRadius;
    private int lastVertical;
    private int lastLight;

    public SpawnOverlay() {
        super(NanoFutureAddon.CATEGORY, "spawn-overlay",
            "Renders blocks where hostile mobs can spawn.");
    }

    @Override
    public void onActivate() {
        scan.cancel();
        positions.clear();
        timer = 0;
        idleTicks = 0;
        lastCenter = null;
    }

    @Override
    public void onDeactivate() {
        scan.cancel();
        positions.clear();
        timer = 0;
        idleTicks = 0;
        lastCenter = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!Utils.canUpdate()) return;

        if (scan.active()) {
            if (scan.step(SCAN_BUDGET)) {
                positions.clear();
                positions.addAll(scan.positions());
            }

            return;
        }

        if (--timer > 0) return;
        timer = 40;

        BlockPos center = scanCenter();
        int radius = this.radius.get();
        int vertical = this.vertical.get();
        int light = minLight.get();

        boolean changed = lastCenter == null || !center.equals(lastCenter)
            || radius != lastRadius || vertical != lastVertical || light != lastLight;

        if (!changed && ++idleTicks < 200) return;

        idleTicks = 0;
        lastCenter = center.immutable();
        lastRadius = radius;
        lastVertical = vertical;
        lastLight = light;

        scan.start(center, radius, vertical, light, pos -> true);

        if (scan.step(SCAN_BUDGET)) {
            positions.clear();
            positions.addAll(scan.positions());
        }
    }

    private BlockPos scanCenter() {
        BlockPos player = mc.player.blockPosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(player.getX(), player.getY(), player.getZ());
        int bottom = mc.level.getMinY();

        while (pos.getY() > bottom
            && mc.level.getBlockState(pos).getCollisionShape(mc.level, pos).isEmpty()) {
            pos.setY(pos.getY() - 1);
        }

        return pos.immutable();
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!Utils.canUpdate()) return;

        for (BlockPos pos : positions) {
            event.renderer.box(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1,
                sideColor, lineColor, ShapeMode.Both, 0);
        }
    }

    @Override
    public String getInfoString() {
        return positions.size() + " spawns";
    }
}
