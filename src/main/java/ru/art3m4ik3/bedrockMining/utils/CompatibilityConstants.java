package ru.art3m4ik3.bedrockMining.utils;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.enchantments.Enchantment;

import java.lang.reflect.Method;

/**
 * Класс для получения совместимых констант для разных версий Minecraft
 */
public class CompatibilityConstants {

  private static String SERVER_VERSION;
  private static int MAJOR_VERSION = 1;
  private static int MINOR_VERSION = 20;

  static {
    try {
      String packageName = Bukkit.getServer().getClass().getPackage().getName();
      SERVER_VERSION = packageName.substring(packageName.lastIndexOf('.') + 1);

      try {
        String versionString = SERVER_VERSION.startsWith("v") ? SERVER_VERSION.substring(1) : SERVER_VERSION;
        String[] parts = versionString.split("_");

        if (parts.length >= 2 && parts[0].matches("\\d+") && parts[1].matches("\\d+")) {
          MAJOR_VERSION = Integer.parseInt(parts[0]);
          MINOR_VERSION = Integer.parseInt(parts[1]);
        } else {
          String bukkitVersion = Bukkit.getBukkitVersion().split("-")[0];
          String[] versionParts = bukkitVersion.split("\\.");
          MAJOR_VERSION = Integer.parseInt(versionParts[0]);
          MINOR_VERSION = Integer.parseInt(versionParts[1]);
        }
      } catch (Exception e) {
        MAJOR_VERSION = 1;
        MINOR_VERSION = 20;
      }
    } catch (Exception e) {
      MAJOR_VERSION = 1;
      MINOR_VERSION = 20;
      SERVER_VERSION = "v1_20_0";
    }
  }

  /**
   * Получить Enchantment.EFFICIENCY для текущей версии
   */
  public static Enchantment getEfficiencyEnchantment() {
    try {
      try {
        return Enchantment.EFFICIENCY;
      } catch (NoSuchFieldError e) {
        try {
          return Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft("efficiency"));
        } catch (NoSuchMethodError nsme) {
          try {
            Method getByNameMethod = Enchantment.class.getMethod("getByName", String.class);
            return (Enchantment) getByNameMethod.invoke(null, "EFFICIENCY");
          } catch (Exception ex) {
            try {
              Method getByNameMethod = Enchantment.class.getMethod("getByName", String.class);
              return (Enchantment) getByNameMethod.invoke(null, "DIG_SPEED");
            } catch (Exception exc) {
              try {
                Method getByIdMethod = Enchantment.class.getMethod("getById", int.class);
                return (Enchantment) getByIdMethod.invoke(null, 32);
              } catch (Exception excp) {
                for (Enchantment enchantment : Enchantment.values()) {
                  try {
                    String name = enchantment.getKey().getKey();
                    if (name.equalsIgnoreCase("efficiency") || name.equalsIgnoreCase("dig_speed")) {
                      return enchantment;
                    }
                  } catch (Exception eKey) {
                    try {
                      Method getNameMethod = enchantment.getClass().getMethod("getName");
                      String name = (String) getNameMethod.invoke(enchantment);
                      if (name.equalsIgnoreCase("EFFICIENCY") || name.equalsIgnoreCase("DIG_SPEED")) {
                        return enchantment;
                      }
                    } catch (Exception eName) {
                    }
                  }
                }
              }
            }
          }
        }
      }
    } catch (Exception e) {
    }

    return null;
  }

  /**
   * Получить совместимую частицу для текущей версии
   */
  public static Particle getBlockCrackParticle() {
    try {
      for (Particle particle : Particle.values()) {
        if (particle.name().contains("BLOCK_CRACK") ||
            particle.name().contains("BLOCK_DUST")) {
          return particle;
        }
      }

      return getCritParticle();
    } catch (Exception e) {
      try {
        return Particle.values()[0];
      } catch (Exception ex) {
        return null;
      }
    }
  }

  /**
   * Получить совместимую частицу облака для текущей версии
   */
  public static Particle getCloudParticle() {
    try {
      for (Particle particle : Particle.values()) {
        if (particle.name().contains("CLOUD")) {
          return particle;
        }
      }

      return Particle.values()[0];
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Получить совместимую частицу пламени для текущей версии
   */
  public static Particle getFlameParticle() {
    try {
      for (Particle particle : Particle.values()) {
        if (particle.name().contains("FLAME") ||
            particle.name().contains("FIRE")) {
          return particle;
        }
      }

      return Particle.values()[0];
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Получить совместимую критическую частицу для текущей версии
   */
  public static Particle getCritParticle() {
    try {
      for (Particle particle : Particle.values()) {
        if (particle.name().contains("CRIT")) {
          return particle;
        }
      }

      return Particle.values()[0];
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Проверяет, поддерживает ли сервер класс Particle
   */
  public static boolean supportsParticles() {
    try {
      Class.forName("org.bukkit.Particle");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  /**
   * Получить версию сервера в формате "1.X.X"
   */
  public static String getVersionString() {
    return MAJOR_VERSION + "." + MINOR_VERSION;
  }

  /**
   * Проверяет, является ли версия сервера новее указанной
   */
  public static boolean isVersionNewerThan(int major, int minor) {
    return (MAJOR_VERSION > major) || (MAJOR_VERSION == major && MINOR_VERSION >= minor);
  }
}
