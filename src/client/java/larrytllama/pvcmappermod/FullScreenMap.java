package larrytllama.pvcmappermod;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
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

import org.lwjgl.glfw.GLFW;

import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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
    public ResourceLocation overlayImage;
    public String overlayImageStatus;
    public ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private boolean isMouseDown = false;
    public SettingsProvider sp;
    public static FullScreenMap createScreen(Component title, PlayerFetchUtils pfu, SettingsProvider sp) {
        FullScreenMap fsm = new FullScreenMap(title);
        fsm.pfu = pfu;
        fsm.sp = sp;
        fsm.zoomlevel = sp.miniMapZoom;
        return fsm;
    }

    public FullScreenMap(Component title) {
        super(title);
    }

    public void showToast(String title, String content) {
        minecraft.getToastManager().addToast(
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
                Minecraft.getInstance().setScreen(null);
                //this.onClose(); // closes the screen
            return true;
        }
        return super.keyPressed(keyEvent);
    }

    // Screen positions
    private int topLeftX = 0;
    private int topLeftZ = 0;
    public int zoomlevel = 8;
    public int maxZoomLevel = 8;
    public int minZoomLevel = 1;

    public int x = 0;
    public int z = 0;
    public int minimapTileSize = 120;

    Map<String, ResourceLocation> tiles = new HashMap<String, ResourceLocation>();

    public void resetTiles() {
        this.tiles = new HashMap<String, ResourceLocation>();
    }
    ResourceLocation blurredTile = ResourceLocation.fromNamespaceAndPath("pvcmappermod",
            "textures/gui/tileloading.png");

    private boolean drawSponsorTooltip = false;

    int lastMouseX = 0;
    int lastMouseY = 0;

    // On mouse move, we'll check for new tiles
    private void onMouseMove(int mouseX, int mouseY) {
        int tilesize = 1 << (17 - zoomlevel);
        double scale = (double) minimapTileSize / tilesize;
        double worldLeft = x;
        double worldTop = z;
        double worldRight = x + (this.width / scale);
        double worldBottom = z + (this.height / scale);
        String dimension = "" + currentDimension;
        // Figure out tile no. at top left/bottom right
        int topLeftTileX = Math.floorDiv((int) worldLeft, tilesize) - 1;
        int topLeftTileZ = Math.floorDiv((int) worldTop, tilesize) - 1;
        int bottomRightTileX = Math.floorDiv((int) worldRight, tilesize) + 1;
        int bottomRightTileZ = Math.floorDiv((int) worldBottom, tilesize) + 1;

        int thisZoomLevel = zoomlevel;
        for (int iX = topLeftTileX; iX < bottomRightTileX; iX++) {
            if(thisZoomLevel != zoomlevel && !currentDimension.equals(dimension)) break;
            for (int iZ = topLeftTileZ; iZ < bottomRightTileZ; iZ++) {
                if(thisZoomLevel != zoomlevel && !currentDimension.equals(dimension)) break;
                ResourceLocation tile = tiles.get(String.format("%s/%d/%d_%d", dimension, thisZoomLevel, iX, iZ));
                if (tile == null) {
                    final int tileX = iX;
                    final int tileZ = iZ;
                    // Temporarily set to a blurred tile to stop the repeating null
                    tiles.put(String.format("%s/%d/%d_%d", dimension, thisZoomLevel, tileX, tileZ), blurredTile);
                    // Make a request for the tile
                    String thisDimension = dimension.equals("minecraft_terra2") && sp.useDarkTiles ? "minecraft_terra2_night" : dimension;
                    String url = String.format("%s%s/%d/%d_%d.png",
                        sp.mapTileSource, thisDimension, zoomlevel, iX, iZ);
                    TextureUtils.fetchRemoteTexture(url, (id) -> {
                        tiles.put(String.format("%s/%d/%d_%d", dimension, thisZoomLevel, tileX, tileZ), id);
                    });
                }
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
                minecraft.getToastManager().addToast(new SystemToast(
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
            System.out.println("Checkbox selected! Finding claims...");
            shownClaims = pfu.getClaimsInBounds(currentDimension, x, (int) (x + (this.width / scale)), z,
                    (int) (z + ((this.height - bottomMapOffset) / scale)));
        } else {
            shownClaims = new ArrayList<ClaimMarkers>();
        }
    }

    public FeatureFetch[] shownFeatures = new FeatureFetch[0];

    private double getScale() {
            return 1 / Math.pow(2, 8);
    }

    private double metersToPixels(double num) {
        return Math.round(num / getScale());
    }

    private boolean isChangingFeatures = false;
    public void resetFeatures() {
        int tilesize = 1 << (17 - zoomlevel);
        double scale = (double) minimapTileSize / tilesize;
        CompletableFuture.runAsync(() -> {
            shownFeatures = pfu.fetchFeatures(
                    currentDimension,
                    x,
                    (int) (x + (this.width / scale)),
                    z,
                    (int) (z + ((this.height - bottomMapOffset) / scale)));
            isChangingFeatures = true;
            // Convert bounds to coordinate format because grr weird format
            for (int i = 0; i < shownFeatures.length; i++) {
                if(shownFeatures[i].featureType.equals("area")) {
                    if(shownFeatures[i].bounds == null) continue;
                    for (int bound = 0; bound < shownFeatures[i].bounds.length; bound++) {
                        shownFeatures[i].bounds[bound][0] = metersToPixels(shownFeatures[i].bounds[bound][0]);
                        shownFeatures[i].bounds[bound][1] = metersToPixels(shownFeatures[i].bounds[bound][1]);
                    }
                }
            }
            isChangingFeatures = false;
        });
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
            for (int i = 0; i < shownFeatures.length; i++) {
                if(shownFeatures[i].id == 1) {
                    System.out.println("X/Z: " + ((shownFeatures[i].x - x) * scale) + " / " + ((shownFeatures[i].z - z) * scale));
                }
                if(shownFeatures[i].featureType.equals("area")) {
                    int itemWidth = minecraft.font.width(shownFeatures[i].name);
                    if( mbe.x() > ((shownFeatures[i].x - x) * scale) - (itemWidth / 2) &&
                        mbe.x() < ((shownFeatures[i].x - x) * scale) + (itemWidth / 2) &&
                        mbe.y() > ((shownFeatures[i].z - z) * scale) - (minecraft.font.lineHeight / 2) &&
                        mbe.y() < ((shownFeatures[i].z - z) * scale) + (minecraft.font.lineHeight / 2)) {
                        if(mbe.hasControlDown()) {
                            minecraft.setScreen(new ChatScreen(String.format("%s: %d, %d in %s", shownFeatures[i].name, shownFeatures[i].x, shownFeatures[i].z, pfu.prettyDimensionName(currentDimension)), false));
                        } else {
                            System.out.println("Feature clicked: " + shownFeatures[i].id);
                            int index = i;
                            CompletableFuture.runAsync(() -> {
                                overlayFeature = pfu.fetchArea(shownFeatures[index].id);
                                overlayItemID = shownFeatures[index].id;
                                overlayItemType = "area";
                                overlayOpen = true;
                                overlayImage = null;
                                overlayImageStatus = "Loading...";
                                if(overlayFeature.area.image != null) {
                                    TextureUtils.fetchRemoteTexture(overlayFeature.area.image, (id) -> {
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
                                    minecraft.setScreen(new ChatScreen(String.format("%s: %d, %d in %s", shownFeatures[i].name, shownFeatures[i].x, shownFeatures[i].z, pfu.prettyDimensionName(currentDimension)), false));
                                } else {
                                    CompletableFuture.runAsync(() -> {
                                        overlayFeature = pfu.fetchPlace(shownFeatures[index].id);
                                        overlayItemID = shownFeatures[index].id;
                                        overlayItemType = "place";
                                        overlayOpen = true;
                                        overlayImage = null;
                                        overlayImageStatus = "Loading...";
                                        if(overlayFeature.place.images != null) {
                                            TextureUtils.fetchRemoteTexture(overlayFeature.place.images, (id) -> {
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
                                    minecraft.setScreen(new ChatScreen(String.format("%s: %d, %d in %s", pfu.getPortalPrettyName(shownFeatures[i].type), shownFeatures[i].x, shownFeatures[i].z, pfu.prettyDimensionName(currentDimension)), false));
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
        // Get current middle
        int oldtilesize = 1 << (17 - zoomlevel);
        double oldscale = (double) minimapTileSize / oldtilesize;
        double oldMiddleX = (this.width / 2) / oldscale;
        double oldMiddleZ = (this.height / 2) / oldscale;
        // Move to new zoom level
        zoomlevel += scroll;;
        zoomlevel = Math.max(minZoomLevel, Math.min(maxZoomLevel, zoomlevel));
        // Set position to new x/z
        int newtilesize = 1 << (17 - zoomlevel);
        double newscale = (double) minimapTileSize / newtilesize;
        double newMiddleX = (this.width / 2) / newscale;
        double newMiddleZ = (this.height / 2) / newscale;
        x -= (newMiddleX - oldMiddleX);
        z -= (newMiddleZ - oldMiddleZ);
        int newZoomLevel = zoomlevel;
        executor.schedule(() -> {
            if(newZoomLevel == zoomlevel) {
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
            for (int i = 0; i < shownFeatures.length; i++) {
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
                hoveredPlaceIndex = -1;
            }
            if(hoveredPlaceIndex == -1) {
                for (int i = 0; i<shownClaims.size(); i++) {
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
                    hoveredClaimIndex = -1;
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
            mc.setScreen(new ConfirmLinkScreen(confirmed -> {
                if (confirmed) {
                    Util.getPlatform().openUri(sponsorURLString);
                }
                mc.setScreen(null);
            }, sponsorURLString, true));
        }
        return super.mouseClicked(mbe, bl);
    }

    public void drawTooltip(GuiGraphics context, List<MutableComponent> content, int x, int y) {
        int lines = content.size();
        Font mcfont = Minecraft.getInstance().font;
        int maxSize = 0;
        for (int i = 0; i < lines; i++) {
            int w = mcfont.width(content.get(i));
            if (w > maxSize) {
                maxSize = w;
            }
        }
        TooltipRenderUtil.renderTooltipBackground(context, x, y, maxSize, mcfont.lineHeight * lines, null);
        for (int i = 0; i < lines; i++) {
            context.drawString(
                    mcfont,
                    content.get(i),
                    x,
                    y + (i * mcfont.lineHeight),
                    0xFFFFFFFF, false);
        }
    }

    private String hoverPlayerName;
    private ResourceLocation hoverPlayerFace = ResourceLocation.fromNamespaceAndPath("minecraft",
            "textures/entity/player/wide/steve.png");

    private void getTooltipPlayer(String uuid, String name) {
        Minecraft mc = Minecraft.getInstance();

        UUID dashed = UUID.fromString(
                uuid.substring(0, 8) + "-" +
                        uuid.substring(8, 12) + "-" +
                        uuid.substring(12, 16) + "-" +
                        uuid.substring(16, 20) + "-" +
                        uuid.substring(20));
        ResolvableProfile resolvable = ResolvableProfile.createUnresolved(dashed);
        resolvable.resolveProfile(Minecraft.getInstance().services().profileResolver()).thenAccept((resolvedProfile) -> {
            CompletableFuture<Optional<PlayerSkin>> skin = mc.getSkinManager().get(resolvedProfile);
            skin.thenAccept(playerSkin -> {
                // Fallback to steeeeeeeeeve
                if (playerSkin.isEmpty()) hoverPlayerFace = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");
                else hoverPlayerFace = playerSkin.get().body().texturePath();
            });
        });
        
    }

    public void drawPlayerTooltip(GuiGraphics context, PlayerFetch player, int x, int y) {

        List<MutableComponent> content = List.of(
                Component.literal(player.name).withStyle(ChatFormatting.BOLD),
                Component.literal(player.x + ", " + player.z + " - " + pfu.prettyDimensionName(player.world)),
                Component.literal("Health: " + (player.health / 2) + "/10 - Armor: " + (player.armor / 2) + "/10"));
        drawTooltip(context, content, x + TooltipRenderUtil.PADDING_LEFT, y + TooltipRenderUtil.PADDING_TOP);
        if (hoverPlayerName != player.name) {
            getTooltipPlayer(player.uuid, player.name);
            hoverPlayerName = player.name;
        }

        context.blit(RenderPipelines.GUI_TEXTURED, hoverPlayerFace,
                x + TooltipRenderUtil.PADDING_LEFT + minecraft.font.width(content.get(0)) + 1,
                y + TooltipRenderUtil.PADDING_TOP, 8, 8, 8, 8, 64, 64);
        context.blit(RenderPipelines.GUI_TEXTURED, hoverPlayerFace,
                x + TooltipRenderUtil.PADDING_LEFT + minecraft.font.width(content.get(0)) + 1,
                y + TooltipRenderUtil.PADDING_TOP, 40, 8, 8, 8, 64, 64);
    }

    // Fills in each pixel x0/y0 - x1/y1 
    public static void drawLine(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            g.fill(x0, y0, x0 + 1, y0 + 1, color);
            if (x0 == x1 && y0 == y1) break;
            int e2 = err * 2;
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx)  { err += dx; y0 += sy; }
        }
    }

    private int bottomMapOffset = 31;

    public ResourceLocation sponsorBanner;
    public List<MutableComponent> sponsorHoverText;
    public String sponsorURLString;

    private final ResourceLocation searchIcon = ResourceLocation.fromNamespaceAndPath("minecraft",
            "textures/gui/sprites/icon/search.png");
    private final ResourceLocation settingsIcon = ResourceLocation.fromNamespaceAndPath("pvcmappermod",
            "textures/gui/settings.png");
    private final ResourceLocation compassIcon = ResourceLocation.fromNamespaceAndPath("minecraft",
            "textures/item/compass_19.png");

    private final ResourceLocation OVERWORLD = ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");
    private final ResourceLocation NETHER = ResourceLocation.fromNamespaceAndPath("minecraft", "the_nether");
    // Heh, eng
    private final ResourceLocation ENG = ResourceLocation.fromNamespaceAndPath("minecraft", "the_end");

    public String currentDimension = getDimensionID();

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        if (overlayOpen && overlayFeature != null) {
            context.drawString(minecraft.font, Component.literal("Press ESC to go back").withStyle(ChatFormatting.ITALIC), 5, 5, 0xFF555555);
            if(overlayItemType.equals("place")) {
                // Title
                context.drawCenteredString(minecraft.font, Component.literal(overlayFeature.place.name).withStyle(ChatFormatting.BOLD), this.width / 2, 40, 0xFFFFFFFF);
                // Dividing line
                context.vLine(this.width / 2, 60, this.height - 20, 0xFF636363);
                // Description
                List<FormattedCharSequence> lines = minecraft.font.split(Component.literal(overlayFeature.place.description), (this.width / 2) - 20 );
                int descHeight = lines.size() * minecraft.font.lineHeight;
                for (int i = 0; i < lines.size(); i++) {
                    context.drawString(minecraft.font, lines.get(i), (this.width / 2) + 10, 63 + (i * minecraft.font.lineHeight), 0xFFFFFFFF);
                }
                
                // Location
                context.blit(RenderPipelines.GUI_TEXTURED, ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/sprites/icon/link.png"), (this.width/2) + 8, 66 + descHeight, 0, 0, 12, 12, 12, 12);
                context.drawString(minecraft.font, String.format("%s, %s in %s", overlayFeature.place.x, overlayFeature.place.z, pfu.prettyDimensionName(overlayFeature.place.dimension)), (this.width / 2) + 22, 68 + descHeight, 0xFFFFFFFF);
            
                // Added by
                context.blit(RenderPipelines.GUI_TEXTURED, ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/sprites/icon/accessibility.png"), (this.width/2) + 8, 72 + descHeight + minecraft.font.lineHeight, 0, 0, 12, 12, 12, 12);
                context.drawString(minecraft.font, String.format("Added by %s", overlayFeature.place.addedBy.name), (this.width / 2) + 22, 74 + descHeight + minecraft.font.lineHeight, 0xFFFFFFFF);

                // Wiki Link
                if(overlayFeature.place.wiki != null && overlayFeature.place.wiki.length() > 1) {
                    context.blit(RenderPipelines.GUI_TEXTURED, ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/sprites/toast/social_interactions.png"), (this.width/2) + 8, 76 + descHeight + (minecraft.font.lineHeight * 2), 0, 0, 12, 12, 12, 12);
                    context.drawString(minecraft.font, overlayFeature.place.wiki, (this.width / 2) + 22, 78 + descHeight + (minecraft.font.lineHeight * 2), 0xFFFFFFFF);
                }

                int currentHeight = 83 + descHeight + (minecraft.font.lineHeight * 3);

                // Features
                if(overlayFeature.place.features != null) {
                    if(overlayFeature.place.features.Public) {
                        context.blit(RenderPipelines.GUI_TEXTURED, ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/sprites/icon/new_realm.png"), (this.width/2) + 8, currentHeight, 0, 0, 24, 12, 24, 12);
                        context.drawString(minecraft.font, "Made for public use", (this.width / 2) + 32, currentHeight + 2, 0xFFFFFFFF);
                        currentHeight += minecraft.font.lineHeight + 5;
                    }
                    if(overlayFeature.place.features.echest) {
                        context.blit(RenderPipelines.GUI_TEXTURED, ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/ender_eye.png"), (this.width/2) + 14, currentHeight, 0, 0, 12, 12, 12, 12);
                        context.drawString(minecraft.font, "Has ender chest access nearby", (this.width / 2) + 32, currentHeight + 2, 0xFFFFFFFF);
                        currentHeight += minecraft.font.lineHeight + 5;
                    }
                    if(overlayFeature.place.features.portal) {
                        context.blit(RenderPipelines.GUI_TEXTURED, ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/nether_portal.png"), (this.width/2) + 14, currentHeight, 0, 0, 12, 12, 12, 384);
                        context.drawString(minecraft.font, "Has nether portal access nearby", (this.width / 2) + 32, currentHeight + 2, 0xFFFFFFFF);
                        currentHeight += minecraft.font.lineHeight + 5;
                    }
                    if(overlayFeature.place.features.historical) {
                        context.blit(RenderPipelines.GUI_TEXTURED, ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/clock_00.png"), (this.width/2) + 14, currentHeight, 0, 0, 12, 12, 12, 12);
                        context.drawString(minecraft.font, "Historical Place", (this.width / 2) + 32, currentHeight + 2, 0xFFFFFFFF);
                        currentHeight += minecraft.font.lineHeight + 5;
                    }
                }

                context.fill(this.width / 6, 63, (this.width / 2) - 10, 183, 0x70000000);

                if(overlayImage != null) {
                    context.blit(RenderPipelines.GUI_TEXTURED, overlayImage, this.width / 6, 63, 0, 0, (this.width / 2) - (this.width / 6) - 10, 120, (this.width / 2) - (this.width / 6) - 10, 120);
                } else {
                    context.drawCenteredString(minecraft.font, overlayImageStatus, (((this.width / 2) - 10 - (this.width / 6)) / 2) + (this.width / 6), 121, 0xFFFFFFFF);
                }


            } else if(overlayItemType.equals("area")) {
                // Title
                context.drawCenteredString(minecraft.font, Component.literal(overlayFeature.area.name).withStyle(ChatFormatting.BOLD), this.width / 2, 40, 0xFFFFFFFF);
                // Dividing line
                context.vLine(this.width / 2, 60, this.height - 20, 0xFF636363);
                // Description
                List<FormattedCharSequence> lines = minecraft.font.split(Component.literal(overlayFeature.area.description), (this.width / 2) - 20 );
                int descHeight = lines.size() * minecraft.font.lineHeight;
                for (int i = 0; i < lines.size(); i++) {
                    context.drawString(minecraft.font, lines.get(i), (this.width / 2) + 10, 63 + (i * minecraft.font.lineHeight), 0xFFFFFFFF);
                }
                
                // Location
                context.blit(RenderPipelines.GUI_TEXTURED, ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/sprites/icon/link.png"), (this.width/2) + 8, 66 + descHeight, 0, 0, 12, 12, 12, 12);
                context.drawString(minecraft.font, String.format("%s, %s in %s", overlayFeature.area.x, overlayFeature.area.z, pfu.prettyDimensionName(overlayFeature.area.dimension)), (this.width / 2) + 22, 68 + descHeight, 0xFFFFFFFF);
            
                // Added by
                context.blit(RenderPipelines.GUI_TEXTURED, ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/sprites/icon/accessibility.png"), (this.width/2) + 8, 72 + descHeight + minecraft.font.lineHeight, 0, 0, 12, 12, 12, 12);
                context.drawString(minecraft.font, String.format("Added by %s", overlayFeature.area.addedBy.username), (this.width / 2) + 22, 74 + descHeight + minecraft.font.lineHeight, 0xFFFFFFFF);

                // Wiki Link
                if(overlayFeature.area.wiki != null && overlayFeature.area.wiki.length() > 1) {
                    context.blit(RenderPipelines.GUI_TEXTURED, ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/sprites/toast/social_interactions.png"), (this.width/2) + 8, 76 + descHeight + (minecraft.font.lineHeight * 2), 0, 0, 12, 12, 12, 12);
                    context.drawString(minecraft.font, overlayFeature.area.wiki, (this.width / 2) + 22, 78 + descHeight + (minecraft.font.lineHeight * 2), 0xFFFFFFFF);
                }

                context.fill(this.width / 6, 63, (this.width / 2) - 10, 183, 0x70000000);

                if(overlayImage != null) {
                    context.blit(RenderPipelines.GUI_TEXTURED, overlayImage, this.width / 6, 63, 0, 0, (this.width / 2) - (this.width / 6) - 10, 120, (this.width / 2) - (this.width / 6) - 10, 120);
                } else {
                    context.drawCenteredString(minecraft.font, overlayImageStatus, (((this.width / 2) - 10 - (this.width / 6)) / 2) + (this.width / 6), 121, 0xFFFFFFFF);
                }
            }
        } else {
            int tilesize = 1 << (17 - zoomlevel);
            double scale = (double) minimapTileSize / tilesize;
            double worldLeft = x;
            double worldTop = z;
            double worldRight = x + (this.width / scale);
            double worldBottom = z + ((this.height - bottomMapOffset) / scale);

            // Figure out tile no. at top left/bottom right
            int topLeftTileX = Math.floorDiv((int) worldLeft, tilesize);
            int topLeftTileZ = Math.floorDiv((int) worldTop, tilesize);
            int bottomRightTileX = Math.floorDiv((int) worldRight, tilesize) + 1;
            int bottomRightTileZ = Math.floorDiv((int) worldBottom, tilesize) + 1;

            // Iterate thru visible tiles
            double xOffsetFromTileStart = Math.floorMod((int) worldLeft, tilesize);
            double zOffsetFromTileStart = Math.floorMod((int) worldTop, tilesize);
            context.scissorStack.push(new ScreenRectangle(0, 0, this.width, this.height - bottomMapOffset));
            for (int iX = topLeftTileX; iX < bottomRightTileX; iX++) {
                for (int iZ = topLeftTileZ; iZ < bottomRightTileZ; iZ++) {
                    ResourceLocation tile = tiles.get(String.format("%s/%d/%d_%d", currentDimension, zoomlevel, iX, iZ));
                    if (tile == null)
                        continue;
                    context.pose().pushMatrix();
                    context.pose().translate(
                            (float) ((topLeftX - (xOffsetFromTileStart * scale)) + // The top left GUI pos - how far
                                                                                   // from the start of the tile
                                    ((iX - topLeftTileX) * minimapTileSize)), // + How many tiles along we are
                            (float) ((topLeftZ - (zOffsetFromTileStart * scale)) + // All the same
                                    ((iZ - topLeftTileZ) * minimapTileSize)) // For the Z axis
                    );
                    context.blit(
                            RenderPipelines.GUI_TEXTURED, tile, // Render tile with the following x pos:
                            0, 0,
                            0, 0, // u/v
                            minimapTileSize, minimapTileSize, // width/height
                            minimapTileSize, minimapTileSize // texturewidth/textureheight
                    );
                    context.pose().popMatrix();
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
                    context.vLine(0, 0, height, outlineColor);
                    // Right
                    context.vLine(width, 0, height, outlineColor);
                    // Top
                    context.hLine(0, width, 0, outlineColor);
                    // Bottom
                    context.hLine(0, width, height, outlineColor);
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
                    ) drawLine(context,
                        (int) ((shownFeatures[hoveredPlaceIndex].bounds[bound][1] - x)*scale),
                        (int) ((shownFeatures[hoveredPlaceIndex].bounds[bound][0] - z)*scale),
                        (int) ((shownFeatures[hoveredPlaceIndex].bounds[bound + 1][1] - x)*scale),
                        (int) ((shownFeatures[hoveredPlaceIndex].bounds[bound + 1][0] - z)*scale),
                        0xFFFF0000
                    );
                }
                // Draw one to connect it back up too
                if(shownFeatures[hoveredPlaceIndex].bounds.length == boundlength) drawLine(context,
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
                    context.blit(RenderPipelines.GUI_TEXTURED, pfu.getPlaceIcon(feature.type), 0, 0, 0, 0, 8, 8, 8, 8);
                    context.pose().popMatrix();
                } else if (feature.featureType.equals("area")) {
                    context.pose().pushMatrix();
                    context.pose().translate((float) ((feature.x - x) * scale), (float) ((feature.z - z) * scale));
                    context.pose().scale((float) 0.5, (float) 0.5);
                    context.fill(-(minecraft.font.width(feature.name) / 2) - 2, -2,
                            (minecraft.font.width(feature.name) / 2) + 2, minecraft.font.lineHeight + 2, 0x80000000);
                    context.drawCenteredString(minecraft.font, feature.name, 0, 0, 0xFFFFFFFF);
                    context.pose().popMatrix();
                } else if (feature.featureType.equals("portal")) {
                    context.pose().pushMatrix();
                    context.pose().translate((float) ((feature.x - x) * scale), (float) ((feature.z - z) * scale));
                    context.pose().translate(-4, -4);
                    context.blit(RenderPipelines.GUI_TEXTURED, pfu.getPortalIcon(feature.type), 0, 0, 0, 0, 8, 8, 8, 8);
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
                if ((player.x > worldLeft && player.x < worldRight)
                        && (player.z > worldTop && player.z < worldBottom)) {
                    context.pose().pushMatrix();
                    float offsetFromLeft = (float) ((player.x - worldLeft) * scale);
                    float offsetFromTop = (float) ((player.z - worldTop) * scale);
                    if(currentDimension.equals(player.world)) {
                        context.pose().translate(offsetFromLeft, offsetFromTop);
                    } else {
                        if(currentDimension.equals("minecraft_overworld")) {
                            offsetFromLeft = (float) ((player.x*8 - worldLeft) * scale);
                            offsetFromTop = (float) ((player.z*8 - worldTop) * scale);
                            context.pose().translate(offsetFromLeft, offsetFromTop);
                            //context.pose().translate(-4, -4);
                        } else {
                            offsetFromLeft = (float) ((player.x/8 - worldLeft) * scale);
                            offsetFromTop = (float) ((player.z/8 - worldTop) * scale);
                            context.pose().translate(offsetFromLeft, offsetFromTop);
                            //context.pose().translate(-4, -4);
                        }
                    }

                    context.pose().rotate((float) Math.toRadians(-player.yaw));
                    context.pose().translate(-4, -4);
                    ResourceLocation playerMarkerChoice;
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

                    context.blit(
                            RenderPipelines.GUI_TEXTURED,
                            playerMarkerChoice,
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
            context.drawString(minecraft.font, currentLocationCoordinates, 4, this.height - bottomMapOffset - minecraft.font.lineHeight - 2, 0xFFFFFFFF);

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
                drawTooltip(context, List.of(
                    Component.literal(placeName).append(Component.literal(" (" + placeId + ")").withStyle(ChatFormatting.GRAY)),
                    Component.literal(subText).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY),
                    Component.literal("Ctrl+Click to share Coords").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY)),
                    mouseX + 7, mouseY + 4);
            } else if(hoveredClaimIndex != -1 && hoveredClaimIndex < shownClaims.size()) {
                // And claims
                String claimHoverOwner = shownClaims.get(hoveredClaimIndex).popup.substring(32).replace("</span>", "");
                drawTooltip(context, List.of(
                    Component.literal("This claim is owned by:").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
                    Component.literal(claimHoverOwner.length() == 0 ? "Nobody (this is an admin claim!)" : claimHoverOwner)
                ), mouseX + 7, mouseY + 4);
            }

            // 387x50
            if (sponsorBanner != null) {
                context.blit(RenderPipelines.GUI_TEXTURED, sponsorBanner, 3, this.height - 28, 0, 0, 193, 25, 193, 25);
                if (drawSponsorTooltip) {
                    drawTooltip(context, sponsorHoverText, mouseX, mouseY);
                }
            } else {
                context.drawString(Minecraft.getInstance().font, "Loading banner...", 10, this.height - 30, 0xFFFFFFFF);
            }

            // Draw button builders
            super.render(context, mouseX, mouseY, delta);
            // Draw on top of buttons
            context.blit(RenderPipelines.GUI_TEXTURED, searchIcon, this.width - 21, 9, 0, 0, 12, 12, 12, 12);
            context.blit(RenderPipelines.GUI_TEXTURED, settingsIcon, this.width - 21, 34, 0, 0, 12, 12, 12, 12);
            context.blit(RenderPipelines.GUI_TEXTURED, compassIcon, this.width - 21, this.height - bottomMapOffset - 21, 0, 0, 12, 12, 12, 12);

            // Zoom level number
            context.blit(RenderPipelines.GUI_TEXTURED,
                    ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/sprites/widget/checkbox.png"),
                    5, 30, 0, 0, 20, 20, 20, 20);
            context.drawCenteredString(minecraft.font, String.format("%d", zoomlevel), 15, 35, 0xFFFFFFFF);
        }
    }

    public String getDimensionID() {
        if(Minecraft.getInstance().level == null) return "minecraft_overworld";
        return "minecraft_" + Minecraft.getInstance().level.dimension().location().getPath();
    }

    @Override
    protected void init() {
        currentDimension = "minecraft_" + Minecraft.getInstance().level.dimension().location().getPath();
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
            minecraft.setScreen(new ChatScreen("/search ", false));
        }).bounds(this.width - 25, 5, 20, 20).tooltip(Tooltip.create(Component.literal("Search PVC Mapper"))).build();
        Button settingsBtn = Button.builder(Component.nullToEmpty(" "), (btn) -> {
            minecraft.setScreen(ClothConfigScreen.createScreen(sp, this));
        }).bounds(this.width - 25, 30, 20, 20).tooltip(Tooltip.create(Component.literal("PVC Mapper Mod Settings"))).build();
        
        // Add sponsor banner code here
        CompletableFuture.runAsync(() -> {
            SponsorBanner banner = SponsorUtils.getBanner();
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
                int tilesize = 1 << (17 - zoomlevel);
                double scale = (double) minimapTileSize / tilesize;
                if(currentDimension.equals("minecraft_overworld")) {
                    currentDimension = "minecraft_the_nether";
                    // Going from OW to N
                    x = (int)(x - ((this.width / 2)*scale));
                    z = (int)(z - ((this.height / 2)*scale));
                    btn.setMessage(Component.literal("Nether"));
                } else if(currentDimension.equals("minecraft_the_nether")) {
                    // Going from N to OW
                    x = (int)(x - ((this.width / 2)*scale));
                    z = (int)(z - ((this.height / 2)*scale));
                    currentDimension = "minecraft_terra2";
                    btn.setMessage(Component.literal("Terra2"));
                } else if(currentDimension.equals("minecraft_terra2")) {
                    currentDimension = "minecraft_overworld";
                    btn.setMessage(Component.literal("Overworld"));
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
