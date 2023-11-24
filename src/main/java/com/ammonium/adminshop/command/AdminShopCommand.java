package com.ammonium.adminshop.command;

import com.ammonium.adminshop.setup.Registration;
import com.ammonium.adminshop.money.MoneyManager;
import com.ammonium.adminshop.network.PacketSyncMoneyToClient;
import com.ammonium.adminshop.setup.Messages;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class AdminShopCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        LiteralArgumentBuilder<CommandSourceStack> adminShopCommand = Commands.literal("adminshop");

        // adminshop getPermit [tier]
        LiteralArgumentBuilder<CommandSourceStack> getPermitCommand = Commands.literal("getPermit").requires(source -> source.hasPermission(3));
        RequiredArgumentBuilder<CommandSourceStack, Integer> getPermitCommandTier = Commands.argument("tier", IntegerArgumentType.integer())
                        .executes(command -> {
                            int tier = IntegerArgumentType.getInteger(command, "tier");
                            return getPermit(command.getSource(), tier);
                        });
        getPermitCommand.then(getPermitCommandTier);

        // adminshop give {owner} {id} {amount}
        LiteralArgumentBuilder<CommandSourceStack> giveMoneyCommand = Commands.literal("give")
                .requires(source -> source.hasPermission(3))
                .then(Commands.argument("owner", EntityArgument.players())
                        .then(Commands.argument("id", IntegerArgumentType.integer())
                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                        .executes(context -> {
                                            EntitySelector ownerSelector = context.getArgument("owner", EntitySelector.class);
                                            int id = IntegerArgumentType.getInteger(context, "id");
                                            int amount = IntegerArgumentType.getInteger(context, "amount");
                                            return giveMoney(context.getSource(), ownerSelector, id, amount);
                                        })
                                )
                        ));

        adminShopCommand.then(getPermitCommand)
                        .then(giveMoneyCommand);
        dispatcher.register(adminShopCommand);
    }


    static int getPermit(CommandSourceStack source, int tier) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (tier == 0) {
            source.sendFailure(Component.literal("Permit tier 0 is the default, all accounts have it!"));
            return 0;
        }

        // Give item
        ItemStack permit = new ItemStack(Registration.PERMIT.get());
        CompoundTag key = permit.getOrCreateTag();
        key.putInt("key", tier);
        permit.setTag(key);

        boolean success = player.getInventory().add(permit);
        if (!success) {
            source.sendFailure(Component.literal("Unable to give permit to player"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Obtained trade permit"), true);
        
        return 1;
    }

    static int giveMoney(CommandSourceStack source, EntitySelector selector, int id, int amount) throws CommandSyntaxException {
        // Get player and MoneyManager
        MoneyManager moneyManager = MoneyManager.get(source.getLevel());
        ServerPlayer player = selector.findSinglePlayer(source);
        String playerUUID = player.getStringUUID();

        // Search for account
        if (!moneyManager.existsBankAccount(playerUUID, id)) {
            source.sendFailure(Component.literal("Account "+player.getName().getString()+":"+id+" does not exist!"));
            return 0;
        }

        // Add money
        boolean success = moneyManager.addBalance(playerUUID, id, amount);

        if (!success) {
            source.sendFailure(Component.literal("Error adding money to account"));
        }

        // Sync client data with all onlineMembers
        List<ServerPlayer> onlinePlayers = source.getLevel().players();
        Set<String> membersUUIDs = moneyManager.getBankAccount(playerUUID, id).getMembers();
        Set<ServerPlayer> onlineMembers = new HashSet<>();
        // Get list of online members to sync, including removed one
        membersUUIDs.forEach(accountMember -> {
            Optional<ServerPlayer> searchMember = onlinePlayers.stream().filter(serverPlayer ->
                    serverPlayer.getStringUUID().equals(accountMember)).findAny();
            searchMember.ifPresent(onlineMembers::add);
        });
        onlineMembers.forEach(memberPlayer -> Messages.sendToPlayer(
                new PacketSyncMoneyToClient(moneyManager.getSharedAccounts().get(memberPlayer.getStringUUID())),
                memberPlayer));
        source.sendSuccess(() -> Component.literal("Successfully added money to account!"), true);
        return 1;
    }
}
