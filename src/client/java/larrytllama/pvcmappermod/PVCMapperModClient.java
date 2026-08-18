package larrytllama.pvcmappermod;

import larrytllama.pvcmappermod.utils.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import larrytllama.pvcmappermod.mixin.client.TabListMixin;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
//? if <26.1 {
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
//?} else {
/*import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;*///?}
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.KeyMapping.Category;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

public class PVCMapperModClient implements ClientModInitializer {
    public Category MOD_CATEGORY = Category.register(ResIdentifier.of("pvcmappermod", "category").get());
    public KeyMapping OPEN_MAP = new KeyMapping("pvcmappermod.open_map", GLFW.GLFW_KEY_M, MOD_CATEGORY);
    public KeyMapping OPEN_SHOPS = new KeyMapping("pvcmappermod.open_shops", GLFW.GLFW_KEY_COMMA, MOD_CATEGORY);
    public KeyMapping MINIMAP_ZOOM_IN = new KeyMapping("pvcmappermod.minimap_zoom_in", GLFW.GLFW_KEY_EQUAL,
            MOD_CATEGORY);
    public KeyMapping MINIMAP_ZOOM_OUT = new KeyMapping("pvcmappermod.minimap_zoom_out", GLFW.GLFW_KEY_MINUS,
            MOD_CATEGORY);

    public FullScreenMap fsm;
    public ShopsScreen shopsScreen = new ShopsScreen(Component.literal("PVC Mapper - Shops View"));
    public Minimap minimap;
    // Brigadier commands execute synchronously inside ChatScreen's text field handler.
    // Setting screen synchronously gets instantly closed/overwritten by the chat screen closing,
    // so we queue the screen/task to open on the very next client tick instead.
    private static net.minecraft.client.gui.screens.Screen NEXT_SCREEN = null;
    private static Runnable NEXT_TICK_TASK = null;

    public static void setScreenOnNextTick(net.minecraft.client.gui.screens.Screen screen) {
        NEXT_SCREEN = screen;
    }
    public static void setScreenOnNextTick(net.minecraft.client.gui.screens.Screen screen, Runnable callback) {
        NEXT_SCREEN = screen;
        NEXT_TICK_TASK = callback;
    }

    public boolean isInPVC = false;

    private static boolean seenMainMenu = false;

