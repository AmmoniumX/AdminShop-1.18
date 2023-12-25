package com.ammonium.adminshop.block.interfaces;

public interface Detector extends ShopMachine {
    void setThreshold(long threshold);
    long getThreshold();
}