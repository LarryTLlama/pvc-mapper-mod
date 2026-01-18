package larrytllama.pvcmappermod;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.google.gson.Gson;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId;
import net.minecraft.network.chat.Component;

public class SettingsProvider {
    private static final SettingsProvider INSTANCE = new SettingsProvider();

    public static SettingsProvider getInstance() {
        return INSTANCE;
    }
    /**
     * Number 1-8 (inclusive)
     */
    public int miniMapZoom = 8;

    public MiniMapPositions miniMapPos = MiniMapPositions.TOP_RIGHT;

    public boolean miniMapEnabled = true;

    public String mapTileSource = "https://pvc.coolwebsite.uk/maps/";

    public boolean useDarkTiles = false;

    public boolean checkForUpdates = true;

    public double minimapScale = 1;

    public BigMapPos bigMapPos = BigMapPos.CENTRE_ON_PLAYER;

    Path path = FabricLoader.getInstance().getConfigDir().resolve("pvcmapper.json");

    public SettingsProvider() {
        updateSettings();
    }

    public void updateSettings() {
        Gson gson = new Gson();
        if(Files.exists(path)) {
            try {
                System.out.println(path.toString());
                SettingsJSON settingsFromFile = gson.fromJson(Files.readString(path), SettingsJSON.class);
                if(settingsFromFile.minimapScale != 0.0) minimapScale = settingsFromFile.minimapScale;
                if(settingsFromFile.miniMapZoom != 0) miniMapZoom = settingsFromFile.miniMapZoom;
                if(settingsFromFile.miniMapPos != null) miniMapPos = settingsFromFile.miniMapPos;
                miniMapEnabled = settingsFromFile.miniMapEnabled;
                if(settingsFromFile.mapTileSource != null) mapTileSource = settingsFromFile.mapTileSource;
                useDarkTiles = settingsFromFile.useDarkTiles;
                if(settingsFromFile.bigMapPos != null) bigMapPos = settingsFromFile.bigMapPos;
                checkForUpdates = settingsFromFile.checkForUpdates;
            } catch(Exception e) {
                System.out.println(e);
                new SystemToast(SystemToastId.FILE_DROP_FAILURE, Component.literal("PVC Mapper Settings Error"), Component.literal("Couldn't open the Setting file, check you have permissions to access it!"));
            }
        } else {
            try {
                Files.createDirectories(path.getParent());
                Files.writeString(
                    path,
                    gson.toJson(new SettingsJSON()),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
                );
            } catch(Exception e) {
                new SystemToast(SystemToastId.LOW_DISK_SPACE, Component.literal("PVC Mapper Settings Error"), Component.literal("Settings did not save and have been set back to defaults. Check your disk!"));
            }
        }
    }

    public void saveSettings() {
        System.out.println("Saving settings!");
        Gson gson = new Gson();
        SettingsJSON settingsToSet = new SettingsJSON();
        settingsToSet.mapTileSource = mapTileSource;
        settingsToSet.miniMapEnabled = miniMapEnabled;
        settingsToSet.minimapScale = minimapScale;
        settingsToSet.miniMapPos = miniMapPos;
        settingsToSet.miniMapZoom = miniMapZoom;
        settingsToSet.useDarkTiles = useDarkTiles;
        settingsToSet.bigMapPos = bigMapPos;
        settingsToSet.checkForUpdates = checkForUpdates;
        try {
            System.out.println("Writing to settings!" + path.getParent().toString());
            Files.createDirectories(path.getParent());
            Files.writeString(
                path,
                gson.toJson(settingsToSet),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch(Exception e) {
            new SystemToast(SystemToastId.LOW_DISK_SPACE, Component.literal("PVC Mapper Settings Error"), Component.literal("Settings did not save and have been set back to defaults. Check your disk!"));
        }
    }
}

class SettingsJSON {
    int miniMapZoom = 8;
    double minimapScale = 1;
    MiniMapPositions miniMapPos = MiniMapPositions.TOP_RIGHT;
    boolean miniMapEnabled = true;
    String mapTileSource = "https://pvc.coolwebsite.uk/maps/";
    boolean useDarkTiles = false;
    BigMapPos bigMapPos = BigMapPos.CENTRE_ON_PLAYER;
    boolean checkForUpdates = true;
}

enum MiniMapPositions {
    TOP_LEFT,
    TOP_RIGHT
};  

enum BigMapPos {
    CENTRE_ON_PLAYER,
    CENTRE_ON_SPAWN
};  