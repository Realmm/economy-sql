package com.projecki.economy.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import com.projecki.economy.manager.EconomyEngine;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;

@CommandAlias("economy")
public class EconomyCommand extends BaseCommand {

    @Default
    public void viewBalance(@NotNull Player p) {
        EconomyEngine.getInstance().getBalance(p).ifPresentOrElse(balance -> {
            p.sendMessage(ChatColor.GOLD + "Balance: " + ChatColor.WHITE + balance);
        }, () -> p.sendMessage(ChatColor.RED + "Balance loading..."));
    }

    @CommandAlias("balance|bal")
    public void viewBalance(@NotNull CommandSender sender, @Nullable String name) {
        if (name == null) {
            if (sender instanceof Player p) {
                viewBalance(p);
            } else sender.sendMessage(ChatColor.RED + "Console unable to view balance of self");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(name);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Unable to find player " + name);
            return;
        }
        EconomyEngine.getInstance().getBalance(target).ifPresentOrElse(balance -> {
            sender.sendMessage(ChatColor.GOLD + target.getName() + "'s Balance: " + ChatColor.WHITE + balance);
        }, () -> sender.sendMessage(ChatColor.RED + target.getName() + "'s balance is loading..."));
    }

    @CommandAlias("setbalance|setbal")
    @CommandPermission("economy.admin")
    public void setBalance(@NotNull CommandSender sender, @NotNull String name, @NotNull String value) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(name);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Unable to find player " + name);
            return;
        }
        try {
            long balance = Long.parseLong(value);
            EconomyEngine.getInstance().setBalance(target, balance);
            sender.sendMessage(ChatColor.GREEN + target.getName() + "'s balance has been set to " + balance);
        } catch (NumberFormatException e) {
            try {
                Double.parseDouble(value);
            } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "Unable to set balance to a non-number, tried '" + value + "'");
                return;
            }
            sender.sendMessage(ChatColor.RED + "Only able to set whole, positive numbers, tried '" + value + "'");
        }
    }

}
