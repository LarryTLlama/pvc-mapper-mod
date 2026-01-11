package larrytllama.pvcmappermod;

public class ShopsHandler {
    public void refetchShops() {

    }
}

class ShopsDataMetadata {
    String timeDumped;
    String link;
    String repeatSponsor;
}

// TODO: Add enchantment support
class ShopItemEnchant {

}

class ShopItem {
    String type;
    String name;
    ShopItemEnchant enchant;
    String[] lore;
    int amount;
}

class ShopRecipe {
    ShopItem item1;
    ShopItem item2;
    ShopItem resultItem;
    int stock;
}

class Shop {
    String shopName;
    String shopOwner;
    String location;
    String world;

}

class ShopsDataPackage {
    ShopsDataMetadata information;
    Shop[] data;
}