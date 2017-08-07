package com.i5mc.sign;

import com.google.common.collect.ImmutableMap;
import lombok.val;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;

/**
 * Created by on 2017/8/7.
 */
public enum LocalMgr {

    INSTANCE;

    private Map<Integer, ExtGift> mapping;
    private ExtGift daily;

    public static void init(FileConfiguration i) {
        INSTANCE.daily = new ExtGift(i.getConfigurationSection("daily"));
        ImmutableMap.Builder<Integer, ExtGift> b = ImmutableMap.builder();
        for (val l : i.getMapList("last")) {
            val gift = new ExtGift(l);
            b.put(gift.getDay(), gift);
        }
        INSTANCE.mapping = b.build();
    }

    public static ExtGift getDaily() {
        return INSTANCE.daily;
    }

    public static ExtGift getLast(int day) {
        return INSTANCE.mapping.get(day);
    }
}
