package larrytllama.pvcmappermod;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Base64;
import java.util.Scanner;
import java.util.Base64.Decoder;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

class SponsorBanner {
    String title;
    String link;
    String imgurl;
    String description;
}

public class SponsorUtils {
    public static SponsorBanner getBanner() {
        System.out.println("Fetching sponsor banner from https://pvc.coolwebsite.uk/api/v1/sponsor-banner");
        SponsorBanner sponsor;
        try(Scanner scanner = new Scanner(
            new URI("https://pvc.coolwebsite.uk/api/v1/sponsor-banner").toURL().openStream(), "UTF-8")) {
            String out = scanner.useDelimiter("\\A").next();
            Gson gson = new Gson();
            sponsor = gson.fromJson(out, SponsorBanner.class);
        } catch(Exception e) {
            sponsor = new SponsorBanner();
            System.out.println("Failed to fetch sponsor banners");
            System.out.println(e);
        }
        return sponsor;
    }

    public static void bannerToTexture(String bannerDataURL, Consumer<ResourceLocation> callback) {
        String b64string = bannerDataURL.replace("data:image/png;base64,", "");
        if(b64string.startsWith("data:")) {
            callback.accept(null); // Wrong file format (not PNG)
            return;
        }
        Decoder decoder = Base64.getDecoder();
        byte[] bytes = decoder.decode(b64string);
        String idString = "sponsorbanner_" + Integer.toHexString(bannerDataURL.hashCode());
        try {
            NativeImage image;
            try (var input = new ByteArrayInputStream(bytes)) {
                image = NativeImage.read(input);
            }
            Minecraft.getInstance().execute(() -> {
                DynamicTexture texture = new DynamicTexture(() -> idString, image);
                ResourceLocation resourceLocation = ResourceLocation.fromNamespaceAndPath("pvcmappermod", idString);
                Minecraft.getInstance().getTextureManager().register(resourceLocation, texture);
                callback.accept(resourceLocation);
            });
        } catch(Exception e) {
            System.out.println("Error converting data to image content:");
            System.out.println(e);
            callback.accept(null);
        }

    }
}
