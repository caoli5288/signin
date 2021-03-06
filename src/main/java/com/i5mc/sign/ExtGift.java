package com.i5mc.sign;

import lombok.Data;
import lombok.val;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.bukkit.util.NumberConversions.toInt;

/**
 * Created by on 2017/8/7.
 */
@Data
public class ExtGift {

    private int day;
    private String display;
    private List<String> command;

    ExtGift(ConfigurationSection mapping) {
        day = mapping.getInt("day");
        display = mapping.getString("display");
        val l = mapping.get("command");
        if (l instanceof List) {
            command = (List<String>) l;
        } else {
            command = Arrays.asList(String.valueOf(l).split("\n"));
        }
    }

    ExtGift(Map<?, ?> mapping) {
        day = toInt(mapping.get("day"));
        display = mapping.get("display").toString();
        val l = mapping.get("command");
        if (l instanceof List) {
            command = (List<String>) l;
        } else {
            command = Arrays.asList(String.valueOf(l).split("\n"));
        }
    }
}
