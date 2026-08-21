package larrytllama.pvcmappermod.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.toasts.Toast;

public class CompatUtils {

    public static void setScreen(Screen screen) {
        setScreen(Minecraft.getInstance(), screen);
    }

    public static void setScreen(Minecraft mc, Screen screen) {
        //? if <26.2 {
        mc.setScreen(screen);
        //?} else {
        /*mc.gui.setScreen(screen);*///?}
    }

    public static void addToast(Toast toast) {
        //? if <26.2 {
        Minecraft.getInstance().getToastManager().addToast(toast);
        //?} else {
        /*Minecraft.getInstance().gui.toastManager().addToast(toast);*///?}
    }

    public static ResIdentifier getIdentifier(net.minecraft.resources.ResourceKey<?> key) {
        //? if <1.21.11 {
        return ResIdentifier.of(key.location());
        //?} else {
        /*return ResIdentifier.of(key.identifier());*///?}
    }

    public static net.minecraft.client.KeyMapping registerKey(net.minecraft.client.KeyMapping keyMapping) {
        //? if <26.1 {
        return net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(keyMapping);
        //?} else {
        /*return net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(keyMapping);*///?}
    }

    public static boolean isKeyDown(int key) {
        //? if <26.3 {
        return com.mojang.blaze3d.platform.InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), key);
        //?} else {
        /*return com.mojang.blaze3d.platform.InputConstants.isKeyDown(key);*///?}
    }
}
