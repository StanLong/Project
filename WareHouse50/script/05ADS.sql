DROP TABLE IF EXISTS ads_traffic_stats_by_channel;
CREATE EXTERNAL TABLE ads_traffic_stats_by_channel
(
    `dt`               STRING COMMENT '统计日期',
    `recent_days`      BIGINT COMMENT '最近天数,1:最近1天,7:最近7天,30:最近30天',
    `channel`          STRING COMMENT '渠道',
    `uv_count`         BIGINT COMMENT '访客人数',
    `avg_duration_sec` BIGINT COMMENT '会话平均停留时长，单位为秒',
    `avg_page_count`   BIGINT COMMENT '会话平均浏览页面数',
    `sv_count`         BIGINT COMMENT '会话数',
    `bounce_rate`      DECIMAL(16, 2) COMMENT '跳出率'
) COMMENT '各渠道流量统计'
    ROW FORMAT DELIMITED FIELDS TERMINATED BY '\t'
    LOCATION '/warehouse/gmall/ads/ads_traffic_stats_by_channel/';

 
DROP TABLE IF EXISTS ads_page_path;
CREATE EXTERNAL TABLE ads_page_path
(
    `dt`          STRING COMMENT '统计日期',
    `recent_days` BIGINT COMMENT '最近天数,1:最近1天,7:最近7天,30:最近30天',
    `source`      STRING COMMENT '跳转起始页面ID',
    `target`      STRING COMMENT '跳转终到页面ID',
    `path_count`  BIGINT COMMENT '跳转次数'
) COMMENT '页面浏览路径分析'
    ROW FORMAT DELIMITED FIELDS TERMINATED BY '\t'
    LOCATION '/warehouse/gmall/ads/ads_page_path/';

DROP TABLE IF EXISTS ads_user_change;
CREATE EXTERNAL TABLE ads_user_change
(
    `dt`               STRING COMMENT '统计日期',
    `user_churn_count` BIGINT COMMENT '流失用户数',
    `user_back_count`  BIGINT COMMENT '回流用户数'
) COMMENT '用户变动统计'
    ROW FORMAT DELIMITED FIELDS TERMINATED BY '\t'
    LOCATION '/warehouse/gmall/ads/ads_user_change/';

 
DROP TABLE IF EXISTS ads_user_retention;
CREATE EXTERNAL TABLE ads_user_retention
(
    `dt`              STRING COMMENT '统计日期',
    `create_date`     STRING COMMENT '用户新增日期',
    `retention_day`   INT COMMENT '截至当前日期留存天数',
    `retention_count` BIGINT COMMENT '留存用户数量',
    `new_user_count`  BIGINT COMMENT '新增用户数量',
    `retention_rate`  DECIMAL(16, 2) COMMENT '留存率'
) COMMENT '用户留存率'
    ROW FORMAT DELIMITED FIELDS TERMINATED BY '\t'
    LOCATION '/warehouse/gmall/ads/ads_user_retention/';

DROP TABLE IF EXISTS ads_user_stats;
CREATE EXTERNAL TABLE ads_user_stats
(
    `dt`                STRING COMMENT '统计日期',
    `recent_days`       BIGINT COMMENT '最近n日,1:最近1日,7:最近7日,30:最近30日',
    `new_user_count`    BIGINT COMMENT '新增用户数',
    `active_user_count` BIGINT COMMENT '活跃用户数'
) COMMENT '用户新增活跃统计'
    ROW FORMAT DELIMITED FIELDS TERMINATED BY '\t'
    LOCATION '/warehouse/gmall/ads/ads_user_stats/';

DROP TABLE IF EXISTS ads_user_action;
CREATE EXTERNAL TABLE ads_user_action
(
    `dt`                STRING COMMENT '统计日期',
    `recent_days`       BIGINT COMMENT '最近天数,1:最近1天,7:最近7天,30:最近30天',
    `home_count`        BIGINT COMMENT '浏览首页人数',
    `good_detail_count` BIGINT COMMENT '浏览商品详情页人数',
    `cart_count`        BIGINT COMMENT '加入购物车人数',
    `order_count`       BIGINT COMMENT '下单人数',
    `payment_count`     BIGINT COMMENT '支付人数'
) COMMENT '漏斗分析'
    ROW FORMAT DELIMITED FIELDS TERMINATED BY '\t'
    LOCATION '/warehouse/gmall/ads/ads_user_action/';

