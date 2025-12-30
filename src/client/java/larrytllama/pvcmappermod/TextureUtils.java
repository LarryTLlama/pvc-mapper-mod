package larrytllama.pvcmappermod;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class TextureUtils {
    static ResourceLocation blurredTile = ResourceLocation.fromNamespaceAndPath("pvcmappermod","textures/gui/tileloading.png");

    /**
     * Fetches an image from a URL, converts it to a DynamicTexture, registers it,
     * and returns the ResourceLocation via a callback.
     *
     * @param urlString The URL of the image.
     * @param callback  The callback to run when the texture is ready.
     */
    public static void fetchRemoteTexture(String urlString, Consumer<ResourceLocation> callback) {
        System.out.println("Fetching image from source: " + urlString);
        CompletableFuture.runAsync(() -> {
            try {
                NativeImage image;
                byte[] imgBytes = new URI(urlString.startsWith("https://") ? urlString : "https://pvc.coolwebsite.uk" + urlString).toURL().openStream().readAllBytes();

                // Download and read the image off-thread
                try (var input = new ByteArrayInputStream(imgBytes)) {
                    image = NativeImage.read(input);
                }

                // Schedule registration on the main render thread
                NativeImage finalImage = image;
                Minecraft.getInstance().execute(() -> {
                    String idStr = "remote_" + Integer.toHexString(urlString.hashCode());
                    DynamicTexture dynamicTexture = new DynamicTexture(() -> idStr, finalImage);
                    //dynamicTexture
                    ResourceLocation resourceLocation = ResourceLocation.fromNamespaceAndPath("pvcmappermod", idStr);
                    Minecraft.getInstance().getTextureManager().register(resourceLocation, dynamicTexture);
                    callback.accept(resourceLocation);
                });

            } catch(FileNotFoundException e) {
                callback.accept(blurredTile);
            } catch (Exception e) {
                callback.accept(null);
                e.printStackTrace();
            }
        });
    }

    
}
