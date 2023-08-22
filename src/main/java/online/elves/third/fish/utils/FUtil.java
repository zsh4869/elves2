package online.elves.third.fish.utils;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import online.elves.third.fish.model.FResp;

/**
 * 网络工具类
 */
@Slf4j
public class FUtil {
    
    public static final String UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.93 Safari/537.36 Edg/90.0.818.56";
    
    /**
     * get 请求 特殊请求
     * @param uri
     * @param key
     * @return
     */
    public static String getSpec(String uri, String key) {
        // 获取返回信息
        HttpResponse response = null;
        try {
            response = HttpRequest.get(uri + key).header("User-Agent", UA).execute();
            // 返回对象
            return response.body();
        } catch (Exception e) {
            log.warn("{} some request get spec error...{} ===> {}", uri, e.getMessage(), JSON.toJSONString(response));
            // 怀疑坐牢了...直接自杀吧
            System.exit(0);
        }
        return null;
    }

    /**
     * post 请求
     * @param uri
     * @param key
     * @return
     */
    public static String postSpec(String uri, String key, String body) {
        // 获取返回信息
        HttpResponse response = null;
        try {
            response = HttpRequest.post(uri + key).header("User-Agent", UA).body(body).execute();
            // 返回对象
            return response.body();
        } catch (Exception e) {
            log.warn("{} some request post spec error...{} ===> {}", uri, e.getMessage(), JSON.toJSONString(response));
            // 怀疑坐牢了...直接自杀吧
            System.exit(0);
        }
        return null;
    }

    /**
     * get 请求
     * @param uri
     * @param key
     * @return
     */
    public static FResp get(String uri, String key) {
        // 获取返回信息
        HttpResponse response = null;
        try {
            response = HttpRequest.get(uri + key).header("User-Agent", UA).execute();
            // 返回对象
            return JSON.parseObject(response.body(), FResp.class);
        } catch (Exception e) {
            log.warn("{} some request get error...{} ===> {}", uri, e.getMessage(), JSON.toJSONString(response));
            // 怀疑坐牢了...直接自杀吧
            System.exit(0);
        }
        return new FResp();
    }
    
    /**
     * post 请求
     * @param uri
     * @param key
     * @return
     */
    public static FResp post(String uri, String key, String body) {
        // 获取返回信息
        HttpResponse response = null;
        try {
            response = HttpRequest.post(uri + key).header("User-Agent", UA).body(body).execute();
            // 返回对象
            return JSON.parseObject(response.body(), FResp.class);
        } catch (Exception e) {
            log.warn("{} some request post error...{} ===> {}", uri, e.getMessage(), JSON.toJSONString(response));
            // 怀疑坐牢了...直接自杀吧
            System.exit(0);
        }
        return new FResp();
    }
    
}
