package larrytllama.pvcmappermod;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId;
import net.minecraft.network.chat.Component;

public class SettingsProvider {
    /**
     * Number 1-8 (inclusive)
     */
    public int miniMapZoom = 8;

    public MiniMapPositions miniMapPos = MiniMapPositions.TOP_RIGHT;

    public ShowMiniMap miniMapEnabled = ShowMiniMap.IN_PVC_ONLY;

    public MapTileSource mapTileSource = MapTileSource.PVC_MAPPER;

    public DefaultWorld defaultWorld = DefaultWorld.CURRENT;

    public boolean useDarkTiles = false;

    Path path = FabricLoader.getInstance().getConfigDir().resolve("pvcmapper.json");


    public SettingsProvider() {
        updateSettings();
    }

    public void updateSettings() {
        Gson gson = new Gson();
        if(Files.exists(path)) {
            try {
                SettingsJSON settingsFromFile = gson.fromJson(Files.readString(path), SettingsJSON.class);
                miniMapZoom = settingsFromFile.miniMapZoom;
                miniMapEnabled = settingsFromFile.miniMapEnabled;
                mapTileSource = settingsFromFile.mapTileSource;
                defaultWorld = settingsFromFile.defaultWorld;
                useDarkTiles = settingsFromFile.useDarkTiles;
            } catch(Exception e) {
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
        Gson gson = new Gson();
        SettingsJSON settingsToSet = new SettingsJSON();
        settingsToSet.defaultWorld = defaultWorld;
        settingsToSet.mapTileSource = mapTileSource;
        settingsToSet.miniMapEnabled = miniMapEnabled;
        settingsToSet.miniMapPos = miniMapPos;
        settingsToSet.miniMapZoom = miniMapZoom;
        settingsToSet.useDarkTiles = useDarkTiles;
        try {
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
    int miniMapZoom;
    MiniMapPositions miniMapPos;
    ShowMiniMap miniMapEnabled;
    MapTileSource mapTileSource;
    DefaultWorld defaultWorld;
    boolean useDarkTiles;
}

enum MiniMapPositions {
    TOP_LEFT,
    TOP_RIGHT
};  
enum ShowMiniMap {
    ENABLED,
    IN_PVC_ONLY,
    DISABLED
};
enum MapTileSource {
    PVC_DIRECT,
    PVC_MAPPER
};
enum DefaultWorld {
    CURRENT,
    OVERWORLD,
    NETHER,
    TERRA2
};