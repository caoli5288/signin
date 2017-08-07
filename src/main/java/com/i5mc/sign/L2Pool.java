package com.i5mc.sign;

import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.i5mc.sign.$.nil;

/**
 * Created on 17-5-25.
 */
public enum L2Pool {

    INSTANCE;

    private final Map<UUID, LocalSign> pool = new ConcurrentHashMap<>();

    public LocalSign get(Player p) {
        return pool.get(p.getUniqueId());
    }

    public void quit(Player p) {
        pool.remove(p.getUniqueId());
    }

    public LocalSign fetch(Player p) {
        return pool.computeIfAbsent(p.getUniqueId(), i -> {
                    LocalSign sign = Main.getPlugin()
                            .getDatabase()
                            .find(LocalSign.class, p.getUniqueId());
                    if (nil(sign)) {
                        sign = Main.getPlugin().getDatabase().createEntityBean(LocalSign.class);
                        sign.setId(i);
                    } else {
                        int l = sign.getLatest().toLocalDateTime().toLocalDate().compareTo(LocalDate.now());
                        if (Math.abs(l) > 1) {
                            sign.setLasted(0);
                        }
                    }
                    return sign;
                }
        );
    }

}
