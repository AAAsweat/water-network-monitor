package com.watermonitor.controller;

import com.watermonitor.service.PipeFlowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class PipeFlowController {

    @Autowired
    private PipeFlowService pipeFlowService;

    // ==================== 数据生成 ====================

    /**
     * 一键生成千万级模拟数据
     * GET /api/generate?startDate=2024-01-01&pipeCount=500
     */
    @GetMapping("/generate")
    public Map<String, Object> generateData(
            @RequestParam(defaultValue = "2023-01-01") String startDate,
            @RequestParam(defaultValue = "500") int pipeCount) {
        return pipeFlowService.generateData(startDate, pipeCount);
    }

    // ==================== 数据分析 ====================

    /**
     * 流量趋势曲线数据
     * GET /api/flow/trend?startDate=2024-01-01&endDate=2024-12-31&pipeNo=PIPE0001
     */
    @GetMapping("/flow/trend")
    public Map<String, Object> flowTrend(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String pipeNo) {
        return pipeFlowService.flowTrend(startDate, endDate, pipeNo);
    }

    /**
     * 异常等级占比分析
     * GET /api/flow/anomaly?startDate=2024-01-01&endDate=2024-12-31
     */
    @GetMapping("/flow/anomaly")
    public Map<String, Object> anomalyStats(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return pipeFlowService.anomalyStats(startDate, endDate);
    }

    /**
     * 工作日/周末流量对比
     * GET /api/flow/weekday-compare?startDate=2024-01-01&endDate=2024-12-31
     */
    @GetMapping("/flow/weekday-compare")
    public Map<String, Object> weekdayWeekendCompare(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return pipeFlowService.weekdayWeekendCompare(startDate, endDate);
    }

    /**
     * 高峰用水时段统计
     * GET /api/flow/peak-hour?startDate=2024-01-01&endDate=2024-12-31
     */
    @GetMapping("/flow/peak-hour")
    public Map<String, Object> peakHourAnalysis(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return pipeFlowService.peakHourAnalysis(startDate, endDate);
    }

    /**
     * 多管网流量对比
     * GET /api/flow/multi-compare?startDate=2024-01-01&endDate=2024-12-31&topN=10
     */
    @GetMapping("/flow/multi-compare")
    public Map<String, Object> multiPipeCompare(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "10") int topN) {
        return pipeFlowService.multiPipeCompare(startDate, endDate, topN);
    }

    // ==================== 实时面板 ====================

    /**
     * 实时监测面板数据
     * GET /api/realtime
     */
    @GetMapping("/realtime")
    public Map<String, Object> realtimePanel() {
        return pipeFlowService.realtimePanel();
    }

    /**
     * 数据概览
     * GET /api/overview
     */
    @GetMapping("/overview")
    public Map<String, Object> dataOverview() {
        return pipeFlowService.dataOverview();
    }

    // ==================== AI预测 ====================

    /**
     * AI流量预测
     * GET /api/predict?pipeNo=PIPE0001
     */
    @GetMapping("/predict")
    public Map<String, Object> predictFlow(@RequestParam String pipeNo) {
        return pipeFlowService.predictFlow(pipeNo);
    }
}
