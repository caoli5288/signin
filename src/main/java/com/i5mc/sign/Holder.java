package com.i5mc.sign;

import com.google.common.collect.ImmutableList;
import com.i5mc.sign.entity.LocalSign;
import lombok.val;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.io.Closeable;
import java.time.LocalDate;
import java.util.List;

import static com.i5mc.sign.$.nil;

/**
 * Created on 16-8-11.
 */
public class Holder implements InventoryHolder, Closeable {

    final LocalSign sign;
    private final Main main;
    private Inventory inventory;
    boolean signed;

    private Holder(Main main, LocalSign sign) {
        this.main = main;
        this.sign = sign;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void update() {
        if (nil(inventory)) {
            inventory = main.getServer().createInventory(this, 9, "每日签到");
        }

        val latest = sign.getLatest();
        if (!nil(latest)) {
            signed = latest.toLocalDateTime().toLocalDate().isEqual(LocalDate.now());
        }

        val item = new ItemStack(Material.BOOK);
        val meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "每日签到");
        meta.setLore(getInfo());

        item.setItemMeta(meta);

        inventory.setItem(0, item);
    }

    private List<String> getInfo() {
        ImmutableList.Builder<String> b = ImmutableList.builder();

        if (signed) {
            b.add("§c已经领取");
            b.add("");
        } else {
            b.add("§e点击领奖");
            b.add("");
            b.add("§3基础奖励： §e" + LocalMgr.getDaily().getDisplay());
            val gift = LocalMgr.getLast(1 + sign.getLasted());
            if (!nil(gift)) {
                b.add("§3额外奖励： §e" + gift.getDisplay());
            }
            b.add("");
        }

        b.add("§3连签天数： §e" + sign.getLasted());
        b.add("§3总签天数： §e" + sign.getDayTotal());
        val latest = sign.getLatest();
        if (!nil(latest)) {
            b.add("§3上次签到： §e" + latest.toString().substring(0, 16));
        }

        return b.build();
    }

    @Override
    public void close() {// For GC friendly.
        if (inventory != null) {
            inventory.clear();
            inventory = null;
        }
    }

    public static Holder of(Main main, LocalSign sign) {
        return new Holder(main, sign);
    }
}
