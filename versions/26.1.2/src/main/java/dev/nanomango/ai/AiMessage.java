package dev.nanomango.ai;

import java.util.List;

public final class AiMessage {
    public enum Role {
        USER,
        ASSISTANT,
        TOOL
    }

    public final Role role;
    public final String text;
    public final List<ToolCall> calls;
    public final String callId;
    public final String callName;

    private AiMessage(Role role, String text, List<ToolCall> calls, String callId, String callName) {
        this.role = role;
        this.text = text == null ? "" : text;
        this.calls = calls == null ? List.of() : List.copyOf(calls);
        this.callId = callId;
        this.callName = callName;
    }

    public static AiMessage user(String text) {
        return new AiMessage(Role.USER, text, null, null, null);
    }

    public static AiMessage assistant(String text) {
        return new AiMessage(Role.ASSISTANT, text, null, null, null);
    }

    public static AiMessage assistant(String text, List<ToolCall> calls) {
        return new AiMessage(Role.ASSISTANT, text, calls, null, null);
    }

    public static AiMessage tool(String callId, String callName, String text) {
        return new AiMessage(Role.TOOL, text, null, callId, callName);
    }
}
