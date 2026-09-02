package larrytllama.pvcmappermod;

import larrytllama.pvcmappermod.utils.*;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
//? if <1.21.11 {
import net.minecraft.Util;
//?} else {
/*import net.minecraft.util.Util;*///?}
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.time.Instant;

import org.lwjgl.glfw.GLFW;

import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//? if <26.1 {
import net.minecraft.client.gui.GuiGraphics;
//?} else {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;*///?}
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Checkbox.Builder;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;

public class FullScreenMap extends Screen {
    private PlayerFetchUtils pfu;
    public boolean overlayOpen;
    public int overlayItemID;
    public String overlayItemType;
    public FeatureTypes overlayFeature;
    public ResIdentifier overlayImage;
    public String overlayImageStatus;
    public ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private boolean isMouseDown = false;
    public SettingsProvider sp;
    public Network[] allNetworks = new Network[0];
    public static FullScreenMap createScreen(Component title, PlayerFetchUtils pfu, SettingsProvider sp) {
        FullScreenMap fsm = new FullScreenMap(title);
        fsm.pfu = pfu;
        fsm.sp = sp;
        // We will allow max level 11 on the full screen map but allow minimap to make the most of it all!
        // (mouse dragging goes weird after that point for some reason. TODO)
        fsm.zoomlevel = Math.min(11, sp.miniMapZoom); 

        pfu.fetchNetworksAsync().thenAccept(networks -> {
            fsm.allNetworks = networks;
        });

        return fsm;
    }


    public FullScreenMap(Component title) {
        super(title);
    }

    public void showToast(String title, String content) {
        CompatUtils.addToast(
            new SystemToast(SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                Component.literal(title),
                Component.literal(content)
            )
        );
    }

    public void navToCoords(int x, int z) {
        int tilesize = 1 << (17 - zoomlevel);
        double scale = (double) minimapTileSize / tilesize;
        this.x = (int)(x - ((this.width/2)/scale));
        this.z = (int)(z - (((this.height - bottomMapOffset)/2)/scale));
        onMouseMove(this.x, this.z);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (keyEvent.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (overlayOpen)
                overlayOpen = false;
            else
                CompatUtils.setScreen(null);
                //this.onClose(); // closes the screen
            return true;
        }
        return super.keyPressed(keyEvent);
    }

    // Screen positions
    private int topLeftX = 0;
    private int topLeftZ = 0;
    public int zoomlevel = 8;
    public int maxZoomLevel = 11;
    public int minZoomLevel = 1;

    public int x = 0;
    public int z = 0;
    
    public int minimapTileSize = 120;

    Map<String, ResIdentifier> tiles = new HashMap<>();

    public void resetTiles() {
        this.tiles = new HashMap<>();
    }
    ResIdentifier blurredTile = ResIdentifier.of("pvcmappermod", "textures/gui/tileloading.png");

    private boolean drawSponsorTooltip = false;

    int lastMouseX = 0;
    int lastMouseY = 0;

    // On mouse move, we'll check for new tiles
    private void onMouseMove(int mouseX, int mouseY) {
        int renderZoom = Math.min(8, zoomlevel);
        int renderTileSize = 1 << (17 - renderZoom);
        int tilesize = 1 << (17 - zoomlevel);
        double scale = (double) minimapTileSize / tilesize;
        double worldLeft = x;
        double worldTop = z;
        double worldRight = x + (this.width / scale);
        double worldBottom = z + (this.height / scale);
        String dimension = "" + currentDimension;
        // Figure out tile no. at top left/bottom right
        int topLeftTileX = MapRenderUtils.worldToTileCoordinate(worldLeft, renderTileSize) - 1;
        int topLeftTileZ = MapRenderUtils.worldToTileCoordinate(worldTop, renderTileSize) - 1;
        int bottomRightTileX = MapRenderUtils.worldToTileCoordinate(worldRight, renderTileSize) + 1;
        int bottomRightTileZ = MapRenderUtils.worldToTileCoordinate(worldBottom, renderTileSize) + 1;

        int thisZoomLevel = zoomlevel;
        for (int iX = topLeftTileX; iX < bottomRightTileX; iX++) {
            if(thisZoomLevel != zoomlevel && !currentDimension.equals(dimension)) break;
            for (int iZ = topLeftTileZ; iZ < bottomRightTileZ; iZ++) {
                if(thisZoomLevel != zoomlevel && !currentDimension.equals(dimension)) break;
                
                String thisDimension = dimension.equals("minecraft_terra2") && sp.useDarkTiles ? "minecraft_terra2_night" : dimension;
                String url = String.format("%s%s/%d/%d_%d.png",
                    sp.mapTileSource, thisDimension, renderZoom, iX, iZ);
                
                // Rely on TextureUtils to manage the cache and deduplicate pending fetches
                TextureUtils.fetchRemoteTexture(url, (id) -> {});
            }
        }
    }

    private int altZoomLevel = 8;

    public ArrayList<ClaimMarkers> shownClaims = new ArrayList<ClaimMarkers>();
    private Checkbox claimsCheckbox;

    public void resetClaims() {
        int tilesize = 1 << (17 - zoomlevel);
        double scale = (double) minimapTileSize / tilesize;
        if (claimsCheckbox.selected()) {
            if (zoomlevel < 8) {
                // Disable for safety
                CompatUtils.addToast(new SystemToast(
                        SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                        Component.literal("Too Many Claims!"),
                        Component.literal("Zoom in to re-enable claims")));
                this.removeWidget(claimsCheckbox);
                Builder checkboxBuilder = Checkbox.builder(Component.literal("Show Claims"), minecraft.font);
                claimsCheckbox = checkboxBuilder
                        .pos(this.width - minecraft.font.width("Show Claims") - 25, this.height - 25).build();
                this.addRenderableWidget(claimsCheckbox);
                shownClaims = new ArrayList<ClaimMarkers>();
                return;
            }
            LogUtils.debug("Checkbox selected! Finding claims...");
            shownClaims = pfu.getClaimsInBounds(currentDimension, x, (int) (x + (this.width / scale)), z,
                    (int) (z + ((this.height - bottomMapOffset) / scale)));
        } else {
            shownClaims = new ArrayList<ClaimMarkers>();
        }
    }

    public FeatureFetch[] shownFeatures = new FeatureFetch[0];

