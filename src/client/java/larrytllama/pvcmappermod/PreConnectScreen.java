package larrytllama.pvcmappermod;

import java.net.URI;
import java.util.List;

import larrytllama.pvcmappermod.utils.CompatUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public class PreConnectScreen extends Screen {
    private final Runnable onConfirm;
    private final Screen parent;

    public PreConnectScreen(Screen parent, Runnable onConfirm) {
        super(Component.literal("About to connect"));
        this.parent = parent;
        this.onConfirm = onConfirm;
    }

    @Override
    protected void init() {
        // Title
        StringWidget title = new StringWidget(Component.literal("Welcome to the PVC Mapper!").withStyle(ChatFormatting.BOLD), Minecraft.getInstance().font);
        title.setPosition((this.width/2) - (Minecraft.getInstance().font.width("Welcome to the PVC Mapper!")/2), 20);
        this.addRenderableWidget(title);

        // Main message
        URI dataUploadsLink;
        try {
            dataUploadsLink = new URI("https://pvc.coolwebsite.uk/help/data-uploading");
        } catch(Exception e) {
            dataUploadsLink = null;
            LogUtils.error("Tried to load the data notice link, but got:", e);
        }

        Component messageString = text(
            styled("Hello!", ChatFormatting.YELLOW),
            " Thanks for trying the unofficial PVC Mapper Mod! Once in game:\n",
            styled("  • ", ChatFormatting.GRAY), "Press ", styled("[M]", ChatFormatting.GOLD), " to open the map\n",
            styled("  • ", ChatFormatting.GRAY), "Press comma ", styled("[,]", ChatFormatting.GOLD), " to open the shops viewer!\n",
            styled("  (You can rebind these keys in the settings).\n", ChatFormatting.GRAY),
            styled("  • ", ChatFormatting.GRAY), "Use ", styled("/mapper help", ChatFormatting.AQUA), " to see the mod commands!\n\n",
            "The mod also auto-uploads things like player ranks (taken from the Tab list), nether portal locations, and Terra2 map data (coming soon!). This stuff isn't available from the official PVC website, so is contributed by mod users like you!\n",
            "To find out more, click 'See Data Notice'. Use this checkbox to enable or disable it:"
        );
        
        int messageY = 20 + (Minecraft.getInstance().font.lineHeight * 2);
        MultiLineTextWidget message = new MultiLineTextWidget(
            messageString, 
            Minecraft.getInstance().font
        );
        message.setMaxWidth(this.width - 40);
        message.setPosition(20, messageY);
        this.addRenderableWidget(message);

        // Checkbox and More Info placed directly under the explanation text
        int controlsY = message.getY() + message.getHeight() + 8;
        // Keep within screen bounds if screen is small / zoomed in
        if (controlsY + 50 > this.height - 40) {
            controlsY = Math.max(message.getY() + 20, this.height - 65);
        }

        Checkbox checkbox = Checkbox.builder(Component.literal("Enable Mapper Uploads"), Minecraft.getInstance().font)
            .selected(PVCMapperModClient.INSTANCE.sp.collectData)
            .onValueChange((cb, bl) -> {
                PVCMapperModClient.INSTANCE.sp.collectData = bl;
            })
            .pos(20, controlsY)
            .build();
        this.addRenderableWidget(checkbox);

        Component moreInfoText = Component.literal("See Data Notice");
        int moreInfoWidth = Minecraft.getInstance().font.width(moreInfoText) + 14;
        int moreInfoX = checkbox.getX() + checkbox.getWidth() + 10;
        int signoffY;
        if (moreInfoX + moreInfoWidth <= this.width - 20) {
            this.addRenderableWidget(Button.builder(moreInfoText, ConfirmLinkScreen.confirmLink(this, dataUploadsLink))
                .bounds(moreInfoX, controlsY - 1, moreInfoWidth, 20).build());
            signoffY = controlsY + 26;
        } else {
            this.addRenderableWidget(Button.builder(moreInfoText, ConfirmLinkScreen.confirmLink(this, dataUploadsLink))
                .bounds(20, controlsY + 22, moreInfoWidth, 20).build());
            signoffY = controlsY + 46;
        }

        StringWidget signoff = new StringWidget(
            Component.literal("Happy Mappering — Larry :)").withStyle(ChatFormatting.GREEN),
            Minecraft.getInstance().font
        );
        signoff.setPosition(20, signoffY);
        this.addRenderableWidget(signoff);

        this.addRenderableWidget(Button.builder(Component.literal("Connect to PVC"), btn -> {
            PVCMapperModClient.INSTANCE.sp.shownDataNotice = true;
            PVCMapperModClient.INSTANCE.sp.saveSettings();
            onConfirm.run();
        }).bounds(this.width/2 - 110, this.height - 35, 100, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), btn -> {
            CompatUtils.setScreen(parent);
        }).bounds(this.width/2 + 10, this.height - 35, 100, 20).build());
    }

    private static MutableComponent text(Object... parts) {
        MutableComponent root = Component.empty();
        for (Object part : parts) {
            if (part instanceof Component c) {
                root.append(c);
            } else if (part != null) {
                root.append(Component.literal(part.toString()));
            }
        }
        return root;
    }

    private static MutableComponent styled(String s, ChatFormatting... formats) {
        return Component.literal(s).withStyle(formats);
    }
}