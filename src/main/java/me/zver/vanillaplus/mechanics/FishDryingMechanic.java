package me.zver.vanillaplus.mechanics;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class FishDryingMechanic implements Listener {
    private final Plugin plugin;
    // Зберігаємо сушарки та їхній стан
    private final Map<Location, DryingFishData> dryingFish = new HashMap<>();

    public FishDryingMechanic(Plugin plugin) {
        this.plugin = plugin;
    }

    // Допоміжний клас для сушіння рибки
    private static class DryingFishData {
        public ArmorStand stand;
        public ItemStack fish;
        public long finishTime;
        public boolean ready = false;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Block clicked = event.getClickedBlock();
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();

        // --- CIGARETTE CRAFTING ---
        if (isTobacco(hand) && isPaper(player.getInventory().getItemInOffHand())) {
            ItemStack off = player.getInventory().getItemInOffHand();
            hand.setAmount(hand.getAmount() - 1);
            off.setAmount(off.getAmount() - 1);
            player.getInventory().addItem(getCigarette());
            player.sendMessage(getLangFromPlugin("fishdryer.cigarette"));
            event.setCancelled(true);
            return;
        }

        // Перевірка: чи це нитка (STRING) у сушарці
        if (clicked.getType() != Material.TRIPWIRE) return;

        // Перевіряємо конструкцію сушарки: 2-7 підряд tripwire
        if (!isValidDryer(clicked)) {
            player.sendMessage(getLangFromPlugin("fishdryer.invalid"));
            return;
        }

        Location dryerLoc = clicked.getLocation();

        // Якщо вже сушиться предмет
        if (dryingFish.containsKey(dryerLoc)) {
            DryingFishData data = dryingFish.get(dryerLoc);
            if (data.ready) {
                ItemStack dried;
                if (isRawFish(data.fish)) {
                    dried = getDriedFish(data.fish);
                } else if (isPoppy(data.fish)) {
                    dried = getDriedTobacco();
                } else {
                    dried = data.fish.clone();
                }
                player.getInventory().addItem(dried);
                data.stand.remove();
                dryingFish.remove(dryerLoc);
                player.sendMessage(getLangFromPlugin("fishdryer.ready"));
            } else {
                long mins = Math.max(1, (data.finishTime - System.currentTimeMillis()) / 1000 / 60);
                player.sendMessage(getLangFromPlugin("fishdryer.not_ready").replace("%minutes%", String.valueOf(mins)));
            }
            event.setCancelled(true);
            return;
        }

        // Якщо гравець тримає рибу або мак (poppy)
        if (!isRawFish(hand) && !isPoppy(hand)) {
            player.sendMessage(getLangFromPlugin("fishdryer.only_raw"));
            return;
        }

        // Почати сушіння
        ItemStack itemToDry = hand.clone();
        itemToDry.setAmount(1);
        hand.setAmount(hand.getAmount() - 1);

        ArmorStand stand = (ArmorStand) clicked.getWorld().spawnEntity(
                clicked.getLocation().add(0.5, 0.1, 0.5), EntityType.ARMOR_STAND);
        stand.setInvisible(true);
        stand.setInvulnerable(true);
        stand.setMarker(true);
        stand.setGravity(false);
        stand.setSmall(true);
        stand.getEquipment().setHelmet(itemToDry);
        stand.setCustomName(isRawFish(itemToDry) ? getLangFromPlugin("holo.fishdryer.fish") : getLangFromPlugin("holo.fishdryer.poppy"));
        stand.setCustomNameVisible(false);

        DryingFishData drying = new DryingFishData();
        drying.stand = stand;
        drying.fish = itemToDry;
        drying.finishTime = System.currentTimeMillis() + (isRawFish(itemToDry) ? 15 : 5) * 60 * 1000;

        dryingFish.put(dryerLoc, drying);

        player.sendMessage(ChatColor.AQUA + (isRawFish(itemToDry) ?
                getLangFromPlugin("fishdryer.started_fish") :
                getLangFromPlugin("fishdryer.started_poppy")));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!dryingFish.containsKey(dryerLoc)) {
                    cancel();
                    return;
                }
                DryingFishData data = dryingFish.get(dryerLoc);
                if (System.currentTimeMillis() >= data.finishTime) {
                    data.ready = true;
                    data.stand.setCustomName(ChatColor.GOLD + (isRawFish(data.fish) ?
                        getLangFromPlugin("holo.fishdryer.done_fish") : getLangFromPlugin("holo.fishdryer.done_poppy")));
                    data.stand.setCustomNameVisible(true);
                    Player p = Bukkit.getPlayer(player.getUniqueId());
                    if (p != null) {
                        p.sendMessage(ChatColor.GREEN + (isRawFish(data.fish) ?
                            getLangFromPlugin("fishdryer.done_fish") : getLangFromPlugin("fishdryer.done_poppy")));
                    }
                    cancel();
                    return;
                }
                clicked.getWorld().spawnParticle(Particle.CLOUD, stand.getLocation().add(0, 0.5, 0), 2, 0.1, 0.1, 0.1, 0.01);
                clicked.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, stand.getLocation().add(0, 0.5, 0), 1, 0.1, 0.1, 0.1, 0.01);
            }
        }.runTaskTimer(plugin, 0, 40);

        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        if (dryingFish.containsKey(loc)) {
            DryingFishData dry = dryingFish.get(loc);
            if (dry.stand != null && !dry.stand.isDead()) dry.stand.remove();
            dryingFish.remove(loc);
        }
    }

    private boolean isValidDryer(Block tripwire) {
        int maxLength = 7;
        int minLength = 2;
        for (BlockFace axis : new BlockFace[]{BlockFace.EAST, BlockFace.NORTH}) {
            int length = 1;
            Block b = tripwire;
            for (int i = 1; i < maxLength; i++) {
                b = b.getRelative(axis);
                if (b.getType() == Material.TRIPWIRE) length++;
                else break;
            }
            b = tripwire;
            for (int i = 1; i < maxLength; i++) {
                b = b.getRelative(axis.getOppositeFace());
                if (b.getType() == Material.TRIPWIRE) length++;
                else break;
            }
            if (length >= minLength && length <= maxLength) return true;
        }
        return false;
    }

    private boolean isRawFish(ItemStack item) {
        if (item == null) return false;
        switch (item.getType()) {
            case COD:
            case SALMON:
            case PUFFERFISH:
            case TROPICAL_FISH:
                return true;
            default:
                return false;
        }
    }

    private boolean isPoppy(ItemStack item) {
        return item != null && item.getType() == Material.POPPY;
    }

    private ItemStack getDriedFish(ItemStack rawFish) {
        ItemStack dried = rawFish.clone();
        ItemMeta meta = dried.getItemMeta();
        meta.setDisplayName(getLangFromPlugin("fishdryer.dried_name"));
        meta.setLore(Arrays.asList(
            getLangFromPlugin("fishdryer.dried_lore1"),
            getLangFromPlugin("fishdryer.dried_lore2")
        ));
        dried.setItemMeta(meta);
        dried.setType(Material.DRIED_KELP); // або залишити як COD/SALMON
        return dried;
    }

    private ItemStack getDriedTobacco() {
        ItemStack tobacco = new ItemStack(Material.PAPER, 1);
        ItemMeta meta = tobacco.getItemMeta();
        meta.setDisplayName(getLangFromPlugin("fishdryer.tobacco_name"));
        meta.setLore(Arrays.asList(getLangFromPlugin("fishdryer.tobacco_lore")));
        meta.setCustomModelData(2003);
        tobacco.setItemMeta(meta);
        return tobacco;
    }

    private ItemStack getCigarette() {
        ItemStack cig = new ItemStack(Material.PAPER, 1);
        ItemMeta meta = cig.getItemMeta();
        meta.setDisplayName(getLangFromPlugin("fishdryer.cigarette_name"));
        meta.setLore(Arrays.asList(
            getLangFromPlugin("fishdryer.cigarette_lore1"),
            getLangFromPlugin("fishdryer.cigarette_lore2")
        ));
        meta.setCustomModelData(2011);
        cig.setItemMeta(meta);
        return cig;
    }

    private boolean isPaper(ItemStack item) {
        return item != null && item.getType() == Material.PAPER && (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName());
    }

    private boolean isTobacco(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() && meta.getDisplayName().contains("Табак");
    }

    // Додаємо метод для отримання перекладу з головного плагіна (static context safe)
    private String getLangFromPlugin(String key) {
        if (plugin instanceof me.zver.vanillaplus.VanillaPlus) {
            return ((me.zver.vanillaplus.VanillaPlus)plugin).tr(key);
        }
        return key;
    }
}