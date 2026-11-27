package dev.nanomango.ads;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

/**
 * Reads {@code config/future-ads.properties}. The file is shared by NanoFutureAddon and the standalone
 * FutureAds mod, and is re-read every time a screen with ads opens so edits apply without a restart.
 */
public final class AdConfig {
    public static final String FILE_NAME = "future-ads.properties";

    public boolean enabled = true;
    public boolean titleScreen = true;
    public boolean multiplayerScreen = true;
    private final Properties props = new Properties();

    public static AdConfig load() {
        AdConfig cfg = new AdConfig();
        Path file = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);

        try {
            if (Files.notExists(file)) writeDefaults(file);
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                cfg.props.load(reader);
            }
        } catch (IOException | IllegalArgumentException ignored) {
            // Bad or unreadable file: fall back to the built-in defaults.
        }

        cfg.enabled = cfg.bool("enabled", true);
        cfg.titleScreen = cfg.bool("title-screen", true);
        cfg.multiplayerScreen = cfg.bool("multiplayer-screen", true);
        return cfg;
    }

    public String name(Ad ad) {
        String value = props.getProperty(ad.key + ".name", "").trim();
        return value.isEmpty() ? ad.defaultName : value;
    }

    /** Configured address, or empty when the ad should look the server up in the saved server list by name. */
    public String address(Ad ad) {
        return props.getProperty(ad.key + ".address", ad.defaultAddress).trim();
    }

    /** Loose server-name match: ignores colour codes, case, spaces and punctuation. */
    public static boolean sameName(String a, String b) {
        return !normalize(a).isEmpty() && normalize(a).equals(normalize(b));
    }

    private static String normalize(String s) {
        if (s == null) return "";
        return s.replaceAll("§.", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private boolean bool(String key, boolean fallback) {
        String value = props.getProperty(key);
        return value == null ? fallback : Boolean.parseBoolean(value.trim());
    }

    private static void writeDefaults(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, """
            # Server ads on the title screen and multiplayer screen.
            # Used by NanoFutureAddon and the standalone FutureAds mod.
            enabled=true
            title-screen=true
            multiplayer-screen=true

            future.name=The Future
            future.address=future.playsmp.xyz

            # Leave an address empty to use the entry with this name from your saved server list.
            verity.name=Truth & Verity
            verity.address=
            """, StandardCharsets.UTF_8);
    }
}
