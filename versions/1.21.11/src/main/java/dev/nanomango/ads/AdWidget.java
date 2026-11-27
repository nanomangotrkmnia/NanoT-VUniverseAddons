package dev.nanomango.ads;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Consumer;

/** A clickable ad panel that joins its server. */
public class AdWidget extends ClickableWidget {
    private final Ad ad;
    private final boolean multiplayer;
    private final Screen parent;
    private final String serverName;
    private final String address;

    public AdWidget(Ad ad, boolean multiplayer, Screen parent, AdConfig cfg, AdLayout.Placement p) {
        super(p.x(), p.y(), p.w(), p.h(), Text.literal(cfg.name(ad)));
        this.ad = ad;
        this.multiplayer = multiplayer;
        this.parent = parent;
        this.serverName = cfg.name(ad);
        this.address = resolve(serverName, cfg.address(ad));

        setTooltip(Tooltip.of(Text.literal(address.isEmpty()
            ? "No address for " + serverName + ". Save it in your server list or set " + ad.key + ".address in config/" + AdConfig.FILE_NAME + "."
            : "Join " + serverName + "\n" + address)));
    }

    /** Adds every ad that fits to {@code screen}, in front of other widgets for clicks. */
    public static void install(Screen screen, boolean multiplayer, Consumer<AdWidget> adder) {
        AdConfig cfg = AdConfig.load();
        if (!cfg.enabled || !(multiplayer ? cfg.multiplayerScreen : cfg.titleScreen)) return;

        List<AdLayout.Placement> placements = multiplayer
            ? AdLayout.multiplayer(screen.width, screen.height)
            : AdLayout.title(screen.width, screen.height);

        for (AdLayout.Placement p : placements) {
            if (!multiplayer && coversWidget(screen, p)) continue;

            AdWidget widget = new AdWidget(p.ad(), multiplayer, screen, cfg, p);
            adder.accept(widget);

            @SuppressWarnings("unchecked")
            List<Element> children = (List<Element>) screen.children();
            if (children.remove(widget)) children.add(0, widget);
        }
    }

    private static boolean coversWidget(Screen screen, AdLayout.Placement p) {
        for (Element child : screen.children()) {
            if (child instanceof ClickableWidget w && p.overlaps(w.getX(), w.getY(), w.getWidth(), w.getHeight())) return true;
        }
        return false;
    }

    private static String resolve(String name, String configured) {
        if (!configured.isEmpty()) return configured;

        ServerList list = new ServerList(MinecraftClient.getInstance());
        list.loadFile();
        for (int i = 0; i < list.size(); i++) {
            ServerInfo server = list.get(i);
            if (AdConfig.sameName(server.name, name)) return server.address;
        }
        return "";
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        AdArt.draw(new AdCanvas() {
            @Override
            public void fill(int x1, int y1, int x2, int y2, int argb) {
                context.fill(x1, y1, x2, y2, argb);
            }

            @Override
            public void text(String text, int x, int y, int argb) {
                context.drawText(font, text, x, y, argb, true);
            }

            @Override
            public int textWidth(String text) {
                return font.getWidth(text);
            }
        }, ad, multiplayer, getX(), getY(), getWidth(), getHeight(), isHovered(), System.currentTimeMillis());
    }

    @Override
    public void onClick(Click click, boolean doubleClick) {
        if (address.isEmpty()) return;

        Screen back = parent instanceof MultiplayerScreen ? parent : new MultiplayerScreen(parent);
        ConnectScreen.connect(back, MinecraftClient.getInstance(), ServerAddress.parse(address),
            new ServerInfo(serverName, address, ServerInfo.ServerType.OTHER), false, null);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
