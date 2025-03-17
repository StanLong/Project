package com.stanlong.base;

import com.stanlong.constant.Constant;
import com.stanlong.util.FlinkSourceUtil;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.runtime.state.hashmap.HashMapStateBackend;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import static org.apache.flink.streaming.api.environment.CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION;

public abstract  class BaseApp {

	public abstract void handle(StreamExecutionEnvironment env, DataStreamSource<String> stream);

	public void start(int port, int parallelism, String ckAndGroupId, String topic) {
		// 1、准备环境
		Configuration conf = new Configuration();
		conf.setInteger("rest.port", port);
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment(conf);
		env.setParallelism(parallelism); // kafka topic 中有四个分区，对应设置4个并行度来消费数据

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
		env.getCheckpointConfig().setCheckpointStorage("hdfs://node02:9000/checkpoint" + ckAndGroupId);  // 指定检查点保存路径

		// 1.4 设置操作hadoop的用户
		System.setProperty("HADOOP_USER_NAME", "root");

		// 2.2 配置Kafka连接器
		KafkaSource<String> kafkaSource = FlinkSourceUtil.getKafkaSource(ckAndGroupId, topic);

		// 2.3 从 kafka里读取数据封装为流
		DataStreamSource<String> kafkaSourceDS = env.fromSource(kafkaSource, WatermarkStrategy.noWatermarks(), "Kafka_Source");

		// 2. 执行具体的处理逻辑
		handle(env, kafkaSourceDS);

		// 3. 执行 Job
		try {
			env.execute();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
