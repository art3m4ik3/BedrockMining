package ru.art3m4ik3.bedrockMining.utils;

import org.bukkit.Particle;
import org.bukkit.entity.Player;
import ru.art3m4ik3.bedrockMining.BedrockMining;

import java.util.HashMap;
import java.util.Map;

/**
 * Класс для отображения прогресса ломания бедрока
 */
public class ProgressDisplay {

  private final BedrockMining plugin;
  private final Player player;
  private String bossBarId;
  private final String displayType;

  public ProgressDisplay(BedrockMining plugin, Player player) {
    this.plugin = plugin;
    this.player = player;
    this.displayType = plugin.getConfigManager().getConfig().getString("progress-display.type", "ACTIONBAR");
  }

  /**
   * Инициализирует отображение прогресса
   */
  public void init() {
    if (displayType.equalsIgnoreCase("BOSSBAR")) {
      bossBarId = plugin.getVersionAdapter().createBossBar(player, formatProgressMessage(0), 0);
    }
  }

  /**
   * Обновляет отображение прогресса
   *
   * @param percent Процент выполнения
   */
  public void update(double percent) {
    String message = formatProgressMessage(percent);

    switch (displayType.toUpperCase()) {
      case "CHAT":
        player.sendMessage(message);
        break;
      case "ACTIONBAR":
        plugin.getVersionAdapter().sendActionBar(player, message);
        break;
      case "BOSSBAR":
        if (bossBarId != null) {
          plugin.getVersionAdapter().updateBossBar(bossBarId, message, percent);
        }
        break;
      case "PARTICLES":
        try {
          Particle particle;
          if (percent < 33) {
            particle = Particle.CLOUD;
          } else if (percent < 66) {
            particle = Particle.CRIT;
          } else {
            particle = Particle.FLAME;
          }

          plugin.getVersionAdapter().spawnParticles(
              player.getTargetBlock(null, 5),
              particle,
              5 + (int) (percent / 10));
        } catch (Exception e) {
          plugin.getVersionAdapter().sendActionBar(player, message);
        }
        break;
    }
  }

  /**
   * Удаляет отображение прогресса
   */
  public void remove() {
    if (bossBarId != null) {
      plugin.getVersionAdapter().removeBossBar(bossBarId);
      bossBarId = null;
    }
  }

  private String formatProgressMessage(double percent) {
    Map<String, String> placeholders = new HashMap<>();
    placeholders.put("percent", String.format("%.1f", percent));
    placeholders.put("progress_bar", plugin.getConfigManager().getProgressBar(percent,
        plugin.getConfigManager().getConfig().getInt("progress-display.progress-symbols.length", 10)));

    String messageKey = "breaking-progress-" + displayType.toLowerCase();
    return plugin.getConfigManager().getMessage(messageKey, placeholders);
  }
}
