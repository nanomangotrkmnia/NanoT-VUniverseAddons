package dev.nanomango.utils;

import java.lang.reflect.Method;

public final class BaritoneBridge {
    private static Method clearArea;
    private static Method isBusy;
    private static Method stop;
    private static Method gotoBlock;
    private static Method gotoXZ;
    private static Method isPathing;
    private static Method status;
    private static Method follow;
    private static Method mine;

    static {
        try {
            Class<?> type = Class.forName("dev.nanomango.utils.BaritoneUtils");
            clearArea = type.getMethod("clearArea", int.class, int.class, int.class, int.class, int.class, int.class);
            isBusy = type.getMethod("isBusy");
            stop = type.getMethod("stop");
            gotoBlock = type.getMethod("gotoBlock", int.class, int.class, int.class);
            gotoXZ = type.getMethod("gotoXZ", int.class, int.class);
            isPathing = type.getMethod("isPathing");
            status = type.getMethod("status");
            follow = type.getMethod("follow", String.class);
            mine = type.getMethod("mine", String.class);
        } catch (Throwable ignored) {
            clearArea = null;
            isBusy = null;
            stop = null;
            gotoBlock = null;
            gotoXZ = null;
            isPathing = null;
            status = null;
            follow = null;
            mine = null;
        }
    }

    private BaritoneBridge() {
    }

    public static void clearArea(int minX, int minZ, int minY, int maxX, int maxZ, int maxY) {
        if (clearArea == null) return;

        try {
            clearArea.invoke(null, minX, minZ, minY, maxX, maxZ, maxY);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    public static boolean available() {
        return gotoBlock != null;
    }

    public static boolean isBusy() {
        if (isBusy == null) return false;

        try {
            return (boolean) isBusy.invoke(null);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    public static void stop() {
        if (stop == null) return;

        try {
            stop.invoke(null);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    public static void gotoBlock(int x, int y, int z) {
        if (gotoBlock == null) return;

        try {
            gotoBlock.invoke(null, x, y, z);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    public static void gotoXZ(int x, int z) {
        if (gotoXZ == null) return;

        try {
            gotoXZ.invoke(null, x, z);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    public static boolean isPathing() {
        if (isPathing == null) return false;

        try {
            return (boolean) isPathing.invoke(null);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    public static void follow(String name) {
        if (follow == null) return;

        try {
            follow.invoke(null, name);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    public static void mine(String block) {
        if (mine == null) return;

        try {
            mine.invoke(null, block);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    public static String status() {
        if (status == null) return "unavailable";

        try {
            Object value = status.invoke(null);
            return value == null ? "unknown" : value.toString();
        } catch (ReflectiveOperationException ignored) {
            return "unavailable";
        }
    }
}
