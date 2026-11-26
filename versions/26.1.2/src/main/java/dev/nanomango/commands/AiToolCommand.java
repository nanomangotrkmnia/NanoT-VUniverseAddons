package dev.nanomango.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.nanomango.ai.AiTool;
import dev.nanomango.ai.AiTools;
import dev.nanomango.ai.ToolCall;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

public class AiToolCommand extends Command {
    public AiToolCommand() {
        super("aitool", "Runs an ai-assistant tool directly.");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(context -> {
            for (AiTool tool : AiTools.all()) info("%s: %s", tool.name(), tool.description());
            return SINGLE_SUCCESS;
        });

        builder.then(argument("name", StringArgumentType.word())
            .executes(context -> run(context.getArgument("name", String.class), "{}"))
            .then(argument("args", StringArgumentType.greedyString())
                .executes(context -> run(context.getArgument("name", String.class),
                    context.getArgument("args", String.class)))));
    }

    private int run(String name, String args) {
        JsonObject arguments;

        try {
            arguments = JsonParser.parseString(args).getAsJsonObject();
        } catch (Exception e) {
            error("Arguments must be a JSON object.");
            return SINGLE_SUCCESS;
        }

        try {
            String result = AiTools.run(new ToolCall(name, name, arguments));
            if (result.length() > 800) result = result.substring(0, 800) + "...";

            info("%s: %s", name, result);
        } catch (Exception e) {
            error("Tool failed: %s", e.getMessage());
        }

        return SINGLE_SUCCESS;
    }
}
