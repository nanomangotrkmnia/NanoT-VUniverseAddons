package dev.nanomango.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.nanomango.modules.AIAssistant;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

public class AiCommand extends Command {
    public AiCommand() {
        super("ai", "Sends a message to the AI assistant.");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.then(argument("message", StringArgumentType.greedyString()).executes(context -> {
            AIAssistant assistant = Modules.get().get(AIAssistant.class);

            if (assistant == null) {
                error("AI Assistant is not loaded.");
                return SINGLE_SUCCESS;
            }

            assistant.ask(context.getArgument("message", String.class));
            return SINGLE_SUCCESS;
        }));

        builder.executes(context -> {
            warning("Usage: .ai <message>");
            return SINGLE_SUCCESS;
        });
    }
}
