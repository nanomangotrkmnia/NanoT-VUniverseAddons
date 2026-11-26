package dev.nanomango.utils;

import net.fabricmc.loader.api.FabricLoader;

public final class BaritoneSupport {
    public static final boolean AVAILABLE = FabricLoader.getInstance().isModLoaded("baritone");

    private BaritoneSupport() {
    }
}
