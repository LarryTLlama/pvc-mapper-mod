package larrytllama.pvcmappermod;

import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
//? if <26.1 {
import net.minecraft.client.gui.GuiGraphics;
//?} else {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;*///?}
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.Font;
import java.lang.System;

import larrytllama.pvcmappermod.utils.GraphicsHelper;
import larrytllama.pvcmappermod.utils.ResIdentifier;

public class WelcomeToast implements Toast {
    private final Component title;
    private final Component subtitle;
    private long startShow = System.currentTimeMillis();
    private ResIdentifier TOAST = ResIdentifier.of("minecraft", "textures/gui/sprites/toast/advancement.png");
    private ResIdentifier TOAST_ICON = ResIdentifier.of("minecraft", "textures/gui/sprites/icon/link.png");

    public WelcomeToast(Component title, Component subtitle) {
        this.title = title;
        this.subtitle = subtitle;
    }

    public void update(ToastManager manager, long startTime) {
        
    }


    @Override
    public Visibility getWantedVisibility() {
        if ( (System.currentTimeMillis() - startShow) > 5000) return Visibility.HIDE;
        return Visibility.SHOW;
    }

    //? if >=26.1 {
    /* @Override
    public void extractRenderState(GuiGraphicsExtractor gui, Font font, long visibleTime) {
        gui.blit(RenderPipelines.GUI_TEXTURED, TOAST.get(), 0, 0, 0, 0, 160, 32, 160, 32);
        gui.blit(RenderPipelines.GUI_TEXTURED, TOAST_ICON.get(), 6, 6, 0, 0, 20, 20, 20, 20);

        if (this.title == null) {
            GraphicsHelper.drawString(gui, font, "Welcome to:", 32, 6, 0xFFFFFFFF);
        } else {
            gui.enableScissor(6, 6, 154, 26);
            GraphicsHelper.drawString(gui,font, this.title, 32, 6, 0xFFFFFFFF);

            gui.disableScissor();
        }

        if (this.subtitle != null) {
            gui.enableScissor(6, 6, 154, 26);
            GraphicsHelper.drawString(gui, font, this.subtitle, 32, 18, 0xFFCCCCCC);
            gui.disableScissor();
        }
    }
    */
    //? } else {
    @Override
    public void render(GuiGraphics gui, Font font, long somethingidk) {
        
        gui.blit(RenderPipelines.GUI_TEXTURED, TOAST.get(), 0, 0, 0, 0, 160, 32, 160, 32);
        gui.blit(RenderPipelines.GUI_TEXTURED, TOAST_ICON.get(), 6, 6, 0, 0, 20, 20, 20, 20);

        if (this.title == null) {
            GraphicsHelper.drawString(gui, font, "Welcome to:", 32, 6, 0xFFFFFFFF);
        } else {
            gui.enableScissor(6, 6, 154, 26);
            GraphicsHelper.drawString(gui,font, this.title, 32, 6, 0xFFFFFFFF);

            gui.disableScissor();
        }

        if (this.subtitle != null) {
            gui.enableScissor(6, 6, 154, 26);
            GraphicsHelper.drawString(gui, font, this.subtitle, 32, 18, 0xFFCCCCCC);
            gui.disableScissor();
        }
    } 
    //? }
}