package dev.nanomango.ai;

import com.google.gson.JsonObject;

public interface AiTool {
    String name();

    String description();

    JsonObject parameters();

    String run(JsonObject arguments) throws Exception;
}
