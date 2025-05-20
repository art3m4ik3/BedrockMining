package ru.art3m4ik3.bedrockMining.utils;

import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Интерфейс для адаптеров, обеспечивающих совместимость с разными версиями
 * Minecraft
 */
public interface VersionAdapter {

  /**
   * Отправляет сообщение в ActionBar игроку
   *
   * @param player  Игрок, которому отправляется сообщение
   * @param message Сообщение
   */
  void sendActionBar(Player player, String message);

  /**
   * Создает BossBar для отображения прогресса
   *
   * @param player  Игрок, для которого создается BossBar
   * @param title   Заголовок BossBar
   * @param percent Процент заполнения (0-100)
   * @return ID созданного BossBar
   */
  String createBossBar(Player player, String title, double percent);

  /**
   * Обновляет существующий BossBar
   *
   * @param id      ID BossBar
   * @param title   Новый заголовок
   * @param percent Новый процент заполнения (0-100)
   */
  void updateBossBar(String id, String title, double percent);

  /**
   * Удаляет BossBar
   *
   * @param id ID BossBar
   */
  void removeBossBar(String id);

  /**
   * Создаёт частицы разрушения блока
   *
   * @param block  Блок, на котором отображаются частицы
   * @param damage Уровень повреждения (0-9)
   */
  void playBlockBreakEffect(Block block, int damage);

  /**
   * Отображает частицы в указанном месте
   *
   * @param block    Блок, рядом с которым отображаются частицы
   * @param particle Тип частиц
   * @param count    Количество частиц
   */
  void spawnParticles(Block block, Particle particle, int count);

  /**
   * Проверяет, соответствует ли инструмент требованиям для ломания бедрока
   *
   * @param item Инструмент
   * @return true, если инструмент подходит
   */
  boolean isValidTool(ItemStack item);

  /**
   * Получает модификатор скорости для инструмента
   *
   * @param item Инструмент
   * @return Коэффициент скорости (1.0 - стандартная скорость)
   */
  double getToolSpeedModifier(ItemStack item);
}
