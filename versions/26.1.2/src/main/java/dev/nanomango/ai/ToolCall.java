package dev.nanomango.ai;

import com.google.gson.JsonObject;

public record ToolCall(String id, String name, JsonObject arguments) {
}
