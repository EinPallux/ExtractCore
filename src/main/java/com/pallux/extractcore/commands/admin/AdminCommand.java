package com.pallux.extractcore.commands.admin;

import com.pallux.extractcore.ExtractCore;
import com.pallux.extractcore.gui.ResetConfirmGUI;
import com.pallux.extractcore.model.PlayerData;
import com.pallux.extractcore.util.ColorUtil;
import com.pallux.extractcore.util.GuiUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.List;

public class AdminCommand implements CommandExecutor, TabCompleter {

    private final ExtractCore plugin;
    private static final List<String> CURRENCIES  = List.of("scrap","screws","energycells","biosamples","techshards");
    private static final List<String> RESET_TYPES = List.of("scrap","screws","energycells","biosamples","techshards","level","core","all");

    public AdminCommand(ExtractCore plugin) { this.plugin = plugin; }

    private String msg(String key) {
        return new GuiUtil(plugin, "admin").str(key);
    }
    private String msg(String key, String... pairs) {
        return new GuiUtil(plugin, "admin").str(key, GuiUtil.ph(pairs));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("ex.admin")) {
            sender.sendMessage(msg("errors.no-permission")); return true;
        }
        if (args.length == 0) { sendHelp(sender); return true; }

        return switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "define" -> handleDefine(sender, args);
            case "set","give","take" -> handleCurrency(sender, args, args[0].toLowerCase());
            case "reset"  -> handleReset(sender, args);
            default       -> { sendHelp(sender); yield true; }
        };
    }

    private boolean handleReload(CommandSender sender) {
        plugin.reload();
        sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getMessages()
            .getString("general.reload-success", "&#5B8DD9Reloaded.")));
        return true;
    }

    private boolean handleDefine(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(msg("errors.define-players-only")); return true;
        }
        if (args.length < 3 || !args[1].equalsIgnoreCase("extract")) {
            sender.sendMessage(msg("errors.define-usage")); return true;
        }
        String name = args[2];
        try {
            var we       = com.sk89q.worldedit.WorldEdit.getInstance();
            var wePlayer = com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(player);
            var region   = we.getSessionManager().get(wePlayer).getSelection(wePlayer.getWorld());
            if (region == null) { sender.sendMessage(msg("errors.worldedit-no-sel")); return true; }
            var min = region.getMinimumPoint();
            var max = region.getMaximumPoint();
            plugin.getExtractionManager().defineZone(name, player.getWorld().getName(),
                min.x(), min.y(), min.z(), max.x(), max.y(), max.z());
            String m = plugin.getConfigManager().getMessages()
                .getString("extraction.zone-defined","&#5B8DD9Zone &e{name} &7defined.")
                .replace("{name}", name);
            sender.sendMessage(ColorUtil.color(m));
        } catch (Exception e) {
            sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getMessages()
                .getString("extraction.zone-define-fail","&cFailed.")));
        }
        return true;
    }

    private boolean handleCurrency(CommandSender sender, String[] args, String action) {
        if (args.length < 4) {
            sender.sendMessage(msg("errors.currency-usage", "action", action)); return true;
        }
        String currency = args[1].toLowerCase();
        if (!CURRENCIES.contains(currency)) {
            sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getMessages()
                .getString("admin.invalid-currency","&cInvalid currency."))); return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        long amount;
        try { amount = Long.parseLong(args[3]); } catch (NumberFormatException e) {
            sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getMessages()
                .getString("general.invalid-amount","&cInvalid amount."))); return true;
        }
        if (amount < 0) { sender.sendMessage(msg("errors.amount-positive")); return true; }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData data = plugin.getPlayerDataManager().load(target.getUniqueId(),
                target.getName() != null ? target.getName() : "Unknown");
            Bukkit.getScheduler().runTask(plugin, () -> {
                switch (action) {
                    case "set"  -> setCurrency(data, currency, amount);
                    case "give" -> addCurrency(data, currency, amount);
                    case "take" -> subtractCurrency(data, currency, amount);
                }
                plugin.getPlayerDataManager().saveAsync(data);
                String key = "admin." + action + "-currency";
                String m = plugin.getConfigManager().getMessages().getString(key, "&#5B8DD9Done.")
                    .replace("{player}", target.getName() != null ? target.getName() : "?")
                    .replace("{currency}", currency).replace("{amount}", String.valueOf(amount));
                sender.sendMessage(ColorUtil.color(m));
            });
        });
        return true;
    }

    private boolean handleReset(CommandSender sender, String[] args) {
        if (!(sender instanceof Player admin)) { sender.sendMessage(msg("errors.players-only")); return true; }
        if (args.length < 3) { sender.sendMessage(msg("errors.reset-usage")); return true; }
        String type = args[1].toLowerCase();
        if (!RESET_TYPES.contains(type)) {
            sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getMessages()
                .getString("admin.invalid-reset-type","&cInvalid reset type."))); return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        new ResetConfirmGUI(plugin, admin, target, type).open();
        return true;
    }

    private void sendHelp(CommandSender s) {
        var g = new GuiUtil(plugin, "admin");
        s.sendMessage(g.str("help.header"));
        s.sendMessage(g.str("help.reload"));
        s.sendMessage(g.str("help.define"));
        s.sendMessage(g.str("help.currency"));
        s.sendMessage(g.str("help.reset"));
    }

    private void setCurrency(PlayerData d, String c, long v) {
        switch (c) {
            case "scrap" -> d.setScrap(v); case "screws" -> d.setScrews(v);
            case "energycells" -> d.setEnergyCells(v); case "biosamples" -> d.setBioSamples(v);
            case "techshards" -> d.setTechShards(v);
        }
    }
    private void addCurrency(PlayerData d, String c, long v) {
        switch (c) {
            case "scrap" -> d.addScrap(v); case "screws" -> d.addScrews(v);
            case "energycells" -> d.addEnergyCells(v); case "biosamples" -> d.addBioSamples(v);
            case "techshards" -> d.addTechShards(v);
        }
    }
    private void subtractCurrency(PlayerData d, String c, long v) {
        switch (c) {
            case "scrap" -> d.setScrap(Math.max(0, d.getScrap() - v));
            case "screws" -> d.setScrews(Math.max(0, d.getScrews() - v));
            case "energycells" -> d.setEnergyCells(Math.max(0, d.getEnergyCells() - v));
            case "biosamples" -> d.setBioSamples(Math.max(0, d.getBioSamples() - v));
            case "techshards" -> d.setTechShards(Math.max(0, d.getTechShards() - v));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("ex.admin")) return List.of();
        if (args.length == 1) return filter(List.of("reload","define","set","give","take","reset"), args[0]);
        if (args.length == 2) return switch (args[0].toLowerCase()) {
            case "define" -> filter(List.of("extract"), args[1]);
            case "set","give","take" -> filter(CURRENCIES, args[1]);
            case "reset" -> filter(RESET_TYPES, args[1]);
            default -> List.of();
        };
        if (args.length == 3 && List.of("set","give","take","reset").contains(args[0].toLowerCase()))
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase())).toList();
        return List.of();
    }

    private List<String> filter(List<String> list, String prefix) {
        return list.stream().filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase())).toList();
    }
}
