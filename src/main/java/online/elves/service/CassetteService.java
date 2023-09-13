package online.elves.service;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import online.elves.config.Const;
import online.elves.constants.CassetteConst;
import online.elves.enums.CrLevel;
import online.elves.service.models.cassette.Fishnet;
import online.elves.service.models.cassette.Harpoon;
import online.elves.third.apis.IceNet;
import online.elves.third.fish.Fish;
import online.elves.utils.DateUtil;
import online.elves.utils.LotteryUtil;
import online.elves.utils.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 盲盒服务类
 */
@Slf4j
@Component
public class CassetteService {

    /**
     * 盲盒基础概率
     */
    private static final TreeMap<Integer, Double> odds = new TreeMap<>();
    /**
     * 渔网概率
     */
    private static final TreeMap<Integer, Double> odds_get = new TreeMap<>();
    /**
     * 渔网使用概率
     */
    private static final TreeMap<Integer, Double> odds_fishnet_skill = new TreeMap<>();
    /**
     * 鱼叉使用概率
     */
    private static final TreeMap<Integer, Double> odds_harpoon_skill = new TreeMap<>();

    // 初始化概率
    static {
        // 渔网
        odds.put(0, 0.20);
        // 鱼叉
        odds.put(1, 0.60);
        // 空盒子
        odds.put(2, 0.20);

        /* 颜色 */

        // 橙色
        odds_get.put(0, 0.20);
        // 紫色
        odds_get.put(1, 0.40);
        // 白色
        odds_get.put(2, 0.40);


        /* 技能 */

        odds_fishnet_skill.put(0, 0.05);
        odds_fishnet_skill.put(1, 0.15);
        odds_fishnet_skill.put(2, 0.40);
        odds_fishnet_skill.put(3, 0.40);


        odds_harpoon_skill.put(0, 0.20);
        odds_harpoon_skill.put(1, 0.30);
        odds_harpoon_skill.put(2, 0.50);
    }

    /**
     * 开盲盒
     *
     * @param user
     * @param isFishnet
     * @return
     */
    public static void open(String user, boolean isFishnet) {
        if (StringUtils.isBlank(user)) {
            return;
        }
        // 开奖
        switch (LotteryUtil.getLv(odds)) {
            case 0:
                if (isFishnet){
                    // 处理渔网
                    dealFishnet(user, LotteryUtil.getLv(odds_get));
                }else {
                    // 什么都没有
                    Fish.sendMsg("亲爱的 @" + user + " " + CrLevel.getCrLvName(user) + " " + " . 你竟然在沙子下面发现一个鱼翅!");
                    CurrencyService.sendCurrency(user, 1, "聊天室游戏-捡鱼叉收获");
                }
                break;
            case 1:
                if (isFishnet){
                    // 什么都没有
                    Fish.sendMsg("亲爱的 @" + user + " " + CrLevel.getCrLvName(user) + " " + " . 你竟然在沙子下面发现一个鱼翅!");
                    CurrencyService.sendCurrency(user, 1, "聊天室游戏-摸渔网收获");
                }else {
                    // 处理鱼叉
                    dealHarpoon(user, LotteryUtil.getLv(odds_get), false);
                }
                break;
            case 2:
                if (isFishnet){
                    // 处理渔网
                    dealFishnet(user, LotteryUtil.getLv(odds_get));
                }else {
                    // 处理鱼叉
                    dealHarpoon(user, LotteryUtil.getLv(odds_get), false);
                }
                break;
            default:
                break;
        }
    }

    /**
     * 处理获得鱼叉
     *
     * @param user
     * @param lv
     * @param isFishnet
     */
    private static void dealHarpoon(String user, int lv, boolean isFishnet) {
        // 鱼叉key
        String harpoonKey = CassetteConst.HARPOON_PREFIX + user;
        // 鱼叉列表
        List<Harpoon> harpoons = Lists.newArrayList();
        // 存在就反序列化
        String tmp = RedisUtil.get(harpoonKey);
        if (StringUtils.isNotBlank(tmp)) {
            harpoons = JSON.parseArray(tmp, Harpoon.class);
        }
        if (harpoons.size() > 9) {
            Fish.send2User(user, "亲爱的玩家. 你背的十把鱼叉太沉了, 已经没有地方放了~ \n\n > 快去标记别的赶海人吧! 不知道标记谁的话, 就去标记我老板吧~ 嘻嘻");
            return;
        }
        // 获得的名称
        String name = lv == 0 ? "橙色鱼叉" : lv == 1 ? "紫色鱼叉" : "白色鱼叉";
        // 加进去
        harpoons.add(Harpoon.builder().quality(lv).name(name).actTime(LocalDateTime.now()).build());
        // 永久有效
        RedisUtil.set(harpoonKey, JSON.toJSONString(harpoons));
        if (isFishnet){
            // 发消息
            Fish.send2User(user, "亲爱的玩家. 你拿出渔网里的东西一看. 竟然是 `" + name + "`~");
        }else {
            // 发消息
            Fish.send2User(user, "亲爱的玩家. 你踩在沙滩上感觉有什么东西硌脚, 抬起脚一看. 竟然是 `" + name + "`~");
        }

    }

