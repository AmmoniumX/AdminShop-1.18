package com.ammonium.adminshop.shop;

import com.ammonium.adminshop.AdminShop;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;

/**
 * Holder class for something that is buyable or sellable in the shop.
 * Can be an Item or Fluid, or an item or fluid Tag.
 * Cannot have a tag as buyable
 */
public class ShopItem {

    private boolean isBuy;
    private boolean isItem; // true = item, false = fluid
    private boolean isTag; // true = (item/fluid)Tag, false = (item/fluid)Stack, only when (!isBuy)
    private long price;
    private int permitTier;
    private ItemStack item; // only when (isItem)
    private FluidStack fluid; // only when (!isItem)
    private ResourceLocation resourceLocation; // location of either item or fluid, only when (!isTag)
    private TagKey<Item> itemTag; // only when (isItem && !isBuy)
    private TagKey<Fluid> fluidTag; // only when (isFluid && !isBuy)
    private CompoundTag nbt; // only when (isBuy && isItem && !isTag)

    private ShopItem(){

    }

    public ItemStack getItem(){
        // Search in item registry if not found
        if (item == null || item.isEmpty()) {
            if(!isTag) {
                item = new ItemStack(ForgeRegistries.ITEMS.getValue(resourceLocation), 1);
                item.setTag(nbt);
            }else{
                itemTag = ItemTags.create(resourceLocation);
                Optional<Item> oitem = ForgeRegistries.ITEMS.getValues().stream()
                        .filter(i -> new ItemStack(i).is(itemTag))
                        .findFirst();
                item = new ItemStack(oitem.orElse(null));
            }
            if (item.isEmpty()) {
                if (!isTag) {
                    AdminShop.LOGGER.error("No item found with location: " + resourceLocation);
                } else {
                    AdminShop.LOGGER.error("No item found with tag: " + itemTag.location());
                }
            }
        }
//        AdminShop.LOGGER.debug("Returning ItemStack: "+item.getDisplayName().getString()+", nbt:"+(hasNBT() ? nbt.toString() : "false."));
        return item;
    }
    public FluidStack getFluid(){
        // Search in item registry if not found
        if (fluid == null || fluid.isEmpty()) {
            if(!isTag){
                Fluid rfluid = ForgeRegistries.FLUIDS.getValue(resourceLocation);
                if (rfluid == null) {
                    fluid = FluidStack.EMPTY;
                } else {
                    fluid = new FluidStack(rfluid, 1, nbt);
                }
            }else{
                fluidTag = FluidTags.create(resourceLocation);
                Optional<Fluid> oFluid = ForgeRegistries.FLUIDS.getValues().stream()
                        .filter(f ->
                                ForgeRegistries.FLUIDS.getHolder(f).map(fluidHolder -> fluidHolder.is(fluidTag)).orElse(false)
                        ).findAny();
                fluid = oFluid.map(value -> new FluidStack(value, 1)).orElse(FluidStack.EMPTY);
            }
            if (fluid.isEmpty()) {
                if (!isTag) {
                    AdminShop.LOGGER.error("No fluid found with location: " + resourceLocation);
                } else {
                    AdminShop.LOGGER.error("No fluid found with tag: " + fluidTag.location());
                }
            }
        }
        return fluid;
    }
    public long getPrice() {return price;}
    public int getPermitTier() {return permitTier;}
    public boolean isBuy() {return isBuy;}
    public boolean isItem() {return isItem;}
    public boolean isTag() {return isTag;}
    public boolean hasNBT() {return (nbt != null && !nbt.isEmpty());}
    public TagKey<Item> getItemTag() {return itemTag;}
    public TagKey<Fluid> getFluidTag() {return fluidTag;}
    public CompoundTag getNbt() {
        return nbt;
    }

    static class Builder{
        private final ShopItem instance;

        public Builder(){
            instance = new ShopItem();
        }

        public Builder setIsBuy(boolean b){
            instance.isBuy = b;
            return this;
        }

