package ru.art3m4ik3.bedrockMining.utils.adapters;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import ru.art3m4ik3.bedrockMining.BedrockMining;
import ru.art3m4ik3.bedrockMining.utils.VersionAdapter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Адаптер для старых версий Minecraft (1.8.x)
 */
public class LegacyAdapter implements VersionAdapter {

  private final BedrockMining plugin;
  private final Map<String, Object> bossBars = new HashMap<>();
  private final Map<UUID, String> playerBossBars = new HashMap<>();

  public LegacyAdapter(BedrockMining plugin) {
    this.plugin = plugin;
  }

  @Override
  public void sendActionBar(Player player, String message) {
    try {
      Class<?> craftPlayerClass = getNMSClass("entity.CraftPlayer", true);
      Class<?> packetPlayOutChatClass = getNMSClass("PacketPlayOutChat");
      Class<?> iChatBaseComponentClass = getNMSClass("IChatBaseComponent");
      Class<?> chatComponentTextClass = getNMSClass("ChatComponentText");

      Constructor<?> chatComponentConstructor = chatComponentTextClass.getDeclaredConstructor(String.class);
      Object chatComponent = chatComponentConstructor.newInstance(message);

      Constructor<?> packetConstructor = packetPlayOutChatClass.getDeclaredConstructor(iChatBaseComponentClass,
          byte.class);
      Object packet = packetConstructor.newInstance(chatComponent, (byte) 2);

      Method getHandleMethod = craftPlayerClass.getDeclaredMethod("getHandle");
      Object craftPlayer = craftPlayerClass.cast(player);
      Object handle = getHandleMethod.invoke(craftPlayer);

      Class<?> entityPlayerClass = getNMSClass("EntityPlayer");
      Object playerConnection = entityPlayerClass.getField("playerConnection").get(handle);

      Class<?> playerConnectionClass = getNMSClass("PlayerConnection");
      Method sendPacketMethod = playerConnectionClass.getDeclaredMethod("sendPacket", getNMSClass("Packet"));
      sendPacketMethod.invoke(playerConnection, packet);
    } catch (Exception e) {
      player.sendMessage(message);
      plugin.getLogger().warning("Failed to send ActionBar: " + e.getMessage());
    }
  }

  @Override
  public String createBossBar(Player player, String title, double percent) {
    String id = UUID.randomUUID().toString();

    playerBossBars.put(player.getUniqueId(), id);
    bossBars.put(id, title);

    new BukkitRunnable() {
      @Override
      public void run() {
        if (!bossBars.containsKey(id) || !player.isOnline()) {
          cancel();
          return;
        }

        player.sendMessage(bossBars.get(id).toString());
      }
    }.runTaskTimer(plugin, 0L, 40L);

    return id;
  }

  @Override
  public void updateBossBar(String id, String title, double percent) {
    bossBars.put(id, title + " - " + (int) percent + "%");
  }

  @Override
  public void removeBossBar(String id) {
    bossBars.remove(id);
  }

