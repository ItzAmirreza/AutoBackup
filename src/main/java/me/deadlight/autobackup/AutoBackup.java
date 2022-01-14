package me.deadlight.autobackup;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import me.deadlight.autobackup.commands.PrimaryCommands;
import me.deadlight.autobackup.configmanager.ConfigLoader;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class AutoBackup extends JavaPlugin {
    public static AutoBackup pluginInstance;
    public static ChannelSftp activeSession;

    @Override
    public void onEnable() {
        pluginInstance = this;
        Utils.logConsole("&eEnabling AutoBackup. Please wait a few moment...");
        // Plugin startup logic
        boolean result = ConfigLoader.loadConfig();
        if (!result) {
            return;
        }

        try {
            Utils.logConsole("&eInitializing connection to SFTP...");
            activeSession = setupConnectionToSFTP();
            activeSession.connect();
        } catch (JSchException e) {
            Utils.logConsole("&cFailed to initialize connection to SFTP server. The problem is either with the data you provided or with the server itself. ");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getServer().getPluginCommand("autobackup").setExecutor(new PrimaryCommands());
        BackupTask.initializeBackupTask();
        Utils.logConsole("&aAutoBackup has been enabled. Backup task has been scheduled.");

    }

    public ChannelSftp setupConnectionToSFTP() throws JSchException {
        JSch jsch = new JSch();
        JSch.setConfig("StrictHostKeyChecking", "no");
        Session jschSession = jsch.getSession(ConfigLoader.username, ConfigLoader.ipAdress, ConfigLoader.port);
        jschSession.setPassword(ConfigLoader.password);
        jschSession.setServerAliveInterval(5 * 1000);
        jschSession.connect();
        return (ChannelSftp) jschSession.openChannel("sftp");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        Utils.logConsole("&cShuttting down AutoBackup...");
        Bukkit.getScheduler().cancelTasks(this);
        if (activeSession != null || !activeSession.isClosed()) {
            activeSession.disconnect();
        }
    }
}
