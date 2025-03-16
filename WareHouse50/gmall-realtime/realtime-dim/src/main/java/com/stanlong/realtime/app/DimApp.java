package com.stanlong.realtime.app;

import com.alibaba.fastjson.JSONObject;
import com.stanlong.bean.TableProcessDim;
import com.stanlong.constant.Constant;
import com.stanlong.realtime.function.HbaseSinkFunction;
import com.stanlong.realtime.function.TableProcessFunction;
import com.stanlong.util.FlinkSourceUtil;
import com.stanlong.util.HBaseUtil;
import com.ververica.cdc.connectors.mysql.source.MySqlSource;
import com.ververica.cdc.connectors.mysql.table.StartupOptions;
import com.ververica.cdc.debezium.JsonDebeziumDeserializationSchema;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.runtime.state.hashmap.HashMapStateBackend;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.BroadcastConnectedStream;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.util.Collector;
import org.apache.hadoop.hbase.client.Connection;

import java.io.IOException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.*;

/**
 * 维度数据处理
 * 数据处理流程
 *
 * 运行模拟生成数据的Jar包 ----》业务数据库 mysql/binlog --maxwell--> topic_db ----> Flink-DimAPP(从topic_db中读取业务数据，使用FlinkCDC读取配置文件信息，将配置信息封装为实体类对象。根据配置到hbase中建/删表，将配置信息进行广播，判断是否是维度，将维度数据同步到hbase表) ----》 维度数据 hbase
 *                                                                                   ↑ FlinkCDC
 *                                                                                mysql配置表
 */
