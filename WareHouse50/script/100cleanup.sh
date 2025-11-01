#!/bin/bash
# 集群清理脚本

CLEAN_DATE=`date "+%Y-%m-%d"`

# 替换掉 node03 上 /opt/sgg5/applog/application.yml 里的业务时间为清理时间
ssh node03 "sed -i 's/mock.date:.*$/mock.date: \"$CLEAN_DATE\"/' /opt/sgg5/applog/application.yml "

# 清理旧数据
ssh node03 "rm -rf /opt/sgg5/applog/logs/*"
hdfs dfs -rm -r -f /origin_data/gmall/log/topic_log/*

# 清理docker中mysql的业务数据
docker exec -it mysql mysql -u root -proot -e "drop database gmall;"
docker exec -it mysql mysql -u root -proot -e "create database gmall default charset utf8 COLLATE utf8_general_ci;"

# 加载宿主机的sql脚本到mysql容器
docker exec -i mysql mysql -u root -proot gmall < /opt/sgg5/gmall.sql