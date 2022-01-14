package me.deadlight.autobackup.configmanager;

import me.deadlight.autobackup.AutoBackup;
import me.deadlight.autobackup.Utils;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;

public class ConfigLoader {

    public static FileConfiguration configuration;
    public static String ipAdress;
    public static int port = 22;
    public static String username;
    public static String password;
    public static String remotePath;
    public static boolean sendAsFolder = false;
    public static int backupInterval;
    public static int aliveInterval = 5000;
    public static String targetMachineSeperator;




    public static boolean loadConfig() {
        File file = new File(AutoBackup.pluginInstance.getDataFolder().getAbsolutePath());
        if (!file.exists()) {
            Utils.colority("&eSeems it's the first time you run this plugin. Creating config file...");
            AutoBackup.pluginInstance.saveDefaultConfig();
            Utils.logConsole("&eConfig file created.");
            Utils.logConsole("Stopping the server, so you can edit the config file.");
            AutoBackup.pluginInstance.getServer().shutdown();
            return false;
        }
        AutoBackup.pluginInstance.saveDefaultConfig();
        configuration = AutoBackup.pluginInstance.getConfig();
        ipAdress = configuration.getString("SFTP.hostname");
        port = configuration.getInt("SFTP.port");
        username = configuration.getString("SFTP.username");
        password = configuration.getString("SFTP.password");
        remotePath = configuration.getString("SFTP.remoteBackupPath", "/opt/AutoBackup/");
        sendAsFolder = configuration.getBoolean("upload-as-folder", false);
        backupInterval = configuration.getInt("backup-interval", 3600);
        aliveInterval = configuration.getInt("SFTP.aliveInterval", 5000);
        targetMachineSeperator = configuration.getString("SFTP.taget-machine-separator", "/");
        return true;
    }


}
