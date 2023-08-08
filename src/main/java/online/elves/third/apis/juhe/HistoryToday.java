package online.elves.third.apis.juhe;

import lombok.Data;

import java.util.List;

/**
 * 历史上的今天
 */
@Data
public class HistoryToday {
    /**
     * 接口状况
     */
    private String reason;
    /**
     * 响应编码
     */
    private Integer error_code;
    /**
     * 历史上的今天列表
     */
    private List<History> result;

    /**
     * 历史
     */
    @Data
    public static class History {
        /**
         * 日期
         */
        private String day;
        /**
         * 年月日
         */
        private String date;
        /**
         * 描述
         */
        private String title;
        /**
         * 排序
         */
        private String e_id;
    }
}
