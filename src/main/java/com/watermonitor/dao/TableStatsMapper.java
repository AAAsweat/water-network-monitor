package com.watermonitor.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TableStatsMapper {

    /** 查询缓存计数 */
    Long getRowCount(@Param("tableName") String tableName);

    /** 插入或更新计数缓存 */
    int upsertRowCount(@Param("tableName") String tableName,
                       @Param("rowCount") long rowCount);
}
