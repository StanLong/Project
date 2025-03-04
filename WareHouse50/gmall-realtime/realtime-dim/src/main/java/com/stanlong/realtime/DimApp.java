package com.stanlong.realtime;

import com.alibaba.fastjson.JSONObject;
import com.stanlong.constant.Constant;
import com.ververica.cdc.connectors.mysql.source.MySqlSource;
import com.ververica.cdc.connectors.mysql.table.StartupOptions;
import com.ververica.cdc.debezium.JsonDebeziumDeserializationSchema;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.runtime.state.hashmap.HashMapStateBackend;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;

import java.io.IOException;
import java.util.Properties;

/**
 * 维度数据处理
 */
public class DimApp {
    public static void main(String[] args) throws Exception {
        // 准备环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(4); // kafka topic 中有四个分区，对应设置4个并行度来消费数据

        // 检查点配置
        env.enableCheckpointing(5000L, CheckpointingMode.EXACTLY_ONCE); // 检查点配置，精确一次
        env.getCheckpointConfig().setCheckpointTimeout(10 * 6000L);
        env.getCheckpointConfig().setExternalizedCheckpointCleanup(CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(2000L);
        env.getCheckpointConfig().setCheckpointStorage("hdfs://node01:9000/checkpoint");

        // 重启策略
        env.setRestartStrategy(RestartStrategies.failureRateRestart(3, Time.days(30), Time.seconds(3)));

        // 状态后端
        env.setStateBackend(new HashMapStateBackend());

        // 设置操作hadoop的用户
        System.setProperty("HADOOP_USER_NAME", "root");

        // 配置Kafka连接器
        String groupId = "dim_app_group";

        KafkaSource<String> kafkaSource = KafkaSource.<String>builder()
                .setBootstrapServers(Constant.KAFKA_BROKERS)
                .setTopics(Constant.TOPIC_DB)
                .setGroupId(groupId)
                .setStartingOffsets(OffsetsInitializer.latest())
                // 使用 SimpleStringSchema 进行反序列化，如果消息为空，会报错
                // .setDeserializer(new SimpleStringSchema())
                .setValueOnlyDeserializer(
                        new DeserializationSchema<String>() { // 这里手动实现反序列化
                            @Override
                            public String deserialize(byte[] bytes) throws IOException {
                                if(bytes != null){
                                    return new String(bytes);
                                }
                                return "";
                            }

                            @Override
                            public boolean isEndOfStream(String s) {
                                return false;
                            }

                            @Override
                            public TypeInformation<String> getProducedType() {
                                return TypeInformation.of(String.class);
                            }
                        }
                ).build();

        // 从 kafka里读取数据封装为流
        DataStreamSource<String> kafkaSourceDS = env.fromSource(kafkaSource, WatermarkStrategy.noWatermarks(), "Kafka_Source");

        SingleOutputStreamOperator<JSONObject> jsonObjectDS = kafkaSourceDS.process(
                new ProcessFunction<String, JSONObject>() {
                    @Override
                    public void processElement(String s, ProcessFunction<String, JSONObject>.Context context, Collector<JSONObject> collector) throws Exception {
                        JSONObject jsonObject = JSONObject.parseObject(s);
                        String db = jsonObject.getString("database");
                        String type = jsonObject.getString("type");
                        String data = jsonObject.getString("data");
                        if ("gmall".equals(db)
                                && ("insert".equals(type)
                                || "update".equals(type)
                                || "delete".equals(type)
                                || "bootstrap-insert".equals(type))
                                && data != null
                                && data.length() > 2
                        ) {
                            collector.collect(jsonObject);
                        }

                    }
                }
        );

        // jsonObjectDS.print();

        // FlinkCDC 实时监控mysql数据源的变化
        Properties props = new Properties();
        props.setProperty("useSSL", "false");
        props.setProperty("allowPublicKeyRetrieval", "true");

        MySqlSource<String> mysqlSource = MySqlSource.<String>builder()
          .hostname(Constant.MYSQL_HOST)
          .port(Constant.MYSQL_PORT)
          .databaseList("gmall_config")
          .tableList("gmall_config.table_process_dim")
          .username(Constant.MYSQL_USER_NAME)
          .password(Constant.MYSQL_PASSWORD)
          .deserializer(new JsonDebeziumDeserializationSchema())
          .startupOptions(StartupOptions.initial())
          .jdbcProperties(props)
          .build();

        DataStreamSource<String> mysqlSourceDS = env.fromSource(mysqlSource, WatermarkStrategy.noWatermarks(), "mysql_source");
        // mysqlSourceDS.print();



        env.execute();
    }
}
