package online.elves.third.apis.juhe;


import lombok.Data;

import java.util.List;

/**
 * 假日日期
 */
@Data
public class HolidayData {
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
         * 年份
         */
        private String year;
        /**
         * 假日列表
         */
        private String holidaylist;
        /**
         * 假日列表对象
         */
        private List<Holiday> holiday_list;
    }

    @Data
    public static class Holiday {
        /**
         * 假日开始时间
         */
        private String startday;
        /**
         * 假日名字
         */
        private String name;
    }
}
