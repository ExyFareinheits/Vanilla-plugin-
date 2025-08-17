package me.zver.vanillaplus.mechanics;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class BlockInteractMechanic implements Listener {
    private final Random random = new Random();
    // Для підрахунку кліків по листві
    private final Map<Location, Integer> leavesClicks = new HashMap<>();
    private final Map<Location, Long> leavesClickTime = new HashMap<>();

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();

        // Пшениця по кругляку
        if (block.getType() == Material.COBBLESTONE && hand.getType() == Material.WHEAT && hand.getAmount() > 0) {
            int amount = 1 + random.nextInt(2); // 1-2 кісткового борошна
            hand.setAmount(hand.getAmount() - 1);
            player.getWorld().dropItemNaturally(block.getLocation().add(0.5, 1, 0.5), new ItemStack(Material.BONE_MEAL, amount));
            block.getWorld().spawnParticle(Particle.CRIT, block.getLocation().add(0.5, 1, 0.5), 10, 0.2, 0.2, 0.2, 0.01);
            block.getWorld().spawnParticle(Particle.CLOUD, block.getLocation().add(0.5, 1.2, 0.5), 20, 0.3, 0.2, 0.3, 0.01);
            event.setCancelled(true);
            return;
        }

        // Кісткове борошно на піску
        if ((block.getType() == Material.SAND || block.getType() == Material.RED_SAND)
                && hand.getType() == Material.BONE_MEAL && hand.getAmount() > 0) {
            double cactusChance = 0.10; // 10%
            double deadBushChance = 0.15; // 15%
            boolean dropped = false;
            if (random.nextDouble() < cactusChance) {
                block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 1, 0.5), new ItemStack(Material.CACTUS, 1));
                dropped = true;
            }
            if (random.nextDouble() < deadBushChance) {
                block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 1, 0.5), new ItemStack(Material.DEAD_BUSH, 1));
                dropped = true;
            }
            if (dropped) {
                hand.setAmount(hand.getAmount() - 1);
                block.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, block.getLocation().add(0.5, 1, 0.5), 10, 0.2, 0.2, 0.2, 0.01);
                event.setCancelled(true);
                return;
            }
        }

        // Кісткове борошно на грибах
        if ((block.getType() == Material.BROWN_MUSHROOM || block.getType() == Material.RED_MUSHROOM)
                && hand.getType() == Material.BONE_MEAL && hand.getAmount() > 0) {
            double bigMushroomChance = 0.25; // 25%
            if (random.nextDouble() < bigMushroomChance) {
                Material bigType = block.getType() == Material.BROWN_MUSHROOM
                        ? Material.BROWN_MUSHROOM_BLOCK
                        : Material.RED_MUSHROOM_BLOCK;
                block.setType(bigType);
                hand.setAmount(hand.getAmount() - 1);
                block.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, block.getLocation().add(0.5, 0.5, 0.5), 20, 0.3, 0.3, 0.3, 0.01);
                event.setCancelled(true);
                return;
            }
        }

        // Випадіння золота з гравію
        if (block.getType() == Material.GRAVEL) {
            double goldNuggetChance = 0.02; // 2%
            if (random.nextDouble() < goldNuggetChance) {
                block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), new ItemStack(Material.GOLD_NUGGET, 1));
                block.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, block.getLocation().add(0.5, 0.5, 0.5), 10, 0.2, 0.2, 0.2, 0.01);
            }
        }

        // Випадіння павутини з акацієвого листя
        if (block.getType() == Material.ACACIA_LEAVES) {
            double cobwebChance = 0.07; // 7%
            if (random.nextDouble() < cobwebChance) {
                block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), new ItemStack(Material.COBWEB, 1));
            }
        }

        // Листя: ламається після 4 кліків або якщо тримати більше 3 секунд
        if (block.getType().name().endsWith("_LEAVES")) {
            Location loc = block.getLocation();
            long now = System.currentTimeMillis();
            leavesClicks.put(loc, leavesClicks.getOrDefault(loc, 0) + 1);
            leavesClickTime.putIfAbsent(loc, now);

            // Якщо 4 кліки — ламаємо
            if (leavesClicks.get(loc) >= 4) {
                block.breakNaturally();
                leavesClicks.remove(loc);
                leavesClickTime.remove(loc);
                return;
            }
            // Якщо тримати більше 3 секунд (1800 мс між першим і останнім кліком)
            if (now - leavesClickTime.get(loc) > 3000) {
                block.breakNaturally();
                leavesClicks.remove(loc);
                leavesClickTime.remove(loc);
                return;
            }
            // Очищаємо якщо минуло більше 4 секунд між кліками
            if (now - leavesClickTime.get(loc) > 4000) {
                leavesClicks.remove(loc);
                leavesClickTime.remove(loc);
            }
        }
    }
}
