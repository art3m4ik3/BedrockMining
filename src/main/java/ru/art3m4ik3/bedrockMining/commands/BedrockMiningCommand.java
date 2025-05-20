package ru.art3m4ik3.bedrockMining.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.art3m4ik3.bedrockMining.BedrockMining;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BedrockMiningCommand implements CommandExecutor, TabCompleter {

  private final BedrockMining plugin;

  public BedrockMiningCommand(BedrockMining plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 0) {
      showHelp(sender);
      return true;
    }

    switch (args[0].toLowerCase()) {
      case "reload":
        if (!sender.hasPermission(
            plugin.getConfigManager().getConfig().getString("permissions.reload-permission", "bedrockmining.reload"))) {
          sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
          return true;
        }

        plugin.reload();
        sender.sendMessage(plugin.getConfigManager().getMessage("reload-success"));
        return true;

      case "info":
        showInfo(sender);
        return true;

      case "help":
        showHelp(sender);
        return true;

      default:
        sender.sendMessage(plugin.getConfigManager().getMessage("command-unknown"));
        return true;
    }
  }

  private void showHelp(CommandSender sender) {
    sender.sendMessage(plugin.getConfigManager().getMessage("help-header"));
    sender.sendMessage(plugin.getConfigManager().getMessage("help-reload"));
    sender.sendMessage(plugin.getConfigManager().getMessage("help-info"));
    sender.sendMessage(plugin.getConfigManager().getMessage("help-footer"));
  }

  private void showInfo(CommandSender sender) {
    Map<String, String> placeholders = new HashMap<>();
    placeholders.put("version", plugin.getDescription().getVersion());

    sender.sendMessage(plugin.getConfigManager().getMessage("info-header"));
    sender.sendMessage(plugin.getConfigManager().getMessage("info-version", placeholders));
    sender.sendMessage(plugin.getConfigManager().getMessage("info-author"));
    sender.sendMessage(plugin.getConfigManager().getMessage("info-footer"));
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    List<String> completions = new ArrayList<>();

    if (args.length == 1) {
      completions.add("help");
      completions.add("info");

      if (sender.hasPermission(
          plugin.getConfigManager().getConfig().getString("permissions.reload-permission", "bedrockmining.reload"))) {
        completions.add("reload");
      }

      return completions.stream()
          .filter(s -> s.startsWith(args[0].toLowerCase()))
          .collect(Collectors.toList());
    }

    return completions;
  }
}
