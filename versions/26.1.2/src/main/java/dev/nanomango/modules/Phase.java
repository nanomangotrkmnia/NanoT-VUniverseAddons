package dev.nanomango.modules;

import dev.nanomango.NanoFutureAddon;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Phase extends Module {
    private static final double MAX_DISTANCE = Math.sqrt(500);

    private Vec3 direction = Vec3.ZERO;
    private int preJumpTicks;
    private int postJumpTicks;
    private boolean needsButtonRelease;
    private boolean allowJumpPacket;

    public Phase() {
        super(NanoFutureAddon.CATEGORY, "phase",
            "Teleports through walls while your hitbox is inside a block (e.g. sand on your head). Abuses the vanilla movement packet limit.",
            "noclip");
    }

    @Override
    public void onActivate() {
        reset();
    }

    @Override
    public void onDeactivate() {
        reset();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (postJumpTicks > 0) postJumpTicks--;

        if (!Utils.canUpdate() || !mc.player.isAlive() || mc.player.isSpectator() || mc.screen != null) {
            preJumpTicks = 0;
            return;
        }

        double forward = (mc.options.keyUp.isDown() ? 1 : 0) - (mc.options.keyDown.isDown() ? 1 : 0);
        double left = (mc.options.keyLeft.isDown() ? 1 : 0) - (mc.options.keyRight.isDown() ? 1 : 0);
        double up = (mc.options.keyJump.isDown() ? 1 : 0) - (mc.options.keyShift.isDown() ? 1 : 0);

        direction = new Vec3(left, up, forward).normalize()
            .yRot(-(float) Math.toRadians(mc.player.getYRot()));

        if (direction.lengthSqr() == 0) {
            needsButtonRelease = false;
            preJumpTicks = 0;
            return;
        }

        if (needsButtonRelease) return;

        if (mc.level.noCollision(mc.player, mc.player.getBoundingBox().contract(1e-5, 1e-5, 1e-5))) {
            preJumpTicks = 0;
            return;
        }

        preJumpTicks++;

        if (preJumpTicks <= 2) return;

        preJumpTicks = 0;
        needsButtonRelease = true;

        Vec3 destination = findDestination();
        if (destination == null) return;

        for (int i = 0; i < 4; i++) {
            mc.getConnection().send(new ServerboundMovePlayerPacket.StatusOnly(false, false));
        }

        mc.player.setPos(destination);
        allowJumpPacket = true;
        postJumpTicks = 2;
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!(event.packet instanceof ServerboundMovePlayerPacket packet)) return;

        if (allowJumpPacket && (packet instanceof ServerboundMovePlayerPacket.PosRot
            || packet instanceof ServerboundMovePlayerPacket.Pos)) {
            allowJumpPacket = false;
            return;
        }

        if (preJumpTicks > 0 || postJumpTicks > 0) event.cancel();
    }

    private Vec3 findDestination() {
        Vec3 start = mc.player.position();
        AABB box = mc.player.getBoundingBox();

        for (double distance = 0.25; distance <= MAX_DISTANCE; distance += 0.25) {
            Vec3 offset = direction.scale(distance);
            AABB moved = box.move(offset.x, offset.y, offset.z);

            if (mc.level.noCollision(mc.player, moved)) return start.add(offset);
        }

        return null;
    }

    private void reset() {
        direction = Vec3.ZERO;
        preJumpTicks = 0;
        postJumpTicks = 0;
        needsButtonRelease = false;
        allowJumpPacket = false;
    }
}
