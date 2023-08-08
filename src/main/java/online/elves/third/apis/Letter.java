package online.elves.third.apis;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import online.elves.config.Const;
import online.elves.third.apis.juhe.Day;
import online.elves.utils.RedisUtil;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.util.Map;

/**
 * 获取指定接口的信息 文字 诗词等
 */
@Slf4j
public class Letter {

    /**
     * 获取每日一句
     */
    public static String getOneWord() {
        // 参数对象
        Map<String, Object> params = Maps.newConcurrentMap();
        params.put("app_id", RedisUtil.get(Const.MXN_API_KEY));
        params.put("app_secret", RedisUtil.get(Const.MXN_API_SECRET));
        // 获取随机一句话
        params.put("count", 1);
        // 笑话列表
        String uri = "https://www.mxnzp.com/api/daily_word/recommend";
        // 获取随机的笑话段子
        String result = HttpUtil.get(uri, params);
        if (StringUtils.isBlank(result)) {
            log.warn("(╥╯^╰╥)服务器开小差了, 要不你再试一下?");
            return "";
        }
        // 救命 这么多转化
        JSONObject onz = JSON.parseObject(result);
        if (onz.getInteger("code") != 1) {
            log.warn("刚跑神了, 要不你再问问我?");
            return "";
        }
        // 一句话
        return onz.getJSONArray("data").getJSONObject(0).getString("content");
    }

    /**
     * 获取心灵鸡汤 聚合接口
     * https://www.juhe.cn/docs/api/id/669
     */
    public static String getSoupJH() {
        // 参数对象
        Map<String, Object> params = Maps.newConcurrentMap();
        // api key
        params.put("key", RedisUtil.get(Const.JU_HE_API + ":SOUP"));
        // 获取当年假日列表
        String uri = "https://apis.juhe.cn/fapig/soup/query";
        // 获取返回结果
        String result = HttpUtil.get(uri, params);
        if (StringUtils.isBlank(result)) {
            log.warn("(╥╯^╰╥)服务器开小差了, 要不你再试一下?");
            return "芜湖...🐔被杀完了, 等我养养~";
        }
        // 假日列表
        try {
            return JSON.parseObject(result).getJSONObject("result").getString("text");
        } catch (Exception e) {
            log.warn("Letter getSoupJH 反序列化异常 => {}", result);
            return "芜湖...🐔跑了没法熬汤, 等我再抓一只~";
        }
    }
}
