package com.i5mc.signin;

import com.i5mc.signin.entity.SignIn;
import com.mengcraft.account.Account;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created on 17-5-25.
 */
public enum L2Pool {

    INSTANCE;

    private final Map<UUID, SignIn> pool;

    L2Pool() {
        pool = new ConcurrentHashMap<>();
    }

    public SignIn get(Player p) {
        return pool.get(p.getUniqueId());
    }

    public SignIn fetch(Player p) {
        return pool.computeIfAbsent(p.getUniqueId(), i -> Main.getPlugin()
                .getDatabase()
                .find(SignIn.class, Account.INSTANCE.getMemberKey(p))
        );
    }

}