    private boolean isChangingFeatures = false;
    public void resetFeatures() {
        int tilesize = 1 << (17 - zoomlevel);
        double scale = (double) minimapTileSize / tilesize;
        pfu.fetchFeaturesAsync(
                currentDimension,
                x,
                (int) (x + (this.width / scale)),
                z,
                (int) (z + ((this.height - bottomMapOffset) / scale)))
            .thenAccept(features -> {
                // Perform bounds calculation on the local 'features' array first to prevent
                // concurrency crashes where the main thread reads half-updated arrays.
                for (int i = 0; i < features.length; i++) {
                    if(features[i].featureType.equals("area")) {
                        if(features[i].bounds == null) continue;
                        for (int bound = 0; bound < features[i].bounds.length; bound++) {
                            features[i].bounds[bound][0] = MapRenderUtils.metersToPixels(features[i].bounds[bound][0]);
                            features[i].bounds[bound][1] = MapRenderUtils.metersToPixels(features[i].bounds[bound][1]);
                        }
                    }
                }
                
                // Safely assign the final array to the global state on the main game thread.
                minecraft.execute(() -> {
                    isChangingFeatures = true;
                    shownFeatures = features;
                    isChangingFeatures = false;
                });
            });
        
        recalculateNetworks();
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double d, double e) {
        int tilesize = 1 << (17 - zoomlevel);
        double scale = (double) minimapTileSize / tilesize;
        x -= d / scale;
        z -= e / scale;
        onMouseMove(x, z);
        return super.mouseDragged(mouseButtonEvent, d, e);
    }

    int hasMovedX;
    int hasMovedZ;

    @Override
    public boolean mouseReleased(MouseButtonEvent mbe) {
        isMouseDown = false;
        if(hasMovedX != x || hasMovedZ != z) {
            resetClaims();
            resetFeatures();
        } else {
            int tilesize = 1 << (17 - zoomlevel);
            double scale = (double) minimapTileSize / tilesize;
            // Check to see if a thing has been clicked
            for (int i = shownFeatures.length - 1; i >= 0; i--) {
                if(shownFeatures[i].id == 1) {
                    LogUtils.debug("X/Z: " + ((shownFeatures[i].x - x) * scale) + " / " + ((shownFeatures[i].z - z) * scale));
                }
                if(shownFeatures[i].featureType.equals("area")) {
                    int itemWidth = minecraft.font.width(shownFeatures[i].name);
                    if( mbe.x() > ((shownFeatures[i].x - x) * scale) - (itemWidth / 2) &&
                        mbe.x() < ((shownFeatures[i].x - x) * scale) + (itemWidth / 2) &&
                        mbe.y() > ((shownFeatures[i].z - z) * scale) - (minecraft.font.lineHeight / 2) &&
                        mbe.y() < ((shownFeatures[i].z - z) * scale) + (minecraft.font.lineHeight / 2)) {
                        if(mbe.hasControlDown()) {
                            CompatUtils.setScreen(minecraft, new ChatScreen(String.format("%s: %d, %d in %s", shownFeatures[i].name, (int) shownFeatures[i].x, (int) shownFeatures[i].z, pfu.prettyDimensionName(currentDimension)), false));
                        } else {
                            LogUtils.debug("Feature clicked: " + shownFeatures[i].id);
                            int index = i;
                            pfu.fetchAreaAsync(shownFeatures[index].id)
                                .thenAccept(feature -> {
                                    overlayFeature = feature;
                                    overlayItemID = shownFeatures[index].id;
                                    overlayItemType = "area";
                                    overlayOpen = true;
                                    overlayImage = null;
                                    overlayImageStatus = "Loading...";
                                    if(overlayFeature.area.image != null) {
                                        TextureUtils.fetchImmediateRemoteTexture(overlayFeature.area.image, (id) -> {
                                            overlayImage = id;
                                            overlayImageStatus = "No image available";
                                        });
                                    } else {
                                        overlayImageStatus = "No image available";
                                    }
                                });
                        }

                    }
                } else {
                    if( mbe.x() > ((shownFeatures[i].x - x) * scale) - 4 &&
                        mbe.x() < ((shownFeatures[i].x - x) * scale) + 4 &&
                        mbe.y() > ((shownFeatures[i].z - z) * scale) - 4 &&
                        mbe.y() < ((shownFeatures[i].z - z) * scale) + 4) {
                        int index = i;
                        switch (shownFeatures[i].featureType) {
                            case "place":
                                if(mbe.hasControlDown()) {
                                    CompatUtils.setScreen(minecraft, new ChatScreen(String.format("%s: %d, %d in %s", shownFeatures[i].name, (int) shownFeatures[i].x, (int) shownFeatures[i].z, pfu.prettyDimensionName(currentDimension)), false));
                                } else {
                                    pfu.fetchPlaceAsync(shownFeatures[index].id)
                                        .thenAccept(feature -> {
                                            overlayFeature = feature;
                                            overlayItemID = shownFeatures[index].id;
                                            overlayItemType = "place";
                                            overlayOpen = true;
                                            overlayImage = null;
                                            overlayImageStatus = "Loading...";
                                            if(overlayFeature.place.images != null) {
                                                TextureUtils.fetchImmediateRemoteTexture(overlayFeature.place.images, (id) -> {
                                                    overlayImage = id;
                                                    overlayImageStatus = "No image available";
                                                });
                                            } else {
                                                overlayImageStatus = "No image available";
                                            }
                                        });
                                }
                                break;

                            case "portal":
                                if(mbe.hasControlDown()) {
                                    CompatUtils.setScreen(minecraft, new ChatScreen(String.format("%s: %d, %d in %s", pfu.getPortalPrettyName(shownFeatures[i].type), (int) shownFeatures[i].x, (int) shownFeatures[i].z, pfu.prettyDimensionName(currentDimension)), false));
                                }
                                break;
                            default:
                                showToast("Feature view error", "Unknown or unsupported feature type '" + shownFeatures[i].featureType + "'");
                                break;
                        }
                        break;
                    }
                }
            }
        }
        return super.mouseReleased(mbe);
    }

    @Override
    public boolean mouseScrolled(double a, double b, double c, double d) {
        int scroll = (int) Math.signum(d);
        if (scroll == 0) return super.mouseScrolled(a, b, c, d);
        // Get cursor position in world offset
        int oldtilesize = 1 << (17 - zoomlevel);
        double oldscale = (double) minimapTileSize / oldtilesize;
        double oldCursorWorldOffsetX = a / oldscale;
        double oldCursorWorldOffsetZ = b / oldscale;
        // Move to new zoom level
        zoomlevel += scroll;
        zoomlevel = Math.max(minZoomLevel, Math.min(maxZoomLevel, zoomlevel));
        // Set position to new x/z
        int newtilesize = 1 << (17 - zoomlevel);
        double newscale = (double) minimapTileSize / newtilesize;
        double newCursorWorldOffsetX = a / newscale;
        double newCursorWorldOffsetZ = b / newscale;
        x -= (newCursorWorldOffsetX - oldCursorWorldOffsetX);
        z -= (newCursorWorldOffsetZ - oldCursorWorldOffsetZ);
        int newZoomLevel = zoomlevel;
        executor.schedule(() -> {
            if(newZoomLevel == zoomlevel) {
                recalculateNetworks();
                onMouseMove(x, z);
                resetClaims();
            }
        }, 250, TimeUnit.MILLISECONDS);
        return super.mouseScrolled(a, b, c, d);
    }

    private int hoveredPlaceIndex;
    private int hoveredClaimIndex;

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        // Reset hover states before recalculating what's under the cursor
        hoveredPlaceIndex = -1;
        hoveredClaimIndex = -1;

