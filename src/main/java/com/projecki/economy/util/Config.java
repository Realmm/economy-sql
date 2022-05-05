package com.projecki.economy.util;

import com.projecki.economy.EconomyPlugin;
import com.projecki.economy.manager.EconomyEngine;

import java.util.Optional;

/**
 * The configuration (config.yml) file handler
 */
public class Config {

    private final EconomyEngine engine;

    public Config(EconomyEngine engine) {
        this.engine = engine;
    }

    /**
     * Get the SQL host, e.g localhost
     * @return The SQL host, if present, otherwise defaults to 'localhost'
     */
    public String getSQLHost() {
        return getString("sql.host").orElse("localhost");
    }

    /**
     * Get the SQL database, e.g economy-sql-database
     * @return The SQL database, if present, otherwise defaults to 'economyDatabase'
     */
    public String getSQLDatabase() {
        return getString("sql.database").orElse("economyDatabase");
    }

    /**
     * Get the SQL username, if present, otherwise empty
     * @return The SQL username, if present, otherwise empty
     */
    public String getSQLUsername() {
        return getString("sql.username").orElse("");
    }

    /**
     * Get the SQL password, if present, otherwise empty
     * @return The SQL password, if present, otherwise empty
     */
    public String getSQLPassword() {
        return getString("sql.password").orElse("");
    }

    private Optional<String> getString(String path) {
        return Optional.ofNullable(engine.getPlugin().getConfig().getString(path));
    }

}
