package com.i5mc.sign;

import com.i5mc.sign.entity.LocalSign;
import com.i5mc.sign.entity.SignLogging;
import com.i5mc.sign.entity.SignMissing;
import com.mengcraft.simpleorm.DatabaseException;
import com.mengcraft.simpleorm.EbeanHandler;
import com.mengcraft.simpleorm.EbeanManager;
import lombok.SneakyThrows;
import lombok.val;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

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
