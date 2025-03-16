package com.stanlong.realtime.function;

import com.alibaba.fastjson.JSONObject;
import com.stanlong.bean.TableProcessDim;
import com.stanlong.constant.Constant;
import com.stanlong.util.HBaseUtil;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.hadoop.hbase.client.Connection;

/**
 * 将流中的数据同步到Hbase中
 */
public class HbaseSinkFunction extends RichSinkFunction<Tuple2<JSONObject, TableProcessDim>> {
	private Connection hbaseConn;
	@Override
	public void open(Configuration parameters) throws Exception {
		hbaseConn = HBaseUtil.getHBaseConnection();
	}

	@Override
	public void close() throws Exception {
		HBaseUtil.closeHBaseConn(hbaseConn);
	}

	// 将流中的数据写出到HBase
	@Override
	public void invoke(Tuple2<JSONObject, TableProcessDim> value, Context context) throws Exception {
		JSONObject jsonObj = value.f0;
		TableProcessDim tableProcessDim = value.f1;
		String type = jsonObj.getString("type");
		jsonObj.remove("type");

		String sinkTable = tableProcessDim.getSinkTable();
		String rowKey = jsonObj.getString(tableProcessDim.getSinkRowKey());

		// 对业务数据库维度表进行了什么操作
		if("delete".equals(type)){
			// 业务表删除， HBase中也删除
			HBaseUtil.delRow(hbaseConn, Constant.HBASE_NAMESPACE, sinkTable, rowKey);
		}else { // 其他操作都是向Hbase中put数据
			String sinkFamily = tableProcessDim.getSinkFamily();
			HBaseUtil.putRow(hbaseConn, Constant.HBASE_NAMESPACE, sinkTable, rowKey, sinkFamily, jsonObj);

		}

	}
}
