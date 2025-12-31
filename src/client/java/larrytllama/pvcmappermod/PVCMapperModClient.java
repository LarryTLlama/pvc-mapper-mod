package larrytllama.pvcmappermod;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping.Category;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class PVCMapperModClient implements ClientModInitializer {
    public KeyMapping OPEN_MAP = new KeyMapping("pvcmappermod.open_map", GLFW.GLFW_KEY_M,
            Category.register(ResourceLocation.fromNamespaceAndPath("pvcmappermod", "category")));

    public FullScreenMap fsm;
    @Override
    public void onInitializeClient() {

        // Set up player fetchererer
        PlayerFetchUtils pfu = new PlayerFetchUtils();
        new MapperCmdHandler(pfu, this);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            pfu.startUpdates();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            pfu.stopUpdates();
        });

        Minimap.attach(pfu);
        OPEN_MAP = KeyBindingHelper.registerKeyBinding(OPEN_MAP);
        fsm = FullScreenMap.createScreen(Component.literal("PVC Mapper - Map View"), pfu);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_MAP.consumeClick()) {
                Minecraft.getInstance().setScreen(fsm);
            }
        });
    }
}