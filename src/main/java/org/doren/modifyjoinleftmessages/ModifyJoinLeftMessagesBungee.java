package org.doren.modifyjoinleftmessages;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

public final class ModifyJoinLeftMessagesBungee extends Plugin implements Listener {

    private final MessageFormatter formatter = new MessageFormatter();
    private final Map<String, Boolean> knownPlayers = new LinkedHashMap<>();
    private VelocityConfig config;

    @Override
    public void onEnable() {
        config = new VelocityConfig(getDataFolder().toPath());
        reloadInternal();
        ProxyServer.getInstance().getPluginManager().registerListener(this, this);
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new ReloadCommand());
        getLogger().info("ModifyJoinLeftMessages enabled on BungeeCord.");
    }

    @EventHandler
    public void onPlayerJoin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        String key = markSeen(player) && hasMessage("first-join") ? "first-join" : "join";
        if (!isMessageEnabled(key)) {
            return;
        }
        broadcast(key, player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerDisconnectEvent event) {
        if (!isMessageEnabled("quit")) {
            return;
        }
        broadcast("quit", event.getPlayer());
    }

    @EventHandler
    public void onPlayerSwitchServer(ServerSwitchEvent event) {
        if (!isMessageEnabled("swap-server") || event.getFrom() == null || event.getPlayer().getServer() == null) {
            return;
        }
        broadcastServerSwitch(event.getPlayer(), event.getFrom().getName(),
                event.getPlayer().getServer().getInfo().getName());
    }

    private void broadcast(String key, ProxiedPlayer player) {
        String raw = formatter.replacePlayer(getMessage(key), player.getName());
        if (raw.isEmpty()) {
            return;
        }
        BaseComponent[] message = TextComponent.fromLegacyText(formatter.formatLegacy(raw));
        ProxyServer.getInstance().getConsole().sendMessage(message);
        for (ProxiedPlayer onlinePlayer : ProxyServer.getInstance().getPlayers()) {
            onlinePlayer.sendMessage(message);
        }
    }

    private void broadcastServerSwitch(ProxiedPlayer player, String oldServer, String newServer) {
        String raw = formatter.replacePlayer(getMessage("swap-server"), player.getName())
                .replace("{old_server}", getServerDisplayName(oldServer))
                .replace("{new_server}", getServerDisplayName(newServer));
        if (raw.isEmpty()) {
            return;
        }
        BaseComponent[] message = TextComponent.fromLegacyText(formatter.formatLegacy(raw));
        ProxyServer.getInstance().getConsole().sendMessage(message);
        for (ProxiedPlayer onlinePlayer : ProxyServer.getInstance().getPlayers()) {
            onlinePlayer.sendMessage(message);
        }
    }

    private String getServerDisplayName(String serverName) {
        return config.getString("messages.server-name." + serverName, serverName);
    }

    private boolean markSeen(ProxiedPlayer player) {
        if (!config.getBoolean("calculate-first-join", true)) {
            return false;
        }
        String uuid = player.getUniqueId().toString();
        boolean firstJoin = !knownPlayers.containsKey(uuid);
        if (firstJoin) {
            knownPlayers.put(uuid, Boolean.TRUE);
            saveKnownPlayers();
        }
        return firstJoin;
    }

    private void reloadInternal() {
        try (InputStream resource = getClass().getClassLoader().getResourceAsStream("config.yml")) {
            config.ensureDefaultConfig(resource);
            config.load();
            knownPlayers.clear();
            knownPlayers.putAll(config.loadKnownPlayers());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load BungeeCord config", e);
        }
    }

    private void saveKnownPlayers() {
        try {
            config.saveKnownPlayers(knownPlayers);
        } catch (IOException e) {
            getLogger().severe("Failed to save known BungeeCord players: " + e.getMessage());
        }
    }

    private void sendSourceMessage(CommandSender sender, String key) {
        String raw = config.getString("messages." + key, "");
        sender.sendMessage(TextComponent.fromLegacyText(formatter.formatLegacy(raw)));
    }

    private boolean isMessageEnabled(String key) {
        if (config.has(key + ".enabled")) {
            return config.getBoolean(key + ".enabled", true);
        }
        return hasMessage(key);
    }

    private boolean hasMessage(String key) {
        return !getMessage(key).isEmpty();
    }

    private String getMessage(String key) {
        String nested = config.getString(key + ".message", null);
        if (nested != null) {
            return nested;
        }

        if ("join".equals(key)) {
            return config.getString("join-message", "");
        }
        if ("first-join".equals(key)) {
            return config.getString("first-join-message", "");
        }
        if ("quit".equals(key)) {
            return config.getString("quit-message", "");
        }
        if ("swap-server".equals(key)) {
            return config.getString("swap-server-message", "");
        }
        return config.getString(key, "");
    }

    private final class ReloadCommand extends Command {

        private ReloadCommand() {
            super("mjlm");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (!sender.hasPermission("modifyjoinleftmessages.base")) {
                sendSourceMessage(sender, "no-permission");
                return;
            }
            if (args.length == 1 && "reload".equalsIgnoreCase(args[0])) {
                if (!sender.hasPermission("modifyjoinleftmessages.reload")) {
                    sendSourceMessage(sender, "no-permission");
                    return;
                }
                reloadInternal();
                sendSourceMessage(sender, "reload-success");
                return;
            }
            sendSourceMessage(sender, "wrong-usage");
        }
    }
}
