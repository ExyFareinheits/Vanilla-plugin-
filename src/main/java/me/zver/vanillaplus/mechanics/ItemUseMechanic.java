package me.zver.vanillaplus.mechanics;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class ItemUseMechanic implements Listener {
    // CustomModelData:
    // 2001 - (Stone Stick)
    // 2002 - (Super Bonemeal)
    // 2003 - (Tobacco)
    // 2004 - (Cigarette)
    // 2010 - (Energy Drink)
    // 2011 - (Adventure Book)
    // 2012 - (Pocket Torch)

    private final Map<UUID, Long> cigaretteCooldown = new HashMap<>();
    private final Random random = new Random();
    private Plugin plugin;

    public ItemUseMechanic(Plugin plugin) {
        this.plugin = plugin;
    }

    // Stone Stick: increased damage to mobs
    @EventHandler
    public void onStoneStickHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == Material.STICK && hasCustomModelData(hand, 2001)) {
            event.setDamage(event.getDamage() + 2.0); // +4 урону
            player.getWorld().spawnParticle(Particle.CRIT, event.getEntity().getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.01);
        }
    }

    // Super Bonemeal: applies to a 3x3 area
    @EventHandler
    public void onSuperBonemealUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        ItemStack hand = event.getPlayer().getInventory().getItemInMainHand();
        if (hand.getType() == Material.BONE_MEAL && hasCustomModelData(hand, 2002)) {
            Block center = event.getClickedBlock();
            int applied = 0;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Block b = center.getRelative(dx, 0, dz);
                    // Check if the block is suitable for bonemeal
                    if (b.getType() == Material.GRASS_BLOCK || b.getType() == Material.DIRT || b.getType() == Material.FARMLAND
                        || b.getType().name().endsWith("_SAPLING") || b.getType().name().endsWith("_CROP")) {
                        b.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, b.getLocation().add(0.5, 1, 0.5), 8, 0.2, 0.2, 0.2, 0.01);
                        b.applyBoneMeal(BlockFace.UP);
                        applied++;
                    }
                }
            }
            if (applied > 0) {
                hand.setAmount(hand.getAmount() - 1);
                event.getPlayer().sendMessage(getLangFromPlugin("bonemeal.area"));
                event.setCancelled(true);
            }
        }
    }

    // Tobacco: creates a smoke particle effect
    @EventHandler
    public void onCigaretteUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR && event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == Material.FISHING_ROD && hasCustomModelData(hand, 2004)) {
            UUID uuid = player.getUniqueId();
            long now = System.currentTimeMillis();
            if (cigaretteCooldown.containsKey(uuid) && now - cigaretteCooldown.get(uuid) < 1500) {
                player.sendMessage(getLangFromPlugin("cigarette.wait"));
                event.setCancelled(true);
                return;
            }
            cigaretteCooldown.put(uuid, now);

            // Damage durability (10-16 uses)
            hand.setDurability((short) (hand.getDurability() + 1));
            if (hand.getDurability() >= 15) {
                player.getInventory().setItemInMainHand(null);
                player.sendMessage(getLangFromPlugin("cigarette.end"));
                return;
            }

            // Effects: heal 0.5 hearts, but slow down
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 0)); // 3 сек
            player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 40, 0)); // 2 сек
            player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 1.0));
            player.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, player.getLocation().add(0, 1.5, 0), 5, 0.2, 0.2, 0.2, 0.01);
            player.sendMessage(getLangFromPlugin("cigarette.puff"));
            event.setCancelled(true);
        }
    }

    // Energy Drink: speed and jump boost
    @EventHandler
    public void onEnergyDrinkUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR && event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == Material.POTION && hasCustomModelData(hand, 2010)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 20, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 20 * 20, 1));
            player.sendMessage(getLangFromPlugin("energy.drink"));
            hand.setAmount(hand.getAmount() - 1);
            event.setCancelled(true);
        }
    }

    // Cigarette crafting: requires custom tobacco
    @EventHandler
    public void onCigaretteCraft(CraftItemEvent event) {
        ItemStack result = event.getRecipe().getResult();
        if (result.getType() == Material.FISHING_ROD && hasCustomModelData(result, 2004)) {
            boolean hasTobacco = false;
            for (ItemStack item : event.getInventory().getMatrix()) {
                if (item != null && item.getType() == Material.PAPER && hasCustomModelData(item, 2003)) {
                    hasTobacco = true;
                }
                // Якщо знайдено звичайний PAPER без CustomModelData — заборонити крафт
                if (item != null && item.getType() == Material.PAPER && !hasCustomModelData(item, 2003)) {
                    event.setCancelled(true);
                    event.getWhoClicked().sendMessage(getLangFromPlugin("cigarette.need_tobacco"));
                    return;
                }
            }
            if (!hasTobacco) {
                event.setCancelled(true);
                event.getWhoClicked().sendMessage(getLangFromPlugin("cigarette.need_tobacco"));
            }
        }
    }

    // Допоміжний метод для перевірки CustomModelData
    private boolean hasCustomModelData(ItemStack item, int data) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == data;
    }

    // Додаємо метод для отримання перекладу з головного плагіна (static context safe)
    private String getLangFromPlugin(String key) {
        if (plugin instanceof me.zver.vanillaplus.VanillaPlus) {
            return ((me.zver.vanillaplus.VanillaPlus)plugin).tr(key);
        }
        return key;
    }
}
