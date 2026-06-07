package com.watermonitor.service.impl;

import com.watermonitor.ai.LinearRegressionModel;
import com.watermonitor.dao.PipeFlowMapper;
import com.watermonitor.dao.TableStatsMapper;
import com.watermonitor.entity.PipeFlowRecord;
import com.watermonitor.service.DataGenerateService;
import com.watermonitor.service.PipeFlowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PipeFlowServiceImpl implements PipeFlowService {

    @Autowired
    private PipeFlowMapper pipeFlowMapper;

    @Autowired
    private TableStatsMapper tableStatsMapper;

    @Autowired
    private DataGenerateService dataGenerateService;

    @Override
    public Map<String, Object> generateData(String startDate, int pipeCount) {
        return dataGenerateService.generateData(startDate, pipeCount);
    }

    // ====== 分析类查询：全查聚合表，真SQL ======

    @Override
    public Map<String, Object> flowTrend(String startDate, String endDate, String pipeNo) {
        List<Map<String, Object>> data = pipeFlowMapper.dailyFlowByDateRange(startDate, endDate, pipeNo);

        List<String> dates = new ArrayList<>();
        List<Object> totalFlows = new ArrayList<>();
        List<Object> avgFlows = new ArrayList<>();

        for (Map<String, Object> row : data) {
            dates.add(row.get("monitorDate").toString());
            totalFlows.add(row.get("totalFlow"));
            avgFlows.add(row.get("avgFlow"));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dates", dates);
        result.put("totalFlows", totalFlows);
        result.put("avgFlows", avgFlows);
        result.put("dataCount", data.size());
        return result;
    }

    @Override
    public Map<String, Object> anomalyStats(String startDate, String endDate) {
        List<Map<String, Object>> stats = pipeFlowMapper.anomalyStatsFromDaily(startDate, endDate);

        String[] levelNames = {"正常", "轻微异常", "中度异常", "严重异常"};
        List<String> labels = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        long total = 0;
        for (Map<String, Object> row : stats) {
            long count = ((Number) row.get("count")).longValue();
            total += count;
        }

        for (Map<String, Object> row : stats) {
            int level = ((Number) row.get("anomalyLevel")).intValue();
            long count = ((Number) row.get("count")).longValue();
            double pct = total > 0 ? Math.round(count * 10000.0 / total) / 100.0 : 0;
            labels.add(levelNames[level]);
            values.add(pct);
            row.put("percentage", pct);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("labels", labels);
        result.put("values", values);
        result.put("detail", stats);
        return result;
    }

    @Override
    public Map<String, Object> weekdayWeekendCompare(String startDate, String endDate) {
        List<Map<String, Object>> data = pipeFlowMapper.weekdayVsWeekendFromDaily(startDate, endDate);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", data);
        for (Map<String, Object> row : data) {
            result.put((String) row.get("dayType"), row.get("avgFlow"));
        }
        return result;
    }

    @Override
    public Map<String, Object> peakHourAnalysis(String startDate, String endDate) {
        List<String> allPipeNos = pipeFlowMapper.getAllPipeNos();
        List<Map<String, Object>> data = pipeFlowMapper.hourlyFlowStats(allPipeNos);

        List<Integer> hours = new ArrayList<>();
        List<Object> avgFlows = new ArrayList<>();
        List<Object> maxFlows = new ArrayList<>();
        int peakHour = 0;
        double peakAvg = 0;

        for (Map<String, Object> row : data) {
            int hour = ((Number) row.get("monitorHour")).intValue();
            double avg = ((Number) row.get("avgFlow")).doubleValue();
            hours.add(hour);
            avgFlows.add(avg);
            maxFlows.add(row.get("maxFlow"));
            if (avg > peakAvg) { peakAvg = avg; peakHour = hour; }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hours", hours);
        result.put("avgFlows", avgFlows);
        result.put("maxFlows", maxFlows);
        result.put("peakHour", peakHour);
        return result;
    }

    @Override
    public Map<String, Object> multiPipeCompare(String startDate, String endDate, int topN) {
        List<String> allPipeNos = pipeFlowMapper.getAllPipeNos();
        if (allPipeNos.size() > topN) {
            allPipeNos = allPipeNos.subList(0, topN);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        if (allPipeNos.isEmpty()) {
            result.put("names", new ArrayList<>());
            result.put("totalFlows", new ArrayList<>());
            result.put("avgFlows", new ArrayList<>());
            return result;
        }

        List<Map<String, Object>> data = pipeFlowMapper.multiPipeFromDaily(startDate, endDate, allPipeNos);

        List<String> names = new ArrayList<>();
        List<Object> totalFlows = new ArrayList<>();
        List<Object> avgFlows = new ArrayList<>();

        for (Map<String, Object> row : data) {
            names.add(row.get("pipeName") != null ? row.get("pipeName").toString() : row.get("pipeNo").toString());
            totalFlows.add(row.get("totalFlow"));
            avgFlows.add(row.get("avgFlow"));
        }

        result.put("names", names);
        result.put("totalFlows", totalFlows);
        result.put("avgFlows", avgFlows);
        return result;
    }

    @Override
    public Map<String, Object> dataOverview() {
        long totalCount = getCachedTotalCount();
        List<String> pipeNos = pipeFlowMapper.getAllPipeNos();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRecords", totalCount);
        result.put("pipeCount", pipeNos.size());
        result.put("pipeNos", pipeNos);
        return result;
    }

    // ====== 实时面板 + AI预测：查原始表 ======

    private long getCachedTotalCount() {
        Long cached = tableStatsMapper.getRowCount("pipe_flow_record");
        if (cached != null && cached > 0) return cached;
        return pipeFlowMapper.countAll();
    }

    @Override
    public Map<String, Object> realtimePanel() {
        List<PipeFlowRecord> latest = pipeFlowMapper.getLatestRecords(20);
        long totalCount = getCachedTotalCount();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("latestRecords", latest);
        result.put("totalRecords", totalCount);
        result.put("recordCount", latest.size());
        return result;
    }

    @Override
    public Map<String, Object> predictFlow(String pipeNo) {
        Map<String, Object> result = new LinkedHashMap<>();

        List<Map<String, Object>> hourlyAvgData = pipeFlowMapper.getRecentHourlyAvgForPipe(pipeNo, 30);

        if (hourlyAvgData.size() < 12) {
            result.put("error", "该管网历史数据不足，无法进行预测");
            return result;
        }

        Map<Integer, Double> hourlyAvgMap = new LinkedHashMap<>();
        for (Map<String, Object> row : hourlyAvgData) {
            int hour = ((Number) row.get("monitorHour")).intValue();
            double avgFlow = ((Number) row.get("avgFlow")).doubleValue();
            hourlyAvgMap.put(hour, avgFlow);
        }

        Map<String, Object> predictionData = LinearRegressionModel.FeatureBuilder.build24HourPredictionData(
            hourlyAvgMap, new ArrayList<>());

        double[][] features = (double[][]) predictionData.get("features");
        double[] targets = (double[]) predictionData.get("targets");
        double[][] predictFeatures = (double[][]) predictionData.get("predictFeatures");
        @SuppressWarnings("unchecked")
        List<Integer> predictHours = (List<Integer>) predictionData.get("hours");

        LinearRegressionModel model = new LinearRegressionModel();
        model.train(features, targets);
        double[] predictions = model.predictBatch(predictFeatures);

        List<Map<String, Object>> forecastData = new ArrayList<>();
        for (int i = 0; i < predictions.length; i++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("hour", predictHours.get(i));
            item.put("predictedFlow", Math.round(predictions[i] * 100.0) / 100.0);
            item.put("historicalAvg", hourlyAvgMap.getOrDefault(predictHours.get(i), 0.0));
            forecastData.add(item);
        }

        result.put("pipeNo", pipeNo);
        result.put("modelScore", Math.round(model.getTrainScore() * 10000.0) / 10000.0);
        result.put("forecast", forecastData);
        double totalPredicted = Arrays.stream(predictions).sum();
        result.put("totalPredicted24h", Math.round(totalPredicted * 100.0) / 100.0);

        return result;
    }
}
