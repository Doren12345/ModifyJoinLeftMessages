package org.doren.modifyjoinleftmessages;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import me.clip.placeholderapi.PlaceholderAPI;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModifyJoinLeftMessages extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("ModifyJoinLeftMessages enabled.");
    }

    @EventHandler
    @SuppressWarnings("deprecation")
    public void onPlayerJoin(PlayerJoinEvent event) {
        String msg = formatMessage("join-message", event.getPlayer());
        event.setJoinMessage(msg);
    }

    @EventHandler
    @SuppressWarnings("deprecation")
    public void onPlayerQuit(PlayerQuitEvent event) {
        String msg = formatMessage("quit-message", event.getPlayer());
        event.setQuitMessage(msg);
    }

    private String formatMessage(String key, Player player) {
        String msg = getConfig().getString(key, "").replace("{player}", player.getName());
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) msg = PlaceholderAPI.setPlaceholders(player, msg);
        msg = translateHexColors(msg);
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("mjlm")) return false;

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (sender.hasPermission("mjlm.reload") || sender.isOp()) {
                reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "配置重新加載完成。");
            } else {
                sender.sendMessage(ChatColor.RED + "您沒有權限。");
            }
        } else {
            sender.sendMessage(ChatColor.YELLOW + "用法: /mjlm reload");
        }
        return true;
    }

    private String translateHexColors(String message) {
        Matcher matcher = Pattern.compile("#([A-Fa-f0-9]{6})").matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, net.md_5.bungee.api.ChatColor.of(matcher.group()).toString());
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
