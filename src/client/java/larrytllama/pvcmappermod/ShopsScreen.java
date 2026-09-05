package larrytllama.pvcmappermod;

import larrytllama.pvcmappermod.utils.*;

import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
//? if <1.21.11 {
import net.minecraft.Util;
//?} else {
/*import net.minecraft.util.Util;*///?}
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button.Builder;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.ChatFormatting;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//? if <26.1 {
import net.minecraft.client.gui.GuiGraphics;
//?} else {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;*///?}

public class ShopsScreen extends Screen {
    Shop[] shops;
    MyList list;
    EditBox itemSearch;
    ArrayList<String> itemsList;
    Button searchBtn;
    
    Shop displayedTrade;

    ResIdentifier sponsorBanner;
    List<MutableComponent> sponsorHoverText;
    String sponsorURLString;
    Boolean drawSponsorTooltip = false; 

    String currentSearchMode = "item";

    CustomItem[] customItems;

    public ShopsScreen(Component title) {
        super(title);
        itemsList = ShopsHandler.getAllItems();
        ShopsHandler.getCustomItems().thenAccept((ci) -> {
            customItems = ci;
        });
    }

    String[] filteredItems = new String[0];

    @Override
    protected void init() {
        // Get width
        int panelWidth = 100;
        Minecraft minecraft = Minecraft.getInstance();
        list = new MyList(minecraft, panelWidth + MyList.SCROLLBAR_WIDTH, this.height - 60, 0, 20);
        list.setPosition(10, 10);
        this.addRenderableWidget(list);

        itemSearch = new EditBox(minecraft.font, (this.width) - 200, this.height - 35, 160, 20,
                Component.literal("Search for items..."));
        itemSearch.setHint(Component.literal("E.G PLAYTIME_CERTIFICATE"));
        itemSearch.setMaxLength(64);
        itemSearch.setFocused(true);
        itemSearch.setResponder((text) -> {
            if(text.contains(" ")) itemSearch.setValue(text.replaceAll(" ", "_"));
            // Get a list of filtered items, up to 5 of em
            filteredItems = new String[5];
            int filledIndex = 0;
            for (int i = 0; i < itemsList.size(); i++) {
                if (itemsList.get(i).toLowerCase().startsWith(text.toLowerCase())) {
                    filteredItems[filledIndex] = itemsList.get(i);
                    filledIndex++;
                    if (filledIndex == 5) break;
                }
            }
        });
        this.addRenderableWidget(itemSearch);

        searchBtn = new Button.Builder(Component.empty(), (b) -> {
            if(currentSearchMode.equals("item")) this.openWithItem(this.itemSearch.getValue());
            else if(currentSearchMode.equals("username")) this.openWithUsername(this.itemSearch.getValue());
            else LogUtils.error("Unknown search mode error");
        }).bounds( this.width - 35, this.height - 35, 20, 20).build();

        this.addRenderableWidget(searchBtn);

        //? if >=1.21.11 {
        /*CycleButton<String> searchMode = CycleButton.<String>builder((String value) -> Component.literal(value), "item")*/
        //?} else {
        CycleButton<String> searchMode = CycleButton.<String>builder((String value) -> Component.literal(value))
        //?}
            .withValues("item", "username")
            .create(
                this.width - 20 - minecraft.font.width("Search by: username"), 
                this.height - 50 - minecraft.font.lineHeight, 
                minecraft.font.width("Search by: username") + 10, minecraft.font.lineHeight + 10, 
                Component.literal("Search by"), (btn, value) -> {
                    currentSearchMode = value;
                    LogUtils.debug(value);
                    switch (value) {
                        case "item":
                            itemSearch.setHint(Component.literal("e.g PLAYTIME_CERTIFICATE"));
                            break;
                        case "username":
                            itemSearch.setHint(Component.literal("e.g .LarryTLlama"));
                            break;
                        default:
                            itemSearch.setHint(Component.literal("Search items or players"));
                            break;
                    }
                } 
            );
        this.addRenderableWidget(searchMode);

        SponsorUtils.getBannerAsync().thenAccept(banner -> {
            SponsorUtils.bannerToTexture(banner.imgurl, (rl) -> {
                this.sponsorBanner = rl;
            });
            sponsorHoverText = List.of(
                    Component.literal(banner.title).withStyle(ChatFormatting.BOLD)
                            .append(Component.literal(" (Click to view)").withStyle(ChatFormatting.GRAY)),
                    Component.literal(banner.description));
            sponsorURLString = banner.link;
        });

        this.setInitialFocus(itemSearch);
        this.setFocused(itemSearch);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (keyEvent.key() == InputConstants.KEY_ESCAPE) {
            if(itemSearch.isFocused()) itemSearch.setFocused(false);
            else CompatUtils.setScreen(null);
        } else if(itemSearch.isFocused()) {
            if(keyEvent.key() == InputConstants.KEY_TAB) {
                if (filteredItems != null && filteredItems.length > 0 && filteredItems[0] != null) {
                    itemSearch.setValue(filteredItems[0]);
                }
            } else if(keyEvent.key() == InputConstants.KEY_RETURN) {
                if(currentSearchMode.equals("item")) this.openWithItem(this.itemSearch.getValue());
                else if(currentSearchMode.equals("username")) this.openWithUsername(this.itemSearch.getValue());
                itemSearch.setFocused(false);
            }
        }
        return super.keyPressed(keyEvent);
    }

