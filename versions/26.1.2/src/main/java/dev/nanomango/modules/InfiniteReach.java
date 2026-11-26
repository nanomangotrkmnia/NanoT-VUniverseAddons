package dev.nanomango.modules;

import dev.nanomango.NanoFutureAddon;
import dev.nanomango.utils.ReachHelper;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IServerboundMovePlayerPacket;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class InfiniteReach extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> maxDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-distance")
        .description("Maximum reach in blocks.")
        .defaultValue(22)
        .min(4.5)
        .max(22)
        .build()
    );

    private final Setting<Integer> packets = sgGeneral.add(new IntSetting.Builder()
        .name("packets")
        .description("Status packets sent before the teleport. Vanilla allows 22 blocks with four.")
        .defaultValue(4)
        .range(1, 5)
        .build()
    );

    private final Setting<Integer> cooldown = sgGeneral.add(new IntSetting.Builder()
        .name("cooldown")
        .description("Ticks between actions.")
        .defaultValue(5)
        .range(1, 20)
        .build()
    );

    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
        .name("swing")
        .description("Swing your hand after acting.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> useBlocks = sgGeneral.add(new BoolSetting.Builder()
        .name("use-blocks")
        .description("Also use blocks on right click.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> containers = sgGeneral.add(new BoolSetting.Builder()
        .name("containers")
        .description("Opens chests and other containers from up to max-distance.")
        .defaultValue(true)
        .build()
    );

    private int cooldownTicks;

    private BlockPos remoteContainer;
    private Vec3 remoteFrom;
    private int remoteWait;
    private boolean remoteOpen;

    public InfiniteReach() {
        super(NanoFutureAddon.CATEGORY, "infinite-reach",
            "Teleports to distant blocks to break, use, or open containers, then teleports back.");
    }

    @Override
    public void onActivate() {
        cooldownTicks = 0;
        clearRemote();
    }

    @Override
    public void onDeactivate() {
        finishRemote(true);
        clearRemote();
        cooldownTicks = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!Utils.canUpdate()) {
            clearRemote();
            return;
        }

        if (remoteContainer != null) {
            tickRemoteContainer();
            return;
        }

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        boolean attack = mc.options.keyAttack.isDown();
        boolean use = mc.options.keyUse.isDown();
        if (!attack && !use) return;

        Vec3 eye = mc.player.getEyePosition();
        Vec3 end = eye.add(mc.player.getViewVector(1).scale(maxDistance.get()));

        BlockHitResult hit = mc.level.clip(new ClipContext(
            eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));

        if (hit.getType() != HitResult.Type.BLOCK) return;

        BlockPos pos = hit.getBlockPos();
        if (mc.level.getBlockState(pos).isAir()) return;

        Vec3 center = Vec3.atCenterOf(pos);
        double range = mc.player.blockInteractionRange();

        if (mc.player.getEyePosition().distanceToSqr(center) <= range * range) return;

        if (use && containers.get() && hasContainer(pos)) {
            Vec3 from = ReachHelper.teleportOpen(pos, hit.getLocation(), () ->
                mc.getConnection().send(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, hit, 0)));

            if (from == null) return;

            remoteContainer = pos.immutable();
            remoteFrom = from;
            remoteWait = 20;
            remoteOpen = false;

            if (swing.get()) mc.player.swing(InteractionHand.MAIN_HAND);
            cooldownTicks = cooldown.get();
            return;
        }

        if (!attack && !useBlocks.get()) return;

        boolean teleported = ReachHelper.teleportAction(pos, hit.getLocation(), () -> {
            if (attack) {
                mc.getConnection().send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, hit.getDirection()));
                mc.getConnection().send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, hit.getDirection()));
            } else {
                mc.getConnection().send(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, hit, 0));
            }
        });

        if (!teleported) return;

        if (swing.get()) mc.player.swing(InteractionHand.MAIN_HAND);
        cooldownTicks = cooldown.get();
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (remoteContainer == null) return;
        if (!(event.packet instanceof ServerboundMovePlayerPacket packet)) return;
        if (((IServerboundMovePlayerPacket) packet).meteor$getTag() == 1337) return;

        event.cancel();
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        clearRemote();
        cooldownTicks = 0;
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        clearRemote();
        cooldownTicks = 0;
    }

    private void tickRemoteContainer() {
        ReachHelper.hold(remoteFrom);

        if (!remoteOpen) {
            if (isContainerOpen()) {
                remoteOpen = true;
                return;
            }

            if (--remoteWait <= 0) finishRemote(false);
            return;
        }

        if (!isContainerOpen()) finishRemote(false);
    }

    private boolean isContainerOpen() {
        return mc.player.containerMenu != mc.player.inventoryMenu;
    }

    private boolean hasContainer(BlockPos pos) {
        return mc.level.getBlockState(pos).getMenuProvider(mc.level, pos) != null;
    }

    private void finishRemote(boolean closeScreen) {
        if (remoteContainer == null) return;

        if (closeScreen && Utils.canUpdate() && isContainerOpen()) {
            mc.player.closeContainer();
        }

        if (remoteFrom != null && Utils.canUpdate() && mc.getConnection() != null) {
            ReachHelper.restore(remoteFrom, mc.player.position());
        }

        clearRemote();
    }

    private void clearRemote() {
        remoteContainer = null;
        remoteFrom = null;
        remoteWait = 0;
        remoteOpen = false;
    }

    public double maxDistance() {
        return maxDistance.get();
    }

    public int packets() {
        return packets.get();
    }
}