    /**
     * 处理获得渔网
     *
     * @param user
     * @param lv
     */
    private static void dealFishnet(String user, int lv) {
        // 渔网key
        String fishnetKey = CassetteConst.FISHNET_PREFIX + user;
        // 渔网
        Fishnet fishnet = new Fishnet(RedisUtil.get(fishnetKey));
        // 当前时间
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime now_7 = LocalDateTime.now().plusDays(7);
        // 过期秒数
        long exp = 7 * 24 * 60 * 60;
        // 获得的名称
        String name = lv == 0 ? "橙色渔网" : lv == 1 ? "紫色渔网" : "白色渔网";
        if (fishnet.getQuality() < 0) {
            // 用新获得的
            fishnet = Fishnet.builder().quality(lv).name(name).actTime(now).expTime(now_7).build();
            // 放进去
            RedisUtil.set(fishnetKey, JSON.toJSONString(fishnet), exp);
            // 发消息
            Fish.send2User(user, "亲爱的玩家. 你感觉自己摸到了一个东西, 拿起来一看. 竟然是 `" + name + "`~ 渔网手柄上写着失效时间: " + DateUtil.formatDay(now_7));
        } else {
            // 比较对象
            if (fishnet.getQuality() > lv) {
                // 用新获得的
                fishnet = Fishnet.builder().quality(lv).name(name).actTime(now).expTime(now_7).build();
                // 放进去
                RedisUtil.set(fishnetKey, JSON.toJSONString(fishnet), exp);
                // 发消息
                Fish.send2User(user, "亲爱的玩家. 你感觉自己摸到了一个东西, 拿起来一看. 竟然是 `" + name + "`~ 渔网手柄上写着失效时间: " + DateUtil.formatDay(now_7));
            } else if (fishnet.getQuality() < lv) {
                // 什么也没有
                Fish.send2User(user, "亲爱的玩家 . 你感觉自己摸到了一个东西, 拿起来一看. 竟然是 `" + name + "`~ 可是你看了看腰里别着的渔网, 还没自己的好呢, 果断丢回了大海! 嘻嘻");
            } else {
                // 刷新当前品质时间
                fishnet.setActTime(now);
                fishnet.setExpTime(now_7);
                // 放回去
                RedisUtil.set(fishnetKey, JSON.toJSONString(fishnet), exp);
                // 发消息
                Fish.send2User(user, "亲爱的玩家 . 你感觉自己摸到了一个东西, 拿起来一看. 竟然是 `" + name + "`~ 你看了看腰里别着的渔网, 两个放在一起, 渔网手柄上写的失效时间竟然刷新了: " + DateUtil.formatDay(now_7));
            }
        }
    }

