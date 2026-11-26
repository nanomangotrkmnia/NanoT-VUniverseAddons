package dev.nanomango.modules;

import dev.nanomango.NanoFutureAddon;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;

public class KeepBreakProgress extends Module {
    public KeepBreakProgress() {
        super(NanoFutureAddon.CATEGORY, "keep-break-progress",
            "Keeps the break progress after you stop mining.");
    }

    public boolean shouldKeepBreaking() {
        return isActive() && Utils.canUpdate();
    }
}
