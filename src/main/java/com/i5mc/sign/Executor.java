package com.i5mc.sign;

import lombok.val;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Timestamp;
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

    private final PlayerPointsAPI point = JavaPlugin.getPlugin(PlayerPoints.class).getAPI();
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
                    val in = L2Pool.INSTANCE.fetch(p);
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
            val sign = L2Pool.INSTANCE.get(p);
            if (sign == null) {
                main.execute(() -> {// IO blocking
                    main.run(() -> process(p, L2Pool.INSTANCE.fetch(p)));
                });
            } else {
                main.run(() -> process(p, sign));
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
        L2Pool.INSTANCE.quit(event.getPlayer());
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
                val sign = holder.sign;
                int daily = LocalMgr.getDaily();
                point.give(p.getUniqueId(), daily);
                p.sendMessage("§b梦世界 §l>> §a您领取了签到奖励§e " + daily + " §a点券");
                sign.setDayTotal(1 + sign.getDayTotal());
                sign.setLasted(1 + sign.getLasted());
                sign.setLatest(new Timestamp(System.currentTimeMillis()));

                val gift = LocalMgr.getLast(sign.getLasted());
                if (!nil(gift)) {
                    val srv = p.getServer();
                    List<String> list = gift.getCommand();
                    val con = srv.getConsoleSender();
                    for (String l : list) {
                        srv.dispatchCommand(con, l.replace("%player%", p.getName()));
                    }
                    p.sendMessage("§b梦世界 §l>> §a您领取了额外奖励§e " + gift.getDisplay());
                }

                main.getDatabase().save(sign);
                main.run(() -> {
                    holder.update();
                    val view = p.getOpenInventory();
                    if (!nil(view)) p.closeInventory();
                });
            });
        }
    }

}
