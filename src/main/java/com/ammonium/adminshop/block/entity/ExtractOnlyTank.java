package com.ammonium.adminshop.block.entity;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;

public class ExtractOnlyTank extends FluidTank {
    private final Runnable onFluidChange;
    public ExtractOnlyTank(int capacity, Runnable onFluidChange) {
        super(capacity);
        this.onFluidChange = onFluidChange;
    }
    @Override
    public int fill(FluidStack resource, FluidAction action) {
        // Prevent any external fluid insertion
        return 0;
    }
    protected int secureFill(FluidStack resource, FluidAction action) {
        int result = super.fill(resource, action);
        if (result > 0 && action.execute()) {
            onFluidChange.run();
        }
        return result;
    }
    @NotNull
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !resource.isFluidEqual(fluid)) {
            return FluidStack.EMPTY;
        }
        return drain(resource.getAmount(), action);
    }
    @NotNull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        FluidStack drained = super.drain(maxDrain, action);
        if (!drained.isEmpty() && action.execute()) {
            onFluidChange.run();
        }
        return drained;
    }
}

