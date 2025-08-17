package me.zver.vanillaplus.mechanics;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class BirchSapMechanic implements Listener {
    // Зберігаємо активні точки збору соку
    private final Set<Location> sapReady = new HashSet<>();
    private final Set<Location> sapHooks = new HashSet<>();
    private final Random random = new Random();
    private final Plugin plugin;

    public BirchSapMechanic(Plugin plugin) {
        this.plugin = plugin;
    }

    // Гравець ставить крюк на березу
    @EventHandler
    public void onHookPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (block.getType() == Material.TRIPWIRE_HOOK) {
            for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
                Block attached = block.getRelative(face);
                if (attached.getType() == Material.BIRCH_LOG) {
                    for (Location loc : sapHooks) {
                        if (loc.getWorld().equals(block.getWorld()) && loc.distance(block.getLocation()) <= 5) {
                            event.getPlayer().sendMessage(getLangFromPlugin("birchsap.already"));
                            event.setCancelled(true);
                            return;
                        }
                    }
                    sapHooks.add(block.getLocation());
                    Bukkit.getScheduler().runTaskLater(
                        plugin,
                        () -> {
                            if (block.getType() == Material.TRIPWIRE_HOOK) {
                                sapReady.add(block.getLocation());
                            }
                        },
                        20L * (10 + random.nextInt(21))
                    );
                    startParticles(block.getLocation());
                    return;
                }
            }
        }
    }

    // Гравець забирає сік пляшкою
    @EventHandler
    public void onPlayerTakeSap(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.TRIPWIRE_HOOK) return;
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() != Material.GLASS_BOTTLE) return;

        Location loc = block.getLocation();
        if (!sapHooks.contains(loc)) return;

        if (sapReady.contains(loc)) {
            hand.setAmount(hand.getAmount() - 1);
            ItemStack sap = getBirchSap();
            player.getInventory().addItem(sap);
            sapReady.remove(loc);
            player.sendMessage(getLangFromPlugin("birchsap.got"));
        } else {
            player.sendMessage(getLangFromPlugin("birchsap.not_ready"));
        }
        event.setCancelled(true);
    }

    // При зламі крюка — прибираємо все
    @EventHandler
    public void onHookBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() == Material.TRIPWIRE_HOOK) {
            sapHooks.remove(block.getLocation());
            sapReady.remove(block.getLocation());
        }
    }

    // Партикли: червоний якщо соку нема, зелений якщо є
    private void startParticles(Location loc) {
        Bukkit.getScheduler().runTaskTimer(
            plugin,
            new Runnable() {
                @Override
                public void run() {
                    if (!sapHooks.contains(loc)) {
                        // Крюк зняли — зупинити партикли
                        Bukkit.getScheduler().cancelTasks(plugin);
                        return;
                    }
                    World world = loc.getWorld();
                    if (sapReady.contains(loc)) {
                        world.spawnParticle(Particle.VILLAGER_HAPPY, loc.clone().add(0.5, 0.2, 0.5), 2, 0.1, 0.1, 0.1, 0.01);
                    } else {
                        world.spawnParticle(Particle.REDSTONE, loc.clone().add(0.5, 0.2, 0.5), 2, 0.1, 0.1, 0.1, new Particle.DustOptions(Color.RED, 1));
                    }
                }
            },
            0L, 20L
        );
    }

    // Гравець використовує березовий сік
    @EventHandler
    public void onPlayerUseBirchSap(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR &&
            event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!isBirchSap(hand)) return;

        PotionEffectType[] effects = PotionEffectType.values();
        PotionEffectType effect = effects[random.nextInt(effects.length)];
        int duration = 60 + random.nextInt(15 * 20);
        int amplifier = random.nextInt(2);

        player.addPotionEffect(new PotionEffect(effect, duration, amplifier));
        player.sendMessage(getLangFromPlugin("birchsap.drink"));
        hand.setAmount(hand.getAmount() - 1);
        event.setCancelled(true);
    }

    // Для отримання соку: затримка 1-4 хвилини (реалізуйте у місці, де гравець збирає сік)
    public void startBirchSapCollection(Player player, Block birch) {
        int delay = 60 + random.nextInt(180);
        player.sendMessage(getLangFromPlugin("birchsap.wait").replace("%minutes%", String.valueOf(delay / 60)));
        new BukkitRunnable() {
            @Override
            public void run() {
                player.getInventory().addItem(getBirchSap());
                player.sendMessage(getLangFromPlugin("birchsap.got"));
            }
        }.runTaskLater(plugin, delay * 20L);
    }

    private boolean isBirchSap(ItemStack item) {
        if (item == null || item.getType() != Material.HONEY_BOTTLE) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() && meta.getDisplayName().contains("Березовий сік");
    }

    private ItemStack getBirchSap() {
        ItemStack sap = new ItemStack(Material.HONEY_BOTTLE, 1);
        ItemMeta meta = sap.getItemMeta();
        meta.setDisplayName(getLangFromPlugin("birchsap.name"));
        meta.setLore(Arrays.asList(
            getLangFromPlugin("birchsap.lore1"),
            getLangFromPlugin("birchsap.lore2")
        ));
        sap.setItemMeta(meta);
        return sap;
    }

    // Додаємо метод для отримання перекладу з головного плагіна (static context safe)
    private String getLangFromPlugin(String key) {
        if (plugin instanceof me.zver.vanillaplus.VanillaPlus) {
            return ((me.zver.vanillaplus.VanillaPlus)plugin).tr(key);
        }
        return key;
    }
}
