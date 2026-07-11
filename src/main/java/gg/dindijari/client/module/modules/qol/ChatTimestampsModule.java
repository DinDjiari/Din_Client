package gg.dindijari.client.module.modules.qol;

import gg.dindijari.client.module.Category;
import gg.dindijari.client.module.Module;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Prepends a dark-grey {@code [HH:mm]} timestamp to every received chat
 * message (player and system) via the supported
 * {@link ClientChatReceivedEvent}. Only affects how messages display locally.
 */
public final class ChatTimestampsModule extends Module {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Creates the module and registers its (enabled-gated) chat listener.
     */
    public ChatTimestampsModule() {
        super("Chat Timestamps", "Shows a [HH:mm] timestamp on chat messages.",
                Category.UTILITY);
        NeoForge.EVENT_BUS.addListener(this::onChat);
    }

    private void onChat(ClientChatReceivedEvent event) {
        if (!isEnabled()) {
            return;
        }
        // Action-bar overlay text is transient status, not chat - leave it alone.
        if (event instanceof ClientChatReceivedEvent.System system && system.isOverlay()) {
            return;
        }
        event.setMessage(Component.empty()
                .append(Component.literal("[" + LocalTime.now().format(FORMAT) + "] ")
                        .withStyle(ChatFormatting.DARK_GRAY))
                .append(event.getMessage()));
    }
}
