package com.i5mc.signin;

import com.i5mc.signin.entity.SignIn;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.Closeable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created on 16-8-11.
 */
public class Holder implements InventoryHolder, Closeable {

    private final static SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private final static String[] MESSAGE = {
            "",
            "请前往论坛签到后，重新",
            "登陆游戏以领取今日奖励",
            "",
            "论坛地址：www.i5mc.com"
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
        if (Main.eq(inventory, null)) {
            inventory = main.getServer().createInventory(this, 9, "每日签到");
        }

        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "每日签到");

        if (Main.eq(signIn, null)) {
            meta.setLore(LineList.of(MESSAGE));
        } else if (signed || (signed = Main.eq(FORMAT.format(new Date(signIn.getTime() * 1000L)), FORMAT.format(new Date())))) {
            meta.setLore(getInfo());
        } else {
            meta.setLore(LineList.of(MESSAGE));
        }
        item.setItemMeta(meta);

        inventory.setItem(0, item);
    }

    private List<String> getInfo() {
        return LineList.of(
                reward() ? "点击领奖" : "已经领取",
                "",
                "今日奖励：" + signIn.getLastreward() + "点",
                "",
                "连签天数：" + signIn.getLasted(),
                "总签天数：" + signIn.getDays()
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
