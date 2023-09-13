package online.elves.service.models.cassette;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;

/**
 * 渔网对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Fishnet {
    /**
     * 品质
     * 0 史诗 橙色
     * 1 稀有 紫色
     * 2 普通 白色
     */
    private int quality;
    /**
     * 颜色名字
     */
    private String name;
    /**
     * 生效时间
     */
    private LocalDateTime actTime;
    /**
     * 失效时间
     */
    private LocalDateTime expTime;

    /**
     * 构建对象
     *
     * @param redisCont
     */
    public Fishnet(String redisCont) {
        if (StringUtils.isBlank(redisCont)) {
            this.quality = -1;
        } else {
            Fishnet fishnet = JSON.parseObject(redisCont, Fishnet.class);
            this.quality = fishnet.quality;
            this.name = fishnet.name;
            this.actTime = fishnet.actTime;
            this.expTime = fishnet.expTime;
        }
    }
}
