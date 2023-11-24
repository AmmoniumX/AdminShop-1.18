package com.ammonium.adminshop.block.entity;

import com.ammonium.adminshop.shop.Shop;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.IReverseTag;
import net.minecraftforge.registries.tags.ITagManager;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class InsertSellableOnlyTank extends FluidTank {
    private final Runnable onFluidChange;
    public InsertSellableOnlyTank(int capacity, Runnable onFluidChange) {
        super(capacity);
        this.onFluidChange = onFluidChange;
    }
    @Override
    public int fill(FluidStack resource, FluidAction action) {
        Fluid fluid = resource.getFluid();
        // First check if in sell fluid map
        boolean isValidFluid = Shop.get().hasSellShopFluid(fluid);
        // Then check if fluid tags in fluid tag map
        if (!isValidFluid) {
            ITagManager<Fluid> fluidTagManager = ForgeRegistries.FLUIDS.tags();
            assert fluidTagManager != null;
            Optional<IReverseTag<Fluid>> oFluidReverseTag = fluidTagManager.getReverseTag(fluid);
            if (oFluidReverseTag.isPresent()) {
                IReverseTag<Fluid> fluidReverseTag = oFluidReverseTag.get();
                Optional<TagKey<Fluid>> oFluidTag = fluidReverseTag.getTagKeys().filter(fluidTag -> Shop.get().hasSellShopFluidTag(fluidTag)).findFirst();
                isValidFluid = oFluidTag.isPresent();
            }
        }
        int result = 0;
        if (isValidFluid) {
            result = super.fill(resource, action);
        }
        if (result > 0 && action.execute()) {
            onFluidChange.run();
        }
        return result;
    }
    @NotNull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        // prevent fluid draining
        return FluidStack.EMPTY;
    }
    @NotNull
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        // prevent fluid draining
        return FluidStack.EMPTY;
    }
    protected FluidStack secureDrain(FluidStack resource, FluidAction action) {
        FluidStack result = super.drain(resource.getAmount(), action);
        if (!result.isEmpty() && action.execute()) {
            onFluidChange.run();
        }
        return result;
    }
}

