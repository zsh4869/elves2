package online.elves.third.apis.juhe;

import lombok.Data;

import java.util.List;

/**
 * 聚合 头条新闻
 */
@Data
public class TopNews {
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
         * 状态
         */
        private String stat;
        /**
         * 新闻列表
         */
        private List<TopNew> data;
        /**
         * 页码
         */
        private String page;
        /**
         * 分页步长
         */
        private String pageSize;
    }

    /**
     * 新闻详情
     */
    @Data
    public static class TopNew {
        /**
         * 新闻唯一键
         */
        private String uniquekey;
        /**
         * 新闻标题
         */
        private String title;
        /**
         * 新闻时间
         */
        private String date;
        /**
         * 新闻分类
         */
        private String category;
        /**
         * 新闻作者
         */
        private String author_name;
        /**
         * 新闻地址
         */
        private String url;
        private String thumbnail_pic_s;
        private String thumbnail_pic_s02;
        private String thumbnail_pic_s03;
        /**
         * 是否有详细内容
         */
        private String is_content;
    }
}