    private float inLevelTicks = 0;
    @Override
    public void onInitializeClient() {
        // Settings provider
        SettingsProvider sp = SettingsProvider.getInstance();
        sp.updateSettings();
        
        // Set up player fetchererer
        PlayerFetchUtils pfu = new PlayerFetchUtils();
        pfu.fetchOrwellMuteCases().thenAccept((omc) -> {
            pfu.omc = omc;
            System.out.println("Fetch orwell mute cases. Let the chaos begin!");
        });
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

        HttpClient http = HttpClient.newHttpClient();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            if(sp.collectData) {
                CompletableFuture.runAsync(() -> {
                    if(this.minimap.isInQueue || this.minimap.isInTerra2 || this.minimap.isLoadingIn || !this.isInPVC) return; // I love when borrowing old code just works <3
                    String jsonString = "[";
                    ClientPacketListener connection = Minecraft.getInstance().getConnection();
                    if(connection == null) return;
                    Collection<PlayerInfo> players = connection.getOnlinePlayers();
                    for (PlayerInfo player : players) {
                        // Get the player's username
                        String name = player.getProfile().name();
                        String tabListName = player.getTabListDisplayName().toString();
                        jsonString = jsonString + String.format("{\"n\": \"%s\", \"tln\": \"%s\"},", name, tabListName);
                    }
                    
                    // Trim trailing comma otherwise JSON formatting will have a fit
                    // (And add array enderer too)
                    jsonString = jsonString.substring(0, jsonString.length() - 1) + "]";

                    // Hey you! Yes you, the one trying to scan the source code to see if you can change your rank on the mapper.
                    // It's actually pretty impossible to hide this stuff. I researched, but it's pretty impossible.
                    // So you *could* just change your ranks, but if you do:
                    //   A) You'll annoy the heck out of me
                    //   B) Your entries will probably (hopefully) be updated by another client 5s later
                    //   C) You're mean :(
                    // So, eh, please don't. Thanks <3
                    HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(NetworkUtils.API_V2 + "/rank-upload/setPlayerRanks?me=" + Minecraft.getInstance().player.getGameProfile().name()))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonString))
                        .header("Content-Type", "application/json")
                        .build();
                    try {
                        http.send(req, HttpResponse.BodyHandlers.ofString());
                    } catch(Exception e) {
                        // No bother, we'll just log to console and ignore!
                        // Who actually cares about HTTP error codes? Nothing wrong with ignoring them! *foreshadowing*
                        LogUtils.error("[PVC Mapper Mod] Oh naur! Uploading data to mapper failed. Here's the error:", e);
                    }
                });
            }
        }, 0, 60000, TimeUnit.MILLISECONDS );

        OPEN_MAP = CompatUtils.registerKey(OPEN_MAP);
        OPEN_SHOPS = CompatUtils.registerKey(OPEN_SHOPS);
        MINIMAP_ZOOM_IN = CompatUtils.registerKey(MINIMAP_ZOOM_IN);
        MINIMAP_ZOOM_OUT = CompatUtils.registerKey(MINIMAP_ZOOM_OUT);
        fsm = FullScreenMap.createScreen(Component.literal("PVC Mapper - Map View"), pfu, sp);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_MAP.consumeClick()) {
                // Alt+M (or Alt+Full-screen-map-key) to hide minimap
                if(InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), GLFW.GLFW_KEY_LEFT_ALT) || InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), GLFW.GLFW_KEY_RIGHT_ALT)) {
                    if(sp.miniMapEnabled) sp.miniMapEnabled = false;
                    else sp.miniMapEnabled = true;
                    sp.saveSettings();
                } else { 
                    CompatUtils.setScreen(fsm);
                }
            }

            while (OPEN_SHOPS.consumeClick()) {
                CompatUtils.setScreen(new ShopsScreen(Component.literal("PVC Mapper - Shops View")));
            }

            while (MINIMAP_ZOOM_IN.consumeClick()) {
                if (this.minimap.zoomlevel != 15) {
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

            // Open queued screen or run queued task
            if (NEXT_SCREEN != null) {
                CompatUtils.setScreen(client, NEXT_SCREEN);
                NEXT_SCREEN = null;
                if (NEXT_TICK_TASK != null) {
                    NEXT_TICK_TASK.run();
                    NEXT_TICK_TASK = null;
                }
            }
        });


        ClientTickEvents.END_CLIENT_TICK.register((client) -> {
            if (client.level == null) return;
            inLevelTicks++;
            if(inLevelTicks == 40) {
                inLevelTicks = 0;
                // Janky AF Terra2 detector with the tab list (of all things)
                PlayerTabOverlay tabList = Minecraft.getInstance().gui.
                    //? if >=26.2 {
                    /*hud.*/
                    //?}
                    getTabList();
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
                    isInPVC = true;
                } else {
                    // Just assume they want mondo, I don't care lol
                    minimap.isInQueue = false;
                    minimap.isInTerra2 = false;
                    isInPVC = false; // Prevent sending ranks when not in server
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

        MutableComponent[] orwellMessagePrefixes = {
            Component.empty().withStyle(Style.EMPTY)
                .append(Component.literal("|").withStyle(Style.EMPTY.withBold(true).withColor(ChatFormatting.GRAY)))
                .append(Component.literal(" «Bot» ").withStyle(Style.EMPTY.withBold(false).withColor(ChatFormatting.GOLD)))
                .append(Component.literal("OrwellBeta").withStyle(Style.EMPTY))
                .append(Component.literal(" › ").withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY))),
            Component.empty().withStyle(Style.EMPTY)
                .append(Component.literal(" «Bot» ").withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)))
                .append(Component.literal("OrwellBeta").withStyle(Style.EMPTY))
                .append(Component.literal(" › ").withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY))),
            Component.empty().withStyle(Style.EMPTY)
                .append(Component.literal("[").withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)))
                .append(Component.literal("OrwellBeta").withStyle(Style.EMPTY.withColor(ChatFormatting.RED)))
                .append(Component.literal(" -> ").withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)))
                .append(Component.literal("me").withStyle(Style.EMPTY.withColor(ChatFormatting.RED)))
                .append(Component.literal("] ").withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)))
        };

        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (Minecraft.getInstance().player == null) return true;
            String text = message.getString();
            if(text.trim().length() == 0) return false;
            if(sp.orwellMeter == OrwellianMeter.FULL_MUTE || sp.orwellMeter == OrwellianMeter.SMART) {
                for (int i = 0; i < orwellMessagePrefixes.length; i++) {
                    if(text.startsWith(orwellMessagePrefixes[i].getString())) {
                        if( sp.orwellMeter == OrwellianMeter.FULL_MUTE ||
                           (
                            sp.orwellMeter == OrwellianMeter.SMART &&
                            (text.contains("@") && !text.contains(Minecraft.getInstance().player.getPlainTextName()))
                           )
                         ) {
                            return false;
                           }
                    }
                }
            }
            return true;
        });

        // Get rid of Orwell to make him angy
        ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> {
            if(sp.orwellMeter == OrwellianMeter.ALL) return message;
            if (Minecraft.getInstance().player == null) return message;
            // Orwell message types:
            
            String text = message.getString();

            int applicablePrefix = -1;
            for (int i = 0; i < orwellMessagePrefixes.length; i++) {
                if(text.startsWith(orwellMessagePrefixes[i].getString())) {
                    applicablePrefix = i;
                    break;
                }
            }
            if(applicablePrefix != -1) {
                // If Orwell is mentioning someone, but not us: ignore
                if(text.contains("@")) {
                    if(text.contains(Minecraft.getInstance().player.getPlainTextName())) {
                        return message;
                    }
                }
                if(pfu.omc == null) return message;
                for (int i = 0; i < pfu.omc.length; i++) {
                    if(pfu.omc[i] == null || pfu.omc[i].includes == null) continue;
                    if(text.contains(pfu.omc[i].includes)) {
                        String otherplayer = "player";
                        for (String word : text.split("\\s+")) {
                            if(word.startsWith("@") && word.length() > 1) {
                                otherplayer = word.substring(1);
                                break;
                            }
                        }
                        if(sp.orwellMeter == OrwellianMeter.SMART) {
                            if(pfu.omc[i].replacewith == null) continue;
                            String outputtext = pfu.omc[i].replacewith
                                .replaceAll("%player%", Minecraft.getInstance().player.getPlainTextName())
                                .replaceAll("%otherplayer%", otherplayer);
                            if(pfu.omc[i].important) {
                                return orwellMessagePrefixes[applicablePrefix].append(Component.literal(outputtext).withStyle(Style.EMPTY)).withStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Component.literal("Original message content:\n").append(message))));
                            } else {
                                return Component.literal(outputtext).withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withItalic(true).withHoverEvent(new HoverEvent.ShowText(Component.literal("Original message content:\n").append(message))));
                            }
                        } else if(sp.orwellMeter == OrwellianMeter.ANGY) {
                            if(pfu.omc[i].angyreplace == null) continue;
                            String outputtext = pfu.omc[i].angyreplace
                                .replaceAll("%player%", Minecraft.getInstance().player.getPlainTextName())
                                .replaceAll("%otherplayer%", otherplayer);
                            return orwellMessagePrefixes[applicablePrefix].append(Component.literal(outputtext).withStyle(Style.EMPTY)).withStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Component.literal("Original message content:\n").append(message))));
                        }
                    }
                }
                return message;
            }

            return message;
        });
    }
}
