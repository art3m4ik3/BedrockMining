package ru.art3m4ik3.bedrockMining.utils.adapters;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.art3m4ik3.bedrockMining.BedrockMining;
import ru.art3m4ik3.bedrockMining.utils.CompatibilityConstants;
import ru.art3m4ik3.bedrockMining.utils.VersionAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Адаптер для версий Minecraft 1.13-1.15
 */
public class NewAdapter implements VersionAdapter {

  private final BedrockMining plugin;
  private final Map<String, BossBar> bossBars = new HashMap<>();

  public NewAdapter(BedrockMining plugin) {
    this.plugin = plugin;
  }

  @Override
  public void sendActionBar(Player player, String message) {
    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
  }

  @Override
  public String createBossBar(Player player, String title, double percent) {
    String id = UUID.randomUUID().toString();

    String colorName = plugin.getConfigManager().getConfig().getString("progress-display.bossbar-color", "PURPLE");
    BarColor color;
    try {
      color = BarColor.valueOf(colorName);
    } catch (IllegalArgumentException e) {
      color = BarColor.PURPLE;
      plugin.getLogger().warning("Invalid boss bar color: " + colorName + ". Using PURPLE instead.");
    }

    BossBar bossBar = Bukkit.createBossBar(title, color, BarStyle.SOLID);
    bossBar.setProgress(percent / 100.0);
    bossBar.addPlayer(player);

    bossBars.put(id, bossBar);
    return id;
  }

  @Override
  public void updateBossBar(String id, String title, double percent) {
    BossBar bossBar = bossBars.get(id);
    if (bossBar != null) {
      bossBar.setTitle(title);
      bossBar.setProgress(percent / 100.0);
    }
  }

  @Override
  public void removeBossBar(String id) {
    BossBar bossBar = bossBars.get(id);
    if (bossBar != null) {
      bossBar.removeAll();
      bossBars.remove(id);
    }
  }

  @Override
  public void playBlockBreakEffect(Block block, int damage) {
    Particle particle;
    try {
      particle = CompatibilityConstants.getBlockCrackParticle();
      block.getWorld().spawnParticle(
          particle,
          block.getLocation().add(0.5, 0.5, 0.5),
          10 * damage,
          0.3, 0.3, 0.3,
          0.01,
          block.getBlockData());
    } catch (Exception e) {
      spawnParticles(block, CompatibilityConstants.getCritParticle(), 10 * damage);
    }
  }

  @Override
  public void spawnParticles(Block block, Particle particle, int count) {
    block.getWorld().spawnParticle(
        particle,
        block.getLocation().add(0.5, 0.5, 0.5),
        count,
        0.3, 0.3, 0.3,
        0.01);
  }

  @Override
  public boolean isValidTool(ItemStack item) {
    if (item == null)
      return false;

    Map<String, Object> tools = plugin.getConfigManager().getConfig()
        .getConfigurationSection("breaking.tools").getValues(false);

    if (!tools.containsKey(item.getType().name())) {
      return false;
    }

    int minEfficiencyLevel = plugin.getConfigManager().getConfig()
        .getInt("breaking.min-efficiency-level", 0);

    if (minEfficiencyLevel > 0) {
      Enchantment efficiency = CompatibilityConstants.getEfficiencyEnchantment();
      if (efficiency == null || !item.containsEnchantment(efficiency)) {
        return false;
      }

      return item.getEnchantmentLevel(efficiency) >= minEfficiencyLevel;
    }

    return true;
  }

  @Override
  public double getToolSpeedModifier(ItemStack item) {
    if (item == null)
      return 0.0;

    double modifier = plugin.getConfigManager().getConfig()
        .getDouble("breaking.tools." + item.getType().name(), 0.0);

    Enchantment efficiency = CompatibilityConstants.getEfficiencyEnchantment();
    if (efficiency != null && item.containsEnchantment(efficiency)) {
      int level = item.getEnchantmentLevel(efficiency);
      modifier += 0.1 * level;
    }

    return modifier;
  }
}
