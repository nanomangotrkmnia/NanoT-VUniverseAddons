package dev.nanomango.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.world.BossEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.Map;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public final class BossBarTools {
    private BossBarTools() {
    }

    public static String bars(JsonObject arguments) {
        JsonObject out = new JsonObject();
        out.addProperty("available", false);

        try {
            BossHealthOverlay overlay = mc.gui.getBossOverlay();

            Field events = null;
            for (Field candidate : BossHealthOverlay.class.getDeclaredFields()) {
                if (Modifier.isStatic(candidate.getModifiers())) continue;
                if (!Map.class.isAssignableFrom(candidate.getType())) continue;
                events = candidate;
                break;
            }

            if (events == null) return out.toString();

            events.setAccessible(true);
            Map<?, ?> map = (Map<?, ?>) events.get(overlay);

            JsonArray array = new JsonArray();

            for (Object value : map.values()) {
                if (!(value instanceof BossEvent bar)) continue;

                float progress = value instanceof LerpingBossEvent lerp ? lerp.getProgress() : 0.0f;

                JsonObject object = new JsonObject();
                object.addProperty("name", bar.getName().getString());
                object.addProperty("percent", round(progress * 100.0));
                object.addProperty("color", bar.getColor().name().toLowerCase(Locale.ROOT));
                object.addProperty("darken_sky", bar.shouldDarkenScreen());
                array.add(object);
            }

            out.addProperty("available", true);
            out.addProperty("count", array.size());
            out.add("bars", array);
        } catch (Exception ignored) {
        }

        return out.toString();
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
