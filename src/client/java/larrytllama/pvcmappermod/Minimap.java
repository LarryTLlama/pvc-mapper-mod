package larrytllama.pvcmappermod;

import larrytllama.pvcmappermod.utils.*;

import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.mojang.blaze3d.systems.RenderSystem;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//? if <26.1 {
import net.minecraft.client.gui.GuiGraphics;
//?} else {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;*///?}
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.component.ResolvableProfile;

/**
 * Definitions for players api
 */
class PlayerFetch {
    String world;
    Float armor;
    String name;
    int x;
    int y;
    int z;
    Float health;
    String uuid;
    Float yaw;
    Float pitch;
    Float roll;
    String afksince;
    String nickname;
    boolean isAFK;
    String[] ranks;
}

class PlaceFetch {
    int id;
    String name;
    String x; // Why tf did I make these strings
    String z; // Like, it's a coord - What was I thinking?
    String type;
    String dimension;
}

public class Minimap {

    public PlayerFetchUtils pfu;
    public SettingsProvider sp;
    public DirectionsProvider dp;
    public Network[] allNetworks;
    public static Minimap attach(PlayerFetchUtils pfu, SettingsProvider sp, DirectionsProvider dp) {
        Minimap minimap = new Minimap();
        minimap.pfu = pfu;
        minimap.dp = dp;
        pfu.fetchNetworksAsync().thenAccept(networks -> {
            minimap.allNetworks = networks;
        });
        minimap.sp = sp;
        //? if <26.1 {
        HudElementRegistry.attachElementBefore(VanillaHudElements.STATUS_EFFECTS,
//?} else {
/*        HudElementRegistry.attachElementBefore(VanillaHudElements.MOB_EFFECTS,*///?}
                ResIdentifier.of("larrytllama.pvcmappermod", "before_chat").get(), minimap::render);
        return minimap;
    }

    private boolean hasNotBeenInitialisedYet = true;


    public ArrayList<PlaceFetch> placesList = new ArrayList<PlaceFetch>();

    private void sendPlayerListFeedback() {
        
        // Calculate tile size from zoom
        int tilesize = 1 << (17 - zoomlevel);
        MutableComponent message = Component.literal("PVC Mapper Minimap\n").withStyle(ChatFormatting.GREEN,
                ChatFormatting.ITALIC);
        Minecraft instance = Minecraft.getInstance();
        // cast do floor calc when casting to int for discrete boundaries & clean sharing
        int minX = (int) Math.floor(instance.player.getX()) - (tilesize / 2);
        int minZ = (int) Math.floor(instance.player.getZ()) - (tilesize / 2);
        int maxX = (int) Math.floor(instance.player.getX()) + (tilesize / 2);
        int maxZ = (int) Math.floor(instance.player.getZ()) + (tilesize / 2);
        Instant now = Instant.now();
        ArrayList<PlayerFetch> playersList = pfu.getPlayers();
        for (int i = 0; i < playersList.size(); i++) {
            PlayerFetch player = playersList.get(i);
            if ((player.x > minX && player.x < maxX) && (player.z > minZ && player.z < maxZ)) {
                Instant then = Instant.parse(player.afksince);
                Duration d = Duration.between(then, now);
                message.append(
                        // 1. LarryTLlama - 1234, -5678
                        // AFK Streak: 1d 2h 3m 4s
                        Component.literal(
                                (i + 1) + ". " + player.name + " - " + player.x + ", " + player.z + "\n" +
                                        "   AFK Streak: "
                                        + (d.toMinutes() < 2 ? "Not AFK"
                                                : ((d.toDaysPart() > 0 ? d.toDaysPart() + "d " : "") +
                                                        (d.toHoursPart() > 0 ? d.toHoursPart() + "h " : "") +
                                                        (d.toMinutesPart() > 0 ? d.toMinutesPart() + "m " : "") +
                                                        (d.toSecondsPart() > 0 ? d.toSecondsPart() + "s " : ""))))
                                .setStyle(
                                        Style.EMPTY.withColor(ChatFormatting.YELLOW)
                                                .withHoverEvent(new HoverEvent.ShowText(Component
                                                        .literal("Highlighted on minimap!\nUuid: " + player.uuid)))));
            }
        }
    }

    private void sendPlaceListFeedback() {

    }

