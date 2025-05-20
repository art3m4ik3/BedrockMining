package ru.art3m4ik3.bedrockMining.utils;

import org.bukkit.Bukkit;
import ru.art3m4ik3.bedrockMining.BedrockMining;
import ru.art3m4ik3.bedrockMining.utils.adapters.*;

public class VersionManager {

  private final BedrockMining plugin;
  private String serverVersion;

  public VersionManager(BedrockMining plugin) {
    this.plugin = plugin;

    try {
      String packageName = Bukkit.getServer().getClass().getPackage().getName();
      String[] parts = packageName.split("\\.");

      if (parts.length > 3) {
        this.serverVersion = parts[3];
      } else {
        this.serverVersion = "v" + Bukkit.getBukkitVersion().split("-")[0].replace(".", "_");
        plugin.getLogger().info("Using alternative version detection: " + this.serverVersion);
      }
    } catch (Exception e) {
      this.serverVersion = "v1_20_1";
      plugin.getLogger().warning("Failed to detect server version: " + e.getMessage());
      plugin.getLogger().warning("Using default version: " + this.serverVersion);
    }
  }

  public VersionAdapter getVersionAdapter() {
    String version = serverVersion.substring(1);
    String[] parts = version.split("_");
    int majorVersion = Integer.parseInt(parts[0]);
    int minorVersion = Integer.parseInt(parts[1]);

    plugin.getLogger().info("Detected server version: " + majorVersion + "." + minorVersion);

    if (majorVersion == 1) {
      if (minorVersion >= 20) {
        return new ModernAdapter(plugin);
      } else if (minorVersion >= 16) {
        return new NewerAdapter(plugin);
      } else if (minorVersion >= 13) {
        return new NewAdapter(plugin);
      } else if (minorVersion >= 9) {
        return new MiddleAdapter(plugin);
      } else {
        return new LegacyAdapter(plugin);
      }
    }

    plugin.getLogger().warning("Could not determine version adapter, using modern adapter");
    return new ModernAdapter(plugin);
  }

  public String getServerVersion() {
    return serverVersion;
  }
}
