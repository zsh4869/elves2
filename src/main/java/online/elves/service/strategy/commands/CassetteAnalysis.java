package online.elves.service.strategy.commands;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import online.elves.config.Const;
import online.elves.constants.CassetteConst;
import online.elves.enums.CrLevel;
import online.elves.service.CassetteService;
import online.elves.service.CurrencyService;
import online.elves.service.models.cassette.Harpoon;
import online.elves.service.strategy.CommandAnalysis;
import online.elves.third.fish.Fish;
import online.elves.utils.DateUtil;
import online.elves.utils.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 盲盒
 */
@Slf4j
@Component
public class CassetteAnalysis extends CommandAnalysis {
    /**
     * 关键字
     */
    private static final List<String> keys = Arrays.asList("摸渔网", "捡鱼叉", "标记", "侦查", "biu", "净化");

    @Override
    public boolean check(String commonKey) {
        return keys.contains(commonKey);
    }

    @Override
    public void process(String commandKey, String commandDesc, String userName) {
        // 当前用户鱼翅货币数量
        int currency = CurrencyService.getCurrency(userName);
        if (currency < 0) {
            Fish.sendMsg("亲爱的 @" + userName + " " + CrLevel.getCrLvName(userName) + " " + " . 你还没有成为渔民呐~");
            return;
        }
        // 货币命令
        switch (commandKey) {
            case "侦查":
                if (currency < 5) {
                    Fish.sendMsg("亲爱的 @" + userName + " " + CrLevel.getCrLvName(userName) + " " + " . 每次侦查要消耗`5个鱼翅`哦~ 去搞点鱼翅吧~");
                } else {
                    // 鱼翅
                    int times = CurrencyService.getCurrency(commandDesc);
                    if (times < 0) {
                        Fish.send2User(userName, "嘻嘻~ " + commandDesc + " . 还没有成为渔民呐~ 你侦查个什么劲儿...");
                    } else {
                        // 鱼丸
                        int lTimes = CurrencyService.getCurrencyFree(commandDesc);
                        // 没有就是0
                        if (lTimes < 0) {
                            lTimes = 0;
                        }
                        Fish.send2User(userName, "报告~ " + commandDesc + " . 的背包里`鱼翅`还有 ...`" + times + "`个~  `鱼丸`还有 ...`" + lTimes + "`个~");
                        // 扣除鱼翅
                        CurrencyService.sendCurrency(userName, -5, "聊天室活动-侦查经费");
                        // 发送通知
                        Fish.sendMsg("亲爱的 @" + userName + " " + CrLevel.getCrLvName(userName) + " " + " . 情报已发送, 请注意查收私信~");
                    }
                }
                break;
            case "摸渔网":
            case "捡鱼叉":
                if (currency < 5) {
                    // 啥也不做
                    Fish.sendMsg("亲爱的 @" + userName + " " + CrLevel.getCrLvName(userName) + " " + " . 每次赶海要消耗`5个鱼翅`哦~ 去搞点鱼翅吧~");
                } else {
                    // 开关锁
                    String timesKey = CassetteConst.LIMIT_PREFIX + userName;
                    // 防沉迷锁
                    String loseLimit = CassetteConst.LOST_LIMIT_PREFIX + userName;
                    // 防沉迷次数
                    int limit = Integer.parseInt(Optional.ofNullable(RedisUtil.get(loseLimit)).orElse("0"));
                    // 加锁
                    if (StringUtils.isBlank(RedisUtil.get(timesKey))) {
                        if (limit > 15) {
                            // 啥也不做
                            Fish.send2User(userName, "亲爱的玩家 . 涨潮啦~ 不能赶海咯! 歇歇吧~(15分钟内只可以赶海十五次哦!)");
                        } else {
                            // 开盲盒
                            CassetteService.open(userName, commandKey.contains("渔网"));
                            // 增加防沉迷次数
                            RedisUtil.reSet(loseLimit, String.valueOf(limit + 1), 15 * 60);
                            // 扣减鱼翅
                            CurrencyService.sendCurrency(userName, -5, "聊天室游戏-" + commandKey);
                            // 加锁 30秒
                            RedisUtil.set(timesKey, "limit", 30);
                        }
                    } else {
                        // 啥也不做
                        Fish.send2User(userName, "亲爱的玩家 . 小涨潮, 无法赶海~ 请耐心等待30秒");
                    }
                }
                break;
            case "净化":
                if (currency < 16) {
                    // 啥也不做
                    Fish.send2User(userName, "亲爱的 " + CrLevel.getCrLvName(userName) + " " + " . 每次 净化 要消耗`16个鱼翅`哦~ 去搞点鱼翅吧~");
                } else {
                    // 清除指定用户的限制
                    RedisUtil.del(CassetteConst.HARPOON_BIU_LIMIT + commandDesc);
                    // 发送通知
                    CurrencyService.sendCurrency(userName, -16, "聊天室活动-净化用户[" + commandDesc + "]的标记上限消耗");
                }
                break;
            case "biu":
            case "标记":
                // 技能加锁
                String harpoonLimit = CassetteConst.HARPOON_LIMIT;
                if (StringUtils.isBlank(RedisUtil.get(harpoonLimit)) || commandDesc.equals(RedisUtil.get(Const.ADMIN))) {
                    // 加锁 30秒
                    RedisUtil.set(harpoonLimit, userName, 30);
                    // 鱼叉key
                    String harpoonKey = CassetteConst.HARPOON_PREFIX + userName;
                    // 鱼叉列表
                    List<Harpoon> harpoons = Lists.newArrayList();
                    // 存在就反序列化
                    String tmp = RedisUtil.get(harpoonKey);
                    if (StringUtils.isNotBlank(tmp)) {
                        harpoons = JSON.parseArray(tmp, Harpoon.class);
                    }
                    if (CollUtil.isEmpty(harpoons)) {
                        // 啥也不做
                        Fish.sendMsg("亲爱的 @" + userName + " " + CrLevel.getCrLvName(userName) + " " + " . 你没有`鱼叉`啦~ 快去开盲盒吧~");
                    } else {
                        // 目标人物
                        if (CurrencyService.getCurrency(commandDesc) < 0) {
                            // 啥也不做
                            Fish.sendMsg("亲爱的 @" + userName + " " + CrLevel.getCrLvName(userName) + " " + commandDesc + "都不是渔民...可不要随便标记误伤别人哦~");
                        } else {
                            String biuLimitKey = CassetteConst.HARPOON_BIU_LIMIT + commandDesc;
                            // 被标记次数
                            int limit = Integer.parseInt(Optional.ofNullable(RedisUtil.get(biuLimitKey)).orElse("0"));
                            // 标记老板没有限制
                            if (commandDesc.equals(RedisUtil.get(Const.ADMIN))) {
                                // 标记
                                CassetteService.biu(userName, commandDesc, harpoonKey);
                            } else {
                                if (currency > 10 && CurrencyService.getCurrencyFree(userName) > 20) {
                                    if (limit > 4) {
                                        // 啥也不做
                                        Fish.sendMsg("亲爱的 @" + userName + " " + CrLevel.getCrLvName(userName) + " " + " . '" + commandDesc + "' 今天已经被标记了五次了, 放过他吧~");
                                    } else {
                                        // 标记
                                        CassetteService.biu(userName, commandDesc, harpoonKey);
                                        // 记上limit
                                        LocalDateTime now = LocalDateTime.now();
                                        LocalDateTime nextDay = LocalDateTime.now().toLocalDate().plusDays(1).atStartOfDay();
                                        // 明天0点过期
                                        RedisUtil.reSet(biuLimitKey, String.valueOf(limit + 1), DateUtil.getInterval(now, nextDay, ChronoUnit.SECONDS));
                                    }
                                } else {
                                    // 啥也不做
                                    Fish.send2User(userName, "亲爱的 " + CrLevel.getCrLvName(userName) + " " + " . 标记别人需要保证自己至少有`10个鱼翅`和`20个鱼丸`哦~不要总想空手套白狼! 嘻嘻");
                                }

                            }

                        }
                    }
                } else {
                    // 啥也不做
                    Fish.send2User(userName, "亲爱的 " + CrLevel.getCrLvName(userName) + " " + " . 鱼叉标记结算中, 请稍后再试~");
                }
                break;
            default:
                // 什么都不用做
                break;
        }
    }
}
