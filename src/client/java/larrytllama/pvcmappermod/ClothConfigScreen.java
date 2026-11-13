package larrytllama.pvcmappermod;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.gui.entries.EnumListEntry;
import me.shedaniel.clothconfig2.gui.entries.IntegerSliderEntry;
import me.shedaniel.clothconfig2.gui.entries.StringListEntry;
import me.shedaniel.clothconfig2.impl.builders.EnumSelectorBuilder;
import me.shedaniel.clothconfig2.impl.builders.IntSliderBuilder;
import java.net.URI;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;

public class ClothConfigScreen extends Screen {
    public SettingsProvider sp;

    public ClothConfigScreen(Component title) {
        super(title);
    }

    public static Screen createScreen(SettingsProvider sp, Screen parentScreen) {
        ClothConfigScreen instance = new ClothConfigScreen(Component.literal("PVC Mapper Mod Settings"));
        sp.updateSettings();
        instance.sp = sp;
        return instance.getClothConfig(parentScreen);
        //return instance;
    }

    private BooleanListEntry showMinimap;
    private BooleanListEntry checkForUpdates;
    private BooleanListEntry useDarkTiles;
    private BooleanListEntry collectData;
    private BooleanListEntry t2TileUploads;
    private StringListEntry t2maptoken;
    private BooleanListEntry debugMode;
    private BooleanListEntry showNetworkLines;
    private StringListEntry mapTileSource;
    private IntegerSliderEntry miniMapZoom;
    private IntegerSliderEntry minimapSize;
    private EnumListEntry<MiniMapPositions> miniMapPos;
    private EnumListEntry<MinimapBackground> minimapBackground;
    private EnumListEntry<BigMapPos> bigMapPos;
    private EnumListEntry<OrwellianMeter> orwellMeter;
    private BooleanListEntry showWelcomePopup;
    private BooleanListEntry showLeavingPopup;
    private BooleanListEntry showInOtherPlaces;
    private BooleanListEntry showPlaces;
    private BooleanListEntry showAreas;
    private BooleanListEntry showNetworks;
    private BooleanListEntry showPlayers;
    private BooleanListEntry showClaims;
    private BooleanListEntry showWelcomeScreen;

