#!/bin/bash

# 启动hadoop集群
echo "第一步: 启动Hadoop集群"
/opt/sgg5/ha.sh start

echo "第二步：启动flume采集日志"
/opt/sgg5/ha.sh/flume_producer_log.sh start

echo "第三步：启动kafka消息队列"
/opt/sgg5/kafka.sh start

echo "第四步：启动flume消费日志"
/opt/sgg5/flume_consumer_log.sh start

echo "第五步：生成用户日志数据"
nohup java -jar /opt/sgg5/applog/gmall2020-mock-log-2021-10-10.jar > /dev/null 2>&1 &

echo "第五步：启动maxwell同步mysql数据"
/opt/sgg5/maxwell.sh start

echo "第七步：生成用户业务数据"
nohup java -jar /opt/sgg5/servicelog/gmall2020-mock-db-2021-11-14.jar > /dev/null 2>&1 &
