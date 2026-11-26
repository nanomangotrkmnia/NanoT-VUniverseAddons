package dev.nanomango.modules;

import dev.nanomango.NanoFutureAddon;
import dev.nanomango.utils.ReachHelper;
import dev.nanomango.utils.SpawnScan;
import dev.nanomango.utils.SpawnScanner;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class AutoTorch extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> radius = sgGeneral.add(new IntSetting.Builder()
        .name("radius")
        .description("Horizontal radius to spawn proof around you.")
        .defaultValue(8)
        .range(1, 24)
        .build()
    );

    private final Setting<Integer> vertical = sgGeneral.add(new IntSetting.Builder()
        .name("vertical")
        .description("Blocks above and below you to scan.")
        .defaultValue(4)
        .range(1, 16)
        .build()
    );

    private final Setting<Integer> minLight = sgGeneral.add(new IntSetting.Builder()
        .name("min-light")
        .description("Torches are placed where light is at or below this level. 7 is safe on old versions.")
        .defaultValue(7)
        .range(0, 15)
        .build()
    );

    private final Setting<Integer> perTick = sgGeneral.add(new IntSetting.Builder()
        .name("per-tick")
        .description("Torches to place per attempt.")
        .defaultValue(1)
        .range(1, 4)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Ticks between placement attempts.")
        .defaultValue(2)
        .range(0, 10)
        .build()
    );

    private final Setting<Boolean> useInfiniteReach = sgGeneral.add(new BoolSetting.Builder()
        .name("use-infinite-reach")
        .description("Places torches beyond normal reach when InfiniteReach is active.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotates towards each torch placement.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> render = sgGeneral.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders spawnable blocks while running.")
        .defaultValue(false)
        .build()
    );

    private final SettingColor sideColor = new SettingColor(255, 80, 80, 55);
    private final SettingColor lineColor = new SettingColor(255, 100, 100, 230);

    private static final int SCAN_BUDGET = 2000;

    private final List<BlockPos> targets = new ArrayList<>();
    private final List<BlockPos> renderPositions = new ArrayList<>();
    private final SpawnScan scan = new SpawnScan();

    private int timer;
    private int scanTimer;
    private boolean warned;

    public AutoTorch() {
        super(NanoFutureAddon.CATEGORY, "auto-torch",
            "Places torches on dark spawnable blocks around you.");
    }

    @Override
    public void onActivate() {
        scan.cancel();
        targets.clear();
        renderPositions.clear();
        timer = 0;
        scanTimer = 0;
        warned = false;
    }

    @Override
    public void onDeactivate() {
        scan.cancel();
        targets.clear();
        renderPositions.clear();
        timer = 0;
        scanTimer = 0;
        warned = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!Utils.canUpdate()) return;
        if (timer > 0) {
            timer--;
            return;
        }

        FindItemResult torch = InvUtils.findInHotbar(Items.TORCH);
        if (!torch.found()) {
            if (!warned) {
                warning("No torches in the hotbar or offhand.");
                warned = true;
            }

            timer = 20;
            return;
        }

        warned = false;

        if (targets.isEmpty()) {
            if (scan.active()) {
                if (!scan.step(SCAN_BUDGET)) return;

                fillTargets();
                if (targets.isEmpty()) {
                    scanTimer = 20;
                    return;
                }
            } else {
                if (scanTimer-- > 0) return;

                scan.start(mc.player.blockPosition(), radius.get(), vertical.get(), minLight.get(),
                    pos -> ReachHelper.inReach(pos, useInfiniteReach.get()));
                scanTimer = 20;

                if (scan.step(SCAN_BUDGET)) fillTargets();
                return;
            }
        }

        int placed = 0;
        Iterator<BlockPos> iterator = targets.iterator();

        while (iterator.hasNext() && placed < perTick.get()) {
            BlockPos pos = iterator.next();
            iterator.remove();

            if (!inRange(pos)) continue;
            if (!SpawnScanner.isSpawnable(pos, minLight.get())) continue;

            boolean far = !ReachHelper.inServerReach(pos);
            if (far && !useInfiniteReach.get()) continue;
            if (!ReachHelper.inReach(pos, useInfiniteReach.get())) continue;

            boolean ok = far
                ? ReachHelper.place(pos, torch, true)
                : BlockUtils.place(pos, torch, rotate.get(), 50);

            if (ok) placed++;
        }

        timer = delay.get();
    }

    private void fillTargets() {
        targets.clear();
        renderPositions.clear();

        List<BlockPos> found = scan.positions();

        if (render.get()) {
            for (int i = 0; i < Math.min(256, found.size()); i++) renderPositions.add(found.get(i));
        }

        targets.addAll(found);

        Vec3 eye = mc.player.getEyePosition();
        targets.sort(Comparator.comparingDouble(pos -> eye.distanceToSqr(Vec3.atCenterOf(pos))));
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get() || !Utils.canUpdate()) return;

        for (BlockPos pos : renderPositions) {
            event.renderer.box(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1,
                sideColor, lineColor, ShapeMode.Both, 0);
        }
    }

    private boolean inRange(BlockPos pos) {
        BlockPos center = mc.player.blockPosition();

        int x = pos.getX() - center.getX();
        int y = pos.getY() - center.getY();
        int z = pos.getZ() - center.getZ();

        return x * x + z * z <= radius.get() * radius.get() && Math.abs(y) <= vertical.get();
    }
}
