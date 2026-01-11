package larrytllama.pvcmappermod;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
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
    public static Minimap attach(PlayerFetchUtils pfu, SettingsProvider sp) {
        Minimap minimap = new Minimap();
        minimap.pfu = pfu;
        minimap.sp = sp;
        HudElementRegistry.attachElementBefore(VanillaHudElements.STATUS_EFFECTS,
                ResourceLocation.fromNamespaceAndPath("larrytllama.pvcmappermod", "before_chat"), minimap::render);
        return minimap;
    }

    private boolean hasNotBeenInitialisedYet = true;

    private int arrHasSubArr(int[][] array, int x, int z) {
        if (hasNotBeenInitialisedYet)
            return -1;
        for (int i = 0; i < array.length; i++) {
            if (array[i][0] == x && array[i][1] == z)
                return i;
        }
        return -1;
    }

    public ArrayList<PlaceFetch> placesList = new ArrayList<PlaceFetch>();
    private String highlightedPlayer;
    private String highlightedPlace;

    private void sendPlayerListFeedback() {
        
        // Calculate tile size from zoom
        int tilesize = 1 << (17 - zoomlevel);
        MutableComponent message = Component.literal("PVC Mapper Minimap\n").withStyle(ChatFormatting.GREEN,
                ChatFormatting.ITALIC);
        Minecraft instance = Minecraft.getInstance();
        int minX = (int) (instance.player.getX()) - (tilesize / 2);
        int minZ = (int) (instance.player.getZ()) - (tilesize / 2);
        int maxX = (int) (instance.player.getX()) + (tilesize / 2);
        int maxZ = (int) (instance.player.getZ()) + (tilesize / 2);
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

    // Image cache
    private ResourceLocation[] textureLocations = new ResourceLocation[4]; // Each x/y.
    private int[][] tileCoords = new int[4][2]; // Each x/y, each tile coord pair.
    
    // Zoom level for map
    public int zoomlevel = 8;
    public int minimapTileSize = 80;
    
    public static final ResourceLocation PLAYER = ResourceLocation.fromNamespaceAndPath("minecraft",
            "textures/map/decorations/player.png");
    public static final ResourceLocation OTHER_PLAYERS_OW = ResourceLocation.fromNamespaceAndPath("minecraft",
            "textures/map/decorations/frame.png");
    public static final ResourceLocation OTHER_PLAYERS_NETHER = ResourceLocation.fromNamespaceAndPath("minecraft",
            "textures/map/decorations/red_marker.png");
    public static final ResourceLocation OTHER_PLAYERS_SOMEWHERE = ResourceLocation.fromNamespaceAndPath("minecraft",
            "textures/map/decorations/blue_marker.png");

    public static final ResourceLocation SHOP_BANNER = ResourceLocation.fromNamespaceAndPath("minecraft",
            "textures/map/decorations/orange_banner.png");
    public static final ResourceLocation EVENT_BANNER = ResourceLocation.fromNamespaceAndPath("minecraft",
            "textures/map/decorations/magenta_banner.png");
    public static final ResourceLocation LANDMARK_BANNER = ResourceLocation.fromNamespaceAndPath("minecraft",
            "textures/map/decorations/yellow_banner.png");
    public static final ResourceLocation BASE_BANNER = ResourceLocation.fromNamespaceAndPath("minecraft",
            "textures/map/decorations/light_blue_banner.png");
    public static final ResourceLocation GRAY_BANNER = ResourceLocation.fromNamespaceAndPath("minecraft",
            "textures/map/decorations/light_gray_banner.png");

    public static final ResourceLocation MAP_BG = ResourceLocation.fromNamespaceAndPath("minecraft",
            "textures/gui/menu_background.png");

    private String prettyDimensionName(String dimension) {
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
        return "minecraft_" + Minecraft.getInstance().level.dimension().location().getPath();
    }

    private void renderMinimapTooltip(GuiGraphics context, @Nullable List<String> text) {
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
        int tooltipX = -1000;
        if(sp.miniMapPos == MiniMapPositions.TOP_RIGHT) {
            tooltipX = context.guiWidth() - 100 - maxSize;
        } else if(sp.miniMapPos == MiniMapPositions.TOP_LEFT) {
            tooltipX = 100;
        }
        TooltipRenderUtil.renderTooltipBackground(
                context,
                tooltipX,
                8,
                maxSize,
                mcfont.lineHeight * lines,
                null);
        for (int i = 0; i < lines; i++) {
            context.drawString(
                    mcfont,
                    tooltipText.get(i),
                    tooltipX,
                    8 + (i * mcfont.lineHeight),
                    0xFFFFFFFF, false);
        }
    }

    private ResourceLocation playerTooltipSkin = ResourceLocation.fromNamespaceAndPath("minecraft","textures/entity/player/wide/steve.png");;

    private void getTooltipPlayer(String uuid, String name) {
        Minecraft mc = Minecraft.getInstance();

        UUID dashed = UUID.fromString(
            uuid.substring(0, 8) + "-" +
            uuid.substring(8, 12) + "-" +
            uuid.substring(12, 16) + "-" +
            uuid.substring(16, 20) + "-" +
            uuid.substring(20)
        );
        ResolvableProfile resolvable = ResolvableProfile.createUnresolved(dashed);
        resolvable.resolveProfile(Minecraft.getInstance().services().profileResolver()).thenAccept((resolvedProfile) -> {
            CompletableFuture<Optional<PlayerSkin>> skin = mc.getSkinManager().get(resolvedProfile);
            skin.thenAccept(playerSkin -> {
                // Fallback to steeeeeeeeeve
                if (playerSkin.isEmpty()) playerTooltipSkin = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");
                else playerTooltipSkin = playerSkin.get().body().texturePath();
            });
        });
    }

    private void renderMinimapTooltipPlayer(GuiGraphics context, List<String> text) {
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
        int tooltipX = -1000;
        if(sp.miniMapPos == MiniMapPositions.TOP_RIGHT) {
            tooltipX = context.guiWidth() - 100 - maxSize;
        } else if(sp.miniMapPos == MiniMapPositions.TOP_LEFT) {
            tooltipX = 100;
        } else {
            System.out.println("Whoops no minimap pos: " + sp.miniMapPos);
        }
        TooltipRenderUtil.renderTooltipBackground(
                context,
                tooltipX,
                8,
                maxSize,
                mcfont.lineHeight * lines,
                null);
        for (int i = 0; i < lines; i++) {
            context.drawString(
                    mcfont,
                    tooltipText.get(i),
                    tooltipX,
                    8 + (i * mcfont.lineHeight),
                    0xFFFFFFFF, false);
        }

        context.blit(RenderPipelines.GUI_TEXTURED, playerTooltipSkin, tooltipX + mcfont.width(text.get(0)) + 1, 8, 8, 8, 8, 8, 64, 64);
        context.blit(RenderPipelines.GUI_TEXTURED, playerTooltipSkin, tooltipX + mcfont.width(text.get(0)) + 1, 8, 40, 8, 8, 8, 64, 64 );
    }

    private String lastDimension = "";

    public void resetTileImageCache() {
        // Reset image cache
        textureLocations = new ResourceLocation[4];
        // Including this one which should never realistically be a value
        // (Unless PVC's still expanding the map 15000 years later)
        tileCoords = new int[][] {
            { Integer.MIN_VALUE, Integer.MIN_VALUE },
            { Integer.MIN_VALUE, Integer.MIN_VALUE },
            { Integer.MIN_VALUE, Integer.MIN_VALUE },
            { Integer.MIN_VALUE, Integer.MIN_VALUE }
        };
    }

    public void render(GuiGraphics context, DeltaTracker tickCounter) {
        if(!sp.miniMapEnabled) return;
        if(!this.lastDimension.equals(getDimensionNID())) {
            this.lastDimension = getDimensionNID();
            // Reset image cache
            textureLocations = new ResourceLocation[4];
            // Including this one which should never realistically be a value
            // (Unless PVC's still expanding the map 15000 years later)
            tileCoords = new int[][] {
                { Integer.MIN_VALUE, Integer.MIN_VALUE },
                { Integer.MIN_VALUE, Integer.MIN_VALUE },
                { Integer.MIN_VALUE, Integer.MIN_VALUE },
                { Integer.MIN_VALUE, Integer.MIN_VALUE }
            };
        }
        boolean tooltipApplied = false;
        Minecraft mc = Minecraft.getInstance();
        int screenwidth = mc.getWindow().getGuiScaledWidth();
        double x = mc.player.getX();
        double z = mc.player.getZ();
        // Calculate tile size from zoom
        int tilesize = 1 << (17 - zoomlevel);
        // Make sure negative tiles start at -1 not -0
        // -256 to work with our 2x2 grid moving minimap
        int divX = Math.floorDiv((int) (x) - 256, tilesize);
        int divZ = Math.floorDiv((int) (z) - 256, tilesize);
        if (tileCoords[0][0] != divX || tileCoords[0][1] != divZ) {
            ResourceLocation[] tempArr = textureLocations.clone();
            // Save ourselves requesting content we already have.
            int newArrayIndex = 0;
            for (int i2 = divZ; i2 < divZ + 2; i2++) {
                for (int i = divX; i < divX + 2; i++) {
                    int arrayIndex = arrHasSubArr(tileCoords, i, i2);
                    if (arrayIndex != -1) {
                        textureLocations[newArrayIndex] = tempArr[arrayIndex];
                    } else {
                        int indexToSet = newArrayIndex;
                        String thisDimension = getDimensionNID().equals("minecraft_terra2") && sp.useDarkTiles ? "minecraft_terra2_night" : getDimensionNID();
                        String url = String.format("%s%s/%d/%d_%d.png",
                            sp.mapTileSource, thisDimension, zoomlevel, i, i2);
                        TextureUtils.fetchRemoteTexture(url, (id) -> {
                            textureLocations[indexToSet] = id;
                        });
                    }
                    newArrayIndex += 1;
                }
            }
            tileCoords[0][0] = divX;
            tileCoords[0][1] = divZ;
            tileCoords[1][0] = divX + 1;
            tileCoords[1][1] = divZ;
            tileCoords[2][0] = divX;
            tileCoords[2][1] = divZ + 1;
            tileCoords[3][0] = divX + 1;
            tileCoords[3][1] = divZ + 1;

            // Add places in too
            CompletableFuture.runAsync(() -> {
                int minx = tileCoords[0][0] * tilesize;
                int maxx = (tileCoords[3][0] * tilesize) + tilesize;
                int minz = tileCoords[0][1] * tilesize;
                int maxz = (tileCoords[3][1] * tilesize) + tilesize;
                try (Scanner scanner = new Scanner(new URI(
                        String.format("https://pvc.coolwebsite.uk/api/v1/fetch/places/%s/%d/%d/%d/%d",
                            getDimensionNID(), minx, maxx, minz, maxz))
                        .toURL().openStream(), "UTF-8")) {
                    String out = scanner.useDelimiter("\\A").next();
                    Gson gson = new Gson();
                    placesList = new ArrayList<PlaceFetch>(Arrays.asList(gson.fromJson(out, PlaceFetch[].class)));
                } catch (Exception e) {
                    System.out.println("Failed to fetch places from PVC Mapper!");
                    System.out.println(e);
                }
            });

            hasNotBeenInitialisedYet = false;
        }

        // Draw the texture at (10, 10) with a size of 64x64
        // renderLayer, texture, x, y, width, height, u, v, regionWidth, regionHeight,
        // textureWidth, textureHeight
        int topLeftX;
        if(sp.miniMapPos == MiniMapPositions.TOP_LEFT) {
            topLeftX = 8;
        } else {
            topLeftX = screenwidth - minimapTileSize - 8;
        }
        int topLeftZ = 8;

        double scale = (double) minimapTileSize / tilesize;

        double tileX = Math.floorDiv((long) x, tilesize);
        double tileZ = Math.floorDiv((long) z, tilesize);

        double localX = Math.floorMod((long) x, tilesize);
        double localZ = Math.floorMod((long) z, tilesize);

        double offsetX = localX * scale;
        double offsetZ = localZ * scale;

        double viewX = offsetX - (minimapTileSize / 2);
        double viewZ = offsetZ - (minimapTileSize / 2);

        context.blit(RenderPipelines.GUI_TEXTURED, MAP_BG, topLeftX - 5, topLeftZ - 5, 0, 0, minimapTileSize + 10,
                minimapTileSize + 10, 16, 16);

        context.enableScissor(topLeftX, topLeftZ, topLeftX + minimapTileSize, topLeftZ + minimapTileSize);
        // Draw each tile to be visible
        if (textureLocations[0] != null) {
            // If I'm stood at 0, 0 this tile will be -1_-1.png and be at topLeftX-40,
            // topLeftZ-40
            // If I'm stood at 256, 256 this tile will be 0_0.png and be at topLeftX-0,
            // topLeftZ-0
            double drawX = (divX + 0 - tileX) * minimapTileSize - viewX;
            double drawZ = (divZ + 0 - tileZ) * minimapTileSize - viewZ;
            context.pose().pushMatrix();
            context.pose().translate((float) (topLeftX + drawX), (float) (topLeftZ + drawZ));
            context.blit(RenderPipelines.GUI_TEXTURED, textureLocations[0], 0, 0, 0, 0, minimapTileSize,
                    minimapTileSize, minimapTileSize, minimapTileSize);
            context.pose().popMatrix();
        }
        if (textureLocations[1] != null) {
            double drawX = (divX + 1 - tileX) * minimapTileSize - viewX;
            double drawZ = (divZ + 0 - tileZ) * minimapTileSize - viewZ;
            context.pose().pushMatrix();
            context.pose().translate((float) (topLeftX + drawX), (float) (topLeftZ + drawZ));
            context.blit(RenderPipelines.GUI_TEXTURED, textureLocations[1], 0, 0, 0, 0, minimapTileSize,
                    minimapTileSize, minimapTileSize, minimapTileSize);
            context.pose().popMatrix();
        }
        if (textureLocations[2] != null) {
            double drawX = (divX + 0 - tileX) * minimapTileSize - viewX;
            double drawZ = (divZ + 1 - tileZ) * minimapTileSize - viewZ;
            context.pose().pushMatrix();
            context.pose().translate((float) (topLeftX + drawX), (float) (topLeftZ + drawZ));
            context.blit(RenderPipelines.GUI_TEXTURED, textureLocations[2], 0, 0, 0, 0, minimapTileSize,
                    minimapTileSize, minimapTileSize, minimapTileSize);
            context.pose().popMatrix();
        }
        if (textureLocations[3] != null) {
            double drawX = (divX + 1 - tileX) * minimapTileSize - viewX;
            double drawZ = (divZ + 1 - tileZ) * minimapTileSize - viewZ;
            context.pose().pushMatrix();
            context.pose().translate((float) (topLeftX + drawX), (float) (topLeftZ + drawZ));
            context.blit(RenderPipelines.GUI_TEXTURED, textureLocations[3], 0, 0, 0, 0, minimapTileSize,
                    minimapTileSize, minimapTileSize, minimapTileSize);
            context.pose().popMatrix();
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

        int minX = (int) (x) - (tilesize / 2);
        int minZ = (int) (z) - (tilesize / 2);
        int maxX = (int) (x) + (tilesize / 2);
        int maxZ = (int) (z) + (tilesize / 2);
        for (int i = 0; i < placesList.size(); i++) {
            PlaceFetch place = placesList.get(i);
            int placeX = Integer.parseInt(place.x);
            int placeZ = Integer.parseInt(place.z);
            if ((placeX > minX && placeX < maxX) && (placeZ > minZ && placeZ < maxZ)) {
                context.pose().pushMatrix();
                double offsetFromPlayerX = (x - placeX) * scale;
                double offsetFromPlayerZ = (z - placeZ) * scale;
                float translateX = (float) ((topLeftX + (minimapTileSize / 2)) - offsetFromPlayerX);
                float translateZ = (float) ((topLeftZ + (minimapTileSize / 2)) - offsetFromPlayerZ);
                context.pose().translate(translateX, translateZ);
                context.pose().translate(-4, -4);
                ResourceLocation placeMarkerChoice;
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
                        placeMarkerChoice,
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

        
        ArrayList<PlayerFetch> playersList = pfu.getPlayers();

        // Now do the other players
        for (int i = 0; i < playersList.size(); i++) {
            PlayerFetch player = playersList.get(i);
            // If player is us, ignore. We know our whole life story already
            if(mc.player.getName().equals(Component.literal(player.name))) continue;
            // If player isn't in our space, ignore
            if ((player.x > minX && player.x < maxX) && (player.z > minZ && player.z < maxZ)) {
                // Draw their marker - Sort rotation
                double offsetFromPlayerX;
                double offsetFromPlayerZ;
                context.pose().pushMatrix();
                
                ResourceLocation playerMarkerChoice;
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
                
                float translateX = (float) ((topLeftX + (minimapTileSize / 2)) - offsetFromPlayerX);
                float translateZ = (float) (topLeftZ + (minimapTileSize / 2) - offsetFromPlayerZ);
                context.enableScissor(topLeftX - 4, topLeftZ - 4, topLeftX + minimapTileSize + 4, topLeftZ + minimapTileSize + 4);
                context.pose().translate(translateX, translateZ);
                context.pose().rotate((float) Math.toRadians(player.yaw - 180));
                context.pose().translate(-4, -4);
                // Now draw their marker
                context.blit(
                        RenderPipelines.GUI_TEXTURED,
                        playerMarkerChoice,
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
                                String.format("%s", player.name),
                                String.format("%d, %d, %d in %s", player.x, player.y, player.z, prettyDimensionName(player.world)),
                                String.format("Health: %.1f, Armor: %.1f", player.health, player.armor)
                            )
                        );
                    }
                }

            }

        }


        // Get player yaw
        float yawDeg = mc.player.getYRot() + 180f;
        float yawRad = (float) Math.toRadians(yawDeg); // negate for screen space
        context.pose().pushMatrix();
        // Move origin to icon center
        context.pose().translate(topLeftX + (minimapTileSize / 2), topLeftZ + (minimapTileSize / 2));
        // Rotate around Z axis (screen space)
        context.pose().rotate(yawRad);
        // Move origin back
        context.pose().translate(-4, -4);
        context.blit(
                RenderPipelines.GUI_TEXTURED,
                PLAYER,
                0, 0,
                0, 0, // u, v
                8, 8, // draw size
                8, 8 // texture size (map_icons.png)
        );
        context.pose().popMatrix();

        if(!tooltipApplied && mc.screen instanceof ChatScreen) {
            renderMinimapTooltip(context, List.of("Hover over the minimap's icons", "to view player or place details!"));
        }

        int textCoordinatesPos = -100;
        if(sp.miniMapPos == MiniMapPositions.TOP_RIGHT) {
            textCoordinatesPos = screenwidth - 48;
        } else if(sp.miniMapPos == MiniMapPositions.TOP_LEFT) {
            textCoordinatesPos = 48;
        }
        // Add coordinate string beneath minimap
        context.drawCenteredString(mc.font, String.format("%d, %d, %d", mc.player.blockPosition().getX(),
                mc.player.blockPosition().getY(), mc.player.blockPosition().getZ()), textCoordinatesPos, 95, 0xFFFFFFFF);
    }



}
