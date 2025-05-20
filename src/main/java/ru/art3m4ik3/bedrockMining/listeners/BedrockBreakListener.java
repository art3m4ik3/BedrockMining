package ru.art3m4ik3.bedrockMining.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.inventory.ItemStack;
import ru.art3m4ik3.bedrockMining.BedrockMining;
import ru.art3m4ik3.bedrockMining.utils.ProgressDisplay;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BedrockBreakListener implements Listener {

  private final BedrockMining plugin;
  private final Map<UUID, BedrockBreakInfo> breakingPlayers = new HashMap<>();

  public BedrockBreakListener(BedrockMining plugin) {
    this.plugin = plugin;

    int updateFrequency = plugin.getConfigManager().getConfig()
        .getInt("progress-display.update-frequency", 5);

    Bukkit.getScheduler().runTaskTimer(plugin, this::updateProgress, updateFrequency, updateFrequency);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onBlockDamage(BlockDamageEvent event) {
    Block block = event.getBlock();
    Player player = event.getPlayer();

    if (block.getType() != Material.BEDROCK) {
      return;
    }

    UUID playerId = player.getUniqueId();
    if (breakingPlayers.containsKey(playerId)) {
      BedrockBreakInfo breakInfo = breakingPlayers.get(playerId);
      if (breakInfo.getBlock().getLocation().equals(block.getLocation())) {
        breakInfo.updateActivity();
        event.setCancelled(true);
        return;
      }
    }

    if (plugin.getConfigManager().getConfig().getBoolean("permissions.require-permission", true) &&
        !player.hasPermission(
            plugin.getConfigManager().getConfig().getString("permissions.break-permission", "bedrockmining.break"))) {
      player.sendMessage(plugin.getConfigManager().getMessage("breaking-no-permission"));
      return;
    }

    ItemStack item = null;
    try {
      item = player.getInventory().getItemInMainHand();
    } catch (NoSuchMethodError e) {
      try {
        Method getItemInHandMethod = player.getClass().getMethod("getItemInHand");
        item = (ItemStack) getItemInHandMethod.invoke(player);
      } catch (Exception ex) {
        plugin.getLogger().warning("Failed to get item in hand: " + ex.getMessage());
      }
    }

    if (!plugin.getVersionAdapter().isValidTool(item)) {
      player.sendMessage(plugin.getConfigManager().getMessage("breaking-wrong-tool"));
      return;
    }

    event.setCancelled(true);

    startBreaking(player, block, item);
  }

  private void startBreaking(Player player, Block block, ItemStack tool) {
    UUID playerId = player.getUniqueId();

    if (breakingPlayers.containsKey(playerId)) {
      cancelBreaking(player, "breaking-canceled");
    }

    double baseBreakTimeSeconds = plugin.getConfigManager().getConfig().getDouble("breaking.time", 5);
    double toolSpeedModifier = plugin.getVersionAdapter().getToolSpeedModifier(tool);

    int breakTimeTicks = (int) (baseBreakTimeSeconds * 20 / toolSpeedModifier);

    BedrockBreakInfo breakInfo = new BedrockBreakInfo(
        block,
        breakTimeTicks,
        0,
        new ProgressDisplay(plugin, player));

    breakingPlayers.put(playerId, breakInfo);

    player.sendMessage(plugin.getConfigManager().getMessage("breaking-started"));

    breakInfo.getProgressDisplay().init();
  }

  private boolean tryPlaySound(Block block, String sound, float volume, float pitch) {
    if (!plugin.getConfigManager().getConfig().getBoolean("breaking.sound-effects.enabled", true)) {
      return false;
    }

    try {
      block.getWorld().playSound(block.getLocation(), sound, volume, pitch);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private void completeBreaking(Player player, BedrockBreakInfo breakInfo) {
    Block block = breakInfo.getBlock();

    if (block.getType() != Material.BEDROCK) {
      return;
    }

    breakInfo.getProgressDisplay().remove();

    player.sendMessage(plugin.getConfigManager().getMessage("breaking-success"));

    if (plugin.getConfigManager().getConfig().getBoolean("breaking.sound-effects.enabled", true)) {
      try {
        float volume = (float) plugin.getConfigManager().getConfig().getDouble("breaking.sound-effects.complete-volume",
            1.0);
        float pitch = 1.0f;

        if (!tryPlaySound(block, "minecraft:block.deepslate.break", volume, pitch) &&
            !tryPlaySound(block, "minecraft:block.stone.break", volume, pitch) &&
            !tryPlaySound(block, "minecraft:block.ancient_debris.break", volume, pitch)) {
          tryPlaySound(block, "dig.stone", volume, pitch);
        }
      } catch (Exception e) {
        plugin.getLogger().warning("Failed to play break sound: " + e.getMessage());
      }
    }

    block.setType(Material.AIR);

    if (plugin.getConfigManager().getConfig().getBoolean("breaking.drop-block", true)) {
      player.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5),
          new ItemStack(Material.BEDROCK, 1));
    }
  }

  private void updateProgress() {
    Map<UUID, BedrockBreakInfo> playersCopy = new HashMap<>(breakingPlayers);
    List<UUID> playersToRemove = new ArrayList<>();

    for (Map.Entry<UUID, BedrockBreakInfo> entry : playersCopy.entrySet()) {
      UUID playerId = entry.getKey();
      BedrockBreakInfo breakInfo = breakingPlayers.get(playerId);
      Player player = Bukkit.getPlayer(playerId);

      if (player == null || !player.isOnline()) {
        playersToRemove.add(playerId);
        continue;
      }

      if (!breakInfo.isActive()) {
        cancelBreaking(player, "breaking-canceled");
        playersToRemove.add(playerId);
        continue;
      }

      breakInfo.increaseProgress();

      double progressPercent = breakInfo.getProgressPercent();
      breakInfo.getProgressDisplay().update(progressPercent);

      Block block = breakInfo.getBlock();

      if (plugin.getConfigManager().getConfig().getBoolean("breaking.particles", true)) {
        int damage = (int) (progressPercent / 10);
        plugin.getVersionAdapter().playBlockBreakEffect(block, damage);

        if (plugin.getConfigManager().getConfig().getBoolean("breaking.sound-effects.enabled", true)) {
          int frequency = plugin.getConfigManager().getConfig().getInt("breaking.sound-effects.frequency", 10);

          int divisor = breakInfo.getTotalTicks() * frequency / 100;
          if (divisor > 0 && breakInfo.getCurrentTicks() % divisor == 0) {
            float minPitch = (float) plugin.getConfigManager().getConfig().getDouble("breaking.sound-effects.min-pitch",
                0.5);
            float maxPitch = (float) plugin.getConfigManager().getConfig().getDouble("breaking.sound-effects.max-pitch",
                1.5);
            float pitch = minPitch + ((maxPitch - minPitch) * (float) (progressPercent / 100.0f));
            float volume = (float) plugin.getConfigManager().getConfig()
                .getDouble("breaking.sound-effects.breaking-volume", 0.3);

            if (!tryPlaySound(block, "minecraft:block.deepslate.hit", volume, pitch) &&
                !tryPlaySound(block, "minecraft:block.stone.hit", volume, pitch) &&
                !tryPlaySound(block, "minecraft:block.ancient_debris.hit", volume, pitch)) {
              tryPlaySound(block, "dig.stone", volume, pitch);
            }
          }
        }
      }

      if (breakInfo.isComplete()) {
        completeBreaking(player, breakInfo);
        playersToRemove.add(playerId);
      }
    }

    for (UUID playerId : playersToRemove) {
      breakingPlayers.remove(playerId);
    }
  }

  private void cancelBreaking(Player player, String messageKey) {
    UUID playerId = player.getUniqueId();
    if (breakingPlayers.containsKey(playerId)) {
      BedrockBreakInfo breakInfo = breakingPlayers.get(playerId);
      breakInfo.getProgressDisplay().remove();
      breakingPlayers.remove(playerId);

      player.sendMessage(plugin.getConfigManager().getMessage(messageKey));
    }
  }

  private static class BedrockBreakInfo {
    private final Block block;
    private final int totalTicks;
    private int currentTicks;
    private final ProgressDisplay progressDisplay;
    private long lastActiveTime;
    private static final long MAX_INACTIVE_TIME = 1000;

    public BedrockBreakInfo(Block block, int totalTicks, int currentTicks, ProgressDisplay progressDisplay) {
      this.block = block;
      this.totalTicks = totalTicks;
      this.currentTicks = currentTicks;
      this.progressDisplay = progressDisplay;
      this.lastActiveTime = System.currentTimeMillis();
    }

    public Block getBlock() {
      return block;
    }

    public void increaseProgress() {
      currentTicks++;
    }

    public double getProgressPercent() {
      return (double) currentTicks / totalTicks * 100;
    }

    public boolean isComplete() {
      return currentTicks >= totalTicks;
    }

    public ProgressDisplay getProgressDisplay() {
      return progressDisplay;
    }

    public void updateActivity() {
      this.lastActiveTime = System.currentTimeMillis();
    }

    public boolean isActive() {
      return System.currentTimeMillis() - lastActiveTime <= MAX_INACTIVE_TIME;
    }

    public int getCurrentTicks() {
      return currentTicks;
    }

    public int getTotalTicks() {
      return totalTicks;
    }
  }
}