        int tilesize = 1 << (17 - zoomlevel);
        double scale = (double) minimapTileSize / tilesize;
        if (sponsorBanner != null) {
            if (mouseY > (this.height - 28) && mouseY < (this.height - 3) && mouseX > 3 && mouseX < 196) {
                drawSponsorTooltip = true;
            } else {
                drawSponsorTooltip = false;
            }
        } else {
            drawSponsorTooltip = false;
        }

        // If mouse is in
        if (mouseY < this.height - bottomMapOffset) {
            for (int i = shownFeatures.length - 1; i >= 0; i--) {
                if (shownFeatures[i].featureType.equals("area")) {
                    double featureX = (minecraft.font.width(shownFeatures[i].name) * 0.5) / 2;
                    double featureY = (minecraft.font.lineHeight * 0.5) / 2;
                    if (mouseX > ((shownFeatures[i].x - x) * scale) - featureX &&
                            mouseX < ((shownFeatures[i].x - x) * scale) + featureX &&
                            mouseY > ((shownFeatures[i].z - z) * scale) - featureY &&
                            mouseY < ((shownFeatures[i].z - z) * scale) + featureY) {
                        hoveredPlaceIndex = i;
                        break;
                    }
                } else {
                    if (mouseX > ((shownFeatures[i].x - x) * scale) - 4
                            && mouseX < ((shownFeatures[i].x - x) * scale) + 4 &&
                            mouseY > ((shownFeatures[i].z - z) * scale) - 4
                            && mouseY < ((shownFeatures[i].z - z) * scale) + 4) {
                        hoveredPlaceIndex = i;
                        break;
                    }
                }
            }
            if(hoveredPlaceIndex == -1) {
                for (int i = shownClaims.size() - 1; i >= 0; i--) {
                    ClaimMarkers claim = shownClaims.get(i);
                    if(claim.type.equals("polgyon")) continue;
                    if(mouseX > (claim.points[0].x - x) * scale &&
                        mouseX < (claim.points[1].x - x) * scale &&
                        mouseY > (claim.points[0].z - z) * scale &&
                        mouseY < (claim.points[1].z - z) * scale
                    ) {
                        hoveredClaimIndex = i;
                        break;
                    }
                }

            }

            // Set text to draw coords to 
            currentLocationCoordinates = Component.literal(String.format("%d, %d", (int) (x + (mouseX / scale)), (int) (z + (mouseY / scale))));
        } else {
            Component.literal("--, --");
        }

