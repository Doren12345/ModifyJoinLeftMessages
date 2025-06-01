package org.doren.modifyjoinleftmessages;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

    private final MiniMessage mini = MiniMessage.miniMessage();
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();
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
        if (useComponentAPI) {
            event.joinMessage(formatComponent("join-message", event.getPlayer()));
        } else {
            event.setJoinMessage(formatLegacy("join-message", event.getPlayer()));
        }
    }

    @EventHandler
    @SuppressWarnings("deprecation")
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (useComponentAPI) {
            event.quitMessage(formatComponent("quit-message", event.getPlayer()));
        } else {
            event.setQuitMessage(formatLegacy("quit-message", event.getPlayer()));
        }
    }

    private Component formatComponent(String key, Player player) {
        String raw = formatRaw(key, player);
        return mini.deserialize(raw);
    }

    private String formatLegacy(String key, Player player) {
        String raw = formatRaw(key, player);
        String legacyText = legacy.serialize(mini.deserialize(raw));
        return ChatColor.translateAlternateColorCodes('&', legacyText.replace('§', '&'));
    }

    private String formatRaw(String key, Player player) {
        String msg = getConfig().getString(key, "").replace("{player}", player.getName());
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            msg = PlaceholderAPI.setPlaceholders(player, msg);
        }
        return msg;
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
            Component msg = mini.deserialize(raw);
            sender.sendMessage(msg);
        } else {
            String legacyText = legacy.serialize(mini.deserialize(raw));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', legacyText.replace('§', '&')));
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("mjlm")) return false;

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
