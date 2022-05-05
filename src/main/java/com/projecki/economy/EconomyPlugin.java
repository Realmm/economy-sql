package com.projecki.economy;

import com.projecki.economy.manager.EconomyEngine;
import com.projecki.economy.manager.EconomyManager;
import org.bukkit.plugin.java.JavaPlugin;

public class EconomyPlugin extends JavaPlugin {

    private static EconomyEngine engine;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        engine = new EconomyManager(this);
    }

    @Override
    public void onDisable() {

    }

    /**
     * Get the {@link EconomyEngine}
     * @return The {@link EconomyEngine}
     */
    public static EconomyEngine getEngine() {
        return engine;
    }

}
