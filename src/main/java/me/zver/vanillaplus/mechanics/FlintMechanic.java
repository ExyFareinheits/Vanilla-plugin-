package me.zver.vanillaplus.mechanics;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.plugin.Plugin;

import java.util.Random;

public class FlintMechanic implements Listener {
    private final Random random = new Random();
    private Plugin plugin;

    public FlintMechanic(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onFlintUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        Player player = event.getPlayer();
        if (block == null) return;
        ItemStack hand = player.getInventory().getItemInMainHand();

        // Перевіряємо, чи гравець тримає кремінь і клацає по каменю
        if (hand.getType() == Material.FLINT && block.getType() == Material.STONE) {
            // Ефект іскри
            block.getWorld().spawnParticle(Particle.FLAME, block.getLocation().add(0.5, 1, 0.5), 8, 0.2, 0.1, 0.2, 0.01);
            block.getWorld().playSound(block.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.7f, 1.7f);

            // Шанс підпалити блок поруч (тільки якщо зверху повітря)
            Block above = block.getRelative(BlockFace.UP);
            if (above.getType() == Material.AIR && random.nextDouble() < 0.15) {
                above.setType(Material.FIRE);
                player.sendMessage(getLangFromPlugin("flint.fire"));
            } else {
                player.sendMessage(getLangFromPlugin("flint.spark"));
            }
            event.setCancelled(true);
        }
    }

    // Додаємо метод для отримання перекладу з головного плагіна (static context safe)
    private String getLangFromPlugin(String key) {
        if (plugin instanceof me.zver.vanillaplus.VanillaPlus) {
            return ((me.zver.vanillaplus.VanillaPlus)plugin).tr(key);
        }
        return key;
    }
}