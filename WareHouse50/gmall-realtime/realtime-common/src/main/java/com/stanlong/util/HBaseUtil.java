package com.stanlong.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;

import java.io.IOException;

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

	public static void createHBaseTable(Connection hbaseConn, String nameSpace, String table, String ... families)
	  throws IOException {
		Admin admin = hbaseConn.getAdmin();
		TableName tableName = TableName.valueOf(nameSpace, table);
		// 判断要建的表是否存在
		if (admin.tableExists(tableName)) {
			System.out.println(nameSpace + " 下的 " + tableName + "表已存在");
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
		System.out.println(nameSpace + "." + tableName + "建议成功");
	}

	public static void dropHBaseTable(Connection hbaseConn, String nameSpace, String table)
	  throws IOException {
		Admin admin = hbaseConn.getAdmin();
		TableName tableName = TableName.valueOf(nameSpace, table);
		if (admin.tableExists(tableName)) {
			admin.disableTable(tableName);
			admin.deleteTable(tableName);
		}
		admin.close();
		System.out.println(nameSpace + "." + tableName + "删除成功！");
	}
}
