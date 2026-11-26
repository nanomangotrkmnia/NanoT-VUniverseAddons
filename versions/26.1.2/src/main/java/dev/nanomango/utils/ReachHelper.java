package dev.nanomango.utils;

import dev.nanomango.modules.InfiniteReach;
import meteordevelopment.meteorclient.mixininterface.IServerboundMovePlayerPacket;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.Reach;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public final class ReachHelper {
    private ReachHelper() {
    }

    public static double serverReach() {
        double range = mc.player.blockInteractionRange();
        Reach reach = Modules.get().get(Reach.class);

        if (reach != null) range -= reach.blockReach();

        return range + 1.0;
    }

    public static double maxReach(boolean teleport) {
        if (teleport) {
            InfiniteReach infinite = Modules.get().get(InfiniteReach.class);

            if (infinite != null && infinite.isActive()) {
                return Math.max(serverReach(), infinite.maxDistance());
            }
        }

        return serverReach();
    }

    public static boolean inServerReach(BlockPos pos) {
        double range = serverReach();
        return mc.player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) <= range * range;
    }

    public static boolean inReach(BlockPos pos, boolean teleport) {
        double range = maxReach(teleport);
        return mc.player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) <= range * range;
    }

    public static int breakTicks(BlockPos pos) {
        float delta = mc.level.getBlockState(pos).getDestroyProgress(mc.player, mc.level, pos);
        if (delta <= 0) return 1;

        return Math.max(1, (int) Math.ceil(0.75f / delta));
    }

    public static Direction startBreak(BlockPos pos, boolean teleport) {
        Direction direction = BlockUtils.getDirection(pos);

        if (inServerReach(pos)) {
            sendAction(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, direction);
            return direction;
        }

        if (!teleport) return null;
        if (!teleportAction(pos, Vec3.atCenterOf(pos), () ->
            sendAction(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, direction))) return null;

        return direction;
    }

    public static void stopBreak(BlockPos pos, Direction direction) {
        sendAction(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, direction);
    }

    public static boolean place(BlockPos pos, FindItemResult item, boolean teleport) {
        if (inServerReach(pos)) return BlockUtils.place(pos, item, false, 50);
        if (!teleport) return false;

        boolean[] placed = {false};
        teleportAction(pos, Vec3.atCenterOf(pos), () -> placed[0] = BlockUtils.place(pos, item, false, 50));

        return placed[0];
    }

    public static void abortBreak(BlockPos pos, Direction direction) {
        sendAction(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, pos, direction);
    }

    public static boolean teleportAction(BlockPos target, Vec3 hitPos, Runnable action) {
        Vec3 start = mc.player.position();
        Vec3 center = Vec3.atCenterOf(target);
        Vec3 destination = findPosition(target, center, hitPos, start);

        if (destination == null) return false;

        for (int i = 0; i < teleportPackets(); i++) sendStatus(false, false);

        sendMove(destination);
        action.run();

        Vec3 home = start.add(0.05, 0.01, 0.0);
        sendMove(start);
        sendMove(home);
        mc.player.setPos(home);

        return true;
    }

    public static Vec3 teleportOpen(BlockPos target, Vec3 hitPos, Runnable action) {
        Vec3 start = mc.player.position();
        Vec3 center = Vec3.atCenterOf(target);
        Vec3 destination = findPosition(target, center, hitPos, start);

        if (destination == null) return null;

        for (int i = 0; i < teleportPackets(); i++) sendStatus(false, false);

        sendMove(destination);
        action.run();

        return destination;
    }

    public static void hold(Vec3 pos) {
        ServerboundMovePlayerPacket.Pos packet =
            new ServerboundMovePlayerPacket.Pos(pos.x, pos.y, pos.z, true, false);

        ((IServerboundMovePlayerPacket) packet).meteor$setTag(1337);
        mc.getConnection().send(packet);
    }

    public static void restore(Vec3 from, Vec3 to) {
        int packets = statusCount(from.distanceTo(to));

        for (int i = 0; i < packets; i++) sendStatus(false, false);
        sendMove(to);
    }

    private static int statusCount(double distance) {
        int packets = (int) Math.ceil(distance * distance / 90.0);
        return Math.max(4, packets);
    }

    private static int teleportPackets() {
        InfiniteReach infinite = Modules.get().get(InfiniteReach.class);
        return infinite != null && infinite.isActive() ? infinite.packets() : 4;
    }

    private static Vec3 findPosition(BlockPos target, Vec3 center, Vec3 hitPos, Vec3 start) {
        AABB base = mc.player.getBoundingBox();
        double reach = serverReach() - 0.5;
        double limit = maxReach(true);
        Vec3 best = null;
        double bestDistance = Double.MAX_VALUE;

        for (double x = -3.5; x <= 3.5; x += 0.5) {
            for (double y = -3.5; y <= 3.5; y += 0.5) {
                for (double z = -3.5; z <= 3.5; z += 0.5) {
                    Vec3 pos = new Vec3(center.x + x, center.y + y, center.z + z);

                    if (BlockPos.containing(pos).equals(target)) continue;
                    if (pos.distanceToSqr(center) > reach * reach) continue;
                    if (pos.distanceToSqr(start) > limit * limit) continue;

                    AABB box = base.move(pos.x - start.x, pos.y - start.y, pos.z - start.z);
                    if (!mc.level.noCollision(mc.player, box)) continue;

                    double distance = pos.distanceToSqr(hitPos);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = pos;
                    }
                }
            }
        }

        return best;
    }

    private static void sendAction(ServerboundPlayerActionPacket.Action action, BlockPos pos, Direction direction) {
        mc.getConnection().send(new ServerboundPlayerActionPacket(action, pos, direction));
    }

    private static void sendMove(Vec3 pos) {
        ServerboundMovePlayerPacket.Pos packet =
            new ServerboundMovePlayerPacket.Pos(pos.x, pos.y, pos.z, false, false);

        ((IServerboundMovePlayerPacket) packet).meteor$setTag(1337);
        mc.getConnection().send(packet);
    }

    private static void sendStatus(boolean onGround, boolean collision) {
        ServerboundMovePlayerPacket.StatusOnly packet =
            new ServerboundMovePlayerPacket.StatusOnly(onGround, collision);

        ((IServerboundMovePlayerPacket) packet).meteor$setTag(1337);
        mc.getConnection().send(packet);
    }
}
