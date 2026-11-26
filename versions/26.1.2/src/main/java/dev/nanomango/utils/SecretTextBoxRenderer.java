package dev.nanomango.utils;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class SecretTextBoxRenderer implements WTextBox.Renderer {
    @Override
    public void render(GuiRenderer renderer, double x, double y, String text, Color color) {
        if (text.isEmpty()) return;

        renderer.text("*".repeat(text.length()), x, y, color, false);
    }
}
