#!/bin/bash
KAFKA_HOME="/opt/kafka_2.13-3.8.0"
case $1 in
"start"){
	for i in node{02..03}
	do
		echo "*********** 启动 $i kafka ***********"
		ssh $i "$KAFKA_HOME/bin/kafka-server-start.sh -daemon $KAFKA_HOME/config/server.properties"
	done
};;
"stop"){
	for i in node{02..03}
	do
		echo "*********** 停止 $i kafka ***********"
		ssh $i "$KAFKA_HOME/bin/kafka-server-stop.sh"
	done
};;
esac
