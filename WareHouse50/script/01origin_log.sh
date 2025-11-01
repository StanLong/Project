#!/bin/bash
# 数据从这里产生， 
# 在 dolphinscheduler 里新建shell工作流，执行这段脚本, worker节点选node03
cd /opt/sgg5/applog/
java -jar gmall2020-mock-log-2021-10-10.jar >/dev/null 2>&1 &