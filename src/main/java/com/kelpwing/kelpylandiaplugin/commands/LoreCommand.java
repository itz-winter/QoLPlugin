package com.kelpwing.kelpylandiaplugin.commands;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /lore set <text>  - Replace all lore with one line (supports & color codes and \n for newlines)
 * /lore add <text>  - Add a new line of lore
 * /lore clear       - Remove all lore from the held item
 * /lore remove <n>  - Remove a specific line number (1-based)
 * /lore insert <n> <text> - Insert a line at position n
 */
public class LoreCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList("set", "add", "clear", "remove", "insert");

    private final KelpylandiaPlugin plugin;

    public LoreCommand(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("qol.lore")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            sendUsage(player);
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "You must be holding an item.");
            return true;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            player.sendMessage(ChatColor.RED + "This item cannot have lore.");
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "set" -> {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /lore set <text>");
                    return true;
                }
                String text = joinArgs(args, 1);
                // Support \n for multi-line lore
                String[] lines = text.split("\\\\n");
                List<String> lore = new ArrayList<>();
                for (String line : lines) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(lore);
                item.setItemMeta(meta);
                player.sendMessage(ChatColor.GREEN + "Lore set to:");
                for (String line : lore) {
                    player.sendMessage(ChatColor.GRAY + "  " + line);
                }
            }
            case "add" -> {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /lore add <text>");
                    return true;
                }
                String text = joinArgs(args, 1);
                String colored = ChatColor.translateAlternateColorCodes('&', text);
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add(colored);
                meta.setLore(lore);
                item.setItemMeta(meta);
                player.sendMessage(ChatColor.GREEN + "Added lore line: " + ChatColor.GRAY + colored);
            }
            case "clear" -> {
                meta.setLore(null);
                item.setItemMeta(meta);
                player.sendMessage(ChatColor.GREEN + "Lore cleared.");
            }
            case "remove" -> {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /lore remove <line number>");
                    return true;
                }
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                if (lore.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "This item has no lore.");
                    return true;
                }
                try {
                    int lineNum = Integer.parseInt(args[1]);
                    if (lineNum < 1 || lineNum > lore.size()) {
                        player.sendMessage(ChatColor.RED + "Line number must be between 1 and " + lore.size() + ".");
                        return true;
                    }
                    String removed = lore.remove(lineNum - 1);
                    meta.setLore(lore.isEmpty() ? null : lore);
                    item.setItemMeta(meta);
                    player.sendMessage(ChatColor.GREEN + "Removed lore line " + lineNum + ": " + ChatColor.GRAY + removed);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid number: " + args[1]);
                }
            }
            case "insert" -> {
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /lore insert <line number> <text>");
                    return true;
                }
                try {
                    int lineNum = Integer.parseInt(args[1]);
                    String text = joinArgs(args, 2);
                    String colored = ChatColor.translateAlternateColorCodes('&', text);
                    List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                    int index = Math.max(0, Math.min(lineNum - 1, lore.size()));
                    lore.add(index, colored);
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                    player.sendMessage(ChatColor.GREEN + "Inserted lore at line " + (index + 1) + ": " + ChatColor.GRAY + colored);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid number: " + args[1]);
                }
            }
            default -> sendUsage(player);
        }

        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage(ChatColor.GOLD + "Lore Commands:");
        player.sendMessage(ChatColor.YELLOW + "/lore set <text>" + ChatColor.GRAY + " - Set lore (use \\n for newlines)");
        player.sendMessage(ChatColor.YELLOW + "/lore add <text>" + ChatColor.GRAY + " - Add a lore line");
        player.sendMessage(ChatColor.YELLOW + "/lore clear" + ChatColor.GRAY + " - Clear all lore");
        player.sendMessage(ChatColor.YELLOW + "/lore remove <line>" + ChatColor.GRAY + " - Remove a lore line");
        player.sendMessage(ChatColor.YELLOW + "/lore insert <line> <text>" + ChatColor.GRAY + " - Insert a lore line");
    }

    private String joinArgs(String[] args, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i > start) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(prefix))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
