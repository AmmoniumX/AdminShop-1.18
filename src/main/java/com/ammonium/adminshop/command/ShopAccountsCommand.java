package com.ammonium.adminshop.command;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.money.BankAccount;
import com.ammonium.adminshop.money.MoneyManager;
import com.ammonium.adminshop.network.MojangAPI;
import com.ammonium.adminshop.network.PacketAccountRemovePermit;
import com.ammonium.adminshop.network.PacketSyncMoneyToClient;
import com.ammonium.adminshop.setup.Messages;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public class ShopAccountsCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        LiteralArgumentBuilder<CommandSourceStack> shopAccountsCommand = Commands.literal("shopAccounts");

        // shopAccounts info
        LiteralArgumentBuilder<CommandSourceStack> infoCommand = Commands.literal("info")
                .executes(command -> info(command.getSource()));

        // shopAccounts listAccounts
        LiteralArgumentBuilder<CommandSourceStack> listAccountsCommand = Commands.literal("listAccounts")
                        .executes(command -> listAccounts(command.getSource()));

        // shopAccounts createAccount [<members>]
        LiteralArgumentBuilder<CommandSourceStack> createAccountCommand = Commands.literal("createAccount")
                        .executes(command -> createAccount(command.getSource()));
        RequiredArgumentBuilder<CommandSourceStack, String> createAccountWithMembersCommand =
                Commands.argument("members", StringArgumentType.greedyString())
                        .executes((command) -> {
                            String users = StringArgumentType.getString(command, "members");
                            return createAccount(command.getSource(), users);
                        });
        createAccountCommand.then(createAccountWithMembersCommand);

        // shopAccounts deleteAccount [id]
        LiteralArgumentBuilder<CommandSourceStack> deleteAccountCommand = Commands.literal("deleteAccount");
        RequiredArgumentBuilder<CommandSourceStack, Integer> deleteAccountWithIDCommand =
                Commands.argument("id", IntegerArgumentType.integer())
                        .executes(command -> {
                            int id = IntegerArgumentType.getInteger(command, "id");
                            return deleteAccount(command.getSource(), id);
                        });
        deleteAccountCommand.then(deleteAccountWithIDCommand);

        // shopAccounts addMember [id] [member]
        LiteralArgumentBuilder<CommandSourceStack> addMemberCommand = Commands.literal("addMember");
        RequiredArgumentBuilder<CommandSourceStack, Integer> addMemberCommandID = Commands.argument("id",
                IntegerArgumentType.integer());
        RequiredArgumentBuilder<CommandSourceStack, String> addMemberCommandMember = Commands.argument("member",
                StringArgumentType.string())
                        .executes(command -> {
                            int id = IntegerArgumentType.getInteger(command, "id");
                            String member = StringArgumentType.getString(command, "member");
                            return addMember(command.getSource(), id, member);
                        });
        addMemberCommand.then(addMemberCommandID.then(addMemberCommandMember));

        // shopAccounts removeMember [id] [member]
        LiteralArgumentBuilder<CommandSourceStack> removeMemberCommand = Commands.literal("removeMember");
        RequiredArgumentBuilder<CommandSourceStack, Integer> removeMemberCommandID = Commands.argument("id",
                IntegerArgumentType.integer());
        RequiredArgumentBuilder<CommandSourceStack, String> removeMemberCommandMember = Commands.argument("member",
                        StringArgumentType.string())
                .executes(command -> {
                    int id = IntegerArgumentType.getInteger(command, "id");
                    String member = StringArgumentType.getString(command, "member");
                    return removeMember(command.getSource(), id, member);
                });
        removeMemberCommand.then(removeMemberCommandID.then(removeMemberCommandMember));

        // shopAccounts removePermit [id] [tier]
        LiteralArgumentBuilder<CommandSourceStack> removePermitCommand = Commands.literal("removePermit");
        RequiredArgumentBuilder<CommandSourceStack, Integer> removePermitCommandId = Commands.argument("id", IntegerArgumentType.integer());
        RequiredArgumentBuilder<CommandSourceStack, Integer> removePermitCommandTier = Commands.argument("tier", IntegerArgumentType.integer())
                .executes(command -> {
                    int id = IntegerArgumentType.getInteger(command, "id");
                    int tier = IntegerArgumentType.getInteger(command, "tier");
                    return removePermit(command.getSource(), id, tier);
                });
        removePermitCommand.then(removePermitCommandId.then(removePermitCommandTier));

        // shopAccounts transfer [amount] [fromOwner] [fromId] [toOwner] [toId]
        LiteralArgumentBuilder<CommandSourceStack> transferCommand = Commands.literal("transfer");
        RequiredArgumentBuilder<CommandSourceStack, Integer> transferCommandAmount = Commands.argument("amount",
                IntegerArgumentType.integer());
        RequiredArgumentBuilder<CommandSourceStack, String> transferCommandFrom = Commands.argument("fromOwner",
                StringArgumentType.string());
        RequiredArgumentBuilder<CommandSourceStack, Integer> transferCommandFromId = Commands.argument("fromId",
                IntegerArgumentType.integer());
        RequiredArgumentBuilder<CommandSourceStack, String> transferCommandTo = Commands.argument("toOwner",
                StringArgumentType.string());
        RequiredArgumentBuilder<CommandSourceStack, Integer> transferCommandToId = Commands.argument("toId",
                IntegerArgumentType.integer())
                        .executes(command -> {
                            int amount = IntegerArgumentType.getInteger(command, "amount");
                            String fromOwner = StringArgumentType.getString(command, "fromOwner");
                            int fromId = IntegerArgumentType.getInteger(command, "fromId");
                            String toOwner = StringArgumentType.getString(command, "toOwner");
                            int toId = IntegerArgumentType.getInteger(command, "toId");
                            return transferMoney(command.getSource(), amount, fromOwner, fromId, toOwner, toId);
                        });
        transferCommand.then(transferCommandAmount.then(transferCommandFrom.then(transferCommandFromId
                .then(transferCommandTo.then(transferCommandToId)))));

        shopAccountsCommand.then(infoCommand)
                    .then(listAccountsCommand)
                    .then(createAccountCommand)
                    .then(deleteAccountCommand)
                    .then(addMemberCommand)
                    .then(removeMemberCommand)
                    .then(removePermitCommand)
                    .then(transferCommand);
        dispatcher.register(shopAccountsCommand);
    }

    private static int info(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("AdminShop, a mod originally by Vnator and forked by Ammonium_"),
                true);
        return 1;
    }
    private static int listAccounts(CommandSourceStack source) throws CommandSyntaxException {
        // returns 1 (success) or 0 (fail)
        ServerPlayer player = source.getPlayerOrException();
        String playerUUID = player.getStringUUID();
        StringBuilder returnMessage = new StringBuilder("Usable bank accounts for "+player.getName().getString()+
                ":"+"\n");

        if (source.getLevel().isClientSide) {
            AdminShop.LOGGER.error("Can't access this from client side!");
            source.sendFailure(Component.literal("Can't access this from client side!"));
            return 0;
        }

        MoneyManager moneyManager = MoneyManager.get(source.getLevel());
        if (!moneyManager.getSharedAccounts().containsKey(playerUUID)) {
            AdminShop.LOGGER.warn("No accounts found for "+player.getName().getString());
            returnMessage.append("None");
        } else {
            List<BankAccount> sharedAccountsSorted = moneyManager.getSharedAccounts().get(playerUUID);
            sharedAccountsSorted.sort((o1, o2) -> {
                if (o1.getOwner().equals(playerUUID) && !o2.getOwner().equals(playerUUID)) {
                    return -1;
                } else if (!o1.getOwner().equals(playerUUID) && o2.getOwner().equals(playerUUID)) {
                    return 1;
                } else if (o1.getOwner().equals(o2.getOwner())) {
                    return Integer.compare(o1.getId(), o2.getId());
                } else {
                    return o1.getOwner().compareTo(o2.getOwner());
                }}
            );
            sharedAccountsSorted.forEach(bankAccount -> {
            returnMessage.append("$");
            returnMessage.append(bankAccount.getBalance());
            returnMessage.append(": ");
            returnMessage.append(getUsernameByUUID(source.getLevel(), bankAccount.getOwner()));
            returnMessage.append(":");
            returnMessage.append(bankAccount.getId());
            returnMessage.append("\nPermits: ");
            bankAccount.getPermits().forEach(permit -> {
                returnMessage.append(permit);
                returnMessage.append(",");
            });
            returnMessage.append("\nMembers: ");
            bankAccount.getMembers().forEach(memberUUID -> {
                returnMessage.append(getUsernameByUUID(source.getLevel(), memberUUID));
                returnMessage.append(" ");
            });
            returnMessage.append("\n");
            });
        }

        source.sendSuccess(() -> Component.literal(returnMessage.toString()), true);
        return 1;
    }

    private static int createAccount(CommandSourceStack source) throws CommandSyntaxException {
        String members = source.getPlayerOrException().getName().getString();
        return createAccount(source, members);
    }

    private static int createAccount(CommandSourceStack source, String members) throws CommandSyntaxException {
        // Create account to MoneyManager, then sync money to every members' clients
        // Split members string to list
        ServerPlayer player = source.getPlayerOrException();
        Set<String> memberNames = new HashSet<>(Set.of(members.split(" ")));

        // Validate that all member names are online player names
        Set<String> onlinePlayerNames = new HashSet<>(source.getOnlinePlayerNames());
        boolean valid = onlinePlayerNames.containsAll(memberNames);
        if(!valid) {
            AdminShop.LOGGER.error("Member list invalid. Member list:");
            AdminShop.LOGGER.error(members);
            source.sendFailure(Component.literal("Member list invalid, all members must be online!"));
            return 0;
        }

        // Try to convert every member to their UUID, fail if can't
        List<ServerPlayer> onlinePlayers = source.getLevel().players();
        Set<String> memberUUIDs = new HashSet<>();
        List<ServerPlayer> memberPlayers = new ArrayList<>();
        memberNames.forEach(name -> {
            Optional<ServerPlayer> searchPlayer = onlinePlayers.stream().filter(serverPlayer -> serverPlayer.getName()
                    .getString().equals(name)).findAny();
            if(searchPlayer.isEmpty()) {
                AdminShop.LOGGER.error("Couldn't find member in onlinePlayers. Member: "+name);
            } else {
                memberPlayers.add(searchPlayer.get());
                memberUUIDs.add(searchPlayer.get().getStringUUID());
            }
        });
        if (memberNames.size() != memberUUIDs.size()) {
            AdminShop.LOGGER.error("Couldn't find all members in online players");
            source.sendFailure(Component.literal("Couldn't find all members in onlinePlayers. Member list: "
                    +members));
            return 0;
        }

        // Verify that ownerUUID is in memberUUIDs
        if (!memberUUIDs.contains(player.getStringUUID()) || !memberPlayers.contains(player)) {
            AdminShop.LOGGER.info("Owner is not in members list, adding.");
            memberUUIDs.add(player.getStringUUID());
            memberNames.add(player.getName().getString());
            memberPlayers.add(player);
        }

        // Create new account
        int newId = MoneyManager.get(source.getLevel()).CreateAccount(player.getStringUUID(), memberUUIDs);

        if (newId == -1) {
            AdminShop.LOGGER.error("Error creating new account!");
            source.sendFailure(Component.literal("Error creating new account!"));
            return 0;
        }

        // If successful, sync client data to each member and return new ID to command
        MoneyManager moneyManager = MoneyManager.get(source.getLevel());

        // Sync client data with all members
        memberPlayers.forEach(member -> Messages.sendToPlayer(new PacketSyncMoneyToClient(
                moneyManager.getSharedAccounts().get(member.getStringUUID())), member));
        source.sendSuccess(() -> Component.literal("Created new account with ID "+newId), true);
        return 1;
    }

    private static int deleteAccount(CommandSourceStack source, int id) throws CommandSyntaxException {
        // Check if trying to delete personal account
        if (id == 1) {
            AdminShop.LOGGER.error("Can't delete personal account!");
            source.sendFailure(Component.literal("Can't delete personal (id 1) account!"));
            return 0;
        }
        // Get player and moneyManager
        ServerPlayer player = source.getPlayerOrException();
        MoneyManager moneyManager = MoneyManager.get(source.getLevel());
        // Check if player has account with said ID
        if (!moneyManager.existsBankAccount(player.getStringUUID(), id)) {
            source.sendFailure(Component.literal("There are no accounts you own with said ID!"));
            return 0;
        }
        // Get list of to-be-deleted account's memberUUIDs
        Set<String> memberUUIDs = moneyManager.getBankAccount(player.getStringUUID(), id).getMembers();
        // Delete bank account
        boolean success = moneyManager.deleteBankAccount(player.getStringUUID(), id);
        if(!success) {
            AdminShop.LOGGER.error("Error deleting bank account!");
            source.sendFailure(Component.literal("Error deleting bank account!"));
        }

        // Get list of online deleted account members
        List<ServerPlayer> onlinePlayers = source.getLevel().players();
        List<ServerPlayer> onlineMembers = new ArrayList<>();
        memberUUIDs.forEach(name -> {
            Optional<ServerPlayer> searchPlayer = onlinePlayers.stream().filter(serverPlayer ->
                    serverPlayer.getStringUUID().equals(name)).findAny();
            searchPlayer.ifPresent(onlineMembers::add);
        });

        // Sync client data with all onlineMembers
        Set<BankAccount> accountSet = moneyManager.getAccountSet();
        onlineMembers.forEach(memberPlayer -> Messages.sendToPlayer(
                new PacketSyncMoneyToClient(moneyManager.getSharedAccounts().get(memberPlayer.getStringUUID())),
                memberPlayer));

        source.sendSuccess(() -> Component.literal("Successfully deleted account "+id), true);
        return 1;
    }

    private static int addMember(CommandSourceStack source, int id, String member) throws CommandSyntaxException {
        // Check if trying to add member to personal account
        if (id == 1) {
            AdminShop.LOGGER.error("Can't add member to personal account.");
            source.sendFailure(Component.literal("Can't add member to personal account (id 1)! Make a shared " +
                    "account with /shopAccounts createAccount [<members>]"));
            return 0;
        }
        // Get player and moneyManager
        ServerPlayer player = source.getPlayerOrException();
        MoneyManager moneyManager = MoneyManager.get(source.getLevel());
        // Check if bank account exists
        if (!moneyManager.existsBankAccount(player.getStringUUID(), id)) {
            AdminShop.LOGGER.error("Can't add member to bank account that doesn't exist.");
            source.sendFailure(Component.literal("That account ID doesn't exist! Use an existing " +
                    "ID from /shopAccounts listAccounts"));
            return 0;
        }
        // Get memberUUID, fail if can't
        List<ServerPlayer> onlinePlayers = source.getLevel().players();
        Optional<ServerPlayer> searchPlayer = onlinePlayers.stream().filter(serverPlayer -> serverPlayer.getName()
                .getString().equals(member)).findAny();
        if(searchPlayer.isEmpty()) {
            AdminShop.LOGGER.error("Couldn't find member in onlinePlayers.");
            source.sendFailure(Component.literal("Couldn't find member "+member+"! Member must be online"));
            return 0;
        }
        String memberUUID = searchPlayer.get().getStringUUID();
        // Check if bank account already has member
        if (moneyManager.getBankAccount(player.getStringUUID(), id).getMembers().contains(memberUUID)) {
            AdminShop.LOGGER.error("BankAccount already has member.");
            source.sendFailure(Component.literal("Account already has member "+member));
            return 0;
        }
        // Add member, return if failed
        boolean success = moneyManager.addMember(player.getStringUUID(), id, memberUUID);
        if (!success) {
            AdminShop.LOGGER.error("Error adding member to bank account.");
            source.sendFailure(Component.literal("Error adding member to bank account."));
            return 0;
        }
        // Get list of online members
        List<ServerPlayer> onlineMembers = new ArrayList<>();
        BankAccount newBankAccount = moneyManager.getBankAccount(player.getStringUUID(), id);
        newBankAccount.getMembers().forEach(accountMember -> {
            Optional<ServerPlayer> searchMember = onlinePlayers.stream().filter(serverPlayer ->
                    serverPlayer.getStringUUID().equals(accountMember)).findAny();
            searchMember.ifPresent(onlineMembers::add);
        });
        // Sync client data with all onlineMembers
        onlineMembers.forEach(memberPlayer -> Messages.sendToPlayer(
                new PacketSyncMoneyToClient(moneyManager.getSharedAccounts().get(memberPlayer.getStringUUID())),
                memberPlayer));

        source.sendSuccess(() -> Component.literal("Successfully added "+member+" to account."), true);
        return 1;
    }
    private static int removeMember(CommandSourceStack source, int id, String member) throws CommandSyntaxException {
        // Check if trying to add member to personal account
        if (id == 1) {
            AdminShop.LOGGER.error("Can't remove member from personal account.");
            source.sendFailure(Component.literal("Can't add remove member from personal account (id 1)!"));
            return 0;
        }
        // Get player and moneyManager
        ServerPlayer player = source.getPlayerOrException();
        MoneyManager moneyManager = MoneyManager.get(source.getLevel());
        // Check if bank account exists
        if (!moneyManager.existsBankAccount(player.getStringUUID(), id)) {
            AdminShop.LOGGER.error("Can't remove member from bank account that doesn't exist.");
            source.sendFailure(Component.literal("That account ID doesn't exist! Use an existing " +
                    "ID from /shopAccounts listAccounts"));
            return 0;
        }
        // Get memberUUID, fail if can't
        List<ServerPlayer> onlinePlayers = source.getLevel().players();
        Optional<ServerPlayer> searchPlayer = onlinePlayers.stream().filter(serverPlayer -> serverPlayer.getName()
                .getString().equals(member)).findAny();
        if(searchPlayer.isEmpty()) {
            AdminShop.LOGGER.error("Couldn't find member in onlinePlayers.");
            source.sendFailure(Component.literal("Couldn't find member "+member+"! Member must be online"));
            return 0;
        }
        String memberUUID = searchPlayer.get().getStringUUID();
        // Check if bank account doesn't have member
        if (!moneyManager.getBankAccount(player.getStringUUID(), id).getMembers().contains(memberUUID)) {
            AdminShop.LOGGER.error("BankAccount doesn't have member.");
            source.sendFailure(Component.literal("Account doesn't have member "+member));
            return 0;
        }
        /// Check if trying to remove owner (self)
        if (player.getStringUUID().equals(memberUUID)) {
            AdminShop.LOGGER.error("Account owner cant remove itself from own account");
            source.sendFailure(Component.literal("You can't remove yourself from an account you own."));
            return 0;
        }
        // Remove member, return if failed
        boolean success = moneyManager.removeMember(player.getStringUUID(), id, memberUUID);
        if (!success) {
            AdminShop.LOGGER.error("Error removing member from bank account.");
            source.sendFailure(Component.literal("Error removing member from bank account."));
            return 0;
        }
        // Remove accounts from removed member's shared accounts
        moneyManager.removeSharedAccount(memberUUID, player.getStringUUID(), id);

        // Get list of online members to sync, including removed one
        List<ServerPlayer> onlineMembers = new ArrayList<>();
        BankAccount newBankAccount = moneyManager.getBankAccount(player.getStringUUID(), id);
        Set<String> membersToSync = new HashSet<>(newBankAccount.getMembers());
        membersToSync.add(memberUUID);
        membersToSync.forEach(accountMember -> {
            Optional<ServerPlayer> searchMember = onlinePlayers.stream().filter(serverPlayer ->
                    serverPlayer.getStringUUID().equals(accountMember)).findAny();
            searchMember.ifPresent(onlineMembers::add);
        });
        // Sync client data with all onlineMembers
        Set<BankAccount> accountSet = moneyManager.getAccountSet();
        onlineMembers.forEach(memberPlayer -> Messages.sendToPlayer(
                new PacketSyncMoneyToClient(moneyManager.getSharedAccounts().get(memberPlayer.getStringUUID())),
                memberPlayer));

        source.sendSuccess(() -> Component.literal("Successfully removed "+member+" from account."), true);
        return 1;
    }
    static int removePermit(CommandSourceStack source, int id, int tier) throws CommandSyntaxException {
        if (tier == 0) {
            source.sendFailure(Component.literal("You can't remove default (0) permit from an account!"));
            return 0;
        }
        // Get player and moneyManager
        ServerPlayer player = source.getPlayerOrException();
        MoneyManager moneyManager = MoneyManager.get(source.getLevel());
        // Check if player has account with said ID
        if (!moneyManager.existsBankAccount(player.getStringUUID(), id)) {
            source.sendFailure(Component.literal("There are no accounts you own with said ID!"));
            return 0;
        }
        // Delete permit
        Messages.sendToServer(new PacketAccountRemovePermit(player.getStringUUID(), id, tier));
        source.sendSuccess(() -> Component.literal("Removed permit from account"), true);
        return 1;
    }

    private static int transferMoney(CommandSourceStack source, int amount, String fromName, int fromId, String toName,
                                     int toId) throws CommandSyntaxException {
        // Get player and moneyManager
        ServerPlayer player = source.getPlayerOrException();
        MoneyManager moneyManager = MoneyManager.get(source.getLevel());
        // Get accounts UUIDs, fail if can't
        String fromUUID, toUUID;
        List<ServerPlayer> onlinePlayers = source.getLevel().players();
        Optional<ServerPlayer> searchPlayer;
        searchPlayer = onlinePlayers.stream().filter(serverPlayer -> serverPlayer.getName()
                .getString().equals(fromName)).findAny();
        if(searchPlayer.isEmpty()) {
            AdminShop.LOGGER.error("Couldn't find player "+fromName+"! Both account owners must be online");
            source.sendFailure(Component.literal("Couldn't find player "+fromName+"! Both account owners must be online"));
            return 0;
        }
        fromUUID = searchPlayer.get().getStringUUID();
        searchPlayer = onlinePlayers.stream().filter(serverPlayer -> serverPlayer.getName()
                .getString().equals(toName)).findAny();
        if(searchPlayer.isEmpty()) {
            AdminShop.LOGGER.error("Couldn't find player "+toName+"! Both account owners must be online");
            source.sendFailure(Component.literal("Couldn't find player "+toName+"! Both account owners must be online"));
            return 0;
        }
        toUUID = searchPlayer.get().getStringUUID();
        // Process request
        AdminShop.LOGGER.info("Transfering "+amount+" from "+fromUUID+":"+fromId+" to "+toUUID+":"+toId);
        // Check if both accounts exist
        if (!moneyManager.existsBankAccount(fromUUID, fromId)) {
            AdminShop.LOGGER.error("Source account doesn't exist.");
            source.sendFailure(Component.literal("Source account doesn't exist! Use an existing " +
                    "ID from /shopAccounts listAccounts"));
            return 0;
        }
        if (!moneyManager.existsBankAccount(toUUID, toId)) {
            AdminShop.LOGGER.error("Destination account doesn't exist.");
            source.sendFailure(Component.literal("Destination account doesn't exist! Use an existing " +
                    "ID from /shopAccounts listAccounts"));
            return 0;
        }
        // Check if source account has enough balance
        if (moneyManager.getBalance(fromUUID, fromId) < amount) {
            AdminShop.LOGGER.error("Source account doesn't have enough funds");
            source.sendFailure(Component.literal("Source account doesn't have enough funds"));
            return 0;
        }
        // Perform transfer
        boolean subtractSuccess = moneyManager.subtractBalance(fromUUID, fromId, amount);
        if (!subtractSuccess) {
            AdminShop.LOGGER.error("Error subtracting from source account");
            source.sendFailure(Component.literal("Error subtracting from source account"));
            return 0;
        }
        boolean addSuccess = moneyManager.addBalance(toUUID, toId, amount);
        if (!addSuccess) {
            AdminShop.LOGGER.error("Error adding to destination account");
            source.sendFailure(Component.literal("Error adding to destination account"));
        }
        AdminShop.LOGGER.info("Transfer successful");
        source.sendSuccess(() -> Component.literal("Transfer successful!"), true);
        return 1;
    }
    private static String getUsernameByUUID(ServerLevel level, String uuid) {
        // Search in online players
        List<ServerPlayer> players = level.players();
        Optional<ServerPlayer> search = players.stream().filter(player ->
                player.getStringUUID().equals(uuid)).findAny();
        return search.map(serverPlayer -> serverPlayer.getName().getString()).orElseGet(() -> MojangAPI.getUsernameByUUID(uuid));
    }
}