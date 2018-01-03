package com.i5mc.sign;

import com.i5mc.sign.entity.LocalSign;
import com.i5mc.sign.entity.SignMissing;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.i5mc.sign.$.nil;

/**
 * Created by on 1月2日.
 */
public class ViewHandler implements InventoryHolder {

    private final Player p;
    private final LocalDate begin;
    private final LocalDate end;
    private final int month;
    private final boolean singleton;

    private Inventory inventory;

    ViewHandler(Player p, int month, boolean singleton) {
        this.p = p;
        this.month = month;
        this.singleton = singleton;
        begin = month >= 1 ? LocalDate.now().minusMonths(month).withDayOfMonth(1) : LocalDate.now().withDayOfMonth(1);
        end = begin.withDayOfMonth(begin.getMonth().length(begin.isLeapYear()));
    }

    public Inventory getInventory() {
        if (inventory == null) init();

        return inventory;
    }

    private void init() {
        inventory = Bukkit.createInventory(this, 54, "§c§l" + begin.format(DateTimeFormatter.ofPattern("yyyy年M月")) + "§6§l签到情况");

        fill(1, end.getDayOfMonth(), Panel.UNKNOWN);

        if (!singleton) {
            button();
        }

        LocalSign local = L2Pool.local(p);

        LocalDate l = local.getLatest().toLocalDateTime().toLocalDate();

        fill(l, LocalDate.now());

        int lasted = local.getLasted();
        fill(lasted > 1 ? l.minusDays(lasted).plusDays(1) : l, l, Panel.YES);

        fill(L2Pool.missing(p, -1).iterator());
    }

    private void fill(LocalDate latest, LocalDate now) {
        if (latest.isEqual(now)) {
            return;
        }

        if (latest.plusDays(1).isBefore(now)) {
            fill(latest.plusDays(1), now.minusDays(1), Panel.NOT);
        }

        fill(now, now, Panel.NOT_YET);
    }

    private void button() {
        if (month <= Main.getViewMaxMonth()) {
            ItemStack item = new ItemStack(Material.PAPER);
            Button.PREV.button(this, item);
            inventory.setItem(45, item);
        }

        if (month >= 1) {
            ItemStack item = new ItemStack(Material.PAPER);
            Button.NEXT.button(this, item);
            inventory.setItem(53, item);
        }

        ItemStack item = new ItemStack(Material.BEACON);
        Button.MIDDLE.button(this, item);
        inventory.setItem(49, item);
    }

    private void fill(Iterator<SignMissing> all) {
        if (!all.hasNext()) {
            return;
        }

        val i = all.next();
        val point = i.getMissingTime().toLocalDateTime().toLocalDate();
        LocalDate l = point.plusDays(i.getMissing()).minusDays(1);

        fill(point, l, Panel.NOT);
        fill(point.minusDays(i.getLasted()), point.minusDays(1), Panel.YES);

        fill(all);
    }

    private void fill(LocalDate i, LocalDate l, Panel panel) {
        if (i.isAfter(end) || l.isBefore(begin)) {
            return;
        }

        fill(i.isAfter(begin) ? i.getDayOfMonth() : 1, l.isBefore(end) ? l.getDayOfMonth() : end.getDayOfMonth(), panel);
    }

    private void fill(int i, int l, Panel panel) {
        fill(i++, panel);
        if (i <= l) fill(i, l, panel);
    }

    private void fill(int day, Panel panel) {
        val item = new ItemStack(Material.STAINED_GLASS_PANE, day--, (short) panel.date, (byte) panel.date);
        panel.metadata(item);

        inventory.setItem(day, item);
    }

    public static void click(ViewHandler view, int slot) {
        val button = Button.BY_SLOT.get(slot);
        if (!nil(button)) {
            button.action(view);
        }
    }

    enum Button {

        PREV(45) {
            void action(ViewHandler view) {
                Main main = Main.getPlugin();
                main.runAsync(() -> {
                    Inventory inventory = new ViewHandler(view.p, view.month < 1 ? 1 : view.month + 1, false).getInventory();
                    main.run(() -> view.p.openInventory(inventory));
                });
            }

            void button(ViewHandler view, ItemStack item) {
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName("查看上个月");
                item.setItemMeta(meta);
            }
        },

        MIDDLE(49) {
            void action(ViewHandler view) {
                LocalDate latest = L2Pool.local(view.p).getLatest().toLocalDateTime().toLocalDate();
                if (latest.isBefore(LocalDate.now())) {
                    Main main = Main.getPlugin();
                    main.getExecutor().sign(view.p);
                    main.run(() -> view.p.closeInventory());// Avoid bug next tick close gui
                }
            }

            void button(ViewHandler view, ItemStack item) {
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.GOLD + "每日签到");
                meta.setLore(Holder.getInfo(L2Pool.local(view.p)));
                item.setItemMeta(meta);
            }
        },

        NEXT(53) {
            void action(ViewHandler view) {
                Main main = Main.getPlugin();
                main.runAsync(() -> {
                    Inventory inventory = new ViewHandler(view.p, view.month - 1, false).getInventory();
                    main.run(() -> view.p.openInventory(inventory));
                });
            }

            void button(ViewHandler view, ItemStack item) {
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName("查看下个月");
                item.setItemMeta(meta);
            }
        };

        static final Map<Integer, Button> BY_SLOT = new HashMap<>();

        static {
            for (Button click : values()) {
                BY_SLOT.put(click.slot, click);
            }
        }

        private final int slot;

        Button(int slot) {
            this.slot = slot;
        }

        void action(ViewHandler view) {
            throw new UnsupportedOperationException("action");
        }

        void button(ViewHandler view, ItemStack item) {
            throw new UnsupportedOperationException("button");
        }
    }

    enum Panel {

        NOT_YET(2) {
            void metadata(ItemStack item) {
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.LIGHT_PURPLE + "今日未签到");
                item.setItemMeta(meta);
            }
        },

        YES(5) {
            void metadata(ItemStack item) {
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.GREEN + "已签到");
            }
        },

        NOT(14) {
            void metadata(ItemStack item) {
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.RED + "未签到");
                item.setItemMeta(meta);
            }
        },

        UNKNOWN(15) {
            void metadata(ItemStack item) {
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.GRAY + "无数据");
                item.setItemMeta(meta);
            }
        };

        private final int date;

        Panel(int date) {
            this.date = date;
        }

        void metadata(ItemStack item) {
            throw new AbstractMethodError();
        }
    }

}
