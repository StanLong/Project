#!/bin/bash
start(){
    echo "------------------------ 启动dolphinscheduler ------------------------"
    /opt/dolphinscheduler-3.1.9/bin/start-all.sh
}
status(){
    /opt/dolphinscheduler-3.1.9/bin/status-all.sh
}
stop(){
    echo "------------------------ 停止dolphinscheduler ------------------------"
    /opt/dolphinscheduler-3.1.9/bin/stop-all.sh
}
case "$1" in
    start)
        $1
        ;;
    status)
        $1
        ;;
    stop)
        $1
        ;;
    *)
    echo $"Usage: $0 {start|status|stop}" 
        exit 2 
esac
