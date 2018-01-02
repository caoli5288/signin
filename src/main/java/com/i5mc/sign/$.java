package com.i5mc.sign;

import com.i5mc.sign.entity.SignMissing;

/**
 * Created on 17-6-1.
 */
public class $ {

    public static boolean nil(Object any) {
        return any == null;
    }

    public static String info(SignMissing missing) {
        return missing.getMissingTime().toLocalDateTime().toLocalTime() + "(" + missing.getMissing() + ")";
    }
}
