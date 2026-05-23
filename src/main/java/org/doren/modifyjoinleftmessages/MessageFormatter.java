package org.doren.modifyjoinleftmessages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class MessageFormatter {

    private final MiniMessage mini = MiniMessage.miniMessage();
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();

    public Component formatComponent(String raw) {
        return mini.deserialize(raw);
    }

    public String formatLegacy(String raw) {
        return legacy.serialize(mini.deserialize(raw));
    }

    public String replacePlayer(String raw, String playerName) {
        return raw.replace("{player}", playerName);
    }
}
