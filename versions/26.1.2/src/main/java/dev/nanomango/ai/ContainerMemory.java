package dev.nanomango.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public final class ContainerMemory {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir()
        .resolve("nanofutureaddon").resolve("containers.json");
    private static final int MAX_CONTAINERS = 50;

    private static JsonObject root;

    private ContainerMemory() {
    }

    public static boolean record(BlockPos pos, String dimension, String title, JsonArray items) {
        try {
            JsonObject containers = containers();
            String key = pos == null
                ? dimension + ":title:" + (title == null ? "container" : title)
                : dimension + ":" + pos.getX() + "," + pos.getY() + "," + pos.getZ();

            JsonObject container = new JsonObject();
            container.addProperty("dimension", dimension);
            container.addProperty("title", title == null ? "" : title);

            if (pos != null) {
                container.addProperty("x", pos.getX());
                container.addProperty("y", pos.getY());
                container.addProperty("z", pos.getZ());
            }

            container.add("items", items);

            if (!containers.has(key) && containers.size() >= MAX_CONTAINERS) return false;

            containers.add(key, container);
            save();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static JsonArray find(String itemId, int limit) {
        JsonArray matches = new JsonArray();

        for (Map.Entry<String, JsonElement> entry : containers().entrySet()) {
            JsonObject container = entry.getValue().getAsJsonObject();
            JsonArray items = container.getAsJsonArray("items");
            if (items == null) continue;

            for (JsonElement element : items) {
                if (matches.size() >= limit) return matches;

                JsonObject item = element.getAsJsonObject();
                if (!item.has("item") || !item.get("item").getAsString().equals(itemId)) continue;

                JsonObject match = item.deepCopy();

                JsonObject location = new JsonObject();
                location.addProperty("title", container.has("title") ? container.get("title").getAsString() : "");
                location.addProperty("dimension", container.has("dimension") ? container.get("dimension").getAsString() : "");

                if (container.has("x")) {
                    location.addProperty("x", container.get("x").getAsInt());
                    location.addProperty("y", container.get("y").getAsInt());
                    location.addProperty("z", container.get("z").getAsInt());
                }

                match.add("container", location);
                matches.add(match);
            }
        }

        return matches;
    }

    private static JsonObject containers() {
        if (root == null) load();

        String scope = MemoryStore.scope();
        JsonObject containers = root.getAsJsonObject(scope);

        if (containers == null) {
            containers = new JsonObject();
            root.add(scope, containers);
        }

        return containers;
    }

    private static void load() {
        try {
            root = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception ignored) {
            root = new JsonObject();
        }
    }

    private static void save() throws IOException {
        Files.createDirectories(FILE.getParent());

        Path temp = FILE.resolveSibling("containers.json.tmp");
        Files.writeString(temp, GSON.toJson(root), StandardCharsets.UTF_8);

        try {
            Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
