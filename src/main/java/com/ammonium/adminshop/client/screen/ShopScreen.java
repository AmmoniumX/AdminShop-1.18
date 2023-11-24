package com.ammonium.adminshop.client.screen;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.block.ShopBlock;
import com.ammonium.adminshop.client.gui.BuySellButton;
import com.ammonium.adminshop.client.gui.ChangeAccountButton;
import com.ammonium.adminshop.client.gui.SetDefaultAccountButton;
import com.ammonium.adminshop.client.gui.ShopButton;
import com.ammonium.adminshop.money.BankAccount;
import com.ammonium.adminshop.money.ClientLocalData;
import com.ammonium.adminshop.money.MoneyFormat;
import com.ammonium.adminshop.network.MojangAPI;
import com.ammonium.adminshop.network.PacketAccountAddPermit;
import com.ammonium.adminshop.network.PacketBuyRequest;
import com.ammonium.adminshop.network.PacketSellRequest;
import com.ammonium.adminshop.setup.ClientConfig;
import com.ammonium.adminshop.setup.Messages;
import com.ammonium.adminshop.shop.Shop;
import com.ammonium.adminshop.shop.ShopItem;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.IReverseTag;
import net.minecraftforge.registries.tags.ITagManager;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ShopScreen extends AbstractContainerScreen<ShopMenu> {
    private final ResourceLocation GUI = new ResourceLocation(AdminShop.MODID, "textures/gui/shop_gui.png");
    private final String GUI_BUY = "gui.buy";
    private final String GUI_SELL = "gui.sell";
    private final String GUI_MONEY = "gui.money_message";
    private final String playerUUID;
    private static final int NUM_ROWS = 4, NUM_COLS = 9;
    private static final int SHOP_BUTTON_X = 16;
    private static final int SHOP_BUTTON_Y = 33;
    private static final int SHOP_BUTTON_SIZE = 18;
    private int rows_passed = 0;
    private final ShopMenu shopMenu;
    private final List<ShopButton> buyButtons;
    private final List<ShopButton> sellButtons;
    private List<ShopItem> searchResults;
    private boolean isBuy; //Whether the Buy option is currently selected
    private Map<Pair<String, Integer>, BankAccount> accountMap;
    private final List<Pair<String, Integer>> usableAccounts = new ArrayList<>();
    private int usableAccountsIndex;
    private ChangeAccountButton changeAccountButton;
    private SetDefaultAccountButton setDefaultAccountButton;
    private BuySellButton buySellButton;
    private EditBox searchBar;
    private int tickCounter = 0;
    private String search = "";
    private final Pair<String, Integer> personalAccount;
    private int relX, relY;

    public ShopScreen(ShopMenu container, Inventory inv, Component name) {
        super(container, inv, name);
        Minecraft mc = Minecraft.getInstance();
        assert mc.player != null;
        assert mc.level != null;
        assert mc.level.isClientSide;
        
        Player player = mc.player;
        this.playerUUID = player.getStringUUID();
        this.personalAccount = Pair.of(playerUUID, 1);
        this.accountMap = ClientLocalData.getAccountMap();

        if (!this.accountMap.containsKey(personalAccount)) {
            AdminShop.LOGGER.warn("Couldn't find personal account, creating one.");
            AdminShop.LOGGER.warn(personalAccount.getKey()+":"+personalAccount.getValue());
            BankAccount personalBankAccount = ClientLocalData.addAccount(new BankAccount(personalAccount.getKey(),
                    personalAccount.getValue()));
            // Refresh account map
            this.accountMap = ClientLocalData.getAccountMap();
        }
        this.usableAccounts.clear();
        ClientLocalData.getUsableAccounts().forEach(account -> this.usableAccounts.add(Pair.of(account.getOwner(),
                account.getId())));

        // Get default account
        Pair<String, Integer> currentAccount = ClientConfig.getDefaultAccount();
        this.usableAccountsIndex = findUsableAccountIndex(currentAccount);
        AdminShop.LOGGER.debug("Set default account to "+currentAccount.getKey()+":"+currentAccount.getValue());

        this.shopMenu = container;
        this.imageWidth = 195;
        this.imageHeight = 222;
        this.tickCounter = 0;
        this.search = "";

        buyButtons = new ArrayList<>();
        sellButtons = new ArrayList<>();

        isBuy = true;
    }

    // Finds index in usableAccounts of account
    private int findUsableAccountIndex(String accOwner, int accId) {
        for (int i = 0; i < this.usableAccounts.size(); i++) {
            Pair<String, Integer> account = this.usableAccounts.get(i);
            if (account.getKey().equals(accOwner) && account.getValue().equals(accId)) {
                return i;
            }
        }
        AdminShop.LOGGER.error("No account "+accOwner+":"+accId+" found in usableAccounts, fallback to personal account");

        // Fallback to personal account
        int personalAccountIndex = this.usableAccounts.indexOf(this.personalAccount);
        if (personalAccountIndex != -1) {
            return personalAccountIndex;
        } else if (!this.usableAccounts.isEmpty()){
            AdminShop.LOGGER.error("Personal account not found in usableAccounts, fallback to first usable account");
            return 0;
        } else {
            AdminShop.LOGGER.error("usableAccounts is empty!");
            return -1;
        }
    }

    private int findUsableAccountIndex(Pair<String, Integer> account) {
        return findUsableAccountIndex(account.getKey(), account.getValue());
    }

    // Update and save the default account to the client config
    public void setDefaultAccount(Pair<String, Integer> account) {
        // Update ClientConfig
        ClientConfig.setDefaultAccount(account);
        // Update the usableAccountsIndex with the new values
        this.usableAccountsIndex = findUsableAccountIndex(account);
    }

    private Pair<String, Integer> getAccountDetails() {
        if (usableAccountsIndex == -1) {
            AdminShop.LOGGER.error("Account isn't properly set!");
            return this.personalAccount;
        }
        return this.usableAccounts.get(this.usableAccountsIndex);
    }

    private BankAccount getBankAccount() {
        return ClientLocalData.getAccountMap().get(getAccountDetails());
    }

//    @SuppressWarnings("resource")
    @Override
    protected void init() {
        super.init();
        relX = (this.width - this.imageWidth) / 2;
        relY = (this.height - this.imageHeight) / 2;
        createShopButtons(false, relX, relY);
        createShopButtons(true, relX, relY);
        createBuySellButton(relX, relY);
        createChangeAccountButton(relX, relY);
        createSetDefaultAccountButton(relX, relY);
        createSearchBar(relX, relY);
        refreshShopButtons();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks){
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.searchBar.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        this.tickCounter++;
        if (this.tickCounter > 20) {
            this.tickCounter = 0;
            filterSearch();
        }
        // Render scroll indicators
        int max_rows_passed = (int) Math.max(Math.ceil(searchResults.size() / (double) NUM_COLS) - 4, 0);
//        AdminShop.LOGGER.debug("rows_passed:"+rows_passed+", max_rows_passed:"+max_rows_passed+", searchResults.size:"+searchResults.size());
//        AdminShop.LOGGER.debug("relX:"+relX+", relY:"+relY);
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        RenderSystem.setShaderTexture(0, GUI);
        pose.translate(0, 0, 300);
        // Top scroll indicator
        if (rows_passed > 0) {
            guiGraphics.blit(GUI, relX+15, relY+32, 15, 223, 162, 8);
        }
        // Bottom scroll indicator
        if (rows_passed < max_rows_passed) {
            guiGraphics.blit(GUI, relX+15, relY+96, 15, 232, 162, 8);
        }
        pose.popPose();
    }

    private void filterSearch() {
        String currSearch = searchBar.getValue();
        if (this.search.equals(currSearch)) {
            return;
        }
        this.search = currSearch;
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        // reset rows passed
        rows_passed = 0;
        createShopButtons(this.isBuy, relX, relY);
        refreshShopButtons();
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PoseStack pose = guiGraphics.pose();
        Font font = Minecraft.getInstance().font;
        pose.pushPose();
        //Block Title
        String blockName = I18n.get(ShopBlock.SCREEN_ADMINSHOP_SHOP);
        guiGraphics.drawCenteredString(font, blockName, getXSize()/2, 6, 0xffffff);

        //Player Inventory Title
        guiGraphics.drawString(font, playerInventoryTitle, 16, getYSize()-94, 0xffffff);

        //Player Balance
        BankAccount selectedAccount = getBankAccount();
        Pair<String, Integer> selectedAccountInfo = Pair.of(selectedAccount.getOwner(), selectedAccount.getId());
        long money = ClientLocalData.getMoney(selectedAccountInfo);
        NumberFormat numberFormat = NumberFormat.getInstance();
        String formatted = Screen.hasAltDown() ? MoneyFormat.forcedFormat(money, MoneyFormat.FormatType.RAW) :
                MoneyFormat.forcedFormat(money, MoneyFormat.FormatType.SHORT);
        guiGraphics.drawString(font,
                I18n.get(GUI_MONEY) + formatted,
                getXSize() - font.width(I18n.get(GUI_MONEY) + formatted) - 6,
                6, 0xffffff); //x, y, color

        // Bank account
        guiGraphics.drawString(font, MojangAPI.getUsernameByUUID(selectedAccountInfo.getKey())+":"+
                selectedAccountInfo.getValue(),16,112,0xffffff);

        //Tooltip for item the player is hovering over
        List<ShopButton> shopButtons = isBuy ? buyButtons : sellButtons;
        Optional<ShopButton> button = shopButtons.stream().filter(b -> b.isMouseOn).findFirst();
        button.ifPresent(shopButton -> guiGraphics.renderTooltip(font, shopButton.getTooltipContent(),
                Optional.empty(), mouseX-(this.width - this.imageWidth)/2,
                mouseY-(this.height - this.imageHeight)/2));
        pose.popPose();

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        assert minecraft != null;
        Slot slot = this.getSlotUnderMouse();
        if (slot != null && Screen.hasShiftDown()) {
            ItemStack itemStack = slot.getItem();
            if (!itemStack.isEmpty()) {
                // Get item clicked on
                Item item = itemStack.getItem();
//                System.out.println("Item clicked on: "+ ForgeRegistries.ITEMS.getKey(item));
                // Check if item is trade permit
                if (ForgeRegistries.ITEMS.getKey(item).toString().equals("adminshop:permit")) {
                    // Check if it has a “key” value
                    if (itemStack.hasTag()) {
                        CompoundTag compoundTag = itemStack.getTag();
                        int key = compoundTag.getInt("key");
                        System.out.println("Key: "+key);
                        // check if key is valid
                        if (key == 0) {
                            AdminShop.LOGGER.error("Trade permit has invalid key!");
                        }
                        // Add permit tier to bank account
                        AdminShop.LOGGER.info("Adding permit "+key+" to account");
//                        Minecraft.getInstance().player.sendSystemMessage(Component.literal("Adding permit "+key+" to account"),
//                                Minecraft.getInstance().player.getUUID());
//                        Minecraft.getInstance().player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                        Messages.sendToServer(new PacketAccountAddPermit(this.usableAccounts.get(this.usableAccountsIndex),
                                key, slot.getSlotIndex()));
                        return false;
                    }
                }
                // Check if item is fluid container
                itemStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(fluidHandler -> {
                    FluidStack fluidStack = fluidHandler.getFluidInTank(0);
                    // Return if container is empty
                    if (fluidStack.isEmpty()) {return;}
                    Fluid fluid = fluidHandler.getFluidInTank(0).getFluid();
                    // Check if fluid is in fluid shop map
                    if (Shop.get().hasSellShopFluid(fluid)) {
                        AdminShop.LOGGER.debug("ShopItem: "+fluidStack.getDisplayName().getString());
                        Messages.sendToServer(new PacketSellRequest(getBankAccount(), slot.getSlotIndex(), fluidStack.getAmount()));
                        return;
                    }
                    // Check if fluid tags are in fluid tag map
                    ITagManager<Fluid> fluidTagManager = ForgeRegistries.FLUIDS.tags();
                    Optional<IReverseTag<Fluid>> oFluidReverseTag = fluidTagManager.getReverseTag(fluid);
                    if (oFluidReverseTag.isEmpty()) {return;}
                    IReverseTag<Fluid> fluidReverseTag = oFluidReverseTag.get();
                    Optional<TagKey<Fluid>> oFluidTag = fluidReverseTag.getTagKeys().filter(fluidTag -> Shop.get().hasSellShopFluidTag(fluidTag)).findFirst();
                    if (oFluidTag.isPresent()) {
                        // Attempt to sell tag
                        AdminShop.LOGGER.debug("ShopItem: "+oFluidTag.get().location());
                        Messages.sendToServer(new PacketSellRequest(getBankAccount(), slot.getSlotIndex(), fluidStack.getAmount()));
                    }
                });
                // Check if item is in sell item map
                if (Shop.get().hasSellShopItem(item)) {
                    ShopItem shopItem = Shop.get().getShopSellItemMap().get(item);
                    // Attempt to sell it
                    AdminShop.LOGGER.debug("ShopItem: "+shopItem.getItem().getDisplayName().getString());
                    Messages.sendToServer(new PacketSellRequest(getBankAccount(), slot.getSlotIndex(), itemStack.getCount()));
                    return false;
                }
                // Check if any of item's tags is in sell item tag map
                Optional<TagKey<Item>> searchTag = itemStack.getTags().filter(itemTag -> Shop.get().hasSellShopItemTag(itemTag)).findFirst();
                if (searchTag.isPresent()) {
                    // Attempt to sell tag
                    AdminShop.LOGGER.debug("ShopItem: "+searchTag.get().location());
                    Messages.sendToServer(new PacketSellRequest(getBankAccount(), slot.getSlotIndex(), itemStack.getCount()));
                    return false;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY){
        RenderSystem.setShaderTexture(0, GUI);
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(GUI, relX, relY, 0, 0, this.imageWidth, this.imageHeight);
    }
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBar.keyPressed(keyCode, scanCode, modifiers) || this.searchBar.canConsumeInput()) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.searchBar.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }


    private void createShopButtons(boolean isBuy, int x, int y){
        List<ShopItem> shopItems = isBuy ? Shop.get().getShopStockBuy() : Shop.get().getShopStockSell();
        // Filter by search if it is set
        if (!this.search.equals("")) {
            shopItems = shopItems.stream().filter(shopItem -> shopItem.getItem().getDisplayName().getString()
                    .toLowerCase().strip().contains(this.search.toLowerCase().strip())).toList();
        }
        // Save to search results even if no search is done
        searchResults = shopItems;
        List<ShopButton> shopButtons = isBuy ? buyButtons : sellButtons;
        //Clear shop buttons if they already exist
        shopButtons.forEach(this::removeWidget);
        shopButtons.clear();
        // Skip rows scrolled past
        int numPassed = rows_passed*NUM_COLS;
        shopItems = shopItems.subList(numPassed, Math.min(numPassed+NUM_ROWS*NUM_COLS, shopItems.size()));
        // Add buttons
        for(int j = 0; j < shopItems.size(); j++){
            final int j2 = j;
            List<ShopItem> finalShopItems = shopItems;
            ShopButton button = new ShopButton(shopItems.get(j),
                    x+SHOP_BUTTON_X+SHOP_BUTTON_SIZE*(j%NUM_COLS),
                    y+SHOP_BUTTON_Y+SHOP_BUTTON_SIZE*((j/NUM_COLS)%NUM_ROWS), (b) -> {
                int quantity = ((ShopButton)b).getQuantity();
                attemptTransaction(getBankAccount(), isBuy, finalShopItems.get(j2), quantity);
            });
            shopButtons.add(button);
            button.visible = isBuy;
            addRenderableWidget(button);
        }

    }

    private void createChangeAccountButton(int x, int y) {
        if(changeAccountButton != null) {
            removeWidget(changeAccountButton);
        }
        changeAccountButton = new ChangeAccountButton(x+127, y+108, (b) -> {
            changeAccounts();
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            assert player != null;
            player.sendSystemMessage(Component.literal("Changed account to "+
                    MojangAPI.getUsernameByUUID(getAccountDetails().getKey())+":"+
                    getAccountDetails().getValue()));
        });
        addRenderableWidget(changeAccountButton);
    }

    private void createSetDefaultAccountButton(int x, int y) {
        if(setDefaultAccountButton != null) {
            removeWidget(setDefaultAccountButton);
        }
        setDefaultAccountButton = new SetDefaultAccountButton(x+109, y+108, (b) -> {
            setDefaultAccount(getAccountDetails());            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            assert player != null;
            player.sendSystemMessage(Component.literal("Set default account to "+
                    MojangAPI.getUsernameByUUID(getAccountDetails().getKey())+":"+
                    getAccountDetails().getValue()));
        });
        addRenderableWidget(setDefaultAccountButton);
    }

    private void changeAccounts() {
        // Check if bankAccount was in usableAccountsIndex
        if (this.usableAccountsIndex == -1) {
            AdminShop.LOGGER.error("BankAccount is not in usableAccountsIndex");
            return;
        }
        // Refresh usable accounts
        Pair<String, Integer> bankAccount = getAccountDetails();
        List<Pair<String, Integer>> localAccountData = new ArrayList<>();
        ClientLocalData.getUsableAccounts().forEach(account -> localAccountData.add(Pair.of(account.getOwner(),
                account.getId())));
        if (!this.usableAccounts.equals(localAccountData)) {
            this.usableAccounts.clear();
            this.usableAccounts.addAll(localAccountData);
        }
        // Change account, either by resetting to first (personal) account or moving to next sorted account
        if (!this.usableAccounts.contains(bankAccount)) {
            this.usableAccountsIndex = 0;
        } else {
            this.usableAccountsIndex = (this.usableAccounts.indexOf(bankAccount) + 1) % this.usableAccounts.size();
        }
    }
    private void createSearchBar(int x, int y) {
        int searchBarWidth = 70;
        int searchBarHeight = 12;
        searchBar = new EditBox(font, x+16, y+18, searchBarWidth, searchBarHeight, Component.literal(""));
        addWidget(searchBar);
    }
    private void createBuySellButton(int x, int y){
        if(buySellButton != null){
            removeWidget(buySellButton);
        }
        buySellButton = new BuySellButton(x+15, y+4,
                I18n.get(GUI_BUY), I18n.get(GUI_SELL), isBuy, (b) -> {
            isBuy = ((BuySellButton)b).switchBuySell();
            int relX = (this.width - this.imageWidth) / 2;
            int relY = (this.height - this.imageHeight) / 2;
            // reset rows passed
            rows_passed = 0;
            createShopButtons(this.isBuy, relX, relY);
            refreshShopButtons();
        });
        addRenderableWidget(buySellButton);
    }

    private void refreshShopButtons(){
        buyButtons.forEach(b -> b.visible = false);
        sellButtons.forEach(b -> b.visible = false);
        changeAccountButton.visible = false;
        setDefaultAccountButton.visible = false;
        List<ShopButton> categoryButtons = isBuy ? buyButtons : sellButtons;
        categoryButtons.forEach(b -> b.visible = true);
        changeAccountButton.visible = true;
        setDefaultAccountButton.visible = true;
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        if (pDelta > 0) {
            // Scroll up
            rows_passed = Math.max(0, rows_passed - 1);
        } else if (pDelta < 0) {
            // Scroll down
            int shopSize = searchResults.size();
            int max_rows_passed = (int) Math.max(Math.ceil(shopSize / (double) NUM_COLS) - 4, 0);
            rows_passed = Math.min(max_rows_passed, rows_passed + 1);
        }
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        createShopButtons(this.isBuy, relX, relY);
        refreshShopButtons();
        return super.mouseScrolled(pMouseX, pMouseY, pDelta);
    }

    private void attemptTransaction(BankAccount bankAccount, boolean isBuy, ShopItem item, int quantity){
        Pair<String, Integer> accountInfo = Pair.of(bankAccount.getOwner(), bankAccount.getId());
        if (isBuy) {
            Messages.sendToServer(new PacketBuyRequest(accountInfo, item, quantity));
        } else {
            Messages.sendToServer(new PacketSellRequest(accountInfo, item, quantity));
        }
        // Refresh account map
        this.accountMap = ClientLocalData.getAccountMap();
    }
}
