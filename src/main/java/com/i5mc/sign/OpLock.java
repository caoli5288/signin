package com.i5mc.sign;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public enum OpLock {

    INSTANCE;

    private final Cache<UUID, Object> locked = CacheBuilder.newBuilder()
            .expireAfterWrite(15, TimeUnit.SECONDS)
            .build();

    public static boolean lock(UUID id) {
        if (INSTANCE.locked.asMap().containsKey(id)) {
            return false;
        }
        INSTANCE.locked.put(id, "");
        return true;
    }

    public static void unlock(UUID id) {
        INSTANCE.locked.invalidate(id);
    }

}
