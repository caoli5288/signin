package com.i5mc.sign;

import com.i5mc.sign.entity.LocalSign;
import com.i5mc.sign.entity.SignLogging;
import com.i5mc.sign.entity.SignMissing;
import lombok.val;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

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

/**
 * Created on 16-8-11.
 */
public class Executor implements CommandExecutor, Listener {

    private final Map<UUID, Holder> map = new HashMap<>();// Always in main thread
    private final Set<UUID> locked = new HashSet<>();
    private final Main main;

    public Executor(Main main) {
        this.main = main;
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

    private void sign(Player p) {
        if (map.containsKey(p.getUniqueId())) {
            val hold = map.get(p.getUniqueId());
            if (nil(hold)) {
                main.execute(() -> {
                    val in = L2Pool.local(p);
                    main.run(() -> {
                        map.put(p.getUniqueId(), Holder.of(main, in));
                        sign(p);
                    });
                });
            } else {
                sign(p, hold);
            }
        }
    }

    private boolean process(Player p) {
        if (map.containsKey(p.getUniqueId())) {
            p.openInventory(map.get(p.getUniqueId()).getInventory());
        } else if (locked.add(p.getUniqueId())) {
            val local = L2Pool.pick(p);
            if (local == null) {
                main.execute(() -> {// IO blocking
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
        if (event.getInventory().getHolder() instanceof Holder) {
            sign((Player) event.getWhoClicked(), (Holder) event.getInventory().getHolder());
            event.setCancelled(true);
        }
    }

    private void sign(Player p, Holder holder) {
        if (!holder.signed) {
            holder.signed = true;// safety
            main.execute(() -> {
                val local = holder.sign;
                val daily = LocalMgr.getDaily();

                if (local.getMissing() >= 1) {
                    SignMissing missing = main.getDatabase().createEntityBean(SignMissing.class);
                    missing.setPlayer(p.getUniqueId());
                    missing.setName(p.getName());
                    missing.setLasted(local.getMissing());
                    missing.setMissing(Math.toIntExact(ChronoUnit.DAYS.between(local.getLatest().toLocalDateTime().toLocalDate(), LocalDate.now()) - 1));
                    missing.setMissingTime(Timestamp.valueOf(local.getLatest().toLocalDateTime().plusDays(1)));
                    main.getDatabase().save(missing);
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
                    srv.dispatchCommand(con, l.replace("%player%", p.getName()));
                }
                p.sendMessage("§b梦世界 §l>> §a您领取了签到奖励§e " + daily.getDisplay());

                val gift = LocalMgr.getLast(local.getLasted());
                if (!nil(gift)) {
                    List<String> list = gift.getCommand();
                    for (String l : list) {
                        srv.dispatchCommand(con, l.replace("%player%", p.getName()));
                    }
                    p.sendMessage("§b梦世界 §l>> §a您领取了额外奖励§e " + gift.getDisplay());
                }

                main.getDatabase().save(local);

                SignLogging logging = main.getDatabase().createEntityBean(SignLogging.class);
                logging.setPlayer(p.getUniqueId());
                logging.setName(p.getName());
                logging.setDateSigned(Timestamp.valueOf(LocalDate.now().atStartOfDay()));

                main.getDatabase().save(logging);

                L2Pool.put(p.getName() + ":day:" + LocalDate.now(), logging);

                main.run(() -> {
                    holder.update();
                    val view = p.getOpenInventory();
                    if (!nil(view)) p.closeInventory();
                });
            });
        }
    }

}
