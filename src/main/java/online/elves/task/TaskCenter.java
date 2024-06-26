package online.elves.task;

import cn.hutool.core.collection.CollUtil;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import online.elves.config.Const;
import online.elves.task.service.TaskService;
import online.elves.third.fish.Fish;
import online.elves.utils.DateUtil;
import online.elves.utils.RedisUtil;
import online.elves.ws.WsClient;
import online.elves.ws.handler.UserChat;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * 活动中心.
 */
@Slf4j
@Component
public class TaskCenter {
    @Resource
    TaskService taskService;

    /**
     * 五秒一次
     */
    @Scheduled(cron = "0/5 * * * * ?")
    public void check15Sec() {
        // 开红包
        taskService.buyCurrency();
        // 在线检查
        taskService.onlineCheck();
    }

    /**
     * 心跳检测 30秒
     */
    @Scheduled(cron = "0/30 * * * * ?")
    public void check30Sec() {
        // 如果对象全部空了
        if (CollUtil.isEmpty(WsClient.session)) {
            // 重建
            WsClient.start(null);
            try {
                // 放入频道
                String elves = RedisUtil.get(Const.ELVES_MAME);
                // 精灵自己的频道
                String uri = "wss://fishpi.cn/user-channel?apiKey=" + Fish.getKey();
                // 建立连接
                WebSocketContainer container = ContainerProvider.getWebSocketContainer();
                WsClient.session.put(elves, Pair.of(DateUtil.maxTime(), container.connectToServer(new UserChat(elves), URI.create(uri))));
            } catch (Exception e) {
                log.info("精灵 建立 ws 链接失败了...{}", e.getMessage());
            }
            return;
        }
        // 临时对象
        Map<String, Pair<LocalDateTime, Session>> temp = Maps.newConcurrentMap();
        // 遍历
        for (Map.Entry<String, Pair<LocalDateTime, Session>> et : WsClient.session.entrySet()) {
            // 对象
            String sKey = et.getKey();
            // session
            Pair<LocalDateTime, Session> etValue = et.getValue();
            // 五分钟前
            LocalDateTime dt = LocalDateTime.now().minusMinutes(5);
            if (etValue.getKey().isAfter(dt)) {
                try {
                    // 没有过期 发送心跳
                    etValue.getValue().getBasicRemote().sendPing(StandardCharsets.UTF_8.encode("-hb-"));
                    // 发送后回写
                    temp.put(sKey, etValue);
                } catch (Exception e) {
                    log.info("{} session 心跳发送失败...{}", sKey, e.getMessage());
                }
            } else {
                // 关闭连接
                try {
                    etValue.getValue().close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "超时清理"));
                } catch (Exception e) {
                    log.info("{} session 关闭失败...{}", sKey, e.getMessage());
                }
            }
        }
        // 回写
        WsClient.session = temp;
    }

    /**
     * 三分钟一次
     */
    @Scheduled(cron = "7 0/2 * * * ?")
    public void check3min() {
        // 记录红包
        taskService.recordRpLog();
    }

    /**
     * 五分钟一次
     */
    @Scheduled(cron = "0 0/5 * * * ?")
    public void check5min() {
        // 迎新
        taskService.welcomeV1();
        // 新人报道
        taskService.runCheckV1();
        // 当前时间
        LocalDateTime now = LocalDateTime.now();
        // 精灵最后一次发言
        String s = RedisUtil.get("LAST:WORD");
        // 最后一次接收到聊天室消息
        String crLast = RedisUtil.get("CR:LAST:WORD");
        if (StringUtils.isBlank(crLast)) {
            crLast = DateUtil.nowStr();
        }
        // 最后一次发言
        LocalDateTime lastCR = DateUtil.parseLdt(crLast);
        // 工作日 非 宵禁时间 如果 半个小时都没收到消息 就重启一下
        if (now.getDayOfWeek().getValue() < 6) {
            if (now.toLocalTime().isAfter(Const.start) && now.toLocalTime().isBefore(Const.end) && lastCR.isBefore(now.minusMinutes(30))) {
                System.exit(0);
            }
        } else {
            // 周末三个小时
            assert lastCR != null;
            if (lastCR.isBefore(now.minusHours(3))) {
                System.exit(0);
            }
        }
        // 精灵发言
        if (StringUtils.isBlank(s)) {
            RedisUtil.set("LAST:WORD", DateUtil.nowStr());
        } else {
            // 最后一次发言
            LocalDateTime last = DateUtil.parseLdt(s);
            // 为空就用当前时间
            if (Objects.isNull(last)) {
                last = now;
            }
            // 三小时精灵没说话了, 说句话
            if (last.isBefore(now.minusHours(3))) {
                switch (new Random().nextInt(3)) {
                    case 0:
                        Fish.sendMsg("数据收集中...完成度 " + (new Random().nextInt(101)) + " %");
                        break;
                    case 1:
                        Fish.sendMsg("风起的天气, 而我在远方想你~");
                        break;
                    case 2:
                        Fish.sendMsg("智障进化中...进化失败~");
                        break;
                    default:
                        Fish.sendMsg("诶嘿嘿, 防AFK发言. 毫无意义~");
                        break;

                }
            }
        }
    }

    /**
     * 午间活动 片段雨
     */
    @Scheduled(cron = "0 30 11,17 * * ?")
    public void mcRain() {
        if (StringUtils.isBlank(RedisUtil.get(Const.CURRENCY_FREE_TIME))) {
            RedisUtil.set(Const.CURRENCY_FREE_TIME, "聊天室活动-天降鱼丸", 60);
            Fish.sendMsg("天降鱼丸, [0,10] 随机个数. 限时 1 min. 冲鸭~");
        } else {
            Fish.sendMsg("天降鱼丸开启中. 冲鸭~");
        }
    }

    /**
     * 执法服务器状态 4 小时一次
     */
    @Scheduled(cron = "0 30 0/4 * * ?")
    public void zfServerState() {
        if (StringUtils.isBlank(RedisUtil.get(Const.PATROL_LIMIT_PREFIX + "FWQZT"))) {
            Fish.sendCMD("执法 服务器状态");
        }
    }

    /**
     * 执法维护 6 小时一次
     */
    @Scheduled(cron = "30 0 0/6 * * ?")
    public void zfWeiHu() {
        if (StringUtils.isBlank(RedisUtil.get(Const.PATROL_LIMIT_PREFIX + "WH"))) {
            Fish.sendCMD("执法 维护");
        }
    }

    /**
     * 执法维护 6 小时一次
     */
    @Scheduled(cron = "0 0 0/12 * * ?")
    public void zfRefresh() {
        if (StringUtils.isBlank(RedisUtil.get(Const.PATROL_LIMIT_PREFIX + "SXHC"))) {
            Fish.sendCMD("执法 刷新缓存");
        }
    }
}
