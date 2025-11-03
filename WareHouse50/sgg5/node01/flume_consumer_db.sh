#!/bin/bash

case $1 in
"start") {
    ssh node02 "nohup flume-ng agent -n a1 -c /opt/flume-1.9.0/conf/ -f /opt/flume-1.9.0/job/kafka_to_hdfs_db.conf  >/dev/null 2>&1 &"
};;
"stop") {
    ssh node02 "ps -ef | grep kafka_to_hdfs_db.conf | grep -v grep |awk  '{print \$2}' | xargs -n1 kill -9"
};;
esac
