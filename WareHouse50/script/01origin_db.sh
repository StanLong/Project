# 生成增量量日志数据
# 该脚本在node02节点是用linux自带的定时任务执行
0 12 * * *  cd /opt/sgg5/servicelog && java -jar /opt/sgg5/servicelog/gmall2020-mock-db-2021-11-14.jar > /dev/null 2>&1 &