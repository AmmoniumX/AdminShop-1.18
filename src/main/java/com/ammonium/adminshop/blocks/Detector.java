package com.ammonium.adminshop.blocks;

public interface Detector extends ShopMachine {
    void setThreshold(long threshold);
    long getThreshold();
}
