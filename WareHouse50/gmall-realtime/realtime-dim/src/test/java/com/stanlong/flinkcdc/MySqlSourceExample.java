package com.stanlong.flinkcdc;

import com.ververica.cdc.connectors.mysql.source.MySqlSource;
import com.ververica.cdc.connectors.mysql.table.StartupOptions;
import com.ververica.cdc.debezium.JsonDebeziumDeserializationSchema;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * FlinkCDC 官方Demo测试
 * 准备工作，开启mysql的binlog日志
 * 然后在数据库建一张测试表 gmall_config：CREATE DATABASE IF NOT EXISTS gmall_config default charset utf8 COLLATE utf8_general_ci;
 * 把这张表加到 binlog 的监控中 binlog-do-db=gmall_config : sed -i '/ binlog-do-db/a binlog-do-db=gmall_config' my.cnf , 修改完成之后要重启数据库服务
 * 在库里新建一张测试表  t_user, 插入两条测试数据，启动程序观察现象
 * create table t_user(id int PRIMARY KEY, name varchar(32) , age int); // 测试CDC需要指定mysql主键， 不然会报错： 'scan.incremental.snapshot.chunk.key-column' must be set when the table doesn't have primary keys
 * insert into t_user(id, name, age) value (1, "zhangsan", 20);
 * insert into t_user(id, name, age) value (2, "lisi", 30);
 */
public class MySqlSourceExample {
    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        MySqlSource<String> mySqlSource = MySqlSource.<String>builder()
                .hostname("node01")
                .port(3306)
                .databaseList("gmall_config") // set captured database
                .tableList("gmall_config.t_user") // set captured table
                .username("root")
                .password("root")
                /**
                 * startupOptions 有三个选项
                 * initial：因为binlog中不一定包含所有的数据，那么需要全表扫描所有的表，形成快照。常用于历史数据， 这个是默认的
                 * earliest：从最早的变更日志开始读取（仅增量，忽略全量数据）。
                 * latest：读binlog中最新的数据 ，常用实时表
                 */
                .startupOptions(StartupOptions.initial())
                .deserializer(new JsonDebeziumDeserializationSchema()) // converts SourceRecord to JSON String
                .build();

        // enable checkpoint
        env.enableCheckpointing(3000);

        env.fromSource(mySqlSource, WatermarkStrategy.noWatermarks(), "MySQL Source").print();

        env.execute("Print MySQL Snapshot + Binlog");
    }
}
