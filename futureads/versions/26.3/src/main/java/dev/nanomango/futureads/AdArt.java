package dev.nanomango.futureads;

import java.util.ArrayList;
import java.util.List;

/** Pixel art for the two ads, drawn with plain fills so it looks the same on every version. */
public final class AdArt {
    private static final int BG = 0xC0101626;
    private static final int BG_HOVER = 0xD81A2440;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int YELLOW = 0xFFFFE14D;
    private static final int YELLOW_DARK = 0xFFD9A400;
    private static final int RED = 0xFFE5322B;
    private static final int RED_SHADOW = 0xFF5A0D0A;
    private static final int LIME = 0xFFB4F03A;
    private static final int ROCK_LIGHT = 0xFF9AA1AC;
    private static final int ROCK_DARK = 0xFF6B717D;
    private static final int GRASS = 0xFF1F7A3A;
    private static final int GRASS_LIGHT = 0xFF35B85A;
    private static final int QUILL = 0xFFF2F2F2;
    private static final int QUILL_EDGE = 0xFFB7C0CC;
    private static final int SHAFT = 0xFFC9BFA8;
    private static final int INK = 0xFF15151D;

    private AdArt() {}

    public static void draw(AdCanvas c, Ad ad, boolean multiplayer, int x, int y, int w, int h, boolean hovered, long timeMs) {
        c.fill(x, y, x + w, y + h, hovered ? BG_HOVER : BG);

        List<String> lines = wrapAll(c, ad.headline(multiplayer), w - 10);
        int textRight = x + 5;
        int ty = y + 5;
        for (String line : lines) {
            c.text(line, x + 5, ty, WHITE);
            textRight = Math.max(textRight, x + 5 + c.textWidth(line));
            ty += 10;
        }
        int textBottom = ty;

        if (ad == Ad.FUTURE) landscape(c, x, y, w, h, textRight, textBottom, timeMs);
        else quill(c, x, y, w, h, textBottom);

        border(c, x, y, w, h, accent(ad, multiplayer, hovered));
        badge(c, x + (int) (w * 0.8f), y - 9 + bob(timeMs));
    }

    private static int accent(Ad ad, boolean multiplayer, boolean hovered) {
        if (hovered) return YELLOW;
        if (!multiplayer) return WHITE;
        return ad == Ad.FUTURE ? RED : LIME;
    }

    private static List<String> wrapAll(AdCanvas c, String[] lines, int maxWidth) {
        List<String> out = new ArrayList<>();
        for (String line : lines) out.addAll(wrap(c, line, maxWidth));
        return out;
    }

