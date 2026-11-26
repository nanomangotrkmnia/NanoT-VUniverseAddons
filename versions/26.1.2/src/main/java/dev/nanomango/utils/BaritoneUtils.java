package dev.nanomango.utils;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.utils.BetterBlockPos;

public final class BaritoneUtils {
    private BaritoneUtils() {
    }

    public static void clearArea(int minX, int minZ, int minY, int maxX, int maxZ, int maxY) {
        IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
        baritone.getBuilderProcess().clearArea(new BetterBlockPos(minX, minY, minZ), new BetterBlockPos(maxX, maxY, maxZ));
    }

    public static boolean isBusy() {
        return BaritoneAPI.getProvider().getPrimaryBaritone().getBuilderProcess().isActive();
    }

    public static void gotoBlock(int x, int y, int z) {
        BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(x, y, z));
    }

    public static void gotoXZ(int x, int z) {
        BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalXZ(x, z));
    }

    public static boolean isPathing() {
        return BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing();
    }

    public static void follow(String name) {
        BaritoneAPI.getProvider().getPrimaryBaritone().getFollowProcess()
            .follow(entity -> entity.getName().getString().equalsIgnoreCase(name));
    }

    public static void mine(String block) {
        BaritoneAPI.getProvider().getPrimaryBaritone().getMineProcess().mineByName(1, block);
    }

    public static String status() {
        IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
        Goal goal = baritone.getCustomGoalProcess().getGoal();

        return goal == null ? "idle" : goal.toString();
    }

    public static void stop() {
        IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
        baritone.getBuilderProcess().onLostControl();
        baritone.getPathingBehavior().cancelEverything();
    }
}
