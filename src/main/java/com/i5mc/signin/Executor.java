package com.i5mc.signin;

import com.i5mc.signin.entity.SignIn;
import com.mengcraft.account.Account;
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
    public boolean onCommand(CommandSender p, Command i, String j, String[] k) {
        return p instanceof Player && process((Player) p);
    }

    private boolean process(Player p) {
        if (map.containsKey(p.getUniqueId())) {
            p.openInventory(map.get(p.getUniqueId()).getInventory());
        } else if (locked.add(p.getUniqueId())) {
            main.execute(() -> {// IO blocking
                SignIn in = main.getDatabase().find(SignIn.class, Account.INSTANCE.getMemberKey(p));
                main.process(() -> {
                    process(p, in);
                });
            });
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
        Holder remove = map.remove(event.getPlayer().getUniqueId());
        if (remove != null) {
            remove.close();
        }
    }

    @EventHandler
    public void handle(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof Holder) {
            process((Player) event.getWhoClicked(), (Holder) event.getInventory().getHolder());
            event.setCancelled(true);
        }
    }

    private void process(Player p, Holder j) {
        if (j.signed() && j.reward()) {
            j.signIn().setTimeApp(j.signIn().getTime() + 1);
            main.execute(() -> {
                int reward = j.signIn().getLastreward() + main.getLastedReward(j.signIn().getLasted());
                point.give(p.getUniqueId(), reward);
                main.getDatabase().save(j.signIn());
                main.process(() -> {
                    j.update();
                    p.closeInventory();
                    p.sendMessage("§b梦世界 §l>> §a您领取了签到奖励§e " + reward + " §a点券");
                });
            });
        }
    }

}
