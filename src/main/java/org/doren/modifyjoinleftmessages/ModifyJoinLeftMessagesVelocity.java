package org.doren.modifyjoinleftmessages;

import com.google.inject.Inject;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;

@Plugin(
        id = "modifyjoinleftmessages",
        name = "ModifyJoinLeftMessages",
        version = "1.0.0"
)
public final class ModifyJoinLeftMessagesVelocity {

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Path dataDirectory;
    private final MessageFormatter formatter = new MessageFormatter();
    private VelocityConfig config;
    private final Map<String, Boolean> knownPlayers = new LinkedHashMap<>();

    @Inject
    public ModifyJoinLeftMessagesVelocity(ProxyServer proxyServer, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        config = new VelocityConfig(dataDirectory);
        reloadInternal();
        proxyServer.getCommandManager().register("mjlm", new ReloadCommand());
        logger.info("ModifyJoinLeftMessages enabled on Velocity.");
    }

    @Subscribe
    public void onPlayerJoin(PostLoginEvent event) {
        Player player = event.getPlayer();
        String key = markSeen(player) && hasMessage("first-join") ? "first-join" : "join";
        if (!isMessageEnabled(key)) {
            return;
        }
        broadcast(key, player);
    }

    @Subscribe
    public void onPlayerQuit(DisconnectEvent event) {
        if (!isMessageEnabled("quit")) {
            return;
        }
        broadcast("quit", event.getPlayer());
    }

    private void broadcast(String key, Player player) {
        String raw = formatter.replacePlayer(getMessage(key), player.getUsername());
        if (raw.isEmpty()) {
            return;
        }
        var message = formatter.formatComponent(raw);
        proxyServer.getConsoleCommandSource().sendMessage(message);
        proxyServer.getAllPlayers().forEach(onlinePlayer -> onlinePlayer.sendMessage(message));
    }

    private boolean markSeen(Player player) {
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
            throw new IllegalStateException("Unable to load Velocity config", e);
        }
    }

    private void saveKnownPlayers() {
        try {
            config.saveKnownPlayers(knownPlayers);
        } catch (IOException e) {
            logger.error("Failed to save known Velocity players.", e);
        }
    }

    private void sendSourceMessage(SimpleCommand.Invocation invocation, String key) {
        String raw = config.getString("messages." + key, "");
        invocation.source().sendMessage(formatter.formatComponent(raw));
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
        return config.getString(key, "");
    }

    private final class ReloadCommand implements SimpleCommand {

        @Override
        public void execute(Invocation invocation) {
            if (!invocation.source().hasPermission("modifyjoinleftmessages.base")) {
                sendSourceMessage(invocation, "no-permission");
                return;
            }
            if (invocation.arguments().length == 1 && "reload".equalsIgnoreCase(invocation.arguments()[0])) {
                if (!invocation.source().hasPermission("modifyjoinleftmessages.reload")) {
                    sendSourceMessage(invocation, "no-permission");
                    return;
                }
                reloadInternal();
                sendSourceMessage(invocation, "reload-success");
                return;
            }
            sendSourceMessage(invocation, "wrong-usage");
        }
    }
}