public class DimApp {
    public static void main(String[] args) throws Exception {
        // 1、准备环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(4); // kafka topic 中有四个分区，对应设置4个并行度来消费数据

        // 1.1 检查点配置， 对状态进行持久化
        env.enableCheckpointing(5000L, CheckpointingMode.EXACTLY_ONCE); // 每隔5秒新建一个检查点，采用“精确一次”的一致性保证
        env.getCheckpointConfig().setCheckpointTimeout(10 * 6000L); // 检查点保存的超时时间，超时后没有完成保存就会被丢弃掉
        env.getCheckpointConfig().setExternalizedCheckpointCleanup(
          CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION); // 作业取消也会保存外部检查点
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(2000L); // 上一个检查点完成之后，检查点协调器最快也要等2s才可以向下一个检查点发出保存指令, 这就意味着即使已经达到了周期触发的时间点，只要距离上一个检查点完成的间隔不够，就依然不能开启下一次检查点的保存

        // 1.2 重启策略
        env.setRestartStrategy(RestartStrategies.failureRateRestart(3, Time.days(30), Time.seconds(3)));

        // 1.3 状态后端，负责状态的检查、存储及维护
        env.setStateBackend(new HashMapStateBackend()); // 哈希表状态后端，也是系统默认的一种状态后端
        env.getCheckpointConfig().setCheckpointStorage("hdfs://node02:9000/checkpoint");  // 指定检查点保存路径





        // 1.4 设置操作hadoop的用户
        System.setProperty("HADOOP_USER_NAME", "root");

        // 2、通过maxwell向kafka发送mysql配置表中的数据
        // 2.1 消费者组
        String groupId = "dim_app_group";

        // 2.2 配置Kafka连接器
        KafkaSource<String> kafkaSource = FlinkSourceUtil.getKafkaSource(groupId, Constant.TOPIC_DB);

        // 2.3 从 kafka里读取数据封装为流
        DataStreamSource<String> kafkaSourceDS = env.fromSource(kafkaSource, WatermarkStrategy.noWatermarks(), "Kafka_Source");

        // 2.4 对业务流中的数据类型进行ETL转换并进行简单的ETL  jsonStr -> jsonObj
        SingleOutputStreamOperator<JSONObject> gmallJsonObjectDS = kafkaSourceDS.process(
                new ProcessFunction<String, JSONObject>() {
                    @Override
                    public void processElement(String s, ProcessFunction<String, JSONObject>.Context context, Collector<JSONObject> collector) throws Exception {
                        JSONObject jsonObject = JSONObject.parseObject(s);
                        String db = jsonObject.getString("database");  // 启动maxwell, 通过maxwell全量读取mysql配置表的信息发送到 topic_bd
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
        // gmallJsonObjectDS.print();

        // 3 FlinkCDC 实时监控 mysql 配置表的变化
        MySqlSource<String> mysqlSource = FlinkSourceUtil.getMysqlSource("gmall_config", "table_process_dim");


        // 3.1 配置流的并行度要设置为1，避免读取配置出错
        DataStreamSource<String> mysqlSourceDS = env.fromSource(mysqlSource, WatermarkStrategy.noWatermarks(), "mysql_source")
          .setParallelism(1);
        // mysqlSourceDS.print();

        // 3.2 对配置流中的数据进行转换，转换成 TableProcessDim 实体类对象
        SingleOutputStreamOperator<TableProcessDim> tableProcessDS = mysqlSourceDS.map(
          new MapFunction<String, TableProcessDim>() {
              @Override
              public TableProcessDim map(String s) throws Exception {
                  JSONObject jsonObject = JSONObject.parseObject(s);
                  String op = jsonObject.getString("op");
                  TableProcessDim tableProcessDim = null;
                  if("d".equals(op)){ // 如果是删除操作，从before属性中获取删除前的最新操作
                      tableProcessDim = jsonObject.getObject("before", TableProcessDim.class);
                  }else { // 如果是其他操作，从after属性中获取最新配置信息
                      tableProcessDim = jsonObject.getObject("after", TableProcessDim.class);
                  }
                  tableProcessDim.setOp(op);
                  return tableProcessDim;
              }
          }
        ).setParallelism(1);

        // tableProcessDS.print();

        // 4 封装的实体类对象 TableProcessDim 保存到 Hbase
	    tableProcessDS = tableProcessDS.map(

          new RichMapFunction<TableProcessDim, TableProcessDim>() {

              private Connection hbaseCon;

              @Override
              public void open(Configuration parameters) throws Exception {
                  hbaseCon = HBaseUtil.getHBaseConnection();
              }

              @Override
              public void close() throws Exception {
                  HBaseUtil.closeHBaseConn(hbaseCon);
              }

              @Override
              public TableProcessDim map(TableProcessDim tableProcessDim) throws Exception {
                  String op = tableProcessDim.getOp();
                  String sinkTable = tableProcessDim.getSinkTable();
                  String[] sinkFamilies = tableProcessDim.getSinkFamily().split(",");
                  if("d".equals(op)){
                      // 从配置表里删除了一条数据，从hbase里对应的表也删除掉
                      HBaseUtil.dropHBaseTable(hbaseCon, Constant.HBASE_NAMESPACE, sinkTable);
                  }else if("r".equals(op) || "c".equals(op)){
                      // 从配置表里读取一条数据或者新增一条数据时，hbase中了也新建一张表
                      HBaseUtil.createHBaseTable(hbaseCon, Constant.HBASE_NAMESPACE, sinkTable, sinkFamilies);
                  }else {
                      // 对配置表的信息进行了修改，则先从 hbase中将对应的表删除掉，再创建新的表。
                      HBaseUtil.dropHBaseTable(hbaseCon, Constant.HBASE_NAMESPACE, sinkTable);
                      HBaseUtil.createHBaseTable(hbaseCon, Constant.HBASE_NAMESPACE, sinkTable, sinkFamilies);

                  }
                  return tableProcessDim;
              }
          }
        ).setParallelism(1);

	    // tableProcessDS.print();

        // 5 把配置流广播出去
        MapStateDescriptor<String, TableProcessDim> mapStateDescriptor = new MapStateDescriptor<>("mapStateDescriptor", String.class, TableProcessDim.class);
        BroadcastStream<TableProcessDim> gmallConfigBroadcast = tableProcessDS.broadcast(mapStateDescriptor);

        // 6 主流和配置流进行连接
        BroadcastConnectedStream<JSONObject, TableProcessDim> connectDS = gmallJsonObjectDS.connect(gmallConfigBroadcast);

        SingleOutputStreamOperator<Tuple2<JSONObject, TableProcessDim>> dimDS = connectDS.process(
          new TableProcessFunction(mapStateDescriptor)
        );

        dimDS.addSink(new HbaseSinkFunction());

        env.execute();
    }


}
