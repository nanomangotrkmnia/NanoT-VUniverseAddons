package dev.nanomango.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

public final class MemoryTools {
    private static final int MAX_KEY = 64;
    private static final int MAX_VALUE = 500;
    private static final int MAX_RESULTS = 20;

    private MemoryTools() {
    }

    public static String remember(JsonObject arguments) {
        if (!AiTools.present(arguments, "key")) return AiTools.error("missing required argument \"key\"");

        String key = AiTools.string(arguments, "key", "").trim();
        if (key.isEmpty() || key.length() > MAX_KEY) {
            return AiTools.error("key must be 1-" + MAX_KEY + " characters");
        }

        if (!AiTools.present(arguments, "value")) return AiTools.error("missing required argument \"value\"");

        String value = AiTools.string(arguments, "value", "").trim();
        if (value.isEmpty()) return AiTools.error("value must not be blank");
        if (value.length() > MAX_VALUE) return AiTools.error("value must be " + MAX_VALUE + " characters or fewer");

        try {
            MemoryStore.remember(key, value);
        } catch (Exception e) {
            return AiTools.error("could not save memory: " + message(e));
        }

        JsonObject out = new JsonObject();
        out.addProperty("stored", true);
        out.addProperty("scope", MemoryStore.scope());
        out.addProperty("key", key);
        return out.toString();
    }

    public static String recall(JsonObject arguments) {
        String query = AiTools.string(arguments, "query", "").trim();
        List<MemoryStore.Entry> entries = MemoryStore.search(query);

        JsonArray array = new JsonArray();
        for (int i = 0; i < Math.min(MAX_RESULTS, entries.size()); i++) {
            MemoryStore.Entry entry = entries.get(i);

            JsonObject object = new JsonObject();
            object.addProperty("key", entry.key());
            object.addProperty("value", entry.value());
            array.add(object);
        }

        JsonObject out = new JsonObject();
        out.addProperty("scope", MemoryStore.scope());
        out.addProperty("query", query);
        out.addProperty("count", entries.size());
        out.addProperty("truncated", entries.size() > MAX_RESULTS);
        out.add("defaults", AiTools.defaults(arguments, "query"));
        out.add("entries", array);
        return out.toString();
    }

    public static String forget(JsonObject arguments) {
        if (!AiTools.present(arguments, "key")) return AiTools.error("missing required argument \"key\"");

        String key = AiTools.string(arguments, "key", "").trim();
        if (key.isEmpty()) return AiTools.error("key must not be blank");

        boolean removed;

        try {
            removed = MemoryStore.forget(key);
        } catch (Exception e) {
            return AiTools.error("could not save memory: " + message(e));
        }

        JsonObject out = new JsonObject();
        out.addProperty("removed", removed);
        out.addProperty("scope", MemoryStore.scope());
        out.addProperty("key", key);
        return out.toString();
    }

    private static String message(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }
}
