package com.watermonitor.service;

import java.util.Map;

public interface PipeFlowService {

    /** 生成模拟数据 */
    Map<String, Object> generateData(String startDate, int pipeCount);

    /** 按时间段统计水流量 */
    Map<String, Object> flowTrend(String startDate, String endDate, String pipeNo);

    /** 异常等级占比分析 */
    Map<String, Object> anomalyStats(String startDate, String endDate);

    /** 工作日/周末流量对比 */
    Map<String, Object> weekdayWeekendCompare(String startDate, String endDate);

    /** 高峰用水时段统计 */
    Map<String, Object> peakHourAnalysis(String startDate, String endDate);

    /** 实时监测面板数据 */
    Map<String, Object> realtimePanel();

    /** 多管网流量对比 */
    Map<String, Object> multiPipeCompare(String startDate, String endDate, int topN);

    /** 总记录数 */
    Map<String, Object> dataOverview();

    /** AI流量预测 */
    Map<String, Object> predictFlow(String pipeNo);
}
