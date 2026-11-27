package dev.nanomango.futureads;

/** The few drawing calls the ad art needs, so the art stays the same on every Minecraft version. */
public interface AdCanvas {
    void fill(int x1, int y1, int x2, int y2, int argb);

    void text(String text, int x, int y, int argb);

    int textWidth(String text);
}
