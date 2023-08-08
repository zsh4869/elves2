package online.elves.third.apis;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import online.elves.config.Const;
import online.elves.third.apis.hotnews.TopurlNews;
import online.elves.third.apis.juhe.TopNews;
import online.elves.utils.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * 热点新闻.
 */
@Slf4j
@Component
public class HotNews {

    /**
     * 缓存更新时间  宵禁结束
     */
    private static LocalTime start = LocalTime.of(8, 0, 0);

    /**
     * 聚合 获取热点新闻
     */
    public static List<TopNews.TopNew> getTopNews() {
        // 参数对象
        Map<String, Object> params = Maps.newConcurrentMap();
        // api key
        params.put("key", RedisUtil.get(Const.JU_HE_API + ":TOP:NEWS"));
        /*
        支持的类型
        top(推荐,默认)
        guonei(国内)
        guoji(国际)
        yule(娱乐)
        tiyu(体育)
        junshi(军事)
        keji(科技)
        caijing(财经)
        youxi(游戏)
        qiche(汽车)
        jiankang(健康)
         */
        params.put("type", "top");
        params.put("page", 1);
        params.put("page_size", 10);
        // 是否只返回有内容详情的新闻, 1:是, 默认0
        params.put("is_filter", 0);
        // 获取当年假日列表
        String uri = "http://v.juhe.cn/toutiao/index";
        // 获取返回结果
        String result = HttpUtil.get(uri, params);
        if (StringUtils.isBlank(result)) {
            log.warn("(╥╯^╰╥)服务器开小差了, 要不你再试一下?");
            return Lists.newArrayList();
        }
        // 假日列表
        try {
            return JSON.parseObject(result, TopNews.class).getResult().getData();
        } catch (Exception e) {
            log.warn("HotNews jh getTopNews 反序列化异常 => {}", result);
            return Lists.newArrayList();
        }
    }

    /**
     * 获取今日新闻
     *
     * @return
     */
    public static TopurlNews getTopurlNews() {
        // 缓存key
        String key = "hot:news:topurl";
        // 获取缓存
        String n = RedisUtil.get(key);
        if (StringUtils.isBlank(n)) {
            // 获取新闻对象
            String get = HttpUtil.get("https://news.topurl.cn/api");
            // 判断是否为空
            if (StringUtils.isNotBlank(get)) {
                // 反序列化
                TopurlNews topurlNews;
                try {
                    topurlNews = JSON.parseObject(get, TopurlNews.class);
                } catch (Exception e) {
                    log.info("hot news topurl 反序列化异常 => {}", get);
                    // 直接返回异常
                    return null;
                }
                // 请求失败
                if (topurlNews.getCode() != 200) {
                    log.info("hot news topurl 请求失败 => {}", get);
                    return null;
                }
                // 没啥问题的话 存下缓存 宵禁结束缓存过期
                LocalDateTime time = LocalDateTime.of(LocalDate.now().plusDays(1), start);
                // 设置缓存
                RedisUtil.set(key, get, Long.valueOf(Duration.between(LocalDateTime.now(), time).getSeconds()).intValue());
                return topurlNews;
            } else {
                log.info("hot news topurl interface has no msg");
            }
            return null;
        } else {
            // 存在的话, 肯定不会反序列化问题了
            return JSON.parseObject(n, TopurlNews.class);
        }
    }

}
