package larrytllama.pvcmappermod;

import larrytllama.pvcmappermod.utils.*;

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

    public String mapTileSource = NetworkUtils.BASE_URL + "/maps/";

    public boolean useDarkTiles = false;

    public boolean checkForUpdates = true;

    public int minimapSizePercent = 0;

    public double minimapScale = 1;

    public MinimapBackground minimapBackground = MinimapBackground.MENU_BG;

    public BigMapPos bigMapPos = BigMapPos.CENTRE_ON_PLAYER;

    public boolean collectData = false;
    public String t2maptoken = "";
    public boolean t2TileUploads = false;
    
    public boolean debugMode = false;
    
    public boolean hideMinimapNetworks = false;

    public OrwellianMeter orwellMeter = OrwellianMeter.ALL;

    public boolean showWelcomePopup = true;

    public boolean showLeavingPopup = true;

    public boolean showInOtherPlaces = false;

    public boolean showPlaces = true;    
    public boolean showAreas = true;    
    public boolean showNetworks = true;    
    public boolean showPlayers = true;    
    public boolean showClaims = false;    

    // Hidden settings for things we need to save but the user can't configure cos I'm lazy
    public boolean shownDataNotice = false;

    Path path = FabricLoader.getInstance().getConfigDir().resolve("pvcmapper.json");

    public SettingsProvider() {
        // Initialization decoupled to PVCMapperModClient to prevent static initialization race conditions
    }

    public void updateSettings() {
        Gson gson = new Gson();
        if(Files.exists(path)) {
            try {
                LogUtils.info("Loading config from: " + path.toString());
                SettingsJSON settingsFromFile = gson.fromJson(Files.readString(path), SettingsJSON.class);
                // converts old minimapScale to closest 25% step
                if(settingsFromFile.minimapSizePercent != 0) {
                    minimapSizePercent = settingsFromFile.minimapSizePercent;
                } else if(settingsFromFile.minimapScale != 0.0 && settingsFromFile.minimapScale != 1.0) {
                    int legacyPct = (int) Math.round(settingsFromFile.minimapScale * 100.0);
                    minimapSizePercent = Math.max(50, ((legacyPct + 12) / 25) * 25);
                }
                if(settingsFromFile.minimapScale != 0.0) minimapScale = settingsFromFile.minimapScale;
                if(settingsFromFile.minimapBackground != null) minimapBackground = settingsFromFile.minimapBackground;
                if(settingsFromFile.miniMapZoom != 0) miniMapZoom = settingsFromFile.miniMapZoom;
                if(settingsFromFile.miniMapPos != null) miniMapPos = settingsFromFile.miniMapPos;
                miniMapEnabled = settingsFromFile.miniMapEnabled;
                if(settingsFromFile.mapTileSource != null) mapTileSource = settingsFromFile.mapTileSource;
                useDarkTiles = settingsFromFile.useDarkTiles;
                if(settingsFromFile.bigMapPos != null) bigMapPos = settingsFromFile.bigMapPos;
                checkForUpdates = settingsFromFile.checkForUpdates;
                collectData = settingsFromFile.collectData;
                t2TileUploads = settingsFromFile.t2TileUploads;
                if(settingsFromFile.t2maptoken != null) t2maptoken = settingsFromFile.t2maptoken;
                debugMode = settingsFromFile.debugMode;
                hideMinimapNetworks = settingsFromFile.hideMinimapNetworks;
                if(settingsFromFile.orwellMeter != null) orwellMeter = settingsFromFile.orwellMeter;
                showWelcomePopup = settingsFromFile.showWelcomePopup;
                showLeavingPopup = settingsFromFile.showLeavingPopup;
                showPlaces = settingsFromFile.showPlaces;
                showAreas = settingsFromFile.showAreas;
                showNetworks = settingsFromFile.showNetworks;
                showPlayers = settingsFromFile.showPlayers;
                showClaims = settingsFromFile.showClaims;
                showInOtherPlaces = settingsFromFile.showInOtherPlaces;
                shownDataNotice = settingsFromFile.shownDataNotice;
            } catch(Exception e) {
                LogUtils.error("Couldn't read settings file", e);
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
        LogUtils.debug("Saving settings!");
        Gson gson = new Gson();
        SettingsJSON settingsToSet = new SettingsJSON();
        settingsToSet.mapTileSource = mapTileSource;
        settingsToSet.miniMapEnabled = miniMapEnabled;
        settingsToSet.minimapSizePercent = minimapSizePercent;
        settingsToSet.minimapScale = minimapSizePercent == 0 ? 1.0 : minimapSizePercent / 100.0;
        settingsToSet.minimapBackground = minimapBackground;
        settingsToSet.miniMapPos = miniMapPos;
        settingsToSet.miniMapZoom = miniMapZoom;
        settingsToSet.useDarkTiles = useDarkTiles;
        settingsToSet.bigMapPos = bigMapPos;
        settingsToSet.checkForUpdates = checkForUpdates;
        settingsToSet.collectData = collectData;
        settingsToSet.t2TileUploads = t2TileUploads;
        settingsToSet.t2maptoken = t2maptoken;
        settingsToSet.debugMode = debugMode;
        settingsToSet.hideMinimapNetworks = hideMinimapNetworks;
        settingsToSet.orwellMeter = orwellMeter;
        settingsToSet.showWelcomePopup = showWelcomePopup;
        settingsToSet.showLeavingPopup = showLeavingPopup;
        settingsToSet.showPlaces = showPlaces;
        settingsToSet.showAreas = showAreas;
        settingsToSet.showNetworks = showNetworks;
        settingsToSet.showPlayers = showPlayers;
        settingsToSet.showClaims = showClaims;
        settingsToSet.showInOtherPlaces = showInOtherPlaces;
        settingsToSet.shownDataNotice = shownDataNotice;
        try {
            LogUtils.debug("Writing to settings!" + path.getParent().toString());
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
    int minimapSizePercent = 0;
    double minimapScale = 1;
    MinimapBackground minimapBackground = MinimapBackground.MENU_BG;
    MiniMapPositions miniMapPos = MiniMapPositions.TOP_RIGHT;
    boolean miniMapEnabled = true;
    String mapTileSource = NetworkUtils.BASE_URL + "/maps/";
    boolean useDarkTiles = false;
    BigMapPos bigMapPos = BigMapPos.CENTRE_ON_PLAYER;
    boolean checkForUpdates = true;
    boolean collectData = false;
    boolean t2TileUploads = false;
    String t2maptoken = "";
    boolean debugMode = false;
    boolean hideMinimapNetworks = false;
    OrwellianMeter orwellMeter = OrwellianMeter.SMART;
    boolean showWelcomePopup = true;
    boolean showLeavingPopup = true;
    boolean showInOtherPlaces = false;
    boolean showPlaces = true;
    boolean showAreas = true;
    boolean showNetworks = true;
    boolean showPlayers = true;
    boolean showClaims = false;
    boolean shownDataNotice = false;
}

enum MinimapBackground {
    MENU_BG,
    MAP_BG
};

enum MiniMapPositions {
    TOP_LEFT,
    TOP_RIGHT
};  

enum BigMapPos {
    CENTRE_ON_PLAYER,
    CENTRE_ON_SPAWN
};  

enum OrwellianMeter {
    ALL,
    SMART,
    FULL_MUTE,
    ANGY
}
