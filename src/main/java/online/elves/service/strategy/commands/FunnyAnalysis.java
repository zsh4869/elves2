package online.elves.service.strategy.commands;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import online.elves.config.Const;
import online.elves.enums.CrLevel;
import online.elves.enums.Words;
import online.elves.mapper.entity.User;
import online.elves.service.CassetteService;
import online.elves.service.FService;
import online.elves.service.CurrencyService;
import online.elves.service.strategy.CommandAnalysis;
import online.elves.third.apis.IceNet;
import online.elves.third.apis.Joke;
import online.elves.third.fish.Fish;
import online.elves.utils.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 娱乐命令分析
 */
@Slf4j
@Component
public class FunnyAnalysis extends CommandAnalysis {

    @Resource
    FService fService;


    /**
     * 关键字
     */
    private static final List<String> keys = Arrays.asList("去打劫", "笑话", "捞鱼丸", "等级", "发个红包", "来个红包", "V50", "v50", "今日水分", "15", "欧皇们", "非酋们", "探路者", "触发词", "爱的回馈", "窝囊费");

    /**
     * 打劫概率
     */
    private static TreeMap<Integer, Double> odds = new TreeMap<>();
    /**
     * 小冰出手了
     */
    private static TreeMap<Integer, Double> odds_ice = new TreeMap<>();
    /**
     * 520 临时抽奖
     */
    private static TreeMap<Integer, Double> odds_520 = new TreeMap<>();

    // 初始化概率
    static {
        // 32-64积分
        odds.put(0, 0.04);
        // 0-2 个鱼翅
        odds.put(1, 0.20);
        // 0-10 个鱼丸
        odds.put(2, 0.36);
        // 无功而返
        odds.put(3, 0.20);
        // 什么也没有 随机扣1-3个鱼丸
        odds.put(4, 0.20);

        // 32-64积分
        odds_ice.put(0, 0.20);
        // 0-2 个鱼翅
        odds_ice.put(1, 0.40);
        // 0-10 个鱼丸
        odds_ice.put(2, 0.40);


        // 1314积分
        odds_520.put(0, 0.001);
        // 520 积分
        odds_520.put(1, 0.044);
        // 77 积分
        odds_520.put(2, 0.055);
        // 7 鱼翅
        odds_520.put(3, 0.200);
        // 7 鱼丸
        odds_520.put(4, 0.300);
        // 0 谢谢参与
        odds_520.put(5, 0.400);
    }

    @Override
    public boolean check(String commonKey) {
        return keys.contains(commonKey);
    }

