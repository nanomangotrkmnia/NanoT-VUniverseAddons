package dev.nanomango.ai;

import java.util.List;

public record Reply(String text, List<ToolCall> calls) {
}
