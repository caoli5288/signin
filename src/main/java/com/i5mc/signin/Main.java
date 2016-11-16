package com.i5mc.signin;

import com.i5mc.signin.entity.SignIn;
import com.mengcraft.simpleorm.DatabaseException;
import com.mengcraft.simpleorm.EbeanHandler;
import com.mengcraft.simpleorm.EbeanManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Created on 16-8-10.
 */
public class Main extends JavaPlugin {

    @Override
    public void onEnable() {
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

    public void execute(Runnable j) {
        getServer().getScheduler().runTaskAsynchronously(this, j);
    }

    public void process(Runnable j) {
        getServer().getScheduler().runTask(this, j);
    }

    public static boolean eq(Object i, Object j) {
        return i == j || (i != null && i.equals(j));
    }
}