    ResIdentifier SEARCH_ICON = ResIdentifier.of("minecraft", "textures/gui/sprites/icon/search.png");

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (sponsorBanner != null) {
            if (mouseY > (this.height - 35) && mouseY < (this.height - 10) && mouseX > 10 && mouseX < 203) {
                drawSponsorTooltip = true;
            } else {
                drawSponsorTooltip = false;
            }
        } else {
            drawSponsorTooltip = false;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mbe, boolean bl) {
        if (mbe.y() > (this.height - 28) && mbe.y() < (this.height - 3) && mbe.x() > 3 && mbe.x() < 196) {
            Minecraft mc = Minecraft.getInstance();
            CompatUtils.setScreen(mc, new ConfirmLinkScreen(confirmed -> {
                if (confirmed) {
                    Util.getPlatform().openUri(sponsorURLString);
                }
                CompatUtils.setScreen(mc, null);
            }, sponsorURLString, true));
        }
        return super.mouseClicked(mbe, bl);
    }

    Map<String, ResIdentifier> tiles = new HashMap<>();
    ResIdentifier blurredTile = ResIdentifier.of("pvcmappermod", "textures/gui/tileloading.png");
    ResIdentifier villagerImg = ResIdentifier.of("pvcmappermod", "textures/gui/villager.png");

    // Trace meeeee
    public void drawMap(/*? if <26.1 {*/GuiGraphics/*?} else {*//*GuiGraphicsExtractor*//*?}*/ graphics, int x, int y, int width, int height, int coordX, int coordZ, String dimension) {
        graphics.enableScissor(x, y, x+width, y+height);

        graphics.pose().pushMatrix();
        graphics.pose().translate(
                x + (width / 2)
                        - MapRenderUtils.worldToLocalTileCoordinate(coordX, 512)
                        - 512,
                y + (height / 2)
                        - MapRenderUtils.worldToLocalTileCoordinate(coordZ, 512)
                        - 512);
        for (int iX = 0; iX < 3; iX++) {
            for (int iZ = 0; iZ < 3; iZ++) {

                int tileX = MapRenderUtils.worldToTileCoordinate(coordX, 512) + iX - 1;
                int tileZ = MapRenderUtils.worldToTileCoordinate(coordZ, 512) + iZ - 1;

                String url = String.format("%s%s/8/%d_%d.png", SettingsProvider.getInstance().mapTileSource, dimension, tileX, tileZ);
                ResIdentifier tile = TextureUtils.getCachedTexture(url);
                
                if(tile == null) {
                    TextureUtils.fetchImmediateRemoteTexture(url, (id) -> {});
                    tile = TextureUtils.blurredTile;
                }
                
                graphics.blit(RenderPipelines.GUI_TEXTURED, tile.get(), (512 * iX), (512 * iZ), 0, 0, 512, 512, 512, 512);
            }
        }
        graphics.pose().popMatrix();
        graphics.blit(RenderPipelines.GUI_TEXTURED, villagerImg.get(), x + (width/2) - 8, y + (height/2) - 8, 0, 0, 16, 16, 16, 16 );
        graphics.disableScissor();
        //graphics.submitOutline(x, y, width, height, 0xAA333333); Was buggy. May add back later. TODO
    }

