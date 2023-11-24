package com.ammonium.adminshop.block.interfaces;

import com.ammonium.adminshop.shop.ShopItem;

public interface BuyerMachine extends ShopMachine {
    void setTargetShopItem(ShopItem item);
}
