package larrytllama.pvcmappermod.mixin.client;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerTabOverlay.class)
public interface TabListMixin {
    // Lets me use the header/footer to find if Terra2 exists
    @Accessor("header")
    Component getHeader();
    @Accessor("footer")
    Component getFooter();
}