package online.elves.apis;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import online.elves.apis.model.EResp;
import online.elves.config.Const;
import online.elves.service.FService;
import online.elves.utils.DateUtil;
import online.elves.utils.RedisUtil;
import online.elves.utils.SortUtil;
import online.elves.utils.StrUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/rank/")
public class RanksController {

    @Resource
    FService fService;

    @GetMapping("cr/msg")
    public EResp<List<JSONObject>> getCredit(@RequestParam("rangeRanks") int rangeRanks, @RequestParam("rangeDays") int rangeDays, @RequestParam("sign") String sign) {
        log.info("rank/cr/msg get ...{}...{}...{}", rangeRanks, rangeDays, sign);
        // 校验密码
        if (sign.equals("tmpSecret:TdsTE1KDRfGx")) {
            if (rangeRanks < 1 || rangeRanks > 100) {
                return EResp.createByErrorMessage("聚合排名不支持100名后的查询");
            }
            if (rangeDays < 0 || rangeDays > 90) {
                return EResp.createByErrorMessage("聚合排名不支持90天外的查询");
            }
            // 当前时间
            LocalDate now = LocalDate.now();
            // 得分集合
            Map<String, Integer> resScore = Maps.newHashMap();
            for (int i = 0; i < rangeDays + 1; i++) {
                resScore = buildRank(resScore, StrUtils.getKey(Const.RANKING_DAY_PREFIX, "20", DateUtil.format(DateUtil.ld2UDate(now.minusDays(i)), DateUtil.DAY_SIMPLE)));
            }
            // 都为空
            if (resScore.isEmpty()) {
                return EResp.createByErrorMessage("没有数据, 要不你检查下条件? 如果你坚持没有问题的话, 就联系下我吧~");
            }
            // 获取用户信息
            Map<Integer, String> userMap = fService.getUserMap(resScore.keySet().stream().map(Integer::valueOf).collect(Collectors.toList()));
            // 排序后的对象
            LinkedHashMap<String, Integer> sorted = SortUtil.sortMapWithValue(resScore);
            // 需要返回的对象
            List<JSONObject> res = Lists.newLinkedList();
            // 排序对象
            int index = 1;
            // 遍历
            for (Map.Entry<String, Integer> e : sorted.entrySet()) {
                // 忽略剩余的人
                if (index > rangeRanks) {
                    continue;
                }
                JSONObject j = new JSONObject();
                j.put("order", index);
                j.put("score", e.getValue());
                String user = userMap.get(Integer.valueOf(e.getKey()));
                if (StringUtils.isBlank(user)) {
                    j.put("someone" + e.getKey(), e.getKey());
                } else {
                    String[] split = user.split("\\(");
                    j.put("userNick", split[0]);
                    j.put("userName", split[1].replace(")", ""));
                }
                res.add(j);
                // 排名增加
                index++;
            }
            // 返回消息
            return EResp.createBySuccess(res);
        }
        return EResp.createByErrorMessage("签名错误, 骚年~ 你要干什么?");
    }

    /**
     * 构建集合
     *
     * @param res
     * @param redisKey
     * @return
     */
    private Map<String, Integer> buildRank(Map<String, Integer> res, String redisKey) {
        // 前几名
        Set<ZSetOperations.TypedTuple> defRank = RedisUtil.rank(redisKey, 0, 99);
        if (CollUtil.isEmpty(defRank)) {
            return res;
        }
        // 遍历
        for (ZSetOperations.TypedTuple t : defRank) {
            // 之前得分
            Integer score = res.getOrDefault(Objects.requireNonNull(t.getValue()).toString(), 0);
            // 当前得分
            Integer timesScore = Objects.requireNonNull(t.getScore()).intValue();
            // 集合对象
            res.put(t.getValue().toString(), score + timesScore);
        }
        return res;
    }
}
