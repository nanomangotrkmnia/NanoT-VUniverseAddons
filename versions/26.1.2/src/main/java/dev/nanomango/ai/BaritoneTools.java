package dev.nanomango.ai;

import com.google.gson.JsonObject;
import dev.nanomango.utils.BaritoneBridge;

public final class BaritoneTools {
    private BaritoneTools() {
    }

    public static String gotoGoal(JsonObject arguments) {
        if (!BaritoneBridge.available()) return AiTools.error("Baritone is not available.");
        String error = AiTools.intError(arguments, "x", "z");
        if (error != null) return AiTools.error(error);

        int x = AiTools.integer(arguments, "x", 0, -30000000, 30000000);
        int z = AiTools.integer(arguments, "z", 0, -30000000, 30000000);

        JsonObject out = new JsonObject();
        out.addProperty("status", "started");
        out.addProperty("x", x);
        out.addProperty("z", z);

        if (AiTools.present(arguments, "y")) {
            int y;

            try {
                y = arguments.get("y").getAsInt();
            } catch (Exception e) {
                return AiTools.error("argument \"y\" must be an integer");
            }

            y = Math.max(-64, Math.min(320, y));
            BaritoneBridge.gotoBlock(x, y, z);
            out.addProperty("y", y);
            out.addProperty("goal", "block");
        } else {
            BaritoneBridge.gotoXZ(x, z);
            out.addProperty("goal", "xz");
        }

        out.add("defaults", AiTools.defaults(arguments, "y"));
        return out.toString();
    }

    public static String stop(JsonObject arguments) {
        if (!BaritoneBridge.available()) return AiTools.error("Baritone is not available.");

        BaritoneBridge.stop();

        JsonObject out = new JsonObject();
        out.addProperty("status", "stopped");
        return out.toString();
    }

    public static String status(JsonObject arguments) {
        JsonObject out = new JsonObject();
        out.addProperty("available", BaritoneBridge.available());

        if (!BaritoneBridge.available()) {
            out.addProperty("pathing", false);
            out.addProperty("building", false);
            return out.toString();
        }

        out.addProperty("pathing", BaritoneBridge.isPathing());
        out.addProperty("building", BaritoneBridge.isBusy());
        out.addProperty("goal", BaritoneBridge.status());
        return out.toString();
    }
}
