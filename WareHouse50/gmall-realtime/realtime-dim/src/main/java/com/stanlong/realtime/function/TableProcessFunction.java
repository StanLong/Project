package com.stanlong.realtime.function;

import com.alibaba.fastjson.JSONObject;
import com.stanlong.bean.TableProcessDim;
import com.stanlong.constant.Constant;
import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.*;

/**
 * 处理主流业务数据和广播流配置数据关联后的逻辑
 */
public class TableProcessFunction extends BroadcastProcessFunction<JSONObject, TableProcessDim, Tuple2<JSONObject, TableProcessDim>> {

	private MapStateDescriptor<String, TableProcessDim> mapStateDescriptor;



	private Map<String, TableProcessDim> configMap = new HashMap<>();

	public TableProcessFunction(MapStateDescriptor<String, TableProcessDim> mapStateDescriptor){
		this.mapStateDescriptor = mapStateDescriptor;
	}

	@Override
	public void open(Configuration parameters) throws Exception {
		// 将配置表中的配置信息预加载到 configMap 中
		Class.forName("com.mysql.cj.jdbc.Driver");
		java.sql.Connection connection = DriverManager.getConnection(Constant.MYSQL_URL, Constant.MYSQL_USER_NAME, Constant.MYSQL_PASSWORD);
		String sql = "select * from gmall_config.table_process.dim";
		PreparedStatement preparedStatement = connection.prepareStatement(sql);
		ResultSet resultSet = preparedStatement.executeQuery();
		ResultSetMetaData metaData = resultSet.getMetaData();
		while (resultSet.next()){
			JSONObject jsonObject = new JSONObject();
			for (int i = 1; i <= metaData.getColumnCount() ; i++) {
				String columnName = metaData.getColumnName(i);
				Object columnValue = resultSet.getObject(i);
				jsonObject.put(columnName, columnValue);
			}
			TableProcessDim tableProcessDim = jsonObject.toJavaObject(TableProcessDim.class);
			configMap.put(tableProcessDim.getSourceTable(), tableProcessDim);

		}

		resultSet.close();
		preparedStatement.close();
		connection.close();


	}



	// 过滤掉不需要传递的字段
	private static void deleteNotNeedColumns(JSONObject dataJsonObj, String sinkColumns){
		List<String> columnList = Arrays.asList(sinkColumns.split(","));
		Set<Map.Entry<String, Object>> entrySet = dataJsonObj.entrySet();
		entrySet.removeIf(entry -> !columnList.contains(entry.getKey()));
	}

	@Override
	public void processElement(JSONObject jsonObject,
	                           BroadcastProcessFunction<JSONObject, TableProcessDim, Tuple2<JSONObject, TableProcessDim>>.ReadOnlyContext readOnlyContext,
	                           Collector<Tuple2<JSONObject, TableProcessDim>> collector) throws Exception {

		// 获取处理数据的表名
		String table = jsonObject.getString("table");
		// 获取广播状态
		ReadOnlyBroadcastState<String, TableProcessDim> broadcastState = readOnlyContext.getBroadcastState(mapStateDescriptor);
		// 根据表名先到广播状态中获取对应的配置信息， 如果没有找到对应的配置，再到configMap中获取
		TableProcessDim tableProcessDim = null;

		if((tableProcessDim = broadcastState.get(table))  != null
		  || (tableProcessDim = configMap.get(table)) != null){
			// 如果根据表名获取到了对应的配置信息，说明当前处理的是维度数据，将维度数据继续向下游传递
			JSONObject dataJSONObject = jsonObject.getJSONObject("data");

			// 在向下游传递数据前，过滤掉不需要的数据
			String sinkColumns = tableProcessDim.getSinkColumns();
			deleteNotNeedColumns(dataJSONObject, sinkColumns);

			// 在向下游传递数据前，补充对维度数据的操作类型
			String type = jsonObject.getString("type");
			dataJSONObject.put("type", type);
			collector.collect(Tuple2.of(dataJSONObject, tableProcessDim));
		}

	}

	@Override
	public void processBroadcastElement(TableProcessDim tableProcessDim,
	                                    BroadcastProcessFunction<JSONObject, TableProcessDim, Tuple2<JSONObject, TableProcessDim>>.Context context, Collector<Tuple2<JSONObject,
	  TableProcessDim>> collector) throws Exception {
		// 获取对配置表的操作类型
		String op = tableProcessDim.getOp();
		BroadcastState<String, TableProcessDim> broadcastState = context.getBroadcastState(mapStateDescriptor);
		String sourceTable = tableProcessDim.getSourceTable();
		if("d".equals(op)){
			// 从配置表中删除一条信息，则从广播状态中也删除一条信息
			broadcastState.remove(sourceTable);
			configMap.remove(sourceTable);
		}else {
			// 对配置表进行了读取，添加或者更新，将最新的配置信息放到广播状态中
			broadcastState.put(sourceTable, tableProcessDim);
			configMap.put(sourceTable, tableProcessDim);

		}

	}
}
