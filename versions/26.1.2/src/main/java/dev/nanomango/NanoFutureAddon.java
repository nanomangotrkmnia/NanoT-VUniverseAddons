package dev.nanomango;

import dev.nanomango.ai.ChatLog;
import dev.nanomango.ai.ContainerTracker;
import dev.nanomango.commands.AiCommand;
import dev.nanomango.commands.AiToolCommand;
import dev.nanomango.commands.ContextClearCommand;
import dev.nanomango.modules.AIAssistant;
import dev.nanomango.modules.AutoChunkMiner;
import dev.nanomango.modules.FastThrow;
import dev.nanomango.modules.AutoTerraform;
import dev.nanomango.modules.AutoTorch;
import dev.nanomango.modules.GhostBlocks;
import dev.nanomango.modules.InfiniteReach;
import dev.nanomango.modules.InstantBreak;
import dev.nanomango.modules.KeepBreakProgress;
import dev.nanomango.modules.Phase;
import dev.nanomango.modules.Reminder;
import dev.nanomango.modules.SpawnOverlay;
import dev.nanomango.modules.TimeShift;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class NanoFutureAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("NanoFuture");

    @Override
    public void onInitialize() {
        LOG.info("Initializing NanoFutureAddon");

        Modules.get().add(new AIAssistant());
        Modules.get().add(new AutoChunkMiner());
        Modules.get().add(new AutoTerraform());
        Modules.get().add(new AutoTorch());
        Modules.get().add(new GhostBlocks());
        Modules.get().add(new FastThrow());
        Modules.get().add(new InfiniteReach());
        Modules.get().add(new InstantBreak());
        Modules.get().add(new KeepBreakProgress());
        Modules.get().add(new Phase());
        Modules.get().add(new Reminder());
        Modules.get().add(new SpawnOverlay());
        Modules.get().add(new TimeShift());

        Commands.add(new AiCommand());
        Commands.add(new AiToolCommand());
        Commands.add(new ContextClearCommand());

        MeteorClient.EVENT_BUS.subscribe(new ChatLog());
        MeteorClient.EVENT_BUS.subscribe(new ContainerTracker());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "dev.nanomango";
    }
}
