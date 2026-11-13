package larrytllama.pvcmappermod;
import larrytllama.pvcmappermod.utils.*;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.Locale;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class ShopsHandler {
    public static SuggestionProvider<FabricClientCommandSource> ITEM_SUGGESTIONS =
    (context, builder) -> {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (Item item : BuiltInRegistries.ITEM) {
            ResIdentifier id = ResIdentifier.of(BuiltInRegistries.ITEM.getKey(item));
            if (id != null) {
                String path = id.getPath();
                if (path.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                    String suggestion = path.toUpperCase(Locale.ROOT);
                    builder.suggest(suggestion);
                }
            }
        }

        List<String> specialItems = List.of("PLAYTIME_CERTIFICATE", "TERRA2_PLAYTIME_CERT", "INACTIVITY_CERTIFICATE", "VOTE_DIAMOND", "DONOR_VOUCHER",
            "SKIP_THE_QUEUE_TICKET", "BULK_TOTEM_VOUCHER", "INACTIVITY_TICKET", "NICKNAME_VOUCHER","HARD_SURVIVAL_TOKEN", "WOMP_WOMP", "SPONSOR_TOKEN", 
            "WRITER_QUILL", "EVENT_COIN", "TRICK_OR_TREAT_COIN", "GCU_COIN", "NERCHIUS_POOP", "THE_BIG_CLAIM", "SLOT_MACHINE_TOKEN", "VOUCHER_CREDITS_TOKEN",
            "LAPIS_CLAIM", "COAL_CLAIM", "REDSTONE_CLAIM", "EMERALD_CLAIM", "DIAMOND_CLAIM", "CHUNK_CLAIM", "HARD_SURVIVAL_CLAIM", "NOTCH_TEMPLE_APPLE", 
            "QUARRY_EXTRACTOR_FUEL", "NEPERO", "BANK_NOTE", "CADDOZZO", "$LLAMACOIN");

        for (String itemString : specialItems) {
            if (itemString.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                builder.suggest(itemString);
            }
        }
        return builder.buildFuture();
    };

    public static ArrayList<String> getAllItems() {
        ArrayList<String> items = new ArrayList<String>();
        items.addAll(List.of("PLAYTIME_CERTIFICATE", "TERRA2_PLAYTIME_CERT", "INACTIVITY_CERTIFICATE", "VOTE_DIAMOND", "DONOR_VOUCHER",
            "SKIP_THE_QUEUE_TICKET", "BULK_TOTEM_VOUCHER", "INACTIVITY_TICKET", "NICKNAME_VOUCHER","HARD_SURVIVAL_TOKEN", "WOMP_WOMP", "SPONSOR_TOKEN", 
            "WRITER_QUILL", "EVENT_COIN", "TRICK_OR_TREAT_COIN", "GCU_COIN", "NERCHIUS_POOP", "THE_BIG_CLAIM", "SLOT_MACHINE_TOKEN", "VOUCHER_CREDITS_TOKEN",
            "LAPIS_CLAIM", "COAL_CLAIM", "REDSTONE_CLAIM", "EMERALD_CLAIM", "DIAMOND_CLAIM", "CHUNK_CLAIM", "HARD_SURVIVAL_CLAIM", "NOTCH_TEMPLE_APPLE", 
            "QUARRY_EXTRACTOR_FUEL", "NEPERO", "BANK_NOTE", "CADDOZZO", "$LLAMACOIN"));

        for (Item item : BuiltInRegistries.ITEM) {
            ResIdentifier id = ResIdentifier.of(BuiltInRegistries.ITEM.getKey(item));
            if (id != null) {
                String itemm = id.getPath().toUpperCase(Locale.ROOT);
                items.add(itemm);
            }
        }
        Collections.sort(items);
        return items;
    }

    public static CompletableFuture<Shop[]> shopsByItemAsync(String item) {
        try {
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(new URI(NetworkUtils.BASE_URL + "/pvc-utils/tradesByItem/" + item))
                .GET().build();
            return NetworkUtils.HTTP_CLIENT.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        Gson gson = new GsonBuilder().registerTypeAdapter(Shop.class, new ShopDeserializer()).create();
                        Shop[] shops = gson.fromJson(response.body(), Shop[].class);
                        return shops;
                    }
                    return new Shop[0];
                })
                .exceptionally(e -> {
                    e.printStackTrace();
                    return new Shop[0];
                });
        } catch(Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(new Shop[0]);
        }
    }

    public static CompletableFuture<Shop[]> shopsByPlayerAsync(String username) {
        try {
            LogUtils.debug("Fetching shops for " + username);
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(new URI(NetworkUtils.BASE_URL + "/pvc-utils/tradesByPlayer/" + username))
                .GET().build();
            return NetworkUtils.HTTP_CLIENT.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        Gson gson = new GsonBuilder().registerTypeAdapter(Shop.class, new ShopDeserializer()).create();
                        Shop[] shops = gson.fromJson(response.body(), Shop[].class);
                        LogUtils.debug(shops.length);
                        return shops;
                    }
                    return new Shop[0];
                })
                .exceptionally(e -> {
                    LogUtils.error("Failed to fetch shops by player", e);
                    e.printStackTrace();
                    return new Shop[0];
                });
        } catch(Exception e) {
            LogUtils.error("Failed to fetch shops by player", e);
            e.printStackTrace();
            return CompletableFuture.completedFuture(new Shop[0]);
        }
    }

    // Possible use later. I'll see if I fancy it
    public static CompletableFuture<ShopsPlayerStats> shopsStatsByPlayerAsync(String username) {
        try {
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(new URI(NetworkUtils.BASE_URL + "/pvc-utils/playerStats/" + username))
                .GET().build();
            return NetworkUtils.HTTP_CLIENT.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        Gson gson = new Gson();
                        ShopsPlayerStats stats = gson.fromJson(response.body(), ShopsPlayerStats.class);
                        return stats;
                    }
                    return new ShopsPlayerStats();
                })
                .exceptionally(e -> {
                    e.printStackTrace();
                    return new ShopsPlayerStats();
                });
        } catch(Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(new ShopsPlayerStats());
        }
    }

    public static String IDToPrettyName(String itemID) {
        String allLowerCase = itemID.replaceAll("_", " ").toLowerCase();
        return allLowerCase.substring(0, 1).toUpperCase() + allLowerCase.substring(1);
    }

    public static AdminShopItem parseAdminShopItem(ItemStack item) {
        if(item == null || item.isEmpty()) return null;
        AdminShopItem asi = new AdminShopItem();
        asi.amount = item.getCount();
        if(item.getCustomName() != null) asi.name = item.getCustomName().getString();
        asi.type = BuiltInRegistries.ITEM.getKey(item.getItem()).getPath().toUpperCase();
        if(asi.type.contains("SHULKER_BOX")) {
            //? if <26.1 {
            List<ItemStack> items = item.get(DataComponents.CONTAINER).stream().toList();
            asi.lore = new String[items.size()];
            int i=0;
            for (ItemStack itemm : items) {
                asi.lore[i] = "- " + itemm.getCount() + "x " + BuiltInRegistries.ITEM.getKey(itemm.getItem()).getPath().toUpperCase();
                i++;
            }
            //?} else {
            /*
            if(item.get(DataComponents.CONTAINER) != null) {
                int i=0;
                asi.lore = new String[(int)item.get(DataComponents.CONTAINER).nonEmptyItemCopyStream().count()];
                for (var template : item.get(DataComponents.CONTAINER).nonEmptyItems()) {
                    asi.lore[i] = "- " + template.count() + "x " + BuiltInRegistries.ITEM.getKey(template.item().value()).getPath().toUpperCase(Locale.ROOT);
                    i++;
                }
            }
            */
            //?}
        } else {
            List<Component> lines = item.get(DataComponents.LORE).lines();
            asi.lore = new String[lines.size()];
            int i = 0;
            for (Component line : lines) {
                asi.lore[i] = line.getString();
                i++;
            }
        }
        asi.enchant = new JsonObject();
        item.getEnchantments().entrySet().forEach((entry) -> {
            String id = entry.getKey().getRegisteredName();
            asi.enchant.addProperty(id, entry.getIntValue());
        });
        return asi;
    }
    
    public static AdminShop decodeShops(MerchantScreen merchantScreen, Entity entity) {
        AdminShop as = new AdminShop();
        as.entity = entity.getType().toString();
        as.shopName = merchantScreen.getTitle().getString();
        as.location = String.format("%d, %d, %d", entity.getBlockX(), entity.getBlockY(), entity.getBlockZ());
        as.world = CompatUtils.getIdentifier(entity.level().dimension()).toString();
        MerchantOffers mos = merchantScreen.getMenu().getOffers();
        as.recipes = new AdminShopRecipe[mos.size()];
        int i = 0;
        for (MerchantOffer mo : mos) {
            as.recipes[i] = new AdminShopRecipe();
            as.recipes[i].item1 = parseAdminShopItem(mo.getCostA());
            as.recipes[i].item2 = parseAdminShopItem(mo.getCostB());
            as.recipes[i].resultItem = parseAdminShopItem(mo.getResult());
            i=i+1;
        }
        return as;
    }

    public static CompletableFuture<CustomItem[]> getCustomItems() {
        try {
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(new URI(NetworkUtils.API_V2 + "/custom-items"))
                .GET().build();
            LogUtils.debug("Fetching custom item dict from PVC Mapper: " + request.uri().toString());
            return NetworkUtils.HTTP_CLIENT.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        Gson gson = new Gson();
                        PreProcessedCustomItem[] preitems = gson.fromJson(response.body(), PreProcessedCustomItem[].class);
                        CustomItem[] items = new CustomItem[preitems.length];
                        for (int i = 0; i < preitems.length; i++) {
                            MutableComponent comp = Component.literal("");
                            for (int i2 = 0; i2 < preitems[i].new_text.length; i2++) {
                                MutableComponent newText = Component.literal(preitems[i].new_text[i2].text);
                                Style newStyle = Style.EMPTY;
                                if(preitems[i].new_text[i2].bold != null) newStyle = newStyle.withBold(preitems[i].new_text[i2].bold);
                                if(preitems[i].new_text[i2].italic != null) newStyle = newStyle.withItalic(preitems[i].new_text[i2].italic);
                                if(preitems[i].new_text[i2].obfuscated != null) newStyle = newStyle.withObfuscated(preitems[i].new_text[i2].obfuscated);
                                if(preitems[i].new_text[i2].underlined != null) newStyle = newStyle.withUnderlined(preitems[i].new_text[i2].underlined);
                                if(preitems[i].new_text[i2].strikethrough != null) newStyle = newStyle.withStrikethrough(preitems[i].new_text[i2].strikethrough);
                                // Enum.valueOf requires an exact uppercase match (unlike the removed getByName method).
                                if(preitems[i].new_text[i2].colour != null) newStyle = newStyle.withColor(ChatFormatting.valueOf(preitems[i].new_text[i2].colour.toUpperCase()));
                                // Game is really weird and makes us use it like this:
                                // Component.literal("").append(Component.literal("First text that's").append(Component.literal("in a different")).append(Component.literal("Colour")))
                                comp = comp.append(newText.setStyle(newStyle));
                            }
                            items[i] = new CustomItem(preitems[i].old_item_id, preitems[i].new_item_id, comp);
                        }
                        return items;
                    }
                    return new CustomItem[0];
                })
                .exceptionally(e -> {
                    e.printStackTrace();
                    return new CustomItem[0];
                });
        } catch(Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(new CustomItem[0]);
        }
    }
}

