package dev.nanomango.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public final class MemoryStore {
    public record Entry(String key, String value) {
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir()
        .resolve("nanofutureaddon").resolve("memory.json");
    private static final int MAX_NOTES = 100;

    private static JsonObject root;

    private MemoryStore() {
    }

    public static String scope() {
        if (mc.getCurrentServer() != null) {
            return "server:" + mc.getCurrentServer().ip.toLowerCase(Locale.ROOT);
        }

        if (mc.hasSingleplayerServer()) {
            return "world:" + singleplayerName().toLowerCase(Locale.ROOT);
        }

        return "local";
    }

    private static String singleplayerName() {
        try {
            Object server = mc.getClass().getMethod("getSingleplayerServer").invoke(mc);
            if (server == null) return "singleplayer";

            Object directory = server.getClass().getMethod("getServerDirectory").invoke(server);
            if (directory instanceof Path path && path.getFileName() != null) {
                return path.getFileName().toString();
            }
        } catch (Exception ignored) {
        }

        return "singleplayer";
    }

    public static synchronized void remember(String key, String value) throws IOException {
        JsonObject notes = notes();

        if (!notes.has(key) && notes.size() >= MAX_NOTES) {
            throw new IOException("memory is full (" + MAX_NOTES + " notes); forget one first");
        }

        notes.addProperty(key, value);
        save();
    }

    public static synchronized List<Entry> search(String query) {
        String text = query == null ? "" : query.toLowerCase(Locale.ROOT);
        List<Entry> entries = new ArrayList<>();

        for (Map.Entry<String, JsonElement> entry : notes().entrySet()) {
            String value = entry.getValue().getAsString();

            if (text.isEmpty()
                || entry.getKey().toLowerCase(Locale.ROOT).contains(text)
                || value.toLowerCase(Locale.ROOT).contains(text)) {
                entries.add(new Entry(entry.getKey(), value));
            }
        }

        return entries;
    }

    public static synchronized boolean forget(String key) throws IOException {
        JsonObject notes = notes();
        if (notes.remove(key) == null) return false;

        save();
        return true;
    }

    private static JsonObject notes() {
        if (root == null) load();

        String scope = scope();
        JsonObject notes = root.getAsJsonObject(scope);

        if (notes == null) {
            notes = new JsonObject();
            root.add(scope, notes);
        }

        return notes;
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

        Path temp = FILE.resolveSibling("memory.json.tmp");
        Files.writeString(temp, GSON.toJson(root), StandardCharsets.UTF_8);

        try {
            Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
