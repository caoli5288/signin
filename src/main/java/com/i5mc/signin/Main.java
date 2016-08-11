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
