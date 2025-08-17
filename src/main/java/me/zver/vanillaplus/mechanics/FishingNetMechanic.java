package me.zver.vanillaplus.mechanics;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.*;

public class FishingNetMechanic implements Listener {
    private final Plugin plugin;
    private final Map<Location, DeployedNetData> deployedNets = new HashMap<>();

    public FishingNetMechanic(Plugin plugin) {
        this.plugin = plugin;
    }

    private static class DeployedNetData {
        public List<ArmorStand> stands = new ArrayList<>();
        public long finishTime;
        public boolean ready = false;
        public List<ItemStack> loot = new ArrayList<>();
        public Location baseLoc;
        public ArmorStand progressBar;
        public long startTime = 0;
    }

    // Для прогресу створення сітки
    private static class NetCraftingData {
        public ArmorStand stand;
        public int stringCount = 0;
        public int ironCount = 0;
        public int meatCount = 0;
        public boolean ready = false;
    }
    private final Map<Location, NetCraftingData> netCrafting = new HashMap<>();

    // --- Закидання сітки ПКМ по воді (оновлено: більше арморстендів, прогресбар, SHIFT+ПКМ забрати сітку, шанс порватися) ---
    @EventHandler
    public void onPlayerUseNet(org.bukkit.event.player.PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        if (!(event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR || event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)) return;
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!isFishingNet(hand)) return;

        // SHIFT+ПКМ — забрати сітку з води, якщо вона ще не зловила улов
        if (player.isSneaking()) {
            Block targetBlock = player.getTargetBlockExact(5, FluidCollisionMode.ALWAYS);
            if (targetBlock != null && targetBlock.getType() == Material.WATER) {
                Location baseLoc = targetBlock.getLocation();
                DeployedNetData net = deployedNets.get(baseLoc);
                if (net != null && !net.ready) {
                    for (ArmorStand s : net.stands) s.remove();
                    if (net.progressBar != null) net.progressBar.remove();
                    deployedNets.remove(baseLoc);
                    player.getInventory().addItem(getFishingNet());
                    player.sendMessage(getLangFromPlugin("net.back"));
                    event.setCancelled(true);
                    return;
                }
            }
        }