    //? if <26.1 {
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    //?} else {
    /*@Override
    public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {*///?}
        if (list.getSelected() != null) {
            Shop trade = list.getSelected().shopTrade;
        }


        if(displayedTrade != null) {
            int boxWidth = (this.width / 2) - 80; 
            // Left box
            graphics.fill(
                130, 
                30, 
                130 + boxWidth, 
                30 + (minecraft.font.lineHeight * 2) + 10, 0xAA333333); 
            GraphicsHelper.drawCenteredString(graphics, minecraft.font, Component.literal("Shop Name: "), 130 + (boxWidth/2), 35, 0xFFFFFFFF);
            GraphicsHelper.drawCenteredString(graphics, minecraft.font, Component.literal(displayedTrade.shopName).withStyle(ChatFormatting.BOLD), 130 + (boxWidth/2), 35 + minecraft.font.lineHeight, 0xFFFFFFFF);
            // Right box
            graphics.fill(
                150 + boxWidth, 
                30, 
                150 + (2 * boxWidth), 
                30 + (minecraft.font.lineHeight * 2) + 10, 0xAA333333); 
            GraphicsHelper.drawCenteredString(graphics, minecraft.font, Component.literal("Stocked By: "), 150 + boxWidth + (boxWidth/2), 35, 0xFFFFFFFF);
            GraphicsHelper.drawCenteredString(graphics, minecraft.font, Component.literal(displayedTrade.shopOwner).withStyle(ChatFormatting.BOLD), 150 + boxWidth + (boxWidth/2), 35 + minecraft.font.lineHeight, 0xFFFFFFFF);

            // Beneath that, the location
            graphics.fill(
                130, 
                30 + (minecraft.font.lineHeight * 2) + 20, 
                this.width - 10, 
                30 + (minecraft.font.lineHeight * 3) + 30, 0xAA333333); 

            GraphicsHelper.drawCenteredString(graphics, minecraft.font, Component.literal("Location: ")
                    .append(Component.literal(displayedTrade.location).withStyle(ChatFormatting.BOLD))
                    .append(Component.literal("   -   Stock: "))
                    .append(Component.literal(displayedTrade.stock.toString() + "x").withStyle(ChatFormatting.BOLD)), 130 + ((this.width - 140)/2), 30 + (minecraft.font.lineHeight * 2) + 25, 0xFFFFFFFF);

            String[] split = displayedTrade.location.split(", ");
            String dim = "minecraft_overworld";
            if(split[2].endsWith(" in Nether")) {
                split[2] = split[2].split(" ", 2)[0];
                dim = "minecraft_the_nether";
            }
            drawMap(graphics, 130, 30 + (minecraft.font.lineHeight * 3) + 40, this.width - 140, (this.height - 60 - minecraft.font.lineHeight) - (30 + (minecraft.font.lineHeight * 3) + 30), Integer.parseInt(split[0]), Integer.parseInt(split[2]), dim);
        }

        GraphicsHelper.drawCenteredString(graphics, Minecraft.getInstance().font, Component.literal("Shops Viewer").withStyle(ChatFormatting.BOLD), (this.width/2)+60, 10, 0xFFFFFFFF);
        //? if <26.1 {
        super.render(graphics, mouseX, mouseY, delta);
        //?} else {
        /*super.extractRenderState(graphics, mouseX, mouseY, delta);*///?}
        graphics.blit(RenderPipelines.GUI_TEXTURED, SEARCH_ICON.get(), this.width - 32, this.height - 32, 0, 0, 12, 12, 12, 12);

