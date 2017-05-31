package com.i5mc.signin;

import lombok.val;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Created on 17-6-1.
 */
public class $ {

    public static boolean today(int unix) {
        val time = Instant.ofEpochSecond(unix).atZone(ZoneId.systemDefault()).toLocalDate();
        return time.isEqual(LocalDate.now());
    }

    public static boolean nil(Object any) {
        return any == null;
    }
}
