package org.doren.modifyjoinleftmessages;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class ModifyJoinLeftMessages extends JavaPlugin implements Listener {

    private final MessageFormatter formatter = new MessageFormatter();
    private boolean useComponentAPI;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        useComponentAPI = supportsComponentMessages();
        getLogger().info("ModifyJoinLeftMessages enabled. Using Component API: " + useComponentAPI);
    }

    @EventHandler
    @SuppressWarnings("deprecation")
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (getConfig().getBoolean("proxy-only", false)) {
            if (useComponentAPI) {
                event.joinMessage((Component) null);
            } else {
                event.setJoinMessage(null);
            }
            return;
        }

        Player player = event.getPlayer();
        String joinMessageKey = !player.hasPlayedBefore() && hasMessage("first-join") ? "first-join" : "join";

        if (!isMessageEnabled(joinMessageKey)) {
            if (useComponentAPI) {
                event.joinMessage((Component) null);
            } else {
                event.setJoinMessage(null);
            }
            return;
        }

        if (useComponentAPI) {
            event.joinMessage(formatComponent(joinMessageKey, player));
        } else {
            event.setJoinMessage(formatLegacy(joinMessageKey, player));
        }
    }

    @EventHandler
    @SuppressWarnings("deprecation")
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (getConfig().getBoolean("proxy-only", false) || !isMessageEnabled("quit")) {
            if (useComponentAPI) {
                event.quitMessage((Component) null);
            } else {
                event.setQuitMessage(null);
            }
            return;
        }

        if (useComponentAPI) {
            event.quitMessage(formatComponent("quit", event.getPlayer()));
        } else {
            event.setQuitMessage(formatLegacy("quit", event.getPlayer()));
        }
    }

    private Component formatComponent(String key, Player player) {
        return formatter.formatComponent(formatRaw(key, player));
    }

    private String formatLegacy(String key, Player player) {
        return formatter.formatLegacy(formatRaw(key, player));
    }

    private String formatRaw(String key, Player player) {
        String msg = formatter.replacePlayer(getMessage(key), player.getName());
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            msg = PlaceholderAPI.setPlaceholders(player, msg);
        }
        return msg;
    }

    private boolean isMessageEnabled(String key) {
        if (getConfig().contains(key + ".enabled")) {
            return getConfig().getBoolean(key + ".enabled", true);
        }
        return hasMessage(key);
    }

    private boolean hasMessage(String key) {
        return !getMessage(key).isEmpty();
    }

    private String getMessage(String key) {
        String nested = getConfig().getString(key + ".message");
        if (nested != null) {
            return nested;
        }

        if ("join".equals(key)) {
            return getConfig().getString("join-message", "");
        }
        if ("first-join".equals(key)) {
            return getConfig().getString("first-join-message", "");
        }
        if ("quit".equals(key)) {
            return getConfig().getString("quit-message", "");
        }
        return getConfig().getString(key, "");
    }

    private boolean supportsComponentMessages() {
        try {
            PlayerJoinEvent.class.getMethod("joinMessage", Component.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private void sendMessage(CommandSender sender, String configKey) {
        String raw = getConfig().getString("messages." + configKey, "");
        if (useComponentAPI && sender instanceof Player) {
            sender.sendMessage(formatter.formatComponent(raw));
        } else {
            sender.sendMessage(formatter.formatLegacy(raw));
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("mjlm")) {
            return false;
        }

        if (!sender.hasPermission("modifyjoinleftmessages.base")) {
            sendMessage(sender, "no-permission");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (sender.hasPermission("modifyjoinleftmessages.reload") || sender.isOp()) {
                reloadConfig();
                sendMessage(sender, "reload-success");
            } else {
                sendMessage(sender, "no-permission");
            }
        } else {
            sendMessage(sender, "wrong-usage");
        }
        return true;
    }
}
