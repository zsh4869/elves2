package online.elves.third.apis;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import online.elves.config.Const;
import online.elves.utils.RedisUtil;

/**
 * 获取指定接口的信息 小冰
 */
@Slf4j
public class IceNet {

    /**
     * 获取指定用的小冰亲密度
     */
    public static Long getUserIntimacy(String uName) {
        // 组装对象
        HttpRequest request = HttpUtil.createGet(RedisUtil.get("ICE:GAME:URI:INTIMACY") + "?user=" + uName);
        request.header("client_id", RedisUtil.get(Const.ICE_KEY));
        request.header("client_secret", RedisUtil.get(Const.ICE_SECRET));
        // 获取响应
        HttpResponse response = request.execute();
        if (response.isOk()) {
            // 反序列化响应对象
            IceResp<Intimacy> iResp = JSON.parseObject(response.body(), new TypeReference<IceResp<Intimacy>>() {
            });
            if (iResp.code == 0) {
                return iResp.data.intimacy;
            }
            log.info("查询用户小冰亲密度失败...{}", response.body());
            return 0L;
        }
        log.info("调用小冰亲密度失败...{}", JSON.toJSONString(response));
        // 不OK了就返回0
        return 0L;
    }

    /**
     * 给小冰送礼
     *
     * @param count
     * @param userName
     * @param oId
     * @param gift
     */
    public static void bribe(int count, String userName, Long oId, String gift) {
        // 组装对象
        HttpRequest request = HttpUtil.createPost(RedisUtil.get("ICE:GAME:URI:BRIBE"));
        request.header("client_id", RedisUtil.get(Const.ICE_KEY));
        request.header("client_secret", RedisUtil.get(Const.ICE_SECRET));
        // 参数
        JSONObject param = new JSONObject();
        param.put("uId", oId);
        param.put("item", gift);
        param.put("num", count);
        param.put("user", userName);
        // 设置请求参数
        request.body(param.toJSONString());
        // 获取响应
        HttpResponse response = request.execute();
        if (response.isOk()) {
            log.info("小冰送礼成功...{}", response.body());
            return;
        }
        log.info("小冰送礼失败...{}", response.body());
    }

    /**
     * 小冰用户亲密度
     */
    @Data
    public static class Intimacy {
        private String user;
        private String uId;
        private Long intimacy;
    }

    /**
     * 小冰接口通用响应
     */
    @Data
    public static class IceResp<T> {
        private int code;
        private T data;
        private String msg;
    }
}
