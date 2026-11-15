package larrytllama.pvcmappermod.mixin.client;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import larrytllama.pvcmappermod.LogUtils;
import larrytllama.pvcmappermod.PVCMapperModClient;
import larrytllama.pvcmappermod.PreConnectScreen;
import larrytllama.pvcmappermod.utils.CompatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList.OnlineServerEntry;
import net.minecraft.client.multiplayer.ServerData;

@Mixin (JoinMultiplayerScreen.class)
public abstract class ServerListEntryMixin {
    private static volatile boolean PVC_ALLOW_JOIN = false;

    @Inject (method = "join", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(ServerData serverData, CallbackInfo ci) {
        if(PVC_ALLOW_JOIN || PVCMapperModClient.INSTANCE.sp.shownDataNotice) {
            PVC_ALLOW_JOIN = false;
            return;
        }
        if (serverData != null && "mc.peacefulvanilla.club".equals(serverData.ip)) {
            Screen self = (Screen)(Object)this;
            CompatUtils.setScreen(new PreConnectScreen(self, () -> {
                // call the original join after confirming
                Minecraft mc = Minecraft.getInstance();
                CompatUtils.setScreen(mc, self);
                mc.execute(() -> {
                    try {
                        PVC_ALLOW_JOIN = true;
                        ((net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen) self).join(serverData);
                    } catch (Throwable t) {
                        LogUtils.error("Failed to join server after pre-connect notice", t);
                    }
                });
            }));
            // cancel default click handling
            ci.cancel();
        }
    }
}