package com.i5mc.sign;

import com.i5mc.sign.entity.SignMissing;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.time.LocalDate;
import java.util.Iterator;

/**
 * Created by on 1月2日.
 */
public class SignViewHandler implements InventoryHolder {

    private Inventory inventory;

    private final Player p;
    private final LocalDate begin;
    private final LocalDate end;

    public SignViewHandler(Player p, String yearAndMonth) {
        this.p = p;
        begin = LocalDate.parse(yearAndMonth + "-01");
        end = LocalDate.parse(yearAndMonth + "-" + begin.getMonth().length(begin.isLeapYear()));
    }

    public Inventory getInventory() {
        if (inventory == null) init();

        return inventory;
    }

    private void init() {
        inventory = Bukkit.createInventory(this, 45, "" + begin + " -> " + end);
        // TODO
    }

    private void missing(Iterator<SignMissing> itr) {

    }

    private void fill(int i, int l, Panel panel) {
        fill(i++, panel);
        if (i <= l) fill(i, l, panel);
    }

    private void fill(int slot, Panel panel) {
        ItemStack item = new ItemStack(160, slot, (short) panel.date, (byte) panel.date);
        inventory.setItem(slot - 1, item);
    }

    private enum Panel {

        OKAY(5),
        NO(14),
        UNKNOWN(15);

        private final int date;

        Panel(int date) {
            this.date = date;
        }
    }

}
