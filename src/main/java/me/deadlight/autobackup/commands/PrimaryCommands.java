package me.deadlight.autobackup.commands;

import com.jcraft.jsch.SftpException;
import me.deadlight.autobackup.AutoBackup;
import me.deadlight.autobackup.BackupTask;
import me.deadlight.autobackup.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;

public class PrimaryCommands implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("autobackup.admin")) {
            sender.sendMessage(Utils.colority("&cYou do not have permission to use this command."));
        }
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length == 0) {
                player.sendMessage(Utils.colority("&cUsage: /autobackup backup"));
                return true;
            } else {
                if (args[0].equalsIgnoreCase("backup")) {
                    if (BackupTask.backuping) {
                        player.sendMessage(Utils.colority("&cBackup is already running. Please do it later."));
                        return true;
                    }
                    player.sendMessage(Utils.colority("&aStarting backup... This may take a while. Please wait."));

                    Bukkit.getScheduler().runTaskLaterAsynchronously(AutoBackup.pluginInstance, new Runnable() {
                        @Override
                        public void run() {
                            try {
                                BackupTask.startBackup();
                                player.sendMessage(Utils.colority("&aBackup complete."));
                            } catch (IOException | SftpException e) {
                                BackupTask.backuping = false;
                                player.sendMessage(Utils.colority("&cAn error occurred while backing up. Please check the console."));
                                e.printStackTrace();
                            }
                        }
                    }, 0);
                } else {
                    player.sendMessage(Utils.colority("&cUsage: /autobackup backup"));
                }
            }

        } else {

            if (args.length == 0) {
                sender.sendMessage(Utils.colority("&cUsage: /autobackup backup"));
                return true;
            } else {
                if (args[0].equalsIgnoreCase("backup")) {
                    if (BackupTask.backuping) {
                        sender.sendMessage(Utils.colority("&cBackup is already running. Please do it later."));
                        return true;
                    }
                    sender.sendMessage(Utils.colority("&aStarting backup... This may take a while. Please wait."));

                        Bukkit.getScheduler().runTaskLaterAsynchronously(AutoBackup.pluginInstance, new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    BackupTask.startBackup();
                                    sender.sendMessage(Utils.colority("&aBackup complete."));
                                } catch (IOException | SftpException e) {
                                    BackupTask.backuping = false;
                                    sender.sendMessage(Utils.colority("&cAn error occurred while backing up. Please check the console."));
                                    e.printStackTrace();
                                }
                            }
                        }, 0);

                } else {
                    sender.sendMessage(Utils.colority("&cUsage: /autobackup backup"));
                }
            }

        }

        return false;
    }
}