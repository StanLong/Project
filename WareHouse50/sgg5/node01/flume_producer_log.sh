#!/bin/bash

case $1 in
"start") {
    ssh node03 "nohup flume-ng agent -n a1 -c /opt/flume-1.9.0/conf/ -f /opt/flume-1.9.0/job/file_to_kafka.conf >/dev/null 2>&1 &"
};;
"stop") {
    ssh node03 "ps -ef | grep file_to_kafka.conf | grep -v grep |awk  '{print \$2}' | xargs -n1 kill -9"
};;
esac