DROP TABLE IF EXISTS ads_new_buyer_stats;
CREATE EXTERNAL TABLE ads_new_buyer_stats
(
    `dt`                     STRING COMMENT '统计日期',
    `recent_days`            BIGINT COMMENT '最近天数,1:最近1天,7:最近7天,30:最近30天',
    `new_order_user_count`   BIGINT COMMENT '新增下单人数',
    `new_payment_user_count` BIGINT COMMENT '新增支付人数'
) COMMENT '新增交易用户统计'
    ROW FORMAT DELIMITED FIELDS TERMINATED BY '\t'
    LOCATION '/warehouse/gmall/ads/ads_new_buyer_stats/';

DROP TABLE IF EXISTS ads_repeat_purchase_by_tm;
CREATE EXTERNAL TABLE ads_repeat_purchase_by_tm
(
    `dt`                STRING COMMENT '统计日期',
    `recent_days`       BIGINT COMMENT '最近天数,7:最近7天,30:最近30天',
    `tm_id`             STRING COMMENT '品牌ID',
    `tm_name`           STRING COMMENT '品牌名称',
    `order_repeat_rate` DECIMAL(16, 2) COMMENT '复购率'
) COMMENT '各品牌复购率统计'
    ROW FORMAT DELIMITED FIELDS TERMINATED BY '\t'
    LOCATION '/warehouse/gmall/ads/ads_repeat_purchase_by_tm/';

DROP TABLE IF EXISTS ads_trade_stats_by_tm;
CREATE EXTERNAL TABLE ads_trade_stats_by_tm
(
    `dt`                      STRING COMMENT '统计日期',
    `recent_days`             BIGINT COMMENT '最近天数,1:最近1天,7:最近7天,30:最近30天',
    `tm_id`                   STRING COMMENT '品牌ID',
    `tm_name`                 STRING COMMENT '品牌名称',
    `order_count`             BIGINT COMMENT '订单数',
    `order_user_count`        BIGINT COMMENT '订单人数',
    `order_refund_count`      BIGINT COMMENT '退单数',
    `order_refund_user_count` BIGINT COMMENT '退单人数'
) COMMENT '各品牌商品交易统计'
    ROW FORMAT DELIMITED FIELDS TERMINATED BY '\t'
    LOCATION '/warehouse/gmall/ads/ads_trade_stats_by_tm/';

DROP TABLE IF EXISTS ads_trade_stats_by_cate;
CREATE EXTERNAL TABLE ads_trade_stats_by_cate
(
    `dt`                      STRING COMMENT '统计日期',
    `recent_days`             BIGINT COMMENT '最近天数,1:最近1天,7:最近7天,30:最近30天',
    `category1_id`            STRING COMMENT '一级分类id',
    `category1_name`          STRING COMMENT '一级分类名称',
    `category2_id`            STRING COMMENT '二级分类id',
    `category2_name`          STRING COMMENT '二级分类名称',
    `category3_id`            STRING COMMENT '三级分类id',
    `category3_name`          STRING COMMENT '三级分类名称',
    `order_count`             BIGINT COMMENT '订单数',
    `order_user_count`        BIGINT COMMENT '订单人数',
    `order_refund_count`      BIGINT COMMENT '退单数',
    `order_refund_user_count` BIGINT COMMENT '退单人数'
) COMMENT '各分类商品交易统计'
    ROW FORMAT DELIMITED FIELDS TERMINATED BY '\t'
    LOCATION '/warehouse/gmall/ads/ads_trade_stats_by_cate/';