    public void registerCommand() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    Commands.literal("minimap")
                        .then(Commands.literal("players")
                                .executes(context -> {
                                    sendPlayerListFeedback();
                                    return 1;
                                }))
                        .then(Commands.literal("places")
                                .executes(context -> {
                                    sendPlaceListFeedback();
                                    return 1;
                                })));
        });
    }

    // Zoom level for map
    public static final int BASE_TILE_SIZE = 128;
    public int zoomlevel = 8;
    public int minimapTileSize = 80;
    public int topLeftX = 8;
    public int lastTileX = Integer.MIN_VALUE;
    public int lastTileZ = Integer.MIN_VALUE;
    
    public static final ResIdentifier PLAYER = ResIdentifier.of("minecraft",
            "textures/map/decorations/player.png");
    public static final ResIdentifier OTHER_PLAYERS_OW = ResIdentifier.of("minecraft",
            "textures/map/decorations/frame.png");
    public static final ResIdentifier OTHER_PLAYERS_NETHER = ResIdentifier.of("minecraft",
            "textures/map/decorations/red_marker.png");
    public static final ResIdentifier OTHER_PLAYERS_SOMEWHERE = ResIdentifier.of("minecraft",
            "textures/map/decorations/blue_marker.png");

    public static final ResIdentifier SHOP_BANNER = ResIdentifier.of("minecraft",
            "textures/map/decorations/orange_banner.png");
    public static final ResIdentifier EVENT_BANNER = ResIdentifier.of("minecraft",
            "textures/map/decorations/magenta_banner.png");
    public static final ResIdentifier LANDMARK_BANNER = ResIdentifier.of("minecraft",
            "textures/map/decorations/yellow_banner.png");
    public static final ResIdentifier BASE_BANNER = ResIdentifier.of("minecraft",
            "textures/map/decorations/light_blue_banner.png");
    public static final ResIdentifier GRAY_BANNER = ResIdentifier.of("minecraft",
            "textures/map/decorations/light_gray_banner.png");

    public static final ResIdentifier MENU_BG = ResIdentifier.of("minecraft",
            "textures/gui/menu_background.png");
    public static final ResIdentifier MAP_BG = ResIdentifier.of("minecraft",
            "textures/map/map_background_checkerboard.png");

    public static final ResIdentifier LOCATOR_DOT = ResIdentifier.of("minecraft",
            "textures/gui/sprites/hud/locator_bar_dot/default_1.png");

    public String prettyDimensionName(String dimension) {
        switch (dimension) {
            case "minecraft_overworld":
                return "Overworld";
            case "minecraft_the_nether":
                return "Nether";
            case "minecraft_the_end":
                return "End";
            case "minecraft_terra2":
                return "Terra2";
            default:
                return "Unknown World";
        }
    }

    public String getDimensionNID() {
        String dimension = "minecraft_" + CompatUtils.getIdentifier(Minecraft.getInstance().level.dimension()).getPath();
        if(isInTerra2 && dimension.equals("minecraft_overworld")) return "minecraft_terra2";
        else if(isInTerra2) return "minecraft_unknown_world";
        else if(isInQueue) return "minecraft_unknown_world";
        return dimension;
    }

    private void renderMinimapTooltip(/*? if <26.1 {*/GuiGraphics/*?} else {*//*GuiGraphicsExtractor*//*?}*/ context, @Nullable List<String> text) {
        List<String> tooltipText = text == null
                ? List.of("Move your mouse over an icon on\n the map to see more details!")
                : text;
        int lines = tooltipText.size();
        Font mcfont = Minecraft.getInstance().font;
        int maxSize = 0;
        for (int i = 0; i<lines; i++) {
            int w = mcfont.width(tooltipText.get(i));
            if(w > maxSize) {
                maxSize = w;
            }
        }
        int pad = getMinimapPadding();
        int tooltipX = -1000;
        if(sp.miniMapPos == MiniMapPositions.TOP_RIGHT) {
            tooltipX = topLeftX - pad - 5 - maxSize;
        } else if(sp.miniMapPos == MiniMapPositions.TOP_LEFT) {
            tooltipX = topLeftX + minimapTileSize + pad + 5;
        }

        if(dp != null && dp.routeActive) {
            MapRenderUtils.drawTooltipString(context, tooltipText, tooltipX, 24 + (mcfont.lineHeight * 2));
        } else {
            MapRenderUtils.drawTooltipString(context, tooltipText, tooltipX, 8);
        }
    }

    private ResIdentifier playerTooltipSkin = ResIdentifier.of("minecraft","textures/entity/player/wide/steve.png");;

    private void getTooltipPlayer(String uuid, String name) {
        PlayerSkinHelper.fetchSkin(uuid, "Minimap", (skin) -> {
            playerTooltipSkin = skin;
        });
    }

    private void renderMinimapTooltipPlayer(/*? if <26.1 {*/GuiGraphics/*?} else {*//*GuiGraphicsExtractor*//*?}*/ context, List<String> text, String uuid) {
        List<String> tooltipText = text;
        int lines = tooltipText.size();
        Font mcfont = Minecraft.getInstance().font;
        int maxSize = 0;
        for (int i = 0; i<lines; i++) {
            int w = mcfont.width(tooltipText.get(i));
            if(w > maxSize) {
                maxSize = w;
            }
        }
        int pad = getMinimapPadding();
        int tooltipX = -1000;
        if(sp.miniMapPos == MiniMapPositions.TOP_RIGHT) {
            tooltipX = topLeftX - pad - 5 - maxSize;
        } else if(sp.miniMapPos == MiniMapPositions.TOP_LEFT) {
            tooltipX = topLeftX + minimapTileSize + pad + 5;
        } else {
            LogUtils.debug("[%s] Whoops no minimap pos: %s", "Minimap", sp.miniMapPos);
        }
        MapRenderUtils.drawTooltipString(context, tooltipText, tooltipX, 8);

        context.blit(RenderPipelines.GUI_TEXTURED, playerTooltipSkin.get(), tooltipX + mcfont.width(text.get(0)) + 1, 8, 8, 8, 8, 8, 64, 64);
        context.blit(RenderPipelines.GUI_TEXTURED, playerTooltipSkin.get(), tooltipX + mcfont.width(text.get(0)) + 1, 8, 40, 8, 8, 8, 64, 64 );

        int hashed = UUID.fromString(uuid.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})", "$1-$2-$3-$4-$5")).hashCode();
        float[] hsv = Color.RGBtoHSB((hashed >> 16) & 0xFF, (hashed >> 8) & 0xFF, hashed & 0xFF, null);
        int rgbout = 0xFF000000 | (Color.HSBtoRGB(hsv[0], Math.max(0.4f, hsv[1]), 1.0f) & 0xFFFFFF);
        context.blit(RenderPipelines.GUI_TEXTURED, LOCATOR_DOT.get(), tooltipX + mcfont.width(text.get(0)) + 11, 8, 0, 0, 8, 8, 9, 9, rgbout);
    }

    private String lastDimension = "";

    public void resetTileImageCache() {
        // Reset image cache
        // Including this one which should never realistically be a value
        // (Unless PVC's still expanding the map 15000 years later)
        lastTileX = Integer.MIN_VALUE;
        lastTileZ = Integer.MIN_VALUE;
    }

    public boolean isInTerra2 = false;
    public boolean isInQueue = true;
    public boolean isLoadingIn = true;
    public String[] spinnerParts = {" |", "/", "-", "\\", " |", "/", "-", "\\"};
    private int spinnerPart = 0;

    private final TransportNetwork transportNetwork = new TransportNetwork();

    // Base minimap size is 80px (100%). Every 25% step is 20px.
    // Step 0 is Auto (~22% screen height in 20px steps, clamped 60-160px).
    // Steps 1+ map to (step + 1) * 25% (step 1 = 50%, step 3 = 100%, etc.).
    public static int getAutoMapBoxSize(int screenHeight) {
        int target = (int) Math.round(screenHeight * 0.22);
        int step = (int) Math.round(target / 20.0);
        return Math.max(60, Math.min(160, step * 20));
    }

    public static int getMaxSliderStep(int screenHeight) {
        // Cap slider at 45% screen height, with a minimum max of 7 steps (200% / 160px)
        return Math.max(7, (int) Math.floor((screenHeight * 0.45) / 20.0));
    }

    public static int percentToStep(int percent, int maxStep) {
        if (percent <= 0) return 0;
        int step = (percent / 25) - 1;
        return Math.max(1, Math.min(maxStep, step));
    }

    public static int stepToPercent(int step) {
        return step <= 0 ? 0 : (step + 1) * 25;
    }

    public static String getSliderLabel(int step, int screenHeight) {
        if (step == 0) {
            int autoSize = getAutoMapBoxSize(screenHeight);
            int autoPct = (int) Math.round((autoSize / 80.0) * 100.0);
            return "Auto (" + autoPct + "%)";
        }
        return ((step + 1) * 25) + "%";
    }

    public int getMapBoxSize() {
        if(sp.minimapSizePercent <= 0) {
            int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            return getAutoMapBoxSize(screenHeight);
        }
        return Math.max(40, (int) Math.round(80.0 * (sp.minimapSizePercent / 100.0)));
    }

    public int getMinimapPadding() {
        // Matches vanilla map background padding (7 units around a 128-unit map)
        return Math.max(2, (int) Math.round(minimapTileSize * (7.0 / 128.0)));
    }

    public void recalculateNetworks() {
        minimapTileSize = getMapBoxSize();
        int tilesize = 1 << (17 - zoomlevel);
        double scale = (double) BASE_TILE_SIZE / tilesize;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        double x = mc.player.getBlockX() - ((minimapTileSize / 2.0) / scale);
        double z = mc.player.getBlockZ() - ((minimapTileSize / 2.0) / scale);

        transportNetwork.recalculate(allNetworks, getDimensionNID(), zoomlevel, BASE_TILE_SIZE, x, z, minimapTileSize, minimapTileSize, "Minimap");
    }

    private float tickAccumulator = 0f; // Track ticks for stuff

    int lastX = 0;
    int lastZ = 0;

    public boolean playerInCoords(int x1, int z1, int x2, int z2, String dimension, int x, int z) {
        if(dimension.equals(this.getDimensionNID())) {
            return x1 < x && x2 > x && z1 < z && z2 > z;
        } else if(this.getDimensionNID().equals("minecraft_overworld")) {
            return x1 < x*8 && x2 > x*8 && z1 < z*8 && z2 > z*8;
        } else {
            return x1 < x/8 && x2 > x/8 && z1 < z/8 && z2 > z/8;
        }
    }

    public void render(/*? if <26.1 {*/GuiGraphics/*?} else {*//*GuiGraphicsExtractor*//*?}*/ context, DeltaTracker tickCounter) {
        if(!sp.showInOtherPlaces && !(Minecraft.getInstance().getConnection().getServerData() != null && Minecraft.getInstance().getConnection().getServerData().ip.contains("peacefulvanilla.club"))) {
            return;
        }
        tickAccumulator += tickCounter.getRealtimeDeltaTicks();
        if(!sp.miniMapEnabled) return;
        minimapTileSize = getMapBoxSize();
        int screenwidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        if(sp.miniMapPos == MiniMapPositions.TOP_LEFT) {
            topLeftX = 8;
        } else {
            topLeftX = screenwidth - minimapTileSize - 8;
        }
        int topLeftZ = 8;
        int pad = getMinimapPadding();

        int bgSize = minimapTileSize + (pad * 2);
        if (sp.minimapBackground == MinimapBackground.MAP_BG) {
            context.blit(RenderPipelines.GUI_TEXTURED, MAP_BG.get(), topLeftX - pad, topLeftZ - pad, 0, 0, bgSize,
                    bgSize, bgSize, bgSize);
        } else {
            context.blit(RenderPipelines.GUI_TEXTURED, MENU_BG.get(), topLeftX - pad, topLeftZ - pad, 0, 0, bgSize,
                    bgSize, 16, 16);
        }

        if(isLoadingIn) {
            Font font = Minecraft.getInstance().font;
            float textScale = minimapTileSize < 80 ? 0.75f : 1.0f;
            float centerX = topLeftX + (minimapTileSize / 2.0f);
            float centerY = topLeftZ + (minimapTileSize / 2.0f);
            context.pose().pushMatrix();
            context.pose().translate(centerX, centerY);
            context.pose().scale(textScale, textScale);
            GraphicsHelper.drawCenteredString(context, font, Component.literal("Loading map"), 0, (int) (-font.lineHeight * 1.2f), 0xFFFFFFFF);
            if(spinnerPart == 8) spinnerPart = 0;
            GraphicsHelper.drawCenteredString(context, font, Component.literal("Please Wait " + spinnerParts[spinnerPart]).withStyle(ChatFormatting.BOLD), 0, (int) (font.lineHeight * 0.4f), 0xFFFFFFFF);
            context.pose().popMatrix();
            if(tickAccumulator >= 10f) {
                spinnerPart++;
                tickAccumulator = 0;
            }
            return;
        }

        if(isInQueue) {
            Font font = Minecraft.getInstance().font;
            // don't let the text bust out of the tiny box if its < 80px
            float textScale = minimapTileSize < 80 ? 0.75f : 1.0f;
            float centerX = topLeftX + (minimapTileSize / 2.0f);
            float centerY = topLeftZ + (minimapTileSize / 2.0f);
            context.pose().pushMatrix();
            context.pose().translate(centerX, centerY);
            context.pose().scale(textScale, textScale);
            GraphicsHelper.drawCenteredString(context, font, Component.literal("You're in..."), 0, (int) (-font.lineHeight * 1.8f), 0xFFFFFFFF);
            GraphicsHelper.drawCenteredString(context, font, Component.literal("The Queue").withStyle(ChatFormatting.BOLD), 0, (int) (-font.lineHeight * 0.7f), 0xFFFFFFFF);
            if(spinnerPart == 8) spinnerPart = 0;
            GraphicsHelper.drawCenteredString(context, font, Component.literal("Please Wait " + spinnerParts[spinnerPart]).withStyle(ChatFormatting.BOLD), 0, (int) (font.lineHeight * 0.9f), 0xFFFFFFFF);
            context.pose().popMatrix();
            
            if(tickAccumulator >= 10f) {
                spinnerPart++;
                tickAccumulator = 0;
            }
            return;
        }

        if(getDimensionNID() == "minecraft_unknown_world") {
            Font font = Minecraft.getInstance().font;
            GraphicsHelper.drawCenteredString(context, font, Component.literal("No map tiles"), topLeftX + (minimapTileSize/ 2), topLeftZ, 0xFFFFFFFF);
            GraphicsHelper.drawCenteredString(context, font, Component.literal("available for"), topLeftX + (minimapTileSize/ 2), topLeftZ + font.lineHeight, 0xFFFFFFFF);
            GraphicsHelper.drawCenteredString(context, font, Component.literal("this world!"), topLeftX + (minimapTileSize/ 2), topLeftZ + (font.lineHeight * 2), 0xFFFFFFFF);
            int textCoordinatesPos = topLeftX + (minimapTileSize / 2);
            int textCoordinatesY = topLeftZ + minimapTileSize + pad + 2;
            GraphicsHelper.drawCenteredString(context, font, String.format("%d, %d, %d", Minecraft.getInstance().player.blockPosition().getX(),
                Minecraft.getInstance().player.blockPosition().getY(), Minecraft.getInstance().player.blockPosition().getZ()), textCoordinatesPos, textCoordinatesY, 0xFFFFFFFF);
            return;
        }

        if(!this.lastDimension.equals(getDimensionNID())) {
            this.lastDimension = getDimensionNID();
            resetTileImageCache();
        }
        boolean tooltipApplied = false;
        Minecraft mc = Minecraft.getInstance();
        double x = mc.player.getBlockX();
        double z = mc.player.getBlockZ();
        // Calculate tile size from zoom
        int renderZoom = Math.min(8, zoomlevel);
        int renderTileSize = 1 << (17 - renderZoom);
        int drawSize = BASE_TILE_SIZE * (1 << (zoomlevel - renderZoom));
        int tilesize = 1 << (17 - zoomlevel);
        double scale = (double) BASE_TILE_SIZE / tilesize;

        double worldLeft = x - ((minimapTileSize / 2.0) / scale);
        double worldTop = z - ((minimapTileSize / 2.0) / scale);
        double worldRight = x + ((minimapTileSize / 2.0) / scale);
        double worldBottom = z + ((minimapTileSize / 2.0) / scale);

        // Figure out which tiles to draw based on viewport bounds
        // If I'm stood at 0, 0 this tile will be -1_-1.png and be shifted by half the map box
        // If I'm stood at 256, 256 this tile will be 0_0.png and align with the top-left
        int topLeftTileX = (int) Math.floor(worldLeft / (double) renderTileSize);
        int topLeftTileZ = (int) Math.floor(worldTop / (double) renderTileSize);
        int bottomRightTileX = (int) Math.floor(worldRight / (double) renderTileSize) + 1;
        int bottomRightTileZ = (int) Math.floor(worldBottom / (double) renderTileSize) + 1;

        // Save ourselves requesting content we already have.
        if (this.lastTileX != topLeftTileX || this.lastTileZ != topLeftTileZ) {
            recalculateNetworks();
            this.lastTileX = topLeftTileX;
            this.lastTileZ = topLeftTileZ;

            // Add places in too
            int minx = topLeftTileX * renderTileSize;
            int maxx = bottomRightTileX * renderTileSize;
            int minz = topLeftTileZ * renderTileSize;
            int maxz = bottomRightTileZ * renderTileSize;
            try {
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(new URI(String.format("%s/fetch/places/%s/%d/%d/%d/%d", NetworkUtils.API_V1,
                        getDimensionNID(), minx, maxx, minz, maxz)))
                    .GET().build();
                NetworkUtils.HTTP_CLIENT.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200) {
                            Gson gson = new Gson();
                            placesList = new ArrayList<PlaceFetch>(Arrays.asList(gson.fromJson(response.body(), PlaceFetch[].class)));
                        } else {
                            LogUtils.error("Failed to fetch places from PVC Mapper! Code: " + response.statusCode());
                        }
                    })
                    .exceptionally(e -> {
                        LogUtils.error("Failed to fetch places from PVC Mapper!", e);
                        return null;
                    });
            } catch (Exception e) {
                LogUtils.error("Failed to fetch places from PVC Mapper!", e);
            }

            hasNotBeenInitialisedYet = false;
        }

        double xOffsetFromTileStart = worldLeft - ((double) topLeftTileX * renderTileSize);
        double zOffsetFromTileStart = worldTop - ((double) topLeftTileZ * renderTileSize);

        context.enableScissor(topLeftX, topLeftZ, topLeftX + minimapTileSize, topLeftZ + minimapTileSize);
        // Draw each tile to be visible
        for (int iX = topLeftTileX; iX < bottomRightTileX; iX++) {
            for (int iZ = topLeftTileZ; iZ < bottomRightTileZ; iZ++) {
                String thisDimension = getDimensionNID().equals("minecraft_terra2") && sp.useDarkTiles ? "minecraft_terra2_night" : getDimensionNID();
                String url = String.format("%s%s/%d/%d_%d.png",
                        sp.mapTileSource, thisDimension, renderZoom, iX, iZ);
                ResIdentifier tile = TextureUtils.getCachedTexture(url);
                if (tile == null) {
                    TextureUtils.fetchImmediateRemoteTexture(url, (id) -> {});
                    tile = TextureUtils.blurredTile;
                }
                context.pose().pushMatrix();
                context.pose().translate(
                        (float) ((topLeftX - (xOffsetFromTileStart * scale)) +
                                ((iX - topLeftTileX) * drawSize)),
                        (float) ((topLeftZ - (zOffsetFromTileStart * scale)) +
                                ((iZ - topLeftTileZ) * drawSize))
                );
                // Draw the texture with a size of drawSizexdrawSize
                // renderLayer, texture, x, y, width, height, u, v, regionWidth, regionHeight,
                // textureWidth, textureHeight
                context.blit(
                        RenderPipelines.GUI_TEXTURED, tile.get(),
                        0, 0,
                        0, 0,
                        drawSize, drawSize,
                        drawSize, drawSize
                );
                context.pose().popMatrix();
            }
        }

        // Draw networks first
        if(this.lastX != mc.player.getBlockX() || this.lastZ != mc.player.getBlockZ()) {
            recalculateNetworks();
        }
        this.lastX = mc.player.getBlockX();
        this.lastZ = mc.player.getBlockZ();
        if(sp.showNetworks && !sp.hideMinimapNetworks) {
            for (int i = 0; i < transportNetwork.getSegments().size(); i++) {
                TransportNetwork.Segment line = transportNetwork.getSegments().get(i);
                MapRenderUtils.drawLine(context, (int)line.coords[0][0] + topLeftZ, (int)line.coords[0][1] + topLeftX, (int)line.coords[1][0] + topLeftZ, (int)line.coords[1][1] + topLeftX, line.colour);
            }
        }

        context.disableScissor();

        // Check for mouse hover over the minimap
        double mx = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth()
                / mc.getWindow().getScreenWidth();
        double my = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight()
                / mc.getWindow().getScreenHeight();
        int mouseX = (int) mx;
        int mouseY = (int) my;
        boolean mouseIsInMap = false;
        if (mouseX > topLeftX && mouseX < (topLeftX + minimapTileSize) && mouseY > topLeftZ
                && mouseY < (topLeftZ + minimapTileSize))
            mouseIsInMap = true;

        int minX = (int) Math.floor(worldLeft);
        int minZ = (int) Math.floor(worldTop);
        int maxX = (int) Math.ceil(worldRight);
        int maxZ = (int) Math.ceil(worldBottom);
        if(sp.showPlaces) {
            for (int i = 0; i < placesList.size(); i++) {
                PlaceFetch place = placesList.get(i);
                int placeX = Integer.parseInt(place.x);
                int placeZ = Integer.parseInt(place.z);
                if ((placeX > minX && placeX < maxX) && (placeZ > minZ && placeZ < maxZ)) {
                    context.pose().pushMatrix();
                    double offsetFromPlayerX = (x - placeX) * scale;
                    double offsetFromPlayerZ = (z - placeZ) * scale;
                    float translateX = (float) ((topLeftX + (minimapTileSize / 2.0)) - offsetFromPlayerX);
                    float translateZ = (float) ((topLeftZ + (minimapTileSize / 2.0)) - offsetFromPlayerZ);
                    context.pose().translate(translateX, translateZ);
                    context.pose().translate(-4, -4);
                    ResIdentifier placeMarkerChoice;
                    switch (place.type) {
                        case "farm":
                        case "landmark":
                        case "museum":
                        case "lighthouse":
                            placeMarkerChoice = LANDMARK_BANNER;
                            break;
                        case "shop":
                        case "mall":
                            placeMarkerChoice = SHOP_BANNER;
                            break;
                        case "union":
                        case "base":
                        case "town":
                            placeMarkerChoice = BASE_BANNER;
                            break;
                        case "event":
                        case "pvp":
                            placeMarkerChoice = EVENT_BANNER;
                            break;
                        default:
                            placeMarkerChoice = GRAY_BANNER;
                            break;
                    }
                    context.blit(
                            RenderPipelines.GUI_TEXTURED,
                            placeMarkerChoice.get(),
                            0, 0, 0, 0, 8, 8, 8, 8);
                    context.pose().popMatrix();

                    // Render tooltip if being hovered
                    if (mouseIsInMap && !tooltipApplied) {
                        if (mouseX > translateX - 4 && mouseX < translateX + 4 && mouseY > translateZ - 4
                                && mouseY < translateZ + 4) {
                            tooltipApplied = true;
                            renderMinimapTooltip(
                                    context,
                                    List.of(
                                            String.format("%s (P%d): Type: %s", place.name, place.id, place.type),
                                            String.format("%s, %s in %s", place.x, place.z,
                                                    prettyDimensionName(place.dimension))));
                        }
                    }
                }
            }
        }

        
        ArrayList<PlayerFetch> playersList = pfu.getPlayers();
        if(sp.showPlayers) {
            // Now do the other players
            for (int i = 0; i < playersList.size(); i++) {
                PlayerFetch player = playersList.get(i);
                // If player is us, ignore. We know our whole life story already
                if(mc.player.getName().equals(Component.literal(player.name))) continue;
                // If player isn't in our space, ignore
                if (playerInCoords(minX, minZ, maxX, maxZ, player.world, player.x, player.z)) {
                    // Draw their marker - Sort rotation
                    double offsetFromPlayerX;
                    double offsetFromPlayerZ;
                    context.pose().pushMatrix();
                    
                    ResIdentifier playerMarkerChoice;
                    switch (player.world) {
                        case "minecraft_overworld":
                            if(getDimensionNID().equals("minecraft_overworld")) {
                                offsetFromPlayerX = (x - player.x) * scale;
                                offsetFromPlayerZ = (z - player.z) * scale;
                            } else {
                                offsetFromPlayerX = (x - (player.x/8)) * scale;
                                offsetFromPlayerZ = (z - (player.z/8)) * scale;
                            }
                            playerMarkerChoice = OTHER_PLAYERS_OW;
                            break;
                        case "minecraft_the_nether":
                            if(getDimensionNID().equals("minecraft_overworld")) {
                                offsetFromPlayerX = (x - (player.x*8)) * scale;
                                offsetFromPlayerZ = (z - (player.z*8)) * scale;
                            } else {
                                offsetFromPlayerX = (x - player.x) * scale;
                                offsetFromPlayerZ = (z - player.z) * scale;
                            }
                            playerMarkerChoice = OTHER_PLAYERS_NETHER;
                            break;
                        default:
                            offsetFromPlayerX = (x - player.x) * scale;
                            offsetFromPlayerZ = (z - player.z) * scale;
                            playerMarkerChoice = OTHER_PLAYERS_SOMEWHERE;
                            break;
                    }

                    float translateX = (float) ((topLeftX + (minimapTileSize / 2.0)) - offsetFromPlayerX);
                    float translateZ = (float) (topLeftZ + (minimapTileSize / 2.0) - offsetFromPlayerZ);
                    context.enableScissor(topLeftX - 4, topLeftZ - 4, topLeftX + minimapTileSize + 4, topLeftZ + minimapTileSize + 4);
                    context.pose().translate(translateX, translateZ);
                    context.pose().rotate((float) Math.toRadians(player.yaw - 180));
                    context.pose().translate(-4, -4);
                    // Now draw their marker
                    context.blit(
                            RenderPipelines.GUI_TEXTURED,
                            playerMarkerChoice.get(),
                            0, 0, 0, 0, 8, 8, 8, 8);

                    context.pose().popMatrix();

                    context.disableScissor();
                    // Render tooltip if being hovered
                    if (!tooltipApplied && mouseIsInMap) {
                        if (mouseX > translateX - 4 && mouseX < translateX + 4 && mouseY > translateZ - 4 && mouseY < translateZ + 4) {
                            tooltipApplied = true;
                            getTooltipPlayer(player.uuid, player.name);
                            renderMinimapTooltipPlayer(context,
                                List.of(
                                    player.name,
                                    String.format("%d, %d, %d in %s", player.x, player.y, player.z, prettyDimensionName(player.world)),
                                    String.format("Health: %.1f, Armor: %.1f", player.health, player.armor),
                                    player.afksince != null && 
                                        ((Instant.now().toEpochMilli() - Instant.parse(player.afksince).toEpochMilli()) / 60000) > 2 
                                        ? String.format("AFK for %d mins", (Instant.now().toEpochMilli() - Instant.parse(player.afksince).toEpochMilli()) / 60000)
                                        : "Currently Active"
                                ), player.uuid
                            );
                        }
                    }

                }

            }
        }


        // Get player yaw
        float yawDeg = mc.player.getYRot() + 180f;
        float yawRad = (float) Math.toRadians(yawDeg); // negate for screen space
        context.pose().pushMatrix();
        // Move origin to icon center
        context.pose().translate(topLeftX + (minimapTileSize / 2.0f), topLeftZ + (minimapTileSize / 2.0f));
        // Rotate around Z axis (screen space)
        context.pose().rotate(yawRad);
        // Move origin back
        context.pose().translate(-4, -4);
        context.blit(
                RenderPipelines.GUI_TEXTURED,
                PLAYER.get(),
                0, 0,
                0, 0, // u, v
                8, 8, // draw size
                8, 8 // texture size (map_icons.png)
        );
        context.pose().popMatrix();

        if(!tooltipApplied && (sp.showPlaces || sp.showPlayers) && /*? if <26.2 {*/mc.screen/*?} else {*//*mc.gui.screen()*//*?}*/ instanceof ChatScreen) {
            renderMinimapTooltip(context, List.of("Hover over the minimap's icons", "to view player or place details!"));
        }

        int textCoordinatesPos = topLeftX + (minimapTileSize / 2);
        int textCoordinatesY = topLeftZ + minimapTileSize + pad + 2;
        // Add coordinate string beneath minimap
        GraphicsHelper.drawCenteredString(context, mc.font, String.format("%d, %d, %d", mc.player.blockPosition().getX(),
                mc.player.blockPosition().getY(), mc.player.blockPosition().getZ()), textCoordinatesPos, textCoordinatesY, 0xFFFFFFFF);

        // Directions stuff
        if(dp != null && dp.routeActive) {
            List<String> cont = List.of();
            if(/*? if <26.2 {*/mc.screen/*?} else {*//*mc.gui.screen()*//*?}*/ instanceof ChatScreen) {
                cont = List.of(
                    String.format("%s %s %s", dp.directionArrow, dp.directionInstruction, dp.directionArrow),
                    String.format("in %dm", dp.directionDistance),
                    "To stop: /mapper stop-directions"
                );
            } else {
                cont = List.of(
                    String.format("%s %s %s", dp.directionArrow, dp.directionInstruction, dp.directionArrow),
                    String.format("in %dm", dp.directionDistance)
                );
            }
            int maxSize = 0;
            for (int i = 0; i < cont.size(); i++) {
                int w = mc.font.width(cont.get(i));
                if (w > maxSize) {
                    maxSize = w;
                }
            }
            int tooltipX = -1000;
            if(sp.miniMapPos == MiniMapPositions.TOP_RIGHT) {
                tooltipX = topLeftX - pad - 5 - maxSize;
            } else if(sp.miniMapPos == MiniMapPositions.TOP_LEFT) {
                tooltipX = topLeftX + minimapTileSize + pad + 5;
            }
            MapRenderUtils.drawTooltipString(context, cont, tooltipX, 8);   
        }
    }



}