        if(itemSearch.isFocused() && currentSearchMode.equals("item")) {
            int y = this.height - 37;
            int maxItemLength = 0;
            for (int i = 0; i < filteredItems.length; i++) {
                if(filteredItems[i] == null) break;
                y -= (minecraft.font.lineHeight + 2);
                maxItemLength = Math.max(maxItemLength, minecraft.font.width(filteredItems[i]));
                GraphicsHelper.drawString(graphics, 
                    Minecraft.getInstance().font, Component.literal(itemSearch.getValue()).withStyle(ChatFormatting.WHITE)
                        .append(Component.literal(filteredItems[i].substring(itemSearch.getValue().length()))
                            .withStyle(ChatFormatting.GRAY)), (this.width) - 198, y, 0xFFFFFFFF);
            }

            graphics.fill((this.width) - 200, y - 2, (this.width) - 200 + maxItemLength + 2, this.height-35, 0xAA000000);
        }

        if (sponsorBanner != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, sponsorBanner.get(), 10, this.height - 35, 0, 0, 193, 25, 193, 25);
            if (drawSponsorTooltip) {
                MapRenderUtils.drawTooltipComponent(graphics, sponsorHoverText, mouseX, mouseY);
            }
        } else {
            GraphicsHelper.drawString(graphics, Minecraft.getInstance().font, "Loading banner...", 10, this.height - 30, 0xFFFFFFFF);
        }
        
        // Render item tooltips from the list
        if (list != null && !list.children().isEmpty()) {
            for (Object entryObj : list.children()) {
                if (entryObj instanceof MyList.Entry) {
                    MyList.Entry entry = (MyList.Entry) entryObj;
                    ItemStack hoveredItem = entry.getHoveredItemStack();
                    if (hoveredItem != null && !hoveredItem.isEmpty()) {
                        List<Component> tooltipLines = hoveredItem.getTooltipLines(Item.TooltipContext.EMPTY, Minecraft.getInstance().player, TooltipFlag.ADVANCED);
                        int biggestLength = 0;
                        for (int i = 0; i < tooltipLines.size(); i++) {
                            biggestLength = Math.max(biggestLength, minecraft.font.width(tooltipLines.get(i)));
                        }
                        GraphicsHelper.drawTooltipBackground(graphics, mouseX + TooltipRenderUtil.MOUSE_OFFSET, (mouseY - TooltipRenderUtil.MOUSE_OFFSET), biggestLength, minecraft.font.lineHeight * tooltipLines.size());
                        for (int i = 0; i < tooltipLines.size(); i++) {
                            GraphicsHelper.drawString(graphics, minecraft.font, tooltipLines.get(i), mouseX + TooltipRenderUtil.MOUSE_OFFSET, (mouseY - TooltipRenderUtil.MOUSE_OFFSET) + (minecraft.font.lineHeight * i), 0xFFFFFFFF);
                        }
                        break; // Only show one tooltip at a time
                    } else {
                        String hoveredString = entry.getHoveredItemName();
                        if(hoveredString != null) {
                            GraphicsHelper.drawTooltipBackground(graphics, mouseX + TooltipRenderUtil.MOUSE_OFFSET, mouseY - TooltipRenderUtil.MOUSE_OFFSET, minecraft.font.width(hoveredString), minecraft.font.lineHeight);
                            GraphicsHelper.drawString(graphics, minecraft.font, hoveredString.toLowerCase().replace("_", " "), mouseX + TooltipRenderUtil.MOUSE_OFFSET, mouseY - TooltipRenderUtil.MOUSE_OFFSET, 0xFFFFFFFF);
                            
                        }
                    }
                }
            }
        }
    }

    public void openWithItem(String itemID) {
        final String item = itemID;
        ShopsHandler.shopsByItemAsync(item)
                .thenAccept(shops -> Minecraft.getInstance().execute(() -> {
                    for (int s = 0; s < shops.length; s++) {
                        Shop shop = shops[s];
                        for (int i = 0; i < customItems.length; i++) {
                            if(shop.currency.equals(customItems[i].convertedItemID)) {
                                shop.priceIsCustom = customItems[i];
                            }
                            if(shop.currency2 != null && shop.currency2.equals(customItems[i].convertedItemID)) {
                                shop.price2IsCustom = customItems[i];
                            }
                            if(shop.item.equals(customItems[i].convertedItemID)) {
                                shop.itemIsCustom = customItems[i];
                            }
                        }
                    }
                    list.removeAllWidgets();
                    for (int i = 0; i < shops.length; i++) {
                        final int index = i;
                        list.addWidget(new MyList.Entry(shops[i], i, 100 - MyList.SCROLLBAR_WIDTH, () -> {
                            displayedTrade = shops[index];
                        }, this));
                    }
                }));
    }

    public void openWithUsername(String username) {
        final String user = username;
        ShopsHandler.shopsByPlayerAsync(user)
            .thenAccept((shops) -> Minecraft.getInstance().execute(() -> {
                for (int s = 0; s < shops.length; s++) {
                    Shop shop = shops[s];
                    for (int i = 0; i < customItems.length; i++) {
                        if(shop.currency.equals(customItems[i].convertedItemID)) {
                            shop.priceIsCustom = customItems[i];
                        }
                        if(shop.currency2 != null && shop.currency2.equals(customItems[i].convertedItemID)) {
                            shop.price2IsCustom = customItems[i];
                        }
                        if(shop.item.equals(customItems[i].convertedItemID)) {
                            shop.itemIsCustom = customItems[i];
                        }
                    }
                }
                list.removeAllWidgets();
                for (int i = 0; i < shops.length; i++) {
                    final int index = i;
                    list.addWidget(new MyList.Entry(shops[i], i, 100 - MyList.SCROLLBAR_WIDTH, () -> {
                        displayedTrade = shops[index];
                    }, this));
                }
            })
        );
    }
}

