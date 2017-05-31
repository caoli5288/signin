package com.i5mc.signin;

import com.i5mc.signin.entity.SignIn;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.Closeable;
import java.util.Arrays;
import java.util.List;

/**
 * Created on 16-8-11.
 */
public class Holder implements InventoryHolder, Closeable {

    private final static String[] MESSAGE = {
            "",
            "§c请前往论坛签到后，重新",
            "§c进入大厅以领取今日奖励",
            "",
            "§3论坛地址： §e§nwww.i5mc.com"
    };

    private final SignIn signIn;
    private final Main main;
    private Inventory inventory;
    private boolean signed;

    private Holder(Main main, SignIn signIn) {
        this.main = main;
        this.signIn = signIn;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void update() {
        if ($.nil(inventory)) {
            inventory = main.getServer().createInventory(this, 9, "每日签到");
        }

        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "每日签到");

        if ($.nil(signIn)) {
            meta.setLore(Arrays.asList(MESSAGE));
        } else if (signed || (signed = $.today(signIn.getTime()))) {
            meta.setLore(getInfo());
        } else {
            meta.setLore(Arrays.asList(MESSAGE));
        }
        item.setItemMeta(meta);

        inventory.setItem(0, item);
    }

    private List<String> getInfo() {
        return Arrays.asList(
                reward() ? "§e点击领奖" : "§c已经领取",
                "",
                "§3基础奖励： §e" + signIn.getLastreward() + "§3点券",
                "",
                "§3连签天数： §e" + signIn.getLasted(),
                "§3额外奖励： §e" + main.getLastedReward(signIn.getLasted()),
                "§3总签天数： §e" + signIn.getDays()
        );
    }

    @Override
    public void close() {// For GC friendly.
        if (inventory != null) {
            inventory.clear();
            inventory = null;
        }
    }

    public boolean signed() {
        return signed;
    }

    public boolean reward() {
        return signIn.getTime() > signIn.getTimeApp();
    }

    public SignIn signIn() {
        return signIn;
    }

    public static Holder of(Main main, SignIn in) {
        return new Holder(main, in);
    }
}
