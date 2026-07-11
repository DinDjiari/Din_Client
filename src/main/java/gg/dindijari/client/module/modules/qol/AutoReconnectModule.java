package gg.dindijari.client.module.modules.qol;

import gg.dindijari.client.gui.screen.ReconnectCountdownScreen;
import gg.dindijari.client.module.Category;
import gg.dindijari.client.module.Module;
import gg.dindijari.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Automatically reconnects after an unexpected disconnect from a multiplayer
 * server. When the vanilla "Disconnected" screen would open (never on a
 * user-chosen disconnect, which goes straight to the menu), it is replaced by
 * a themed countdown that reconnects after the configured delay, up to the
 * configured number of attempts per connection. Cancel and Reconnect Now are
 * always available; a successful login resets the attempt counter.
 */
public final class AutoReconnectModule extends Module {

    private final NumberSetting delaySeconds = new NumberSetting(
            "Delay", "Seconds to wait before reconnecting.", 5, 1, 60, 1);
    private final NumberSetting maxAttempts = new NumberSetting(
            "Attempts", "Maximum automatic attempts per disconnect.", 3, 1, 10, 1);

    private ServerData lastServer;
    private int attemptsUsed;

    /**
     * Creates the module and registers its (enabled-gated) listeners.
     */
    public AutoReconnectModule() {
        super("Auto Reconnect", "Reconnects automatically after losing connection.",
                Category.UTILITY);
        addSetting(delaySeconds, maxAttempts);
        NeoForge.EVENT_BUS.addListener(this::onLogin);
        NeoForge.EVENT_BUS.addListener(this::onScreenOpening);
    }

    private void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        lastServer = Minecraft.getInstance().getCurrentServer();
        attemptsUsed = 0;
    }

    private void onScreenOpening(ScreenEvent.Opening event) {
        if (!isEnabled() || lastServer == null || lastServer.isLan()
                || event.getNewScreen() == null
                || event.getNewScreen().getClass() != DisconnectedScreen.class) {
            return;
        }
        if (attemptsUsed >= maxAttempts.getAsInt()) {
            return;
        }
        event.setNewScreen(new ReconnectCountdownScreen(this, lastServer));
    }

    /**
     * Records one automatic attempt being spent.
     */
    public void countAttempt() {
        attemptsUsed++;
    }

    /** @return automatic attempts already used for this disconnect */
    public int attemptsUsed() {
        return attemptsUsed;
    }

    /** @return the configured maximum automatic attempts */
    public int maxAttempts() {
        return maxAttempts.getAsInt();
    }

    /** @return the configured reconnect delay in seconds */
    public int delaySeconds() {
        return delaySeconds.getAsInt();
    }
}
