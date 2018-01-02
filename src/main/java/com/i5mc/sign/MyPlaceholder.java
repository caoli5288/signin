package com.i5mc.sign;

import com.i5mc.sign.entity.SignLogging;
import com.i5mc.sign.entity.SignMissing;
import lombok.val;
import me.clip.placeholderapi.external.EZPlaceholderHook;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

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

    private enum Lab {

        TODAY((p, input) -> {
            val i = L2Pool.local(p);
            return i.getLatest().toLocalDateTime().toLocalDate().isEqual(LocalDate.now()) ? "true" : "false";
        }),

        IF((p, itr) -> {
            LocalDate day = LocalDate.parse(itr.next());
            SignLogging logging = L2Pool.pull(p.getUniqueId() + ":day:" + day, () -> {
                List<SignLogging> list = Main.getPlugin().getDatabase().find(SignLogging.class)
                        .where("player = ? and date_signed = ?")
                        .setParameter(1, p.getUniqueId())
                        .setParameter(2, String.valueOf(day))
                        .setMaxRows(1)
                        .findList();
                return list.isEmpty() ? null : list.iterator().next();
            });
            return logging == null ? "false" : "true";
        }),

        MISSING((p, input) -> {
            List<SignMissing> list = L2Pool.missing(p, input.hasNext() ? Integer.parseInt(input.next()) : -1);
            return "" + list;
        }),

        TOTAL((p, input) -> {
            val i = L2Pool.local(p);
            return "" + i.getDayTotal();
        });

        private final IReq req;

        Lab(IReq req) {
            this.req = req;
        }
    }

    @Override
    public String onPlaceholderRequest(Player p, String request) {
        val input = Arrays.asList(request.split("_")).iterator();
        if (input.hasNext()) {
            val label = input.next().toUpperCase();
            try {
                return Lab.valueOf(label).req.request(p, input);
            } catch (Exception ign) {
//                ;
            }
        }
        return "null";
    }

}