// List for left side
class MyList extends ContainerObjectSelectionList<MyList.Entry> {

    public int currentHoverIndex;
    public int currentHoverItemIndex;

    public MyList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
        super(minecraft, width, height, y, itemHeight);
    }

    public void addWidget(Entry entry) {
        this.addEntry(entry);
    }

    public void removeWidget(Entry entry) {
        this.removeEntry(entry);
    }

    public void removeAllWidgets() {
        this.clearEntries();
    }
    
    @Override
    public int getRowWidth() {
        return 100 - (MyList.SCROLLBAR_WIDTH * 4);
    }

    

    // List buttons
    public static class Entry extends ContainerObjectSelectionList.Entry<Entry> {
        private Button button;
        public Shop shopTrade;
        // If any of the three items are unknown show...
        private final ResIdentifier UNKNOWN_ITEM = ResIdentifier.of("minecraft", "textures/gui/sprites/icon/unseen_notification.png");
        // Here's the arrows too, so we can go full villager-trade mode
        private final ResIdentifier TRADE_ARROW = ResIdentifier.of("minecraft", "textures/gui/sprites/container/villager/trade_arrow.png");
        private final ResIdentifier TRADE_ARROW_OOS = ResIdentifier.of("minecraft", "textures/gui/sprites/container/villager/trade_arrow_out_of_stock.png");

        public Runnable runWhenActive;
        public int index;
        private final ShopsScreen screen;
        
        // Tooltip tracking
        private ItemStack hoveredItemStack = null;
        private String hoveredItemName = null;
        private int lastMouseX = 0;
        private int lastMouseY = 0;

        public Entry(Shop shopTrade, int index, int width, Runnable runWhenActive, ShopsScreen screen) {
            this.shopTrade = shopTrade;
            Builder btn = Button.builder(Component.empty(), b -> {
                runWhenActive.run();
            }).width(width);
            /*MutableComponent tooltiptext = Component.literal(shopTrade.price + "x " + ShopsHandler.IDToPrettyName(shopTrade.currency));
            if(shopTrade.currency2 != null) tooltiptext = tooltiptext.append(Component.literal(" + " + shopTrade.price2 + "x " + ShopsHandler.IDToPrettyName(shopTrade.currency2)));
            tooltiptext = tooltiptext.append(Component.literal(" ➡ "));
            tooltiptext = tooltiptext.append(shopTrade.tradeAmount + "x " + ShopsHandler.IDToPrettyName(shopTrade.item));
            btn.tooltip(
                Tooltip.create(tooltiptext)
            );*/

            this.button = btn.build();
            this.runWhenActive = runWhenActive;
            this.index = index;
            this.screen = screen;
        }

        @Override
        public List<GuiEventListener> children() {
            return List.of(button);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(); // accessibility narration
        }

        //? if <26.1 {
        @Override
        public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float delta) {
        //?} else {
        /*@Override
        public void extractContent(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float delta) {*///?}
            int y = getContentY();
            button.setPosition(13, y);
            //? if <26.1 {
button.render(graphics, mouseX, mouseY, delta);
//?} else {
/*button.extractRenderState(graphics, mouseX, mouseY, delta);*///?}
            
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            hoveredItemStack = null;
            hoveredItemName = null;

            Optional<Holder.Reference<Item>> item;
            try {
                if(shopTrade.priceIsCustom != null) {
                    ItemStack is = new ItemStack(BuiltInRegistries.ITEM.get(ResIdentifier.parse("minecraft:" + shopTrade.priceIsCustom.originalItemID.toLowerCase()).get()).get(), shopTrade.tradeAmount);
                    is.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, shopTrade.priceIsCustom.customName);
                    is.set(DataComponents.LORE, new ItemLore(List.of(
                        Component.literal("✓").setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(true))
                        .append(Component.literal(" Genuine Item").setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withBold(false)))
                    )));
                    is.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
                    GraphicsHelper.renderItem(graphics, is, 17, y + 1);
                    if (isMouseOver(mouseX, mouseY, 17, y + 1, 16, 16)) {
                        hoveredItemStack = is;
                    }
                } else {
                    item = BuiltInRegistries.ITEM.get(ResIdentifier.parse("minecraft:" + shopTrade.currency.toLowerCase()).get());
                    if (item.isEmpty()) {
                        graphics.blit(RenderPipelines.GUI_TEXTURED, UNKNOWN_ITEM.get(), 17, y + 3, 0, 0, 12, 12, 12, 12);
                        if (isMouseOver(mouseX, mouseY, 17, y + 1, 16, 16)) {
                            hoveredItemName = shopTrade.currency;
                        }
                    } else {
                        ItemStack is = new ItemStack(item.get(), shopTrade.price);
                        GraphicsHelper.renderItem(graphics, is, 17, y + 1);
                        if (isMouseOver(mouseX, mouseY, 17, y + 1, 16, 16)) {
                            hoveredItemStack = is;
                        }
                    }
                }
            } catch(Exception e) {
                // If all else fails, I.E the $ in $LLAMACOIN just give the UNKNOWN_ITEM thingy
                
                graphics.blit(RenderPipelines.GUI_TEXTURED, UNKNOWN_ITEM.get(), 17, y + 3, 0, 0, 12, 12, 12, 12);
            }
            if(shopTrade.price > 1) GraphicsHelper.drawCenteredString(graphics, Minecraft.getInstance().font, shopTrade.price.toString(), 30, y + 10, 0xFFFFFFFF);

            if (shopTrade.currency2 != null) {
                // Lets go round again!
                try {
                    if(shopTrade.price2IsCustom != null) {
                        ItemStack is = new ItemStack(BuiltInRegistries.ITEM.get(ResIdentifier.parse("minecraft:" + shopTrade.price2IsCustom.originalItemID.toLowerCase()).get()).get(), shopTrade.tradeAmount);
                        is.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, shopTrade.price2IsCustom.customName);
                        is.set(DataComponents.LORE, new ItemLore(List.of(
                            Component.literal("✓").setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(true))
                            .append(Component.literal(" Genuine Item").setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withBold(false)))
                        )));
                        is.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
                        GraphicsHelper.renderItem(graphics, is, 37, y + 1);
                        if (isMouseOver(mouseX, mouseY, 37, y + 1, 16, 16)) {
                            hoveredItemStack = is;
                        }
                    } else {  
                        Optional<Holder.Reference<Item>> item2 = BuiltInRegistries.ITEM
                            .get(ResIdentifier.parse("minecraft:" + shopTrade.currency2.toLowerCase()).get());

                        if (item2.isEmpty()) {
                            graphics.blit(RenderPipelines.GUI_TEXTURED, UNKNOWN_ITEM.get(), 37, y + 3, 0, 0, 12, 12, 12, 12);
                            if (isMouseOver(mouseX, mouseY, 37, y + 1, 16, 16)) {
                                hoveredItemName = shopTrade.currency2;
                            }
                        } else {
                            ItemStack is2 = new ItemStack(item2.get(), shopTrade.price2);
                            GraphicsHelper.renderItem(graphics, is2, 37, y + 1);
                            if (isMouseOver(mouseX, mouseY, 37, y + 1, 16, 16)) {
                                hoveredItemStack = is2;
                            }
                        }
                    }
                } catch(Exception e) {
                    graphics.blit(RenderPipelines.GUI_TEXTURED, UNKNOWN_ITEM.get(), 37, y + 3, 0, 0, 12, 12, 12, 12);
                }
                if(shopTrade.price2 > 1) GraphicsHelper.drawCenteredString(graphics, Minecraft.getInstance().font, shopTrade.price2.toString(), 50, y + 10, 0xFFFFFFFF);
            }

            // Arrow intermission
            if (shopTrade.stock == 0) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, TRADE_ARROW_OOS.get(), 59, y + 4, 0, 0, 10, 10, 10, 10);
            } else {
                graphics.blit(RenderPipelines.GUI_TEXTURED, TRADE_ARROW.get(), 59, y + 4, 0, 0, 10, 10, 10, 10);
            }

            // (duh duh duh -duhhh) One More Time!
            try {
                if(shopTrade.itemIsCustom != null) {
                    ItemStack is = new ItemStack(BuiltInRegistries.ITEM.get(ResIdentifier.parse("minecraft:" + shopTrade.itemIsCustom.originalItemID.toLowerCase()).get()).get(), shopTrade.tradeAmount);
                    is.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, shopTrade.itemIsCustom.customName);
                    is.set(DataComponents.LORE, new ItemLore(List.of(
                        Component.literal("✓").setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(true))
                        .append(Component.literal(" Genuine Item").setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withBold(false)))
                    )));
                    is.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
                    GraphicsHelper.renderItem(graphics, is, 77, y + 1);
                    if (isMouseOver(mouseX, mouseY, 77, y + 1, 16, 16)) {
                        hoveredItemStack = is;
                    }
                } else {
                    Optional<Holder.Reference<Item>> item3 = BuiltInRegistries.ITEM
                        .get(ResIdentifier.parse("minecraft:" + shopTrade.item.toLowerCase()).get());
                    if (item3.isEmpty()) {
                        graphics.blit(RenderPipelines.GUI_TEXTURED, UNKNOWN_ITEM.get(), 77, y + 3, 0, 0, 12, 12, 12, 12);
                        if (isMouseOver(mouseX, mouseY, 77, y + 1, 16, 16)) {
                            hoveredItemName = shopTrade.item;
                        }
                    } else {
                        ItemStack is = new ItemStack(item3.get(), shopTrade.tradeAmount);
                        if(shopTrade.customName != null) is.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal(shopTrade.customName));
                        for (int i = 0; i < shopTrade.enchants.length; i++) {
                            try {
                                Holder<Enchantment> holder = Minecraft.getInstance().level.registryAccess()
                                        .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ResourceKey.create(
                                                Registries.ENCHANTMENT,
                                                ResIdentifier.parse(shopTrade.enchants[i].id).get()));
                                is.enchant(holder, shopTrade.enchants[i].level);
                            } catch (Exception e) {
                            }
                        }
                        GraphicsHelper.renderItem(graphics, is, 77, y + 1);
                        if (isMouseOver(mouseX, mouseY, 77, y + 1, 16, 16)) {
                            hoveredItemStack = is;
                        }
                    }
                }
            } catch(Exception e) {
                LogUtils.error("Error rendering item in list", e);
                graphics.blit(RenderPipelines.GUI_TEXTURED, UNKNOWN_ITEM.get(), 77, y + 3, 0, 0, 12, 12, 12, 12);
            }
            
            if(shopTrade.tradeAmount > 1) GraphicsHelper.drawCenteredString(graphics, Minecraft.getInstance().font, shopTrade.tradeAmount.toString(), 90, y + 10, 0xFFFFFFFF);
        }
        
        private boolean isMouseOver(int mouseX, int mouseY, int x, int y, int width, int height) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }

        public String getHoveredItemName() {
            return hoveredItemName;
        }
        
        public ItemStack getHoveredItemStack() {
            return hoveredItemStack;
        }
        
        public int getLastMouseX() {
            return lastMouseX;
        }
        
        public int getLastMouseY() {
            return lastMouseY;
        }

    }
}