    /**
     * 使用渔网
     *
     * @param user
     * @return
     */
    public static int useFishnet(String user) {
        // 渔网key
        String fishnetKey = CassetteConst.FISHNET_PREFIX + user;
        // 渔网
        Fishnet fishnet = new Fishnet(RedisUtil.get(fishnetKey));
        // 渔网品质 没有渔网是 -1
        switch (fishnet.getQuality()) {
            case 0:
                // 橙色
                // 预计获得的数量
                int oCount = new Random().nextInt(11) + 10;
                // 技能
                int oLv = LotteryUtil.getLv(odds_fishnet_skill);
                switch (oLv) {
                    case 0:
                        // 获得等量鱼翅
                        Fish.sendMsg("亲爱的 @" + user + " " + CrLevel.getCrLvName(user) + " " + " . 你捞鱼丸的时候捞到了一个`鱼翅福袋`哦~ 橙色渔名不虚传");
                        CurrencyService.sendCurrency(user, oCount, "橙色渔网技能-获得等量鱼翅");
                        break;
                    case 1:
                        // 获得等量鱼丸
                        Fish.sendMsg("亲爱的 @" + user + " " + CrLevel.getCrLvName(user) + " " + " . 你捞鱼丸的时候捞到了一个`鱼丸福袋`哦~ 橙色渔网好厉害");
                        CurrencyService.sendCurrencyFree(user, oCount, "橙色渔网技能-鱼丸数量翻倍");
                        break;
                    case 2:
                        // 中了个鱼叉
                        Fish.sendMsg("亲爱的 @" + user + " " + CrLevel.getCrLvName(user) + " " + " . 你捞鱼丸的时候捞到了一个`鱼叉`哦~ 橙色渔网牛波一~");
                        dealHarpoon(user, new Random().nextInt(3), true);
                        break;
                    case 3:
                    default:
                        // 无事发生
                        break;
                }
                return oCount;
            case 1:
                // 紫色
                // 预计获得的数量
                int pCount = new Random().nextInt(6) + 10;
                // 技能
                int pLv = LotteryUtil.getLv(odds_fishnet_skill);
                switch (pLv) {
                    case 0:
                        // 获得等量鱼丸
                        Fish.sendMsg("亲爱的 @" + user + " " + CrLevel.getCrLvName(user) + " " + " . 你捞鱼丸的时候捞到了一个`鱼丸福袋`哦~ 紫色渔网好厉害");
                        CurrencyService.sendCurrencyFree(user, pCount, "紫色渔网技能-鱼丸数量翻倍");
                        break;
                    case 1:
                        // 中了个鱼叉
                        Fish.sendMsg("亲爱的 @" + user + " " + CrLevel.getCrLvName(user) + " " + " . 你捞鱼丸的时候捞到了一个`鱼叉`哦~ 紫色渔网也牛波一~");
                        dealHarpoon(user, new Random().nextInt(3), true);
                        break;
                    case 2:
                    case 3:
                    default:
                        // 无事发生
                        break;
                }
                return pCount;
            case 2:
                // 白色
                return new Random().nextInt(10) + 1;
            case -1:
            default:
                // 没有渔网
                return new Random().nextInt(11);

        }
    }

