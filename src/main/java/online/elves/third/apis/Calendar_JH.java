package online.elves.third.apis;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import online.elves.config.Const;
import online.elves.third.apis.juhe.Day;
import online.elves.third.apis.juhe.HolidayData;
import online.elves.utils.DateUtil;
import online.elves.utils.RedisUtil;
import online.elves.utils.SortUtil;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 聚合接口日历
 */
@Slf4j
public class Calendar_JH {
    public static void main(String[] args) {
        // 排序后的对象
        LinkedHashMap<String, Integer> sorted = SortUtil.sortMapWithValue(getYear());
    }

    /**
     * 聚合接口的星座运势  今日
     * https://www.juhe.cn/docs/api/id/177
     *
     * @return
     */
    public static Day.Dt getDay() {
        // 参数对象
        Map<String, Object> params = Maps.newConcurrentMap();
        // api key
        params.put("key", RedisUtil.get(Const.JU_HE_API + ":CALENDAR"));
        // 当年日期
        LocalDate now = LocalDate.now();
        // 年月日
        params.put("date", now.getYear() + "-" + now.getMonth().getValue() + "-" + now.getDayOfMonth());
        // 获取当年假日列表
        String uri = "http://v.juhe.cn/calendar/day";
        // 获取返回结果
        String result = HttpUtil.get(uri, params);
        if (StringUtils.isBlank(result)) {
            log.warn("(╥╯^╰╥)服务器开小差了, 要不你再试一下?");
            return new Day.Dt();
        }
        // 假日列表
        try {
            return JSON.parseObject(result, Day.class).getResult().getData();
        } catch (Exception e) {
            log.warn("Calendar_JH getDay 反序列化异常 => {}", result);
            return new Day.Dt();
        }
    }

    /**
     * 聚合接口的星座运势  今日
     * https://www.juhe.cn/docs/api/id/177
     *
     * @return
     */
    public static Map<String, Integer> getYear() {
        // 结果
        Map<String, Integer> res = Maps.newHashMap();
        // 参数对象
        Map<String, Object> params = Maps.newConcurrentMap();
        // api key
        params.put("key", RedisUtil.get(Const.JU_HE_API + ":CALENDAR"));
        // 当年日期
        LocalDate now = LocalDate.now();
        // 明年1月1日
        LocalDate next11 = LocalDate.of(now.getYear() + 1, 1, 1);
        // 元旦天数
        res.put("元旦节", DateUtil.getInterval(now, next11, ChronoUnit.DAYS).intValue());
        params.put("year", now.getYear());
        // 获取当年假日列表
        String uri = "http://v.juhe.cn/calendar/year";
        // 获取返回结果
        String result = HttpUtil.get(uri, params);
        if (StringUtils.isBlank(result)) {
            log.warn("(╥╯^╰╥)服务器开小差了, 要不你再试一下?");
            return res;
        }
        // 假日列表
        try {
            HolidayData hd = JSON.parseObject(result, HolidayData.class);
            // 获取假日列表
            List<HolidayData.Holiday> holidayList = hd.getResult().getData().getHoliday_list();
            // 遍历列表
            for (HolidayData.Holiday hh : holidayList) {
                // 获取假日信息
                LocalDate date = DateUtil.parseLd(hh.getStartday());
                // 跳过已经过完的节日
                assert date != null;
                if (date.isBefore(now)) {
                    continue;
                }
                // 放入相差天数
                res.put(hh.getName(), DateUtil.getInterval(now, date, ChronoUnit.DAYS).intValue());
            }
        } catch (Exception e) {
            log.warn("Calendar_JH getYear 反序列化异常 => {}", result);
            return res;
        }
        return res;
    }
}
