package dev.nanomango.ads;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

/** A clickable ad panel that joins its server. */
public class AdWidget extends AbstractWidget {
    private final Ad ad;
    private final boolean multiplayer;
    private final Screen parent;
    private final String serverName;
    private final String address;

    public AdWidget(Ad ad, boolean multiplayer, Screen parent, AdConfig cfg, AdLayout.Placement p) {
        super(p.x(), p.y(), p.w(), p.h(), Component.literal(cfg.name(ad)));
        this.ad = ad;
        this.multiplayer = multiplayer;
        this.parent = parent;
        this.serverName = cfg.name(ad);
        this.address = resolve(serverName, cfg.address(ad));

        setTooltip(Tooltip.create(Component.literal(address.isEmpty()
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
            List<GuiEventListener> children = (List<GuiEventListener>) screen.children();
            if (children.remove(widget)) children.add(0, widget);
        }
    }

    private static boolean coversWidget(Screen screen, AdLayout.Placement p) {
        for (GuiEventListener child : screen.children()) {
            if (child instanceof AbstractWidget w && p.overlaps(w.getX(), w.getY(), w.getWidth(), w.getHeight())) return true;
        }
        return false;
    }

    private static String resolve(String name, String configured) {
        if (!configured.isEmpty()) return configured;

        ServerList list = new ServerList(Minecraft.getInstance());
        list.load();
        for (int i = 0; i < list.size(); i++) {
            ServerData server = list.get(i);
            if (AdConfig.sameName(server.name, name)) return server.ip;
        }
        return "";
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        Font font = Minecraft.getInstance().font;
        AdArt.draw(new AdCanvas() {
            @Override
            public void fill(int x1, int y1, int x2, int y2, int argb) {
                g.fill(x1, y1, x2, y2, argb);
            }

            @Override
            public void text(String text, int x, int y, int argb) {
                g.text(font, text, x, y, argb, true);
            }

            @Override
            public int textWidth(String text) {
                return font.width(text);
            }
        }, ad, multiplayer, getX(), getY(), getWidth(), getHeight(), isHovered(), System.currentTimeMillis());
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (address.isEmpty()) return;

        Screen back = parent instanceof JoinMultiplayerScreen ? parent : new JoinMultiplayerScreen(parent);
        ConnectScreen.startConnecting(back, Minecraft.getInstance(), ServerAddress.parseString(address),
            new ServerData(serverName, address, ServerData.Type.OTHER), false, null);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
