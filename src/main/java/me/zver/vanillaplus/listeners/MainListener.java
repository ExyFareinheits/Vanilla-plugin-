package me.zver.vanillaplus.listeners;

import java.util.Random;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class MainListener implements Listener {
    private final JavaPlugin plugin;
    private final Random random = new Random();

    public MainListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();

        // Перемелювання пшениці об кругляк (тільки якщо в руці пшениця)
        if (block.getType() == Material.COBBLESTONE && hand.getType() == Material.WHEAT && hand.getAmount() > 0) {
            int amount = 1 + random.nextInt(2); // 1-2 кісткового борошна
            hand.setAmount(hand.getAmount() - 1);
            player.getWorld().dropItemNaturally(block.getLocation().add(0.5, 1, 0.5), new ItemStack(Material.BONE_MEAL, amount));
            block.getWorld().spawnParticle(Particle.CRIT, block.getLocation().add(0.5, 1, 0.5), 10, 0.2, 0.2, 0.2, 0.01);
            block.getWorld().spawnParticle(Particle.CLOUD, block.getLocation().add(0.5, 1.2, 0.5), 20, 0.3, 0.2, 0.3, 0.01);
            event.setCancelled(true);
            return;
        }

        // Використання кісткового борошна на піску
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

        // Використання кісткового борошна на грибах
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

        // Інші дії при взаємодії з блоками
        Material type = block.getType();
        if (!type.name().endsWith("_LEAVES")) return;

        // Візуальний ефект "трусіння" (частинки)
        block.getWorld().spawnParticle(Particle.BLOCK_CRACK, block.getLocation().add(0.5, 0.5, 0.5), 20, 0.3, 0.3, 0.3, block.getBlockData());

        // Випадіння предметів з шансом
        double stickChance = 0.25; // 25%
        double leavesChance = 0.10; // 10%
        double honeycombChance = 0.03; // 3%

        if (random.nextDouble() < stickChance) {
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), new ItemStack(Material.STICK, 1));
        }
        if (random.nextDouble() < leavesChance) {
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), new ItemStack(type, 1));
        }
        if (random.nextDouble() < honeycombChance) {
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), new ItemStack(Material.HONEYCOMB, 1));
        }

        // Випадіння павутини з акацієвого листя
        if (type == Material.ACACIA_LEAVES) {
            double cobwebChance = 0.07; // 7%
            if (random.nextDouble() < cobwebChance) {
                block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), new ItemStack(Material.COBWEB, 1));
            }
        }
    }
}
