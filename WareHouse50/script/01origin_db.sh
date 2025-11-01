# 生成业务增量日志数据
# 该脚本在node02节点是用linux自带的定时任务执行
48 14 * * *  cd /opt/sgg5/servicelog && java -jar /opt/sgg5/servicelog/gmall2020-mock-db-2021-11-14.jar > /var/log/inc.log

# 生成业务全量日志数据
50 14 * * *  cd /opt/sgg5 && bash mysql_datax_hdfs_full.sh all > /var/log/servicelog/full.log 