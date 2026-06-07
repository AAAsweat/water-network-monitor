package com.watermonitor.dao;

import com.watermonitor.entity.PipeFlowRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Mapper
public interface PipeFlowMapper {

    /** 批量插入监测记录 */
    int batchInsert(@Param("list") List<PipeFlowRecord> list);

    /** 按时间段统计总水流量 */
    List<Map<String, Object>> sumFlowByDateRange(@Param("startDate") String startDate,
                                                  @Param("endDate") String endDate,
                                                  @Param("pipeNo") String pipeNo);

    /** 按管网统计水流量 */
    List<Map<String, Object>> sumFlowByPipe(@Param("startDate") String startDate,
                                             @Param("endDate") String endDate);

    /** 异常等级占比统计 */
    List<Map<String, Object>> anomalyLevelStats(@Param("startDate") String startDate,
                                                 @Param("endDate") String endDate);

    /** 工作日/周末流量对比 */
    List<Map<String, Object>> weekdayVsWeekendFlow(@Param("startDate") String startDate,
                                                    @Param("endDate") String endDate);

    /** 各小时平均流量（高峰时段分析） */
    List<Map<String, Object>> hourlyAvgFlow(@Param("startDate") String startDate,
                                             @Param("endDate") String endDate);

    /** 获取最近监测数据（实时面板） */
    List<PipeFlowRecord> getLatestRecords(@Param("limit") int limit);

    /** 获取总记录数 */
    long countAll();

    /** 获取某管网最近N条历史流量数据（用于AI预测） */
    List<Map<String, Object>> getHistoryFlowForPredict(@Param("pipeNo") String pipeNo,
                                                        @Param("limit") int limit);

    /** 获取某管网某小时的近期平均流量（用于预测） */
    List<Map<String, Object>> getRecentHourlyAvgForPipe(@Param("pipeNo") String pipeNo,
                                                         @Param("daysBack") int daysBack);

    /** 多管网流量对比 */
    List<Map<String, Object>> multiPipeCompare(@Param("startDate") String startDate,
                                                @Param("endDate") String endDate,
                                                @Param("pipeNos") List<String> pipeNos);

    /** 按日期统计总流量 */
    List<Map<String, Object>> dailyFlowByDateRange(@Param("startDate") String startDate,
                                                    @Param("endDate") String endDate,
                                                    @Param("pipeNo") String pipeNo);

    /** 获取所有管网编号 */
    List<String> getAllPipeNos();

    // ====== 聚合表查询（替代大表全扫） ======

    /** 从日汇总表查异常占比 */
    List<Map<String, Object>> anomalyStatsFromDaily(@Param("startDate") String startDate,
                                                     @Param("endDate") String endDate);

    /** 从小时汇总表查各小时均值 */
    List<Map<String, Object>> hourlyFlowStats(@Param("pipeNos") List<String> pipeNos);

    /** 从日汇总表查工作日/周末对比 */
    List<Map<String, Object>> weekdayVsWeekendFromDaily(@Param("startDate") String startDate,
                                                         @Param("endDate") String endDate);

    /** 从日汇总表查多管网对比 */
    List<Map<String, Object>> multiPipeFromDaily(@Param("startDate") String startDate,
                                                  @Param("endDate") String endDate,
                                                  @Param("pipeNos") List<String> pipeNos);
}
