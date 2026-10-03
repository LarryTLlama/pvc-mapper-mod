package larrytllama.pvcmappermod.utils;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceKey;

public class CompatUtils {

    public static void setScreen(Screen screen) {
        setScreen(Minecraft.getInstance(), screen);
    }

    public static void setScreen(Minecraft mc, Screen screen) {
        //$ if >=26.2 'mc.gui.setScreen(screen);' else 'mc.setScreen(screen);'
        mc.setScreen(screen);
    }

    public static void addToast(Toast toast) {
        Minecraft mc = Minecraft.getInstance();
        //$ if >=26.2 'mc.gui.toastManager().addToast(toast);' else 'mc.getToastManager().addToast(toast);'
        mc.getToastManager().addToast(toast);
    }

    public static ResIdentifier getIdentifier(ResourceKey<?> key) {
        //$ if >=1.21.11 'return ResIdentifier.of(key.identifier());' else 'return ResIdentifier.of(key.location());'
        return ResIdentifier.of(key.location());
    }

    public static KeyMapping registerKey(KeyMapping keyMapping) {
        //$ if >=26.1 'return net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(keyMapping);' else 'return net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(keyMapping);'
        return net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(keyMapping);
    }

    public static boolean isKeyDown(int key) {
        Minecraft mc = Minecraft.getInstance();
        //$ if >=26.3 'return InputConstants.isKeyDown(key);' else 'return InputConstants.isKeyDown(mc.getWindow(), key);'
        return InputConstants.isKeyDown(mc.getWindow(), key);
    }
}
