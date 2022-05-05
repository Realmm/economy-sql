package com.projecki.economy.manager;

import com.projecki.economy.EconomyPlugin;
import com.projecki.economy.util.Config;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * The engine for the economy
 */
public interface EconomyEngine {

    /**
     * Get the {@link EconomyEngine} instance
     * @return The {@link EconomyEngine} instance
     */
    static EconomyEngine getInstance() {
        return EconomyPlugin.getEngine();
    }

    /**
     * Get the {@link EconomyPlugin}
     * @return The {@link EconomyPlugin}
     */
    EconomyPlugin getPlugin();

    /**
     * Get the config
     * @return The config
     */
    Config getConfig();

    /**
     * Get the balance of the player, if cached
     *
     * @param p The player to get the balance of
     * @return The balance of the player, if cached
     */
    Optional<Long> getBalance(@NotNull OfflinePlayer p);

    /**
     * Set a players balance in local cache
     *
     * @param p        The players balance to update
     * @param balance  The updated balance
     */
    void setBalance(@NotNull OfflinePlayer p, long balance);

}
