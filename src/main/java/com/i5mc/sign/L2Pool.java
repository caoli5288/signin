package com.i5mc.sign;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.i5mc.sign.entity.LocalSign;
import lombok.SneakyThrows;
import lombok.val;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.i5mc.sign.$.nil;

/**
 * Created on 17-5-25.
 */
public enum L2Pool {

    INSTANCE;

    private final Cache<String, Object> pool = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();

    private final Object invalid = new Object();

    public static LocalSign pick(Player p) {
        return (LocalSign) INSTANCE.pool.asMap().get(p.getName() + ":local");
    }

    public static void put(String key, Object any) {
        INSTANCE.pool.put(key, any);
    }

    @SneakyThrows
    public static <T> T pull(String key, Supplier<T> supplier) {
        val pull = INSTANCE.pool.get(key, () -> {
            T value = supplier.get();
            return value == null ? INSTANCE.invalid : value;
        });
        return pull == INSTANCE.invalid ? null : (T) pull;
    }

    public static void quit(Player p) {
        INSTANCE.pool.invalidate(p.getName() + ":local");
    }

    @SneakyThrows
    public static LocalSign local(Player p) {
        return (LocalSign) INSTANCE.pool.get(p.getName() + ":local", () -> {
            LocalSign local = Main.getPlugin()
                    .getDatabase()
                    .find(LocalSign.class, p.getUniqueId());
            if (nil(local)) {
                local = Main.getPlugin().getDatabase().createEntityBean(LocalSign.class);
                local.setId(p.getUniqueId());
                local.setName(p.getName());
            } else {
                if (nil(local.getName())) local.setName(p.getName());
                if (LocalDate.now().toEpochDay() - local.getLatest().toLocalDateTime().toLocalDate().toEpochDay() > 1) {
                    local.setMissing(local.getLasted());
                    local.setLasted(-1);
                }
            }
            return local;
        });
    }

}
