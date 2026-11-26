package dev.nanomango.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.nanomango.utils.BaritoneBridge;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public final class WaypointTools {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir()
        .resolve("nanofutureaddon").resolve("waypoints.json");
    private static final int MAX_WAYPOINTS = 100;

    private static JsonObject root;

    private WaypointTools() {
    }

    public static String save(JsonObject arguments) {
        if (mc.player == null) return AiTools.error("Not in a world.");
        if (!AiTools.present(arguments, "name")) return AiTools.error("missing required argument \"name\"");

        String name = AiTools.string(arguments, "name", "").trim();
        if (name.isEmpty() || name.length() > 32) return AiTools.error("name must be 1-32 characters");

        try {
            if (!notes().has(name) && notes().size() >= MAX_WAYPOINTS) {
                return AiTools.error("waypoint list is full; forget one first");
            }

            JsonObject waypoint = new JsonObject();
            waypoint.addProperty("x", mc.player.blockPosition().getX());
            waypoint.addProperty("y", mc.player.blockPosition().getY());
            waypoint.addProperty("z", mc.player.blockPosition().getZ());
            waypoint.addProperty("dimension", dimension());

            notes().add(name, waypoint);
            saveFile();

            JsonObject out = new JsonObject();
            out.addProperty("saved", true);
            out.addProperty("name", name);
            out.add("waypoint", waypoint);
            return out.toString();
        } catch (Exception e) {
            return AiTools.error("could not save waypoint: " + message(e));
        }
    }

    public static String list(JsonObject arguments) {
        if (mc.player == null) return AiTools.error("Not in a world.");

        JsonObject notes = notes();
        JsonArrayBuilder array = new JsonArrayBuilder();
        String dimension = dimension();

        for (Map.Entry<String, JsonElement> entry : notes.entrySet()) {
            JsonObject waypoint = entry.getValue().getAsJsonObject();
            int x = waypoint.get("x").getAsInt();
            int y = waypoint.get("y").getAsInt();
            int z = waypoint.get("z").getAsInt();
            String storedDimension = waypoint.has("dimension") ? waypoint.get("dimension").getAsString() : "";

            JsonObject object = new JsonObject();
            object.addProperty("name", entry.getKey());
            object.addProperty("x", x);
            object.addProperty("y", y);
            object.addProperty("z", z);
            object.addProperty("dimension", storedDimension);
            object.addProperty("same_dimension", storedDimension.equals(dimension));
            object.addProperty("distance", round(Math.sqrt(
                Math.pow(x + 0.5 - mc.player.getX(), 2)
                    + Math.pow(y + 0.5 - mc.player.getY(), 2)
                    + Math.pow(z + 0.5 - mc.player.getZ(), 2))));
            array.add(object);
        }

        JsonObject out = new JsonObject();
        out.addProperty("count", notes.size());
        out.add("waypoints", array.build());
        return out.toString();
    }

    public static String gotoWaypoint(JsonObject arguments) {
        if (mc.player == null) return AiTools.error("Not in a world.");
        if (!BaritoneBridge.available()) return AiTools.error("Baritone is not available.");
        if (!AiTools.present(arguments, "name")) return AiTools.error("missing required argument \"name\"");

        String name = AiTools.string(arguments, "name", "").trim();
        JsonObject waypoint = notes().getAsJsonObject(name);
        if (waypoint == null) return AiTools.error("unknown waypoint \"" + name + "\"");

        String storedDimension = waypoint.has("dimension") ? waypoint.get("dimension").getAsString() : "";
        if (!storedDimension.equals(dimension())) {
            return AiTools.error("waypoint \"" + name + "\" is in " + storedDimension);
        }

        int x = waypoint.get("x").getAsInt();
        int y = waypoint.get("y").getAsInt();
        int z = waypoint.get("z").getAsInt();
        BaritoneBridge.gotoBlock(x, y, z);

        JsonObject out = new JsonObject();
        out.addProperty("status", "started");
        out.addProperty("name", name);
        out.addProperty("x", x);
        out.addProperty("y", y);
        out.addProperty("z", z);
        return out.toString();
    }

    private static JsonObject notes() {
        if (root == null) loadFile();

        String scope = MemoryStore.scope();
        JsonObject notes = root.getAsJsonObject(scope);

        if (notes == null) {
            notes = new JsonObject();
            root.add(scope, notes);
        }

        return notes;
    }

    private static void loadFile() {
        try {
            root = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception ignored) {
            root = new JsonObject();
        }
    }

    private static void saveFile() throws IOException {
        Files.createDirectories(FILE.getParent());

        Path temp = FILE.resolveSibling("waypoints.json.tmp");
        Files.writeString(temp, GSON.toJson(root), StandardCharsets.UTF_8);

        try {
            Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String dimension() {
        return mc.level.dimension().identifier().toString();
    }

    private static String message(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static final class JsonArrayBuilder {
        private final com.google.gson.JsonArray array = new com.google.gson.JsonArray();

        void add(JsonObject object) {
            array.add(object);
        }

        com.google.gson.JsonArray build() {
            return array;
        }
    }
}
