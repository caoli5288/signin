package com.i5mc.sign;

import com.i5mc.sign.entity.LocalSign;
import com.i5mc.sign.entity.SignLogging;
import com.i5mc.sign.entity.SignMissing;
import lombok.val;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.i5mc.sign.$.nil;
import static org.bukkit.Material.AIR;

/**
 * Created on 16-8-11.
 */
public class Executor implements CommandExecutor, Listener {

    private final Map<UUID, Holder> map = new HashMap<>();// Always in main thread
    private final Set<UUID> locked = new HashSet<>();
    private final Main main;

    Executor(Main main) {
        this.main = main;
    }

    public Holder holder(Player p) {
        return map.get(p.getUniqueId());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command i, String j, String[] input) {
        if (!(sender instanceof Player)) return false;
        if (input.length == 0) return process((Player) sender);
        if (input[0].equals("sign")) {
            val p = ((Player) sender);
            sign(p);
            return true;
        }
        return false;
    }

    public void sign(Player p) {
        if (!OpLock.lock(p.getUniqueId())) {
            Main.getMessenger().send(p, "wait", "&6请稍后再试");
            return;
        }

        val hold = map.get(p.getUniqueId());
        if (nil(hold)) {
            main.runAsync(() -> {
                val local = L2Pool.local(p);
                val n = Holder.of(main, local);
                n.update();
                main.run(() -> {
                    map.put(p.getUniqueId(), n);
                    sign(p, n);
                });
            });
        } else {
            sign(p, hold);
        }
    }

    private boolean process(Player p) {
        if (map.containsKey(p.getUniqueId())) {
            p.openInventory(map.get(p.getUniqueId()).getInventory());
        } else if (locked.add(p.getUniqueId())) {
            val local = L2Pool.pick(p);
            if (local == null) {
                main.runAsync(() -> {// IO blocking
                    main.run(() -> process(p, L2Pool.local(p)));
                });
            } else {
                main.run(() -> process(p, local));
            }
        }
        return true;
    }

    private void process(Player p, LocalSign sign) {
        Holder holder = Holder.of(main, sign);
        holder.update();
        map.put(p.getUniqueId(), holder);
        locked.remove(p.getUniqueId());
        p.openInventory(holder.getInventory());
    }

    @EventHandler
    public void handle(PlayerQuitEvent event) {
        L2Pool.quit(event.getPlayer());
        Holder remove = map.remove(event.getPlayer().getUniqueId());
        if (!nil(remove)) {
            remove.close();
        }
    }

    @EventHandler
    public void handle(InventoryClickEvent event) {
        InventoryHolder h = event.getClickedInventory().getHolder();
        if (h instanceof Holder) {
            event.setCancelled(true);
            ItemStack click = event.getCurrentItem();
            if (click == null || click.getType() == AIR) {
                return;
            }
            sign((Player) event.getWhoClicked(), (Holder) h);
        } else if (h instanceof ViewHandler) {
            event.setCancelled(true);
            ItemStack click = event.getCurrentItem();
            if (click == null || click.getType() == AIR) {
                return;
            }

            ViewHandler.click(((ViewHandler) h), event.getRawSlot());
        }
    }

    private void sign(Player p, Holder holder) {
        if (!holder.signed) {
            holder.signed = true;// safety
            main.runAsync(() -> {
                val local = holder.sign;
                val daily = ExtGiftMgr.getDaily();

                if (local.getMissing() >= 1) {
                    SignMissing missing = main.db.bean(SignMissing.class);
                    missing.setPlayer(p.getUniqueId());
                    missing.setName(p.getName());
                    missing.setLasted(local.getMissing());
                    missing.setMissing(Math.toIntExact(ChronoUnit.DAYS.between(local.getLatest().toLocalDateTime().toLocalDate(), LocalDate.now()) - 1));
                    missing.setMissingTime(Timestamp.valueOf(local.getLatest().toLocalDateTime().plusDays(1)));
                    main.db.save(missing);
                    List<SignMissing> all = L2Pool.pull(p.getUniqueId() + ":missing");
                    if (!nil(all)) all.add(missing);
                    L2Pool.remove(p.getUniqueId() + ":missing:");
                }

                local.setDayTotal(1 + local.getDayTotal());

                if (local.getLasted() < 1) {
                    local.setLasted(1);
                } else {
                    local.setLasted(1 + local.getLasted());
                }

                local.setLatest(Timestamp.from(Instant.now()));

                val srv = p.getServer();
                val con = srv.getConsoleSender();
                for (String l : daily.getCommand()) {
                    if (!l.isEmpty()) {
                        main.run(() -> srv.dispatchCommand(con, PlaceholderAPI.setPlaceholders(p, l.replace("%player%", p.getName()))));
                    }
                }
                p.sendMessage(Main.getMessenger().find("receive", "§b梦世界 §l>> §a您领取了签到奖励§e ") + daily.getDisplay());

                val gift = ExtGiftMgr.getLasted(local.getLasted());
                if (!nil(gift)) {
                    List<String> list = gift.getCommand();
                    for (String l : list) {
                        if (!l.isEmpty()) {
                            main.run(() -> srv.dispatchCommand(con, PlaceholderAPI.setPlaceholders(p, l.replace("%player%", p.getName()))));
                        }
                    }
                    p.sendMessage(Main.getMessenger().find("receive_extra", "§b梦世界 §l>> §a您领取了额外奖励§e ") + gift.getDisplay());
                }

                main.persist(local);

                SignLogging logging = main.db.bean(SignLogging.class);
                logging.setPlayer(p.getUniqueId());
                logging.setName(p.getName());
                logging.setDateSigned(Timestamp.from(Instant.now()));

                main.db.save(logging);

                L2Pool.put(p.getUniqueId() + ":day:" + LocalDate.now(), logging);

                main.run(() -> {
                    holder.update();
                    val view = p.getOpenInventory();
                    if (!nil(view)) p.closeInventory();
                });
            });
        }
        OpLock.unlock(p.getUniqueId());
    }

}
