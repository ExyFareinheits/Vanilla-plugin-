package me.zver.vanillaplus;

import me.zver.vanillaplus.listeners.MainListener;
import me.zver.vanillaplus.mechanics.BlockInteractMechanic;
import me.zver.vanillaplus.mechanics.FishDryingMechanic;
import me.zver.vanillaplus.mechanics.ItemUseMechanic;
import me.zver.vanillaplus.mechanics.BirchSapMechanic;
import me.zver.vanillaplus.mechanics.FlintMechanic;
import me.zver.vanillaplus.mechanics.StonecutterMechanic;
import me.zver.vanillaplus.mechanics.SandSieveMechanic;
import me.zver.vanillaplus.mechanics.FishingNetMechanic; 
import me.zver.vanillaplus.items.AxeTreeChopMechanic;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.block.Action;
import org.bukkit.util.Vector;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;

import java.util.Random;
import java.util.Locale;

public class VanillaPlus extends JavaPlugin {
    private final Random random = new Random();
    private FileConfiguration lang;
    private static VanillaPlus instance;

    @Override
    public void onEnable() {
        instance = this;
        // Яскраве сповіщення у консолі про запуск плагіну
        Bukkit.getConsoleSender().sendMessage("§a==============================");
        Bukkit.getConsoleSender().sendMessage("§eVanillaPlus §av.1.0 §7succesfully activated!");
        Bukkit.getConsoleSender().sendMessage("§aАвтор: Fareinheits | Whoyouare519");
        Bukkit.getConsoleSender().sendMessage("§a==============================");

        // Додаємо рецепт: 4 Rotten Flesh -> 1 Leather
        ItemStack leather = new ItemStack(Material.LEATHER, 1);
        NamespacedKey key = new NamespacedKey(this, "leather_from_rotten_flesh");
        ShapelessRecipe recipe = new ShapelessRecipe(key, leather);
        recipe.addIngredient(4, Material.ROTTEN_FLESH);
        Bukkit.addRecipe(recipe);

        // Реєструємо івент-лісенер з логуванням
        Bukkit.getLogger().info("[VanillaPlus] MainListener...");
        Bukkit.getPluginManager().registerEvents(new MainListener(this), this);

        Bukkit.getLogger().info("[VanillaPlus] BlockInteractMechanic...");
        Bukkit.getPluginManager().registerEvents(new BlockInteractMechanic(), this);

        Bukkit.getLogger().info("[VanillaPlus] FishDryingMechanic...");
        Bukkit.getPluginManager().registerEvents(new FishDryingMechanic(this), this);

        Bukkit.getLogger().info("[VanillaPlus] ItemUseMechanic...");
        Bukkit.getPluginManager().registerEvents(new ItemUseMechanic(this), this);

        Bukkit.getLogger().info("[VanillaPlus] BirchSapMechanic...");
        Bukkit.getPluginManager().registerEvents(new BirchSapMechanic(this), this);

        Bukkit.getLogger().info("[VanillaPlus] FlintMechanic...");
        Bukkit.getPluginManager().registerEvents(new FlintMechanic(this), this);

        Bukkit.getLogger().info("[VanillaPlus] StonecutterMechanic...");
        Bukkit.getPluginManager().registerEvents(new StonecutterMechanic(this), this);

        Bukkit.getLogger().info("[VanillaPlus] SandSieveMechanic...");
        Bukkit.getPluginManager().registerEvents(new SandSieveMechanic(this), this);

        Bukkit.getLogger().info("[VanillaPlus] FishingNetMechanic...");
        Bukkit.getPluginManager().registerEvents(new FishingNetMechanic(this), this);

        Bukkit.getLogger().info("AxeTreeChopMechanic...");
        Bukkit.getPluginManager().registerEvents(new AxeTreeChopMechanic(this), this);

        saveDefaultLang();
        reloadLang();
    }

    // Додаємо цей метод поза onEnable()
    private void saveDefaultLang() {
        File langFile = new File(getDataFolder(), "lang.yml");
        if (!langFile.exists()) {
            saveResource("lang.yml", false);
        }
    }

    public void reloadLang() {
        File langFile = new File(getDataFolder(), "lang.yml");
        lang = YamlConfiguration.loadConfiguration(langFile);
        Bukkit.getLogger().info("[VanillaPlus] lang.yml reloaded!");
    }

    // Метод для отримання перекладу з підстановкою
    public String tr(String key, Object... args) {
        String msg = lang.getString(key, null); // шукає ключ у lang.yml
        if (msg == null) return key;
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length - 1; i += 2) {
                String k = String.valueOf(args[i]);
                String v = String.valueOf(args[i + 1]);
                msg = msg.replace("%" + k + "%", v);
            }
        }
        return msg;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("vanillaplusreload")) {
            if (!sender.hasPermission("vanillaplus.reload")) {
                sender.sendMessage(tr("error.no_perm"));
                return true;
            }
            reloadConfig(); // перезавантажує config.yml
            reloadLang();   // перезавантажує lang.yml (оновлює lang у пам'яті)
            sender.sendMessage(tr("plugin.enabled"));
            return true;
        }
        return false;
    }

    public static VanillaPlus getInstance() {
        return instance;
    }
}