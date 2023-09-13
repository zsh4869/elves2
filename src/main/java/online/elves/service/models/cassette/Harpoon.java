package online.elves.service.models.cassette;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 鱼叉对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Harpoon {
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
}
