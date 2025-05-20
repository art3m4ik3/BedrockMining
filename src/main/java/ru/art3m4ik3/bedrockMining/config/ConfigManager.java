package ru.art3m4ik3.bedrockMining.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.art3m4ik3.bedrockMining.BedrockMining;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

  private final BedrockMining plugin;
  private FileConfiguration config;
  private FileConfiguration langConfig;
  private final Map<String, String> messages = new HashMap<>();

  public ConfigManager(BedrockMining plugin) {
    this.plugin = plugin;
  }

  public void loadConfig() {
    plugin.saveDefaultConfig();
    plugin.reloadConfig();
    config = plugin.getConfig();
  }

  public void loadLanguage() {
    String langCode = config.getString("language", "en");
    File langFile = new File(plugin.getDataFolder(), "lang/" + langCode + ".yml");

    if (!langFile.exists()) {
      try {
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
          langDir.mkdir();
        }

        plugin.saveResource("lang/" + langCode + ".yml", false);
      } catch (Exception e) {
        plugin.getLogger().warning("Failed to create language file: " + e.getMessage());

        if (!langCode.equals("en")) {
          langCode = "en";
          plugin.saveResource("lang/en.yml", false);
        }
      }
    }

    langConfig = YamlConfiguration.loadConfiguration(langFile);

    InputStream defaultLangStream = plugin.getResource("lang/" + langCode + ".yml");
    if (defaultLangStream != null) {
      YamlConfiguration defaultLang = YamlConfiguration.loadConfiguration(
          new InputStreamReader(defaultLangStream, StandardCharsets.UTF_8));
      langConfig.setDefaults(defaultLang);
    }

    loadMessages();
  }

  private String formatMessage(String message) {
    if (message == null)
      return "";

    message = message.replace("&", "§");

    if (message.contains("{prefix}")) {
      String prefix = messages.getOrDefault("prefix", "&7[&bBedrock&3Mining&7]");
      message = message.replace("{prefix}", formatMessage(prefix));
    }

    if (message.contains("#")) {
      try {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("#[a-fA-F0-9]{6}");
        java.util.regex.Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
          String color = message.substring(matcher.start(), matcher.end());
          message = message.replace(color, net.md_5.bungee.api.ChatColor.of(color).toString());
        }
      } catch (Exception e) {
      }
    }

    return message;
  }

  private void loadMessages() {
    for (String key : langConfig.getKeys(true)) {
      if (langConfig.isString(key)) {
        messages.put(key, langConfig.getString(key));
      }
    }
  }

  public String getMessage(String key) {
    return formatMessage(messages.getOrDefault(key, key));
  }

  public String getMessage(String key, Map<String, String> placeholders) {
    String message = messages.getOrDefault(key, key);

    if (placeholders != null) {
      for (Map.Entry<String, String> entry : placeholders.entrySet()) {
        message = message.replace("{" + entry.getKey() + "}", entry.getValue());
      }
    }

    return formatMessage(message);
  }

  public String getProgressBar(double percent, int length) {
    StringBuilder bar = new StringBuilder();
    int filled = (int) ((percent * length) / 100);

    String filledChar = getConfig().getString("progress-display.progress-symbols.filled", "■");
    String emptyChar = getConfig().getString("progress-display.progress-symbols.empty", "□");
    String filledColor = getConfig().getString("progress-display.progress-symbols.filled-color", "&a");
    String emptyColor = getConfig().getString("progress-display.progress-symbols.empty-color", "&7");

    bar.append(formatMessage(filledColor));
    for (int i = 0; i < filled; i++) {
      bar.append(filledChar);
    }

    bar.append(formatMessage(emptyColor));
    for (int i = filled; i < length; i++) {
      bar.append(emptyChar);
    }

    return bar.toString();
  }

  public FileConfiguration getConfig() {
    return config;
  }

  public void saveConfig() {
    try {
      config.save(new File(plugin.getDataFolder(), "config.yml"));
    } catch (IOException e) {
      plugin.getLogger().warning("Failed to save config: " + e.getMessage());
    }
  }
}
