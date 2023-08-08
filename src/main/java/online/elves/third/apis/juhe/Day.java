package online.elves.third.apis.juhe;

import lombok.Data;

import java.util.List;

/**
 * 聚合数据 今日老黄历
 */
@Data
public class Day {
    /**
     * 请求结果 Success Fail
     */
    private String reason;
    /**
     * 结果对象
     */
    private Result result;
    /**
     * 异常编码
     */
    private Integer error_code;

    /**
     * 结果对象
     */
    @Data
    public static class Result {
        /**
         * 数据对象
         */
        private Dt data;
    }

    /**
     * 数据对象
     */
    @Data
    public static class Dt {
        /**
         * 属相
         */
        private String animalsYear;
        /**
         * 周几
         */
        private String weekday;
        /**
         * 纪年
         */
        private String lunarYear;
        /**
         * 农历月日
         */
        private String lunar;
        /**
         * 具体日期
         */
        private String date;
        /**
         * 宜
         */
        private String suit;
        /**
         * 忌
         */
        private String avoid;
        /**
         * 假日
         */
        private String holiday;
        /**
         * 假日描述
         */
        private String desc;
    }

}
