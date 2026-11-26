package dev.nanomango.modules;

import dev.nanomango.NanoFutureAddon;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;

import net.minecraft.world.InteractionHand;

import net.minecraft.world.phys.Vec3;

public class InstantBreak extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> fists = sgGeneral.add(new BoolSetting.Builder()
        .name("fists")
        .description("Packet-spams the target like Wurst SuperInstaMine. Works with an empty hand.")
        .defaultValue(false)
        .build()
    );

    private BlockPos target;
    private Direction direction;
    private int startAge;

    public InstantBreak() {
        super(NanoFutureAddon.CATEGORY, "instant-break",
            "Starts the break with a packet, then repeatedly STOPs once the server considers it finished.");
    }

    @Override
    public void onActivate() {
        target = null;
    }

    @Override
    public void onDeactivate() {
        if (target != null) abort();
        target = null;
    }

    @EventHandler
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        if (!Utils.canUpdate() || !BlockUtils.canBreak(event.blockPos)) return;

        event.cancel();

        if (event.blockPos.equals(target)) return;

        if (target != null) abort();

        target = event.blockPos;
        direction = event.direction;
        startAge = mc.player.tickCount;

        mc.getConnection().send(new ServerboundPlayerActionPacket(
            ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, target, direction));
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (target == null) return;

        if (!Utils.canUpdate() || mc.level.getBlockState(target).isAir()) {
            target = null;
            return;
        }

        Vec3 center = Vec3.atCenterOf(target);
        double range = mc.player.blockInteractionRange();

        if (mc.player.getEyePosition().distanceToSqr(center) > range * range) {
            abort();
            target = null;
            return;
        }

        float delta = mc.level.getBlockState(target).getDestroyProgress(mc.player, mc.level, target);

        if (delta <= 0) {
            abort();
            target = null;
            return;
        }

        if (fists.get()) {
            mc.player.swing(InteractionHand.MAIN_HAND);

            mc.getConnection().send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, target, direction));
            mc.getConnection().send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, target, direction));
            return;
        }

        mc.player.swing(InteractionHand.MAIN_HAND);

        float progress = delta * (mc.player.tickCount - startAge + 1);

        if (progress >= 0.7f) {
            mc.getConnection().send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, target, direction));
        }
    }

    private void abort() {
        if (mc.getConnection() != null) {
            mc.getConnection().send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, target, direction));
        }

        if (mc.gameMode != null) mc.gameMode.stopDestroyBlock();
    }
}