    @Override
    public void process(String commandKey, String commandDesc, String userName) {
        // 娱乐命令
        switch (commandKey) {
            case "20230824101011":
                LocalDateTime now520 = LocalDateTime.now();
                if (now520.isAfter(LocalDateTime.of(now520.toLocalDate(), LocalTime.of(18, 0, 0)))) {
                    Fish.sendMsg("亲爱的 @" + userName + " " + CrLevel.getCrLvName(userName) + " " + "  七夕活动已经结束啦~ 期待下次活动与你相遇...嘻嘻!~不要摸鱼了, 快跟最爱的Ta去过七夕吧❤️ ");
                } else {
                    // 过期时间 到明天0点
                    int exp = Long.valueOf(Duration.between(now520, now520.toLocalDate().plusDays(1).atStartOfDay()).getSeconds()).intValue();
                    // 幸运标识
                    String luck520 = RedisUtil.get("LUCK:520:" + userName);
                    // 初始化
                    if (StringUtils.isBlank(luck520)) {
                        luck520 = "0";
                    }
                    // 每人限制三次
                    if (Integer.parseInt(luck520) < 3) {
                        // 数字化
                        int anInt = Integer.parseInt(luck520);
                        // 回写
                        RedisUtil.reSet("LUCK:520:" + userName, String.valueOf(anInt + 1), exp);
                        // 抽奖
                        switch (LotteryUtil.getLv(odds_520)) {
                            case 0:
                                Fish.sendMsg("# 💐💐恭喜恭喜💐💐 @" + userName + " " + CrLevel.getCrLvName(userName) + " " + " 获得`特等奖` [**1314**] 积分~");
                                Fish.sendSpecify(userName, 1314, userName + ", 七夕活动 特等奖!");
                                break;
                            case 1:
                                Fish.sendMsg("## 💐恭喜💐 @" + userName + " " + CrLevel.getCrLvName(userName) + " " + " 获得`一等奖` [**520**] 积分~");
                                Fish.sendSpecify(userName, 520, userName + ", 七夕活动 一等奖!");
                                break;
                            case 2:
                                Fish.sendMsg("### 💐恭喜💐 @" + userName + " " + CrLevel.getCrLvName(userName) + " " + " 获得`二等奖` [**77**] 积分~");
                                Fish.sendSpecify(userName, 77, userName + ", 七夕活动 二等奖!");
                                break;
                            case 3:
                                Fish.sendMsg("#### 💐恭喜💐 @" + userName + " " + CrLevel.getCrLvName(userName) + " " + " 获得`三等奖` [**7**] 鱼翅~");
                                CurrencyService.sendCurrency(userName, 7, "聊天室活动-七夕节日抽奖-三等奖");
                                break;
                            case 4:
                                Fish.sendMsg("#### 💐恭喜💐 @" + userName + " " + CrLevel.getCrLvName(userName) + " " + " 获得`四等奖` [**7**] 鱼丸~");
                                CurrencyService.sendCurrencyFree(userName, 7, "聊天室活动-七夕节日抽奖-四等奖");
                                break;
                            case 5:
                                Fish.sendMsg("@" + userName + " " + CrLevel.getCrLvName(userName) + " " + " 谢谢参与, 祝你今天和Ta开心快乐~");
                                break;
                            default:
                                break;
                        }
                    } else {
                        Fish.send2User(userName, "亲, 每人只有三次抽奖机会, 你已经用完啦~快期待和你的Ta过快乐的节日吧~❤️ 嘿嘿");
                    }
                }
                break;
            case "去打劫":
                // 财阀标记
                String cfCount = RedisUtil.get(Const.CURRENCY_TIMES_PREFIX + userName);
                if (StringUtils.isNotBlank(cfCount)) {
                    // 幸运编码
                    String lKey = "luck:try:" + userName;
                    // 是财阀. 每天第一次打劫 概率获得sth.
                    if (StringUtils.isBlank(RedisUtil.get(lKey))) {
                        // 当前时间
                        LocalDateTime now = LocalDateTime.now();
                        // 第二天0点过期
                        RedisUtil.set(lKey, userName, Long.valueOf(Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).getSeconds()).intValue());
                        // 奖品等级
                        int lv = LotteryUtil.getLv(odds);
                        // 小冰助力
                        boolean ice = false;
                        // 小冰亲密度大于1000 则打劫小冰会出手
                        if (IceNet.getUserIntimacy(userName) > 1000) {
                            lv = LotteryUtil.getLv(odds_ice);
                            ice = true;
                        }
                        // 计算概率 送东西
                        switch (lv) {
                            case 0:
                                int money = new SecureRandom().nextInt(32) + 32;
                                Fish.sendMsg("@" + userName + " " + CrLevel.getCrLvName(userName) + " " + " 承你吉言.我" + (ice ? "和小冰" : "") + "打劫回来咯~ 我抢到了300积分, 可是半路摔了一跤, 就剩... " + money + "  积分...了, ┭┮﹏┭┮ 呜呜呜~");
                                Fish.sendSpecify(userName, money, userName + ", 喏~ 给你!");
                                break;
                            case 1:
                                int s1 = new SecureRandom().nextInt(3);
                                Fish.sendMsg("@" + userName + " " + CrLevel.getCrLvName(userName) + " " + " 哇.我" + (ice ? "和小冰" : "") + "打劫回来了~ 抢到了... " + s1 + "  个`鱼翅`...等下你要分我点啊~ ^_^");
                                CurrencyService.sendCurrency(userName, s1, "聊天室活动-打劫");
                                break;
                            case 2:
                                int s2 = new SecureRandom().nextInt(11);
                                Fish.sendMsg("@" + userName + " " + CrLevel.getCrLvName(userName) + " " + " 哇.我" + (ice ? "和小冰" : "") + "打劫回来了~ 抢到了... " + s2 + "  个`鱼丸`...等下你要分我点啊~ ^_^");
                                CurrencyService.sendCurrencyFree(userName, s2, "聊天室活动-打劫");
                                break;
                            case 3:
                                Fish.sendMsg("@" + userName + " " + CrLevel.getCrLvName(userName) + " " + " 哎呦呦...我头晕~ 打劫的事情改日再说吧...");
                                break;
                            case 4:
                                int rz = new SecureRandom().nextInt(3) + 1;
                                Fish.sendMsg("@" + userName + " " + CrLevel.getCrLvName(userName) + " " + " 哼, 一天啥事儿没干净陪你打劫了. 还啥也抢不到... 撂挑子不干了");
                                CurrencyService.sendCurrencyFree(userName, -rz, "聊天室活动-打劫-无功而返");
                                break;
                            default:
                                break;
                        }
                        return;
                    }
                    // 不为空 啥也不做....
                }
                Fish.sendMsg("@" + userName + " " + CrLevel.getCrLvName(userName) + " " + Words.random("r"));
                break;
            case "爱的回馈":
                // 财阀标记
                String loveCount = RedisUtil.get(Const.CURRENCY_TIMES_PREFIX + userName);
                if (StringUtils.isNotBlank(loveCount)) {
                    // 幸运编码
                    String lKey = "love:return:" + userName;
                    // 是财阀. 每天第一次打劫 概率获得sth.
                    if (StringUtils.isBlank(RedisUtil.get(lKey))) {
                        if (IceNet.getUserIntimacy(userName) > 10240) {
                            // 当前时间
                            LocalDateTime now = LocalDateTime.now();
                            // 第二天0点过期
                            RedisUtil.set(lKey, userName, Long.valueOf(Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).getSeconds()).intValue());
                            int money = new SecureRandom().nextInt(32) + 32;
                            Fish.sendMsg("@" + userName + " " + CrLevel.getCrLvName(userName) + " 小冰感受到了你的爱, 让我谢谢你哦~");
                            Fish.sendSpecify(userName, money, userName + ", 爱的回馈!");
                        } else {
                            Fish.sendMsg("亲爱的 @" + userName + " " + CrLevel.getCrLvName(userName) + " " + " . 渔民大人~ 和小冰的亲密度要大于`10240`才会每天有`爱的回馈`呢, 加油呀!~");
                        }
                    } else {
                        Fish.sendMsg("亲爱的 @" + userName + " " + CrLevel.getCrLvName(userName) + " " + " . 嘻嘻~ 今天回馈过了呢~");
                    }
                } else {
                    Fish.sendMsg("亲爱的 @" + userName + " " + CrLevel.getCrLvName(userName) + " " + " . 你还没有成为我的渔民大人呐~");
                }
                break;
            case "笑话":
                Fish.sendMsg("@" + userName + " " + CrLevel.getCrLvName(userName) + " " + "  \n\n" + Joke.getJoke());
                break;
            case "捞鱼丸":
                String zzk = RedisUtil.get(Const.CURRENCY_FREE_TIME);
                if (StringUtils.isBlank(zzk)) {
                    Fish.send2User(userName, "渔民大人~ 心急吃不了热豆腐啦~ 现在天空一片晴朗, 哪里像有鱼丸的样子呀. 嘻嘻");
                } else {
                    // 当前兑换次数
                    String times = RedisUtil.get(Const.CURRENCY_TIMES_PREFIX + userName);
                    if (StringUtils.isBlank(times)) {
                        Fish.sendMsg("亲爱的 @" + userName + " " + CrLevel.getCrLvName(userName) + " " + " . 你还没有成为我的渔民大人呐~");
                        break;
                    }
                    // 缓存key 没人只有一次机会
                    String rKey = "zzk-" + userName;
                    // 获取对象
                    String zzkU = RedisUtil.get(rKey);
                    if (StringUtils.isBlank(zzkU)) {
                        // 可以沾
                        CurrencyService.sendCurrencyFree(userName, CassetteService.useFishnet(userName), zzk);
                        // 设置缓存 180 肯定大于活动时间
                        RedisUtil.set(rKey, userName, 180);
                    } else {
                        Fish.send2User(userName, "你已经参与过啦~ 期待下次咯. 嘻嘻");
                    }
                }
                break;
            case "15":
            case "今日水分":
                // 用户编码
                Integer userNo_ = Fish.getUserNo(userName);
                // 缓存key
                String key_ = StrUtils.getKey(Const.RANKING_DAY_PREFIX, "20", DateUtil.format(new Date(), "yyyyMMdd"));
                // 获取得分
                Double score_ = RedisUtil.getScore(key_, userNo_ + "");
                // 不存在就赋值 0
                if (Objects.isNull(score_)) {
                    score_ = Double.valueOf("0");
                }
                // 当前经验
                int exp_ = score_.intValue();
                Fish.sendMsg("亲爱的 @" + userName + " 你今天水了 [ " + exp_ + " ] 点经验啦~" + " \n\n > 一起工作的才叫同事, 一起摸鱼的叫同伙~ 加油, 同伙");
                break;
            case "等级":
                // 用户编码
                Integer userNo = Fish.getUserNo(userName);
                // 缓存key
                String key = StrUtils.getKey(Const.RANKING_PREFIX, "24");
                // 获取得分
                Double score = RedisUtil.getScore(key, userNo + "");
                // 不存在就赋值 0
                if (Objects.isNull(score)) {
                    score = Double.valueOf("0");
                }
                // 当前经验
                int exp = score.intValue();
                // 当前等级
                CrLevel crLv = CrLevel.get(exp);
                Fish.sendMsg("亲爱的 @" + userName + " 您的聊天室等级为 " + CrLevel.getCrLvName(userName) + " [当前经验值: " + exp + "/" + crLv.end + "] " + " \n\n > 等级分为 " + String.join(" => ", Const.CHAT_ROOM_LEVEL_NAME));
                break;
            case "发个红包":
            case "来个红包":
                Fish.sendCMD("小冰 来个红包");
                break;
            case "V50":
            case "v50":
            case "窝囊费":
                // 日常奖励CD
                String cd = "DAILY:REWARDS:CD";
                // 幸运编码
                String lKey = "DAILY:REWARDS:USER:" + userName;
                // 是不是要翻倍
                boolean isDouble = IceNet.getUserIntimacy(userName) > 8192;
                // 当前周几
                switch (LocalDate.now().getDayOfWeek().getValue()){
                    case 4:
                        if ("V50,v50".contains(commandKey)){
                            if (isDouble){
                                dailyRewards(userName, lKey, cd, 128, " , 小冰说多给你点!", "KFC");
                            }else {
                                dailyRewards(userName, lKey, cd, 50, " 给, 彰显实力!", "KFC");
                            }
                        }else {
                            Fish.sendMsg("@" + userName + " 今天只能V50, 谁来也不行!");
                        }
                        break;
                    case 6:
                        if ("窝囊费".contains(commandKey)){
                            if (isDouble){
                                dailyRewards(userName, lKey, cd, 128, " 加班辛苦了,小冰说加油!", "WNF");
                            }else {
                                dailyRewards(userName, lKey, cd, 66, " 这B班, 不上也行!", "WNF");
                            }
                        }else {
                            Fish.sendMsg("@" + userName + " 加班不加班? 不加班就去享受自己的周末!!!");
                        }
                        break;
                    case 1:
                    case 2:
                    case 3:
                    case 5:
                    case 7:
                    default:
                        Fish.sendMsg("@" + userName + " 我掐指一算. 嘻嘻~ 今天什么日子都不是");
                        break;
                }
                break;
            case "欧皇们":
                // 返回对象
                JSONObject resp = JSON.parseObject(HttpUtil.get(RedisUtil.get("ICE:GAME:RANK")));
                // 排行榜
                JSONArray data = resp.getJSONArray("data");
                // 构建返回对象
                StringBuilder res = new StringBuilder("来看看咱们的欧皇们!").append("\n\n");
                buildTable(data, res);
                // 发送消息
                Fish.sendMsg(res.toString());
                break;
            case "非酋们":
                // 返回对象
                JSONObject uresp = JSON.parseObject(HttpUtil.get(RedisUtil.get("ICE:GAME:RANK:NULL:LUCK")));
                // 排行榜
                JSONArray udata = uresp.getJSONArray("data");
                // 构建返回对象
                StringBuilder ures = new StringBuilder("来看看咱们的非酋们! 统统不许笑").append("\n\n");
                // 组合下bug
                buildTable(udata, ures);
                // 发送消息
                Fish.sendMsg(ures.toString());
                break;
            case "探路者":
                // 返回对象
                JSONObject maze = JSON.parseObject(HttpUtil.get(RedisUtil.get("HANCEL:GAME:RANK:MAZE")));
                // 排行榜
                JSONArray mazeData = maze.getJSONArray("records");
                // 构建返回对象
                StringBuilder mRes = new StringBuilder("看看你的方向感怎么样, [迷宫游戏](https://maze.hancel.org/)排行榜, 积分大放送").append("\n\n");
                // 组合下bug
                buildMazeTable(mazeData, mRes);
                // 发送消息
                Fish.sendMsg(mRes.toString());
                break;
            case "触发词":
                // 违禁词
                String bWords = RedisUtil.get("BLACK:WORD");
                if (StringUtils.isBlank(bWords)) {
                    bWords = "";
                }
                if (RegularUtil.isOrderCase(commandDesc) && !bWords.contains(commandDesc)) {
                    // 加锁  增加 CD
                    if (StringUtils.isBlank(RedisUtil.get("CHANGE_CMD_WORD"))) {
                        // 修改 15秒
                        RedisUtil.set("CHANGE_CMD_WORD", "limit", 15);
                        // 鱼翅个数
                        int cTimes = CurrencyService.getCurrency(userName);
                        // 判断次数
                        if (cTimes < 0) {
                            // 啥也不做
                            Fish.sendMsg("亲爱的 @" + userName + " . 你还没有成为渔民呢(~~╭(╯^╰)╮~~)");
                        } else {
                            // 需要消耗66个鱼翅
                            int count = 66;
                            // 不够扣
                            if (count > cTimes) {
                                Fish.send2User(userName, "亲爱的渔民大人 . 执行自定义触发词需要 [" + count + "] `鱼翅`~ 但是你背包里没有那么多啦~");
                            } else {
                                // 增加鱼翅
                                CurrencyService.sendCurrency(userName, -count, "触发词修改消耗");
                                // 设置限定词
                                RedisUtil.set(Const.CMD_USER_SET + userName, "凌," + commandDesc);
                            }
                        }
                    } else {
                        Fish.send2User(userName, "亲爱的渔民大人. 业务繁忙, 请稍后重试(~~╭(╯^╰)╮~~), 全局锁`15s`");
                    }
                } else {
                    Fish.send2User(userName, "亲爱的渔民大人. 触发词为英文数字或中文字符[1,3]个哦. 不要瞎写(~~也许你有违禁词~~)!!!");
                }
                break;
            default:
                // 什么也不用做
                break;
        }
    }

    /**
     * 每日奖励
     * @param userName
     * @param lKey
     * @param cd
     */
    private static void dailyRewards(String userName, String lKey, String cd, int money, String memo, String rankKey) {
        // 每天只能有一次
        if (StringUtils.isBlank(RedisUtil.get(lKey))) {
            if (StringUtils.isBlank(RedisUtil.get(cd))) {
                // 当前时间
                LocalDateTime now = LocalDateTime.now();
                // 第二天0点过期
                RedisUtil.set(lKey, userName, Long.valueOf(Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).getSeconds()).intValue());
                // CD 10秒
                RedisUtil.set(cd, userName, 10);
                // 发红包
                Fish.sendSpecify(userName, money, userName + memo);
                // 记录排行榜
                RedisUtil.incrScore(Const.RANKING_PREFIX + rankKey, String.valueOf(Fish.getUserNo(userName)), 1);
            } else {
                Fish.sendMsg("@" + userName + " 不要复读, 不要着急. 我一分钟只能发六个哦~(其实能发十个, 但是我就不~ 嘻嘻)");
            }
        } else {
            Fish.sendMsg("@" + userName + " 怎么肥事儿~ 口袋空空, 我没有红包啦~");
        }
    }

    /**
     * 构建表格
     *
     * @param data
     * @param res
     */
    private void buildMazeTable(JSONArray data, StringBuilder res) {
        res.append("|排行|用户|已到达|总步数|").append("\n");
        res.append("|:----:|:----:|:----:|:----:|").append("\n");
        // 排行计数器
        AtomicInteger p = new AtomicInteger(0);
        data.forEach(x -> {
            if (p.get() > 9) {
                return;
            }
            // 转换对象
            JSONObject o = (JSONObject) x;
            res.append("|").append(p.addAndGet(1));
            // 用户
            User uname = fService.getUser(o.getString("username"));
            res.append("|").append(uname.getUserNick()).append("([").append(uname.getUserName()).append("](https://maze.hancel.org/u/").append(uname.getUserName()).append("))");
            res.append("|").append(o.getInteger("stage"));
            res.append("|").append(o.getInteger("step"));
            res.append("|").append("\n");
        });
    }

    /**
     * 构建表格
     *
     * @param data
     * @param res
     */
    private void buildTable(JSONArray data, StringBuilder res) {
        res.append("|排行|用户|抽奖次数|特等奖|一等奖|二等奖|三等奖|四等奖|五等奖|六等奖|参与奖|").append("\n");
        res.append("|:----:|:----:|:----:|:----:|:----:|:----:|:----:|:----:|:----:|:----:|:----:|").append("\n");
        AtomicInteger p = new AtomicInteger(0);
        data.stream().forEach(x -> {
            // 转换对象
            JSONObject o = (JSONObject) x;
            res.append("|").append(p.addAndGet(1));
            // 用户
            User uname = fService.getUser(o.getString("uname"));
            res.append("|").append(uname.getUserNick()).append("(").append(uname.getUserName()).append(")");
            res.append("|").append(o.getInteger("pay_times"));
            res.append("|").append(o.getInteger("lv1_times"));
            res.append("|").append(o.getInteger("lv2_times"));
            res.append("|").append(o.getInteger("lv3_times"));
            res.append("|").append(o.getInteger("lv4_times"));
            res.append("|").append(o.getInteger("lv5_times"));
            res.append("|").append(o.getInteger("lv6_times"));
            res.append("|").append(o.getInteger("lv7_times"));
            res.append("|").append(o.getInteger("lv8_times"));
            res.append("|").append("\n");
        });
    }

}