class PreProcessedCustomItemText {
    Boolean bold;
    Boolean italic;
    Boolean underlined;
    Boolean obfuscated;
    Boolean strikethrough;
    String text;
    String colour;
}

class PreProcessedCustomItem {
    String new_item_id;
    String old_item_id;
    PreProcessedCustomItemText[] new_text;
}

class CustomItem {
    CustomItem(String oiid, String ciid, Component cn) {
        this.customName = cn;
        this.originalItemID = oiid;
        this.convertedItemID = ciid;
    }

    Component customName;
    String originalItemID;
    String convertedItemID;
}

class ShopDeserializer implements JsonDeserializer<Shop> {
    @Override
    public Shop deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
            throws JsonParseException {

        JsonObject obj = json.getAsJsonObject();
        Shop shop = new Shop();
        LogUtils.debug("Applying properties");
        shop.currency = obj.get("currency").getAsString();
        shop.currency2 = obj.get("currency2").isJsonNull() ? null : obj.get("currency2").getAsString();
        shop.customName = obj.get("customName").isJsonNull() ? null : obj.get("customName").getAsString();
        shop.inShulker = obj.get("inShulker").getAsBoolean();
        shop.item = obj.get("item").getAsString();
        shop.location = obj.get("location").getAsString();
        shop.normalisedPrice = obj.get("normalizedPrice").getAsDouble();
        shop.price = obj.get("price").getAsInt();
        shop.price2 = obj.get("price2").isJsonNull() ? null : obj.get("price2").getAsInt();
        shop.shopHash = obj.get("shopHash").getAsString();
        shop.shopName = obj.get("shopName").getAsString();
        shop.shopOwner = obj.get("shopOwner").getAsString();
        shop.stock = obj.get("stock").getAsInt();
        shop.tradeAmount = obj.get("tradeAmount").getAsInt();
        LogUtils.debug("Applying enchantments");
        // Get enchants as ShopEnchants
        try {
            JsonArray enchants = obj.get("enchants").getAsJsonArray();
            shop.enchants = new ShopEnchants[enchants.size()];
            LogUtils.debug("Applying " + enchants.size() + " enchantments");
            // Put em all in!
            for (int se = 0; se < enchants.size(); se++) {
                JsonObject enchant = enchants.get(se).getAsJsonObject();
                String enchantID = enchant.keySet().iterator().next();
                int level = enchant.get(enchantID).getAsInt();
                LogUtils.debug("Parsed enchant - ID: " + enchantID + ", Level: " + level);
                shop.enchants[se] = new ShopEnchants();
                shop.enchants[se].id = enchantID;
                shop.enchants[se].level = level;
                LogUtils.debug("Set enchant - ID: " + shop.enchants[se].id + ", Level: " + shop.enchants[se].level);
            }
        } catch(Exception e) {
            shop.enchants = new ShopEnchants[0];
        }

        return shop;
    }
}

class ShopEnchants {
    String id;
    int level;
}

class Shop {
    String currency;
    String currency2;
    String customName;
    Boolean inShulker;
    String item;
    String location;
    double normalisedPrice;
    Integer price;
    Integer price2;
    String shopHash;
    String shopName;
    String shopOwner;
    Integer stock;
    Integer tradeAmount;
    ShopEnchants[] enchants;
    CustomItem priceIsCustom;
    CustomItem price2IsCustom;
    CustomItem itemIsCustom;
}

class ShopsPlayerStats {
    double inStockRatio;
    Integer shops;
    Integer trades;
    Integer uniqueItemsOnSale;
}

class AdminShop {
    String shopName;
    String location;
    String world;
    AdminShopRecipe[] recipes;
    String entity;
}

class AdminShopRecipe {
    AdminShopItem item1;
    AdminShopItem item2;
    AdminShopItem resultItem;
    Integer stock;
}

class AdminShopItem {
    String type;
    String name;
    String[] lore;
    Integer amount;
    JsonObject enchant;
}
