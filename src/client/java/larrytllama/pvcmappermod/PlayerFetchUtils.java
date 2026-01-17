package larrytllama.pvcmappermod;

import java.io.FileNotFoundException;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class PlayerFetchUtils {
    private ScheduledExecutorService scheduler;
    private ArrayList<PlayerFetch> playersList = new ArrayList<PlayerFetch>();
    private ClaimFetch claimsList;
    private ClaimFetch netherClaimsList;
    public boolean claimsFetched = false;
    public final ResourceLocation OTHER_PLAYERS_OW = ResourceLocation.fromNamespaceAndPath("minecraft",
            "textures/map/decorations/frame.png");
    public final ResourceLocation OTHER_PLAYERS_NETHER = ResourceLocation.fromNamespaceAndPath("minecraft",
            "textures/map/decorations/red_marker.png");
    public final ResourceLocation OTHER_PLAYERS_SOMEWHERE = ResourceLocation.fromNamespaceAndPath("minecraft",
            "textures/map/decorations/blue_marker.png");

    public void showToast(String title, String content) {
        Minecraft.getInstance().getToastManager().addToast(
                new SystemToast(SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                    Component.literal(title),
                    Component.literal(content)
            )
        );
    }

    private int errorCount;

    public void startUpdates() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(
                () -> {
                    // Prevent loading when not needed
                    if (Minecraft.getInstance().level == null) {
                        stopUpdates();
                        return;
                    }
                    try (Scanner scanner = new Scanner(
                            new URI("https://pvc.coolwebsite.uk/api/v1/players").toURL().openStream(), "UTF-8")) {
                        String out = scanner.useDelimiter("\\A").next();
                        Gson gson = new Gson();
                        playersList = new ArrayList<PlayerFetch>(
                                Arrays.asList(gson.fromJson(out, PlayerFetch[].class)));
                        errorCount = 0;
                    } catch (Exception e) {
                        errorCount += 1;
                        showToast("PVC Mapper Error", "Couldn't connect to PVC Mapper. Relog to retry live tracking.");
                        stopUpdates();
                        System.out.println("Failed to fetch players from PVC Mapper!");
                        System.out.println(e);
                    }
                },
                0,
                1500, // Every 1.5s, the interval between updates on the mapper
                TimeUnit.MILLISECONDS);

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            stopUpdates();
        });

        // Hoy in the claims too because why not
        CompletableFuture.runAsync(() -> fetchClaims());
    }

    public void stopUpdates() {
        scheduler.shutdownNow();
    }

    public ArrayList<PlayerFetch> getPlayers() {
        return playersList;
    }

    public ClaimMarkers[] getClaims() {
        return claimsList.markers;
    }

    public ArrayList<ClaimMarkers> getClaimsInBounds(String dimension, int minX, int maxX, int minZ, int maxZ) {
        ArrayList<ClaimMarkers> claimsInBounds = new ArrayList<ClaimMarkers>();
        if(dimension.equals("minecraft_overworld")) {
            if (claimsList == null) return claimsInBounds;
            for (int i = 0; i < claimsList.markers.length; i++) {
                if (claimsList.markers[i].points == null) continue;
                if (claimsList.markers[i].type.equals("rectangle")) {
                    for (int i2 = 0; i2 < claimsList.markers[i].points.length; i2++) {
                        if (claimsList.markers[i].points[i2].x > minX &&
                                claimsList.markers[i].points[i2].x < maxX &&
                                claimsList.markers[i].points[i2].z > minZ &&
                                claimsList.markers[i].points[i2].z < maxZ) {
                                    claimsInBounds.add(claimsList.markers[i]);
                                    break;
                                }
                    }
                } else if(claimsList.markers[i].type.equals("polygon")) {
                    for (int i2 = 0; i2 < claimsList.markers[i].polygonPoints.length; i2++) {
                        for (int i3 = 0; i3 < claimsList.markers[i].polygonPoints[i2].length; i3++) {
                        if (claimsList.markers[i].polygonPoints[i2][i3].x > minX &&
                                claimsList.markers[i].polygonPoints[i2][i3].x < maxX &&
                                claimsList.markers[i].polygonPoints[i2][i3].z > minZ &&
                                claimsList.markers[i].polygonPoints[i2][i3].z < maxZ) {
                                    claimsInBounds.add(claimsList.markers[i]);
                                    break;
                                }
                        }
                    }
                } else {
                    System.out.println("Unknown claim type: " + claimsList.markers[i].type);
                }
            }
        } else if(dimension.equals("minecraft_the_nether")) {
            if (netherClaimsList == null) { 
                System.out.println("No nether claims! :O");
                return claimsInBounds;
            }
            for (int i = 0; i < netherClaimsList.markers.length; i++) {
                if (netherClaimsList.markers[i].points == null) continue;
                if (netherClaimsList.markers[i].type.equals("rectangle")) {
                    for (int i2 = 0; i2 < netherClaimsList.markers[i].points.length; i2++) {
                        if (netherClaimsList.markers[i].points[i2].x > minX &&
                                netherClaimsList.markers[i].points[i2].x < maxX &&
                                netherClaimsList.markers[i].points[i2].z > minZ &&
                                netherClaimsList.markers[i].points[i2].z < maxZ) {
                                    claimsInBounds.add(netherClaimsList.markers[i]);
                                    break;
                                }
                    }
                } else if(netherClaimsList.markers[i].type.equals("polygon")) {
                    for (int i2 = 0; i2 < netherClaimsList.markers[i].polygonPoints.length; i2++) {
                        for (int i3 = 0; i3 < netherClaimsList.markers[i].polygonPoints[i2].length; i3++) {
                        if (netherClaimsList.markers[i].polygonPoints[i2][i3].x > minX &&
                                netherClaimsList.markers[i].polygonPoints[i2][i3].x < maxX &&
                                netherClaimsList.markers[i].polygonPoints[i2][i3].z > minZ &&
                                netherClaimsList.markers[i].polygonPoints[i2][i3].z < maxZ) {
                                    claimsInBounds.add(netherClaimsList.markers[i]);
                                    break;
                                }
                        }
                    }
                } else {
                    System.out.println("Unknown claim type: " + netherClaimsList.markers[i].type);
                }
            }
        } else {
            System.out.println("Unknown claimed dimension: " + dimension);
        }
        return claimsInBounds;
    }

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

    public void fetchClaims() {
        System.out.println("Fetching claims from https://pvc.coolwebsite.uk/api/v1/claims");
        try (Scanner scanner = new Scanner(new URI("https://pvc.coolwebsite.uk/api/v1/claims").toURL().openStream(),
                "UTF-8")) {
            String out = scanner.useDelimiter("\\A").next();
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(ClaimFetch.class, new ClaimMarkerDeserialize())
                    .create();
            claimsList = gson.fromJson(out, ClaimFetch.class);
        } catch (Exception e) {
            //showToast("Mapper Connect Error", "Check your internet connection?");
            System.out.println("Failed to fetch claims from PVC Mapper!");
            System.out.println(e);
        }
        // Get nether claims too!
        try (Scanner scanner = new Scanner(new URI("https://pvc.coolwebsite.uk/api/v1/claims/nether").toURL().openStream(),
                "UTF-8")) {
            String out = scanner.useDelimiter("\\A").next();
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(ClaimFetch.class, new ClaimMarkerDeserialize())
                    .create();
            netherClaimsList = gson.fromJson(out, ClaimFetch.class);
        } catch (Exception e) {
            //showToast("Mapper Connect Error", "Check your internet connection?");
            System.out.println("Failed to fetch claims from PVC Mapper!");
            System.out.println(e);
        }
    }

    public FeatureFetch[] fetchFeatures(String dimension, int x1, int x2, int z1, int z2) {
        String url = String.format("https://pvc.coolwebsite.uk/api/v2/fetch/things/%s/%d/%d/%d/%d", dimension, x1, x2, z1, z2);
        System.out.println("Fetching all features in area from PVC Mapper.");
        try (Scanner scanner = new Scanner(new URI(url).toURL().openStream(),
                "UTF-8")) {
            String out = scanner.useDelimiter("\\A").next();
            Gson gson = new Gson();
            return gson.fromJson(out, FeatureFetch[].class);
        } catch (Exception e) {
            showToast("Mapper Connect Error", "Check your internet connection?");
            System.out.println("Failed to fetch features from PVC Mapper!");
            System.out.println(e);
            return new FeatureFetch[0];
        }
    }

    private final ResourceLocation SHOP_BANNER = ResourceLocation.fromNamespaceAndPath("minecraft",
            "textures/map/decorations/orange_banner.png");
    private final ResourceLocation EVENT_BANNER = ResourceLocation.fromNamespaceAndPath("minecraft",
            "textures/map/decorations/magenta_banner.png");
    private final ResourceLocation LANDMARK_BANNER = ResourceLocation.fromNamespaceAndPath("minecraft",
            "textures/map/decorations/yellow_banner.png");
    private final ResourceLocation BASE_BANNER = ResourceLocation.fromNamespaceAndPath("minecraft",
            "textures/map/decorations/light_blue_banner.png");
    private final ResourceLocation GRAY_BANNER = ResourceLocation.fromNamespaceAndPath("minecraft",
            "textures/map/decorations/light_gray_banner.png");
    public ResourceLocation getPlaceIcon(String placeType) {
        switch (placeType) {
            case "farm":
            case "landmark":
            case "museum":
            case "lighthouse":
                return LANDMARK_BANNER;
            case "shop":
            case "mall":
                return SHOP_BANNER;
            case "union":
            case "base":
            case "town":
                return BASE_BANNER;
            case "event":
            case "pvp":
                return EVENT_BANNER;
            default:
                return GRAY_BANNER;
        }
    }

    private final ResourceLocation NETHER_PORTAL =  ResourceLocation.fromNamespaceAndPath("pvcmappermod","textures/gui/portal.png");
    private final ResourceLocation ENDER_CHEST = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/ender_eye.png");
    private final ResourceLocation TERRA2_PORTAL = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/sprites/icon/new_realm.png");
    private final ResourceLocation UNKNOWN_FEATURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/sprites/icon/unseen_notification.png");

    public ResourceLocation getPortalIcon(String portalType) {
        switch (portalType) {
            case "portal":
                return NETHER_PORTAL;
            case "portalandechest":
                return NETHER_PORTAL;
            case "echest":
                return ENDER_CHEST;
            case "terra2portal":
                return TERRA2_PORTAL;
            default:
                return UNKNOWN_FEATURE;
        }
    }

    public String getPortalPrettyName(String portalType) {
        switch (portalType) {
            case "portal":
                return "Nether Portal";
            case "portalandechest":
                return "Nether Portal with Ender Chest";
            case "echest":
                return "Ender Chest";
            case "terra2portal":
                return "Terra2 Portal";
            default:
                return "";
        }
    }

    public FeatureTypes fetchPlace(int placeId) {
        FeatureTypes ft = new FeatureTypes();
        System.out.println("Fetching place with ID: " + placeId);
        try (Scanner scanner = new Scanner(
            new URI("https://pvc.coolwebsite.uk/api/v1/fetch/places/byid/" + placeId).toURL().openStream(), "UTF-8")) {
            String out = scanner.useDelimiter("\\A").next();
            Gson gson = new Gson();
            ft.place = gson.fromJson(out, PlacesFetch.class);
            return ft;
        } catch (FileNotFoundException e) {
            showToast("Place not found!", "The place may have just been deleted.");
            return ft;
        } catch (Exception e) {
            showToast("Mapper Connect Error", "Check your connection and try again later!");
            System.out.println("Failed to fetch places from PVC Mapper!");
            System.out.println(e);
            return ft;
        }
    }

    private double getScale() {
            return 1 / Math.pow(2, 8);
    }

    private double metersToPixels(double num) {
        return Math.round(num / getScale());
    }

    public FeatureTypes fetchArea(int placeId) {
        FeatureTypes ft = new FeatureTypes();
        System.out.println("Fetching area with ID: " + placeId);
        try (Scanner scanner = new Scanner(
            new URI("https://pvc.coolwebsite.uk/api/v1/fetch/area/byid/" + placeId).toURL().openStream(), "UTF-8")) {
            String out = scanner.useDelimiter("\\A").next();
            Gson gson = new Gson();
            ft.area = gson.fromJson(out, AreasFetch.class);
            System.out.println("X/Z: " + ft.area.x + " / " + ft.area.z);
            ft.area.x = String.format("%d", (int)(metersToPixels(Double.parseDouble(ft.area.x))));
            ft.area.z = String.format("%d", (int)(metersToPixels(Double.parseDouble(ft.area.z))));
            return ft;
        } catch (FileNotFoundException e) {
            showToast("Area not found!", "The area may have just been deleted.");
            return ft;
        } catch (Exception e) {
            showToast("Mapper Connect Error", "Check your connection and try again later!");
            System.out.println("Failed to fetch places from PVC Mapper!");
            System.out.println(e);
            return ft;
        }
    }

    public SearchResult[] fetchSearchResults(String query) {
        try (Scanner scanner = new Scanner(
            new URI("https://pvc.coolwebsite.uk/api/v2/search/" + query).toURL().openStream(), "UTF-8")) {
            String out = scanner.useDelimiter("\\A").next();
            Gson gson = new Gson();
            return gson.fromJson(out, SearchResult[].class);
        } catch (FileNotFoundException e) {
            showToast("Search not available!", "Try again later! :(");
            return null;
        } catch (Exception e) {
            showToast("Mapper Connect Error", "Check your connection and try again later!");
            System.out.println("Failed to fetch places from PVC Mapper!");
            System.out.println(e);
            return null;
        }
    }

    public void checkForUpdates() {
        String McVersionName = SharedConstants.getCurrentVersion().name();
        String MapperModVersionName;
        try {
            MapperModVersionName = FabricLoader.getInstance().getModContainer("pvc-mapper-mod").orElseThrow().getMetadata().getVersion().getFriendlyString();
        } catch(Exception e) {
            // Just forget it, something weird's going on
            return;
        }
        System.out.println("Checking for updates from https://pvc.coolwebsite.uk/mod/downloads/modversions.json...");
        System.out.println("Current version: " + McVersionName + ", " + MapperModVersionName);
        try (Scanner scanner = new Scanner(
            new URI("https://pvc.coolwebsite.uk/mod/downloads/modversions.json").toURL().openStream(), "UTF-8"
        )) {
            String out = scanner.useDelimiter("\\A").next();
            Gson gson = new Gson();
            VersionHistory[] vh = gson.fromJson(out, VersionHistory[].class);
            for (int i = 0; i < vh.length; i++) {
                if(vh[i].mcVersion.equals(McVersionName)) {
                    System.out.println("Version for this MC version is found! v" + vh[i].version);
                    if(!vh[i].version.equals(MapperModVersionName)) {
                        Minecraft.getInstance().getToastManager().addToast(new SystemToast(SystemToastId.PERIODIC_NOTIFICATION, Component.literal("PVC Mapper Mod Updates"), Component.literal("v" +vh[i].version + " now available! See website for details.")));
                    }
                    return;
                }
            }
        } catch (Exception e) {
            Minecraft.getInstance().getToastManager().addToast(new SystemToast(SystemToastId.PERIODIC_NOTIFICATION, Component.literal("PVC Mapper Mod Error"), Component.literal("Check for updates failed. The Mapper may be down!")));
        }
    }
}

