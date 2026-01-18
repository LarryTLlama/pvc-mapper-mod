package larrytllama.pvcmappermod;

import java.util.concurrent.CompletableFuture;

import org.lwjgl.glfw.GLFW;

import larrytllama.pvcmappermod.mixin.client.TabListMixin;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerTabOverlay;
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

    private float inLevelTicks = 0;
    @Override
    public void onInitializeClient() {
        // Settings provider
        SettingsProvider sp = SettingsProvider.getInstance();
        // Set up player fetchererer
        PlayerFetchUtils pfu = new PlayerFetchUtils();
        new MapperCmdHandler(pfu, this);
        

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
                //this.fsm.resetTiles();
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


        ClientTickEvents.END_CLIENT_TICK.register((client) -> {
            if (client.level == null) return;
            inLevelTicks++;
            if(inLevelTicks == 40) {
                inLevelTicks = 0;
                // Janky AF Terra2 detector with the tab list (of all things)
                PlayerTabOverlay tabList = Minecraft.getInstance().gui.getTabList();
                // Check against IP if in server
                if( Minecraft.getInstance().getConnection().getServerData() != null && 
                    Minecraft.getInstance().getConnection().getServerData().ip.contains("peacefulvanilla.club")) { // Just in case they change the subdomain
                    // If null, we're in Terra2
                    if(((TabListMixin) tabList).getHeader() == null || ((TabListMixin) tabList).getFooter() == null) {
                        minimap.isInTerra2 = true;
                        minimap.isInQueue = false;
                    // Missing footer text, in Queue
                    } else if(!((TabListMixin) tabList).getFooter().getString().contains("Visit the website for more info")) {
                        minimap.isInQueue = true;
                        minimap.isInTerra2 = false;
                    // None of the above, in Mondo
                    } else {
                        minimap.isInQueue = false;
                        minimap.isInTerra2 = false;
                    }
                } else {
                    // Just assume they want mondo, I don't care lol
                    minimap.isInQueue = false;
                    minimap.isInTerra2 = false;
                }
                minimap.isLoadingIn = false;
            }
        });
        ClientPlayConnectionEvents.JOIN.register((a, b, c) -> {
            minimap.isInQueue = false;
            minimap.isInTerra2 = false;
            minimap.isLoadingIn = true;
            pfu.startUpdates();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            pfu.stopUpdates();
        });
    }
}