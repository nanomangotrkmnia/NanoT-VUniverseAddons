package dev.nanomango.ads;

import java.util.ArrayList;
import java.util.List;

/** Where the ads go, in scaled GUI pixels. Screens skip a placement that would cover a vanilla button. */
public final class AdLayout {
    public record Placement(Ad ad, int x, int y, int w, int h) {
        public boolean overlaps(int ox, int oy, int ow, int oh) {
            return x < ox + ow && ox < x + w && y < oy + oh && oy < y + h;
        }
    }

    /** Half the server list row width plus the scrollbar, measured from the screen centre. */
    private static final int LIST_HALF_WIDTH = 170;
    private static final int MIN_WIDTH = 56;

    private AdLayout() {}

    /** "Try the Verity" bottom-left beside the skin, "Join The Future!" bottom-right above the copyright. */
    public static List<Placement> title(int width, int height) {
        List<Placement> out = new ArrayList<>();
        int h = 56;

        out.add(new Placement(Ad.VERITY, 8, height - 20 - h, 66, h));

        int futureW = Math.min(142, width / 2 - 24);
        if (futureW >= MIN_WIDTH) out.add(new Placement(Ad.FUTURE, width - 6 - futureW, height - 14 - h, futureW, h));
        return out;
    }

    /** "Join The Future!" top-right and "Try at T&V" left, both outside the server list. */
    public static List<Placement> multiplayer(int width, int height) {
        List<Placement> out = new ArrayList<>();
        int top = 36;
        int footer = 70;
        int side = width / 2 - LIST_HALF_WIDTH - 6;
        if (side < MIN_WIDTH) return out;

        int futureW = Math.min(side, 110);
        int futureH = futureW < 96 ? 66 : 56;
        if (top + futureH <= height - footer) out.add(new Placement(Ad.FUTURE, width - 6 - futureW, top, futureW, futureH));

        int verityW = Math.min(side, 80);
        int verityH = 60;
        if (top + 4 + verityH <= height - footer) out.add(new Placement(Ad.VERITY, 6, top + 4, verityW, verityH));
        return out;
    }
}
