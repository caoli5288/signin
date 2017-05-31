package com.i5mc.signin;

import com.i5mc.signin.entity.SignIn;
import com.mengcraft.simpleorm.DatabaseException;
import com.mengcraft.simpleorm.EbeanHandler;
import com.mengcraft.simpleorm.EbeanManager;
import lombok.val;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Created on 16-8-10.
 */
public class Main extends JavaPlugin {

    private static Main plugin;

    @Override
    public void onEnable() {
        plugin = this;
        EbeanHandler handler = EbeanManager.DEFAULT.getHandler(this);
        if (handler.isNotInitialized()) {
            handler.define(SignIn.class);
            try {
                handler.initialize();
            } catch (DatabaseException e) {
                throw new RuntimeException(e);
            }
        }
        handler.reflect();

        Executor executor = new Executor(this);

        getCommand("signin").setExecutor(executor);
        getServer().getPluginManager().registerEvents(executor, this);

        val hook = new MyPlaceholder(this);
        hook.hook();
    }

    public static Main getPlugin() {
        return plugin;
    }

    public int getLastedReward(int day) {
        if (day > 364) {
            return 50;
        }
        if (day > 179) {
            return 30;
        }
        if (day > 89) {
            return 20;
        }
        if (day > 34) {
            return 15;
        }
        if (day > 19) {
            return 10;
        }
        if (day > 14) {
            return 8;
        }
        if (day > 9) {
            return 5;
        }
        if (day > 6) {
            return 3;
        }
        if (day > 4) {
            return 2;
        }
        if (day > 2) {
            return 1;
        }
        return 0;
    }

    public void execute(Runnable r) {
        CompletableFuture.runAsync(r);
    }

    public void process(Runnable j) {
        getServer().getScheduler().runTask(this, j);
    }

    public static boolean eq(Object i, Object j) {
        return Objects.equals(i, j);
    }

}
