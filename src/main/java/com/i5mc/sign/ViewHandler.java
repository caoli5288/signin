package com.i5mc.sign;

import com.i5mc.sign.entity.LocalSign;
import com.i5mc.sign.entity.SignMissing;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

    private Inventory inventory;

    public ViewHandler(Player p, int month) {
        this.p = p;
        this.month = month;
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

        LocalSign local = L2Pool.local(p);

        LocalDate l = local.getLatest().toLocalDateTime().toLocalDate();

        button();
        now(l, LocalDate.now());

        int lasted = local.getLasted();
        fill(lasted > 1 ? l.minusDays(lasted).plusDays(1) : l, l, Panel.YES);

        fill(L2Pool.missing(p, -1).iterator());
    }

    private void now(LocalDate l, LocalDate now) {
        if (l.isEqual(now)) {
            return;
        }

        fill(now.getDayOfMonth(), Panel.WAIT);

        if (l.plusDays(1).isBefore(now)) {
            fill(l.plusDays(1), now.minusDays(1), Panel.NO);
        }
    }

    private void button() {
        if (month <= Main.getViewMaxMonth()) {
            inventory.setItem(45, Button.LEFT.button(this));
        }

        inventory.setItem(49, Button.MIDDLE.button(this));

        if (month >= 1) {
            inventory.setItem(53, Button.RIGHT.button(this));
        }
    }

    private void fill(Iterator<SignMissing> all) {
        if (!all.hasNext()) {
            return;
        }

        val i = all.next();
        val point = i.getMissingTime().toLocalDateTime().toLocalDate();
        LocalDate l = point.plusDays(i.getMissing()).minusDays(1);

        fill(point, l, Panel.NO);
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

    private void fill(int slot, Panel panel) {
        val item = new ItemStack(160, slot, (short) panel.date, (byte) panel.date);
        val meta = item.getItemMeta();
        panel.metadata(meta);
        item.setItemMeta(meta);

        inventory.setItem(slot - 1, item);
    }

    public static void click(ViewHandler view, int slot) {
        val button = Button.BY_SLOT.get(slot);
        if (!nil(button)) {
            button.click(view);
        }
    }

    enum Button {

        LEFT(45) {
            void click(ViewHandler view) {
                Main main = Main.getPlugin();
                main.runAsync(() -> {
                    Inventory inventory = new ViewHandler(view.p, view.month < 1 ? 1 : view.month + 1).getInventory();
                    main.run(() -> view.p.openInventory(inventory));
                });
            }

            ItemStack button(ViewHandler view) {
                ItemStack item = new ItemStack(339);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName("查看上个月");
                item.setItemMeta(meta);
                return item;
            }
        },

        MIDDLE(49) {
            void click(ViewHandler view) {
                LocalDate latest = L2Pool.local(view.p).getLatest().toLocalDateTime().toLocalDate();
                if (latest.isBefore(LocalDate.now())) {
                    Main main = Main.getPlugin();
                    main.getExecutor().sign(view.p);
                    main.run(() -> view.p.closeInventory());
                }
            }

            ItemStack button(ViewHandler view) {
                ItemStack item = new ItemStack(138);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.GOLD + "每日签到");
                meta.setLore(Holder.getInfo(L2Pool.local(view.p)));
                item.setItemMeta(meta);
                return item;
            }
        },

        RIGHT(53) {
            void click(ViewHandler view) {
                Main main = Main.getPlugin();
                main.runAsync(() -> {
                    Inventory inventory = new ViewHandler(view.p, view.month - 1).getInventory();
                    main.run(() -> view.p.openInventory(inventory));
                });
            }

            ItemStack button(ViewHandler view) {
                ItemStack item = new ItemStack(339);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName("查看下个月");
                item.setItemMeta(meta);
                return item;
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

        void click(ViewHandler view) {
            throw new UnsupportedOperationException("click");
        }

        ItemStack button(ViewHandler view) {
            throw new UnsupportedOperationException("button");
        }
    }

    enum Panel {

        WAIT(2) {
            void metadata(ItemMeta meta) {
                meta.setDisplayName(ChatColor.LIGHT_PURPLE + "今日未签到");
            }
        },

        YES(5) {
            void metadata(ItemMeta meta) {
                meta.setDisplayName(ChatColor.GREEN + "已签到");
            }
        },

        NO(14) {
            void metadata(ItemMeta meta) {
                meta.setDisplayName(ChatColor.RED + "未签到");
            }
        },

        UNKNOWN(15) {
            void metadata(ItemMeta meta) {
                meta.setDisplayName(ChatColor.GRAY + "无数据");
            }
        };

        private final int date;

        Panel(int date) {
            this.date = date;
        }

        void metadata(ItemMeta meta) {
            throw new AbstractMethodError();
        }
    }

}
