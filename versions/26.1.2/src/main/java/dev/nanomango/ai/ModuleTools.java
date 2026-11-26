package dev.nanomango.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class ModuleTools {
    private static final int MAX_MODULES = 100;
    private static final int MAX_SETTINGS = 60;

    private ModuleTools() {
    }

    public static String list(JsonObject arguments) {
        List<Module> modules = new ArrayList<>(Modules.get().getAll());
        modules.sort(Comparator.comparing(module -> module.name));

        JsonArray array = new JsonArray();
        int count = Math.min(MAX_MODULES, modules.size());

        for (int i = 0; i < count; i++) {
            Module module = modules.get(i);

            JsonObject object = new JsonObject();
            object.addProperty("name", module.name);
            object.addProperty("active", module.isActive());
            object.addProperty("description", module.description);
            array.add(object);
        }

        JsonObject out = new JsonObject();
        out.addProperty("count", modules.size());
        out.addProperty("truncated", modules.size() > MAX_MODULES);
        out.add("modules", array);
        return out.toString();
    }

    public static String get(JsonObject arguments) {
        if (!AiTools.present(arguments, "name")) return AiTools.error("missing required argument \"name\"");

        String name = AiTools.string(arguments, "name", "");
        Module module = Modules.get().get(name);
        if (module == null) return AiTools.error("unknown module \"" + name + "\"");

        JsonArray settings = new JsonArray();
        int count = 0;

        for (SettingGroup group : module.settings) {
            for (Setting<?> setting : group) {
                if (!setting.isVisible()) continue;
                if (count++ >= MAX_SETTINGS) break;

                JsonObject object = new JsonObject();
                object.addProperty("group", group.name);
                object.addProperty("name", setting.name);
                object.addProperty("description", setting.description);
                object.addProperty("value", value(setting));
                settings.add(object);
            }
        }

        JsonObject out = new JsonObject();
        out.addProperty("name", module.name);
        out.addProperty("description", module.description);
        out.addProperty("active", module.isActive());
        out.add("settings", settings);
        return out.toString();
    }

    public static String set(JsonObject arguments) {
        if (!AiTools.present(arguments, "name")) return AiTools.error("missing required argument \"name\"");

        String name = AiTools.string(arguments, "name", "");
        Module module = Modules.get().get(name);
        if (module == null) return AiTools.error("unknown module \"" + name + "\"");

        boolean enabled = AiTools.bool(arguments, "enabled", !module.isActive());
        if (module.isActive() != enabled) module.toggle();

        JsonObject out = new JsonObject();
        out.addProperty("name", module.name);
        out.addProperty("active", module.isActive());
        return out.toString();
    }

    public static String setSetting(JsonObject arguments) {
        String moduleName = AiTools.string(arguments, "module", "");
        String settingName = AiTools.string(arguments, "setting", "");

        if (moduleName.isBlank()) return AiTools.error("missing required argument \"module\"");
        if (settingName.isBlank()) return AiTools.error("missing required argument \"setting\"");
        if (!AiTools.present(arguments, "value")) return AiTools.error("missing required argument \"value\"");

        Module module = Modules.get().get(moduleName);
        if (module == null) return AiTools.error("unknown module \"" + moduleName + "\"");

        Setting<?> setting = module.settings.get(settingName);
        if (setting == null) return AiTools.error("unknown setting \"" + settingName + "\" in " + module.name);
        if (!setting.isVisible()) return AiTools.error("setting \"" + settingName + "\" is hidden");
        if (sensitive(setting.name)) return AiTools.error("setting \"" + settingName + "\" is private");

        String value = AiTools.string(arguments, "value", "");
        if (!setting.parse(value)) return AiTools.error("invalid value for \"" + settingName + "\"");

        JsonObject out = new JsonObject();
        out.addProperty("module", module.name);
        out.addProperty("setting", setting.name);
        out.addProperty("value", value(setting));
        return out.toString();
    }

    private static String value(Setting<?> setting) {
        if (sensitive(setting.name)) return "***";

        Object value = setting.get();
        return value == null ? "null" : String.valueOf(value);
    }

    private static boolean sensitive(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.contains("key") || lower.contains("token")
            || lower.contains("password") || lower.contains("secret")
            || lower.equals("allow-actions");
    }
}