  @Override
  public void playBlockBreakEffect(Block block, int damage) {
    try {
      block.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, block.getType());
    } catch (Exception e) {
      plugin.getLogger().warning("Failed to play block break effect: " + e.getMessage());
    }
  }

  @Override
  public void spawnParticles(Block block, Particle particle, int count) {
    try {
      Effect effect = Effect.SMOKE;

      String particleName = particle.name();
      if (particleName.contains("FLAME") || particleName.contains("FIRE")) {
        effect = Effect.valueOf("FLAME");
      } else if (particleName.contains("SMOKE")) {
        effect = Effect.SMOKE;
      } else if (particleName.contains("CRIT")) {
        effect = Effect.valueOf("CRIT");
      }

      for (int i = 0; i < count; i++) {
        double x = block.getLocation().getX() + Math.random() * 0.6 + 0.2;
        double y = block.getLocation().getY() + Math.random() * 0.6 + 0.2;
        double z = block.getLocation().getZ() + Math.random() * 0.6 + 0.2;

        block.getWorld().playEffect(block.getLocation().add(x, y, z), effect, 0);
      }
    } catch (Exception e) {
      plugin.getLogger().warning("Failed to spawn particles: " + e.getMessage());
    }
  }

  @Override
  public double getToolSpeedModifier(ItemStack item) {
    if (!isValidTool(item)) {
      return 0.0;
    }
    return getToolMultiplier(item);
  }

  @Override
  public boolean isValidTool(ItemStack item) {
    if (item == null)
      return false;

    Map<String, Object> configTools = plugin.getConfigManager().getConfig()
        .getConfigurationSection("breaking.tools")
        .getValues(false);

    String itemName = item.getType().name();
    if (configTools.containsKey(itemName)) {
      return true;
    }

    try {
      if (itemName.equals("GOLDEN_APPLE")) {
        if (item.getType().name().equals("ENCHANTED_GOLDEN_APPLE")
            && configTools.containsKey("ENCHANTED_GOLDEN_APPLE")) {
          return true;
        }
      }
    } catch (Exception e) {
      plugin.getLogger().warning("Error checking golden apple: " + e.getMessage());
    }

    return false;
  }

  private double getToolMultiplier(ItemStack item) {
    if (item == null)
      return 0.0;

    Map<String, Object> configTools = plugin.getConfigManager().getConfig()
        .getConfigurationSection("breaking.tools")
        .getValues(false);

    double multiplier = 0.0;
    String itemName = item.getType().name();

    if (configTools.containsKey(itemName)) {
      Object value = configTools.get(itemName);
      if (value instanceof Number) {
        multiplier = ((Number) value).doubleValue();
      }
    } else if (itemName.equals("GOLDEN_APPLE")) {
      try {
        if (item.getType().name().equals("ENCHANTED_GOLDEN_APPLE")) {
          multiplier = plugin.getConfigManager().getConfig()
              .getDouble("breaking.tools.ENCHANTED_GOLDEN_APPLE", 0.5);
        }
      } catch (Exception e) {
        plugin.getLogger().warning("Error checking golden apple multiplier: " + e.getMessage());
      }
    }

    Enchantment efficiency = null;
    try {
      try {
        Class<?> enchClass = Class.forName("org.bukkit.enchantments.Enchantment");
        Method method = enchClass.getMethod("getByKey", Class.forName("org.bukkit.NamespacedKey"));
        efficiency = (Enchantment) method.invoke(null,
            Class.forName("org.bukkit.NamespacedKey").getMethod("minecraft", String.class).invoke(null, "efficiency"));
      } catch (Exception e1) {
        try {
          efficiency = (Enchantment) Class.forName("org.bukkit.enchantments.Enchantment")
              .getMethod("getByName", String.class).invoke(null, "DIG_SPEED");
        } catch (Exception e2) {
          efficiency = (Enchantment) Class.forName("org.bukkit.enchantments.Enchantment")
              .getMethod("getById", int.class).invoke(null, 32);
        }
      }

      if (efficiency != null && item.containsEnchantment(efficiency)) {
        int level = item.getEnchantmentLevel(efficiency);
        multiplier *= (1.0 + (plugin.getConfigManager().getConfig()
            .getDouble("breaking.efficiency-multiplier", 0.1) * level));
      }
    } catch (Exception e) {
      plugin.getLogger().warning("Error checking efficiency enchantment: " + e.getMessage());
    }

    return multiplier;
  }

  private Class<?> getNMSClass(String nmsClassName) throws ClassNotFoundException {
    return getNMSClass(nmsClassName, false);
  }

  private Class<?> getNMSClass(String nmsClassName, boolean isCraftBukkit) throws ClassNotFoundException {
    String version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
    String className = isCraftBukkit
        ? "org.bukkit.craftbukkit." + version + "." + nmsClassName
        : "net.minecraft.server." + version + "." + nmsClassName;
    return Class.forName(className);
  }
}
