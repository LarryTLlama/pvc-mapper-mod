package larrytllama.pvcmappermod;

import java.util.concurrent.CompletableFuture;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.KeyMapping.Category;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class PVCMapperModClient implements ClientModInitializer {
    public Category MOD_CATEGORY = Category.register(ResourceLocation.fromNamespaceAndPath("pvcmappermod", "category"));
    public KeyMapping OPEN_MAP = new KeyMapping("pvcmappermod.open_map", GLFW.GLFW_KEY_M, MOD_CATEGORY);
    public KeyMapping MINIMAP_ZOOM_IN = new KeyMapping("pvcmappermod.minimap_zoom_in", GLFW.GLFW_KEY_EQUAL,
            MOD_CATEGORY);
    public KeyMapping MINIMAP_ZOOM_OUT = new KeyMapping("pvcmappermod.minimap_zoom_out", GLFW.GLFW_KEY_MINUS,
            MOD_CATEGORY);

    public FullScreenMap fsm;
    public Minimap minimap;

    private static boolean seenMainMenu = false;

    @Override
    public void onInitializeClient() {
        // Settings provider
        SettingsProvider sp = SettingsProvider.getInstance();
        // Set up player fetchererer
        PlayerFetchUtils pfu = new PlayerFetchUtils();
        new MapperCmdHandler(pfu, this);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            pfu.startUpdates();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            pfu.stopUpdates();
        });

        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!seenMainMenu && screen instanceof Screen) {
                seenMainMenu = true;
                // Check for updates
                if(sp.checkForUpdates) {
                    CompletableFuture.runAsync(() -> {
                        pfu.checkForUpdates();
                    });
                }
            }
        });

        this.minimap = Minimap.attach(pfu, sp);
        OPEN_MAP = KeyBindingHelper.registerKeyBinding(OPEN_MAP);
        MINIMAP_ZOOM_IN = KeyBindingHelper.registerKeyBinding(MINIMAP_ZOOM_IN);
        MINIMAP_ZOOM_OUT = KeyBindingHelper.registerKeyBinding(MINIMAP_ZOOM_OUT);
        fsm = FullScreenMap.createScreen(Component.literal("PVC Mapper - Map View"), pfu, sp);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_MAP.consumeClick()) {
                this.fsm.resetTiles();
                Minecraft.getInstance().setScreen(fsm);
            }

            while (MINIMAP_ZOOM_IN.consumeClick()) {
                if (this.minimap.zoomlevel != 8) {
                    this.minimap.zoomlevel += 1;
                    this.minimap.resetTileImageCache();
                }
            }

            while (MINIMAP_ZOOM_OUT.consumeClick()) {
                if (this.minimap.zoomlevel != 1) {
                    this.minimap.zoomlevel -= 1;
                    this.minimap.resetTileImageCache();
                }
            }
        });
    }
}