    private static List<String> wrap(AdCanvas c, String text, int maxWidth) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String next = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && c.textWidth(next) > maxWidth) {
                out.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(next);
            }
        }
        if (!line.isEmpty()) out.add(line.toString());
        return out;
    }

    /** A sketchy double stroke: a solid outline plus a faint offset line along the bottom and right. */
    private static void border(AdCanvas c, int x, int y, int w, int h, int color) {
        c.fill(x, y, x + w, y + 1, color);
        c.fill(x, y + h - 1, x + w, y + h, color);
        c.fill(x, y, x + 1, y + h, color);
        c.fill(x + w - 1, y, x + w, y + h, color);

        int faint = (color & 0x00FFFFFF) | 0x70000000;
        c.fill(x + 3, y + h, x + w + 1, y + h + 1, faint);
        c.fill(x + w, y + 3, x + w + 1, y + h + 1, faint);
    }

    /** The red "!" sticking out of the top edge, bobbing slowly. */
    private static void badge(AdCanvas c, int x, int y) {
        c.fill(x + 1, y + 1, x + 3, y + 8, RED_SHADOW);
        c.fill(x + 1, y + 10, x + 3, y + 12, RED_SHADOW);
        c.fill(x, y, x + 2, y + 7, RED);
        c.fill(x, y + 9, x + 2, y + 11, RED);
    }

    private static int bob(long timeMs) {
        return (int) Math.round(Math.sin(timeMs / 260.0) * 1.5);
    }

    /** "Join The Future!": two snowy peaks, a sun and a strip of grass. */
    private static void landscape(AdCanvas c, int x, int y, int w, int h, int textRight, int textBottom, long timeMs) {
        int ground = y + h - 9;

        c.fill(x + 1, ground - 12, x + w - 1, ground, 0x30406A9A);

        int r = Math.max(4, Math.min(8, h / 8));
        int sunX = x + w - r - 9;
        int sunY = y + r + 4;
        if (sunX - r > textRight + 3) {
            disc(c, sunX, sunY, r, YELLOW_DARK);
            disc(c, sunX, sunY, r - 1, YELLOW);
            for (int i = -1; i <= 1; i++) {
                int sx = sunX + i * (r / 2 + 1);
                c.fill(sx, sunY - r / 2, sx + 1, sunY + r / 2 + 1, YELLOW_DARK);
            }
        }

        int peakTop = Math.min(ground - 8, Math.max(textBottom + 2, ground - h / 2));
        mountain(c, x + (int) (w * 0.30f), peakTop, ground, Math.max(10, (int) (w * 0.20f)));
        mountain(c, x + (int) (w * 0.55f), peakTop + 3, ground, Math.max(9, (int) (w * 0.17f)));

        c.fill(x + 1, ground, x + w - 1, y + h - 1, GRASS);
        c.fill(x + 1, ground, x + w - 1, ground + 1, GRASS_LIGHT);
        int sway = (int) ((timeMs / 400) % 2);
        for (int gx = x + 2; gx < x + w - 2; gx += 2) {
            int bladeH = 1 + ((gx * 7 + (gx >> 2) * 3) & 3);
            int lean = ((gx >> 1) & 1) == sway ? 0 : 1;
            c.fill(gx + lean, ground - bladeH, gx + lean + 1, ground, GRASS_LIGHT);
        }
    }

    private static void mountain(AdCanvas c, int cx, int peakY, int baseY, int halfWidth) {
        int rows = baseY - peakY;
        if (rows <= 0) return;
        int snowRows = Math.max(2, rows / 4);

        for (int t = 0; t < rows; t++) {
            int half = Math.max(0, t * halfWidth / rows);
            int yy = peakY + t;
            if (t < snowRows) {
                c.fill(cx - half, yy, cx + half + 1, yy + 1, WHITE);
            } else {
                c.fill(cx - half, yy, cx, yy + 1, ROCK_LIGHT);
                c.fill(cx, yy, cx + half + 1, yy + 1, ROCK_DARK);
                if (t == snowRows) {
                    for (int sx = cx - half; sx <= cx + half; sx += 2) c.fill(sx, yy, sx + 1, yy + 1, WHITE);
                }
            }
        }
    }

    private static void disc(AdCanvas c, int cx, int cy, int r, int color) {
        if (r <= 0) return;
        for (int dy = -r; dy <= r; dy++) {
            int half = (int) Math.floor(Math.sqrt(r * r - dy * dy + 0.5));
            c.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }

    /** "Try the Verity": a white quill pen leaning right with an ink blot at the nib. */
    private static void quill(AdCanvas c, int x, int y, int w, int h, int textBottom) {
        int n = Math.max(8, Math.min(22, Math.min(y + h - 6 - textBottom - 3, w - 24)));
        int tx = x + (w - n) / 2 - 2;
        int ty = y + h - 6;

        c.fill(tx - 4, ty + 2, tx, ty + 3, INK);
        c.fill(tx - 3, ty + 1, tx - 1, ty + 2, INK);

        for (int i = 0; i <= n; i++) {
            int px = tx + i;
            int py = ty - i;
            if (i >= 4) {
                int vane = (int) Math.round(4 * Math.sin(Math.PI * (i - 4) / (n - 3.0)));
                for (int k = 1; k <= vane; k++) {
                    c.fill(px + k, py + k, px + k + 2, py + k + 1, k == vane ? QUILL_EDGE : QUILL);
                }
                for (int k = 1; k < vane; k++) {
                    c.fill(px - k, py - k, px - k + 2, py - k + 1, QUILL);
                }
            }
            c.fill(px, py, px + 2, py + 1, i < 3 ? INK : SHAFT);
        }
    }
}
