package me.deadlight.autobackup;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import me.deadlight.autobackup.configmanager.ConfigLoader;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BackupTask {
    private static boolean isInitialized;
    public static boolean backuping = false;

    public static void initializeBackupTask() {
        //avoid running multiple times in case of reload
        if (isInitialized) {
            return;
        }
        isInitialized = true;
        Bukkit.getScheduler().runTaskTimerAsynchronously(AutoBackup.pluginInstance, new Runnable() {
            @Override
            public void run() {

                if (AutoBackup.activeSession.isConnected()) {
                    try {
                        startBackup();
                    } catch (IOException | SftpException e) {
                        Utils.logConsole("Backup operation failed: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    try {
                        AutoBackup.activeSession = AutoBackup.pluginInstance.setupConnectionToSFTP();
                        AutoBackup.activeSession.connect();
                        startBackup();
                    } catch (JSchException | IOException | SftpException e) {
                        Utils.logConsole("Backup operation failed: " + e.getMessage());
                        e.printStackTrace();

                    }
                }

            }
        }, 20L * ConfigLoader.backupInterval, 20L * ConfigLoader.backupInterval);
    }


    public static void startBackup() throws IOException, SftpException {
        if (backuping) {
            return;
        }
        backuping = true;
        //basically zips the server folder and uploads it to the SFTP server

        //get the plugin config folder
        String pluginFolder = AutoBackup.pluginInstance.getDataFolder().getAbsolutePath();
        //go one level up to the server folder
        String serverFolder = pluginFolder.substring(0, pluginFolder.lastIndexOf(File.separator));
        //zip only folders inside the serverFolder path

        //get date and time for the backup file name as String
        String timeStamp = new SimpleDateFormat("yyyy.MM.dd - HH.mm.ss").format(new Date());

        //get folders in the server folder
        File[] allFiles = new File(serverFolder).listFiles();
        //all config folders
        List<File> allConfigFolders = new ArrayList<>();
        for (File folder : allFiles) {
            if (folder.isDirectory() && !folder.getName().equalsIgnoreCase("AutoBackup")) {
                allConfigFolders.add(folder);
            }
        }
        //get all active world folders
        List<File> allWorldFolders = new ArrayList<>();
        for (World world : AutoBackup.pluginInstance.getServer().getWorlds()) {
            File session = new File(Bukkit.getWorldContainer(), world.getName() + File.separator + "session.lock");
            session.delete();
            allWorldFolders.add(world.getWorldFolder());
        }

//----------------------------------------------------------------------------------------------------------------------

        if (!ConfigLoader.sendAsFolder) {
            ZipUtility zipUtility = new ZipUtility();
            File theStampFolder = new File(pluginFolder + File.separator + timeStamp);
            //create the folder theStampFolder
            theStampFolder.mkdirs();
            //----------------back up folder is ready ^^^------------------

            zipUtility.zip(allConfigFolders, pluginFolder + File.separator + timeStamp + File.separator + "configs.zip");

            zipUtility.zip(allWorldFolders, pluginFolder + File.separator + timeStamp + File.separator + "worlds.zip");
            //finished zipping the world folders
            for (World world : AutoBackup.pluginInstance.getServer().getWorlds()) {
                File session = new File(Bukkit.getWorldContainer(), world.getName() + File.separator + "session.lock");
                try {
                    session.createNewFile();
                    DataOutputStream dataoutputstream = new DataOutputStream(new FileOutputStream(session));

                    try {
                        dataoutputstream.writeLong(System.currentTimeMillis());
                    } finally {
                        dataoutputstream.close();
                    }
                } catch (IOException ioexception) {
                    Utils.logConsole("&eCould not create session lock for " + world.getName());
                }

            }
            //----------------Files are zipped and ready ^^^------------------
            List<File> finalFile = new ArrayList<>();
            finalFile.add(new File(theStampFolder.getAbsoluteFile() + File.separator + "configs.zip"));
            finalFile.add(new File(theStampFolder.getAbsoluteFile() + File.separator + "worlds.zip"));
            zipUtility.zip(finalFile,
                    pluginFolder + File.separator + timeStamp + ".zip");
            //delete the timestamp folder
            File zipFile = new File(pluginFolder + File.separator + timeStamp + ".zip");
            if (!zipFile.exists()) {
                Utils.logConsole("&cFailed to create the zip file.");
                return;
            }
            if (AutoBackup.pluginInstance == null || !AutoBackup.activeSession.isConnected()) {
                try {
                    assert AutoBackup.pluginInstance != null;
                    AutoBackup.activeSession = AutoBackup.pluginInstance.setupConnectionToSFTP();
                    AutoBackup.activeSession.connect();

                    Utils.logConsole("&aConnected to the SFTP server.");
                } catch (JSchException e) {
                    Utils.logConsole("&cFailed to connect to the SFTP server.");
                    e.printStackTrace();
                    return;
                }
            }
            Utils.logConsole("&aUploading the backup file to the SFTP server...");
            try {
                AutoBackup.activeSession.put(zipFile.getAbsolutePath(), ConfigLoader.remotePath + ConfigLoader.targetMachineSeperator + timeStamp + ".zip");
            } catch (Exception e) {
                Utils.logConsole("&eMaking directory &7" + ConfigLoader.remotePath);
                AutoBackup.activeSession.mkdir(ConfigLoader.remotePath);
                AutoBackup.activeSession.put(zipFile.getAbsolutePath(), ConfigLoader.remotePath + ConfigLoader.targetMachineSeperator + timeStamp + ".zip");
            }
            zipFile.delete();
            FileUtils.forceDelete(theStampFolder);
            Utils.logConsole("&aUpload has finished. Located at: " + ConfigLoader.remotePath + ConfigLoader.targetMachineSeperator + timeStamp + ".zip");
            backuping = false;
        } else {
            //send as folder
            try {
                AutoBackup.activeSession.mkdir(ConfigLoader.remotePath);
            } catch (SftpException e) {
                //just ignore
            }
            String correctRemotePath = ConfigLoader.remotePath;
            if (!correctRemotePath.endsWith(ConfigLoader.targetMachineSeperator)) {
                //remove the last slash
                correctRemotePath = correctRemotePath.substring(0, correctRemotePath.length() - 1);
            }
            AutoBackup.activeSession.mkdir(correctRemotePath + ConfigLoader.targetMachineSeperator + timeStamp);
            AutoBackup.activeSession.mkdir(correctRemotePath + ConfigLoader.targetMachineSeperator + timeStamp + ConfigLoader.targetMachineSeperator + "configs");
            AutoBackup.activeSession.mkdir(correctRemotePath + ConfigLoader.targetMachineSeperator + timeStamp + ConfigLoader.targetMachineSeperator + "worlds");
            for (File file : allConfigFolders) {
                putFolder(file.getAbsolutePath(), correctRemotePath + ConfigLoader.targetMachineSeperator + timeStamp + ConfigLoader.targetMachineSeperator + "configs");
            }
            for (File file : allWorldFolders) {
                putFolder(file.getAbsolutePath(), correctRemotePath + ConfigLoader.targetMachineSeperator + timeStamp + ConfigLoader.targetMachineSeperator + "worlds");
            }
            Utils.logConsole("&aUpload has finished. Located at: " + correctRemotePath + ConfigLoader.targetMachineSeperator + timeStamp);
            backuping = false;
        }

    }
    //bannerboard path | /configs path
    //fonts path | bannerboard path
    private static void putFolder(String path, String remotePath) throws SftpException {
        File fPath = new File(path);
        AutoBackup.activeSession.mkdir(remotePath + ConfigLoader.targetMachineSeperator + fPath.getName());
        File[] files = fPath.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    putFolder(file.getAbsolutePath(), remotePath + ConfigLoader.targetMachineSeperator + fPath.getName());
                } else {
                    AutoBackup.activeSession.put(file.getAbsolutePath(), remotePath + ConfigLoader.targetMachineSeperator + fPath.getName());
                }
            }
        }
    }


}
