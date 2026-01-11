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
    private BooleanListEntry useDarkTiles;
    private StringListEntry mapTileSource;
    private IntegerSliderEntry miniMapZoom;
    private EnumListEntry<MiniMapPositions> miniMapPos;

    public Screen getClothConfig(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
        //.setParentScreen(this)
        .setSavingRunnable(() -> {
            sp.miniMapEnabled = this.showMinimap.getValue();
            sp.miniMapPos = this.miniMapPos.getValue();
            sp.mapTileSource = this.mapTileSource.getValue();
            sp.miniMapZoom = this.miniMapZoom.getValue();
            sp.useDarkTiles = this.useDarkTiles.getValue();
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
            .setTooltip(Component.literal("Set the default zoom level of the minimap (when the server loads). 1-8 (8 is the most zoomed in)"))
            .build();
        minimapSettings.addEntry(this.miniMapZoom);

        ConfigCategory miscSettings = builder.getOrCreateCategory(Component.literal("Miscellaneous Settings"));
        this.mapTileSource = entryBuilder.startTextField(Component.literal("Tile Source"), sp.mapTileSource)
            .setDefaultValue("https://pvc.coolwebsite.uk/maps/") 
            .setTooltip(Component.literal("Where the PVC Mapper Mod should get its background tiles from."), Component.literal("If the default isn't working, try: 'https://web.peacefulvanilla.club/maps/tiles/'"))
            .build();
        miscSettings.addEntry(this.mapTileSource);
        this.useDarkTiles = entryBuilder.startBooleanToggle(Component.literal("Use Darker Tiles"), sp.useDarkTiles)
            .setDefaultValue(false)
            .setTooltip(Component.literal("Only applies to Terra2 tiles: Set the tileset to use the night (dark) mode instead of the default."))
            .build();
        miscSettings.addEntry(this.useDarkTiles);
        miscSettings.addEntry(
            entryBuilder.startTextDescription(Component.literal("To change keybinds, head to Options > Controls > Keybinds and scroll down!")).build()
        );

        return builder.build();
    }
}
