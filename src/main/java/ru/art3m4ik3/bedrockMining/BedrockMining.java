package ru.art3m4ik3.bedrockMining;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.art3m4ik3.bedrockMining.commands.BedrockMiningCommand;
import ru.art3m4ik3.bedrockMining.config.ConfigManager;
import ru.art3m4ik3.bedrockMining.listeners.BedrockBreakListener;
import ru.art3m4ik3.bedrockMining.utils.VersionAdapter;
import ru.art3m4ik3.bedrockMining.utils.VersionManager;

public final class BedrockMining extends JavaPlugin {

  private static BedrockMining instance;
  private ConfigManager configManager;
  private VersionManager versionManager;
  private VersionAdapter versionAdapter;

  @Override
  public void onEnable() {
    instance = this;

    configManager = new ConfigManager(this);
    configManager.loadConfig();
    configManager.loadLanguage();

    versionManager = new VersionManager(this);
    versionAdapter = versionManager.getVersionAdapter();

    Bukkit.getPluginManager().registerEvents(new BedrockBreakListener(this), this);

    getCommand("bedrockmining").setExecutor(new BedrockMiningCommand(this));

    getLogger().info("BedrockMining has been successfully enabled!");
  }

  @Override
  public void onDisable() {
    getLogger().info("BedrockMining has been disabled!");
  }

  public static BedrockMining getInstance() {
    return instance;
  }

  public ConfigManager getConfigManager() {
    return configManager;
  }

  public VersionAdapter getVersionAdapter() {
    return versionAdapter;
  }

  public void reload() {
    configManager.loadConfig();
    configManager.loadLanguage();
  }
}