    public Screen getClothConfig(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
        .setParentScreen(parent)
        .setSavingRunnable(() -> {
            sp.miniMapEnabled = this.showMinimap.getValue();
            sp.miniMapPos = this.miniMapPos.getValue();
            sp.minimapBackground = this.minimapBackground.getValue();
            sp.mapTileSource = this.mapTileSource.getValue();
            sp.miniMapZoom = this.miniMapZoom.getValue();
            sp.useDarkTiles = this.useDarkTiles.getValue();
            sp.bigMapPos = this.bigMapPos.getValue();
            sp.checkForUpdates = this.checkForUpdates.getValue();
            int step = this.minimapSize.getValue();
            sp.minimapSizePercent = Minimap.stepToPercent(step);
            sp.minimapScale = sp.minimapSizePercent == 0 ? 1.0 : sp.minimapSizePercent / 100.0;
            sp.collectData = this.collectData.getValue();
            sp.t2TileUploads = this.t2TileUploads.getValue();
            sp.t2maptoken = this.t2maptoken.getValue();
            sp.debugMode = this.debugMode.getValue();
            sp.hideMinimapNetworks = !this.showNetworkLines.getValue();
            sp.orwellMeter = this.orwellMeter.getValue();
            sp.showWelcomePopup = this.showWelcomePopup.getValue();
            sp.showLeavingPopup = this.showLeavingPopup.getValue();
            sp.showInOtherPlaces = this.showInOtherPlaces.getValue();

            sp.showPlaces = this.showPlaces.getValue();
            sp.showAreas = this.showAreas.getValue();
            sp.showNetworks = this.showNetworks.getValue();
            sp.showPlayers = this.showPlayers.getValue();
            if (this.showClaims != null) {
                sp.showClaims = this.showClaims.getValue();
            }
            if (this.showWelcomeScreen != null) {
                sp.shownDataNotice = !this.showWelcomeScreen.getValue();
            }

            sp.saveSettings();
        })
        .setTitle(Component.literal("PVC Mapper Mod Settings"));
        ConfigCategory minimapSettings = builder.getOrCreateCategory(Component.literal("Minimap"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        this.showMinimap = entryBuilder.startBooleanToggle(Component.literal("Show Minimap"), sp.miniMapEnabled)
            .setDefaultValue(true)
            .setTooltip(Component.literal("Hide the minimap from view."))
            .build();
        minimapSettings.addEntry(this.showMinimap);
        // Inverted for UI consistency while not touching settings file
        this.showNetworkLines = entryBuilder.startBooleanToggle(Component.literal("Show Network Lines"), !sp.hideMinimapNetworks)
            .setDefaultValue(true)
            .setTooltip(Component.literal("Show the network lines on the minimap view."))
            .build();
        minimapSettings.addEntry(this.showNetworkLines);
        this.miniMapPos = entryBuilder.startEnumSelector(Component.literal("Minimap Position"), MiniMapPositions.class, sp.miniMapPos)
            .setDefaultValue(MiniMapPositions.TOP_RIGHT) 
            .setEnumNameProvider(pos -> pos == MiniMapPositions.TOP_LEFT
                ? Component.literal("Top left")
                : Component.literal("Top right"))
            .setTooltip(Component.literal("Choose where the minimap goes!"))
            .build();
        minimapSettings.addEntry(this.miniMapPos);
        this.minimapBackground = entryBuilder.startEnumSelector(Component.literal("Minimap Background"), MinimapBackground.class, sp.minimapBackground)
            .setDefaultValue(MinimapBackground.MENU_BG)
            .setEnumNameProvider(bg -> bg == MinimapBackground.MAP_BG
                ? Component.literal("Parchment Map")
                : Component.literal("Darken Background"))
            .setTooltip(Component.literal("Choose between menu background (black translucent) or map parchment background."))
            .build();
        minimapSettings.addEntry(this.minimapBackground);
        this.miniMapZoom = entryBuilder.startIntSlider(Component.literal("Minimap Default Zoom"), sp.miniMapZoom, 1, 15)
            .setDefaultValue(8) 
            .setTooltip(Component.literal("Set the default zoom level of the minimap (when the server loads)."), Component.literal("8 is the most zoomed in (default)."), Component.literal("1 is the most zoomed out."))
            .build();
        minimapSettings.addEntry(this.miniMapZoom);
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int maxStep = Minimap.getMaxSliderStep(screenHeight);
        int currentStep = Minimap.percentToStep(sp.minimapSizePercent, maxStep);

        this.minimapSize = entryBuilder.startIntSlider(Component.literal("Minimap Size"), currentStep, 0, maxStep)
            .setDefaultValue(0)
            .setTextGetter(step -> Component.literal(Minimap.getSliderLabel(step, screenHeight)))
            .setTooltip(
                Component.literal("Set the viewport size of the minimap relative to 80 in-game pixels."),
                Component.literal("Auto-adjusts to your screen resolution and GUI scale."),
                Component.literal("Larger sizes reveal more of the surrounding map.")
            )
            .build();
        minimapSettings.addEntry(this.minimapSize);

        ConfigCategory alertSettings = builder.getOrCreateCategory(Component.literal("Alerts"));
        this.showWelcomePopup = entryBuilder.startBooleanToggle(Component.literal("Show Welcome Popup"),  sp.showWelcomePopup)
            .setDefaultValue(true)
            .setTooltip(
                Component.literal("Show a popup when you enter an area marked on the mapper")
            )
            .build();
        alertSettings.addEntry(this.showWelcomePopup);
        this.showLeavingPopup = entryBuilder.startBooleanToggle(Component.literal("Show Leaving Popup"),  sp.showLeavingPopup)
            .setDefaultValue(true)
            .setTooltip(
                Component.literal("Show a popup when you leave an area marked on the mapper")
            )
            .build();
        alertSettings.addEntry(this.showLeavingPopup);

        alertSettings.addEntry(
            entryBuilder.startTextDescription(Component.literal("You'll always be alerted when you disconnect from the Mapper server.")).build()
        );

        this.checkForUpdates = entryBuilder.startBooleanToggle(Component.literal("Check for Updates"), sp.checkForUpdates)
            .setDefaultValue(true)
            .setTooltip(Component.literal("Check for PVC Mapper Mod updates on Game Launch"), Component.literal("(Displays a non-intrusive popup in the top right to let you know!)"))
            .build();
        alertSettings.addEntry(this.checkForUpdates);
        
        ConfigCategory filterSettings = builder.getOrCreateCategory(Component.literal("Map Filters"));
        this.showPlaces = entryBuilder.startBooleanToggle(Component.literal("Show Places"), sp.showPlaces)
            .setDefaultValue(true)
            .setTooltip(Component.literal("Show places (banner icons) on minimap and map screen."))
            .build();
        filterSettings.addEntry(this.showPlaces);
        this.showAreas = entryBuilder.startBooleanToggle(Component.literal("Show Areas"), sp.showAreas)
            .setDefaultValue(true)
            .setTooltip(Component.literal("Show area labels on minimap and map screen."))
            .build();
        filterSettings.addEntry(this.showAreas);
        this.showNetworks = entryBuilder.startBooleanToggle(Component.literal("Show Networks"), sp.showNetworks)
            .setDefaultValue(true)
            .setTooltip(Component.literal("Show network lines and labels on minimap and map screen."))
            .build();
        filterSettings.addEntry(this.showNetworks);
        this.showPlayers = entryBuilder.startBooleanToggle(Component.literal("Show Players"), sp.showPlayers)
            .setDefaultValue(true)
            .setTooltip(Component.literal("Show player markers on minimap and map screen."))
            .build();
        filterSettings.addEntry(this.showPlayers);
        this.showClaims = entryBuilder.startBooleanToggle(Component.literal("Show Claims"), sp.showClaims)
            .setDefaultValue(false)
            .setTooltip(Component.literal("Show claim borders on the map screen."))
            .build();
        filterSettings.addEntry(this.showClaims);
        filterSettings.addEntry(
            entryBuilder.startTextDescription(Component.literal("You can also change these settings in the map menu by clicking the filters button.")).build()
        );

        ConfigCategory uploadSettings = builder.getOrCreateCategory(Component.literal("Data & Uploads"));
        this.collectData = entryBuilder.startBooleanToggle(Component.literal("Enable Mapper Uploads"), sp.collectData)
            .setDefaultValue(false)
            .setTooltip(
                Component.literal("Contribute to the PVC Mapper by uploading stuff as you play!"),
                Component.literal("(The mod simply uploads the entries provided by the tab list)")
            )
            .build();
        uploadSettings.addEntry(this.collectData);
        
        uploadSettings.addEntry(
            entryBuilder.startTextDescription(
                Component.literal("  ↳ What data is uploaded? ")
                .append(Component.literal("See Data Notice ↗").withStyle(s -> s
                    .withColor(ChatFormatting.AQUA)
                    .withUnderlined(true)
                    .withClickEvent(new ClickEvent.OpenUrl(URI.create("https://pvc.coolwebsite.uk/help/data-uploading")))
                ))
            ).build()
        );
        
        this.t2TileUploads = entryBuilder.startBooleanToggle(Component.literal("Enable Terra2 Tile Uploads"), sp.t2TileUploads)
            .setDefaultValue(false)
            .setTooltipSupplier(enabled -> {
                if (!enabled && this.t2maptoken != null && !this.t2maptoken.getValue().isBlank()) {
                    return Optional.of(new Component[]{
                        Component.literal("Uploads are currently paused (your token is saved).").withStyle(ChatFormatting.YELLOW),
                        Component.literal("(Potato PC players on a dodgy connection may wish to keep this off for performance)")
                    });
                }
                return Optional.of(new Component[]{
                    Component.literal("Upload Terra2 Tiles to the PVC Mapper as you move around!"),
                    Component.literal("(Potato PC players on a dodgy connection may wish to keep this off for performance)")
                });
            })
            .build();
        uploadSettings.addEntry(this.t2TileUploads);

        this.t2maptoken = entryBuilder.startStrField(Component.literal("  ↳ Your Mapper Token"), sp.t2maptoken)
            .setDefaultValue("")
            .setTooltip(Component.literal("So PVC Mapper knows it's you!"))
            .setErrorSupplier(val -> this.t2TileUploads.getValue() && val.isBlank()
                ? Optional.of(Component.literal("A valid token (see below) is needed first!"))
                : Optional.empty())
            .build();
        uploadSettings.addEntry(this.t2maptoken);
        
        uploadSettings.addEntry(
            entryBuilder.startTextDescription(
                Component.literal("    ↳ Get your token: Sign in to the Editor → click at the top right → 'Tokens' ")
                    .append(Component.literal("Open Editor ↗").withStyle(s -> s
                        .withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent.OpenUrl(URI.create("https://pvc.coolwebsite.uk/create/v2")))
                    ))
            ).build()
        );

        ConfigCategory miscSettings = builder.getOrCreateCategory(Component.literal("Miscellaneous"));
        this.orwellMeter = entryBuilder.startEnumSelector(Component.literal("Orwell Mute Mode"), OrwellianMeter.class, sp.orwellMeter)
            .setDefaultValue(OrwellianMeter.ALL)
            .setTooltip(
                Component.literal("Quiet OrwellBeta down in chat by different amounts:"), 
                Component.literal("ALL = Usual chat chaos by Orwell"),
                Component.literal("SMART = Shortens and grays-out unimportant messages. [Recommended]"),
                Component.literal("FULL_MUTE = Mute Orwell entirely! You may miss out on important notices"),
                Component.literal("ANGY = Oh boy, you've upset him now")
            )
            .build();
        miscSettings.addEntry(this.orwellMeter);
        this.mapTileSource = entryBuilder.startTextField(Component.literal("Tile Source"), sp.mapTileSource)
            .setDefaultValue(NetworkUtils.BASE_URL + "/maps/") 
            .setTooltip(Component.literal("Where the PVC Mapper Mod should get its background tiles from."), Component.literal("Important! Your URL must include the / at the end!").withStyle(ChatFormatting.RED), Component.literal("If the default isn't working, try: https://web.peacefulvanilla.club/maps/tiles/"))
            .build();
        miscSettings.addEntry(this.mapTileSource);
        this.useDarkTiles = entryBuilder.startBooleanToggle(Component.literal("Use Darker Tiles"), sp.useDarkTiles)
            .setDefaultValue(false)
            .setTooltip(Component.literal("Set the tileset to use the night (dark) mode instead of the default (day)."), Component.literal("Only applies to Terra2 (for now!)"))
            .build();
        miscSettings.addEntry(this.useDarkTiles);
        this.bigMapPos = entryBuilder.startEnumSelector(Component.literal("Big Map Position"), BigMapPos.class, sp.bigMapPos)
            .setDefaultValue(BigMapPos.CENTRE_ON_PLAYER) 
            .setEnumNameProvider(pos -> pos == BigMapPos.CENTRE_ON_PLAYER
                ? Component.literal("Open to you")
                : Component.literal("Open to spawn"))
            .setTooltip(Component.literal("Choose where the big map opens to!"), Component.literal("Note: The map will re-open to your last dragged location."))
            .build();
        miscSettings.addEntry(this.bigMapPos);
        
        this.showWelcomeScreen = entryBuilder.startBooleanToggle(Component.literal("Show Welcome Screen on Next Connect"), !sp.shownDataNotice)
        .setDefaultValue(false)
        .setTooltip(Component.literal("Re-display the welcome notice and keybind reminders next time you connect to PVC."))
            .build();
        miscSettings.addEntry(this.showWelcomeScreen);
        
        this.showInOtherPlaces = entryBuilder.startBooleanToggle(Component.literal("Show mod features elsewhere"), sp.showInOtherPlaces)
        .setDefaultValue(false)
        .setTooltip(Component.literal("Enables or disables all mapper mod features in servers or worlds other than PVC."), Component.literal("NOTE: You will need to rejoin PVC or install Mod Menu to change settings from another server with this set to False."))
        .build();
        miscSettings.addEntry(this.showInOtherPlaces);
        
        this.debugMode = entryBuilder.startBooleanToggle(Component.literal("Debug Mode"), sp.debugMode)
        .setDefaultValue(false)
        .setTooltip(Component.literal("Adds extra logging in the console."))
        .build();
        miscSettings.addEntry(this.debugMode);
        
        miscSettings.addEntry(
            entryBuilder.startTextDescription(Component.literal("To change keybinds, head to Options > Controls > Keybinds and scroll down!")).build()
        );
        
        return builder.build();
    }
}
