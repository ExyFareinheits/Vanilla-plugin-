package me.zver.vanillaplus.items;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.bukkit.block.BlockFace;

import java.util.*;

public class AxeTreeChopMechanic implements Listener {
    private final Map<Block, Integer> chopProgress = new HashMap<>();
    private final Plugin plugin;

    public AxeTreeChopMechanic(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAxeBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material mat = block.getType();
        Player player = event.getPlayer();
        ItemStack axe = player.getInventory().getItemInMainHand();

        // Додаємо перевірку на SHIFT
        if (!player.isSneaking()) return;

        if (!isLog(mat)) return;
        if (!isAxe(axe)) return;

        int requiredHits = getRequiredHits(axe.getType());
        int progress = chopProgress.getOrDefault(block, 0) + 1;

        // Партікли та звук
        block.getWorld().spawnParticle(Particle.BLOCK_CRACK, block.getLocation().add(0.5,0.5,0.5), 12, 0.3,0.3,0.3, 0.1, block.getBlockData());
        block.getWorld().playSound(block.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.7f, 1.1f);

        if (progress >= requiredHits) {
            chopProgress.remove(block);
            breakTree(block, player);
            // Дозволяємо зламати блок (повалити дерево)
        } else {
            chopProgress.put(block, progress);
            event.setCancelled(true); // Не дозволяємо зламати, поки не досягнуто ліміт
        }
    }

    private boolean isLog(Material mat) {
        String name = mat.name();
        return name.endsWith("_LOG") || name.endsWith("_STEM");
    }

    private boolean isAxe(ItemStack item) {
        if (item == null) return false;
        switch (item.getType()) {
            case WOODEN_AXE:
            case STONE_AXE:
            case IRON_AXE:
            case GOLDEN_AXE:
            case DIAMOND_AXE:
            case NETHERITE_AXE:
                return true;
            default:
                return false;
        }
    }

    private int getRequiredHits(Material axeType) {
        switch (axeType) {
            case WOODEN_AXE: return 3 + new Random().nextInt(2); // 3-4
            case STONE_AXE: return 2 + new Random().nextInt(2); // 2-3
            case IRON_AXE:
            case GOLDEN_AXE:
            case DIAMOND_AXE:
            case NETHERITE_AXE:
                return 2;
            default: return 4;
        }
    }

    private void breakTree(Block start, Player player) {
        Set<Block> visited = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();
        queue.add(start);

        while (!queue.isEmpty()) {
            Block b = queue.poll();
            if (!visited.add(b)) continue;
            if (!isLog(b.getType())) continue;
            b.breakNaturally(player.getInventory().getItemInMainHand());
            for (BlockFace face : BlockFace.values()) {
                Block rel = b.getRelative(face);
                if (!visited.contains(rel) && isLog(rel.getType())) {
                    queue.add(rel);
                }
            }
        }
    }
}