        super.mouseMoved(mouseX, mouseY);
    }

    public MutableComponent currentLocationCoordinates = Component.literal("--, --");

    @Override
    public boolean mouseClicked(MouseButtonEvent mbe, boolean bl) {
        isMouseDown = true;
        hasMovedX = x;
        hasMovedZ = z;
        if (mbe.y() > (this.height - 28) && mbe.y() < (this.height - 3) && mbe.x() > 3 && mbe.x() < 196) {
            Minecraft mc = Minecraft.getInstance();
            CompatUtils.setScreen(mc, new ConfirmLinkScreen(confirmed -> {
                if (confirmed) {
                    Util.getPlatform().openUri(sponsorURLString);
                }
                CompatUtils.setScreen(mc, null);
            }, sponsorURLString, true));
        }
        return super.mouseClicked(mbe, bl);
    }

    private String hoverPlayerName;
    private ResIdentifier hoverPlayerFace = PlayerSkinHelper.DEFAULT_SKIN;

    private void getTooltipPlayer(String uuid, String name) {
        PlayerSkinHelper.fetchSkin(uuid, "FullScreenMap", (skin) -> {
            hoverPlayerFace = skin;
        });
    }

    public void drawPlayerTooltip(/*? if <26.1 {*/GuiGraphics/*?} else {*//*GuiGraphicsExtractor*//*?}*/ context, PlayerFetch player, int x, int y) {

        List<MutableComponent> content = List.of(
                Component.literal(player.name).withStyle(ChatFormatting.BOLD),
                Component.literal(player.x + ", " + player.z + " - " + pfu.prettyDimensionName(player.world)),
                Component.literal("Health: " + (player.health / 2) + "/10 - Armor: " + (player.armor / 2) + "/10"),
                player.afksince != null && 
                    ((Instant.now().toEpochMilli() - Instant.parse(player.afksince).toEpochMilli()) / 60000) > 2 
                    ? Component.literal(String.format("AFK for %d mins", (Instant.now().toEpochMilli() - Instant.parse(player.afksince).toEpochMilli()) / 60000)) 
                    : Component.literal("Currently Active")
            );
        
        MapRenderUtils.drawTooltipComponent(context, content, x + TooltipRenderUtil.PADDING_LEFT, y + TooltipRenderUtil.PADDING_TOP);
        if (hoverPlayerName != player.name) {
            getTooltipPlayer(player.uuid, player.name);
            hoverPlayerName = player.name;
        }

        context.blit(RenderPipelines.GUI_TEXTURED, hoverPlayerFace.get(),
                x + TooltipRenderUtil.PADDING_LEFT + minecraft.font.width(content.get(0)) + 1,
                y + TooltipRenderUtil.PADDING_TOP, 8, 8, 8, 8, 64, 64);
        context.blit(RenderPipelines.GUI_TEXTURED, hoverPlayerFace.get(),
                x + TooltipRenderUtil.PADDING_LEFT + minecraft.font.width(content.get(0)) + 1,
                y + TooltipRenderUtil.PADDING_TOP, 40, 8, 8, 8, 64, 64);
    }

    // Improved drawLine

    private int bottomMapOffset = 31;

    public ResIdentifier sponsorBanner;
    public List<MutableComponent> sponsorHoverText;
    public String sponsorURLString;

    private final ResIdentifier searchIcon = ResIdentifier.of("minecraft",
            "textures/gui/sprites/icon/search.png");
    private final ResIdentifier settingsIcon = ResIdentifier.of("pvcmappermod",
            "textures/gui/settings.png");
    private final ResIdentifier compassIcon = ResIdentifier.of("minecraft",
            "textures/item/compass_19.png");

    private final ResIdentifier OVERWORLD = ResIdentifier.of("minecraft", "overworld");
    private final ResIdentifier NETHER = ResIdentifier.of("minecraft", "the_nether");
    // Heh, eng
    private final ResIdentifier ENG = ResIdentifier.of("minecraft", "the_end");

    private final TransportNetwork transportNetwork = new TransportNetwork();

    public void recalculateNetworks() {
        int tilesize = 1 << (17 - zoomlevel);
        double scale = (double) minimapTileSize / tilesize;

        transportNetwork.recalculate(allNetworks, currentDimension, zoomlevel, minimapTileSize, x, z, (this.width / scale), ((this.height - bottomMapOffset) / scale), "FullScreenMap");
    }

    public String currentDimension = getDimensionID();

    int lastX = 0;
    int lastZ = 0;

    //? if <26.1 {
    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
    //?} else {
    /*@Override
    public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {*///?}
        if (overlayOpen && overlayFeature != null) {
            GraphicsHelper.drawString(context, minecraft.font, Component.literal("Press ESC to go back").withStyle(ChatFormatting.ITALIC), 5, 5, 0xFF555555);
            if(overlayItemType.equals("place")) {
                // Title
                GraphicsHelper.drawCenteredString(context, minecraft.font, Component.literal(overlayFeature.place.name).withStyle(ChatFormatting.BOLD), this.width / 2, 40, 0xFFFFFFFF);
                // Dividing line
                GraphicsHelper.vLine(context, this.width / 2, 60, this.height - 20, 0xFF636363);
                // Description
                List<FormattedCharSequence> lines = minecraft.font.split(Component.literal(overlayFeature.place.description), (this.width / 2) - 20 );
                int descHeight = lines.size() * minecraft.font.lineHeight;
                for (int i = 0; i < lines.size(); i++) {
                    GraphicsHelper.drawString(context, minecraft.font, lines.get(i), (this.width / 2) + 10, 63 + (i * minecraft.font.lineHeight), 0xFFFFFFFF);
                }
                
                // Location
                context.blit(RenderPipelines.GUI_TEXTURED, ResIdentifier.of("minecraft", "textures/gui/sprites/icon/link.png").get(), (this.width/2) + 8, 66 + descHeight, 0, 0, 12, 12, 12, 12);
                GraphicsHelper.drawString(context, minecraft.font, String.format("%s, %s in %s", overlayFeature.place.x, overlayFeature.place.z, pfu.prettyDimensionName(overlayFeature.place.dimension)), (this.width / 2) + 22, 68 + descHeight, 0xFFFFFFFF);
            
                // Added by
                context.blit(RenderPipelines.GUI_TEXTURED, ResIdentifier.of("minecraft", "textures/gui/sprites/icon/accessibility.png").get(), (this.width/2) + 8, 72 + descHeight + minecraft.font.lineHeight, 0, 0, 12, 12, 12, 12);
                GraphicsHelper.drawString(context, minecraft.font, String.format("Added by %s", overlayFeature.place.addedBy.name), (this.width / 2) + 22, 74 + descHeight + minecraft.font.lineHeight, 0xFFFFFFFF);

                // Wiki Link
                if(overlayFeature.place.wiki != null && overlayFeature.place.wiki.length() > 1) {
                    context.blit(RenderPipelines.GUI_TEXTURED, ResIdentifier.of("minecraft", "textures/gui/sprites/toast/social_interactions.png").get(), (this.width/2) + 8, 76 + descHeight + (minecraft.font.lineHeight * 2), 0, 0, 12, 12, 12, 12);
                    GraphicsHelper.drawString(context, minecraft.font, overlayFeature.place.wiki, (this.width / 2) + 22, 78 + descHeight + (minecraft.font.lineHeight * 2), 0xFFFFFFFF);
                }

                int currentHeight = 83 + descHeight + (minecraft.font.lineHeight * 3);

                // Features
                if(overlayFeature.place.features != null) {
                    if(overlayFeature.place.features.Public) {
                        context.blit(RenderPipelines.GUI_TEXTURED, ResIdentifier.of("minecraft", "textures/gui/sprites/icon/new_realm.png").get(), (this.width/2) + 8, currentHeight, 0, 0, 24, 12, 24, 12);
                        GraphicsHelper.drawString(context, minecraft.font, "Made for public use", (this.width / 2) + 32, currentHeight + 2, 0xFFFFFFFF);
                        currentHeight += minecraft.font.lineHeight + 5;
                    }
                    if(overlayFeature.place.features.echest) {
                        context.blit(RenderPipelines.GUI_TEXTURED, ResIdentifier.of("minecraft", "textures/item/ender_eye.png").get(), (this.width/2) + 14, currentHeight, 0, 0, 12, 12, 12, 12);
                        GraphicsHelper.drawString(context, minecraft.font, "Has ender chest access nearby", (this.width / 2) + 32, currentHeight + 2, 0xFFFFFFFF);
                        currentHeight += minecraft.font.lineHeight + 5;
                    }
                    if(overlayFeature.place.features.portal) {
                        context.blit(RenderPipelines.GUI_TEXTURED, ResIdentifier.of("minecraft", "textures/block/nether_portal.png").get(), (this.width/2) + 14, currentHeight, 0, 0, 12, 12, 12, 384);
                        GraphicsHelper.drawString(context, minecraft.font, "Has nether portal access nearby", (this.width / 2) + 32, currentHeight + 2, 0xFFFFFFFF);
                        currentHeight += minecraft.font.lineHeight + 5;
                    }
                    if(overlayFeature.place.features.historical) {
                        context.blit(RenderPipelines.GUI_TEXTURED, ResIdentifier.of("minecraft", "textures/item/clock_00.png").get(), (this.width/2) + 14, currentHeight, 0, 0, 12, 12, 12, 12);
                        GraphicsHelper.drawString(context, minecraft.font, "Historical Place", (this.width / 2) + 32, currentHeight + 2, 0xFFFFFFFF);
                        currentHeight += minecraft.font.lineHeight + 5;
                    }
                }

                context.fill(this.width / 6, 63, (this.width / 2) - 10, 183, 0x70000000);

                if(overlayImage != null) {
                    context.blit(RenderPipelines.GUI_TEXTURED, overlayImage.get(), this.width / 6, 63, 0, 0, (this.width / 2) - (this.width / 6) - 10, 120, (this.width / 2) - (this.width / 6) - 10, 120);
                } else {
                    GraphicsHelper.drawCenteredString(context, minecraft.font, overlayImageStatus, (((this.width / 2) - 10 - (this.width / 6)) / 2) + (this.width / 6), 121, 0xFFFFFFFF);
                }


            } else if(overlayItemType.equals("area")) {
                try {
                    // Title
                    GraphicsHelper.drawCenteredString(context, minecraft.font, Component.literal(overlayFeature.area.name).withStyle(ChatFormatting.BOLD), this.width / 2, 40, 0xFFFFFFFF);
                    // Dividing line
                    GraphicsHelper.vLine(context, this.width / 2, 60, this.height - 20, 0xFF636363);
                    // Description
                    List<FormattedCharSequence> lines = minecraft.font.split(Component.literal(overlayFeature.area.description), (this.width / 2) - 20 );
                    int descHeight = lines.size() * minecraft.font.lineHeight;
                    for (int i = 0; i < lines.size(); i++) {
                        GraphicsHelper.drawString(context, minecraft.font, lines.get(i), (this.width / 2) + 10, 63 + (i * minecraft.font.lineHeight), 0xFFFFFFFF);
                    }

                    // Location
                    context.blit(RenderPipelines.GUI_TEXTURED, ResIdentifier.of("minecraft", "textures/gui/sprites/icon/link.png").get(), (this.width/2) + 8, 66 + descHeight, 0, 0, 12, 12, 12, 12);
                    GraphicsHelper.drawString(context, minecraft.font, String.format("%s, %s in %s", overlayFeature.area.x, overlayFeature.area.z, pfu.prettyDimensionName(overlayFeature.area.dimension)), (this.width / 2) + 22, 68 + descHeight, 0xFFFFFFFF);
                
                    // Added by
                    context.blit(RenderPipelines.GUI_TEXTURED, ResIdentifier.of("minecraft", "textures/gui/sprites/icon/accessibility.png").get(), (this.width/2) + 8, 72 + descHeight + minecraft.font.lineHeight, 0, 0, 12, 12, 12, 12);
                    GraphicsHelper.drawString(context, minecraft.font, String.format("Added by %s", overlayFeature.area.addedBy.username), (this.width / 2) + 22, 74 + descHeight + minecraft.font.lineHeight, 0xFFFFFFFF);

                    // Wiki Link
                    if(overlayFeature.area.wiki != null && overlayFeature.area.wiki.length() > 1) {
                        context.blit(RenderPipelines.GUI_TEXTURED, ResIdentifier.of("minecraft", "textures/gui/sprites/toast/social_interactions.png").get(), (this.width/2) + 8, 76 + descHeight + (minecraft.font.lineHeight * 2), 0, 0, 12, 12, 12, 12);
                        GraphicsHelper.drawString(context, minecraft.font, overlayFeature.area.wiki, (this.width / 2) + 22, 78 + descHeight + (minecraft.font.lineHeight * 2), 0xFFFFFFFF);
                    }

                    context.fill(this.width / 6, 63, (this.width / 2) - 10, 183, 0x70000000);

                    if(overlayImage != null) {
                        context.blit(RenderPipelines.GUI_TEXTURED, overlayImage.get(), this.width / 6, 63, 0, 0, (this.width / 2) - (this.width / 6) - 10, 120, (this.width / 2) - (this.width / 6) - 10, 120);
                    } else {
                        GraphicsHelper.drawCenteredString(context, minecraft.font, overlayImageStatus, (((this.width / 2) - 10 - (this.width / 6)) / 2) + (this.width / 6), 121, 0xFFFFFFFF);
                    }
                } catch(Exception e) {
                    LogUtils.debug("[PVC Mapper Mod] Unable to write area details to screen renderererer.");
                }
            }
        } else {
            int renderZoom = Math.min(8, zoomlevel);
            int renderTileSize = 1 << (17 - renderZoom);
            int drawSize = minimapTileSize * (1 << (zoomlevel - renderZoom));
            
            int tilesize = 1 << (17 - zoomlevel);
            double scale = (double) minimapTileSize / tilesize;
            double worldLeft = x;
            double worldTop = z;
            double worldRight = x + (this.width / scale);
            double worldBottom = z + ((this.height - bottomMapOffset) / scale);

            // Figure out tile no. at top left/bottom right
            int topLeftTileX = MapRenderUtils.worldToTileCoordinate(worldLeft, renderTileSize);
            int topLeftTileZ = MapRenderUtils.worldToTileCoordinate(worldTop, renderTileSize);
            int bottomRightTileX = MapRenderUtils.worldToTileCoordinate(worldRight, renderTileSize) + 1;
            int bottomRightTileZ = MapRenderUtils.worldToTileCoordinate(worldBottom, renderTileSize) + 1;

            // Iterate thru visible tiles
            double xOffsetFromTileStart = MapRenderUtils.worldToLocalTileCoordinate(worldLeft, renderTileSize);
            double zOffsetFromTileStart = MapRenderUtils.worldToLocalTileCoordinate(worldTop, renderTileSize);
            context.scissorStack.push(new ScreenRectangle(0, 0, this.width, this.height - bottomMapOffset));
            for (int iX = topLeftTileX; iX < bottomRightTileX; iX++) {
                for (int iZ = topLeftTileZ; iZ < bottomRightTileZ; iZ++) {
                    String thisDimension = currentDimension.equals("minecraft_terra2") && sp.useDarkTiles ? "minecraft_terra2_night" : currentDimension;
                    String url = String.format("%s%s/%d/%d_%d.png", sp.mapTileSource, thisDimension, renderZoom, iX, iZ);
                    ResIdentifier tile = TextureUtils.getCachedTexture(url);
                    if (tile == null)
                        tile = TextureUtils.blurredTile;
                    context.pose().pushMatrix();
                    context.pose().translate(
                            (float) ((topLeftX - (xOffsetFromTileStart * scale)) + // The top left GUI pos - how far
                                                                                   // from the start of the tile
                                    ((iX - topLeftTileX) * drawSize)), // + How many tiles along we are
                            (float) ((topLeftZ - (zOffsetFromTileStart * scale)) + // All the same
                                    ((iZ - topLeftTileZ) * drawSize)) // For the Z axis
                    );
                    context.blit(
                            RenderPipelines.GUI_TEXTURED, tile.get(), // Render tile with the following x pos:
                            0, 0,
                            0, 0, // u/v
                            drawSize, drawSize, // width/height
                            drawSize, drawSize // texturewidth/textureheight
                    );
                    context.pose().popMatrix();
                }
            }

            // Draw networks first
            if(this.lastX != this.x || this.lastZ != this.z) {
                recalculateNetworks();
            }
            this.lastX = this.x;
            this.lastZ = this.z;
            for (int i = 0; i < transportNetwork.getSegments().size(); i++) {
                TransportNetwork.Segment line = transportNetwork.getSegments().get(i);
                MapRenderUtils.drawLine(context, (int)line.coords[0][0], (int)line.coords[0][1], (int)line.coords[1][0], (int)line.coords[1][1], line.colour);
            }

            int networkLinePadding = 75;

            // Draw street names on top
            if(zoomlevel > 9) {
                for (int i = 0; i<transportNetwork.getSegments().size();i++) {
                    TransportNetwork.Segment line = transportNetwork.getSegments().get(i);
                    int nameLength = minecraft.font.width(line.streetName) / 2;
                    double[][] coords = line.coords;

                    // Figure out how many we can fit along, with padding on either side.
                    double lineLength = Math.sqrt( 
                            Math.pow(
                                Math.max(coords[0][1], coords[1][1]) - Math.min(coords[1][1], coords[0][1]), 
                            (double)(2)) + Math.pow(
                                Math.max(coords[1][0], coords[0][0]) - Math.min(coords[1][0], coords[0][0]),
                            (double)(2))
                        );
                    int numberToDraw = (int)Math.floor(lineLength / (nameLength + (networkLinePadding) / scale));
                    
                    while(numberToDraw > 0) {
                        context.pose().pushMatrix();
                        context.pose().translate((float) (coords[0][0]), (float) (coords[0][1]));
                        context.pose().rotate((float)Math.toRadians(line.lineBearing));
                        if(numberToDraw * (networkLinePadding + (line.nameWidth / 2)) > lineLength) {
                            context.pose().popMatrix();
                            numberToDraw -= 1;
                            continue;
                        }
                        context.pose().translate((float)(numberToDraw * (networkLinePadding + (line.nameWidth / 2))), 0);
                        //context.pose().rotate(-(float)Math.toRadians(line.lineBearing));
                        if(line.lineBearing > 90 && line.lineBearing < 270) context.pose().rotate((float)Math.toRadians(-180));
                        context.pose().scale((float) 0.5, (float) 0.5);
                        context.fill(-(line.nameWidth / 2) - 2, -2, (line.nameWidth / 2) + 2, minecraft.font.lineHeight + 2, 0x80000000);
                        GraphicsHelper.drawCenteredString(context, minecraft.font, line.streetName, 0, 0, 0xFFFFFFFF);
                        context.pose().popMatrix();
                        numberToDraw-=1;
                    }
                }
            }

            // Draw claims
            for (int i = 0; i < shownClaims.size(); i++) {
                ClaimMarkers claim = shownClaims.get(i);
                if (claim.type.equals("rectangle")) {
                    context.pose().pushMatrix();
                    context.pose().translate((float) ((claim.points[0].x - x) * scale),
                            (float) ((claim.points[0].z - z) * scale));
                    context.fill(0, 0, (int) ((claim.points[1].x - claim.points[0].x) * scale),
                            (int) ((claim.points[1].z - claim.points[0].z) * scale),
                            (int) Long.parseLong(String.format("%02X%s", Math.round(claim.fillOpacity * 255f),
                                    claim.fillColor.substring(1)), 16));

                    // Draw outlines
                    int outlineColor = (int) Long.parseLong("ff" + claim.color.substring(1), 16);
                    int width = (int) ((claim.points[1].x - claim.points[0].x) * scale);
                    int height = (int) ((claim.points[1].z - claim.points[0].z) * scale);
                    // Left
                    GraphicsHelper.vLine(context, 0, 0, height, outlineColor);
                    // Right
                    GraphicsHelper.vLine(context, width, 0, height, outlineColor);
                    // Top
                    GraphicsHelper.hLine(context, 0, width, 0, outlineColor);
                    // Bottom
                    GraphicsHelper.hLine(context, 0, width, height, outlineColor);
                    context.pose().popMatrix();
                    // context.submitOutline((int)((claim.points[0].x - x) * scale),
                    // (int)((claim.points[0].z - z) * scale), (int)((claim.points[1].x -
                    // claim.points[0].x) * scale) + 1, (int)((claim.points[1].z -
                    // claim.points[0].z) * scale) + 1, (int) Long.parseLong("ff" +
                    // claim.color.substring(1), 16));
                }
            }

            // Draw area bounds on hover before place labels
            try {
            if(!isMouseDown && !isChangingFeatures && hoveredPlaceIndex != -1 && hoveredPlaceIndex < shownFeatures.length && 
                shownFeatures[hoveredPlaceIndex].featureType.equals("area") &&
                shownFeatures[hoveredPlaceIndex].bounds != null) { 
                int boundlength = shownFeatures[hoveredPlaceIndex].bounds.length;
                for (int bound = 0; bound < boundlength  - 1; bound++) {
                    if( shownFeatures[hoveredPlaceIndex].bounds != null &&
                        shownFeatures[hoveredPlaceIndex].bounds[bound].length == 2
                    ) MapRenderUtils.drawLine(context,
                        (int) ((shownFeatures[hoveredPlaceIndex].bounds[bound][1] - x)*scale),
                        (int) ((shownFeatures[hoveredPlaceIndex].bounds[bound][0] - z)*scale),
                        (int) ((shownFeatures[hoveredPlaceIndex].bounds[bound + 1][1] - x)*scale),
                        (int) ((shownFeatures[hoveredPlaceIndex].bounds[bound + 1][0] - z)*scale),
                        0xFFFF0000
                    );
                }
                // Draw one to connect it back up too
                if(shownFeatures[hoveredPlaceIndex].bounds.length == boundlength) MapRenderUtils.drawLine(context,
                    (int) ((shownFeatures[hoveredPlaceIndex].bounds[boundlength - 1][1] - x)*scale),
                    (int) ((shownFeatures[hoveredPlaceIndex].bounds[boundlength - 1][0] - z)*scale),
                    (int) ((shownFeatures[hoveredPlaceIndex].bounds[0][1] - x)*scale),
                    (int) ((shownFeatures[hoveredPlaceIndex].bounds[0][0] - z)*scale),
                    0xFFFF0000
                );
            } }
            catch(Exception e) {
                // I don't care, I'm losing it with the errors in this aaAAa-
            }

            // Draw places
            for (int i = 0; i < shownFeatures.length; i++) {
                FeatureFetch feature = shownFeatures[i];
                if (feature.featureType.equals("place")) {
                    context.pose().pushMatrix();
                    context.pose().translate((float) ((feature.x - x) * scale), (float) ((feature.z - z) * scale));
                    context.pose().translate(-4, -4);
                    context.blit(RenderPipelines.GUI_TEXTURED, pfu.getPlaceIcon(feature.type).get(), 0, 0, 0, 0, 8, 8, 8, 8);
                    context.pose().popMatrix();
                } else if (feature.featureType.equals("area")) {
                    context.pose().pushMatrix();
                    context.pose().translate((float) ((feature.x - x) * scale), (float) ((feature.z - z) * scale));
                    context.pose().scale((float) 0.5, (float) 0.5);
                    context.fill(-(minecraft.font.width(feature.name) / 2) - 2, -2,
                            (minecraft.font.width(feature.name) / 2) + 2, minecraft.font.lineHeight + 2, 0x80000000);
                    GraphicsHelper.drawCenteredString(context, minecraft.font, feature.name, 0, 0, 0xFFFFFFFF);
                    context.pose().popMatrix();
                } else if (feature.featureType.equals("portal")) {
                    context.pose().pushMatrix();
                    context.pose().translate((float) ((feature.x - x) * scale), (float) ((feature.z - z) * scale));
                    context.pose().translate(-4, -4);
                    context.blit(RenderPipelines.GUI_TEXTURED, pfu.getPortalIcon(feature.type).get(), 0, 0, 0, 0, 8, 8, 8, 8);
                    context.pose().popMatrix();
                }
            }

            PlayerFetch hoveredPlayer = null;
            // Draw players
            ArrayList<PlayerFetch> playersList = pfu.getPlayers();
            for (int i = 0; i < playersList.size(); i++) {
                PlayerFetch player = playersList.get(i);
                if (minecraft.player.getName() == Component.literal(player.name))
                    continue;
                // Calculate the player's Effective X/Z coordinates for the map's current dimension.
                // This must be done BEFORE checking if they are within the screen bounds (worldLeft/worldRight),
                // otherwise players in the Nether will be skipped when zoomed in on the Overworld.
                double effectiveX = player.x;
                double effectiveZ = player.z;

                if (!currentDimension.equals(player.world)) {
                    if (currentDimension.equals("minecraft_overworld")) {
                        effectiveX = player.x * 8;
                        effectiveZ = player.z * 8;
                    } else {
                        effectiveX = player.x / 8;
                        effectiveZ = player.z / 8;
                    }
                }

                if ((effectiveX > worldLeft && effectiveX < worldRight)
                        && (effectiveZ > worldTop && effectiveZ < worldBottom)) {
                    context.pose().pushMatrix();
                    float offsetFromLeft = (float) ((effectiveX - worldLeft) * scale);
                    float offsetFromTop = (float) ((effectiveZ - worldTop) * scale);
                    context.pose().translate(offsetFromLeft, offsetFromTop);

                    context.pose().rotate((float) Math.toRadians(player.yaw - 180));
                    context.pose().translate(-4, -4);
                    ResIdentifier playerMarkerChoice;
                    if(minecraft.player.getName().equals(Component.literal(player.name))) {
                        playerMarkerChoice = pfu.THIS_PLAYER;
                    } else {
                        switch (player.world) {
                            case "minecraft_overworld":
                                playerMarkerChoice = pfu.OTHER_PLAYERS_OW;
                                break;
                            case "minecraft_the_nether":
                            playerMarkerChoice = pfu.OTHER_PLAYERS_NETHER;
                            break;
                        default:
                            playerMarkerChoice = pfu.OTHER_PLAYERS_SOMEWHERE;
                            break;
                    }
                    }
                    context.blit(
                            RenderPipelines.GUI_TEXTURED,
                            playerMarkerChoice.get(),
                            0, 0, 0, 0, 8, 8, 8, 8);
                    context.pose().popMatrix();

                    if (mouseX > offsetFromLeft - 4 && mouseX < offsetFromLeft + 4 && mouseY > offsetFromTop - 4
                            && mouseY < offsetFromTop + 4) {
                        hoveredPlayer = player;
                    }
                }
            }


            MutableComponent coords = currentLocationCoordinates;
            if(coords == null) coords = Component.literal("--, --");
            context.fill(2, this.height - bottomMapOffset - minecraft.font.lineHeight - 4, 6 + minecraft.font.width(coords.getString()), this.height - bottomMapOffset - 1, 0xB0000000);
            GraphicsHelper.drawString(context, minecraft.font, currentLocationCoordinates, 4, this.height - bottomMapOffset - minecraft.font.lineHeight - 2, 0xFFFFFFFF);

            context.scissorStack.pop();

            // Tooltips
            if (hoveredPlayer != null) {
                // Players
                drawPlayerTooltip(context, hoveredPlayer, mouseX, mouseY);
            } else if (hoveredPlaceIndex != -1 && hoveredPlaceIndex < shownFeatures.length) {
                // Features
                String placeName = shownFeatures[hoveredPlaceIndex].name;
                String subText = shownFeatures[hoveredPlaceIndex].featureType.equals("portal") ?
                    pfu.getPortalPrettyName(shownFeatures[hoveredPlaceIndex].type) :
                    "Click to view details...";
                String placeId;
                // "Hell yeah, he uses switch/cases. +10 aura points"
                switch (shownFeatures[hoveredPlaceIndex].featureType) {
                    case "place":
                        placeId = "P" + shownFeatures[hoveredPlaceIndex].id;
                        break;
                    case "area":
                        placeId = "A" + shownFeatures[hoveredPlaceIndex].id;
                        break;
                    case "portal":
                        placeId = "SP" + shownFeatures[hoveredPlaceIndex].id;
                        break;
                    default:
                        placeId = "" + shownFeatures[hoveredPlaceIndex].id;
                        break;
                }
                MapRenderUtils.drawTooltipComponent(context, List.of(
                    Component.literal(placeName).append(Component.literal(" (" + placeId + ")").withStyle(ChatFormatting.GRAY)),
                    Component.literal(subText).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY),
                    Component.literal("Ctrl+Click to share Coords").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY)),
                    mouseX + 7, mouseY + 4);
            } else if(hoveredClaimIndex != -1 && hoveredClaimIndex < shownClaims.size()) {
                // And claims
                String claimHoverOwner = shownClaims.get(hoveredClaimIndex).popup.substring(32).replace("</span>", "");
                MapRenderUtils.drawTooltipComponent(context, List.of(
                    Component.literal("This claim is owned by:").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
                    Component.literal(claimHoverOwner.length() == 0 ? "Nobody (this is an admin claim!)" : claimHoverOwner)
                ), mouseX + 7, mouseY + 4);
            }

            // 387x50
            if (sponsorBanner != null) {
                context.blit(RenderPipelines.GUI_TEXTURED, sponsorBanner.get(), 3, this.height - 28, 0, 0, 193, 25, 193, 25);
                if (drawSponsorTooltip) {
                    MapRenderUtils.drawTooltipComponent(context, sponsorHoverText, mouseX, mouseY);
                }
            } else {
                GraphicsHelper.drawString(context, Minecraft.getInstance().font, "Loading banner...", 10, this.height - 30, 0xFFFFFFFF);
            }

            // Draw button builders
            //? if <26.1 {
            super.render(context, mouseX, mouseY, delta);
            //?} else {
            /*super.extractRenderState(context, mouseX, mouseY, delta);*///?}
            // Draw on top of buttons
            context.blit(RenderPipelines.GUI_TEXTURED, searchIcon.get(), this.width - 21, 9, 0, 0, 12, 12, 12, 12);
            context.blit(RenderPipelines.GUI_TEXTURED, settingsIcon.get(), this.width - 21, 34, 0, 0, 12, 12, 12, 12);
            context.blit(RenderPipelines.GUI_TEXTURED, compassIcon.get(), this.width - 21, this.height - bottomMapOffset - 21, 0, 0, 12, 12, 12, 12);

            // Zoom level number
            context.blit(RenderPipelines.GUI_TEXTURED,
                    ResIdentifier.of("minecraft", "textures/gui/sprites/widget/checkbox.png").get(),
                    5, 30, 0, 0, 20, 20, 20, 20);
            GraphicsHelper.drawCenteredString(context, minecraft.font, String.format("%d", zoomlevel), 15, 35, 0xFFFFFFFF);
        }
    }

    private void changeMapScale(int zoomDelta, double coordScale) {
        int oldtilesize = 1 << (17 - zoomlevel);
        double oldscale = (double) minimapTileSize / oldtilesize;
        double cx = x + (this.width / 2.0) / oldscale;
        double cz = z + (this.height / 2.0) / oldscale;

        cx *= coordScale;
        cz *= coordScale;
        zoomlevel = Math.max(minZoomLevel, Math.min(maxZoomLevel, zoomlevel + zoomDelta));

        int newtilesize = 1 << (17 - zoomlevel);
        double newscale = (double) minimapTileSize / newtilesize;
        x = (int) (cx - (this.width / 2.0) / newscale);
        z = (int) (cz - (this.height / 2.0) / newscale);
    }

    private static final String[] DIMENSION_CYCLE = {
        "minecraft_overworld",
        "minecraft_the_nether",
        "minecraft_terra2"
        // "minecraft_the_end" can be added here
    };

    private double getDimensionCoordinateScale(String dimension) {
        // Defines the coordinate scale relative to the Overworld
        return dimension.equals("minecraft_the_nether") ? 8.0 : 1.0;
    }

    private int getDimensionZoomOffset(String dimension) {
        // Defines the base zoom offset relative to the Overworld
        return dimension.equals("minecraft_the_nether") ? 3 : 0;
    }

    public String getDimensionID() {
        if(Minecraft.getInstance().level == null) return "minecraft_overworld";
        return "minecraft_" + CompatUtils.getIdentifier(Minecraft.getInstance().level.dimension()).getPath();
    }

    @Override
    protected void init() {
        currentDimension = "minecraft_" + CompatUtils.getIdentifier(Minecraft.getInstance().level.dimension()).getPath();
        Button negZoomBtn = Button.builder(Component.nullToEmpty("-"), (btn) -> {
            // Get current middle
            int oldtilesize = 1 << (17 - zoomlevel);
            double oldscale = (double) minimapTileSize / oldtilesize;
            double oldMiddleX = (this.width / 2) / oldscale;
            double oldMiddleZ = (this.height / 2) / oldscale;
            zoomlevel -= 1;
            if (zoomlevel < minZoomLevel)
                zoomlevel = minZoomLevel;
            // Set position to new x/z
            int newtilesize = 1 << (17 - zoomlevel);
            double newscale = (double) minimapTileSize / newtilesize;
            double newMiddleX = (this.width / 2) / newscale;
            double newMiddleZ = (this.height / 2) / newscale;
            x -= (newMiddleX - oldMiddleX);
            z -= (newMiddleZ - oldMiddleZ);
            onMouseMove(x, z);
            resetClaims();
        }).bounds(5, 55, 20, 20).tooltip(Tooltip.create(Component.literal("Zoom out"))).build();
        Button posZoomBtn = Button.builder(Component.nullToEmpty("+"), (btn) -> {
            // Get current middle
            int oldtilesize = 1 << (17 - zoomlevel);
            double oldscale = (double) minimapTileSize / oldtilesize;
            double oldMiddleX = (this.width / 2) / oldscale;
            double oldMiddleZ = (this.height / 2) / oldscale;
            zoomlevel += 1;
            if (zoomlevel > maxZoomLevel)
                zoomlevel = maxZoomLevel;
            // Set position to new x/z
            int newtilesize = 1 << (17 - zoomlevel);
            double newscale = (double) minimapTileSize / newtilesize;
            double newMiddleX = (this.width / 2) / newscale;
            double newMiddleZ = (this.height / 2) / newscale;
            x -= (newMiddleX - oldMiddleX);
            z -= (newMiddleZ - oldMiddleZ);
            onMouseMove(x, z);
            resetClaims();
        }).bounds(5, 5, 20, 20).tooltip(Tooltip.create(Component.literal("Zoom in"))).build();
        Button searchZoomBtn = Button.builder(Component.nullToEmpty(" "), (btn) -> {
            CompatUtils.setScreen(minecraft, new ChatScreen("/search ", false));
        }).bounds(this.width - 25, 5, 20, 20).tooltip(Tooltip.create(Component.literal("Search PVC Mapper"))).build();
        Button settingsBtn = Button.builder(Component.nullToEmpty(" "), (btn) -> {
            CompatUtils.setScreen(minecraft, ClothConfigScreen.createScreen(sp, this));
        }).bounds(this.width - 25, 30, 20, 20).tooltip(Tooltip.create(Component.literal("PVC Mapper Mod Settings"))).build();
        
        // Add sponsor banner code here
        SponsorUtils.getBannerAsync().thenAccept(banner -> {
            SponsorUtils.bannerToTexture(banner.imgurl, (rl) -> {
                this.sponsorBanner = rl;
            });
            sponsorHoverText = List.of(
                    Component.literal(banner.title).withStyle(ChatFormatting.BOLD)
                            .append(Component.literal(" (Click to view)").withStyle(ChatFormatting.GRAY)),
                    Component.literal(banner.description));
            sponsorURLString = banner.link;
        });

        int checkboxX = this.width - minecraft.font.width("Show Claims") - 25;
        Builder checkboxBuilder = Checkbox.builder(Component.literal("Show Claims"), minecraft.font);
        checkboxBuilder.onValueChange((checkbox, bl) -> resetClaims());
        claimsCheckbox = checkboxBuilder.pos(checkboxX, this.height - 25).build();
        this.addRenderableWidget(claimsCheckbox);

        Button dimensionButton = Button.builder(
            Component.literal(pfu.prettyDimensionName(currentDimension)).withStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Component.literal("Switch Dimension")))),
            (btn) -> {
                String oldDimension = currentDimension;
                
                // Find next dimension in cycle
                int nextIndex = 0;
                for (int i = 0; i < DIMENSION_CYCLE.length; i++) {
                    if (DIMENSION_CYCLE[i].equals(oldDimension)) {
                        nextIndex = (i + 1) % DIMENSION_CYCLE.length;
                        break;
                    }
                }
                currentDimension = DIMENSION_CYCLE[nextIndex];
                btn.setMessage(Component.literal(pfu.prettyDimensionName(currentDimension)));

                double coordScale = getDimensionCoordinateScale(oldDimension) / getDimensionCoordinateScale(currentDimension);
                int zoomDelta = getDimensionZoomOffset(currentDimension) - getDimensionZoomOffset(oldDimension);

                if (coordScale != 1.0 || zoomDelta != 0) {
                    // Shifting zoom by 3 steps exactly matches an 8x coordinate scale (2^3 = 8)
                    changeMapScale(zoomDelta, coordScale);
                }

                onMouseMove(x, z);
                resetFeatures();
            }
        ).bounds(
            this.width 
            - 25 
            - minecraft.font.width("Overworld") 
            - minecraft.font.width("Show Claims")
            - 10, this.height - 26, minecraft.font.width("Overworld") + 5, 20).build();
        this.addRenderableWidget(dimensionButton);

        Button goToMe = Button.builder(Component.literal(""), (btn) -> {
            int tilesize = 1 << (17 - zoomlevel);
            double scale = (double) minimapTileSize / tilesize;
            x = (int) (minecraft.player.getBlockX()-(this.width / scale) / 2);
            z = (int) (minecraft.player.getBlockZ()-(this.height / scale) / 2);
            onMouseMove(x, z);
            resetFeatures();

        }).tooltip(Tooltip.create(Component.literal("Go to Current Location"))).bounds(this.width - 25, this.height - bottomMapOffset - 25, 20, 20).build();
        this.addRenderableWidget(goToMe);

        if(sp.bigMapPos == BigMapPos.CENTRE_ON_SPAWN) {
            int tilesize = 1 << (17 - zoomlevel);
            double scale = (double) minimapTileSize / tilesize;
            x = (int) (-(this.width / scale) / 2);
            z = (int) (-(this.height / scale) / 2);
        } else if(sp.bigMapPos == BigMapPos.CENTRE_ON_PLAYER) {
            int tilesize = 1 << (17 - zoomlevel);
            double scale = (double) minimapTileSize / tilesize;
            x = (int) (minecraft.player.getBlockX()-(this.width / scale) / 2);
            z = (int) (minecraft.player.getBlockZ()-(this.height / scale) / 2);
        }
        onMouseMove(x, z);
        resetFeatures();

        this.addRenderableWidget(negZoomBtn);
        this.addRenderableWidget(posZoomBtn);
        this.addRenderableWidget(searchZoomBtn);
        this.addRenderableWidget(settingsBtn);

    }
}

