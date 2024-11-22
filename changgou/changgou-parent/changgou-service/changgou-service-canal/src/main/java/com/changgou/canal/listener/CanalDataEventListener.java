package com.changgou.canal.listener;

import com.alibaba.otter.canal.protocol.CanalEntry;
import com.xpand.starter.canal.annotation.*;

@CanalEventListener
public class CanalDataEventListener {
    @InsertListenPoint
    public void onEventInsert(CanalEntry.EventType eventType, CanalEntry.RowData rowData){
        for (CanalEntry.Column column : rowData.getAfterColumnsList()) {
            System.out.println("列名：" + column.getName() + "----------变更的数据： " + column.getValue());
        }

    }

    @UpdateListenPoint
    public void onEventUpdate(CanalEntry.EventType eventType, CanalEntry.RowData rowData) {
        for (CanalEntry.Column column : rowData.getBeforeColumnsList()) {
            System.out.println("修改前列名：" + column.getName() + "----------变更的数据： " + column.getValue());
        }

        for (CanalEntry.Column column : rowData.getAfterColumnsList()) {
            System.out.println("修改后列名：" + column.getName() + "----------变更的数据： " + column.getValue());
        }
    }

    /***
     * 删除数据监听
     * @param eventType
     */
    @DeleteListenPoint
    public void onEventDelete(CanalEntry.EventType eventType, CanalEntry.RowData rowData) {
        for (CanalEntry.Column column : rowData.getBeforeColumnsList()) {
            System.out.println("删除前列名： " + column.getName() + "变更的数据" + column.getValue());
        }

    }

    @ListenPoint(destination = "example", // 指定实例的地址
            schema = "changgou_content", // 自定义监控的数据库
            table = {"tb_content_category", "tb_content"}, // 自定义监控的表
            eventType = {CanalEntry.EventType.UPDATE, CanalEntry.EventType.DELETE} // 自定义监控类型
    )
    public void onEventCustomUpdate(CanalEntry.EventType eventType, CanalEntry.RowData rowData){
        for (CanalEntry.Column column : rowData.getBeforeColumnsList()) {
            System.out.println("自定义监控前列名：" + column.getName() + "----------变更的数据： " + column.getValue());
        }

        for (CanalEntry.Column column : rowData.getAfterColumnsList()) {
            System.out.println("自定义监控后列名：" + column.getName() + "----------变更的数据： " + column.getValue());
        }

    }
}
