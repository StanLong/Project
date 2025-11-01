#!/bin/bash

case $1 in
"start"){
    echo "-------- 启动 [node02,node03] kafka --------"
    ./kafka.sh start
    
    echo "-------- 启动 [node01] maxwell --------"
    ./maxwell.sh start
    
    echo "-------- 启动 [node03] flume 采集日志--------"
    ./flume_producer_log.sh start
    
    echo "-------- 启动 [node02] flume 消费日志--------"
    ./flume_consumer_log.sh start
    echo "-------- 启动 [node02] flume 消费业务数据 --------"
    ./flume_consumer_db.sh start
    
};;
"stop"){
    echo "-------- 关闭 [node01] maxwell --------"
    ./maxwell.sh stop

    echo "-------- 关闭 [node02] flume 停止消费业务数据 --------"
    ./flume_consumer_db.sh stop
    echo "-------- 关闭 [node03] flume 停止消费日志数据--------"
    ./flume_consumer_log.sh stop
    
    echo "-------- 关闭 [node03] flume 停止采集日志数据--------"
    ./flume_producer_log.sh stop
    
    echo "-------- 关闭 [node02, node03]kafka --------"
    ./kafka.sh stop
};;
esac
