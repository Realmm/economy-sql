package com.projecki.economy.manager;

import com.projecki.economy.EconomyPlugin;
import com.projecki.economy.util.Config;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * The economy database
 */
public class Database implements EconomyDatabase {

    private Connection connection;
    private final String host, database, username, password;
    private final EconomyEngine engine;
    private final EconomyPlugin plugin;
    private final String tableName = "economy";

    static {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("The driver for MySQL was not found");
        }
    }

    Database(EconomyEngine engine, String host, String database, String username, String password, @NotNull Runnable afterSetup) {
        this.host = host;
        this.database = database;
        this.username = username;
        this.password = password;
        this.engine = engine;
        this.plugin = engine.getPlugin();
        createDatabase(() -> {
            createEconomyTable(() -> {
                ping();
                afterSetup.run();
            });
        });
    }

    private void refreshConnection() {
        connection = getNewConnection(false).orElseThrow(() ->
                new IllegalStateException("Unable to establish live connection"));
    }

    private void createDatabase(@NotNull Runnable after) {
        plugin.getLogger().log(Level.INFO, "Initializing economy database...");
        execute(true,
                "CREATE DATABASE IF NOT EXISTS " + engine.getConfig().getSQLDatabase()
        ).thenRun(() -> {
            plugin.getLogger().log(Level.INFO, "Economy database initialized!");
            refreshConnection();
            after.run();
        });
    }

    private void createEconomyTable(@NotNull Runnable after) {
        plugin.getLogger().log(Level.INFO, "Setting up economy table...");
        execute(
                "CREATE TABLE IF NOT EXISTS " + tableName + " " +
                        "(uuid VARCHAR(255) NOT NULL, " +
                        "balance BIGINT, " +
                        "PRIMARY KEY (uuid))"
        ).thenRun(() -> {
            plugin.getLogger().log(Level.INFO, "Economy table setup!");
            after.run();
        });
    }

    @Override
    public CompletableFuture<Set<UUID>> getUUIDs() {
        return CompletableFuture.supplyAsync(() -> {
            return query(r -> {
                Set<UUID> uuids = new HashSet<>();
                try {
                    while (r.next()) {
                        UUID uuid = UUID.fromString(r.getString("uuid"));
                        uuids.add(uuid);
                    }
                    return uuids;
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return uuids;
            }, "SELECT uuid FROM " + tableName).join().orElse(new HashSet<>());
        });
    }

    @Override
    public CompletableFuture<@Nullable Long> getBalance(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            return query(r -> {
                Long balance = null;
                try {
                    r.next();
                    balance = r.getLong("balance");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return balance;
            }, "SELECT * FROM " + tableName + " WHERE uuid='" + uuid.toString() + "'").join().orElse(null);
        });
    }

    @Override
    public CompletableFuture<Void> saveBalance(UUID uuid, long balance) {
        return CompletableFuture.runAsync(() -> {
            execute("INSERT INTO " + tableName + " (uuid, balance) VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE uuid=VALUES(uuid), balance=VALUES(balance)", uuid.toString(), balance).join();
        });
    }

    @Override
    public boolean isLive() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Execute an update in the database asynchronously
     *
     * @param sql    The sql
     * @param params The parameters to apply to the statement
     */
    private CompletableFuture<Void> execute(String sql, Object... params) {
        return execute(false, sql, params);
    }

    /**
     * Execute an update in the database asynchronously
     *
     * @param creatingDatabase Whether creating the database initially, or not (used for managing proper connection)
     * @param sql              The sql
     * @param params           The parameters to apply to the statement
     */
    private CompletableFuture<Void> execute(boolean creatingDatabase, String sql, Object... params) {
        return CompletableFuture.runAsync(() -> {
            PreparedStatement statement = prepare(creatingDatabase, sql);
            try {
                for (int i = 0; i < params.length; i++) {
                    statement.setObject(i + 1, params[i]);
                }
                statement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Query the database asynchronously
     *
     * @param function The function to apply to the {@link ResultSet}
     * @param sql      The sql
     * @param params   The parameters to apply to the statement
     * @param <T>      The type of data to look for
     * @return The data, with the appropriate type if found
     */
    private <T> CompletableFuture<Optional<T>> query(Function<? super ResultSet, T> function, String sql, Object... params) {
        PreparedStatement statement = prepare(false, sql);
        return CompletableFuture.supplyAsync(() -> {
            try {
                for (int i = 0; i < params.length; i++) {
                    statement.setObject(i + 1, params[i]);
                }
                ResultSet resultSet = statement.executeQuery(sql);
                return Optional.ofNullable(function.apply(resultSet));
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            return Optional.empty();
        });
    }

    private PreparedStatement prepare(boolean creatingDatabase, String statement) {
        try {
            if (connection == null || connection.isClosed()) {
                connection = getNewConnection(creatingDatabase).orElse(null);
                if (connection == null) throw new IllegalStateException("Unable to find an active connection");
                return connection.prepareStatement(statement);
            }
            return connection.prepareStatement(statement);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("Unable to prepare statement, no valid connection found");
    }

    private void ping() {
        final int delay = 10;
        CompletableFuture.runAsync(() -> {
            Statement statement = null;
            try {
                if (connection != null && !connection.isClosed()) {
                    statement = connection.createStatement();
                    statement.execute("SELECT 1");
                }
            } catch (SQLException ignored) {
                connection = getNewConnection(true).orElse(null);
                plugin.getLogger().log(Level.SEVERE, "Connection issue, will try again in " + delay + " seconds");
            } finally {
                if (statement != null) {
                    try {
                        statement.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).thenAcceptAsync(v -> {
            try {
                Thread.sleep(delay * 1000);
                ping();
            } catch (InterruptedException ignored) {
            }
        });
    }

    private Optional<Connection> getNewConnection(boolean creatingDatabase) {
        try {
            String url = "jdbc:mysql://" + host + ":3306/" + (creatingDatabase ? "" : database);
            return Optional.of(DriverManager.getConnection(url, username, password));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

}
