package me.deadlight.autobackup;

import org.bukkit.ChatColor;

public class Utils {

    public static String pluginPrefix = "&8[&bAutoBackup&8] ";

    public static String colority(String str) {
        return ChatColor.translateAlternateColorCodes('&', str);
    }

    public static void logConsole(String str) {
        AutoBackup.pluginInstance.getServer().getConsoleSender().sendMessage(colority(pluginPrefix + str));
    }




}
