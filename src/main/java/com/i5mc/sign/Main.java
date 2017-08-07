package com.i5mc.sign;

import com.mengcraft.simpleorm.DatabaseException;
import com.mengcraft.simpleorm.EbeanHandler;
import com.mengcraft.simpleorm.EbeanManager;
import lombok.val;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * Created on 16-8-10.
 */
public class Main extends JavaPlugin {

    private static Main plugin;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        plugin = this;
        EbeanHandler handler = EbeanManager.DEFAULT.getHandler(this);
        if (handler.isNotInitialized()) {
            handler.define(LocalSign.class);
            try {
                handler.initialize();
            } catch (DatabaseException e) {
                throw new RuntimeException(e);
            }
        }
        handler.reflect();
        handler.install();

        LocalMgr.init(getConfig());

        Executor executor = new Executor(this);

        PluginCommand command = getCommand("sign");
        command.setExecutor(executor);
        command.setAliases(Arrays.asList("签到", "每日签到"));

        getServer().getPluginManager().registerEvents(executor, this);

        val hook = new MyPlaceholder(this);
        hook.hook();
    }

    public static Main getPlugin() {
        return plugin;
    }

    public void execute(Runnable r) {
        CompletableFuture.runAsync(r);
    }

    public void run(Runnable j) {
        getServer().getScheduler().runTask(this, j);
    }

    public static void log(String message) {
        plugin.getLogger().info(message);
    }

}
