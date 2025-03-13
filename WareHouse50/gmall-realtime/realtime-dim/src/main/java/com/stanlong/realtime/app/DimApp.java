package com.stanlong.realtime.app;

import com.alibaba.fastjson.JSONObject;
import com.stanlong.bean.TableProcessDim;
import com.stanlong.constant.Constant;
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
import org.apache.flink.util.Collector;
import org.apache.hadoop.hbase.client.Connection;

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

        // 检查点配置， 对状态进行持久化
        env.enableCheckpointing(5000L, CheckpointingMode.EXACTLY_ONCE); // 每隔5秒新建一个检查点，采用“精确一次”的一致性保证
        env.getCheckpointConfig().setCheckpointTimeout(10 * 6000L); // 检查点保存的超时时间，超时后没有完成保存就会被丢弃掉
        env.getCheckpointConfig().setExternalizedCheckpointCleanup(
          CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION); // 作业取消也会保存外部检查点
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(2000L); // 上一个检查点完成之后，检查点协调器最快也要等2s才可以向下一个检查点发出保存指令, 这就意味着即使已经达到了周期触发的时间点，只要距离上一个检查点完成的间隔不够，就依然不能开启下一次检查点的保存

        // 重启策略
        env.setRestartStrategy(RestartStrategies.failureRateRestart(3, Time.days(30), Time.seconds(3)));

        // 状态后端，负责状态的检查、存储及维护
        env.setStateBackend(new HashMapStateBackend()); // 哈希表状态后端，也是系统默认的一种状态后端
        env.getCheckpointConfig().setCheckpointStorage("hdfs://node01:9000/checkpoint");  // 指定检查点保存路径

        // 设置操作hadoop的用户
        System.setProperty("HADOOP_USER_NAME", "root");

        // 消费者组
        String groupId = "dim_app_group";

        // 配置Kafka连接器
        KafkaSource<String> kafkaSource = KafkaSource.<String>builder()
                .setBootstrapServers(Constant.KAFKA_BROKERS)
                .setTopics(Constant.TOPIC_DB)
                .setGroupId(groupId)
                .setStartingOffsets(OffsetsInitializer.latest())
                // 使用 SimpleStringSchema 进行反序列化，如果消息为空，会报错。
                // .setDeserializer(new SimpleStringSchema())
                // 这里手动实现反序列化
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
        jsonObjectDS.print();

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

        // 配置流的并行度要设置为1，避免读取配置出错
        DataStreamSource<String> mysqlSourceDS = env.fromSource(mysqlSource, WatermarkStrategy.noWatermarks(), "mysql_source")
          .setParallelism(1);
        // mysqlSourceDS.print();

        // 对配置流中的数据进行转换，转换成实体类对象
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

        // 把配置流广播出去
        MapStateDescriptor<String, TableProcessDim> mapStateDescriptor = new MapStateDescriptor<>("mapStateDescriptor", String.class, TableProcessDim.class);
        BroadcastStream<TableProcessDim> broadcast = tableProcessDS.broadcast(mapStateDescriptor);

        // 主流和配置流进行连接
        BroadcastConnectedStream<JSONObject, TableProcessDim> connectDS = jsonObjectDS.connect(broadcast);

        SingleOutputStreamOperator<Tuple2<JSONObject, TableProcessDim>> dimDS = connectDS.process(
          new BroadcastProcessFunction<JSONObject, TableProcessDim, Tuple2<JSONObject, TableProcessDim> >() {

              @Override
              public void processElement(JSONObject jsonObject,
                                         BroadcastProcessFunction<JSONObject, TableProcessDim, Tuple2<JSONObject, TableProcessDim>>.ReadOnlyContext readOnlyContext,
                                         Collector<Tuple2<JSONObject, TableProcessDim>> collector) throws Exception {
                  // 获取处理数据的表名
                  String table = jsonObject.getString("table");
                  // 获取广播状态
                  ReadOnlyBroadcastState<String, TableProcessDim> broadcastState = readOnlyContext.getBroadcastState(mapStateDescriptor);
                  // 根据表名到广播状态中获取对应的配置信息
                  TableProcessDim tableProcessDim = broadcastState.get(table);
                  if(tableProcessDim  != null){
                      // 如果根据表名获取到了对应的配置信息，说明当前处理的是维度数据，将维度数据继续向下游传递
                      JSONObject dataJSONObject = jsonObject.getJSONObject("data");
                      // 在向下游传递数据前，补充对维度数据的操作类型
                      String type = jsonObject.getString("type");
                      dataJSONObject.put("type", type);
                      collector.collect(Tuple2.of(dataJSONObject, tableProcessDim));
                  }

              }

              // 处理广播流配置信息，将配置数据放到广播状态或者从广播状态中删除对应的配置
              @Override
              public void processBroadcastElement(TableProcessDim tableProcessDim,
                                                  BroadcastProcessFunction<JSONObject, TableProcessDim, Tuple2<JSONObject, TableProcessDim>>.Context context,
                                                  Collector<Tuple2<JSONObject, TableProcessDim>> collector) throws Exception {
                  // 获取对配置表的操作类型
                  String op = tableProcessDim.getOp();
                  BroadcastState<String, TableProcessDim> broadcastState = context.getBroadcastState(mapStateDescriptor);
                  String sourceTable = tableProcessDim.getSourceTable();
                  if("d".equals(op)){
                      // 从配置表中删除一条信息，则从广播状态中也删除一条信息
                      broadcastState.remove(sourceTable);
                  }else {
                      // 对配置表进行了读取，添加或者更新，将最新的配置信息放到广播状态中
                      broadcastState.put(sourceTable, tableProcessDim);
                  }

              }
          }
        );

        // dimDS.print();


        env.execute();
    }
}
