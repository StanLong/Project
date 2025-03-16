package com.stanlong.util;

import com.stanlong.constant.Constant;
import com.ververica.cdc.connectors.mysql.source.MySqlSource;
import com.ververica.cdc.connectors.mysql.table.StartupOptions;
import com.ververica.cdc.debezium.JsonDebeziumDeserializationSchema;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;


public class FlinkSourceUtil {

	// 获取KafkaSource
	public static KafkaSource<String> getKafkaSource(String groupId,  String topic) {
		return KafkaSource.<String>builder()
		  .setBootstrapServers(Constant.KAFKA_BROKERS)
		  .setGroupId(groupId)
		  .setTopics(topic)
		  .setStartingOffsets(OffsetsInitializer.latest())
		  .setValueOnlyDeserializer(new DeserializationSchema<String>() {
			  @Override
			  public String deserialize(byte[] message) throws IOException {
				  if (message != null) {
					  return new String(message, StandardCharsets.UTF_8);
				  }
				  return null;
			  }

			  @Override
			  public boolean isEndOfStream(String nextElement) {
				  return false;
			  }

			  @Override
			  public TypeInformation<String> getProducedType() {
				  return Types.STRING;
			  }
		  }).build();
	}

	// 获取 mysqlsource
	public static MySqlSource<String> getMysqlSource(String database, String tableName){
		Properties props = new Properties();
		props.setProperty("useSSL", "false");
		props.setProperty("allowPublicKeyRetrieval", "true");

		return MySqlSource.<String>builder()
		  .hostname(Constant.MYSQL_HOST)
		  .port(Constant.MYSQL_PORT)
		  .databaseList(database)
		  .tableList(database+"."+tableName)
		  .username(Constant.MYSQL_USER_NAME)
		  .password(Constant.MYSQL_PASSWORD)
		  .deserializer(new JsonDebeziumDeserializationSchema())
		  .startupOptions(StartupOptions.initial())
		  .jdbcProperties(props)
		  .build();
	}

}