        public Builder setIsItem(boolean b){
            instance.isItem = b;
            return this;
        }

        public Builder setIsTag(boolean b){
            instance.isTag = b;
            return this;
        }

        public Builder setPrice(long p) {
            instance.price = p;
            return this;
        }

        public Builder setPermitTier(int t) {
            instance.permitTier = t;
            return this;
        }

        public Builder setData(String resourceString, CompoundTag nbt){
            ResourceLocation resource = new ResourceLocation(resourceString);
            instance.resourceLocation = resource;
            if(instance.isItem) {
                if(!instance.isTag) {
                    Item item = ForgeRegistries.ITEMS.getValue(resource);
                    if (item == null) {
                        AdminShop.LOGGER.error("Error creating ShopItem: Item \""+resource+"\" not found!");
                    }
                    instance.item = new ItemStack(item, 1);
                    instance.item.setTag(nbt);
                    instance.nbt = nbt;
                    StringBuilder debugMessage = new StringBuilder("Created ItemStack: ");
                    debugMessage.append(instance.item.getDisplayName().getString()).append(", ").append(instance.item.getCount()).append(", ");
                    if (instance.item.getTag() != null) { debugMessage.append(instance.item.getTag()); }
                    AdminShop.LOGGER.debug(debugMessage.toString());
                }else{
                    instance.itemTag = ItemTags.create(resource);
                    Optional<Item> oItem = ForgeRegistries.ITEMS.getValues().stream()
                            .filter(i -> new ItemStack(i).is(instance.itemTag))
                            .findFirst();
                    if (oItem.isEmpty()) {
                        AdminShop.LOGGER.error("Error creating ShopItem: Item tag \""+resource+"\" not found!");
                    }
                    instance.item = new ItemStack(oItem.orElse(null));
//                    AdminShop.LOGGER.info("Item stack item: "+instance.item.getDisplayName().getString());
                    instance.nbt = nbt;
                }
            }else{
                if(!instance.isTag){
                    Fluid fluid = ForgeRegistries.FLUIDS.getValue(resource);
                    if (fluid != null) {
                        instance.fluid = new FluidStack(fluid, 1, nbt);
                    } else {
                        AdminShop.LOGGER.error("Error creating ShopItem: Fluid \""+resource+"\" not found!");
                        instance.fluid = FluidStack.EMPTY;
                    }
                }else{
                    instance.fluidTag = FluidTags.create(resource);
                    Optional<Fluid> oFluid = ForgeRegistries.FLUIDS.getValues().stream()
                            .filter(f ->
                                    ForgeRegistries.FLUIDS.getHolder(f).map(fluidHolder -> fluidHolder.is(instance.fluidTag)).orElse(false)
                            ).findAny();
                    if (oFluid.isPresent()) {
                        instance.fluid = new FluidStack(oFluid.get(), 1);
                    } else {
                        AdminShop.LOGGER.error("Error creating ShopItem: Fluid tag \""+resource+"\" not found!");
                        instance.fluid = FluidStack.EMPTY;
                    }
                }
                instance.nbt = nbt;
            }
            return this;
        }

        public ShopItem build(){
            return instance;
        }
    }

    /**
     *
     * @return Display name of the item/fluid contained in this
     */
    public String toString(){
        if(isItem && !isTag)    //Item
            // Remove square brackets from item name
            return ((item.getDisplayName().getString().startsWith("[") && item.getDisplayName().getString().endsWith("]")) ?
                    item.getDisplayName().getString().substring(1, item.getDisplayName().getString().length()-1) :
                    item.getDisplayName().getString());
        else if(isItem)         //Item Tag
            return I18n.get("gui.item_tag") + itemTag.location();
        else if(!isTag)         //Fluid
            return fluid.getDisplayName().getString();
        else                    //Fluid Tag
            return I18n.get("gui.fluid_tag") + fluidTag.location();
    }
}
