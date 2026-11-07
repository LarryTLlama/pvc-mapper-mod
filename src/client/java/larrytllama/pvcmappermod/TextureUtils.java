package larrytllama.pvcmappermod;

import larrytllama.pvcmappermod.utils.ResIdentifier;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;


import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import larrytllama.pvcmappermod.utils.*;

public class TextureUtils {
    // Limits background tile downloads to prevent Cloudflare rate-limiting.
    // Increased to 16 from 7-8 because the Stagger logic below prevents bursts. Could try higher.
    private static final int MAX_CONCURRENT_DOWNLOADS = 25;
    
    // Global stagger to prevent Cloudflare/Origin challenges during initialization's fetches.
    private static final java.util.concurrent.atomic.AtomicLong lastFetchTime = new java.util.concurrent.atomic.AtomicLong(0);
    private static final int FETCH_STAGGER_MS = 50;

    // LIFO Queue for pending downloads. Prioritizes the most recently requested tiles.
    private static final LinkedBlockingDeque<Runnable> fetchQueue = new LinkedBlockingDeque<>();
    private static final java.util.concurrent.atomic.AtomicInteger activeDownloads = new java.util.concurrent.atomic.AtomicInteger(0);
    private static final ConcurrentHashMap<String, Long> failedFetches = new ConcurrentHashMap<>();

    private static void pumpQueue() {
        while (activeDownloads.get() < MAX_CONCURRENT_DOWNLOADS) {
            Runnable task = fetchQueue.pollFirst();
            if (task == null) break;
            
            activeDownloads.incrementAndGet();
            Thread.ofVirtual().start(() -> {
                try {
                    task.run();
                } finally {
                    activeDownloads.decrementAndGet();
                    pumpQueue();
                }
            });
        }
    }
    
    private static final int MAX_CACHED_TILES = 200;
    
