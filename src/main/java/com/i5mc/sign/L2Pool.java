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
        return (LocalSign) INSTANCE.pool.asMap().get(p.getUniqueId() + ":local");
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

    public static <T> T pull(String key) {
        return (T) INSTANCE.pool.asMap().get(key);
    }

    public static List<SignMissing> missing(Player p, int limit) {
        if (limit >= 1) {
            LocalDate day = LocalDate.now().minusDays(limit);
            return L2Pool.pull(p.getUniqueId() + ":missing:" + day, () -> Main.getPlugin().db.find(SignMissing.class)
                    .where("player = ? and missing_time > ?")
                    .setParameter(1, p.getUniqueId())
                    .setParameter(2, Timestamp.valueOf(day.atStartOfDay()))
                    .orderBy("missing_time desc")
                    .findList());
        } else {
            return L2Pool.pull(p.getUniqueId() + ":missing", () -> Main.getPlugin().db.find(SignMissing.class)
                    .where("player = ?")
                    .setParameter(1, p.getUniqueId())
                    .orderBy("missing_time desc")
                    .findList());
        }
    }

    public static void missing(Player p, int limit, List<SignMissing> all) {
        if (limit >= 1) {
            put(p.getUniqueId() + ":missing:" + LocalDate.now().minusDays(limit), all);
        } else {
            put(p.getUniqueId() + ":missing", all);
        }
    }

    public static void quit(Player p) {
        remove(Pattern.compile(p.getUniqueId() + ":(.+)"));
    }

    @SneakyThrows
    public static LocalSign local(Player p) {
        return (LocalSign) INSTANCE.pool.get(p.getUniqueId() + ":local", () -> {
            LocalSign local = Main.getPlugin()
                    .db
                    .find(LocalSign.class, p.getUniqueId());
            if (nil(local)) {
                local = Main.getPlugin().db.bean(LocalSign.class);
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
