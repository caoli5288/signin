package com.i5mc.signin;

import com.i5mc.signin.entity.SignIn;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
            if ($.nil(hold)) {
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
            SignIn in = L2Pool.INSTANCE.get(p);
            if (in == null) {
                main.execute(() -> {// IO blocking
                    main.run(() -> process(p, L2Pool.INSTANCE.fetch(p)));
                });
            } else {
                main.run(() -> process(p, in));
            }
        }
        return true;
    }

    private void process(Player p, SignIn in) {
        Holder holder = Holder.of(main, in);
        holder.update();
        map.put(p.getUniqueId(), holder);
        locked.remove(p.getUniqueId());
        p.openInventory(holder.getInventory());
    }

    @EventHandler
    public void handle(PlayerQuitEvent event) {
        L2Pool.INSTANCE.quit(event.getPlayer());
        Holder remove = map.remove(event.getPlayer().getUniqueId());
        if (!$.nil(remove)) {
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
        if (holder.signed() && holder.reward()) {
            holder.signIn().setTimeApp(holder.signIn().getTime() + 1);
            main.execute(() -> {
                int reward = holder.signIn().getLastreward() + main.getLastedReward(holder.signIn().getLasted());
                point.give(p.getUniqueId(), reward);
                main.getDatabase().save(holder.signIn());
                main.run(() -> {
                    holder.update();
                    val view = p.getOpenInventory();
                    if (!$.nil(view)) p.closeInventory();
                    p.sendMessage("§b梦世界 §l>> §a您领取了签到奖励§e " + reward + " §a点券");
                });
            });
        }
    }

}