class CoordPair {
    int x;
    int z;
}

class ClaimMarkers {
    String fillColor;
    String popup;
    String color;
    float fillOpacity;
    float weight;
    String type;
    CoordPair[] points;
    CoordPair[][] polygonPoints;
}

class ClaimMarkerDeserialize implements JsonDeserializer<ClaimFetch> {
    @Override
    public ClaimFetch deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
            throws JsonParseException {

        JsonObject obj = json.getAsJsonObject();
        ClaimFetch cf = new ClaimFetch();
        cf.hide = obj.get("hide").getAsBoolean();
        cf.z_index = obj.get("z_index").getAsInt();
        cf.name = obj.get("name").getAsString();
        cf.control = obj.get("control").getAsBoolean();
        cf.id = obj.get("id").getAsString();
        cf.order = obj.get("order").getAsInt();
        cf.timestamp = obj.get("timestamp").getAsInt();
        // Create cf markers
        JsonArray markers = obj.get("markers").getAsJsonArray();
        cf.markers = new ClaimMarkers[markers.size()];
        // Put em all in!
        for (int cm = 0; cm < markers.size(); cm++) {
            JsonObject marker = markers.get(cm).getAsJsonObject();
            cf.markers[cm] = new ClaimMarkers();
            cf.markers[cm].fillColor = marker.get("fillColor").getAsString();
            cf.markers[cm].popup = marker.get("popup").getAsString();
            cf.markers[cm].color = marker.get("color").getAsString();
            cf.markers[cm].fillOpacity = marker.get("fillOpacity").getAsFloat();
            cf.markers[cm].weight = marker.get("weight").getAsFloat();
            cf.markers[cm].type = marker.get("type").getAsString();
            // Get all points
            JsonArray coordPairs = marker.get("points").getAsJsonArray();

            // It's a polygon!
            if (coordPairs.get(0).isJsonArray()) {
                cf.markers[cm].polygonPoints = new CoordPair[coordPairs.size()][];
                for (int cps = 0; cps < coordPairs.size(); cps++) {
                    JsonArray coordPairSet = coordPairs.get(cps).getAsJsonArray();
                    cf.markers[cm].polygonPoints[cps] = new CoordPair[coordPairSet.size()];
                    for (int cpss = 0; cpss < coordPairSet.size(); cpss++) {
                        JsonObject coordPairSubSet = coordPairSet.get(cpss).getAsJsonObject();
                        cf.markers[cm].polygonPoints[cps][cpss] = new CoordPair();
                        cf.markers[cm].polygonPoints[cps][cpss].x = coordPairSubSet.get("x").getAsInt();
                        cf.markers[cm].polygonPoints[cps][cpss].z = coordPairSubSet.get("z").getAsInt();
                    }
                }
            } else {
                cf.markers[cm].points = new CoordPair[coordPairs.size()];
                for (int cp = 0; cp < coordPairs.size(); cp++) {
                    JsonObject coordPair = coordPairs.get(cp).getAsJsonObject();
                    cf.markers[cm].points[cp] = new CoordPair();
                    cf.markers[cm].points[cp].x = coordPair.get("x").getAsInt();
                    cf.markers[cm].points[cp].z = coordPair.get("z").getAsInt();
                }

            }
        }

        return cf;
    }
}