    /**
     * 标记玩家 使用鱼叉
     *
     * @param userName
     * @param target
     * @param userHarpoonKey
     */
    public static void biu(String userName, String target, String userHarpoonKey) {
        // 鱼叉列表
        List<Harpoon> harpoons = JSON.parseArray(RedisUtil.get(userHarpoonKey), Harpoon.class);
        // 获取最小的对象
        Harpoon harpoon = harpoons.stream().min(Comparator.comparing(Harpoon::getActTime)).get();
        // 移除对象
        harpoons.remove(harpoon);
        // 放回去 完成消耗
        RedisUtil.set(userHarpoonKey, JSON.toJSONString(harpoons));
        // 标记老板了, 直接消耗鱼叉
        if (target.equals(RedisUtil.get(Const.ADMIN))) {
            Fish.send2User(userName, "你在`" + DateUtil.formatDay(harpoon.getActTime()) + "`获得的`" + harpoon.getName() + "`已销毁[Cause:标记渔场老板(老板怒气值+1<嘻嘻, 逗你玩儿>)]");
            return;
        }
        // 被标记玩家鱼翅
        int currency = CurrencyService.getCurrency(target);
        // 被标记玩家鱼丸
        int currencyFree = CurrencyService.getCurrencyFree(target);
        // 区别对待鱼叉品质
        switch (harpoon.getQuality()) {
            case 0:
                // 橙色
                Fish.send2User(userName, "你在`" + DateUtil.formatDay(harpoon.getActTime()) + "`获得的`" + harpoon.getName() + "`已使用, 技能发动成功~");
                // 技能概率 100%
                switch (LotteryUtil.getLv(odds_harpoon_skill)) {
                    case 0:
                        // 鱼翅数量
                        int count0 = new Random().nextInt(11) + 15;
                        if (count0 > currency) {
                            count0 = currency;
                        }
                        CurrencyService.sendCurrency(target, -count0, "橙色鱼叉抢夺, 被用户 " + userName + " 标记! 快去复仇~");
                        CurrencyService.sendCurrency(userName, count0, "橙色鱼叉抢夺, 被标记用户 " + target);
                        if (target.equals("xiaoIce")) {
                            IceNet.bribe(-count0 * 2, userName, Objects.requireNonNull(Fish.getUser(userName)).getOId(), "鱼翅");
                        }
                        break;
                    case 1:
                        // 鱼翅数量
                        int count1 = new Random().nextInt(11) + 10;
                        if (count1 > currency) {
                            count1 = currency;
                        }
                        CurrencyService.sendCurrency(target, -count1, "橙色鱼叉抢夺, 被用户 " + userName + " 标记! 快去复仇~");
                        CurrencyService.sendCurrency(userName, count1, "橙色鱼叉抢夺, 被标记用户 " + target);
                        if (target.equals("xiaoIce")) {
                            IceNet.bribe(-count1 * 2, userName, Objects.requireNonNull(Fish.getUser(userName)).getOId(), "鱼翅");
                        }
                        break;
                    case 2:
                        // 鱼翅数量
                        int count2 = new Random().nextInt(11) + 5;
                        if (count2 > currency) {
                            count2 = currency;
                        }
                        CurrencyService.sendCurrency(target, -count2, "橙色鱼叉抢夺, 被用户 " + userName + " 标记! 快去复仇~");
                        CurrencyService.sendCurrency(userName, count2, "橙色鱼叉抢夺, 被标记用户 " + target);
                        if (target.equals("xiaoIce")) {
                            IceNet.bribe(-count2 * 2, userName, Objects.requireNonNull(Fish.getUser(userName)).getOId(), "鱼翅");
                        }
                        break;
                    default:
                        break;
                }
                break;
            case 1:
                // 紫色
                int lv1 = LotteryUtil.getLv(odds_harpoon_skill);
                // 技能发动成功
                if (new Random().nextInt(10) + 1 < 9 && lv1 > 0) {
                    Fish.send2User(userName, "你在`" + DateUtil.formatDay(harpoon.getActTime()) + "`获得的`" + harpoon.getName() + "`已使用, 技能发动成功~");
                    switch (lv1) {
                        case 1:
                            // 鱼翅数量
                            int count1 = new Random().nextInt(11) + 1;
                            if (count1 > currency) {
                                count1 = currency;
                            }
                            CurrencyService.sendCurrency(target, -count1, "紫色鱼叉抢夺, 被用户 " + userName + " 标记! 快去复仇~");
                            CurrencyService.sendCurrency(userName, count1, "紫色鱼叉抢夺, 被标记用户 " + target);
                            if (target.equals("xiaoIce")) {
                                IceNet.bribe(-count1 * 2, userName, Objects.requireNonNull(Fish.getUser(userName)).getOId(), "鱼翅");
                            }
                            break;
                        case 2:
                            // 鱼翅数量
                            int count2 = new Random().nextInt(11) + 10;
                            if (count2 > currencyFree) {
                                count2 = currencyFree;
                            }
                            CurrencyService.sendCurrencyFree(target, -count2, "紫色鱼叉抢夺, 被用户 " + userName + " 标记! 快去复仇~");
                            CurrencyService.sendCurrencyFree(userName, count2, "紫色鱼叉抢夺, 被标记用户 " + target);
                            if (target.equals("xiaoIce")) {
                                IceNet.bribe(-count2 * 2, userName, Objects.requireNonNull(Fish.getUser(userName)).getOId(), "鱼丸");
                            }
                            break;
                        default:
                            break;
                    }
                } else {
                    // 失败
                    Fish.send2User(userName, "你在`" + DateUtil.formatDay(harpoon.getActTime()) + "`获得的`" + harpoon.getName() + "`已使用, 但是技能发动失败了, 什么也没得到~ 失望之余你看到了地上又一个`鱼翅福袋`, 捡了起来~");
                    CurrencyService.sendCurrency(userName, new Random().nextInt(3), "紫色鱼叉使用-意外捡到的`鱼翅福袋`");
                }
                break;
            case 2:
                // 白色处理
            default:
                // 技能发动成功
                if (new Random().nextInt(11) % 2 == 0 && LotteryUtil.getLv(odds_harpoon_skill) == 2) {
                    // 发生惨案的鱼丸
                    int count2 = new Random().nextInt(11);
                    if (currencyFree < count2) {
                        count2 = currencyFree;
                    }
                    Fish.send2User(userName, "你在`" + DateUtil.formatDay(harpoon.getActTime()) + "`获得的`" + harpoon.getName() + "`已使用, 技能发动成功~");
                    CurrencyService.sendCurrencyFree(target, -count2, "白色鱼叉抢夺, 被用户 " + userName + " 标记! 快去复仇~");
                    CurrencyService.sendCurrencyFree(userName, count2, "白色鱼叉抢夺, 被标记用户 " + target);
                    if (target.equals("xiaoIce")) {
                        IceNet.bribe(-count2 * 2, userName, Objects.requireNonNull(Fish.getUser(userName)).getOId(), "鱼丸");
                    }
                    break;
                } else {
                    Fish.send2User(userName, "你在`" + DateUtil.formatDay(harpoon.getActTime()) + "`获得的`" + harpoon.getName() + "`已使用, 但是技能发动失败了, 什么也没得到~ 失望之余你看到了地上又一个`鱼丸福袋`, 捡了起来~");
                    CurrencyService.sendCurrency(userName, new Random().nextInt(11), "白色鱼叉使用-意外捡到的`鱼丸福袋`");
                }
                break;
        }
    }
}
