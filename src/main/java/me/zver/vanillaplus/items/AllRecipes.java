package me.zver.vanillaplus.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class AllRecipes {
    // Повертає всі ItemStack-и результатів кастомних рецептів для відображення або документації

    public static ItemStack stoneStick() {
        ItemStack item = new ItemStack(Material.STICK, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(me.zver.vanillaplus.VanillaPlus.getInstance().tr("allrecipes.stone_stick_name"));
        meta.setLore(Arrays.asList(me.zver.vanillaplus.VanillaPlus.getInstance().tr("allrecipes.stone_stick_lore")));
        meta.setCustomModelData(2001);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack shapedSaddle() {
        ItemStack item = new ItemStack(Material.SADDLE, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(me.zver.vanillaplus.VanillaPlus.getInstance().tr("allrecipes.saddle_name"));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack nameTag() {
        return new ItemStack(Material.NAME_TAG, 1);
    }

    public static ItemStack slimeBall() {
        return new ItemStack(Material.SLIME_BALL, 1);
    }

    public static ItemStack brush() {
        return new ItemStack(Material.BRUSH, 1);
    }

    public static ItemStack heartOfTheSea() {
        return new ItemStack(Material.HEART_OF_THE_SEA, 1);
    }

    public static ItemStack minerShield() {
        ItemStack item = new ItemStack(Material.SHIELD, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(me.zver.vanillaplus.VanillaPlus.getInstance().tr("allrecipes.miner_shield_name"));
        meta.setLore(Arrays.asList(
            me.zver.vanillaplus.VanillaPlus.getInstance().tr("allrecipes.miner_shield_lore1"),
            me.zver.vanillaplus.VanillaPlus.getInstance().tr("allrecipes.miner_shield_lore2")
        ));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack superTorch() {
        ItemStack item = new ItemStack(Material.TORCH, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(me.zver.vanillaplus.VanillaPlus.getInstance().tr("allrecipes.super_torch_name"));
        meta.setLore(Arrays.asList(me.zver.vanillaplus.VanillaPlus.getInstance().tr("allrecipes.super_torch_lore")));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack pouchOfDust() {
        ItemStack item = new ItemStack(Material.GUNPOWDER, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(me.zver.vanillaplus.VanillaPlus.getInstance().tr("allrecipes.pouch_dust_name"));
        meta.setLore(Arrays.asList(me.zver.vanillaplus.VanillaPlus.getInstance().tr("allrecipes.pouch_dust_lore")));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack xpBook() {
        ItemStack item = new ItemStack(Material.BOOK, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(me.zver.vanillaplus.VanillaPlus.getInstance().tr("allrecipes.xp_book_name"));
        meta.setLore(Arrays.asList(me.zver.vanillaplus.VanillaPlus.getInstance().tr("allrecipes.xp_book_lore")));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack superBonemeal() {
        ItemStack item = new ItemStack(Material.BONE_MEAL, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(me.zver.vanillaplus.VanillaPlus.getInstance().tr("allrecipes.super_bonemeal_name"));
        meta.setLore(Arrays.asList(me.zver.vanillaplus.VanillaPlus.getInstance().tr("allrecipes.super_bonemeal_lore")));
        meta.setCustomModelData(2002);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack tobacco() {
        ItemStack item = new ItemStack(Material.PAPER, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(me.zver.vanillaplus.VanillaPlus.getInstance().tr("allrecipes.tobacco_name"));
        meta.setLore(Arrays.asList(me.zver.vanillaplus.VanillaPlus.getInstance().tr("allrecipes.tobacco_lore")));
        meta.setCustomModelData(2003);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack cigarette() {
        ItemStack item = new ItemStack(Material.FISHING_ROD, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(me.zver.vanillaplus.VanillaPlus.getInstance().tr("allrecipes.cigarette_name"));
        meta.setLore(Arrays.asList(
            me.zver.vanillaplus.VanillaPlus.getInstance().tr("allrecipes.cigarette_lore1"),
            me.zver.vanillaplus.VanillaPlus.getInstance().tr("allrecipes.cigarette_lore2")
        ));
        meta.setCustomModelData(2004);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack pocketTorch() {
        ItemStack item = new ItemStack(Material.TORCH, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(me.zver.vanillaplus.VanillaPlus.getInstance().tr("allrecipes.pocket_torch_name"));
        meta.setLore(Arrays.asList(
            me.zver.vanillaplus.VanillaPlus.getInstance().tr("allrecipes.pocket_torch_lore1"),
            me.zver.vanillaplus.VanillaPlus.getInstance().tr("allrecipes.pocket_torch_lore2")
        ));
        meta.setCustomModelData(2012);
        item.setItemMeta(meta);
        return item;
    }


}

