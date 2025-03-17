package com.stanlong.realtime.app;

import com.alibaba.fastjson.JSONObject;
import com.stanlong.base.BaseApp;
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
public class DimApp extends BaseApp {
    public static void main(String[] args) throws Exception {
        new DimApp().start(10001, 4, "dim_app", Constant.TOPIC_DB);
    }

    @Override
    public void handle(StreamExecutionEnvironment env, DataStreamSource<String> kafkaSourceDS) {
        // 2.4 对业务流中的数据类型进行ETL转换并进行简单的ETL  jsonStr -> jsonObj
        SingleOutputStreamOperator<JSONObject> gmallJsonObjectDS = etl(kafkaSourceDS);
        // gmallJsonObjectDS.print();

        SingleOutputStreamOperator<TableProcessDim> tableProcessDS = readTableProcess(env);

        // tableProcessDS.print();

        // 4 封装的实体类对象 TableProcessDim 保存到 Hbase
        tableProcessDS = createHbaseTable(tableProcessDS);

        // tableProcessDS.print();

        // 5 把配置流广播出去
        SingleOutputStreamOperator<Tuple2<JSONObject, TableProcessDim>> dimDS = connect(tableProcessDS, gmallJsonObjectDS);

        dimDS.addSink(new HbaseSinkFunction());
    }

    private static SingleOutputStreamOperator<Tuple2<JSONObject, TableProcessDim>> connect(SingleOutputStreamOperator<TableProcessDim> tableProcessDS, SingleOutputStreamOperator<JSONObject> gmallJsonObjectDS) {
        MapStateDescriptor<String, TableProcessDim> mapStateDescriptor = new MapStateDescriptor<>("mapStateDescriptor", String.class, TableProcessDim.class);
        BroadcastStream<TableProcessDim> gmallConfigBroadcast = tableProcessDS.broadcast(mapStateDescriptor);

        // 6 主流和配置流进行连接
        BroadcastConnectedStream<JSONObject, TableProcessDim> connectDS = gmallJsonObjectDS.connect(gmallConfigBroadcast);

        SingleOutputStreamOperator<Tuple2<JSONObject, TableProcessDim>> dimDS = connectDS.process(
          new TableProcessFunction(mapStateDescriptor)
        );
        return dimDS;
    }

    private static SingleOutputStreamOperator<TableProcessDim> createHbaseTable(SingleOutputStreamOperator<TableProcessDim> tableProcessDS) {
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
        return tableProcessDS;
    }

    private static SingleOutputStreamOperator<TableProcessDim> readTableProcess(StreamExecutionEnvironment env) {
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
        return tableProcessDS;
    }

    private static SingleOutputStreamOperator<JSONObject> etl(DataStreamSource<String> kafkaSourceDS) {
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
        return gmallJsonObjectDS;
    }
}
