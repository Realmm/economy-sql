package com.projecki.economy.manager;

import co.aikar.commands.PaperCommandManager;
import com.projecki.economy.EconomyPlugin;
import com.projecki.economy.commands.EconomyCommand;
import com.projecki.economy.manager.Database;
import com.projecki.economy.manager.EconomyDatabase;
import com.projecki.economy.manager.EconomyEngine;
import com.projecki.economy.util.Config;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;

public class EconomyManager implements EconomyEngine, Listener {

    private final EconomyPlugin plugin;
    private final Config config;
    private final EconomyDatabase database;
    private final Map<UUID, Long> balances = new HashMap<>();

    public EconomyManager(EconomyPlugin plugin) {
        this.plugin = plugin;
        this.config = new Config(this);

        plugin.getLogger().log(Level.INFO, "Setting up database...");
        this.database = new Database(this, config.getSQLHost(), config.getSQLDatabase(), config.getSQLUsername(), config.getSQLPassword(), () -> {
            plugin.getLogger().log(Level.INFO, "Database setup complete!");
        });

        PaperCommandManager manager = new PaperCommandManager(plugin);
        manager.registerCommand(new EconomyCommand());
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        plugin.getLogger().log(Level.INFO, "Loading player " + e.getPlayer().getName() + "'s balance...");
        loadPlayerBalance(e.getPlayer().getUniqueId(), () -> {
            plugin.getLogger().log(Level.INFO, e.getPlayer().getName() + "'s balance loaded");
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.getLogger().log(Level.INFO, "Saving player " + e.getPlayer().getName() + "'s balance...");
        savePlayerBalance(e.getPlayer().getUniqueId(), () -> {
            plugin.getLogger().log(Level.INFO, e.getPlayer().getName() + "'s balance saved");
        });
    }

    @Override
    public EconomyPlugin getPlugin() {
        return plugin;
    }

    @Override
    public Config getConfig() {
        return config;
    }

    @Override
    public Optional<Long> getBalance(@NotNull OfflinePlayer p) {
        return Optional.ofNullable(balances.get(p.getUniqueId()));
    }

    @Override
    public void setBalance(@NotNull OfflinePlayer p, long balance) {
        if (balance < 0) throw new IllegalStateException("Unable to set negative balance");
        UUID uuid = p.getUniqueId();
        balances.put(uuid, balance);
    }

    /**
     * Save the players balance, from cache
     *
     * @param uuid The uuid of the player to save
     * @param after The runnable to trigger after the players balance is completely saved
     */
    private void savePlayerBalance(@NotNull UUID uuid, @Nullable Runnable after) {
        CompletableFuture.runAsync(() -> {
            if (balances.get(uuid) == null) return;
            database.saveBalance(uuid, balances.get(uuid)).join();
        }).thenRun(after);
    }

    /**
     * Loads the players balance from the database, if present, otherwise caches and saves 0 as the players balance
     *
     * @param uuid The uuid to load the balance of
     * @param after The runnable to trigger after the players balance is completely loaded
     */
    private void loadPlayerBalance(@NotNull UUID uuid, @Nullable Runnable after) {
        CompletableFuture.runAsync(() -> {
            Long balance = database.getBalance(uuid).join();
            if (balance == null) {
                database.saveBalance(uuid, 0).join();
                balances.put(uuid, 0L);
            } else balances.put(uuid, balance);
        }).thenRun(after);
    }

}
