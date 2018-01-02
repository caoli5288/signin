package com.i5mc.sign;

import com.google.common.base.Preconditions;
import com.i5mc.sign.entity.LocalSign;
import com.i5mc.sign.entity.SignLogging;
import com.i5mc.sign.entity.SignMissing;
import com.mengcraft.simpleorm.DatabaseException;
import com.mengcraft.simpleorm.EbeanHandler;
import com.mengcraft.simpleorm.EbeanManager;
import lombok.SneakyThrows;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.i5mc.sign.$.nil;

/**
 * Created on 16-8-10.
 */
public class Main extends JavaPlugin {

    private static Main plugin;
    private ExecutorService backend;
    private Executor executor;

    @SneakyThrows
    public void onEnable() {
        saveDefaultConfig();

        plugin = this;
        EbeanHandler db = EbeanManager.DEFAULT.getHandler(this);
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

        backend = Executors.newSingleThreadExecutor();

        LocalMgr.init(getConfig());

        executor = new Executor(this);

        PluginCommand command = getCommand("sign");
        command.setExecutor(executor);

        getServer().getPluginManager().registerEvents(executor, this);

        val hook = new MyPlaceholder(this);
        hook.hook();

        PluginHelper.addExecutor(this, "补签", "补签.admin", this::missing);
    }

    private void missing(CommandSender who, List<String> input) {
        if (input.isEmpty()) {
            who.sendMessage("/补签 <玩家> <天数> [范围]");
        } else {
            Iterator<String> itr = input.iterator();
            Player p = Bukkit.getPlayerExact(itr.next());
            Preconditions.checkNotNull(p);

            int day = Integer.parseInt(itr.next());

            int limit = itr.hasNext() ? Integer.parseInt(itr.next()) : -1;
            List<SignMissing> list = L2Pool.missing(p, limit);
            Preconditions.checkState(!list.isEmpty(), "没有断签记录");

            //
            List<SignMissing> removal = new ArrayList<>();
            LinkedList<SignMissing> all = new LinkedList<>(list);
            //

            val local = L2Pool.local(p);

            fix(p, local, day, all, removal);

            if (!all.isEmpty()) {
                execute(() -> getDatabase().save(all));
            }

            L2Pool.missing(p, limit, new ArrayList<>(all));

            if (!removal.isEmpty()) {
                removal.forEach(missing -> local.setLasted(local.getLasted() + missing.getLasted()));
                execute(() -> getDatabase().delete(removal));
            }

            Holder holder = executor.holder(p);
            holder.update();

            execute(() -> getDatabase().save(local));

            who.sendMessage("玩家 " + p.getName() + " 补签到完成");
        }
    }

    private void fix(Player p, LocalSign local, int day, LinkedList<SignMissing> all, List<SignMissing> removal) {
        if (day < 1 || all.isEmpty()) {
            return;
        }

        val logging = getDatabase().createEntityBean(SignLogging.class);
        val missing = all.element();

        logging.setPlayer(p.getUniqueId());
        logging.setName(p.getName());
        logging.setDateSigned(missing.getMissingTime());

        execute(() -> getDatabase().save(logging));

        missing.setMissing(missing.getMissing() - 1);

        local.setLasted(local.getLasted() + 1);
        local.setDayTotal(local.getDayTotal() + 1);

        if (missing.getMissing() < 1) {
            removal.add(all.remove());
        }

        fix(p, local, day - 1, all, removal);
    }

    @Override
    public void onDisable() {
        if (!nil(backend)) backend.shutdown();
    }

    public static Main getPlugin() {
        return plugin;
    }

    public void execute(Runnable r) {
        backend.execute(r);
    }

    public void run(Runnable j) {
        getServer().getScheduler().runTask(this, j);
    }

    public static void log(String message) {
        plugin.getLogger().info(message);
    }

}