        Block targetBlock = null;
        if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            targetBlock = event.getClickedBlock();
        }
        if (targetBlock == null || targetBlock.getType() != Material.WATER) {
            targetBlock = player.getTargetBlockExact(5, FluidCollisionMode.ALWAYS);
            if (targetBlock == null || targetBlock.getType() != Material.WATER) {
                player.sendMessage(getLangFromPlugin("net.not_water"));
                return;
            }
        }

        Location baseLoc = targetBlock.getLocation();
        if (deployedNets.containsKey(baseLoc)) {
            player.sendMessage(getLangFromPlugin("net.already"));
            return;
        }

        hand.setAmount(hand.getAmount() - 1);

        // --- Візуалізація сітки: багато арморстендів по колу + 1 по центру (прогресбар) ---
        List<ArmorStand> stands = new ArrayList<>();
        int count = 8;
        double radius = 0.35;
        double yOffset = 0.0;
        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count;
            Vector offset = new Vector(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            Location loc = baseLoc.clone().add(0.5, yOffset, 0.5).add(offset);
            ArmorStand stand = (ArmorStand) baseLoc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
            stand.setInvisible(true);
            stand.setInvulnerable(true);
            stand.setMarker(true);
            stand.setGravity(false);
            stand.setSmall(true);
            stand.getEquipment().setHelmet(new ItemStack(Material.COBWEB));
            // Всі павутинки під кутом 90 градусів
            stand.setHeadPose(new org.bukkit.util.EulerAngle(Math.PI/2, 0, 0));
            stand.setCustomNameVisible(false);
            stands.add(stand);
        }
        // Центровий арморстенд для прогресбару
        ArmorStand progressBar = (ArmorStand) baseLoc.getWorld().spawnEntity(baseLoc.clone().add(0.5, yOffset + 0.1, 0.5), EntityType.ARMOR_STAND);
        progressBar.setInvisible(true);
        progressBar.setInvulnerable(true);
        progressBar.setMarker(true);
        progressBar.setGravity(false);
        progressBar.setSmall(true);
        progressBar.setCustomNameVisible(true);
        progressBar.setCustomName("§7[Сітка: ░░░░░░░░░░░░░░░░░░░░]");
        progressBar.getEquipment().setHelmet(new ItemStack(Material.COBWEB));
        progressBar.setHeadPose(new org.bukkit.util.EulerAngle(Math.PI/2, 0, 0));

        DeployedNetData net = new DeployedNetData();
        net.stands = stands;
        net.baseLoc = baseLoc;
        net.progressBar = progressBar;
        net.finishTime = System.currentTimeMillis() + (3 + new Random().nextInt(15)) * 60 * 1000;
        deployedNets.put(baseLoc, net);

        player.sendMessage(ChatColor.AQUA + getLangFromPlugin("net.throw"));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!deployedNets.containsKey(baseLoc)) {
                    cancel();
                    return;
                }
                DeployedNetData data = deployedNets.get(baseLoc);
                long now = System.currentTimeMillis();
                long total = data.finishTime - (data.startTime != 0 ? data.startTime : (data.startTime = now));
                long elapsed = Math.max(0, now - data.startTime);
                double progress = Math.min(1.0, (double) elapsed / (data.finishTime - data.startTime));
                int filled = (int) Math.round(progress * 20);
                StringBuilder bar = new StringBuilder(getLangFromPlugin("holo.net.progress").replace("%bar%", ""));
                for (int i = 0; i < 20; i++) bar.append(i < filled ? "§a█" : "§7░");
                bar.append("]");
                data.progressBar.setCustomName(bar.toString());
                data.progressBar.teleport(baseLoc.clone().add(0.5, yOffset + 0.1, 0.5));
                data.progressBar.getWorld().spawnParticle(Particle.END_ROD, data.progressBar.getLocation().add(0,0.4,0), 2, 0.1,0.1,0.1,0.01);

                if (now >= data.finishTime) {
                    if (new Random().nextDouble() < 0.2) {
                        for (ArmorStand s : data.stands) s.remove();
                        data.progressBar.remove();
                        deployedNets.remove(baseLoc);
                        player.sendMessage(getLangFromPlugin("net.broken"));
                        player.getWorld().spawnParticle(Particle.SMOKE_LARGE, baseLoc.clone().add(0.5, yOffset + 0.5, 0.5), 16, 0.3,0.3,0.3,0.01);
                        player.getWorld().playSound(baseLoc, Sound.ENTITY_ITEM_BREAK, 1f, 0.8f);
                        cancel();
                        return;
                    }
                    data.ready = true;
                    data.progressBar.setCustomName(getLangFromPlugin("holo.net.ready"));
                    for (ArmorStand stand : data.stands) {
                        stand.getWorld().spawnParticle(Particle.SPELL_WITCH, stand.getLocation().add(0,0.5,0), 12, 0.15,0.2,0.15, 0.05);
                        stand.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, stand.getLocation().add(0,0.5,0), 8, 0.1,0.1,0.1,0.01);
                    }
                    data.loot = getRandomNetLoot();
                    cancel();
                    return;
                }
                for (ArmorStand stand : data.stands) {
                    stand.getWorld().spawnParticle(Particle.WATER_BUBBLE, stand.getLocation().add(0,0.2,0), 3, 0.1,0.1,0.1,0.01);
                    stand.getWorld().spawnParticle(Particle.WATER_SPLASH, stand.getLocation().add(0,0.2,0), 2, 0.1,0.1,0.1,0.01);
                }
            }
        }.runTaskTimer(plugin, 0, 40);
        event.setCancelled(true);
    }

    // --- Забрати улов: підбір сітки через підхід до арморстенду (Proximity Pickup) ---
    @EventHandler
    public void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location playerLoc = player.getLocation();
        for (Map.Entry<Location, DeployedNetData> entry : deployedNets.entrySet()) {
            DeployedNetData data = entry.getValue();
            if (!data.ready) continue;
            if (data.progressBar.getLocation().distance(playerLoc) < 1.2) {
                player.getInventory().addItem(getFishingNet());
                for (ItemStack loot : data.loot) player.getInventory().addItem(loot);
                for (ArmorStand s : data.stands) s.remove();
                if (data.progressBar != null) data.progressBar.remove();
                deployedNets.remove(entry.getKey());
                player.sendMessage(getLangFromPlugin("net.pickup"));
                player.getWorld().spawnParticle(Particle.TOTEM, player.getLocation().add(0,1,0), 16, 0.4,0.7,0.4,0.2);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1.2f);
                return;
            }
        }
    }

    // --- Забрати улов через ПКМ по арморстенду (гравець підпливає і натискає ПКМ по голограмі) ---
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof ArmorStand)) return;
        ArmorStand stand = (ArmorStand) event.getRightClicked();
        String name = stand.getCustomName();
        if (name == null || !name.contains(getLangFromPlugin("holo.net.pickup"))) return;
        // Знаходимо DeployedNetData по baseLoc
        Location baseLoc = null;
        for (Map.Entry<Location, DeployedNetData> entry : deployedNets.entrySet()) {
            if (entry.getValue().stands.contains(stand)) {
                baseLoc = entry.getKey();
                break;
            }
        }
        if (baseLoc == null) return;
        DeployedNetData data = deployedNets.get(baseLoc);
        if (data == null || !data.ready) return;
        Player player = event.getPlayer();
        // Видаємо лут і сітку
        player.getInventory().addItem(getFishingNet());
        for (ItemStack loot : data.loot) player.getInventory().addItem(loot);
        // Видаляємо всі арморстенди сітки
        for (ArmorStand s : data.stands) s.remove();
        deployedNets.remove(baseLoc);
        player.sendMessage(getLangFromPlugin("net.pickup"));
        event.setCancelled(true);
    }

    // --- Якщо зламали блок під сіткою, видалити всі арморстенди ---
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        if (deployedNets.containsKey(loc)) {
            DeployedNetData net = deployedNets.get(loc);
            for (ArmorStand s : net.stands) if (s != null && !s.isDead()) s.remove();
            deployedNets.remove(loc);
        }
    }

    // --- Генерація луту (можна враховувати тип м'яса, якщо потрібно) ---
    private List<ItemStack> getRandomNetLoot() {
        List<ItemStack> loot = new ArrayList<>();
        Random r = new Random();

        // 40% шанс нічого не випаде
        if (r.nextDouble() < 0.4) return loot;

        // Більше риби, якщо крафт з риби (можна додати аргумент)
        int cod = 1 + r.nextInt(3);
        int salmon = r.nextInt(2);
        for (int i = 0; i < cod; i++) loot.add(new ItemStack(Material.COD, 1));
        for (int i = 0; i < salmon; i++) loot.add(new ItemStack(Material.SALMON, 1));

        // 3% шанс на залізний інструмент з енчантами (дуже побитий)
        if (r.nextDouble() < 0.03) {
            Material[] tools = {
                Material.IRON_SHOVEL, Material.IRON_PICKAXE, Material.IRON_AXE, Material.FISHING_ROD
            };
            ItemStack tool = new ItemStack(tools[r.nextInt(tools.length)], 1);
            ItemMeta meta = tool.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(getLangFromPlugin("net.old_tool")); // §fСтарий інструмент
                meta.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, 1, true);
                if (r.nextBoolean()) meta.addEnchant(org.bukkit.enchantments.Enchantment.LOOT_BONUS_BLOCKS, 1, true);
                tool.setItemMeta(meta);
            }
            tool.setDurability((short) (tool.getType().getMaxDurability() - r.nextInt(10) - 20));
            loot.add(tool);
        }

        // 10% шанс на руду (без діаманта/незериту)
        if (r.nextDouble() < 0.07) {
            Material[] ores = {
                Material.IRON_INGOT, Material.GOLD_INGOT, Material.COAL, Material.LAPIS_LAZULI,
                Material.REDSTONE, Material.EMERALD
            };
            loot.add(new ItemStack(ores[r.nextInt(ores.length)], 1 + r.nextInt(2)));
        }

        // Інші прості предмети (папір, морква, буряк, шкіра)
        if (r.nextDouble() < 0.15) {
            Material[] misc = {Material.PAPER, Material.CARROT, Material.BEETROOT, Material.LEATHER};
            loot.add(new ItemStack(misc[r.nextInt(misc.length)], 1));
        }

        return loot;
    }

    private boolean isFishingNet(ItemStack item) {
        if (item == null || item.getType() != Material.COBWEB) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() && meta.getDisplayName().contains(getLangFromPlugin("net.item_name")); // "Риболовна сітка"
    }

    private ItemStack getFishingNet() {
        ItemStack net = new ItemStack(Material.COBWEB, 1);
        ItemMeta meta = net.getItemMeta();
        meta.setDisplayName(getLangFromPlugin("net.item_display")); // §eРиболовна сітка
        meta.setLore(Arrays.asList(
            getLangFromPlugin("net.item_lore1"), // §7Використовуйте у воді
            getLangFromPlugin("net.item_lore2")  // §8Може зловити рибу або інші предмети
        ));
        meta.setCustomModelData(2010);
        net.setItemMeta(meta);
        return net;
    }

    // --- Взаємодія з Loom для прогресивного крафту сітки ---
    @EventHandler
    public void onPlayerInteractLoom(org.bukkit.event.player.PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Block clicked = event.getClickedBlock();
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (clicked == null || clicked.getType() != Material.LOOM) return;

        // Дозволяємо ванільний Loom, якщо гравець тримає НЕ нитку, НЕ залізо і НЕ сире м'ясо
        if (!isCraftItem(hand)) {
            // Не скасовуємо подію, Loom працює як у ванілі
            return;
        }

        Location loomLoc = clicked.getLocation();
        NetCraftingData data = netCrafting.get(loomLoc);

        if (data == null) {
            ArmorStand stand = (ArmorStand) clicked.getWorld().spawnEntity(
                    clicked.getLocation().add(0.5, 0.5, 0.5), EntityType.ARMOR_STAND);
            stand.setInvisible(true);
            stand.setInvulnerable(true);
            stand.setMarker(true);
            stand.setGravity(false);
            stand.setSmall(true);
            stand.getEquipment().setHelmet(new ItemStack(Material.COBWEB));
            stand.setCustomNameVisible(true);
            data = new NetCraftingData();
            data.stand = stand;
            netCrafting.put(loomLoc, data);
            player.sendMessage(ChatColor.AQUA + getLangFromPlugin("net.craft_start"));
            event.setCancelled(true);
        } else if (data.ready) {
            player.getInventory().addItem(getFishingNet());
            if (data.stand != null && !data.stand.isDead()) data.stand.remove();
            netCrafting.remove(loomLoc);
            player.sendMessage(ChatColor.GREEN + getLangFromPlugin("net.craft_ready"));
            event.setCancelled(true);
            return;
        } else {
            boolean added = false;
            if (hand.getType() == Material.STRING && data.stringCount < 14) {
                hand.setAmount(hand.getAmount() - 1);
                data.stringCount++;
                added = true;
            } else if (hand.getType() == Material.IRON_INGOT && data.ironCount < 4) {
                hand.setAmount(hand.getAmount() - 1);
                data.ironCount++;
                added = true;
            } else if (isRawMeat(hand) && data.meatCount < 3) {
                hand.setAmount(hand.getAmount() - 1);
                data.meatCount++;
                added = true;
            }
            double y = 0.5 + ((data.stringCount + data.ironCount + data.meatCount) / 21.0) * 1.0;
            data.stand.teleport(loomLoc.clone().add(0.5, y, 0.5));
            StringBuilder bar = new StringBuilder(getLangFromPlugin("net.craft_progress_bar"));
            int total = data.stringCount + data.ironCount + data.meatCount;
            int filled = (int) Math.round((total / 21.0) * 20);
            for (int i = 0; i < 20; i++) bar.append(i < filled ? "§a|" : "§7|");
            bar.append(getLangFromPlugin("net.craft_progress_info")
                .replace("%string%", String.valueOf(data.stringCount))
                .replace("%iron%", String.valueOf(data.ironCount))
                .replace("%meat%", String.valueOf(data.meatCount))
            );
            data.stand.setCustomName(bar.toString());
            clicked.getWorld().spawnParticle(Particle.END_ROD, data.stand.getLocation().add(0,0.5,0), 5, 0.1,0.1,0.1,0.01);
            if (data.stringCount >= 14 && data.ironCount >= 4 && data.meatCount >= 3) {
                data.ready = true;
                data.stand.setCustomName(getLangFromPlugin("net.craft_ready_holo"));
                data.stand.teleport(loomLoc.clone().add(0.5, 1.5, 0.5));
            }
            if (!added) {
                player.sendMessage(ChatColor.YELLOW + getLangFromPlugin("net.craft_need"));
            }
            event.setCancelled(true);
        }
    }

    // Якщо зламали Loom під час крафту — прогрес збивається, голограма видаляється
    @EventHandler
    public void onLoomBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.LOOM) return;
        Location loc = block.getLocation();
        if (netCrafting.containsKey(loc)) {
            NetCraftingData data = netCrafting.get(loc);
            if (data.stand != null && !data.stand.isDead()) data.stand.remove();
            netCrafting.remove(loc);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getLocation().distance(loc) < 8)
                    p.sendMessage(ChatColor.RED + getLangFromPlugin("net.making")); 
            }
        }
    }

    // Допоміжний метод: чи це сире м'ясо (НЕ смажене)
       private boolean isRawMeat(ItemStack item) {
        if (item == null) return false;
        switch (item.getType()) {
            case BEEF:
            case CHICKEN:
            case PORKCHOP:
            case MUTTON:
            case RABBIT:
            case COD:
            case SALMON:
            case TROPICAL_FISH:
            case PUFFERFISH:
                return true;
            default:
                return false;
        }
    }

    // Чи це предмет для крафту сітки
    private boolean isCraftItem(ItemStack item) {
        if (item == null) return false;
        return item.getType() == Material.STRING || item.getType() == Material.IRON_INGOT || isRawMeat(item);
    }

    // --- Дроп з сітки (повний список) ---
    /*
    Дроп з сітки:
    - COD
    - SALMON
    - IRON_SHOVEL (з енчантами, побитий)
    - IRON_PICKAXE (з енчантами, побитий)
    - IRON_AXE (з енчантами, побитий)
    - FISHING_ROD (з енчантами, побитий)
    - IRON_INGOT
    - GOLD_INGOT
    - COAL
    - LAPIS_LAZULI
    - REDSTONE
    - EMERALD
    - PAPER
    - CARROT
    - BEETROOT
    - LEATHER
    */

    // Додаємо метод для отримання перекладу з головного плагіна (static context safe)
    private String getLangFromPlugin(String key) {
        if (plugin instanceof me.zver.vanillaplus.VanillaPlus) {
            return ((me.zver.vanillaplus.VanillaPlus)plugin).tr(key);
        }
        return key;
    }
}