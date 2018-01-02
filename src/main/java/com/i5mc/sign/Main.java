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

import java.sql.Timestamp;
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

        Executor executor = new Executor(this);

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

            List<SignMissing> list = L2Pool.missing(p, itr.hasNext() ? Integer.parseInt(itr.next()) : -1);
            Preconditions.checkState(!list.isEmpty(), "没有断签记录");

            //
            List<SignMissing> removal = new ArrayList<>();
            LinkedList<SignMissing> all = new LinkedList<>(list);
            //

            fix(p, day, all, removal);

            execute(() -> getDatabase().save(all));

            if (removal.isEmpty()) {
                return;
            }

            val local = L2Pool.local(p);
            removal.forEach(missing -> local.setLasted(local.getLasted() + missing.getLasted()));

            execute(() -> getDatabase().save(local));
            execute(() -> getDatabase().delete(removal));
        }
    }

    private void fix(Player p, int day, LinkedList<SignMissing> all, List<SignMissing> removal) {
        if (day < 1 || all.isEmpty()) return;
        val missing = all.element();

        val logging = new SignLogging();
        logging.setPlayer(p.getUniqueId());
        logging.setName(p.getName());
        logging.setDateSigned(missing.getMissingTime());

        execute(() -> getDatabase().insert(logging));

        missing.setMissing(missing.getMissing() - 1);
        missing.setMissingTime(Timestamp.valueOf(missing.getMissingTime().toLocalDateTime().plusDays(1)));

        if (missing.getMissing() < 1) {
            removal.add(all.remove());
        }

        fix(p, day - 1, all, removal);
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
