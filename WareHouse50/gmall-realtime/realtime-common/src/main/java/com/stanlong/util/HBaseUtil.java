package com.stanlong.util;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.Set;

public class HBaseUtil {

	public static Connection getHBaseConnection() throws IOException {
		Configuration conf = new Configuration();
		conf.set("hbase.zookeeper.quorum", "node02");
		conf.set("hbase.zookeeper.property.clientPort", "2181");
		return ConnectionFactory.createConnection(conf);
	}

	public static void closeHBaseConn(Connection hbaseConn) throws IOException {
		if (hbaseConn != null && !hbaseConn.isClosed()) {
			hbaseConn.close();
		}
	}

	public static void createHBaseTable(Connection hbaseConn, String namespace, String table, String ... families)
	  throws IOException {
		Admin admin = hbaseConn.getAdmin();
		TableName tableName = TableName.valueOf(namespace, table);
		// 判断要建的表是否存在
		if (admin.tableExists(tableName)) {
			System.out.println(namespace + " 下的 " + tableName + "表已存在");
			return;
		}
		if(families.length < 1){
			System.out.println("至少需要一个列簇");
			return;
		}

		// 表描述器
		TableDescriptorBuilder tableDescriptorBuilder = TableDescriptorBuilder.newBuilder(tableName);


		for (String family : families){
			ColumnFamilyDescriptor cfDesc = ColumnFamilyDescriptorBuilder.of(family);	// 列族描述器
			tableDescriptorBuilder.setColumnFamily(cfDesc); // 指定列族
		}
		admin.createTable(tableDescriptorBuilder.build());
		admin.close();
		System.out.println(namespace + "." + tableName + "建议成功");
	}

	public static void dropHBaseTable(Connection hbaseConn, String namespace, String table)
	  throws IOException {
		Admin admin = hbaseConn.getAdmin();
		TableName tableName = TableName.valueOf(namespace, table);
		if (admin.tableExists(tableName)) {
			admin.disableTable(tableName);
			admin.deleteTable(tableName);
		}
		admin.close();
		System.out.println(namespace + "." + tableName + "删除成功！");
	}

	public static void putRow(Connection connection, String namespace, String tableName, String rowKey, String family, JSONObject jsonObject){
		TableName tableObj = TableName.valueOf(namespace, tableName);
		try (Table table = getHBaseConnection().getTable(tableObj);){
			Put put = new Put(Bytes.toBytes(rowKey));
			Set<String> keys = jsonObject.keySet();
			for(String key : keys){
				String value = jsonObject.getString(key);
				if(StringUtils.isNotEmpty(value)){
					put.addColumn(Bytes.toBytes(family), Bytes.toBytes(key), Bytes.toBytes(value));
				}
			}
			table.put(put);
			System.out.println("向 " + namespace + "." + tableName + " 中put数据"+ rowKey + "成功！");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void  delRow(Connection connection, String namespace, String tableName, String rowKey){
		TableName tableObj = TableName.valueOf(namespace, tableName);
		try (Table table = getHBaseConnection().getTable(tableObj);){
			Delete delete = new Delete(Bytes.toBytes(rowKey));
			table.delete(delete);
			System.out.println("从 " + namespace + "." + tableName + " 中delete数据"+ rowKey + "成功！");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
