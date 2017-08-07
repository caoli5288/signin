package com.i5mc.sign;

import lombok.val;
import me.clip.placeholderapi.external.EZPlaceholderHook;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Iterator;

import static com.i5mc.sign.$.nil;

/**
 * Created on 17-5-25.
 */
public class MyPlaceholder extends EZPlaceholderHook {

    public MyPlaceholder(Plugin plugin) {
        super(plugin, "signin");
    }

    private interface IReq {

        String request(Player p, Iterator<String> input);
    }

    private static String today(Player p, Iterator<String> input) {
        val i = L2Pool.INSTANCE.get(p);
        if (nil(i)) {
            Main.getPlugin().execute(() -> L2Pool.INSTANCE.fetch(p));
            return "";
        }
        return i.getLatest().toLocalDateTime().toLocalDate().isEqual(LocalDate.now()) ? "true" : "";
    }

    private enum Lab {

        TODAY(MyPlaceholder::today);

        private final IReq req;

        Lab(IReq req) {
            this.req = req;
        }
    }


    @Override
    public String onPlaceholderRequest(Player p, String request) {
        val input = Arrays.asList(request.split("[-_]")).iterator();
        if (input.hasNext()) {
            val label = input.next().toUpperCase();
            try {
                return Lab.valueOf(label).req.request(p, input);
            } catch (IllegalArgumentException e) {
//                ;
            }
        }
        return null;
    }

}
