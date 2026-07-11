package gg.dindijari.client.gui.screen;

import gg.dindijari.client.gui.widget.ThemedButton;
import gg.dindijari.client.gui.widget.ThemedTextField;
import gg.dindijari.client.render.ColorUtil;
import gg.dindijari.client.render.Fonts;
import gg.dindijari.client.render.Render2D;
import gg.dindijari.client.render.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import net.minecraft.client.gui.screens.EditServerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The client's Multiplayer screen, replacing the vanilla server browser
 * (swapped in by {@link gg.dindijari.client.gui.ScreenManager}) and matching
 * the design reference: header with accent underline, search field top-right,
 * servers as rounded cards (icon square, name, address, player count and ping
 * bars on the right; "offline" for unreachable hosts), selected card outlined
 * in accent, and a bottom row of <em>Join Server</em> (accent) /
 * <em>Add Server</em> / <em>Direct Connect</em> / <em>Back</em>.
 *
 * <p>The server list, status pinging, connecting and the add/direct-connect
 * flows are all the vanilla implementations — only the presentation is
 * replaced. Double-clicking a card joins it.
 */
public final class DindijariJoinMultiplayerScreen extends ThemedScreen {

    private static final Logger LOGGER = LoggerFactory.getLogger("dindijariclient/servers");
    /** Latency thresholds (ms) for 5..1 ping bars, mirroring vanilla's tiers. */
    private static final long[] PING_TIERS = {150, 300, 600, 1000};
    private static final int PING_GOOD = 0xFF4CD964;
    private static final int PING_BAD = 0xFFFF5555;

    private final Screen parent;
    private final Component header = Fonts.ui("Multiplayer");
    private final Component emptyLabel = Fonts.ui("No servers yet — add one!");
    private final Component offlineLabel = Fonts.ui("offline");
    private final Component pingingLabel = Fonts.ui("...");
    private final ServerStatusPinger pinger = new ServerStatusPinger();
    private final Map<ServerData, Component> nameCache = new HashMap<>();
    private final Map<ServerData, Component> addressCache = new HashMap<>();

    private ServerList servers;
    private final List<ServerData> visible = new ArrayList<>();
    private String filter = "";
    private ServerData selected;
    private float scroll;
    private long lastClickMs;
    private ServerData lastClickTarget;
    private ThemedButton joinButton;

