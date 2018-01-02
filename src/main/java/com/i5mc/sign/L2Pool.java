package com.i5mc.sign;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.i5mc.sign.entity.LocalSign;
import com.i5mc.sign.entity.SignMissing;
import lombok.SneakyThrows;
import lombok.val;
import org.bukkit.entity.Player;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Pattern;

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

    public static void remove(Pattern pattern) {
        INSTANCE.pool.asMap().keySet().removeIf(key -> pattern.matcher(key).matches());
    }

    @SneakyThrows
    public static <T> T pull(String key, Supplier<T> supplier) {
        val pull = INSTANCE.pool.get(key, () -> {
            T value = supplier.get();
            return value == null ? INSTANCE.invalid : value;
        });
        return pull == INSTANCE.invalid ? null : (T) pull;
    }

    public static List<SignMissing> missing(Player p, int limit) {
        if (limit >= 1) {
            LocalDate day = LocalDate.now().minusDays(limit);
            Timestamp l = Timestamp.valueOf(day.atStartOfDay());
            return L2Pool.pull(p.getName() + ":missing:" + day, () -> Main.getPlugin().getDatabase().find(SignMissing.class)
                    .where("player = ? and missing_time > ?")
                    .setParameter(1, p.getUniqueId())
                    .setParameter(2, l)
                    .orderBy("missing_time desc")
                    .findList());
        } else {
            return L2Pool.pull(p.getName() + ":missing", () -> Main.getPlugin().getDatabase().find(SignMissing.class)
                    .where("player = ?")
                    .setParameter(1, p.getUniqueId())
                    .orderBy("missing_time desc")
                    .findList());
        }
    }

    public static void quit(Player p) {
        remove(Pattern.compile(p.getName() + ":(.+)"));
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
