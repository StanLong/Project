#!/bin/bash
# 生成日志数据
ssh node01 "cd /opt/sgg5/servicelog;java -jar gmall2020-mock-db-2021-11-14.jar >/dev/null 2>&1 & "
