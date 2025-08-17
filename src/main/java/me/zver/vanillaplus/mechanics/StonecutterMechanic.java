package me.zver.vanillaplus.mechanics;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class StonecutterMechanic implements Listener {
    private final Map<UUID, Map<String, Integer>> sharpenCount = new HashMap<>();
    private final Map<UUID, Map<String, Long>> sharpenCooldown = new HashMap<>();
    private final Random random = new Random();
    private Plugin plugin;

    public StonecutterMechanic(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onStonecutterUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.STONECUTTER) return;
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();

        // --- Розширені рецепти ---
        // Дерево -> палки
        if (hand.getType() == Material.OAK_LOG || hand.getType() == Material.BIRCH_LOG || hand.getType() == Material.SPRUCE_LOG
                || hand.getType() == Material.JUNGLE_LOG || hand.getType() == Material.ACACIA_LOG || hand.getType() == Material.DARK_OAK_LOG) {
            player.getInventory().removeItem(new ItemStack(hand.getType(), 1));
            player.getInventory().addItem(new ItemStack(Material.STICK, 8));
            block.getWorld().spawnParticle(Particle.CRIT, block.getLocation().add(0.5, 1, 0.5), 10, 0.2, 0.2, 0.2, 0.01);
            player.sendMessage(getLangFromPlugin("stonecutter.sticks"));
            event.setCancelled(true);
            return;
        }
        // Шерсть -> нитки
        if (hand.getType().name().endsWith("_WOOL")) {
            player.getInventory().removeItem(new ItemStack(hand.getType(), 1));
            player.getInventory().addItem(new ItemStack(Material.STRING, 4));
            block.getWorld().spawnParticle(Particle.CRIT, block.getLocation().add(0.5, 1, 0.5), 10, 0.2, 0.2, 0.2, 0.01);
            player.sendMessage(getLangFromPlugin("stonecutter.string"));
            event.setCancelled(true);
            return;
        }
        // Лід -> сніжки
        if (hand.getType() == Material.ICE || hand.getType() == Material.PACKED_ICE || hand.getType() == Material.BLUE_ICE) {
            player.getInventory().removeItem(new ItemStack(hand.getType(), 1));
            player.getInventory().addItem(new ItemStack(Material.SNOWBALL, 4));
            block.getWorld().spawnParticle(Particle.CRIT, block.getLocation().add(0.5, 1, 0.5), 10, 0.2, 0.2, 0.2, 0.01);
            player.sendMessage(getLangFromPlugin("stonecutter.snowballs"));
            event.setCancelled(true);
            return;
        }

        // --- Заточування інструментів ---
        if (isSharpenable(hand)) {
            String key = hand.getType().toString() + ":" + player.getUniqueId();
            Map<String, Integer> countMap = sharpenCount.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
            Map<String, Long> cooldownMap = sharpenCooldown.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
            int count = countMap.getOrDefault(key, 0);
            long last = cooldownMap.getOrDefault(key, 0L);

            // КД 5 хвилин після 3 заточок
            if (count >= 3 && System.currentTimeMillis() - last < 5 * 60 * 1000) {
                player.sendMessage(getLangFromPlugin("stonecutter.cooldown"));
                return;
            }
            // Перевірка на тип і міцність
            if (!canSharpen(hand)) {
                player.sendMessage(getLangFromPlugin("stonecutter.cant_sharpen"));
                return;
            }

            // Білі партикли при заточці
            block.getWorld().spawnParticle(Particle.CLOUD, block.getLocation().add(0.5, 1, 0.5), 8, 0.2, 0.2, 0.2, 0.01);

            // Відновлення міцності
            Damageable dmg = (Damageable) hand.getItemMeta();
            int restore = 20 + random.nextInt(31); // 20-50
            int newDamage = Math.max(0, dmg.getDamage() - restore);
            boolean enchanted = false;

            // Шанс зачарування
            if (random.nextDouble() < 0.10) {
                if (hand.getType().toString().contains("SWORD") && !dmg.hasEnchant(Enchantment.DAMAGE_ALL)) {
                    dmg.addEnchant(Enchantment.DAMAGE_ALL, 1 + random.nextInt(2), true);
                    enchanted = true;
                } else if ((hand.getType().toString().contains("PICKAXE") || hand.getType().toString().contains("AXE"))
                        && !dmg.hasEnchant(Enchantment.DIG_SPEED)) {
                    dmg.addEnchant(Enchantment.DIG_SPEED, 1 + random.nextInt(2), true);
                    enchanted = true;
                }
            }

            // Шанс поломки
            if (random.nextDouble() < 0.07) {
                player.getInventory().setItemInMainHand(null);
                block.getWorld().spawnParticle(Particle.SMOKE_LARGE, block.getLocation().add(0.5, 1, 0.5), 10, 0.2, 0.2, 0.2, 0.01);
                player.sendMessage(getLangFromPlugin("stonecutter.broken"));
                countMap.put(key, count + 1);
                if (count + 1 >= 3) cooldownMap.put(key, System.currentTimeMillis());
                event.setCancelled(true);
                return;
            }

            dmg.setDamage(newDamage);
            hand.setItemMeta((ItemMeta) dmg);

            // Партикли та повідомлення
            if (enchanted) {
                block.getWorld().spawnParticle(Particle.SPELL_WITCH, block.getLocation().add(0.5, 1, 0.5), 10, 0.2, 0.2, 0.2, 0.01);
                player.sendMessage(getLangFromPlugin("stonecutter.enchanted"));
            } else {
                block.getWorld().spawnParticle(Particle.REDSTONE, block.getLocation().add(0.5, 1, 0.5), 10, 0.2, 0.2, 0.2, new Particle.DustOptions(Color.RED, 1));
                player.sendMessage(getLangFromPlugin("stonecutter.sharpened").replace("%restore%", String.valueOf(restore)));
            }

            countMap.put(key, count + 1);
            if (count + 1 >= 3) cooldownMap.put(key, System.currentTimeMillis());
            event.setCancelled(true);
        }
    }

    private boolean isSharpenable(ItemStack item) {
        if (item == null) return false;
        Material t = item.getType();
        // Тільки залізні, золоті, діамантові, незеритові мечі/кайла/сокири
        return (t.toString().endsWith("SWORD") || t.toString().endsWith("PICKAXE") || t.toString().endsWith("AXE"))
                && !(t == Material.WOODEN_SWORD || t == Material.STONE_SWORD
                || t == Material.WOODEN_PICKAXE || t == Material.STONE_PICKAXE
                || t == Material.WOODEN_AXE || t == Material.STONE_AXE);
    }

    private boolean canSharpen(ItemStack item) {
        if (!(item.getItemMeta() instanceof Damageable)) return false;
        Damageable dmg = (Damageable) item.getItemMeta();
        Material t = item.getType();
        int max = t.toString().contains("DIAMOND") || t.toString().contains("NETHERITE") ? 600 : 140;
        return dmg.getDamage() > 0 && dmg.getDamage() >= max;
    }

    // Додаємо метод для отримання перекладу з головного плагіна (static context safe)
    private String getLangFromPlugin(String key) {
        if (plugin instanceof me.zver.vanillaplus.VanillaPlus) {
            return ((me.zver.vanillaplus.VanillaPlus)plugin).tr(key);
        }
        return key;
    }
}