    // LRU Cache for managing memory limit
    private static final java.util.Map<String, ResIdentifier> tileCache = java.util.Collections.synchronizedMap(
        new java.util.LinkedHashMap<String, ResIdentifier>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(java.util.Map.Entry<String, ResIdentifier> eldest) {
                if (size() > MAX_CACHED_TILES) {
                    ResIdentifier id = eldest.getValue();
                    net.minecraft.client.Minecraft.getInstance().getTextureManager().release(id.get());
                    activeTiles.decrementAndGet();
                    return true;
                }
                return false;
            }
        }
    );
    
    // Memory Profiling Counter
    public static final java.util.concurrent.atomic.AtomicInteger activeTiles = new java.util.concurrent.atomic.AtomicInteger(0);
    
    public static final ResIdentifier blurredTile = ResIdentifier.of("pvcmappermod", "textures/gui/tileloading.png");

    private static final ConcurrentHashMap<String, Boolean> pendingFetches = new ConcurrentHashMap<>();

    /**
     * Fetches an image from a URL, converts it to a DynamicTexture, registers it,
     * and returns the ResourceLocation / Identifier via a callback. Uses the bounded download pool.
     *
     * @param urlString The URL of the image.
     * @param callback  The callback to run when the texture is ready.
     */
    public static void fetchRemoteTexture(String urlString, Consumer<ResIdentifier> callback) {
        ResIdentifier cached = tileCache.get(urlString);
        if (cached != null) {
            callback.accept(cached);
            return;
        }

        Long failedTime = failedFetches.get(urlString);
        if (failedTime != null && (System.currentTimeMillis() - failedTime < 10000)) {
            // Already failed recently. Use blurred tile silently.
            callback.accept(blurredTile);
            return;
        }

        if (pendingFetches.putIfAbsent(urlString, Boolean.TRUE) != null) {
            // Already fetching
            callback.accept(blurredTile);
            return;
        }

        LogUtils.debug("Fetching image from source: " + urlString);
        fetchQueue.addFirst(() -> {
            executeFetch(urlString, callback);
        });
        pumpQueue();
    }

    /**
     * Instantly fetches an image, bypassing the background tile queue.
     * Intended exclusively for UI elements like POI overlays and banners.
     */
    public static void fetchImmediateRemoteTexture(String urlString, Consumer<ResIdentifier> callback) {
        ResIdentifier cached = tileCache.get(urlString);
        if (cached != null) {
            callback.accept(cached);
            return;
        }

        if (pendingFetches.putIfAbsent(urlString, Boolean.TRUE) != null) {
            callback.accept(blurredTile);
            return;
        }

        LogUtils.debug("Fetching immediate UI image from source: " + urlString);
        Thread.ofVirtual().start(() -> {
            executeFetch(urlString, callback);
        });
    }

    public static ResIdentifier getCachedTexture(String urlString) {
        return tileCache.get(urlString);
    }

    public static void clearCache() {
        Minecraft.getInstance().execute(() -> {
            for (ResIdentifier id : tileCache.values()) {
                Minecraft.getInstance().getTextureManager().release(id.get());
                activeTiles.decrementAndGet();
            }
            tileCache.clear();
            pendingFetches.clear();
            failedFetches.clear();
        });
    }

    private static void executeFetch(String urlString, Consumer<ResIdentifier> callback) {
        // Apply Global Stagger to avoid overwhelming the server/Cloudflare with bursts from initialization
        try {
            long now = System.currentTimeMillis();
            long nextTime = lastFetchTime.updateAndGet(last -> Math.max(last + FETCH_STAGGER_MS, now));
            long sleepTime = nextTime - now;
            if (sleepTime > 0) {
                Thread.sleep(sleepTime);
            }
        } catch (InterruptedException ignored) {}

        try {
            String targetUrl = urlString.startsWith("https://") ? urlString : NetworkUtils.BASE_URL + urlString;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(targetUrl))
                    .GET()
                    .build();

            // Use InputStream handler for memory efficiency (pipes directly to NativeImage)
            HttpResponse<InputStream> response = NetworkUtils.HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() == 404) {
                Minecraft.getInstance().execute(() -> {
                    pendingFetches.remove(urlString);
                    callback.accept(blurredTile);
                });
                return;
            }
            
            if (response.statusCode() != 200) {
                LogUtils.warn("[TextureUtils] Failed to fetch tile: " + urlString + " (HTTP " + response.statusCode() + ")");
                failedFetches.put(urlString, System.currentTimeMillis());
                Minecraft.getInstance().execute(() -> {
                    pendingFetches.remove(urlString);
                    callback.accept(blurredTile); // using blurredTile so we don't return null
                });
                return;
            }

            // Check if Cloudflare served an explicit HTML challenge instead of an image
            String contentType = response.headers().firstValue("Content-Type").orElse("");
            if (contentType.contains("text/html")) {
                handleSoftRateLimit(urlString, "Cloudflare Challenge", callback);
                return;
            }

            NativeImage image;
            // Download and read the image off-thread
            try (InputStream input = response.body()) {
                image = NativeImage.read(input);
            }

            // Schedule registration on the main render thread
            NativeImage finalImage = image;
            Minecraft.getInstance().execute(() -> {
                String idStr = "remote_" + Integer.toHexString(urlString.hashCode());
                DynamicTexture dynamicTexture = new DynamicTexture(() -> idStr, finalImage);
                //dynamicTexture
                ResIdentifier resourceLocation = ResIdentifier.of("pvcmappermod", idStr);
                Minecraft.getInstance().getTextureManager().register(resourceLocation.get(), dynamicTexture);
                tileCache.put(urlString, resourceLocation);
                
                int currentTiles = activeTiles.incrementAndGet();
                int osThreads = java.lang.management.ManagementFactory.getThreadMXBean().getThreadCount();
                LogUtils.debug("[Memory Profiling] Active Map Tiles Loaded: %d (Est. VRAM: %.2f MB) | OS Threads: %d", currentTiles, currentTiles * 0.25, osThreads);
                
                pendingFetches.remove(urlString);
                callback.accept(resourceLocation);
                failedFetches.remove(urlString); // clear cache on success
            });

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("Bad PNG Signature")) {
                handleSoftRateLimit(urlString, "Fake Image Header", callback);
                return;
            }

            LogUtils.warn("[TextureUtils Error] Exception while fetching URL: " + urlString, e);
            Minecraft.getInstance().execute(() -> {
                pendingFetches.remove(urlString);
                callback.accept(null);
            });
        }
    }

    /**
     * Handles Cloudflare "Soft Rate Limits" by applying a backoff and serving a placeholder.
     */
    private static void handleSoftRateLimit(String urlString, String reason, Consumer<ResIdentifier> callback) {
        LogUtils.warn("[TextureUtils] Soft Rate Limit Triggered (" + reason + "): " + urlString);
        // Back off for 20 seconds for this specific URL
        failedFetches.put(urlString, System.currentTimeMillis() + 20000);
        Minecraft.getInstance().execute(() -> callback.accept(blurredTile));
    }
}
