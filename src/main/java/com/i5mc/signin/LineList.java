package com.i5mc.signin;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 16-8-11.
 */
public class LineList {

    public static <E> List<E> of(E... j) {
        ArrayList<E> out = new ArrayList<>(j.length);
        for (E i : j) {
            out.add(i);
        }
        return out;
    }

}