class ClaimFetch {
    boolean hide;
    int z_index;
    String name;
    boolean control;
    String id;
    ClaimMarkers[] markers;
    int order;
    int timestamp;
}

class FeatureFetch {
    float x;
    float z;
    String name;
    int id;
    String type;
    String featureType;
    double[][] bounds;
}

class FeatureTypes {
    PlacesFetch place;
    AreasFetch area;
}

class AddedBy {
    String username;
    String name;
    String uuid;
}

class PlaceFeatures {
    boolean echest;
    boolean portal;
    @SerializedName("public")
    boolean Public;
    boolean historical;
}

class PlacesFetch {
    int id;
    String name;
    String description;
    String x;
    String z;
    String wiki;
    String createdBy;
    String dateCreated;
    AddedBy addedBy;
    String type;
    String hits;
    String images;
    PlaceFeatures features;
    String dimension;
    String createdAt;
    String updatedAt;
}

class AreasFetch {
    int id;
    String name;
    String description;
    String wiki;
    String type;
    String dimension;
    String image;
    double[][] bounds;
    AddedBy addedBy;
    String size;
    String x;
    String z;
    String createdAt;
    String updatedAt;
}

class SearchResult {
    String type;
    String name;
    String id;
    String description;
    int x;
    int z;
}

class VersionHistory {
    String version;
    String mcVersion;
    String fabricLoaderVersion;
    String dateReleased;
    String whatsNew;
    String url;
}