package com.projecki.economy.manager;

import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The database for the economy
 */
public interface EconomyDatabase {

    /**
     * Get all UUID's from the database
     * @return All UUID's from the database
     */
    CompletableFuture<Set<UUID>> getUUIDs();

    /**
     * Get the balance of a player from the database
     * @param uuid The UUID of the player to get the balance of
     * @return The balance of the player, or null if not found
     */
    CompletableFuture<@Nullable Long> getBalance(UUID uuid);

    /**
     * Save a players balance in the database
     * @param uuid The uuid to save
     * @param balance The value to save
     * @return Completed once the value is saved
     */
    CompletableFuture<Void> saveBalance(UUID uuid, long balance);

    /**
     * Check if the connection is currently live
     * @return True if the connection is live, otherwise false
     */
    boolean isLive();

}
