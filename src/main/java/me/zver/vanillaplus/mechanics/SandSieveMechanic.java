package me.zver.vanillaplus.mechanics;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class SandSieveMechanic implements Listener {
    private final Plugin plugin;
    private final Map<Location, SieveData> sieves = new HashMap<>();

    public SandSieveMechanic(Plugin plugin) {
        this.plugin = plugin;
    }

    private static class SieveData {
        public ArmorStand hologram;
        public int filteredBlocks = 0;
    }

    // Перевірка конструкції сита (scaffolding + 4 trapdoor)
    private boolean isValidSieve(Block center) {
        if (center.getType() != Material.SCAFFOLDING) return false;
        BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        for (BlockFace face : faces) {
            Block b = center.getRelative(face);
            if (!b.getType().name().endsWith("TRAPDOOR")) return false;
        }
        return true;
    }

    // Гравець ставить блок — перевіряємо сито
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (block.getType() == Material.SCAFFOLDING || block.getType().name().endsWith("TRAPDOOR")) {
            // Перевіряємо всі scaffolding поруч
            for (BlockFace face : BlockFace.values()) {
                Block center = block.getRelative(face);
                if (isValidSieve(center) && !sieves.containsKey(center.getLocation())) {
                    // Створюємо голограму
                    ArmorStand stand = (ArmorStand) center.getWorld().spawnEntity(center.getLocation().add(0.5, 1.2, 0.5), EntityType.ARMOR_STAND);
                    stand.setInvisible(true);
                    stand.setMarker(true);
                    stand.setCustomName(getLangFromPlugin("holo.sieve.progress").replace("%count%", "0"));
                    stand.setCustomNameVisible(true);
                    stand.setGravity(false);

                    SieveData data = new SieveData();
                    data.hologram = stand;
                    sieves.put(center.getLocation(), data);

                    // Сірий партикл
                    center.getWorld().spawnParticle(Particle.SMOKE_NORMAL, center.getLocation().add(0.5, 1, 0.5), 20, 0.3, 0.2, 0.3, 0.01);
                    event.getPlayer().sendMessage(ChatColor.GRAY + getLangFromPlugin("sieve.created"));
                    return;
                }
            }
        }
    }

    // Гравець клацає по ситу з піском або гравієм
    @EventHandler
    public void onPlayerUseSieve(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.SCAFFOLDING) return;
        Location loc = block.getLocation();
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (!sieves.containsKey(loc)) {
            player.sendMessage(ChatColor.RED + getLangFromPlugin("sieve.not_sieve"));
            return;
        }

        if (hand.getType() != Material.SAND && hand.getType() != Material.GRAVEL) {
            player.sendMessage(ChatColor.YELLOW + getLangFromPlugin("sieve.use_sand"));
            return;
        }

        SieveData data = sieves.get(loc);
        hand.setAmount(hand.getAmount() - 1);
        data.filteredBlocks++;
        updateHologram(data);

        // Партикли завжди будуть!
        block.getWorld().spawnParticle(Particle.CLOUD, block.getLocation().add(0.5, 1, 0.5), 10, 0.2, 0.2, 0.2, 0.01);

        // Дроп
        ItemStack drop = getSieveDrop(hand.getType());
        if (drop.getType() != Material.AIR) {
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 1, 0.5), drop);
            // Партикли успіху, якщо щось випало
            block.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, block.getLocation().add(0.5, 1, 0.5), 8, 0.2, 0.2, 0.2, 0.01);
        } else {
            // Партикли "нічого не знайдено"
            block.getWorld().spawnParticle(Particle.SMOKE_NORMAL, block.getLocation().add(0.5, 1, 0.5), 8, 0.2, 0.2, 0.2, 0.01);
        }
        // Ламаємо блок піску/гравію під курсором (якщо він є)
        Block above = block.getRelative(BlockFace.UP);
        if (above.getType() == Material.SAND || above.getType() == Material.GRAVEL) {
            above.setType(Material.AIR);
        }

        event.setCancelled(true);
    }

    // Зламали сито — прибираємо голограму і дані
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location loc = block.getLocation();
        if (sieves.containsKey(loc)) {
            SieveData data = sieves.remove(loc);
            if (data.hologram != null && !data.hologram.isDead()) data.hologram.remove();
            block.getWorld().spawnParticle(Particle.REDSTONE, block.getLocation().add(0.5, 1, 0.5), 10, 0.2, 0.2, 0.2, new Particle.DustOptions(Color.GRAY, 1));
            block.getWorld().playSound(block.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1, 1);
            for (Player p : block.getWorld().getPlayers()) {
                if (p.getLocation().distance(loc) < 8)
                    p.sendMessage(ChatColor.RED + getLangFromPlugin("sieve.broken"));
            }
        }
    }

    private void updateHologram(SieveData data) {
        if (data.hologram != null && !data.hologram.isDead()) {
            data.hologram.setCustomName(getLangFromPlugin("holo.sieve.progress").replace("%count%", String.valueOf(data.filteredBlocks)));
        }
    }

    // Дроп для піску/гравію
    private ItemStack getSieveDrop(Material type) {
        double roll = Math.random();
        // 80% шанс нічого не випаде
        if (roll < 0.8) {
            return new ItemStack(Material.AIR, 1);
        }
        if (type == Material.SAND) {
            if (roll < 0.70) { // сміття (пісок, гравій, вугілля, бруд)
                Material[] trash = {Material.COBBLESTONE, Material.COAL, Material.DIRT, Material.STRING};
                return new ItemStack(trash[new Random().nextInt(trash.length)], 1);
            } else if (roll < 0.85) { // руди (самородки, порох, редстоун)
                Material[] ores = {Material.IRON_NUGGET, Material.GOLD_NUGGET, Material.CHARCOAL, Material.GUNPOWDER, Material.REDSTONE};
                return new ItemStack(ores[new Random().nextInt(ores.length)], 1);
            } else { // інше (папір, морква, буряк, шкіра)
                Material[] misc = {Material.PAPER, Material.CARROT, Material.BEETROOT, Material.LEATHER};
                return new ItemStack(misc[new Random().nextInt(misc.length)], 1);
            }
        } else { // GRAVEL
            if (roll < 0.70) {
                Material[] trash = {Material.SAND, Material.FLINT, Material.DIRT, Material.GRAVEL};
                return new ItemStack(trash[new Random().nextInt(trash.length)], 1);
            } else if (roll < 0.85) {
                Material[] ores = {Material.IRON_NUGGET, Material.GOLD_NUGGET, Material.CHARCOAL, Material.GUNPOWDER, Material.REDSTONE};
                return new ItemStack(ores[new Random().nextInt(ores.length)], 1);
            } else {
                Material[] misc = {Material.PAPER, Material.CARROT, Material.BEETROOT, Material.LEATHER};
                return new ItemStack(misc[new Random().nextInt(misc.length)], 1);
            }
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