    /**
     * Creates the screen.
     *
     * @param parent the screen to return to
     */
    public DindijariJoinMultiplayerScreen(Screen parent) {
        super(Component.translatable("multiplayer.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (servers == null) {
            servers = new ServerList(this.minecraft);
            servers.load();
            for (int i = 0; i < servers.size(); i++) {
                ping(servers.get(i));
            }
        }
        refilter();

        int pad = Math.round(Theme.px(24));
        int searchW = Math.round(Theme.px(220));
        addRenderableWidget(new ThemedTextField(this.width - pad - searchW, pad,
                searchW, Math.round(Theme.px(26)), "Search servers...", q -> {
            filter = q.toLowerCase(Locale.ROOT);
            refilter();
        }));

        int bh = Math.round(Theme.px(34));
        int by = this.height - bh - Math.round(Theme.px(20));
        int gap = Math.round(Theme.px(Theme.GRID));
        int bx = pad;
        joinButton = new ThemedButton(bx, by, Math.round(Theme.px(140)), bh,
                "Join Server", true, this::joinSelected);
        joinButton.active = false;
        addRenderableWidget(joinButton);
        bx += Math.round(Theme.px(140)) + gap;
        addRenderableWidget(new ThemedButton(bx, by, Math.round(Theme.px(120)), bh,
                "Add Server", this::addServer));
        bx += Math.round(Theme.px(120)) + gap;
        addRenderableWidget(new ThemedButton(bx, by, Math.round(Theme.px(140)), bh,
                "Direct Connect", this::directConnect));
        bx += Math.round(Theme.px(140)) + gap;
        addRenderableWidget(new ThemedButton(bx, by, Math.round(Theme.px(90)), bh,
                "Back", this::onClose));
    }

    private void ping(ServerData data) {
        data.setState(ServerData.State.PINGING);
        new Thread(() -> {
            try {
                pinger.pingServer(data, () -> {
                }, () -> data.setState(ServerData.State.SUCCESSFUL));
            } catch (UnknownHostException e) {
                data.ping = -1L;
                data.setState(ServerData.State.UNREACHABLE);
            } catch (Exception e) {
                LOGGER.warn("Failed to ping {}", data.ip, e);
                data.ping = -1L;
                data.setState(ServerData.State.UNREACHABLE);
            }
        }, "dindijari-server-ping").start();
    }

    private void refilter() {
        visible.clear();
        if (servers != null) {
            for (int i = 0; i < servers.size(); i++) {
                ServerData data = servers.get(i);
                if (filter.isEmpty()
                        || data.name.toLowerCase(Locale.ROOT).contains(filter)
                        || data.ip.toLowerCase(Locale.ROOT).contains(filter)) {
                    visible.add(data);
                }
            }
        }
        if (selected != null && !visible.contains(selected)) {
            selected = null;
        }
        if (joinButton != null) {
            joinButton.active = selected != null;
        }
    }

    @Override
    public void tick() {
        super.tick();
        pinger.tick();
    }

    @Override
    public void removed() {
        super.removed();
        pinger.removeAll();
    }

    // ------------------------------------------------------------------
    // Layout (mirrors the singleplayer screen)
    // ------------------------------------------------------------------

    private float listX() {
        return Theme.px(24);
    }

    private float listY() {
        return Theme.px(64);
    }

    private float listW() {
        return this.width - 2 * listX();
    }

    private float listH() {
        return this.height - listY() - Theme.px(66);
    }

    private float cardH() {
        return Theme.px(52);
    }

    private float cardStride() {
        return cardH() + Theme.px(Theme.GRID);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        float pad = Theme.px(24);
        Fonts.drawScaled(g, header, pad, pad, 1.5F, Theme.TEXT_PRIMARY, false);
        Render2D.fillRounded(g, pad, Theme.snap(pad + 9 * 1.5F + Theme.px(5)),
                Fonts.width(header) * 1.5F * 0.6F, Theme.px(3), Theme.px(1.5F), Theme.accent());

        if (visible.isEmpty()) {
            Fonts.drawCentered(g, emptyLabel, this.width / 2.0F, this.height / 2.0F,
                    1.0F, Theme.TEXT_SECONDARY, false);
            return;
        }

        clampScroll();
        float x = listX();
        float w = listW();
        float yTop = listY();
        float yBottom = yTop + listH();
        float cy = yTop - scroll;

        g.enableScissor((int) x - 2, (int) yTop - 2, (int) (x + w) + 2, (int) yBottom + 2);
        for (ServerData data : visible) {
            if (cy + cardH() >= yTop && cy <= yBottom) {
                renderCard(g, data, x, cy, w, mouseX, mouseY);
            }
            cy += cardStride();
        }
        g.disableScissor();
    }

    private void renderCard(GuiGraphics g, ServerData data, float x, float y, float w,
                            int mouseX, int mouseY) {
        boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + cardH();
        boolean isSelected = data == selected;
        float radius = Theme.px(Theme.PANEL_RADIUS);
        Render2D.fillRounded(g, x, y, w, cardH(), radius,
                hover || isSelected ? Theme.BUTTON : ColorUtil.withAlpha(Theme.BUTTON, 170));
        if (isSelected) {
            Render2D.outlineRounded(g, x, y, w, cardH(), radius, 1.2F, Theme.accent());
        }

        // Accent icon square, per the reference.
        float iconSize = Theme.px(36);
        float iconX = x + Theme.px(8);
        float iconY = y + (cardH() - iconSize) / 2;
        Render2D.fillRounded(g, iconX, iconY, iconSize, iconSize, Theme.px(6), 0xFF0E0E10);
        Render2D.fillRounded(g, iconX + Theme.px(10), iconY + Theme.px(10),
                iconSize - Theme.px(20), iconSize - Theme.px(20), Theme.px(3), Theme.accent());

        float textX = iconX + iconSize + Theme.px(10);
        Fonts.draw(g, nameCache.computeIfAbsent(data, d -> Fonts.ui(d.name)),
                textX, y + Theme.px(10), Theme.TEXT_PRIMARY, false);
        Fonts.drawScaled(g, addressCache.computeIfAbsent(data, d -> Fonts.ui(d.ip)),
                textX, y + Theme.px(30), 0.8F, Theme.TEXT_SECONDARY, false);

        // Right side: ping bars + player count / offline.
        float barsRight = x + w - Theme.px(14);
        renderPingBars(g, barsRight, y + cardH() / 2, data);
        Component status = statusOf(data);
        Fonts.drawScaled(g, status, barsRight - Theme.px(30) - Fonts.width(status) * 0.8F,
                y + cardH() / 2 - 3.5F, 0.8F, Theme.TEXT_SECONDARY, false);
    }

    private Component statusOf(ServerData data) {
        if (data.state() == ServerData.State.UNREACHABLE) {
            return offlineLabel;
        }
        if (data.state() == ServerData.State.PINGING || data.status == null) {
            return pingingLabel;
        }
        return data.status;
    }

    private void renderPingBars(GuiGraphics g, float right, float centerY, ServerData data) {
        boolean online = data.state() == ServerData.State.SUCCESSFUL && data.ping >= 0;
        int filled;
        if (!online) {
            filled = 1;
        } else {
            filled = 5;
            for (long tier : PING_TIERS) {
                if (data.ping > tier) {
                    filled--;
                }
            }
        }
        float barW = Theme.px(3.5F);
        float gap = Theme.px(2.5F);
        for (int i = 0; i < 5; i++) {
            float barH = Theme.px(6 + i * 3);
            float bx = right - (5 - i) * (barW + gap);
            boolean lit = online ? i < filled : i == 0;
            int color = online ? PING_GOOD : PING_BAD;
            Render2D.fillRounded(g, bx, centerY + Theme.px(9) - barH, barW, barH, Theme.px(1.5F),
                    lit ? color : ColorUtil.withAlpha(Theme.TEXT_SECONDARY, 70));
        }
    }

    // ------------------------------------------------------------------
    // Input and actions
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0
                && mx >= listX() && mx <= listX() + listW()
                && my >= listY() && my <= listY() + listH()) {
            float cy = listY() - scroll;
            for (ServerData data : visible) {
                if (my >= cy && my <= cy + cardH()) {
                    boolean doubleClick = data == lastClickTarget
                            && System.currentTimeMillis() - lastClickMs < 400;
                    lastClickMs = System.currentTimeMillis();
                    lastClickTarget = data;
                    selected = data;
                    joinButton.active = true;
                    setFocused(null);
                    if (doubleClick) {
                        joinSelected();
                    }
                    return true;
                }
                cy += cardStride();
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        scroll -= (float) dy * cardStride();
        clampScroll();
        return true;
    }

    private void clampScroll() {
        float max = Math.max(0, visible.size() * cardStride() - listH());
        scroll = Math.max(0, Math.min(scroll, max));
    }

    private void joinSelected() {
        if (selected != null) {
            join(selected);
        }
    }

    private void join(ServerData data) {
        ConnectScreen.startConnecting(this, this.minecraft,
                ServerAddress.parseString(data.ip), data, false, null);
    }

    private void addServer() {
        ServerData newData = new ServerData("Minecraft Server", "", ServerData.Type.OTHER);
        this.minecraft.setScreen(new EditServerScreen(this, confirmed -> {
            if (confirmed) {
                servers.add(newData, false);
                servers.save();
                ping(newData);
                refilter();
            }
            this.minecraft.setScreen(this);
        }, newData));
    }

    private void directConnect() {
        ServerData temp = new ServerData("Direct Connection", "", ServerData.Type.OTHER);
        this.minecraft.setScreen(new DirectJoinServerScreen(this, confirmed -> {
            if (confirmed) {
                join(temp);
            } else {
                this.minecraft.setScreen(this);
            }
        }, temp));
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
