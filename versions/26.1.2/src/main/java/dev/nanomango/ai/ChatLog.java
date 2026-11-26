package dev.nanomango.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.orbit.EventHandler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class ChatLog {
    private static final int MAX_LINES = 50;

    private static final ArrayDeque<String> LINES = new ArrayDeque<>();

    @EventHandler
    private void onMessage(ReceiveMessageEvent event) {
        if (event.getMessage() == null) return;

        String text = event.getMessage().getString().trim();
        if (text.isEmpty()) return;

        synchronized (LINES) {
            LINES.addLast(text);
            while (LINES.size() > MAX_LINES) LINES.removeFirst();
        }
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        synchronized (LINES) {
            LINES.clear();
        }
    }

    public static String read(JsonObject arguments) {
        int count = AiTools.integer(arguments, "count", 10, 1, 20);

        List<String> lines;
        synchronized (LINES) {
            lines = new ArrayList<>(LINES);
        }

        int start = Math.max(0, lines.size() - count);
        JsonArray array = new JsonArray();

        for (int i = start; i < lines.size(); i++) {
            JsonObject line = new JsonObject();
            line.addProperty("text", lines.get(i));
            array.add(line);
        }

        JsonObject out = new JsonObject();
        out.addProperty("count", lines.size());
        out.add("lines", array);
        return out.toString();
    }
}
