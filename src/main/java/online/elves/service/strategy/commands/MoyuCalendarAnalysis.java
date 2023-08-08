package online.elves.service.strategy.commands;

import lombok.extern.slf4j.Slf4j;
import online.elves.service.strategy.CommandAnalysis;
import online.elves.third.apis.Calendar_JH;
import online.elves.third.apis.MoYuCalendar;
import online.elves.third.apis.Vocation;
import online.elves.third.fish.Fish;
import online.elves.utils.DateUtil;
import online.elves.utils.RedisUtil;
import online.elves.utils.SortUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 摸鱼日历.
 */
@Slf4j
@Component
public class MoyuCalendarAnalysis extends CommandAnalysis {
    /**
     * 关键字
     */
    private static final List<String> keys = Arrays.asList("摸鱼历", "摸鱼", "日历", "鱼历", "报时");

    @Override
    public boolean check(String commonKey) {
        return keys.contains(commonKey);
    }

    @Override
    public void process(String commandKey, String commandDesc, String userName) {
        if (commandKey.equals("报时")) {
            String s = RedisUtil.get("FISH:VOCATION:WORD");
            if (StringUtils.isBlank(s)) {
                StringBuilder my = new StringBuilder(Vocation.getWord(Vocation.get())).append("\n\n");
                // 今年的节日排序
                LinkedHashMap<String, Integer> sorted = SortUtil.sortMapWithValue(Calendar_JH.getYear());
                // 遍历
                for (Map.Entry<String, Integer> entry : sorted.entrySet()) {
                    my.append("距离 **").append(entry.getKey()).append("** 还有 **").append(entry.getValue()).append("** 天").append(" \n\n");
                }
                s = my.toString();
                LocalDateTime now = LocalDateTime.now();
                // 到明天还有多少秒
                RedisUtil.set("FISH:VOCATION:WORD", s, DateUtil.getInterval(now, now.toLocalDate().plusDays(1).atStartOfDay(), ChronoUnit.SECONDS));
            }
            Fish.sendMsg(s);
        } else {
            Fish.sendMsg(MoYuCalendar.getMyCalJH());
        }
    }
}
