package dev.nanomango.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.nanomango.modules.AIAssistant;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

public class ContextClearCommand extends Command {
    public ContextClearCommand() {
        super("contextclear", "Clears the AI assistant context.");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(context -> {
            AIAssistant assistant = Modules.get().get(AIAssistant.class);

            if (assistant != null) assistant.clearContext();

            info("Context cleared.");
            return SINGLE_SUCCESS;
        });
    }
}
