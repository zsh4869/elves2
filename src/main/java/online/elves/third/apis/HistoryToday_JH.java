package online.elves.third.apis;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import online.elves.config.Const;
import online.elves.third.apis.juhe.Day;
import online.elves.third.apis.juhe.HistoryToday;
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
 * 聚合接口 历史上的今天
 */
@Slf4j
public class HistoryToday_JH {
    public static void main(String[] args) {
    }

    /**
     * 聚合接口 历史上的今天
     * https://www.juhe.cn/box/index/id/63
     *
     * @return
     */
    public static List<HistoryToday.History> getHistoryToday() {
        // 参数对象
        Map<String, Object> params = Maps.newConcurrentMap();
        // api key
        params.put("key", RedisUtil.get(Const.JU_HE_API + ":HISTORY:TODAY"));
        // 当年日期
        LocalDate now = LocalDate.now();
        // 年月日
        params.put("date", now.getMonth().getValue() + "/" + now.getDayOfMonth());
        // 获取当年假日列表
        String uri = "http://v.juhe.cn/todayOnhistory/queryEvent.php";
        // 获取返回结果
        String result = HttpUtil.get(uri, params);
        if (StringUtils.isBlank(result)) {
            log.warn("(╥╯^╰╥)服务器开小差了, 要不你再试一下?");
            return Lists.newArrayList();
        }
        // 假日列表
        try {
            return JSON.parseObject(result, HistoryToday.class).getResult();
        } catch (Exception e) {
            log.warn("HistoryToday_JH getHistoryToday 反序列化异常 => {}", result);
            return Lists.newArrayList();
        }
    }
}
