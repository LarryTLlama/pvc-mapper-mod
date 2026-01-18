package larrytllama.pvcmappermod;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.gui.entries.EnumListEntry;
import me.shedaniel.clothconfig2.gui.entries.IntegerSliderEntry;
import me.shedaniel.clothconfig2.gui.entries.LongSliderEntry;
import me.shedaniel.clothconfig2.gui.entries.StringListEntry;
import me.shedaniel.clothconfig2.impl.builders.EnumSelectorBuilder;
import me.shedaniel.clothconfig2.impl.builders.IntSliderBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
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
    private StringListEntry mapTileSource;
    private IntegerSliderEntry miniMapZoom;
    private LongSliderEntry minimapScale;
    private EnumListEntry<MiniMapPositions> miniMapPos;
    private EnumListEntry<BigMapPos> bigMapPos;

    public Screen getClothConfig(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
        //.setParentScreen(this)
        .setSavingRunnable(() -> {
            sp.miniMapEnabled = this.showMinimap.getValue();
            sp.miniMapPos = this.miniMapPos.getValue();
            sp.mapTileSource = this.mapTileSource.getValue();
            sp.miniMapZoom = this.miniMapZoom.getValue();
            sp.useDarkTiles = this.useDarkTiles.getValue();
            sp.bigMapPos = this.bigMapPos.getValue();
            sp.checkForUpdates = this.checkForUpdates.getValue();
            sp.minimapScale = this.minimapScale.getValue() / 100.0;
            sp.saveSettings();
        })
        .setTitle(Component.literal("PVC Mapper Mod Settings"));
        ConfigCategory minimapSettings = builder.getOrCreateCategory(Component.literal("Minimap Settings"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        this.showMinimap = entryBuilder.startBooleanToggle(Component.literal("Show Minimap"), sp.miniMapEnabled)
            .setDefaultValue(true)
            .setTooltip(Component.literal("Hide the minimap from view."))
            .build();
        minimapSettings.addEntry(this.showMinimap);
        this.miniMapPos = entryBuilder.startEnumSelector(Component.literal("Minimap Position"), MiniMapPositions.class, sp.miniMapPos)
            .setDefaultValue(MiniMapPositions.TOP_RIGHT) 
            .setTooltip(Component.literal("Choose where the minimap goes!"))
            .build();
        minimapSettings.addEntry(this.miniMapPos);
        this.miniMapZoom = entryBuilder.startIntSlider(Component.literal("Minimap Default Zoom"), sp.miniMapZoom, 1, 8)
            .setDefaultValue(8) 
            .setTooltip(Component.literal("Set the default zoom level of the minimap (when the server loads)."), Component.literal("8 is the most zoomed in (default)."), Component.literal("1 is the most zoomed out."))
            .build();
        minimapSettings.addEntry(this.miniMapZoom);
        this.minimapScale = entryBuilder.startLongSlider(Component.literal("Minimap Scale"), (long) (sp.minimapScale * 100), (long) (1), (long) (200))
            .setDefaultValue(100) 
            .setTooltip(Component.literal("Set the size of the minimap"), Component.literal("1% (the min) almost hides the map."), Component.literal("200% (the max) is 2x the original scale."), Component.literal("Note: Minimap mouse hovering is disabled when this isn't set to 100"))
            .build();
        minimapSettings.addEntry(this.minimapScale);

        ConfigCategory miscSettings = builder.getOrCreateCategory(Component.literal("Miscellaneous Settings"));
        this.mapTileSource = entryBuilder.startTextField(Component.literal("Tile Source"), sp.mapTileSource)
            .setDefaultValue("https://pvc.coolwebsite.uk/maps/") 
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
            .setTooltip(Component.literal("Choose where the big map opens to!"), Component.literal("CENTRE_ON_PLAYER = Open to you"), Component.literal("CENTRE_ON_SPAWN = Open to spawn"), Component.literal("Note: The map will re-open to your last dragged location."))
            .build();
        miscSettings.addEntry(this.bigMapPos);
        this.checkForUpdates = entryBuilder.startBooleanToggle(Component.literal("Check for Updates"), sp.checkForUpdates)
            .setDefaultValue(true)
            .setTooltip(Component.literal("Check for PVC Mapper Mod updates on Game Launch"), Component.literal("(Displays a non-intrusive popup in the top right to let you know!)"))
            .build();
        miscSettings.addEntry(this.checkForUpdates);
        miscSettings.addEntry(
            entryBuilder.startTextDescription(Component.literal("To change keybinds, head to Options > Controls > Keybinds and scroll down!")).build()
        );

        return builder.build();
    }
}
