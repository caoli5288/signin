package com.i5mc.sign;

import com.avaje.ebean.Update;
import com.i5mc.sign.entity.LocalSign;
import com.i5mc.sign.entity.SignLogging;
import com.i5mc.sign.entity.SignMissing;
import com.mengcraft.simpleorm.DatabaseException;
import com.mengcraft.simpleorm.EbeanHandler;
import com.mengcraft.simpleorm.EbeanManager;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import static com.i5mc.sign.$.nil;

/**
 * Created on 16-8-10.
 */
public class Main extends JavaPlugin {

    private static Main plugin;

    @Getter
    private Executor executor;

    @Getter
    private static int viewMaxMonth;
    private static int viewDirectMaxMonth;
    EbeanHandler db;

    @Getter
    private static Messenger messenger;

    @SneakyThrows
    public void onEnable() {
        saveDefaultConfig();

        plugin = this;
        db = EbeanManager.DEFAULT.getHandler(this);
        if (db.isNotInitialized()) {
            db.define(LocalSign.class);
            db.define(SignLogging.class);
            db.define(SignMissing.class);
            try {
                db.initialize();
            } catch (DatabaseException e) {
                throw new RuntimeException(e);
            }
        }
        db.reflect();
        db.install();

        try {
            db.getServer().createSqlUpdate("alter table `sign_logging` add index `idx_player` (`player`, `date_signed`);")
                    .execute();
        } catch (Exception ign) {
//
        }

        try {
            db.getServer().createSqlUpdate("alter table `sign_missing` add index `idx_player` (`player`, `missing_time`);")
                    .execute();
        } catch (Exception ign) {
//
        }

        ExtGiftMgr.init(getConfig());

        executor = new Executor(this);

        PluginCommand command = getCommand("sign");
        command.setExecutor(executor);

        getServer().getPluginManager().registerEvents(executor, this);

        val hook = new MyPlaceholder(this);
        hook.hook();

        viewMaxMonth = getConfig().getInt("view.max_month", 1);
        viewDirectMaxMonth = getConfig().getInt("view.direct_max_month", 12);

        messenger = new Messenger(this);

        PluginHelper.addExecutor(this, "补签", "补签.admin", this::fixing);
        PluginHelper.addExecutor(this, "签到详情", this::view);
    }

    private void view(CommandSender who, List<String> input) {
        val p = (Player) who;
        runAsync(() -> {// IO blocking while inventory build
            int mon = input.isEmpty() ? -1 : month(input.iterator().next());
            if (mon > viewDirectMaxMonth) {
                who.sendMessage(ChatColor.RED + "最多可查看最近" + viewDirectMaxMonth + "个月签到");
                return;
            }
            val l = ViewHandler.newInventory(p, mon, mon >= 1);
            run(() -> p.openInventory(l));
        });
    }

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("y-M");

    @SneakyThrows
    private int month(String label) {
        if (label.matches("\\d+")) {
            return Integer.parseInt(label);
        }

        Date l = dateFormat.parse(label);
        LocalDate i = l.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        return Math.toIntExact(ChronoUnit.MONTHS.between(i.withDayOfMonth(1), LocalDate.now()));
    }

    private void fixing(CommandSender who, List<String> input) {
        if (input.isEmpty()) {
            who.sendMessage("/补签 <玩家> <天> [范围]");
            return;
        }

        Iterator<String> itr = input.iterator();
        Player p = Bukkit.getPlayerExact(itr.next());
        if (p == null) {
            who.sendMessage("指定玩家不在线");
            return;
        }

        int day = Integer.parseInt(itr.next());
        int l = itr.hasNext() ? Integer.parseInt(itr.next()) : -1;

        List<SignMissing> list = L2Pool.missing(p, l);
        if (list.isEmpty()) {
            who.sendMessage("玩家无断签记录");
            return;
        }

        if (!OpLock.lock(p.getUniqueId())) {
            messenger.send(p, "wait", "&6请稍后再试");
            return;
        }

        //
        List<SignMissing> removal = new ArrayList<>();
        LinkedList<SignMissing> all = new LinkedList<>(list);
        //

        val local = L2Pool.local(p);

        fixing(p, local, day, all, removal);

        if (!all.isEmpty()) {
            runAsync(() -> db.save(all));
        }

        L2Pool.missing(p, l, new ArrayList<>(all));

        if (!removal.isEmpty()) {
            removal.forEach(missing -> local.setLasted(local.getLasted() + missing.getLasted()));
            runAsync(() -> db.delete(removal));
        }

        Holder holder = executor.holder(p);
        if (!nil(holder)) holder.update();

        runAsync(() -> db.save(local));

        OpLock.unlock(p.getUniqueId());

        who.sendMessage("玩家 " + p.getName() + " 补签到完成");
    }

    private void fixing(Player p, LocalSign local, int day, LinkedList<SignMissing> all, List<SignMissing> removal) {
        if (day < 1 || all.isEmpty()) {
            return;
        }

        val logging = db.bean(SignLogging.class);
        val missing = all.element();

        logging.setPlayer(p.getUniqueId());
        logging.setName(p.getName());

        missing.setMissing(missing.getMissing() - 1);
        if (missing.getMissing() < 1) {
            removal.add(all.remove());
            logging.setDateSigned(missing.getMissingTime());
        } else {
            logging.setDateSigned(Timestamp.valueOf(missing.getMissingTime().toLocalDateTime().plusDays(missing.getMissing())));
        }

        logging.setFixTime(Timestamp.from(Instant.now()));

        runAsync(() -> db.save(logging));

        local.setLasted(local.getLasted() + 1);
        local.setDayTotal(local.getDayTotal() + 1);

        fixing(p, local, day - 1, all, removal);
    }

    public void runAsync(Runnable r) {
        CompletableFuture.runAsync(r).exceptionally(this::log);
    }

    private Void log(Throwable thr) {
        getLogger().log(Level.SEVERE, "" + thr, thr);
        return null;
    }

    public void run(Runnable j) {
        getServer().getScheduler().runTask(this, j);
    }

    public void persist(LocalSign input) {
        Update<LocalSign> sql = db.getServer().createUpdate(LocalSign.class, "update local_sign set name = :name, day_total = :total, lasted = :lasted, latest = :latest where id = :id")
                .set("name", input.getName())
                .set("total", input.getDayTotal())
                .set("lasted", input.getLasted())
                .set("latest", input.getLatest())
                .set("id", input.getId());
        if (!(sql.execute() == 1)) {
            db.save(input);
        }
    }

    public static Main getPlugin() {
        return plugin;
    }

}