DROP TABLE IF EXISTS ads_sku_cart_num_top3_by_cate;
CREATE EXTERNAL TABLE ads_sku_cart_num_top3_by_cate
(
    `dt`             STRING COMMENT '统计日期',
    `category1_id`   STRING COMMENT '一级分类ID',
    `category1_name` STRING COMMENT '一级分类名称',
    `category2_id`   STRING COMMENT '二级分类ID',
    `category2_name` STRING COMMENT '二级分类名称',
    `category3_id`   STRING COMMENT '三级分类ID',
    `category3_name` STRING COMMENT '三级分类名称',
    `sku_id`         STRING COMMENT '商品id',
    `sku_name`       STRING COMMENT '商品名称',
    `cart_num`       BIGINT COMMENT '购物车中商品数量',
    `rk`             BIGINT COMMENT '排名'
) COMMENT '各分类商品购物车存量Top10'
    ROW FORMAT DELIMITED FIELDS TERMINATED BY '\t'
    LOCATION '/warehouse/gmall/ads/ads_sku_cart_num_top3_by_cate/';

DROP TABLE IF EXISTS ads_trade_stats;
CREATE EXTERNAL TABLE ads_trade_stats
(
    `dt`                      STRING COMMENT '统计日期',
    `recent_days`             BIGINT COMMENT '最近天数,1:最近1日,7:最近7天,30:最近30天',
    `order_total_amount`      DECIMAL(16, 2) COMMENT '订单总额,GMV',
    `order_count`             BIGINT COMMENT '订单数',
    `order_user_count`        BIGINT COMMENT '下单人数',
    `order_refund_count`      BIGINT COMMENT '退单数',
    `order_refund_user_count` BIGINT COMMENT '退单人数'
) COMMENT '交易统计'
    ROW FORMAT DELIMITED FIELDS TERMINATED BY '\t'
    LOCATION '/warehouse/gmall/ads/ads_trade_stats/';

DROP TABLE IF EXISTS ads_order_by_province;
CREATE EXTERNAL TABLE ads_order_by_province
(
    `dt`                 STRING COMMENT '统计日期',
    `recent_days`        BIGINT COMMENT '最近天数,1:最近1天,7:最近7天,30:最近30天',
    `province_id`        STRING COMMENT '省份ID',
    `province_name`      STRING COMMENT '省份名称',
    `area_code`          STRING COMMENT '地区编码',
    `iso_code`           STRING COMMENT '国际标准地区编码',
    `iso_code_3166_2`    STRING COMMENT '国际标准地区编码',
    `order_count`        BIGINT COMMENT '订单数',
    `order_total_amount` DECIMAL(16, 2) COMMENT '订单金额'
) COMMENT '各地区订单统计'
    ROW FORMAT DELIMITED FIELDS TERMINATED BY '\t'
    LOCATION '/warehouse/gmall/ads/ads_order_by_province/';

DROP TABLE IF EXISTS ads_coupon_stats;
CREATE EXTERNAL TABLE ads_coupon_stats
(
    `dt`          STRING COMMENT '统计日期',
    `coupon_id`   STRING COMMENT '优惠券ID',
    `coupon_name` STRING COMMENT '优惠券名称',
    `start_date`  STRING COMMENT '发布日期',
    `rule_name`   STRING COMMENT '优惠规则，例如满100元减10元',
    `reduce_rate` DECIMAL(16, 2) COMMENT '补贴率'
) COMMENT '优惠券统计'
    ROW FORMAT DELIMITED FIELDS TERMINATED BY '\t'
    LOCATION '/warehouse/gmall/ads/ads_coupon_stats/';

DROP TABLE IF EXISTS ads_activity_stats;
CREATE EXTERNAL TABLE ads_activity_stats
(
    `dt`            STRING COMMENT '统计日期',
    `activity_id`   STRING COMMENT '活动ID',
    `activity_name` STRING COMMENT '活动名称',
    `start_date`    STRING COMMENT '活动开始日期',
    `reduce_rate`   DECIMAL(16, 2) COMMENT '补贴率'
) COMMENT '活动统计'
    ROW FORMAT DELIMITED FIELDS TERMINATED BY '\t'
    LOCATION '/warehouse/gmall/ads/ads_activity_stats/';